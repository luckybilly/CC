package com.billy.android.register.cc.generator

import com.billy.android.register.RegisterTransform
import org.objectweb.asm.*

/**
 * 生成provider类
 */
class ProviderGenerator implements Opcodes {
    public static final String MULTI_PROCESS_PROVIDER_CLASS = "com/billy/cc/core/component/remote/RemoteProvider"

    public static final String MAIN_CC_SUB_PROCESS_FOLDER = "com/billy/cc/core/providers"

    static String getSubProcessProviderClassName(String processName) {
        processName = getSubProcessRealName(processName).replaceAll("\\.", "_")
        return "${MAIN_CC_SUB_PROCESS_FOLDER}/CC_Provider_${processName}"
    }

    static String getSubProcessRealName(String processName) {
        if (processName && processName.startsWith(":"))
            processName = processName.substring(1)
        return processName
    }

    /**
     * 生成RemoteProvider的子类
     * @param className 子类名称（根据
     * @param dir
     */
    static void generateProvider(String processName, File dir) {
        String className = getSubProcessProviderClassName(processName)
        File file = new File(dir, className + ".class")
        if (file.exists()) {
            return
        }
        println("${RegisterTransform.PLUGIN_NAME} generated a provider: ${file.absolutePath}")
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs()
        }
        file.createNewFile()
        byte[] bytes = generate(className, MULTI_PROCESS_PROVIDER_CLASS)
        FileOutputStream fos = new FileOutputStream(file)
        fos.write(bytes)
        fos.close()
    }

    /**
     * 用ASM生成指定类的子类
     * @param className 要生成的子类名称
     * @param superClassName 指定的父类名称
     * @return 生成的字节码
     * @throws Exception
     */
    static byte[] generate(String className, String superClassName) throws Exception {
        ClassWriter cw = new ClassWriter(0)
        cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, superClassName, null)
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        mv.visitCode()
        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", "()V", false)
        mv.visitInsn(RETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
        cw.visitEnd()
        return cw.toByteArray()
    }
}