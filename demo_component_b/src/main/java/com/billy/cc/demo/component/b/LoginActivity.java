package com.billy.cc.demo.component.b;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;

/**
 * 模拟登录页面
 * @author billy.qi
 * @since 17/11/23 13:01
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editText;
    private String callId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        callId = intent.getStringExtra("callId");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        editText = new EditText(this);
        editText.setHint(R.string.demo_b_username_hint);
        editText.setText("billy");
        layout.addView(editText, params);
        Button button = new Button(this);
        button.setText(R.string.demo_b_click_login);
        button.setOnClickListener(this);
        layout.addView(button, params);
        setContentView(layout);
    }

    @Override
    public void onClick(View v) {
        String username = editText.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.demo_b_username_hint, Toast.LENGTH_SHORT).show();
        } else {
            Global.loginUserName = username;
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //判断是否为CC调用打开本页面
        if (callId != null) {
            CCResult result;
            if (TextUtils.isEmpty(Global.loginUserName)) {
                result = CCResult.error("login canceled");
            } else {
                result = CCResult.success(Global.KEY_USERNAME, Global.loginUserName);
            }
            //为确保不管登录成功与否都会调用CC.sendCCResult，在onDestroy方法中调用
            CC.sendCCResult(callId, result);
        }
    }
}
