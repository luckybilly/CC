package com.billy.cc.core.component;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.billy.android.pools.ObjPool;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 组件调用
 * CC = Component Caller
 * @author billy.qi
 */
@SuppressLint("PrivateApi")

public class CC {
    private static final String TAG = "ComponentCaller";
    private static final String VERBOSE_TAG = "CC_VERBOSE";
    /**
     * 默认超时时间为1秒
     */
    private static final long DEFAULT_TIMEOUT = 1000;
    static boolean DEBUG = false;
    static boolean VERBOSE_LOG = false;
    /**
     * 是否响应跨app的组件调用
     * 为了方便开发调试，默认设置为允许响应跨app组件调用
     * 为了安全，app上线时可以将此值设置为false，避免被恶意调用
     */
    static boolean RESPONSE_FOR_REMOTE_CC = true;
    /**
     * 如果调用到当前app内没有的组件，是否尝试去其它app内调用（每人为true）
     */
    static boolean CALL_REMOTE_CC_IF_NEED = true;

    static final ConcurrentHashMap<String, CC> CC_MAP = new ConcurrentHashMap<>();
    private static Application application;

    static {
        try {
            application = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 预留初始化方法
     * 在Application.onCreate(...)中调用
     * @param app 为了防止反射获取application对象失败，预留此初始化功能
     */
    public static void init(Application app) {
        application = app;
    }
    
    private static final ObjPool<Builder, String> BUILDER_POOL = new ObjPool<Builder, String>() {
        @Override
        protected Builder newInstance(String componentName) {
            return new Builder();
        }
    };

    private WeakReference<Context> context;
    /**
     * 组件名称
     */
    private String componentName;
    /**
     * 组件中某个功能的名称，用以区别同一个组件中不同功能的调用
     */
    private String actionName;
    private final Map<String, Object> params = new HashMap<>();
    /**
     * 回调对象
     * 采用弱引用防止内存泄露
     */
    private WeakReference<IComponentCallback> callback;
    /**
     * 是否异步执行
     */
    private boolean async;
    private final List<ICCInterceptor> interceptors = new ArrayList<>();
    private boolean callbackOnMainThread;
    /**
     * 调用超时时间，默认值（同步调用：1000， 异步调用：0）
     */
    private long timeout = -1;
    private String callId;
    private WeakReference<ICaller> caller;
    private AtomicBoolean canceled = new AtomicBoolean(false);
    private AtomicBoolean timeoutStatus = new AtomicBoolean(false);

    private CC(String componentName) {
        this.componentName = componentName;
    }
    
    public static Builder obtainBuilder(String componentName) {
        return BUILDER_POOL.get(componentName);
    }
    public static Application getApplication() {
        return application;
    }

    public static class Builder implements ObjPool.Resetable, ObjPool.Initable<String> {
        private CC cr;
        
        private Builder() {
        }

        public Builder setContext(Context context) {
            if (context != null) {
                cr.context = new WeakReference<>(context);
            }
            return this;
        }

        public Builder setComponentName(String componentName) {
            cr.componentName = componentName;
            return this;
        }

        /**
         * 不限制超时时间
         * @return Builder自身
         */
        public Builder setNoTimeout() {
            return setTimeout(0);
        }

        /**
         * 设置超时时间
         * @param timeout 超时时间限制(ms)
         * @return Builder自身
         */
        public Builder setTimeout(long timeout) {
            if (timeout >= 0) {
                cr.timeout = timeout;
            } else {
                logError("Invalid timeout value:" + timeout
                        + ", timeout should >= 0. timeout will be set as default:"
                        + DEFAULT_TIMEOUT);
            }
            return this;
        }

        public Builder setActionName(String actionName) {
            cr.actionName = actionName;
            return this;
        }

        public Builder setParams(Map<String, Object> params) {
            cr.params.clear();
            return addParams(params);
        }

        public Builder addParams(Map<String, Object> params) {
            if (params != null) {
                for (String key : params.keySet()) {
                    addParam(key, params.get(key));
                }
            }
            return this;
        }

        /**
         * 添加调用参数
         * @param key 参数的key
         * @param value 参数的value
         * @return Builder自身
         */
        public Builder addParam(String key, Object value) {
            cr.params.put(key, value);
            return this;
        }
        /**
         * 添加组件调用前的拦截器
         * @param interceptor 拦截器
         * @return Builder自身
         */
        public Builder addInterceptor(ICCInterceptor interceptor) {
            if (interceptor != null) {
                cr.interceptors.add(interceptor);
            }
            return this;
        }

        public CC build() {
            CC cr = this.cr;
            //回收复用builder
            BUILDER_POOL.put(this);
            if (TextUtils.isEmpty(cr.componentName)) {
                logError("ComponentName is empty:" + cr.toString());
            }
            return cr;
        }

        @Override
        public void reset() {
            this.cr = null;
        }

        @Override
        public void init(String componentName) {
            this.cr = new CC(componentName);
        }
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        put(json, "callId", callId);
        put(json, "context", getContext());
        put(json, "componentName", componentName);
        put(json, "actionName", actionName);
        put(json, "timeout", timeout);
        put(json, "callbackOnMainThread", callbackOnMainThread);
        put(json, "params", CCUtil.convertToJson(params));
        put(json, "interceptors", interceptors);
        put(json, "callback", getCallback());
        return json.toString();
    }

    private void put(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Context getContext() {
        if (context != null) {
            Context context = this.context.get();
            if (context != null) {
                return context;
            }
        }
        return application;
    }


    public String getActionName() {
        return actionName;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public boolean isAsync() {
        return async;
    }

    boolean isCallbackOnMainThread() {
        return callbackOnMainThread;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getCallId() {
        return callId;
    }

    public boolean isCanceled() {
        return canceled.get();
    }

    public boolean isTimeout() {
        return timeoutStatus.get();
    }

    IComponentCallback getCallback() {
        if (callback != null) {
            return callback.get();
        }
        return null;
    }

    String getComponentName() {
        return componentName;
    }

    List<ICCInterceptor> getInterceptors() {
        return interceptors;
    }

    void setCaller(ICaller caller) {
        this.caller = new WeakReference<>(caller);
    }

    /**
     * 异步调用，且不需要回调
     * @return callId，可用于取消调用的任务
     */
    public String callAsync() {
        return callAsync(null);
    }
    /**
     * 异步调用,在异步线程执行回调
     * @param callback 回调函数
     * @return callId 用于取消
     */
    public String callAsync(IComponentCallback callback) {
        this.callbackOnMainThread = false;
        return processCallAsync(callback);
    }
    /**
     * 异步调用,在主线程执行回调
     * @param callback 回调函数
     * @return callId 用于取消
     */
    public String callAsyncCallbackOnMainThread(IComponentCallback callback) {
        this.callbackOnMainThread = true;
        return processCallAsync(callback);
    }

    private String processCallAsync(IComponentCallback callback) {
        if (callback != null) {
            this.callback = new WeakReference<>(callback);
        }
        this.async = true;
        //调用方未设置超时时间，默认为无超时时间
        if (timeout < 0) {
            timeout = 0;
        }
        this.callId = nextCallId();
        this.canceled.set(false);
        this.timeoutStatus.set(false);
        if (VERBOSE_LOG) {
            verboseLog(callId, "start to callAsync:" + this);
        }
        ComponentManager.call(this);
        return callId;
    }

    /**
     * 同步调用
     * @return CCResult
     */
    public CCResult call() {
        this.callback = null;
        this.async = false;
        boolean mainThreadCallWithNoTimeout = timeout == 0 && Looper.getMainLooper() == Looper.myLooper();
        //主线程下的同步调用必须设置超时时间，默认为1秒
        if (mainThreadCallWithNoTimeout || timeout < 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        this.callId = nextCallId();
        this.canceled.set(false);
        this.timeoutStatus.set(false);
        if (VERBOSE_LOG) {
            verboseLog(callId, "start to call:" + this);
        }
        return ComponentManager.call(this);
    }

    /**
     * 取消本组件的调用
     */
    public void cancel() {
        if (!canceled.compareAndSet(false, true)) {
            return;
        }
        if (this.caller != null) {
            ICaller cancelable = this.caller.get();
            if (cancelable != null) {
                if (VERBOSE_LOG) {
                    verboseLog(callId, "call CC.cancel()");
                }
                cancelable.cancel();
            }
        }
    }

    public static void cancel(String callId) {
        CC cc = CC_MAP.get(callId);
        if (cc != null) {
            cc.cancel();
        }
    }
    public void timeout() {
        if (!timeoutStatus.compareAndSet(false, true)) {
            return;
        }
        if (this.caller != null) {
            ICaller caller = this.caller.get();
            if (caller != null) {
                if (VERBOSE_LOG) {
                    verboseLog(callId, "call timeout()");
                }
                caller.timeout();
            }
        }
    }

    public static void timeout(String callId) {
        CC cc = CC_MAP.get(callId);
        if (cc != null) {
            cc.timeout();
        }
    }

    /**
     * 在任意位置回调结果
     * 组件的onCall方法被调用后，<b>必须确保所有分支均会调用</b>到此方法将组件调用结果回调给调用方
     * @param callId 回调对象的调用id
     * @param result 回调的结果
     */
    public static void sendCCResult(String callId, CCResult result) {
        if (VERBOSE_LOG) {
            verboseLog(callId, "CCResult received by CC.sendCCResult(...).CCResult:" + result);
        }
        LocalCCInterceptor localCC = LocalCCInterceptor.RESULT_RECEIVER.get(callId);
        if (localCC != null) {
            if (result == null) {
                logError("CC.sendCCResult called, But result is null. "
                        + "ComponentName=" + localCC.cc.getComponentName());
            }
            localCC.receiveCCResult(result);
        } else {
            log("CCResult received, but cannot found callId:" + callId);
        }
    }

    /**
     * 在任意位置回调结果
     * @param callId 回调对象的调用id
     * @param result 回调的结果
     * @deprecated use {@link #sendCCResult(String, CCResult)}
     */
    @Deprecated
    public static void invokeCallback(String callId, CCResult result) {
        sendCCResult(callId, result);
    }

    /**
     * 获取当前app内是否含有指定的组件
     * @param componentName 组件名称
     * @return true:有， false:没有
     */
    public static boolean hasComponent(String componentName) {
        return ComponentManager.hasComponent(componentName);
    }

    /**
     * 动态注册组件(类似于动态注册广播接收器BroadcastReceiver)
     * @param component 组件对象
     */
    public static void registerComponent(IComponent component) {
        ComponentManager.registerComponent(component);
    }

    /**
     * 动态反注册组件(类似于反注册广播接收器BroadcastReceiver)
     * @param component 组件对象
     */
    public static void unregisterComponent(IComponent component) {
        ComponentManager.unregisterComponent(component);
    }

    private static String prefix;
    private static AtomicInteger index = new AtomicInteger(1);
    private String nextCallId() {
        if (TextUtils.isEmpty(prefix)) {
            Context context = getContext();
            if (context != null) {
                prefix = context.getPackageName() + ":";
            } else {
                prefix = ":::";
            }
        }
        return prefix + index.getAndIncrement();
    }

    static void verboseLog(String callId, String s, Object... args) {
        if (VERBOSE_LOG) {
            s = format(s, args);
            Log.i(CC.VERBOSE_TAG, callId + " >>>> " + s);
        }
    }

    static void log(String s, Object... args) {
        if (DEBUG && application != null) {
            s = format(s, args);
            Log.i(CC.TAG, application.getPackageName() + " ---- " + s);
        }
    }
    static void logError(String s, Object... args) {
        if (DEBUG && application != null) {
            s = format(s, args);
            Log.e(CC.TAG, application.getPackageName() + " ---- " + s);
        }
    }

    private static String format(String s, Object... args) {
        try {
            if (args != null && args.length > 0) {
                s = String.format(s, args);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * 开关组件调用过程详细日志，默认为关闭状态
     * @param enable 开关（true：显示详细日志， false：关闭。）
     */
    public static void enableVerboseLog(boolean enable) {
        VERBOSE_LOG = enable;
    }

    /**
     * 开关debug模式（打印日志），默认为关闭状态
     * @param enable 开关（true：打开debug模式， false：关闭。默认为false）
     */
    public static void enableDebug(boolean enable) {
        DEBUG = enable;
    }
    /**
     * 开关跨app调用组件支持，默认为打开状态
     *  1. 某个componentName当前app中不存在时，是否尝试调用其它app的此组件
     *  2. 接收到跨app调用时，是否执行本次调用
     * @param enable 开关（true：会执行，默认值为true； false：不会）
     */
    public static void enableRemoteCC(boolean enable) {
        RESPONSE_FOR_REMOTE_CC = enable;
        CALL_REMOTE_CC_IF_NEED = enable;
    }
}
