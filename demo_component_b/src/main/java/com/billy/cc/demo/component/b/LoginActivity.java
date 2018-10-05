package com.billy.cc.demo.component.b;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.CCUtil;
import com.billy.cc.demo.base.bean.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        callId = CCUtil.getNavigateCallId(this);
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
            //仅业务提示，登录操作并未结束
            Toast.makeText(this, R.string.demo_b_username_hint, Toast.LENGTH_SHORT).show();
        } else {
            UserStateManager.setLoginUser(new User(1, username));
            //返回登录结果
            sendLoginResult();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //为确保一定会调用CC.sendCCResult，在onDestroy中再次确认是否已返回登录结果
        sendLoginResult();
    }

    private boolean resultSent;

    private void sendLoginResult() {
        if (resultSent) {
            return;
        }
        resultSent = true;
        //判断是否为CC调用打开本页面
        if (callId != null) {
            CCResult result;
            if (UserStateManager.getLoginUser() == null) {
                result = CCResult.error("login canceled");
            } else {
                //演示跨app传递自定义类型及各种集合类型
                List<User> list = new ArrayList<>();
                list.add(new User(1, "aaa"));
                list.add(new User(3, "ccc"));
                SparseArray<User> userSparseArray = new SparseArray<>();
                userSparseArray.put(1, new User(1, "a"));
                userSparseArray.put(10, new User(10, "a"));
                userSparseArray.put(30, new User(30, "a"));
                User[][] userArray = new User[5][2];
                SparseIntArray sparseIntArray = new SparseIntArray();
                sparseIntArray.put(1, 111);
                sparseIntArray.put(2, 222);
                Map<String, User> map = new HashMap<>();
                map.put("user1", new User(1, "111"));
                map.put("user2", new User(2, "222"));

                result = CCResult.success(UserStateManager.KEY_USER, UserStateManager.getLoginUser()) //User
                        .addData("list", list) // List<User>
                        .addData("nullObject", null) //null
                        .addData("sparseArray", userSparseArray) //SparseArray<User>
                        .addData("sparseIntArray", sparseIntArray) //SparseIntArray/SparseLongArray
                        .addData("user2Array", userArray) // User[][]
                        .addData("untypedArray", list.toArray()) // Object[]
                        .addData("typedArray", list.toArray(new User[]{})) // User[]
                        .addData("map", map) // Map
                ;
            }
            //为确保不管登录成功与否都会调用CC.sendCCResult，在onDestroy方法中调用
            CC.sendCCResult(callId, result);
        }
    }
}
