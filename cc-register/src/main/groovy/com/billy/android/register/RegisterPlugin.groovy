package com.billy.android.register

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.billy.android.register.generator.ManifestGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * 自动注册插件入口
 * @author billy.qi
 * @since 17/3/14 17:35
 */
public class RegisterPlugin implements Plugin<Project> {
    public static final String PLUGIN_NAME = 'cc-register'
    public static final String EXT_NAME = 'ccregister'

    @Override
    public void apply(Project project) {
        /**
         * 注册transform接口
         */
        def isApp = project.plugins.hasPlugin(AppPlugin)
        project.extensions.create(EXT_NAME, RegisterExtension)
        if (isApp) {
            println "project(${project.name}) apply ${PLUGIN_NAME} plugin"
            def android = project.extensions.getByType(AppExtension)
            def transformImpl = new RegisterTransform(project)
            android.registerTransform(transformImpl)
            project.afterEvaluate {
                RegisterExtension config = init(project, transformImpl)//此处要先于transformImpl.transform方法执行
                if (config.multiProcessEnabled) {
                    ManifestGenerator.generateManifestFileContent(project, config.excludeProcessNames)
                }
            }
        }
    }

    static RegisterExtension init(Project project, RegisterTransform transformImpl) {
        RegisterExtension config = project.extensions.findByName(EXT_NAME) as RegisterExtension
        config.project = project
        config.convertConfig()
        addDefaultRegistry(config.list)
        transformImpl.config = config
        return config
    }

    static void addDefaultRegistry(ArrayList<RegisterInfo> list) {
        def exclude = ['com/billy/cc/core/component/.*']
        addDefaultRegistryFor(list,
                'com.billy.cc.core.component.IComponent',
                'com.billy.cc.core.component.ComponentManager',
                'registerComponent',
                exclude)
        addDefaultRegistryFor(list,
                'com.billy.cc.core.component.IGlobalCCInterceptor',
                'com.billy.cc.core.component.GlobalCCInterceptorManager',
                'registerGlobalInterceptor',
                exclude)
        addDefaultRegistryFor(list,
                'com.billy.cc.core.component.IParamJsonConverter',
                'com.billy.cc.core.component.remote.RemoteParamUtil',
                'initRemoteCCParamJsonConverter',
                exclude)
    }

    static void addDefaultRegistryFor(ArrayList<RegisterInfo> list, String interfaceName,
                                      String codeInsertToClassName, String registerMethodName,
                                      List<String> exclude) {
        if (!list.find { it.interfaceName == RegisterInfo.convertDotToSlash(interfaceName) }) {
            RegisterInfo info = new RegisterInfo()
            info.interfaceName = interfaceName
            info.superClassNames = []
            info.initClassName = codeInsertToClassName //代码注入的类
            info.registerMethodName = registerMethodName //生成的代码所调用的方法
            info.exclude = exclude
            info.init()
            list.add(info)
        }
    }

}
