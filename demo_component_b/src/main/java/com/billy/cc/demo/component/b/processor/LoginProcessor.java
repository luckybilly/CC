package com.billy.cc.demo.component.b.processor;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCUtil;
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
        CCUtil.navigateTo(cc, LoginActivity.class);
        //不立即调用CC.sendCCResult,返回true
        return true;
    }
}
