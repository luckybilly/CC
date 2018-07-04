package com.billy.cc.core.component;

import android.text.TextUtils;

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
    private static final ConcurrentHashMap<String, IComponent> COMPONENTS = new ConcurrentHashMap<>();
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
        //如果不提动调用此方法，static块中的代码将在第一次进行组件调用时(cc.callXxx())执行
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
                e.printStackTrace();
            }
        }
    }

    static String getComponentProcessName(Class<? extends IComponent> componentClass) {
        SubProcess subProcess = componentClass.getAnnotation(SubProcess.class);
        String packageName = CC.getApplication().getPackageName();
        //TODO 需要兼容：app的默认进程名称有可能不是包名
        // 通过在application节点添加android:process="a.b.c"可以指定默认进程名称
        String defaultProcessName = packageName;
        String processName;
        if (subProcess != null) {
            processName = subProcess.name();
            if (TextUtils.isEmpty(processName)) {
                processName = defaultProcessName;
            } else if (processName.startsWith(SUB_PROCESS_SEPARATOR)) {
                processName = packageName + processName;
            }
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
        chain.addInterceptor(ValidateInterceptor.getInstance());
        if (!cc.isWithoutGlobalInterceptor()) {
            chain.addInterceptors(INTERCEPTORS);
        }
        chain.addInterceptors(cc.getInterceptors());
        String componentName = cc.getComponentName();
        if (hasComponent(componentName)) {
            //调用当前进程中的组件
            chain.addInterceptor(LocalCCInterceptor.getInstance());
        } else if (!TextUtils.isEmpty(getComponentProcessName(componentName))) {
            //app内部跨进程调用组件
            chain.addInterceptor(SubProcessCCInterceptor.getInstance());
        } else {
            //跨App调用组件
            chain.addInterceptor(RemoteCCInterceptor.getInstance());
        }
        chain.addInterceptor(Wait4ResultInterceptor.getInstance());
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

    static void threadPool(Runnable runnable) {
        if (runnable != null) {
            CC_THREAD_POOL.execute(runnable);
        }
    }

    static String getComponentProcessName(String componentName) {
        return COMPONENT_PROCESS_NAMES.get(componentName);
    }

    static final String COMPONENT_DYNAMIC_COMPONENT_OPTION = "internal.cc.dynamicComponentOption";
    static final String ACTION_REGISTER = "registerDynamicComponent";
    static final String ACTION_UNREGISTER = "unregisterDynamicComponent";
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
                default:
                    CC.sendCCResult(cc.getCallId(), CCResult.error("unsupported action:" + actionName));
                    break;
            }
            return false;
        }
    }
}
