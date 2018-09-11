package com.billy.android.register.generator

import com.android.build.gradle.AppExtension
import com.billy.android.register.RegisterTransform
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import org.gradle.api.Project

/**
 * 生成provider类
 */
class ManifestGenerator {
    static final String AUTHORITY = "com.billy.cc.core.remote"

    static void generateManifestFileContent(Project project) {
        def android = project.extensions.getByType(AppExtension)
        android.applicationVariants.all { variant ->
            String pkgName = [variant.mergedFlavor.applicationId, variant.buildType.applicationIdSuffix].findAll().join()
            variant.outputs.each { output ->
                output.processManifest.doLast {
                    output.processManifest.outputs.files.each { File file ->
                        //在gradle plugin 3.0.0之前，file是文件，且文件名为AndroidManifest.xml
                        //在gradle plugin 3.0.0之后，file是目录，且不包含AndroidManifest.xml，需要自己拼接
                        if (file.isDirectory() || file.name.equalsIgnoreCase("AndroidManifest.xml")) {
                            if (file.isDirectory()) {
                                //3.0.0之后，在目录下查找AndroidManifest.xml文件
                                doGenerateProviderContent(pkgName, new File(file, "AndroidManifest.xml"))
                            } else {
                                //3.0.0之前，直接使用AndroidManifest.xml文件
                                doGenerateProviderContent(pkgName, file)
                            }
                        }
                    }
                }
            }
        }
    }

    static void doGenerateProviderContent(String pkgName, File manifestFile) {
        if (!manifestFile || !manifestFile.exists())
            return
        println "generate provider content into file:${manifestFile.absolutePath}"
        def manifest = new XmlSlurper().parse(manifestFile)
        if (!pkgName) pkgName = manifest.'@package'
        Set<String> existProviders = getExistProviders(manifest)

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
        processNames.removeAll(existProviders)
        if (!processNames.empty) {
            writeProvidersIntoManifestFile(pkgName, manifestFile, processNames)
        }
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
                    provider(
                            "android:authorities": "${pkgName}${processName}.${AUTHORITY}",
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

    private static Set<String> getExistProviders(GPathResult manifest) {
        def existProviders = new HashSet<>()
        def prefix = ProviderGenerator.MAIN_CC_SUB_PROCESS_FOLDER.replaceAll("/", ".")
        manifest.application.provider.each {
            String name = it.'@android:name'
            if (name && name.startsWith(prefix)) {
                existProviders.add(it.'@android:process')
            }
        }
        return existProviders
    }
}