package com.billy.cc.demo.component.b.processor;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.demo.component.b.UserStateManager;

/**
 * 添加监听登录状态的动态组件
 * @author billy.qi
 */
public class LoginObserverAddProcessor implements IActionProcessor {
    @Override
    public String getActionName() {
        return "addLoginObserver";
    }

    @Override
    public boolean onActionCall(CC cc) {
        String dynamicComponentName = cc.getParamItem("componentName");
        String dynamicActionName = cc.getParamItem("actionName");
        boolean success = UserStateManager.addObserver(dynamicComponentName, dynamicActionName);
        CCResult result = success ? CCResult.success() : CCResult.error("");
        CC.sendCCResult(cc.getCallId(), result);
        return false;
    }
}
