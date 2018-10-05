package com.billy.cc.demo.component.b.processor;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.demo.component.b.UserStateManager;

/**
 * 删除监听登录状态的动态组件
 * @author billy.qi
 */
public class LoginObserverRemoveProcessor implements IActionProcessor {
    @Override
    public String getActionName() {
        return "removeLoginObserver";
    }

    @Override
    public boolean onActionCall(CC cc) {
        String dynamicComponentName = cc.getParamItem("componentName");
        UserStateManager.removeObserver(dynamicComponentName);
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        return false;
    }
}
