package com.billy.android.register

import com.android.build.gradle.AppExtension
import com.billy.android.register.cc.DefaultRegistryHelper
import com.billy.android.register.cc.ProjectModuleManager
import com.billy.android.register.cc.generator.ManifestGenerator
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
        println "project(${project.name}) apply ${PLUGIN_NAME} plugin"
        project.extensions.create(EXT_NAME, RegisterExtension)
        def isApp = ProjectModuleManager.manageModule(project)
        if (isApp) {
            println "project(${project.name}) register ${PLUGIN_NAME} transform"
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
        RegisterExtension extension = project.extensions.findByName(EXT_NAME) as RegisterExtension
        extension.project = project
        extension.convertConfig()
        DefaultRegistryHelper.addDefaultRegistry(extension.list)
        transformImpl.extension = extension
        return extension
    }

}
