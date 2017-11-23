package com.billy.cc.demo.component.b.processor;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;

/**
 * 定义action的处理类
 * @author billy.qi
 */
public interface IActionProcessor {
    String getActionName();

    /**
     * action的处理类
     * @param cc cc
     * @return 是否异步调用 {@link CC#sendCCResult(String, CCResult)} . true：异步， false：同步调用
     */
    boolean onActionCall(CC cc);
}
