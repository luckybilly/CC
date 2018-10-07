package com.billy.cc.core.component.remote;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * 用于跨app探索组件及唤醒app的activity
 * @author billy.qi
 * @since 18/7/2 23:38
 */
public class RemoteConnectionActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
