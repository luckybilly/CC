package com.billy.cc.demo.component.jsbridge;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.billy.cc.core.component.CCUtil;
import com.github.lzyzsd.jsbridge.BridgeWebView;

/**
 * 用于demo展示一个网页
 * @author billy.qi
 * @since 18/9/16 12:45
 */
public class WebActivity extends AppCompatActivity {
    public static String EXTRA_URL = "url";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BridgeWebView webView = BridgeWebViewHelper.createWebView(this);
        setContentView(webView);

        String url = CCUtil.getNavigateParam(this, EXTRA_URL, null);
        if (!TextUtils.isEmpty(url)) {
            webView.loadUrl(url);
        }
    }
}
