package com.billy.cc.demo.component.b.processor;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.demo.component.b.UserStateManager;


/**
 * 退出登录
 * @author billy.qi
 */
public class LogoutProcessor implements IActionProcessor {

    @Override
    public String getActionName() {
        return "logout";
    }

    @Override
    public boolean onActionCall(CC cc) {
        UserStateManager.setLoginUser(null);
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        return false;
    }
}
