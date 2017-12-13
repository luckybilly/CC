package com.billy.cc.demo.component.b.processor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.billy.cc.core.component.CC;
import com.billy.cc.demo.component.b.Global;
import com.billy.cc.demo.component.b.LoginActivity;

/**
 * 登录
 * @author billy.qi
 */
public class LoginProcessor implements IActionProcessor {
    @Override
    public String getActionName() {
        return "login";
    }

    @Override
    public boolean onActionCall(CC cc) {
        //clear login user info
        Global.loginUserName = null;
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
