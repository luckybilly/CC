package debug.jsbridge;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.demo.component.jsbridge.R;

/**
 * jsBridge组件的开发调试页面
 * @author billy.qi
 * @since 18/9/15 10:48
 */
public class DebugWebActivity extends AppCompatActivity {

    private EditText urlEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_jsbridge_demo_activity);
        urlEditText = (EditText) findViewById(R.id.et_url);
        findViewById(R.id.btn_load).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlEditText.getText().toString().trim();
                if (TextUtils.isEmpty(url)) {
                    Toast.makeText(DebugWebActivity.this, "please input url!", Toast.LENGTH_SHORT).show();
                } else {
                    CC.obtainBuilder("webComponent")
                            .setActionName("openUrl")
                            .setContext(DebugWebActivity.this)
                            .addParam("url", url)
                            .build().call();
                }
            }
        });
    }
}
