package com.billy.cc.core.component;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CC监控系统
 * 每个CC对象有自己的超时时间点，
 * 维护一个待监控的CC列表，当列表不为空时，启动一个线程进行监控
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
            cc.addCancelOnFragmentDestroyIfSet();
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
        return callId == null ? null : CC_MAP.get(callId);
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
                    long millis = minTimeoutAt - SystemClock.elapsedRealtime();
                    if (millis > 0) {
                        synchronized (LOCK) {
                            LOCK.wait(millis);
                        }
                    }
                    //next cc timeout
                    long min = Long.MAX_VALUE;
                    long now = SystemClock.elapsedRealtime();
                    for (CC cc : CC_MAP.values()) {
                        if (!cc.isFinished()) {
                            long timeoutAt = cc.timeoutAt;
                            if (timeoutAt > 0) {
                                if (timeoutAt < now) {
                                    executeTimeout(cc);
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

        /**
         * 执行 timeout()
         * 注意：如果处于程序调试状态和CC.DEBUG是true，
         * 两个都满足情况下，不执行超时 timeout()
         * @param cc
         */
        private void executeTimeout(CC cc) {
            if (!CC.DEBUG) {
                cc.timeout();
                return;
            }
            if (!Debug.isDebuggerConnected()) {
                cc.timeout();
            }
        }
    }

    /**
     * activity lifecycle monitor
     *
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static class ActivityMonitor implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
        @Override public void onActivityStarted(Activity activity) { }
        @Override public void onActivityResumed(Activity activity) { }
        @Override public void onActivityPaused(Activity activity) { }
        @Override public void onActivityStopped(Activity activity) { }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
        @Override public void onActivityDestroyed(Activity activity) {
            Collection<CC> values = CC_MAP.values();
            for (CC cc : values) {
                if (!cc.isFinished() && cc.cancelOnDestroyActivity != null
                        && cc.cancelOnDestroyActivity.get() == activity) {
                    cc.cancelOnDestroy(activity);
                }
            }
        }
    }

    static class FragmentMonitor extends FragmentManager.FragmentLifecycleCallbacks {
        WeakReference<CC> reference;

        FragmentMonitor(CC cc) {
            this.reference = new WeakReference<>(cc);
        }

        @Override
        public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
            if (reference != null) {
                CC cc = reference.get();
                if (cc != null && !cc.isFinished()) {
                    WeakReference<Fragment> fragReference = cc.cancelOnDestroyFragment;
                    if (fragReference != null) {
                        Fragment fragment = fragReference.get();
                        if (f == fragment) {
                            cc.cancelOnDestroy(f);
                        }
                    }
                }
            }
        }
    }
}