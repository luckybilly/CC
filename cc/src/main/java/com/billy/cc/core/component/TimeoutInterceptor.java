package com.billy.cc.core.component;

/**
 * 超时检测拦截器
 * @author billy.qi
 */
class TimeoutInterceptor implements ICCInterceptor {

    private CCResult result;
    private CC cc;

    @Override
    public CCResult intercept(Chain chain) {
        this.cc = chain.getCC();
        if (cc.getTimeout() > 0) {
            new CheckTimeoutTask().start();
        }
        result = chain.proceed();
        return result;
    }


    /**
     * 超时检测
     */
    private class CheckTimeoutTask extends Thread {
        private long startTime = System.currentTimeMillis();

        @Override
        public void run() {
            while(result == null && System.currentTimeMillis() - startTime < cc.getTimeout()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            if (result == null) {
                if (CC.VERBOSE_LOG) {
                    CC.verboseLog(cc.getCallId(), "time is out, timeout="
                            + cc.getTimeout() + " ms");
                }
                CC.timeout(cc.getCallId());
            }
        }
    }
}
