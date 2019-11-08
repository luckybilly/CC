package com.billy.cc.core.component;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import com.billy.android.pools.ObjPool;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.billy.cc.core.component.CCUtil.put;
import static com.billy.cc.core.component.ComponentManager.ACTION_REGISTER;
import static com.billy.cc.core.component.ComponentManager.ACTION_UNREGISTER;
import static com.billy.cc.core.component.ComponentManager.COMPONENT_DYNAMIC_COMPONENT_OPTION;
import static com.billy.cc.core.component.ComponentManager.KEY_COMPONENT_NAME;
import static com.billy.cc.core.component.ComponentManager.KEY_PROCESS_NAME;

/**
 * 组件调用
 * CC = Component Caller
 * @author billy.qi
 */
@SuppressLint("PrivateApi")

public class CC {
    private static final String TAG = "ComponentCaller";
    private static final String VERBOSE_TAG = "ComponentCaller_VERBOSE";
    public static final String CC_NULL_KEY = "CC_NULL_KEY";
    /**
     * 默认超时时间为2秒
     */
    private static final long DEFAULT_TIMEOUT = 2000;
    static boolean DEBUG = false;
    static boolean VERBOSE_LOG = false;
    /**
     * 是否响应跨app的组件调用
     * 为了方便开发调试，默认设置为允许响应跨app组件调用
     * 为了安全，app上线时可以将此值设置为false，避免被恶意调用
     */
    private static boolean REMOTE_CC_ENABLED = false;

    private volatile CCResult result;

    private final byte[] wait4resultLock = new byte[0];

    private static Application application;

    WeakReference<Activity> cancelOnDestroyActivity;

    WeakReference<Fragment> cancelOnDestroyFragment;

    private volatile boolean waiting;

    static {
        Application app = CCUtil.initApplication();
        if (app != null) {
            init(app);
        }
    }

    /**
     * 初始化方法
     * 仅初始化CC，不初始化组件和拦截器
     * 在Application.onCreate(...)中调用
     * @param app Application
     */
    public static synchronized void init(Application app) {
        init(app, false, false);
    }

    /**
     * 初始化方法
     * 同时初始化组件和全局拦截器
     * @param app Application
     * @param initComponents 如果设置为true则同时初始化组件
     * @param initGlobalInterceptors 如果设置为true则同时初始化全局拦截器
     */
    public static synchronized void init(Application app, boolean initComponents, boolean initGlobalInterceptors) {
        if (application == null && app != null) {
            application = app;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                application.registerActivityLifecycleCallbacks(new CCMonitor.ActivityMonitor());
            }
        }
        if (initComponents) {
            ComponentManager.init();
        }
        if (initGlobalInterceptors) {
            GlobalCCInterceptorManager.init();
        }
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
     */
    private IComponentCallback callback;
    /**
     * 是否异步执行
     */
    private boolean async;
    private final List<ICCInterceptor> interceptors = new ArrayList<>();
    private boolean callbackOnMainThread;
    /**
     * 调用超时时间，默认值（同步调用：2000， 异步调用：0）
     */
    private long timeout = -1;
    long timeoutAt;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private String callId;
    private volatile boolean canceled = false;
    private volatile boolean timeoutStatus = false;
    private boolean withoutGlobalInterceptor = false;

    private CC(String componentName) {
        this.componentName = componentName;
    }

    /**
     * 创建CC对象的Builder<br>
     * <b>此对象会被CC框架复用，请勿在程序中保存</b>
     * @param componentName 要调用的组件名称
     * @return 创建CC对象的Builder
     */
    public static Builder obtainBuilder(String componentName) {
        return BUILDER_POOL.get(componentName);
    }

    /**
     * 获取当前app的Application对象
     * @return application对象
     */
    public static Application getApplication() {
        return application;
    }

    /**
     * CC的Builder,支持链式调用<br>
     * 最终通过调用{@link Builder#build()}方法获取CC对象
     * <b>此对象会被CC框架复用，请勿在程序中保存</b>
     */
    public static class Builder implements ObjPool.Resetable, ObjPool.Initable<String> {
        private CC cr;
        
        private Builder() { }

        /**
         * 设置context
         * @param context 设置为null时无效<br>
         *                调用同一个app的组件时，组件实现方通过cc.getContext()将返回此值<br>
         *                调用其它app的组件时，组件实现方通过cc.getContext()将返回组件所在app的Application对象
         * @return Builder自身
         */
        public Builder setContext(Context context) {
            if (context != null) {
                cr.context = new WeakReference<>(context);
            }
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

        /**
         * 用于调取同一个组件的不同action（可以理解为分组的概念：将不同的action分组在一个组件里对外提供服务）
         * @param actionName action的名称，组件在执行时可根据此值执行不同的动作，返回不同的信息
         * @return Builder自身
         */
        public Builder setActionName(String actionName) {
            cr.actionName = actionName;
            return this;
        }

        public Builder withoutGlobalInterceptor() {
            cr.withoutGlobalInterceptor = true;
            return this;
        }

        /**
         * 设置组件调用的参数（将清空之前设置的参数列表）
         * @param params 参数 {@link Map} 类型
         * @return Builder自身
         */
        public Builder setParams(Map<String, Object> params) {
            cr.params.clear();
            return addParams(params);
        }
        /**
         * 设置组件调用的参数（使用默认Key）<br>
         * 适用于只有单个参数时省略key的情况下使用 <br>
         * （与其它参数不冲突，后续若有其它参数仍然通过addParam添加即可）
         * @param param 参数
         * @return Builder自身
         */
        public Builder setParamWithNoKey(Object param) {
            return addParam(CC_NULL_KEY, param);
        }

        /**
         * 向组件调用的参数列表中添加参数
         * @param params 参数 {@link Map} 类型
         * @return Builder自身
         */
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
        /**
         * 设置activity.onDestroy时自动cancel
         * @param activity 监控此activity的生命周期，在onDestroy方法被调用后若cc未执行完则自动cancel
         * @return Builder自身
         */
        public Builder cancelOnDestroyWith(Activity activity) {
            if (activity != null) {
                cr.cancelOnDestroyActivity = new WeakReference<>(activity);
            }
            return this;
        }

        /**
         * 设置fragment.onDestroy时自动cancel
         * @param fragment 监控此fragment的生命周期，在onDestroy方法被调用后若cc未执行完则自动cancel
         * @return Builder自身
         */
        public Builder cancelOnDestroyWith(Fragment fragment) {
            if (fragment != null) {
                cr.cancelOnDestroyFragment = new WeakReference<>(fragment);
            }
            return this;
        }

        /**
         * 直接设置callId
         * 在跨进程调用时供CC库内部使用，保持跨进程调用的callId一致
         * 便于跟踪调用流程和cancel、timeout
         * @param callId 原callId
         * @return Builder自身
         */
        Builder setCallId(String callId) {
            if (!TextUtils.isEmpty(callId)) {
                cr.callId = callId;
            }
            return this;
        }

        /**
         * 构建CC对象
         * @return CC对象
         */
        public CC build() {
            CC cr = this.cr;
            //回收复用builder
            BUILDER_POOL.put(this);
            if (TextUtils.isEmpty(cr.componentName)) {
                logError("ComponentName is empty:" + cr.toString());
            }
            return cr;
        }

        /**
         * 用于Builder对象池对此对象的重置
         */
        @Override
        public void reset() {
            this.cr = null;
        }

        /**
         * 用于Builder对象池对此对象的初始化
         * @param componentName 组件名称
         */
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
        put(json, "withoutGlobalInterceptor", withoutGlobalInterceptor);
        put(json, "callbackOnMainThread", callbackOnMainThread);
        put(json, "params", CCUtil.convertToJson(params));
        put(json, "interceptors", interceptors);
        put(json, "callback", getCallback());
        return json.toString();
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

    void forwardTo(String componentName) {
        this.componentName = componentName;
    }

    public String getActionName() {
        return actionName;
    }

    /**
     * get all params
     * @return all params as map
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * 获取通过 {@link Builder#setParamWithNoKey(Object)} 设置的参数
     * @param defaultValue 默认值
     * @param <T> 泛型，返回值的类型
     * @return 未设置key（使用默认key）的参数
     */
    public <T> T getParamItemWithNoKey(T defaultValue) {
        return getParamItem(CC_NULL_KEY, defaultValue);
    }

    /**
     * 获取通过 {@link Builder#setParamWithNoKey(Object)} 设置的参数
     * @param <T> 泛型，返回值的类型
     * @return 未设置key（使用默认key）的参数
     */
    public <T> T getParamItemWithNoKey() {
        return getParamItem(CC_NULL_KEY);
    }

    /**
     * get param(auto class casted) by key
     * @param key key for param
     * @param defaultValue default value if not found or class cast error
     * @param <T> class to cast for param
     * @return class casted param
     */
    public <T> T getParamItem(String key, T defaultValue) {
        T item = getParamItem(key);
        if (item == null) {
            return defaultValue;
        }
        return item;
    }

    /**
     * get param(auto class casted) by key
     * @param key key for param
     * @param <T> class to cast for param
     * @return class casted param
     */
    public <T> T getParamItem(String key) {
        try {
            return (T) params.get(key);
        } catch(Exception e) {
            CCUtil.printStackTrace(e);
        }
        return null;
    }

    boolean isAsync() {
        return async;
    }

    boolean isCallbackOnMainThread() {
        return callbackOnMainThread;
    }

    long getTimeout() {
        return timeout;
    }

    public String getCallId() {
        return callId;
    }

    boolean isCanceled() {
        return canceled;
    }

    /**
     * 判断是否需要中止运行，本次调用被手动取消或已超时。
     * 组件在处理耗时操作时，要根据此状态进行判断，以免进行无效操作
     * @return <code>true</code>:需要中止继续执行；false:可以继续运行
     */
    public boolean isStopped() {
        return canceled || timeoutStatus;
    }

    boolean isTimeout() {
        return timeoutStatus;
    }

    boolean isWithoutGlobalInterceptor() {
        return withoutGlobalInterceptor;
    }

    CCResult getResult() {
        return result;
    }

    void setResult(CCResult result) {
        finished.set(true);
        this.result = result;
    }

    void setResult4Waiting(CCResult result) {
        try {
            synchronized (wait4resultLock) {
                if (VERBOSE_LOG) {
                    verboseLog(callId, "setResult" + (waiting ? "4Waiting" : "")
                            + ". CCResult:" + result);
                }
                setResult(result);
                if (waiting) {
                    waiting = false;
                    wait4resultLock.notifyAll();
                }
            }
        } catch(Exception e) {
            CCUtil.printStackTrace(e);
        }
    }

    void wait4Result() {
        //等待调用CC.sendCCResult(callId, result)
        synchronized (wait4resultLock) {
            if (!isFinished()) {
                try {
                    verboseLog(callId, "start waiting for CC.sendCCResult(...)");
                    waiting = true;
                    wait4resultLock.wait();
                    verboseLog(callId, "end waiting for CC.sendCCResult(...)");
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    IComponentCallback getCallback() {
        return callback;
    }

    /**
     * 在onDestroy后，自动cancel
     */
    void cancelOnDestroy(Object reason) {
        if (!isFinished()) {
            if (VERBOSE_LOG) {
                verboseLog(callId, "call cancel on " + reason + " destroyed");
            }
            cancel();
        }
    }

    void addCancelOnFragmentDestroyIfSet() {
        if (cancelOnDestroyFragment == null) {
            return;
        }
        Fragment fragment = cancelOnDestroyFragment.get();
        if (fragment == null) {
            return;
        }
        FragmentManager manager = fragment.getFragmentManager();
        if (manager != null) {
            manager.registerFragmentLifecycleCallbacks(
                    new CCMonitor.FragmentMonitor(this)
                    , false);
        }
    }

    public String getComponentName() {
        return componentName;
    }

    List<ICCInterceptor> getInterceptors() {
        return interceptors;
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
            this.callback = callback;
        }
        this.async = true;
        //调用方未设置超时时间，默认为无超时时间
        if (timeout < 0) {
            timeout = 0;
        }
        setTimeoutAt();
        this.callId = nextCallId();
        this.canceled = false;
        this.timeoutStatus = false;
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
        //主线程下的同步调用必须设置超时时间，默认为2秒
        if (mainThreadCallWithNoTimeout || timeout < 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        setTimeoutAt();
        this.callId = nextCallId();
        this.canceled = false;
        this.timeoutStatus = false;
        //加上开关判断，防止开关关闭的情况下也执行this.toString()方法
        if (VERBOSE_LOG) {
            verboseLog(callId, "start to call:" + this);
        }
        return ComponentManager.call(this);
    }

    /**
     * 设定超时
     */
    private void setTimeoutAt() {
        if (timeout > 0) {
            timeoutAt = SystemClock.elapsedRealtime() + timeout;
        } else {
            timeoutAt = 0;
        }
    }

    /**
     * 取消本组件的调用
     */
    public void cancel() {
        if (markFinished()) {
            canceled = true;
            setResult4Waiting(CCResult.error(CCResult.CODE_ERROR_CANCELED));
            verboseLog(callId, "call cancel()");
        } else {
            verboseLog(callId, "call cancel(). but this cc is already finished");
        }
    }

    boolean isFinished() {
        return finished.get();
    }

    private boolean markFinished() {
        return finished.compareAndSet(false, true);
    }

    /**
     * 取消正在执行的CC调用
     * 要用此功能时，组件实现方需要在执行过程中调用<code>cc.isStopped()</code>以判断是否中止执行
     * 可参考 demo_component_b 中的 GetNetworkDataProcessor
     * @param callId 组件调用的Id
     */
    public static void cancel(String callId) {
        verboseLog(callId, "call CC.cancel()");
        CC cc = CCMonitor.getById(callId);
        if (cc != null) {
            cc.cancel();
        }
    }
    static void timeout(String callId) {
        verboseLog(callId, "call CC.cancel()");
        CC cc = CCMonitor.getById(callId);
        if (cc != null) {
            cc.timeout();
        }
    }
    void timeout() {
        if (markFinished()) {
            timeoutStatus = true;
            setResult4Waiting(CCResult.error(CCResult.CODE_ERROR_TIMEOUT));
            verboseLog(callId, "timeout");
        } else {
            verboseLog(callId, "call timeout(). but this cc is already finished");
        }
    }

    /**
     * 同步调用或设置了回调，则表示调用方需要获得结果
     * @return 是否需要拿到组件调用结果
     */
    public boolean resultRequired() {
        return !async || callback != null;
    }

    /**
     * 在任意位置回调结果
     * 组件的onCall方法被调用后，<b>必须确保所有分支均会调用</b>到此方法将组件调用结果回调给调用方
     * @param callId 回调对象的调用id
     * @param ccResult 回调的结果
     */
    public static void sendCCResult(String callId, CCResult ccResult) {
        if (VERBOSE_LOG) {
            verboseLog(callId, "CCResult received by CC.sendCCResult(...).CCResult:" + ccResult);
        }
        CC cc = CCMonitor.getById(callId);
        if (cc != null) {
            if (cc.markFinished()) {
                if (ccResult == null) {
                    ccResult = CCResult.defaultNullResult();
                    logError("CC.sendCCResult called, But ccResult is null, set it to CCResult.defaultNullResult(). "
                            + "ComponentName=" + cc.getComponentName());
                }
                cc.setResult4Waiting(ccResult);
            } else {
                logError("CC.sendCCResult called, But ccResult is null. "
                        + "ComponentName=" + cc.getComponentName());
            }
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
    public static void registerComponent(IDynamicComponent component) {
        if (component == null) {
            return;
        }
        ComponentManager.registerComponent(component);
        //子进程中注册的动态组件要通知主进程
        //动态组件被注册在当前进程中
        String curProcessName = CCUtil.getCurProcessName();
        if (!isMainProcess()) {
            CC.obtainBuilder(COMPONENT_DYNAMIC_COMPONENT_OPTION)
                    .setActionName(ACTION_REGISTER)
                    .addParam(KEY_COMPONENT_NAME, component.getName())
                    .addParam(KEY_PROCESS_NAME, curProcessName)
                    .build().callAsync();
        }
    }

    /**
     * 动态反注册组件(类似于反注册广播接收器BroadcastReceiver)
     * @param component 组件对象
     */
    public static void unregisterComponent(IDynamicComponent component) {
        if (component == null) {
            return;
        }
        ComponentManager.unregisterComponent(component);
        //子进程中注销的动态组件要通知主进程
        if (!isMainProcess()) {
            CC.obtainBuilder(COMPONENT_DYNAMIC_COMPONENT_OPTION)
                    .setActionName(ACTION_UNREGISTER)
                    .addParam(KEY_COMPONENT_NAME, component.getName())
                    .build().callAsync();
        }
    }

    /**
     * 注册一个全局拦截器
     * 每个拦截器类型只能注册一次
     * {@link IGlobalCCInterceptor}接口的直接实现类默认会自动注册，无需手动添加
     * 若不需要自动注册，可使用子接口的方式来定义
     * @param interceptor 全局拦截器
     */
    public static void registerGlobalInterceptor(IGlobalCCInterceptor interceptor) {
        GlobalCCInterceptorManager.registerGlobalInterceptor(interceptor);
    }

    /**
     * 注销一个全局拦截器
     * @param clazz 拦截器的类型
     */
    public static void unregisterGlobalInterceptor(Class<? extends IGlobalCCInterceptor> clazz) {
        GlobalCCInterceptorManager.unregisterGlobalInterceptor(clazz);
    }

    private static String prefix;
    private static AtomicInteger index = new AtomicInteger(1);
    private String nextCallId() {
        //为了保持跨进程调用时callId一致
        //如果已设置callId，则不额外生成
        if (!TextUtils.isEmpty(callId)) {
            return callId;
        }
        if (TextUtils.isEmpty(prefix)) {
            String curProcessName = CCUtil.getCurProcessName();
            if (!TextUtils.isEmpty(curProcessName)) {
                prefix = curProcessName + ":";
            } else {
                return ":::" + index.getAndIncrement();
            }
        }
        return prefix + index.getAndIncrement();
    }

    static void verboseLog(String callId, String s, Object... args) {
        if (VERBOSE_LOG) {
            s = format(s, args);
            Log.i(CC.VERBOSE_TAG, "(" + CCUtil.getCurProcessName() +")(" + Thread.currentThread().getName() + ")"
                    + callId + " >>>> " + s);
        }
    }

    public static void log(String s, Object... args) {
        if (DEBUG) {
            s = format(s, args);
            Log.i(CC.TAG, s);
        }
    }
    static void logError(String s, Object... args) {
        if (DEBUG) {
            s = format(s, args);
            Log.e(CC.TAG, s);
        }
    }

    private static String format(String s, Object... args) {
        try {
            if (args != null && args.length > 0) {
                s = String.format(s, args);
            }
        } catch(Exception e) {
            CCUtil.printStackTrace(e);
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
     * 开关跨app调用组件支持，默认为关闭状态
     *  1. 某个componentName当前app中不存在时，是否尝试调用其它app的此组件
     *  2. 接收到跨app调用时，是否执行本次调用
     *  3. 建议仅在开发阶段调试时打开，正式发布时关闭
     * @param enable 开关（true：会执行； false：不会）
     */
    public static void enableRemoteCC(boolean enable) {
        REMOTE_CC_ENABLED = enable;
        if (enable && application != null) {
            RemoteCCInterceptor.getInstance().enableRemoteCC();
        }
    }

    public static boolean isRemoteCCEnabled() {
        return REMOTE_CC_ENABLED;
    }

    /**
     * 当前进程是否以包名在运行（当前进程是否为主进程）
     */
    public static boolean isMainProcess(){
        return CCUtil.isMainProcess();
    }

    public static boolean isDebugMode() {
        return DEBUG;
    }
}
