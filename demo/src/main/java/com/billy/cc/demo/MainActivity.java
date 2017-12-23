package com.billy.cc.demo;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.IComponentCallback;

/**
 * @author billy
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String COMPONENT_NAME_A = "demo.ComponentA";

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.console);
        addOnClickListeners(R.id.componentAOpenActivity
                , R.id.test_lifecycle
                , R.id.componentAAsyncOpenActivity
                , R.id.componentAGetData
                , R.id.componentAAsyncGetData
                , R.id.componentB
                , R.id.componentBAsync
                , R.id.componentBGetData
                , R.id.componentBLogin
        );
    }

    private void addOnClickListeners(@IdRes int... ids) {
        if (ids != null) {
            for (@IdRes int id : ids) {
                findViewById(id).setOnClickListener(this);
            }
        }
    }
    CC componentBCC;
    @Override
    public void onClick(View v) {
        textView.setText("");
        CCResult result = null;
        CC cc = null;
        switch (v.getId()) {
            case R.id.test_lifecycle:
                CC.obtainBuilder("demo.lifecycle")
                        .build()
                        .callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.componentAOpenActivity:
                cc = CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("showActivityA")
                        .build();
                result = cc.call();
                break;
            case R.id.componentAAsyncOpenActivity:
                CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("showActivityA")
                        .build().callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.componentAGetData:
                cc = CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("getInfo")
                        .build();
                result = cc.call();
                break;
            case R.id.componentAAsyncGetData:
                CC.obtainBuilder(COMPONENT_NAME_A)
                        .setActionName("getInfo")
                        .addInterceptor(new MissYouInterceptor())
                        .build().callAsyncCallbackOnMainThread(printResultCallback);
                break;
            case R.id.componentB:
                cc = CC.obtainBuilder("ComponentB")
                        .setActionName("showActivity")
                        .build();
                result = cc.call();
                break;
            case R.id.componentBAsync:
                if (componentBCC != null) {
                    componentBCC.cancel();
                    Toast.makeText(this, R.string.canceled, Toast.LENGTH_SHORT).show();
                } else {
                    componentBCC = CC.obtainBuilder("ComponentB")
                            .setActionName("getNetworkData")
                            .build();
                    componentBCC.callAsyncCallbackOnMainThread(new IComponentCallback() {
                        @Override
                        public void onResult(CC cc, CCResult ccResult) {
                            componentBCC = null;
                            showResult(cc, ccResult);
                        }
                    });
                    Toast.makeText(this, R.string.clickToCancel, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.componentBGetData:
                cc = CC.obtainBuilder("ComponentB")
                        .setActionName("getData")
                        .build();
                result = cc.call();
                break;
            case R.id.componentBLogin:
                CC.obtainBuilder("ComponentB")
                        .setActionName("login")
                        .build()
                        .callAsyncCallbackOnMainThread(printResultCallback);
                break;
            default:
                break;
        }
        if (cc != null && result != null) {
            showResult(cc, result);
        }
    }
    IComponentCallback printResultCallback = new IComponentCallback() {
        @Override
        public void onResult(CC cc, CCResult result) {
            showResult(cc, result);
        }
    };
    private void showResult(CC cc, CCResult result) {
        String text = "result:\n" + JsonFormat.format(result.toString());
        text += "\n\n---------------------\n\n";
        text += "cc:\n" + JsonFormat.format(cc.toString());
        textView.setText(text);
    }

    @Override
    protected void onDestroy() {
        if (componentBCC != null) {
            componentBCC.cancel();
        }
        super.onDestroy();
    }
}
