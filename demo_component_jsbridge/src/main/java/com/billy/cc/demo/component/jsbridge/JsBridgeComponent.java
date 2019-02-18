package com.billy.cc.demo.component.jsbridge;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.IComponent;
import com.billy.cc.core.component.annotation.AllProcess;
import com.github.lzyzsd.jsbridge.BridgeWebView;

/**
 * jsBridge组件
 * @author billy.qi
 * @since 18/9/15 10:33
 */
@AllProcess
public class JsBridgeComponent implements IComponent {
    @Override
    public String getName() {
        return "jsBridge";
    }

    @Override
    public boolean onCall(CC cc) {
        String actionName = cc.getActionName();
        switch (actionName) {
            case "createWebView":
                //由于JsBridgeComponent添加了@AllProcess注解
                // 在任意进程可以调用此action来创建一个新的面向组件封装的WebView
                return createWebView(cc);
            default:
                CC.sendCCResult(cc.getCallId(), CCResult.errorUnsupportedActionName());
                break;
        }
        return false;
    }

    private boolean createWebView(CC cc) {
        BridgeWebView webView = BridgeWebViewHelper.createWebView(cc.getContext());
        CC.sendCCResult(cc.getCallId(), CCResult.success("webView", webView));
        return false;
    }
}
