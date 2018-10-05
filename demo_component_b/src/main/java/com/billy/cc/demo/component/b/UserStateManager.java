package com.billy.cc.demo.component.b;

import android.text.TextUtils;

import com.billy.cc.core.component.CC;
import com.billy.cc.demo.base.bean.User;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * demo：用户登录状态管理
 * @author billy.qi
 */
public class UserStateManager {
    public static final String KEY_USER = "user";

    /** 当前登录用户 */
    private static User loginUser;

    /** 存储当前监听登录状态的所有组件的名称-action键值对 */
    private static final Map<String, String> USER_LOGIN_OBSERVER = new ConcurrentHashMap<>();

    public static boolean addObserver(String componentName, String actionName) {
        if (!TextUtils.isEmpty(componentName)) {
            USER_LOGIN_OBSERVER.put(componentName, actionName);
            //开始监听时，立即返回当前的登录状态
            onUserLoginStateUpdated(componentName, actionName);
            return true;
        }
        return false;
    }

    public static void removeObserver(String componentName) {
        USER_LOGIN_OBSERVER.remove(componentName);
    }

    private static void onUserLoginStateUpdated() {
        //登录状态改变时，立即通知所有监听登录状态的组件
        if (!USER_LOGIN_OBSERVER.isEmpty()) {
            Set<Map.Entry<String, String>> entries = USER_LOGIN_OBSERVER.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                onUserLoginStateUpdated(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void onUserLoginStateUpdated(String componentName, String actionName) {
        CC.obtainBuilder(componentName)
                .setActionName(actionName)
                .addParam(KEY_USER, loginUser)
                .build().callAsync();
    }

    public static void setLoginUser(User user) {
        if (user != loginUser) {
            loginUser = user;
            onUserLoginStateUpdated();
        }
    }

    public static User getLoginUser() {
        return loginUser;
    }

}
