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
        project.extensions.create(EXT_NAME, CcRegisterConfig)
        if (isApp) {
            println "project(${project.name}) apply ${PLUGIN_NAME} plugin"
            def android = project.extensions.getByType(AppExtension)
            def transformImpl = new RegisterTransform(project)
            android.registerTransform(transformImpl)
            project.afterEvaluate {
                CcRegisterConfig config = init(project, transformImpl)//此处要先于transformImpl.transform方法执行
                if (config.multiProcessEnabled) {
                    ManifestGenerator.generateManifestFileContent(project, config.excludeProcessNames)
                }
            }
        }
    }

    static CcRegisterConfig init(Project project, RegisterTransform transformImpl) {
        CcRegisterConfig config = project.extensions.findByName(EXT_NAME) as CcRegisterConfig
        config.project = project
        config.convertConfig()
        transformImpl.config = config
        return config
    }

}
