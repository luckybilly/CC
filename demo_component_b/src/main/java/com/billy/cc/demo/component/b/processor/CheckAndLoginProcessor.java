package com.billy.cc.demo.component.b.processor;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.CCUtil;
import com.billy.cc.demo.component.b.LoginActivity;
import com.billy.cc.demo.component.b.UserStateManager;


/**
 * Check user login state:<br>
 * return success if user is already login<br>
 * otherwise start the LoginActivity and return loginResult after login is finished
 * @author billy.qi
 */
public class CheckAndLoginProcessor implements IActionProcessor {

    @Override
    public String getActionName() {
        return "checkAndLogin";
    }

    @Override
    public boolean onActionCall(CC cc) {
        if (UserStateManager.getLoginUser() != null) {
            //already login, return username
            CCResult result = CCResult.success(UserStateManager.KEY_USER, UserStateManager.getLoginUser());
            CC.sendCCResult(cc.getCallId(), result);
            return false;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CC.getApplication(), "please login first!", Toast.LENGTH_SHORT).show();
            }
        });
        CCUtil.navigateTo(cc, LoginActivity.class);
        //不立即调用CC.sendCCResult,返回true
        return true;
    }
}
