package com.billy.cc.demo.component.b.processor;

import android.util.Log;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;

/**
 * 获取网络数据：模拟发送网络请求
 * @author billy.qi
 */
public class GetNetworkDataProcessor implements IActionProcessor {
    @Override
    public String getActionName() {
        return "getNetworkData";
    }

    @Override
    public boolean onActionCall(CC cc) {
        new GetDataRunnable(cc).start();
        //需要异步调用CC.sendCCResult(...)，返回true
        return true;
    }

    class GetDataRunnable extends Thread {
        CC cc;

        GetDataRunnable(CC cc) {
            this.cc = cc;
        }

        @Override
        public void run() {
            int maxStep = 6;
            int step = 1;
            for (; step <= maxStep; step++) {
                //判断超时或取消状态
                if (cc.isStopped()) {
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //仅在未中止时调用回调
            if (!cc.isStopped()) {
                CC.sendCCResult(cc.getCallId(), CCResult.success("networkdata", "data from network"));
            } else {
                Log.e("ComponentB", "get data from network stopped. step=" + step);
            }
        }
    }
}
