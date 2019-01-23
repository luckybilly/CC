package com.billy.android.register.cc

import com.billy.android.register.RegisterInfo

/**
 * CC框架自身默认的注册配置辅助类
 */
class DefaultRegistryHelper {

    static void addDefaultRegistry(ArrayList<RegisterInfo> list) {
        def exclude = ['com/billy/cc/core/component/.*']
        addDefaultRegistryFor(list,
                'com.billy.cc.core.component.IComponent',
                'com.billy.cc.core.component.ComponentManager',
                'registerComponent',
                RegisterInfo.PARAM_TYPE_OBJECT,
                exclude)
        addDefaultRegistryFor(list,
                'com.billy.cc.core.component.IGlobalCCInterceptor',
                'com.billy.cc.core.component.GlobalCCInterceptorManager',
                'registerGlobalInterceptor',
                RegisterInfo.PARAM_TYPE_OBJECT,
                exclude)
        addDefaultRegistryFor(list,
                'com.billy.cc.core.component.IParamJsonConverter',
                'com.billy.cc.core.component.remote.RemoteParamUtil',
                'initRemoteCCParamJsonConverter',
                RegisterInfo.PARAM_TYPE_OBJECT,
                exclude)
    }

    static void addDefaultRegistryFor(ArrayList<RegisterInfo> list, String interfaceName,
                                      String codeInsertToClassName, String registerMethodName,
                                      String paramType,
                                      List<String> exclude) {
        if (!list.find { it.interfaceName == RegisterInfo.convertDotToSlash(interfaceName) }) {
            RegisterInfo info = new RegisterInfo()
            info.interfaceName = interfaceName
            info.superClassNames = []
            info.initClassName = codeInsertToClassName //代码注入的类
            info.registerMethodName = registerMethodName //生成的代码所调用的方法
            info.paramType = paramType //注册方法的类型
            info.exclude = exclude
            info.init()
            list.add(info)
        }
    }
}