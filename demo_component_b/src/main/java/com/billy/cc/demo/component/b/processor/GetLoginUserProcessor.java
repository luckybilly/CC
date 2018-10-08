package com.billy.cc.demo.component.b.processor;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.demo.component.b.UserStateManager;


/**
 * 获取当前登录用户信息
 * @author billy.qi
 */
public class GetLoginUserProcessor implements IActionProcessor {

    @Override
    public String getActionName() {
        return "getLoginUser";
    }

    @Override
    public boolean onActionCall(CC cc) {
        if (UserStateManager.getLoginUser() != null) {
            //already login, return username
            CCResult result = CCResult.success(UserStateManager.KEY_USER, UserStateManager.getLoginUser());
            CC.sendCCResult(cc.getCallId(), result);
        } else {
            CC.sendCCResult(cc.getCallId(), CCResult.error("no login user"));
        }
        return false;
    }
}
