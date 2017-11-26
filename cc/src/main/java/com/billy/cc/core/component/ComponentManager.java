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
                } else if (hasComponent(name)) {
                    CC.logError( "component (" + component.getClass().getName()
                            + ") with name:" + name
                            + " has already exists:" + COMPONENTS.get(name).getClass().getName());
                } else {
                    COMPONENTS.put(name, component);
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
        return COMPONENTS.get(componentName) != null;
    }

    /**
     * 组件调用统一入口
     * @param cc 组件调用指令
     * @return 组件调用结果（同步调用的返回值）
     */
    static CCResult call(CC cc) {
        String callId = cc.getCallId();
        Chain chain = new Chain(cc);
        String componentName = cc.getComponentName();
        if (TextUtils.isEmpty(componentName)) {
            //没有指定要调用的组件名称，中止运行
            chain.setInterceptors(new StopCCInterceptor(CCResult.CODE_ERROR_COMPONENT_NAME_EMPTY));
        } else if (cc.getContext() == null) {
            //context为null (没有设置context 且 CC中获取application失败)
            chain.setInterceptors(new StopCCInterceptor(CCResult.CODE_ERROR_CONTEXT_NULL));
        } else {
            IComponent component = COMPONENTS.get(componentName);
            if (component == null && !CC.CALL_REMOTE_CC_IF_NEED) {
                chain.setInterceptors(new StopCCInterceptor(CCResult.CODE_ERROR_NO_COMPONENT_FOUND));
                CC.log("componentName=" + componentName
                        + " is not exists in " + cc.getContext().getPackageName()
                        + " and CC.enableRemoteCC is " + CC.CALL_REMOTE_CC_IF_NEED);
            } else {
                if (component != null) {
                    chain.addInterceptor(new LocalCCInterceptor(component));
                } else {
                    chain.addInterceptor(new RemoteCCInterceptor(cc));
                }
            }
        }
        CCProcessor processor = new CCProcessor(chain);
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
            if (ccResult == null) {
                ccResult = CCResult.defaultNullResult();
            }
            if (CC.VERBOSE_LOG) {
                CC.verboseLog(callId, "cc finished.CCResult:" + ccResult);
            }
            //同步调用的返回结果，永不为null，默认为CCResult.defaultNullResult()
            return ccResult;
        }
    }
}
