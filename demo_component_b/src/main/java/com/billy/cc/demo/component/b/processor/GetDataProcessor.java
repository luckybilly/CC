package com.billy.cc.demo.component.b.processor;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;

/**
 * 模拟获取内存中的数据（应用场景举例：获取用户中心组件的登录信息）
 * @author billy.qi
 */
public class GetDataProcessor implements IActionProcessor {
    @Override
    public String getActionName() {
        return "getData";
    }

    @Override
    public boolean onActionCall(CC cc) {
        CC.sendCCResult(cc.getCallId(), CCResult.success("data", "data from memory"));
        return false;
    }
}
