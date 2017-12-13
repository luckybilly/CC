package com.billy.cc.demo.component.b.processor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.demo.component.b.Global;
import com.billy.cc.demo.component.b.LoginActivity;


/**
 * Check user login state:<br>
 * return success if user is already login<br>
 * otherwise start the LoginActivity and return loginResult after login is finished
 * @author billy.qi
 */
public class CheckAndLoginProcessor implements IActionProcessor {

    @Override
    public String getActionName() {
        return "checkAndLogin";
    }

    @Override
    public boolean onActionCall(CC cc) {
        if (!TextUtils.isEmpty(Global.loginUserName)) {
            //already login, return username
            CCResult result = CCResult.success(Global.KEY_USERNAME, Global.loginUserName);
            CC.sendCCResult(cc.getCallId(), result);
            return false;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CC.getApplication(), "please login first!", Toast.LENGTH_SHORT).show();
            }
        });
        Context context = cc.getContext();
        Intent intent = new Intent(context, LoginActivity.class);
        if (!(context instanceof Activity)) {
            //调用方没有设置context或app间组件跳转，context为application
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra("callId", cc.getCallId());
        context.startActivity(intent);
        //不立即调用CC.sendCCResult,返回true
        return true;
    }
}
