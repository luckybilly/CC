package com.billy.cc.core.component.remote;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author billy.qi
 * @since 18/7/3 22:39
 */
public class RemoteConnection {

    /**
     * 获取当前设备上安装的可供跨app调用组件的App列表
     * @return 包名集合
     */
    public static List<String> scanComponentApps() {
        Application application = CC.getApplication();
        String curPkg = application.getPackageName();
        PackageManager pm = application.getPackageManager();
        // 查询所有已经安装的应用程序
        Intent intent = new Intent("action.com.billy.cc.connection");
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        List<String> packageNames = new ArrayList<>();
        for (ResolveInfo info : list) {
            ActivityInfo activityInfo = info.activityInfo;
            String packageName = activityInfo.packageName;
            if (curPkg.equals(packageName)) {
                continue;
            }
            if (tryWakeup(packageName)) {
                packageNames.add(packageName);
            }
        }
        return packageNames;
    }

    /**
     * 检测组件App是否存在，并顺便唤醒App
     * @param packageName app的包名
     * @return 成功与否（true:app存在，false: 不存在）
     */
    public static boolean tryWakeup(String packageName) {
        long time = SystemClock.elapsedRealtime();
        Intent intent = new Intent();
        intent.setClassName(packageName, RemoteConnectionActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            CC.getApplication().startActivity(intent);
            CC.log("wakeup remote app '%s' success. time=%d", packageName, (SystemClock.elapsedRealtime() - time));
            return true;
        } catch(Exception e) {
            CCUtil.printStackTrace(e);
            CC.log("wakeup remote app '%s' failed. time=%d", packageName, (SystemClock.elapsedRealtime() - time));
            return false;
        }
    }
}
