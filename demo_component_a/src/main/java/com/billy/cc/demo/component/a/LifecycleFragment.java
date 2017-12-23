package com.billy.cc.demo.component.a;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.IComponentCallback;


/**
 * @author billy.qi
 * @since 17/12/8 15:30
 */
public class LifecycleFragment extends Fragment {
    static int index = 1;
    private int curIndex;
    private TextView log;
    private TextView textView;

    public LifecycleFragment() {
        curIndex = index++;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("TestFragment", "TestFragment.onCreate:" + curIndex);
    }

    private void log(String s) {
        if (log != null && !TextUtils.isEmpty(s)) {
            log.setText(s);
        }
        Log.i("TestFragment", s);
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = container.getContext();
        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        scrollView.addView(layout);
        layout.setOrientation(LinearLayout.VERTICAL);
        textView = new TextView(context);
        layout.addView(textView);
        textView.setGravity(Gravity.CENTER);
        CC cc = CC.obtainBuilder("ComponentB")
                .setActionName("getNetworkData")
                .cancelOnDestroyWith(this)
                .build();
        cc.callAsyncCallbackOnMainThread(new IComponentCallback() {
                    @Override
                    public void onResult(CC cc, CCResult result) {
                        String text = "callId=" + cc.getCallId() + "\n" + JsonFormat.format(result.toString());
                        Toast.makeText(CC.getApplication(), text, Toast.LENGTH_SHORT).show();
                        log(text);
                    }
                });
        textView.setText(getString(R.string.demo_a_life_cycle_fragment_notice, cc.getCallId()));
        log = new TextView(context);
        
        layout.addView(log);
        return scrollView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        log("TestFragment.onDestroyView:" + curIndex);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("TestFragment.onDestroy:" + curIndex);
    }

    public void addText(final String text) {
        if (textView != null && !TextUtils.isEmpty(text)) {
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.append("\n" + text);
                }
            });
        }
    }
}
