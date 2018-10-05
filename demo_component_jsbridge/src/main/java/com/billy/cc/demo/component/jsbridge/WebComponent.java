package com.billy.cc.demo.component.jsbridge;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.CCUtil;
import com.billy.cc.core.component.IComponent;
import com.billy.cc.core.component.annotation.SubProcess;

/**
 * @author billy.qi
 * @since 18/9/16 13:26
 */
@SubProcess(":web")
public class WebComponent implements IComponent {
    @Override
    public String getName() {
        return "webComponent";
    }

    @Override
    public boolean onCall(CC cc) {
        String actionName = cc.getActionName();
        switch (actionName) {
            case "openUrl":
                return openUrl(cc);
            default:
                CC.sendCCResult(cc.getCallId(), CCResult.error("unsupported action:" + actionName));
                break;
        }

        return false;
    }

    private boolean openUrl(CC cc) {
        CCUtil.navigateTo(cc, WebActivity.class);
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        return false;
    }
}
