package com.billy.cc.core.component;

import android.app.Service;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IBinder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * app之间的组件调用处理
 * @author billy.qi
 * @since 17/7/2 14:58
 */
public class ComponentService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CC.log("ComponentService.onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CC.log("ComponentService.onDestroy");
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        CC.log("ComponentService.onStartCommand");
        ComponentManager.threadPool(new Processor(intent, startId));
        return super.onStartCommand(intent, flags, startId);
    }

    private class Processor implements Runnable {
        Intent intent;
        int startId;
        LocalSocket socket = null;
        ObjectOutputStream out = null;
        private CC cc;
        private ObjectInputStream in;

        Processor(Intent intent, int startId) {
            this.intent = intent;
            this.startId = startId;
        }

        @Override
        public void run() {
            try{
                process();
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception ignored) {
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                }
            }

            stopSelf(startId);
        }

        private void process() {
            String callId = intent.getStringExtra(RemoteCCInterceptor.KEY_CALL_ID);
            String componentName = intent.getStringExtra(RemoteCCInterceptor.KEY_COMPONENT_NAME);
            String socketName = intent.getStringExtra(RemoteCCInterceptor.KEY_SOCKET_NAME);
            try {
                socket = new LocalSocket();
                socket.connect(new LocalSocketAddress(socketName));
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());
            } catch(Exception e) {
                e.printStackTrace();
            }
            boolean success = socket != null && socket.isConnected();
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "localSocket connect success:" + success);
            }
            if (!success) {
                CC.log("remote component call failed. name:" + socketName);
                return;//建立连接失败，忽略此次需要处理的任务（无法返回结果）
            }
            RemoteCC remoteCC = null;
            try {
                remoteCC = (RemoteCC) in.readObject();
            } catch(Exception e) {
                e.printStackTrace();
            }
            CCResult ccResult;
            if (remoteCC != null) {
                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(callId, "start to perform remote cc.");
                }
                //由于RemoteCCInterceptor中已处理同步/异步调用的逻辑，此处直接同步调用即可
                cc = CC.obtainBuilder(componentName)
                        .setActionName(remoteCC.getActionName())
                        .setParams(remoteCC.getParams())
                        .setTimeout(remoteCC.getTimeout())
                        .withoutGlobalInterceptor() //避免重复调用拦截器
                        .build();
                ComponentManager.threadPool(new ReceiveMsgFromRemoteCaller(cc, in));
                ccResult = cc.call();

                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(callId, "finished perform remote cc.CCResult:" + ccResult);
                }
            } else {
                ccResult = CCResult.error(CCResult.CODE_ERROR_REMOTE_CC_DELIVERY_FAILED);
            }

            if (socket != null && socket.isConnected()) {
                try{
                    //将结果返回给组件调用方
                    out.writeObject(new RemoteCCResult(ccResult));
                    out.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                    CC.log(callId + " remote component call failed. socket send result failed");
                }
            } else {
                CC.log(callId + " remote component call failed. socket is not connected");
            }
        }
    }

    private class ReceiveMsgFromRemoteCaller implements Runnable {
        private CC cc;
        private ObjectInputStream in;

        ReceiveMsgFromRemoteCaller(CC cc, ObjectInputStream in) {
            this.cc = cc;
            this.in = in;
        }

        @Override
        public void run() {
            Object msg;
            try{
                while((msg = in.readObject()) != null) {
                    if (CC.VERBOSE_LOG) {
                        CC.verboseLog(cc.getCallId(), "receive message by localSocket:\"" + msg + "\"");
                    }
                    if (RemoteCCInterceptor.MSG_CANCEL.equals(msg)) {
                        cc.cancel();
                        break;
                    } else if (RemoteCCInterceptor.MSG_TIMEOUT.equals(msg)) {
                        cc.timeout();
                        break;
                    }
                }
            } catch(Exception ignored) {
            }
        }
    }

}
