package com.billy.cc.core.component;

import android.os.DeadObjectException;
import android.os.Looper;
import android.os.RemoteException;

import com.billy.cc.core.component.remote.IRemoteCCService;
import com.billy.cc.core.component.remote.IRemoteCallback;
import com.billy.cc.core.component.remote.RemoteCC;
import com.billy.cc.core.component.remote.RemoteCCResult;

import java.util.concurrent.ConcurrentHashMap;

/**
 * App内跨进程调用组件
 * @author billy.qi
 * @since 18/6/24 00:25
 */
class SubProcessCCInterceptor implements ICCInterceptor {

    private static final ConcurrentHashMap<String, IRemoteCCService> CONNECTIONS = new ConcurrentHashMap<>();

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class SubProcessCCInterceptorHolder {
        private static final SubProcessCCInterceptor INSTANCE = new SubProcessCCInterceptor();
    }
    SubProcessCCInterceptor(){}
    /** 获取SubProcessCCInterceptor的单例对象 */
    static SubProcessCCInterceptor getInstance() {
        return SubProcessCCInterceptorHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    @Override
    public CCResult intercept(Chain chain) {
        String componentName = chain.getCC().getComponentName();
        String processName = ComponentManager.getComponentProcessName(componentName);
        return multiProcessCall(chain, processName, CONNECTIONS);
    }

    CCResult multiProcessCall(Chain chain, String processName
            , ConcurrentHashMap<String, IRemoteCCService> connectionCache) {
        if (processName == null) {
            return CCResult.error(CCResult.CODE_ERROR_NO_COMPONENT_FOUND);
        }
        CC cc = chain.getCC();
        //主线程同步调用时，跨进程也要在主线程同步调用
        boolean isMainThreadSyncCall = !cc.isAsync() && Looper.getMainLooper() == Looper.myLooper();
        ProcessCrossTask task = new ProcessCrossTask(cc, processName, connectionCache, isMainThreadSyncCall);
        ComponentManager.threadPool(task);
        if (!cc.isFinished()) {
            //执行 Wait4ResultInterceptor
            chain.proceed();
            //如果是提前结束的，跨进程通知被调用方
            if (cc.isCanceled()) {
                task.cancel();
            } else if (cc.isTimeout()) {
                task.timeout();
            }
        }
        return cc.getResult();
    }

    protected IRemoteCCService getMultiProcessService(String processName) {
        CC.log("start to get RemoteService from process %s", processName);
        IRemoteCCService service = RemoteCCService.get(processName);
        CC.log("get RemoteService from process %s %s!", processName, (service != null ? "success" : "failed"));
        return service;
    }

    class ProcessCrossTask implements Runnable {

        private final CC cc;
        private final String processName;
        private final ConcurrentHashMap<String, IRemoteCCService> connectionCache;
        private final boolean isMainThreadSyncCall;
        private IRemoteCCService service;

        ProcessCrossTask(CC cc, String processName, ConcurrentHashMap<String, IRemoteCCService> connectionCache, boolean isMainThreadSyncCall) {
            this.cc = cc;
            this.processName = processName;
            this.connectionCache = connectionCache;
            this.isMainThreadSyncCall = isMainThreadSyncCall;
        }

        @Override
        public void run() {
            RemoteCC processCrossCC = new RemoteCC(cc, isMainThreadSyncCall);
            call(processCrossCC);
        }

        private void call(RemoteCC remoteCC) {
            try {
                service = connectionCache.get(processName);
                if (service == null) {
                    //获取跨进程通信的binder
                    service = getMultiProcessService(processName);
                    if (service != null) {
                        connectionCache.put(processName, service);
                    }
                }
                if (cc.isFinished()) {
                    CC.verboseLog(cc.getCallId(), "cc is finished before call %s process", processName);
                    return;
                }
                if (service == null) {
                    CC.verboseLog(cc.getCallId(), "RemoteService is not found for process: %s", processName);
                    setResult(CCResult.error(CCResult.CODE_ERROR_NO_COMPONENT_FOUND));
                    return;
                }
                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(cc.getCallId(), "start to call process:%s, RemoteCC: %s"
                            , processName, remoteCC.toString());
                }
                service.call(remoteCC, new IRemoteCallback.Stub() {
                    @Override
                    public void callback(RemoteCCResult remoteCCResult) throws RemoteException {
                        try {
                            if (CC.VERBOSE_LOG) {
                                CC.verboseLog(cc.getCallId(), "receive RemoteCCResult from process:%s, RemoteCCResult: %s"
                                        , processName, remoteCCResult.toString());
                            }
                            setResult(remoteCCResult.toCCResult());
                        } catch(Exception e) {
                            CCUtil.printStackTrace(e);
                            setResult(CCResult.error(CCResult.CODE_ERROR_REMOTE_CC_DELIVERY_FAILED));
                        }
                    }
                });
            } catch (DeadObjectException e) {
                RemoteCCService.remove(processName);
                connectionCache.remove(processName);
                call(remoteCC);
            } catch (Exception e) {
                CCUtil.printStackTrace(e);
                setResult(CCResult.error(CCResult.CODE_ERROR_REMOTE_CC_DELIVERY_FAILED));
            }
        }

        void setResult(CCResult result) {
            cc.setResult4Waiting(result);
        }

        void cancel() {
            try {
                service.cancel(cc.getCallId());
            } catch (Exception e) {
                CCUtil.printStackTrace(e);
            }
        }

        void timeout() {
            try {
                service.timeout(cc.getCallId());
            } catch (Exception e) {
                CCUtil.printStackTrace(e);
            }
        }
    }

}
