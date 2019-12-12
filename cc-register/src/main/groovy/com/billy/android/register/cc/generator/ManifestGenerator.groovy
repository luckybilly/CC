package com.billy.android.register.cc.generator

import com.android.build.gradle.AppExtension
import com.android.builder.model.Version
import com.billy.android.register.RegisterPlugin
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import org.gradle.api.Project
import org.gradle.util.GradleVersion

/**
 * 生成provider类
 */
class ManifestGenerator {
    static final String AUTHORITY = "com.billy.cc.core.remote"

    static Map<String, Set<String>> cachedProcessNames = new HashMap<>()

    static void cacheProcessNames(String projectName, String variantName, Set<String> processNames) {
        cachedProcessNames.put(projectName + "_" + variantName, processNames)
    }

    static Set<String> getCachedProcessNames(String projectName, String variantName) {
        return cachedProcessNames.get(projectName + "_" + variantName)
    }

    /**
     * 为processManifest的task添加自动注入子进程provider的功能
     */
    static void generateManifestFileContent(Project project, ArrayList<String> excludeProcessNames) {
        def android = project.extensions.getByType(AppExtension)
        android.applicationVariants.all { variant ->
            String pkgName = [variant.mergedFlavor.applicationId, variant.mergedFlavor.applicationIdSuffix, variant.buildType.applicationIdSuffix].findAll().join()
            variant.outputs.each { output ->
                def processManifest = null
                def gradlePluginAfter_3_3_0 = GradleVersion.version(Version.ANDROID_GRADLE_PLUGIN_VERSION) >= GradleVersion.version('3.3.0')
                //fix warning:
                //  WARNING: API 'variantOutput.getProcessManifest()' is obsolete and has
                //  been replaced with 'variantOutput.getProcessManifestProvider()'.
                //  It will be removed at the end of 2019.
                if (gradlePluginAfter_3_3_0) {
                    try {
                        processManifest = output.processManifestProvider.get()
                    } catch(Throwable ignored){}
                }
                if(processManifest == null) {
                    processManifest = output.processManifest
                }
                processManifest.doLast {
                    processManifest.outputs.files.each { File file ->
                        //在gradle plugin 3.0.0之前，file是文件，且文件名为AndroidManifest.xml
                        //在gradle plugin 3.0.0之后，file是目录，AndroidManifest.xml文件在此目录下
                        def manifestFile = null
                        if (file.name =="AndroidManifest.xml") {
                            manifestFile = file
                        } else if (file.isDirectory()) {
                            manifestFile = new File(file, "AndroidManifest.xml")
                        }
                        if (manifestFile && manifestFile.exists()) {
                            println "${RegisterPlugin.PLUGIN_NAME} regist provider into:${manifestFile.absolutePath}"
                            def manifest = new XmlSlurper().parse(manifestFile)
                            if (!pkgName) pkgName = manifest.'@package'
                            HashSet<String> processNames = getAllManifestedProcessNames(manifest)
                            processNames.removeAll(excludeProcessNames)
                            if (!processNames.empty) {
                                writeProvidersIntoManifestFile(pkgName, manifestFile, processNames)
                            }
                            //将processManifestTask执行后扫描出的子进程名称缓存起来给transform使用
                            cacheProcessNames(project.name, variant.name, processNames)
                        }
                    }
                }
            }
        }
    }


    /**
     * 分析merge后的AndroidManifest.xml文件中的四大组件，收集所有子进程名称
     */
    private static HashSet<String> getAllManifestedProcessNames(GPathResult manifest) {
        Set<String> processNames = new HashSet<>()
        manifest.application.activity.each {
            addSubProcess(processNames, it)
        }
        manifest.application.service.each {
            addSubProcess(processNames, it)
        }
        manifest.application.receiver.each {
            addSubProcess(processNames, it)
        }
        manifest.application.provider.each {
            addSubProcess(processNames, it)
        }
        return processNames
    }

    private static void addSubProcess(Set<String> processNames, def it) {
        String processName = it.'@android:process'
        if (processName && !processNames.contains(processName)) {
            processNames.add(processName)
        }
    }

    private static void writeProvidersIntoManifestFile(String pkgName, File manifestFile, Set<String> processNames) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.root {
            processNames.each { processName ->
                if (processName){
                    def providerName = ProviderGenerator.getSubProcessProviderClassName(processName)
                    providerName = providerName.replaceAll("/", ".")
                    //兼容以冒号开头的子进程和直接命名的子进程
                    def realProcess = processName.startsWith(":") ? (pkgName + processName) : processName
                    provider(
                            "android:authorities": "${realProcess}.${AUTHORITY}",
                            "android:exported": "true",
                            "android:name": providerName,
                            "android:process": processName
                    )
                }
            }
        }
        def providerXml = writer.toString().replace("<root>", "").replace("</root>", "")

        String content = manifestFile.getText("UTF-8")
        int index = content.lastIndexOf("</application>")
        content = content.substring(0, index) + providerXml + content.substring(index)
        manifestFile.write(content, 'UTF-8')
    }

}