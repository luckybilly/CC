package com.billy.cc.demo.component.jsbridge;

import android.content.Context;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.CCUtil;
import com.billy.cc.core.component.IComponentCallback;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;

import org.json.JSONObject;

import java.util.Map;

/**
 * 面向组件封装WebView
 * @author billy.qi
 * @since 18/9/15 12:10
 */
public class BridgeWebViewHelper {

    static BridgeWebView createWebView(Context context) {

        final BridgeWebView webView = new BridgeWebView(context);
        webView.registerHandler("callCCComponent", new BridgeHandler() {
            @Override
            public void handler(String data, final CallBackFunction function) {
                JSONObject json = null;
                try {
                    json = new JSONObject(data);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                if (json == null) {
                    CCResult result = CCResult.error("json parse error");
                    function.onCallBack(result.toString());
                } else {
                    String componentName = json.optString("componentName");
                    String actionName = json.optString("actionName");
                    Map<String, Object> map = null;
                    JSONObject dataJson = json.optJSONObject("data");
                    if (dataJson != null) {
                        map = CCUtil.convertToMap(dataJson);
                    }
                    CC.obtainBuilder(componentName)
                            .setActionName(actionName)
                            .setParams(map)
                            .setContext(webView.getContext())
                            .build().callAsyncCallbackOnMainThread(new IComponentCallback() {
                        @Override
                        public void onResult(CC cc, CCResult result) {
                            function.onCallBack(result.toString());
                        }
                    });
                }
            }
        });
        return webView;
    }
}
