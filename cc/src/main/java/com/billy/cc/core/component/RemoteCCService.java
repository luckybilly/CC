package com.billy.cc.core.component;

import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;

import com.billy.cc.core.component.remote.IRemoteCCService;
import com.billy.cc.core.component.remote.IRemoteCallback;
import com.billy.cc.core.component.remote.RemoteCC;
import com.billy.cc.core.component.remote.RemoteCCResult;
import com.billy.cc.core.component.remote.RemoteCursor;
import com.billy.cc.core.component.remote.RemoteProvider;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨进程调用组件的Binder
 * @author billy.qi
 * @since 18/6/24 11:31
 */
public class RemoteCCService extends IRemoteCCService.Stub {

    private Handler mainThreadHandler;

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class RemoteCCServiceHolder {
        private static final RemoteCCService INSTANCE = new RemoteCCService();
    }
    private RemoteCCService(){
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }
    /** 获取RemoteCCService的单例对象 */
    public static RemoteCCService getInstance() {
        return RemoteCCService.RemoteCCServiceHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    @Override
    public void call(final RemoteCC remoteCC, final IRemoteCallback callback) throws RemoteException {
        if (isInvalidate()) {
            return;
        }
        String componentName = remoteCC.getComponentName();
        final String callId = remoteCC.getCallId();
        if (CC.VERBOSE_LOG) {
            CC.verboseLog(callId, "receive call from other process. RemoteCC: %s", remoteCC.toString());
        }
        if (!ComponentManager.hasComponent(componentName)) {
            CC.verboseLog(callId, "There is no component found for name:%s in process:%s", componentName, CCUtil.getCurProcessName());
            doCallback(callback, callId, CCResult.error(CCResult.CODE_ERROR_NO_COMPONENT_FOUND));
            return;
        }
        final CC cc = CC.obtainBuilder(componentName)
                .setActionName(remoteCC.getActionName())
                .setParams(remoteCC.getParams())
                .setCallId(remoteCC.getCallId())
                .withoutGlobalInterceptor() //为了不重复调用拦截器，全局拦截器需要下沉复用，只在调用方进程中执行
                .setNoTimeout() //超时逻辑在调用方进程中处理
                .build();
        if (remoteCC.isMainThreadSyncCall()) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    CCResult ccResult = cc.call();
                    doCallback(callback, callId, ccResult);
                }
            });
        } else {
            cc.callAsync(new IComponentCallback() {
                @Override
                public void onResult(CC cc, CCResult result) {
                    doCallback(callback, callId, result);
                }
            });
        }
    }

    private boolean isInvalidate() {
        //未开启跨app调用时进行跨app调用视为无效调用
        return !CC.isRemoteCCEnabled() && getCallingUid() != Process.myUid();
    }

    private static void doCallback(IRemoteCallback callback, String callId, CCResult ccResult) {
        try {
            RemoteCCResult remoteCCResult;
            try{
                remoteCCResult = new RemoteCCResult(ccResult);
                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(callId, "callback to other process. RemoteCCResult: %s", remoteCCResult.toString());
                }
            }catch(Exception e){
                remoteCCResult = new RemoteCCResult(CCResult.error(CCResult.CODE_ERROR_REMOTE_CC_DELIVERY_FAILED));
                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(callId, "remote CC success. But result can not be converted for IPC. RemoteCCResult: %s", remoteCCResult.toString());
                }
            }
            callback.callback(remoteCCResult);
        } catch (RemoteException e) {
            CCUtil.printStackTrace(e);
            CC.verboseLog(callId, "remote doCallback failed!");
        }
    }

    @Override
    public void cancel(String callId) throws RemoteException {
        if (isInvalidate()) {
            return;
        }
        CC.cancel(callId);
    }

    @Override
    public void timeout(String callId) throws RemoteException {
        if (isInvalidate()) {
            return;
        }
        CC.timeout(callId);
    }

    @Override
    public String getComponentProcessName(String componentName) throws RemoteException {
        if (isInvalidate()) {
            return null;
        }
        return ComponentManager.getComponentProcessName(componentName);
    }

    private static final ConcurrentHashMap<String, IRemoteCCService> CACHE = new ConcurrentHashMap<>();
    private static final byte[] LOCK = new byte[0];

    private static Uri getDispatcherProviderUri(String processName) {
        return Uri.parse("content://" + processName + "." + RemoteProvider.URI_SUFFIX + "/cc");
    }

    static IRemoteCCService get(String processNameTo) {
        IRemoteCCService service = CACHE.get(processNameTo);
        if (service == null && CC.getApplication() != null) {
            synchronized (LOCK) {
                service = CACHE.get(processNameTo);
                if (service == null) {
                    service = getService(processNameTo);
                    if (service != null) {
                        CACHE.put(processNameTo, service);
                    }
                }
            }
        }
        return service;
    }

    static void remove(String processName) {
        CACHE.remove(processName);
    }

    private static IRemoteCCService getService(String processNameTo) {
        Cursor cursor = null;
        try {
            cursor = CC.getApplication().getContentResolver()
                    .query(getDispatcherProviderUri(processNameTo)
                            , RemoteProvider.PROJECTION_MAIN, null
                            , null, null
                    );
            if (cursor == null) {
                return null;
            }
            return RemoteCursor.getRemoteCCService(cursor);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    CCUtil.printStackTrace(e);
                }
            }
        }
    }
}
