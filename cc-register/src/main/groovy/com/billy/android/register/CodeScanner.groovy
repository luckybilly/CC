package com.billy.android.register


import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Pattern
/**
 *
 * @author billy.qi
 * @since 17/3/20 11:48
 */
class CodeScanner {

    ArrayList<RegisterInfo> infoList
    Map<String, ScanHarvest> cacheMap
    Set<String> cachedJarContainsInitClass = new HashSet<>()

    CodeScanner(ArrayList<RegisterInfo> infoList, Map<String, ScanHarvest> cacheMap) {
        this.infoList = infoList
        this.cacheMap = cacheMap
    }

    /**
     * 扫描jar包
     * @param jarFile 来源jar包文件
     * @param destFile transform后的目标jar包文件
     */
    boolean scanJar(File jarFile, File destFile) {
        //检查是否存在缓存，有就添加class list 和 设置fileContainsInitClass
        if (!jarFile || hitCache(jarFile, destFile))
            return false

        def srcFilePath = jarFile.absolutePath
        def file = new JarFile(jarFile)
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            String entryName = jarEntry.getName()
            //support包不扫描
            if (entryName.startsWith("android/support"))
                break
            checkInitClass(entryName, destFile, srcFilePath)
            //是否要过滤这个类，这个可配置
            if (shouldProcessClass(entryName)) {
                InputStream inputStream = file.getInputStream(jarEntry)
                scanClass(inputStream, jarFile.absolutePath)
                inputStream.close()
            }
        }
        if (null != file) {
            file.close()
        }
        //加入缓存
        addToCacheMap(null, null, srcFilePath)
        return true
    }
    /**
     * 检查此entryName是不是被注入注册代码的类，如果是则记录此文件（class文件或jar文件）用于后续的注册代码注入
     * @param entryName
     * @param destFile
     */
    boolean checkInitClass(String entryName, File destFile) {
        checkInitClass(entryName, destFile, "")
    }

    boolean checkInitClass(String entryName, File destFile, String srcFilePath) {
        if (entryName == null || !entryName.endsWith(".class"))
            return
        entryName = entryName.substring(0, entryName.lastIndexOf('.'))
        def found = false
        infoList.each { ext ->
            if (ext.initClassName == entryName) {
                ext.fileContainsInitClass = destFile
                if (destFile.name.endsWith(".jar") || destFile.name.endsWith(".class")) {
                    addToCacheMap(null, entryName, srcFilePath)
                    found = true
                }
            }
        }
        return found
    }

    // file in folder like these
    //com/billy/testplugin/Aop.class
    //com/billy/testplugin/BuildConfig.class
    //com/billy/testplugin/R$attr.class
    //com/billy/testplugin/R.class
    // entry in jar like these
    //android/support/v4/BuildConfig.class
    //com/lib/xiwei/common/util/UiTools.class
    boolean shouldProcessClass(String entryName) {
//        println('classes:' + entryName)
        if (entryName == null || !entryName.endsWith(".class"))
            return false
        entryName = entryName.substring(0, entryName.lastIndexOf('.'))
        def length = infoList.size()
        for (int i = 0; i < length; i++) {
            if (shouldProcessThisClassForRegister(infoList.get(i), entryName))
                return true
        }
        return false
    }

    /**
     * 过滤器进行过滤
     * @param info
     * @param entryName
     * @return
     */
    private static boolean shouldProcessThisClassForRegister(RegisterInfo info, String entryName) {
        if (info != null) {
            def list = info.includePatterns
            if (list) {
                def exlist = info.excludePatterns
                Pattern pattern, p
                for (int i = 0; i < list.size(); i++) {
                    pattern = list.get(i)
                    if (pattern.matcher(entryName).matches()) {
                        if (exlist) {
                            for (int j = 0; j < exlist.size(); j++) {
                                p = exlist.get(j)
                                if (p.matcher(entryName).matches())
                                    return false
                            }
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 处理class的注入
     * @param file class文件
     * @return 修改后的字节码文件内容
     */
    boolean scanClass(File file) {
        return scanClass(file.newInputStream(), file.absolutePath)
    }

    //refer hack class when object init
    boolean scanClass(InputStream inputStream, String filePath) {
        int api = getAsmApiLevel()
        try {
            ClassReader cr = new MyClassReader(inputStream)
            ClassWriter cw = new ClassWriter(cr, 0)
            ScanClassVisitor cv = new ScanClassVisitor(api, cw, filePath)
            cr.accept(cv, ClassReader.EXPAND_FRAMES)
            inputStream.close()

            return cv.found
        } catch (Throwable throwable) {
            System.err.println("\n>>>>>>>>>>>>ERROR: An error occurred while scanning class file(built by jdk: $lastClassBuildVersion):" +
                    "\n\t" + filePath)
            if (throwable instanceof IllegalArgumentException) {
                System.err.println("Maybe the current ASM(${api >> 16}.x) version is not compatible with the jdk version that compiled this class. " +
                        "\nTo resolve this problem, please update your gradle version to use higher ASM version, " +
                        "or rebuild your class/jar with lower level JDK." +
                        "\n\nFor example: We first encountered this problem with Gson 2.8.6 which built by jdk version 53 (JDK9), " +
                        "\nwe need ASM6 to scan its classes(update android gradle plugin to 3.2.0 or higher)\n")
            }
            throw throwable
        }
    }
    short lastClassBuildVersion;

    class MyClassReader extends ClassReader {

        MyClassReader(InputStream is) throws IOException {
            super(is)
        }

        @Override
        short readShort(int index) {
            def s = super.readShort(index)
            if (index == 6) {
                // cache the last class file compiled JDK version before crash happens
                lastClassBuildVersion = s
            }
            return s
        }
    }


    private static int ASM_LEVEL = 0
    static int getAsmApiLevel() {
        if (ASM_LEVEL > 0) return ASM_LEVEL
        int api = Opcodes.ASM5
        for (i in (10..5)) {
            try {
                def field = Opcodes.class.getDeclaredField("ASM" + i)
                if (field != null) {
                    api = field.get(null)
                    break
                }
            } catch (Throwable ignored) {
            }
        }
        ASM_LEVEL = api
        return ASM_LEVEL
    }

    class ScanClassVisitor extends ClassVisitor {
        private String filePath
        private def found = false

        ScanClassVisitor(int api, ClassVisitor cv, String filePath) {
            super(api, cv)
            this.filePath = filePath
        }

        boolean is(int access, int flag) {
            return (access & flag) == flag
        }

        boolean isFound() {
            return found
        }

        void visit(int version, int access, String name, String signature,
                   String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
            //抽象类、接口、非public等类无法调用其无参构造方法
            if (is(access, Opcodes.ACC_ABSTRACT)
                    || is(access, Opcodes.ACC_INTERFACE)
                    || !is(access, Opcodes.ACC_PUBLIC)
            ) {
                return
            }
            infoList.each { ext ->
                if (shouldProcessThisClassForRegister(ext, name)) {
                    def interfaceName = ext.interfaceName
                    if (superName != 'java/lang/Object' && !ext.superClassNames.isEmpty()) {
                        for (int i = 0; i < ext.superClassNames.size(); i++) {
                            if (ext.superClassNames.get(i) == superName) {
                                gotOne(interfaceName, name, ext)
                                return
                            }
                        }
                    }
                    if (interfaceName && interfaces != null) {
                        interfaces.each { itName ->
                            if (itName == interfaceName) {
                                gotOne(interfaceName, name, ext)
                            }
                        }
                    }
                }
            }
        }

        void gotOne(String interfaceName, String className, RegisterInfo ext) {
            ext.classList.add(className) //需要把对象注入到管理类 就是fileContainsInitClass
            found = true
            addToCacheMap(interfaceName, className, filePath)
        }
    }
    /**
     * 扫描到的类添加到map
     * @param interfaceName
     * @param name
     * @param srcFilePath
     */
    private void addToCacheMap(String interfaceName, String name, String srcFilePath) {
        if (!srcFilePath.endsWith(".jar") && !srcFilePath.endsWith(".class")|| cacheMap == null) return
        def scanHarvest = cacheMap.get(srcFilePath)
        if (!scanHarvest) {
            scanHarvest = new ScanHarvest()
            cacheMap.put(srcFilePath, scanHarvest)
        }
        if (name) {
            ScanHarvest.Harvest harvest = new ScanHarvest.Harvest()
            harvest.setIsInitClass(interfaceName == null)
            harvest.setInterfaceName(interfaceName)
            harvest.setClassName(name)
            scanHarvest.harvestList.add(harvest)
        }
    }

    boolean isCachedJarContainsInitClass(String filePath) {
        return cachedJarContainsInitClass.contains(filePath)
    }

    /**
     * 检查是否存在缓存，有就添加class list 和 设置fileContainsInitClass
     * @param jarFile
     * @param destFile
     * @return 是否存在缓存
     */
    boolean hitCache(File jarFile, File destFile) {
        def jarFilePath = jarFile.absolutePath
        if (cacheMap != null) {
            ScanHarvest scanHarvest = cacheMap.get(jarFilePath)
            if (scanHarvest) {
                infoList.each { info ->
                    scanHarvest.harvestList.each { harvest ->
                        //       println("----ccMainHarvest-------"+ccMainHarvest.className)
                        if (harvest.isInitClass) {
                            if (info.initClassName == harvest.className) {
                                info.fileContainsInitClass = destFile
                                cachedJarContainsInitClass.add(jarFilePath)
                            }
                        } else if (info.interfaceName == harvest.interfaceName) {
                            info.classList.add(harvest.className)
                        }
                    }
                }
                return true
            }
        }
        return false
    }
}