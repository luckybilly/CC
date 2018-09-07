package com.billy.android.register

import org.apache.commons.io.IOUtils
import org.objectweb.asm.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
/**
 *
 * @author billy.qi
 * @since 17/3/20 11:48
 */
class CodeInsertProcessor {
    RegisterInfo extension

    private CodeInsertProcessor(RegisterInfo extension) {
        this.extension = extension
    }

    static void insertInitCodeTo(RegisterInfo extension) {
        if (extension != null && !extension.classList.isEmpty()) {
            CodeInsertProcessor processor = new CodeInsertProcessor(extension)
            File file = extension.fileContainsInitClass
            if (file.getName().endsWith('.jar'))
                processor.generateCodeIntoJarFile(file)
            else
                processor.generateCodeIntoClassFile(file)
        }
    }

    //处理jar包中的class代码注入
    private File generateCodeIntoJarFile(File jarFile) {
        if (jarFile) {
            def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")
            if (optJar.exists())
                optJar.delete()
            def file = new JarFile(jarFile)
            Enumeration enumeration = file.entries()
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar))

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = file.getInputStream(jarEntry)
                jarOutputStream.putNextEntry(zipEntry)
                if (isInitClass(entryName)) {
                    println('generate code into:' + entryName)
                    def bytes = doGenerateCode(inputStream)
                    jarOutputStream.write(bytes)
                } else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                inputStream.close()
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            file.close()

            if (jarFile.exists()) {
                jarFile.delete()
            }
            optJar.renameTo(jarFile)
        }
        return jarFile
    }

    boolean isInitClass(String entryName) {
        if (entryName == null || !entryName.endsWith(".class"))
            return false
        if (extension.initClassName) {
            entryName = entryName.substring(0, entryName.lastIndexOf('.'))
            return extension.initClassName == entryName
        }
        return false
    }
    /**
     * 处理class的注入
     * @param file class文件
     * @return 修改后的字节码文件内容
     */
    private byte[] generateCodeIntoClassFile(File file) {
        def optClass = new File(file.getParent(), file.name + ".opt")

        FileInputStream inputStream = new FileInputStream(file)
        FileOutputStream outputStream = new FileOutputStream(optClass)

        def bytes = doGenerateCode(inputStream)
        outputStream.write(bytes)
        inputStream.close()
        outputStream.close()
        if (file.exists()) {
            file.delete()
        }
        optClass.renameTo(file)
        return bytes
    }

    private byte[] doGenerateCode(InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ClassVisitor cv = new MyClassVisitor(Opcodes.ASM5, cw)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    class MyClassVisitor extends ClassVisitor {

        MyClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        void visit(int version, int access, String name, String signature,
                   String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
        }
        @Override
        MethodVisitor visitMethod(int access, String name, String desc,
                                  String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
            if (name == extension.initMethodName) { //注入代码到指定的方法之中
                boolean _static = (access & Opcodes.ACC_STATIC) > 0
                mv = new MyMethodVisitor(Opcodes.ASM5, mv, _static)
            }
            return mv
        }
    }

    class MyMethodVisitor extends MethodVisitor {
        boolean _static;

        MyMethodVisitor(int api, MethodVisitor mv, boolean _static) {
            super(api, mv)
            this._static = _static;
        }

        @Override
        void visitInsn(int opcode) {
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
                extension.classList.each { name ->
                    if (!_static) {
                        //加载this
                        mv.visitVarInsn(Opcodes.ALOAD, 0)
                    }
                    //用无参构造方法创建一个组件实例
                    mv.visitTypeInsn(Opcodes.NEW, name)
                    mv.visitInsn(Opcodes.DUP)
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, name, "<init>", "()V", false)
                    //调用注册方法将组件实例注册到组件库中
                    if (_static) {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC
                                , extension.registerClassName
                                , extension.registerMethodName
                                , "(L${extension.interfaceName};)V"
                                , false)
                    } else {
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL
                                , extension.registerClassName
                                , extension.registerMethodName
                                , "(L${extension.interfaceName};)V"
                                , false)
                    }
                }
            }
            super.visitInsn(opcode)
        }
        @Override
        void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}