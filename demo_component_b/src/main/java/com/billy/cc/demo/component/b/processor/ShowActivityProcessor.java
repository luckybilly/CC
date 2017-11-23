package com.billy.cc.demo.component.b.processor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.demo.component.b.ActivityB;

/**
 * 页面跳转
 * @author billy.qi
 */
public class ShowActivityProcessor implements IActionProcessor {
    @Override
    public String getActionName() {
        return "showActivity";
    }

    @Override
    public boolean onActionCall(CC cc) {
        Context context = cc.getContext();
        Intent intent = new Intent(context, ActivityB.class);
        if (!(context instanceof Activity)) {
            //调用方没有设置context或app间组件跳转，context为application
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        //需要确保每个
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        return false;
    }
}
