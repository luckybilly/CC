package com.billy.cc.demo.lifecycle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.IComponent;
import com.billy.cc.core.component.IComponentCallback;

/**
 * demo for AOP <br>
 * login first before startActivity
 * @author billy.qi
 */
public class LifecycleComponent implements IComponent {
    @Override
    public String getName() {
        return "demo.lifecycle";
    }

    @Override
    public boolean onCall(CC cc) {
        checkLoginAndTurnToLifecycle(cc);
        return true;
    }

    private void checkLoginAndTurnToLifecycle(final CC cc) {
        CC.obtainBuilder("ComponentB")
                .setActionName("checkAndLogin")
                .build()
                .callAsync(new IComponentCallback() {
                    @Override
                    public void onResult(CC loginCC, CCResult result) {
                        CCResult ccResult;
                        if (result.isSuccess()) {
                            ccResult = CCResult.success();
                            openLifecycleActivity(cc);
                        } else {
                            ccResult = result;
                        }
                        CC.sendCCResult(cc.getCallId(), ccResult);
                    }
                });
    }

    private void openLifecycleActivity(CC cc) {
        Context context = cc.getContext();
        Intent intent = new Intent(context, LifecycleActivity.class);
        if (!(context instanceof Activity)) {
            // context maybe an application object if caller dose not setContext
            // or call across apps
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra("callId", cc.getCallId());
        context.startActivity(intent);
    }
}
