package com.billy.cc.core.component;

import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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
        final CC cc = CC.obtainBuilder(remoteCC.getComponentName())
                .setActionName(remoteCC.getActionName())
                .setParams(remoteCC.getParams())
                .setCallId(remoteCC.getCallId())
                .withoutGlobalInterceptor()
                .build();
        if (remoteCC.isMainThreadSyncCall()) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    CCResult ccResult = cc.call();
                    doCallback(callback, cc, ccResult);
                }
            });
        } else if (remoteCC.isResultRequired()) {
            cc.callAsync(new IComponentCallback() {
                @Override
                public void onResult(CC cc, CCResult result) {
                    doCallback(callback, cc, result);
                }
            });
        } else {
            cc.callAsync();
            doCallback(callback, cc, CCResult.success());
        }
    }

    private static void doCallback(IRemoteCallback callback, CC cc, CCResult ccResult) {
        try {
            callback.callback(new RemoteCCResult(ccResult));
        } catch (RemoteException e) {
            e.printStackTrace();
            CC.verboseLog(cc.getCallId(), "remote doCallback failed!");
        }
    }

    @Override
    public void cancel(String callId) throws RemoteException {
        CC.cancel(callId);
    }

    @Override
    public void timeout(String callId) throws RemoteException {
        CC.timeout(callId);
    }

    @Override
    public String getComponentProcessName(String componentName) throws RemoteException {
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
                    e.printStackTrace();
                }
            }
        }
    }
}
