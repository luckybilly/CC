package com.billy.cc.core.component;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局拦截器{@link IGlobalCCInterceptor}的管理类
 * @author billy.qi
 * @since 18/5/26 14:07
 */
class GlobalCCInterceptorManager {
    static final CopyOnWriteArrayList<IGlobalCCInterceptor> INTERCEPTORS = new CopyOnWriteArrayList<>();

    /*
      加载类时自动调用初始化：注册所有全局拦截器 (实现 IGlobalCCInterceptor 接口的类)
      通过auto-register插件生成拦截器注册代码
      生成的代码如下:
      static {
          registerGlobalInterceptor(new InterceptorA());
          registerGlobalInterceptor(new InterceptorB());
      }
    */

    /**
     * 提前初始化所有全局拦截器
     */
    static void init(){
        //调用此方法时，虚拟机会加载GlobalCCInterceptorManager类
        //会自动执行static块中的全局拦截器注册，调用拦截器类的无参构造方法
        //如果不提前调用此方法，static块中的代码将在第一次进行组件调用时(cc.callXxx())执行
    }

    static void registerGlobalInterceptor(IGlobalCCInterceptor interceptor) {
        if (interceptor == null) {
            if (CC.DEBUG) {
                CC.logError("register global interceptor is null!");
            }
        } else {
            Class clazz = interceptor.getClass();
            synchronized (INTERCEPTORS) {
                int index = 0;
                for (IGlobalCCInterceptor it : INTERCEPTORS) {
                    if (it.getClass() == clazz) {
                        if (CC.DEBUG) {
                            CC.logError("duplicate global interceptor:" + clazz.getName());
                        }
                        return;
                    }
                    if (it.priority() > interceptor.priority()) {
                        index++;
                    }
                }
                INTERCEPTORS.add(index,interceptor);
            }
            if (CC.DEBUG) {
                CC.log("register global interceptor success! priority = "
                        + interceptor.priority() + ", class = " + clazz.getName());
            }
        }
    }

    static void unregisterGlobalInterceptor(Class<? extends IGlobalCCInterceptor> clazz) {
        synchronized (INTERCEPTORS) {
            for (IGlobalCCInterceptor next : INTERCEPTORS) {
                if (next.getClass() == clazz) {
                    INTERCEPTORS.remove(next);
                    if (CC.DEBUG) {
                        CC.log("unregister global interceptor success! class = " + clazz.getName());
                    }
                    break;
                }
            }
        }
    }
}
