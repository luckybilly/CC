package com.billy.android.register.generator

import org.objectweb.asm.*

/**
 * 生成provider类
 */
class ProviderGenerator implements Opcodes {
    public static final String MULTI_PROCESS_PROVIDER_CLASS = "com/billy/cc/core/component/remote/RemoteProvider"

    static void generateProvider(String className, File dir) {
        File file = new File(dir, className + ".class")
        if (file.exists()) {
            file.delete()
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs()
        }
        file.createNewFile()
        byte[] bytes = generate(className, MULTI_PROCESS_PROVIDER_CLASS)
        FileOutputStream fos = new FileOutputStream(file)
        fos.write(bytes)
        fos.close()
    }

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