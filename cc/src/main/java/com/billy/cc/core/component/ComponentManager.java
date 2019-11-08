package com.billy.cc.core.component;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.billy.cc.core.component.annotation.AllProcess;
import com.billy.cc.core.component.annotation.SubProcess;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.billy.cc.core.component.GlobalCCInterceptorManager.INTERCEPTORS;

/**
 * 组件调用管理类
 * @author billy.qi
 * @since 17/6/28 20:14
 */
class ComponentManager {
    /** 当前进程中的组件集合 */
    private static final ConcurrentHashMap<String, IComponent> COMPONENTS = new ConcurrentHashMap<>();
    /**
     * 组件名称对应的进程名称集合
     * 当前进程为主进程：包含当前app内的所有静态组件和动态组件的（名称 - 进程名）的映射表
     * 当前进程为子进程：包含当前app内的所有静态组件和当前进程内注册的动态组件的（名称 - 进程名）的映射表
     */
    private static final ConcurrentHashMap<String, String> COMPONENT_PROCESS_NAMES = new ConcurrentHashMap<>();
    private static final String SUB_PROCESS_SEPARATOR = ":";
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("cc-pool-" + thread.getId());
            return thread;
        }
    };
    static final ExecutorService CC_THREAD_POOL = new ThreadPoolExecutor(2, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), THREAD_FACTORY);

    static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    static {
        registerComponent(new DynamicComponentOption());
        //加载类时自动调用初始化：注册所有组件
        //通过auto-register插件生成组件注册代码
        //生成的代码如下:
        //registerComponent(new ComponentA());
        //registerComponent(new ComponentAA());
    }

    /**
     * 提前初始化所有全局拦截器
     */
    static void init(){
        //调用此方法时，虚拟机会加载ComponentManager类
        //会自动执行static块中的组件自动注册，调用组件类的无参构造方法
        //如果不提前调用此方法，static块中的代码将在第一次进行组件调用时(cc.callXxx())执行
    }

    /**
     * 注册组件
     */
    static void registerComponent(IComponent component) {
        if (component != null) {
            try{
                String name = component.getName();
                if (TextUtils.isEmpty(name)) {
                    CC.logError("component " + component.getClass().getName()
                            + " register with an empty name. abort this component.");
                } else {
                    String processName = getComponentProcessName(component.getClass());
                    COMPONENT_PROCESS_NAMES.put(name, processName);
                    if (!processName.equals(CCUtil.getCurProcessName())) {
                        return;
                    }
                    IComponent oldComponent = COMPONENTS.put(name, component);

                    if (oldComponent != null) {
                        CC.logError( "component (" + component.getClass().getName()
                                + ") with name:" + name
                                + " has already exists, replaced:" + oldComponent.getClass().getName());
                    } else if (CC.DEBUG) {
                        CC.log("register component success! component name = '"
                                + name + "', class = " + component.getClass().getName());
                    }
                }
            } catch(Exception e) {
                CCUtil.printStackTrace(e);
            }
        }
    }

    /**
     * 获取组件的进程名称<br>
     * 注：由于动态组件返回的是当前进程名称，此方法仅适用于在组件注册时使用
     * @param componentClass 组件类
     * @return 组件所在进程名称
     */
    private static String getComponentProcessName(Class<? extends IComponent> componentClass) {
        if (IDynamicComponent.class.isAssignableFrom(componentClass)) {
            //动态组件只注册在当前进程内，其进程名称与当前进程相同
            return CCUtil.getCurProcessName();
        }
        String packageName = CC.getApplication().getPackageName();
        //TODO 尚未兼容：app的默认进程名称有可能不是包名
        // 通过在application节点添加android:process="a.b.c"可以指定默认进程名称
        String defaultProcessName = packageName;
        AllProcess allProcess = componentClass.getAnnotation(AllProcess.class);
        if (allProcess != null) {
            return CCUtil.getCurProcessName();
        }
        String processName;
        SubProcess subProcess = componentClass.getAnnotation(SubProcess.class);
        if (subProcess != null) {
            //读取注解中的进程名称
            processName = subProcess.value();
            if (TextUtils.isEmpty(processName)) {
                //如果为配置进程名称，则默认为主进程
                processName = defaultProcessName;
            } else if (processName.startsWith(SUB_PROCESS_SEPARATOR)) {
                //如果是用冒号":"开头，则视为包含包名的子进程
                processName = packageName + processName;
            }
            //如果配置了进程名称，但不是以冒号开头，则将配置的名称作为该组件的进程名称
        } else {
            processName = defaultProcessName;
        }
        return processName;
    }

    static void unregisterComponent(IComponent component) {
        if (component != null) {
            String name = component.getName();
            if (hasComponent(name)) {
                COMPONENTS.remove(name);
            }
        }
    }

    static boolean hasComponent(String componentName) {
        return getComponentByName(componentName) != null;
    }

    /**
     * 组件调用统一入口
     * @param cc 组件调用指令
     * @return 组件调用结果（同步调用的返回值）
     */
    static CCResult call(CC cc) {
        String callId = cc.getCallId();
        Chain chain = new Chain(cc);
        if (!cc.isWithoutGlobalInterceptor()) {
            chain.addInterceptors(INTERCEPTORS);
        }
        chain.addInterceptors(cc.getInterceptors());
        // 有效性校验放在自定义拦截器之后执行，优先执行自定义拦截器，让其可以拦截到所有组件调用
        // 执行实际调用的拦截器在校验有效性结束后再添加
        chain.addInterceptor(ValidateInterceptor.getInstance());
        ChainProcessor processor = new ChainProcessor(chain);
        //异步调用，放到线程池中运行
        if (cc.isAsync()) {
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "put into thread pool");
            }
            CC_THREAD_POOL.submit(processor);
            //异步调用时此方法返回null，CCResult通过callback回调
            return null;
        } else {
            //同步调用，直接执行
            CCResult ccResult;
            try {
                ccResult = processor.call();
            } catch (Exception e) {
                ccResult = CCResult.defaultExceptionResult(e);
            }
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "cc finished.CCResult:" + ccResult);
            }
            //同步调用的返回结果，永不为null，默认为CCResult.defaultNullResult()
            return ccResult;
        }
    }

    static IComponent getComponentByName(String componentName) {
        return COMPONENTS.get(componentName);
    }

    static void mainThread(Runnable runnable) {
        MAIN_THREAD_HANDLER.post(runnable);
    }

    static void threadPool(Runnable runnable) {
        if (runnable != null) {
            CC_THREAD_POOL.execute(runnable);
        }
    }

    static String getComponentProcessName(String componentName) {
        if (TextUtils.isEmpty(componentName)) {
            return null;
        }
        String processName = COMPONENT_PROCESS_NAMES.get(componentName);
        if (TextUtils.isEmpty(processName) && !CCUtil.isMainProcess()) {
            //若当前子进程中不包含此组件，有可能是在其它子进程中注册的动态组件
            //先去主进程中确认一下是否有此动态组件
            processName = CC.obtainBuilder(COMPONENT_DYNAMIC_COMPONENT_OPTION)
                    .setActionName(ACTION_GET_PROCESS_NAME)
                    .addParam(KEY_COMPONENT_NAME, componentName)
                    .build().call().getDataItem(KEY_PROCESS_NAME, null);
        }
        return processName;
    }

    static final String COMPONENT_DYNAMIC_COMPONENT_OPTION = "internal.cc.dynamicComponentOption";
    static final String ACTION_REGISTER = "registerDynamicComponent";
    static final String ACTION_UNREGISTER = "unregisterDynamicComponent";
    static final String ACTION_GET_PROCESS_NAME = "getDynamicComponentProcessName";
    static final String KEY_COMPONENT_NAME = "componentName";
    static final String KEY_PROCESS_NAME = "processName";

    static class DynamicComponentOption implements IComponent {

        @Override
        public String getName() {
            return COMPONENT_DYNAMIC_COMPONENT_OPTION;
        }

        @Override
        public boolean onCall(CC cc) {
            String actionName = cc.getActionName();
            String componentName = cc.getParamItem(KEY_COMPONENT_NAME, null);
            String processName = cc.getParamItem(KEY_PROCESS_NAME, null);
            switch (actionName) {
                case ACTION_REGISTER:
                    COMPONENT_PROCESS_NAMES.put(componentName, processName);
                    CC.sendCCResult(cc.getCallId(), CCResult.success());
                    break;
                case ACTION_UNREGISTER:
                    COMPONENT_PROCESS_NAMES.remove(componentName);
                    CC.sendCCResult(cc.getCallId(), CCResult.success());
                    break;
                case ACTION_GET_PROCESS_NAME:
                    processName = COMPONENT_PROCESS_NAMES.get(componentName);
                    CC.sendCCResult(cc.getCallId(), CCResult.success(KEY_PROCESS_NAME, processName));
                    break;
                default:
                    CC.sendCCResult(cc.getCallId(), CCResult.error("unsupported action:" + actionName));
                    break;
            }
            return false;
        }
    }
}
