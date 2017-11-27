package com.billy.cc.core.component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CC监控系统
 * 每个CC对象有自己的超时时间点，
 * 维护一个待监控的CC列表，当列表不为空时，启动一个线程进行监控
 * 每10ms循环遍历一次，若列表为空，则退出线程，下次添加监控的时候再启动一个新线程
 * @author billy.qi
 */
class CCMonitor {

    static final ConcurrentHashMap<String, CC> CC_MAP = new ConcurrentHashMap<>();

    private static final AtomicBoolean STOPPED = new AtomicBoolean(true);
    private static volatile long minTimeoutAt = Long.MAX_VALUE;
    private static final byte[] LOCK = new byte[0];

    static void addMonitorFor(CC cc) {
        if (cc != null) {
            CC_MAP.put(cc.getCallId(), cc);
            long timeoutAt = cc.timeoutAt;
            if (timeoutAt > 0) {
                if (minTimeoutAt > timeoutAt) {
                    minTimeoutAt = timeoutAt;
                    //如果最小timeout时间有变化，且监控线程在wait，则唤醒监控线程
                    synchronized (LOCK) {
                        LOCK.notifyAll();
                    }
                }
                if (STOPPED.compareAndSet(true, false)) {
                    new TimeoutMonitorThread().start();
                }
            }
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(cc.getCallId(), "totalCC count=" + CC_MAP.size()
                        + ". add monitor for:" + cc);
            }
        }
    }

    static CC getById(String callId) {
        return CC_MAP.get(callId);
    }

    static void removeById(String callId) {
        CC_MAP.remove(callId);
    }

    /**
     * CC超时监控线程
     */
    private static class TimeoutMonitorThread extends Thread {
        @Override
        public void run() {
            if (STOPPED.get()) {
                return;
            }
            while(CC_MAP.size() > 0 || minTimeoutAt == Long.MAX_VALUE) {
                try {
                    long millis = minTimeoutAt - System.currentTimeMillis();
                    if (millis > 0) {
                        synchronized (LOCK) {
                            LOCK.wait(millis);
                        }
                    }
                    //next cc timeout
                    long min = Long.MAX_VALUE;
                    long now = System.currentTimeMillis();
                    for (CC cc : CC_MAP.values()) {
                        if (!cc.isFinished()) {
                            long timeoutAt = cc.timeoutAt;
                            if (timeoutAt > 0) {
                                if (timeoutAt < now) {
                                    cc.timeout();
                                } else if (timeoutAt < min) {
                                    min = timeoutAt;
                                }
                            }
                        }
                    }
                    minTimeoutAt = min;
                } catch (InterruptedException ignored) {
                }
            }
            STOPPED.set(true);
        }
    }

}
