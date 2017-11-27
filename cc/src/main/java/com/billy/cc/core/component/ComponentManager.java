package com.billy.cc.core.component;

import android.text.TextUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 组件调用管理类
 * @author billy.qi
 * @since 17/6/28 20:14
 */
class ComponentManager {
    private static final ConcurrentHashMap<String, IComponent> COMPONENTS = new ConcurrentHashMap<>();
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

    //加载类时自动调用初始化：注册所有组件
    //通过auto-register插件生成组件注册代码
    //生成的代码如下:
//  static {
//      registerComponent(new ComponentA());
//      registerComponent(new ComponentAA());
//  }

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
                    IComponent oldComponent = COMPONENTS.put(name, component);
                    if (oldComponent != null) {
                        CC.logError( "component (" + component.getClass().getName()
                                + ") with name:" + name
                                + " has already exists, replaced:" + oldComponent.getClass().getName());
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
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
        chain.addInterceptors(cc.getInterceptors());
        if (hasComponent(cc.getComponentName())) {
            chain.addInterceptor(LocalCCInterceptor.getInstance());
        } else {
            chain.addInterceptor(new RemoteCCInterceptor(cc));
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
}
