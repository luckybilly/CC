package com.billy.android.register.cc

import com.billy.android.register.RegisterPlugin
import org.gradle.api.Project

import java.util.regex.Pattern

/**
 * 工程中的组件module管理工具
 * 1. 用于管理组件module以application或library方式进行编译
 * 2. 用于管理组件依赖（只在给当前module进行集成打包时才添加对组件的依赖，以便于进行代码隔离）
 */
class ProjectModuleManager {
    static final String PLUGIN_NAME = RegisterPlugin.PLUGIN_NAME
    
    //为区别于组件单独以app方式运行的task，将组件module打包成aar时，在local.properties文件中添加 assemble_aar_for_cc_component=true
    static final String ASSEMBLE_AAR_FOR_CC_COMPONENT = "assemble_aar_for_cc_component"
    //组件单独以app方式运行时使用的测试代码所在目录(manifest/java/assets/res等),这个目录下的文件不会打包进主app
    static final String DEBUG_DIR = "src/main/debug/"
    //主app，一直以application方式编译
    static final String MODULE_MAIN_APP = "mainApp" 
    //apply了cc-settings-2.gradle的module，但不是组件，而是一直作为library被其它组件依赖
    static final String MODULE_ALWAYS_LIBRARY = "alwaysLib" 
    

    static String mainModuleName
    static boolean taskIsAssemble

    static boolean manageModule(Project project) {
        taskIsAssemble = false
        mainModuleName = null
        Properties localProperties = new Properties()
        try {
            def localFile = project.rootProject.file('local.properties')
            if (localFile != null && localFile.exists()) {
                localProperties.load(localFile.newDataInputStream())
            }
        } catch (Exception ignored) {
            println("${PLUGIN_NAME}: local.properties not found")
        }
        initByTask(project)

        def mainApp = isMainApp(project)
        def assembleFor = isAssembleFor(project)
        def buildingAar = isBuildingAar(localProperties)
        def alwaysLib = isAlwaysLib(project)

        boolean runAsApp = false
        if (mainApp) {
            runAsApp = true
        } else if (alwaysLib || buildingAar) {
            runAsApp = false
        } else if (assembleFor || !taskIsAssemble) {
            runAsApp = true
        }
        project.ext.runAsApp = runAsApp
        println "${PLUGIN_NAME}: project=${project.name}, runAsApp=${runAsApp} . taskIsAssemble:${taskIsAssemble}. " +
                "settings(mainApp:${mainApp}, alwaysLib:${alwaysLib}, assembleThisModule:${assembleFor}, buildingAar:${buildingAar})"
        if (runAsApp) {
            project.apply plugin: 'com.android.application'

            project.android.sourceSets.main {
                //debug模式下，如果存在src/main/debug/AndroidManifest.xml，则自动使用其作为manifest文件
                def debugManifest = "${DEBUG_DIR}AndroidManifest.xml"
                if (project.file(debugManifest).exists()) {
                    manifest.srcFile debugManifest
                }
                //debug模式下，如果存在src/main/debug/assets，则自动将其添加到assets源码目录
                if (project.file("${DEBUG_DIR}assets").exists()) {
                    assets.srcDirs = ['src/main/assets', "${DEBUG_DIR}assets"]
                }
                //debug模式下，如果存在src/main/debug/java，则自动将其添加到java源码目录
                if (project.file("${DEBUG_DIR}java").exists()) {
                    java.srcDirs = ['src/main/java', "${DEBUG_DIR}java"]
                }
                //debug模式下，如果存在src/main/debug/res，则自动将其添加到资源目录
                if (project.file("${DEBUG_DIR}res").exists()) {
                    res.srcDirs = ['src/main/res', "${DEBUG_DIR}res"]
                }
            }
        } else {
            project.apply plugin: 'com.android.library'
        }
        //为build.gradle添加addComponent方法
        addComponentDependencyMethod(project, localProperties)
        return runAsApp
    }

    //需要集成打包相关的task
    static final String TASK_TYPES = ".*((((ASSEMBLE)|(BUILD)|(INSTALL)|((BUILD)?TINKER)|(RESGUARD)).*)|(ASR)|(ASD))"
    static void initByTask(Project project) {
        def taskNames = project.gradle.startParameter.taskNames
        def allModuleBuildApkPattern = Pattern.compile(TASK_TYPES)
        for (String task : taskNames) {
            if (allModuleBuildApkPattern.matcher(task.toUpperCase()).matches()) {
                taskIsAssemble = true
                if (task.contains(":")) {
                    def arr = task.split(":")
                    mainModuleName = arr[arr.length - 2].trim()
                }
                break
            }
        }
    }

    /**
     * 当前是否正在给指定的module集成打包
     */
    static boolean isAssembleFor(Project project) {
        return project.name == mainModuleName
    }
    static boolean isMainApp(Project project) {
        return project.ext.has(MODULE_MAIN_APP) && project.ext.mainApp
    }
    static boolean isAlwaysLib(Project project) {
        return project.ext.has(MODULE_ALWAYS_LIBRARY) && project.ext.alwaysLib
    }
    //判断当前设置的环境是否为组件打aar包（比如将组件打包上传maven库）
    static boolean isBuildingAar(Properties localProperties) {
        return 'true' == localProperties.getProperty(ASSEMBLE_AAR_FOR_CC_COMPONENT)
    }

    //组件依赖的方法，用于进行代码隔离
    //对组件库的依赖格式： addComponent dependencyName [, realDependency]
    // 使用示例见demo/build.gradle
    //  dependencyName: 组件库的名称，推荐直接使用使用module的名称
    //  realDependency(可选): 组件库对应的实际依赖，可以是module依赖，也可以是maven依赖
    //    如果未配置realDependency，将自动依赖 project(":$dependencyName")
    //    realDependency可以为如下2种中的一种:
    //      module依赖 : project(':demo_component_b') //如果module名称跟dependencyName相同，可省略(推荐)
    //      maven依赖  : 'com.billy.demo:demoB:1.1.0' //如果使用了maven私服，请使用此方式
    static void addComponentDependencyMethod(Project project, Properties localProperties) {
        //当前task是否为给本module打apk包
        def curModuleIsBuildingApk = taskIsAssemble && (mainModuleName == null && isMainApp(project) || mainModuleName == project.name)
        project.ext.addComponent = { dependencyName, realDependency = null ->
            //不是在为本app module打apk包，不添加对组件的依赖
            if (!curModuleIsBuildingApk)
                return
            def excludeModule = 'true' == localProperties.getProperty(dependencyName)
            if (!excludeModule) {
                def componentProject = project.rootProject.subprojects.find { it.name == dependencyName }
                def dependencyMode = (project.gradle.gradleVersion as float) >= 4.1F ? 'api' : 'compile'
                if (realDependency) {
                    //通过参数传递的依赖方式，如：
                    // project(':moduleName')
                    // 或
                    // 'com.billy.demo:demoA:1.1.0'
                    project.dependencies.add(dependencyMode, realDependency)
                    println "CC >>>> add $realDependency to ${project.name}'s dependencies"
                } else if (componentProject) {
                    //第二个参数未传，默认为按照module来进行依赖
                    project.dependencies.add(dependencyMode, project.project(":$dependencyName"))
                    println "CC >>>> add project(\":$dependencyName\") to ${project.name}'s dependencies"
                } else {
                    throw new RuntimeException(
                            "CC >>>> add dependency by [ addComponent '$dependencyName' ] occurred an error:" +
                                    "\n'$dependencyName' is not a module in current project" +
                                    " and the 2nd param is not specified for realDependency" +
                                    "\nPlease make sure the module name is '$dependencyName'" +
                                    "\nelse" +
                                    "\nyou can specify the real dependency via add the 2nd param, for example: " +
                                    "addComponent '$dependencyName', 'com.billy.demo:demoB:1.1.0'")
                }
            }
        }
    }
}