package com.billy.cc.demo.component.b.processor;

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
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //仅在未cancel时调用回调
            if (!cc.isCanceled()) {
                CC.sendCCResult(cc.getCallId(), CCResult.success("networkdata", "data from network"));
            }
        }
    }
}
