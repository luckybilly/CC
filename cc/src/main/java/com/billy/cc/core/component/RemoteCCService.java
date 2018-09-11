package com.billy.cc.core.component;

import android.database.Cursor;
import android.net.Uri;
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

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class RemoteCCServiceHolder {
        private static final RemoteCCService INSTANCE = new RemoteCCService();
    }
    private RemoteCCService(){}
    /** 获取RemoteCCService的单例对象 */
    public static RemoteCCService getInstance() {
        return RemoteCCService.RemoteCCServiceHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    static class RemoteComponentCallback implements IComponentCallback {

        private IRemoteCallback callback;

        public RemoteComponentCallback(IRemoteCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onResult(CC cc, CCResult result) {
            try {
                callback.callback(new RemoteCCResult(result));
            } catch (RemoteException e) {
                CC.verboseLog(cc.getCallId(), "remote callback failed!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void call(RemoteCC remoteCC, IRemoteCallback callback) throws RemoteException {
        CC cc = CC.obtainBuilder(remoteCC.getComponentName())
                .setActionName(remoteCC.getActionName())
                .setParams(remoteCC.getParams())
                .setCallId(remoteCC.getCallId())
                .withoutGlobalInterceptor()
                .build();
        if (remoteCC.isResultRequired()) {
            cc.callAsync(new RemoteComponentCallback(callback));
        } else {
            cc.callAsync();
            callback.callback(new RemoteCCResult(CCResult.success()));
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
