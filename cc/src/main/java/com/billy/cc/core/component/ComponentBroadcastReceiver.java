package com.billy.cc.core.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * app之间的组件调用通信方式：广播
 * @author billy.qi
 */
public class ComponentBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        CC.log("onReceive, packageName=" + context.getPackageName() + "， action=" + action);
        if (!CC.RESPONSE_FOR_REMOTE_CC) {
            CC.log("receive cc, but CC.enableRemoteCC() is set to false in this app");
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        String componentName = extras.getString(RemoteCCInterceptor.KEY_COMPONENT_NAME);
        if (TextUtils.isEmpty(componentName) ||
                !ComponentManager.hasComponent(componentName)) {
            //当前app中不包含此组件，直接返回（其它app会响应该调用）
            return;
        }
        if (CC.VERBOSE_LOG) {
            CC.verboseLog(extras.getString(RemoteCCInterceptor.KEY_CALL_ID)
                , "receive remote cc, start service to perform it.");
        }
        Intent serviceIntent = new Intent(context, ComponentService.class);
        serviceIntent.putExtras(extras);
        context.startService(serviceIntent);
    }
}
