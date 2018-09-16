package com.billy.cc.demo.component.jsbridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
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
        Context context = cc.getContext();
        String url = cc.getParamItem("url");
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, url);
        if (!(context instanceof Activity)) {
            //调用方没有设置context或app间组件跳转，context为application
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        return false;
    }
}
