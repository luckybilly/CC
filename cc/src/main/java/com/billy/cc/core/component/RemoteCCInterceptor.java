package com.billy.cc.core.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.DeadObjectException;
import android.os.SystemClock;
import android.text.TextUtils;

import com.billy.cc.core.component.remote.IRemoteCCService;
import com.billy.cc.core.component.remote.RemoteConnection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨App调用组件
 * 继承自 {@link SubProcessCCInterceptor}, 额外处理了跨App的进程连接
 * @author billy.qi
 * @since 18/6/24 00:25
 */
class RemoteCCInterceptor extends SubProcessCCInterceptor {

    private static final ConcurrentHashMap<String, IRemoteCCService> REMOTE_CONNECTIONS = new ConcurrentHashMap<>();

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class RemoteCCInterceptorHolder {
        private static final RemoteCCInterceptor INSTANCE = new RemoteCCInterceptor();
    }
    private RemoteCCInterceptor(){}
    /** 获取{@link RemoteCCInterceptor}的单例对象 */
    static RemoteCCInterceptor getInstance() {
        return RemoteCCInterceptorHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    @Override
    public CCResult intercept(Chain chain) {
        String processName = getProcessName(chain.getCC().getComponentName());
        if (!TextUtils.isEmpty(processName)) {
            return multiProcessCall(chain, processName, REMOTE_CONNECTIONS);
        }
        return CCResult.error(CCResult.CODE_ERROR_NO_COMPONENT_FOUND);
    }

    private String getProcessName(String componentName) {
        String processName = null;
        try {
            for (Map.Entry<String, IRemoteCCService> entry : REMOTE_CONNECTIONS.entrySet()) {
                try {
                    processName = entry.getValue().getComponentProcessName(componentName);
                } catch(DeadObjectException e) {
                    String processNameTo = entry.getKey();
                    RemoteCCService.remove(processNameTo);
                    IRemoteCCService service = RemoteCCService.get(processNameTo);
                    if (service == null) {
                        String packageName = processNameTo.split(":")[0];
                        boolean wakeup = RemoteConnection.tryWakeup(packageName);
                        CC.log("wakeup remote app '%s'. success=%b.", packageName, wakeup);
                        if (wakeup) {
                            service = getMultiProcessService(processNameTo);
                        }
                    }
                    if (service != null) {
                        try {
                            processName = service.getComponentProcessName(componentName);
                            REMOTE_CONNECTIONS.put(processNameTo, service);
                        } catch(Exception ex) {
                            CCUtil.printStackTrace(ex);
                        }
                    }
                }
                if (!TextUtils.isEmpty(processName)) {
                    return processName;
                }
            }
        } catch(Exception e) {
            CCUtil.printStackTrace(e);
        }
        return processName;
    }

    void enableRemoteCC() {
        //监听设备上其它包含CC组件的app
        listenComponentApps();
        connect(RemoteConnection.scanComponentApps());
    }

    private static final String INTENT_FILTER_SCHEME = "package";
    private void listenComponentApps() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        intentFilter.addDataScheme(INTENT_FILTER_SCHEME);
        CC.getApplication().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getDataString();
                if (TextUtils.isEmpty(packageName)) {
                    return;
                }
                if (packageName.startsWith(INTENT_FILTER_SCHEME)) {
                    packageName = packageName.replace(INTENT_FILTER_SCHEME + ":", "");
                }
                String action = intent.getAction();
                CC.log("onReceived.....pkg=" + packageName + ", action=" + action);
                if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    REMOTE_CONNECTIONS.remove(packageName);
                } else {
                    CC.log("start to wakeup remote app:%s", packageName);
                    if (RemoteConnection.tryWakeup(packageName)) {
                        ComponentManager.threadPool(new ConnectTask(packageName));
                    }
                }
            }
        }, intentFilter);
    }

    private void connect(List<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) {
            return;
        }
        for (String pkg : packageNames) {
            ComponentManager.threadPool(new ConnectTask(pkg));
        }
    }

    class ConnectTask implements Runnable {
        String packageName;

        ConnectTask(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public void run() {
            IRemoteCCService service = getMultiProcessService(packageName);
            if (service != null) {
                REMOTE_CONNECTIONS.put(packageName, service);
            }
        }
    }

    private static final int MAX_CONNECT_TIME_DURATION = 1000;
    @Override
    protected IRemoteCCService getMultiProcessService(String packageName) {
        long start = SystemClock.elapsedRealtime();
        IRemoteCCService service = null;
        while (SystemClock.elapsedRealtime() - start < MAX_CONNECT_TIME_DURATION) {
            service = RemoteCCService.get(packageName);
            if (service != null) {
                break;
            }
            SystemClock.sleep(50);
        }
        CC.log("connect remote app '%s' %s. cost time=%d"
                , packageName
                , service == null ? "failed" : "success"
                , (SystemClock.elapsedRealtime() - start));
        return service;
    }

}
