package com.billy.cc.core.component;

import android.app.Service;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IBinder;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

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
        PrintWriter out = null;
        private CC cc;
        private BufferedReader in;

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
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
            String actionName = intent.getStringExtra(RemoteCCInterceptor.KEY_ACTION_NAME);
            String str = intent.getStringExtra(RemoteCCInterceptor.KEY_PARAMS);
            Map<String, Object> params = null;
            if (!TextUtils.isEmpty(str)) {
                try{
                    JSONObject json = new JSONObject(str);
                    params = CCUtil.convertToMap(json);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            long timeout = intent.getLongExtra(RemoteCCInterceptor.KEY_TIMEOUT, 0);
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "start to perform remote cc.");
            }
            //由于RemoteCCInterceptor中已处理同步/异步调用的逻辑，此处直接同步调用即可
            cc = CC.obtainBuilder(componentName)
                    .setActionName(actionName)
                    .setParams(params)
                    .setTimeout(timeout)
                    .build();
            ComponentManager.threadPool(new ReceiveMsgFromRemoteCaller(cc, in));
            CCResult ccResult = cc.call();

            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "finished perform remote cc.CCResult:" + ccResult);
            }

            if (socket != null && socket.isConnected()) {
                try{
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream())), true);
                    //将结果返回给组件调用方
                    out.println(ccResult.toString());
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
        private BufferedReader in;

        ReceiveMsgFromRemoteCaller(CC cc, BufferedReader in) {
            this.cc = cc;
            this.in = in;
        }

        @Override
        public void run() {
            String msg;
            try{
                while((msg = in.readLine()) != null) {
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
