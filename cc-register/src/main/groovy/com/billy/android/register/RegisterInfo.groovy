package com.billy.android.register

import java.util.regex.Pattern
/**
 * aop的配置信息
 * @author billy.qi
 * @since 17/3/28 11:48
 */
class RegisterInfo {
    static final DEFAULT_EXCLUDE = [
            '.*/R(\\$[^/]*)?'
            , '.*/BuildConfig$'
    ]
    //以下是可配置参数
    String interfaceName = ''
    ArrayList<String> superClassNames = []
    String initClassName = ''
    String initMethodName = ''
    String registerClassName = ''
    String registerMethodName = ''
    ArrayList<String> include = []
    ArrayList<String> exclude = []

    //以下不是可配置参数
    ArrayList<Pattern> includePatterns = []
    ArrayList<Pattern> excludePatterns = []
    File fileContainsInitClass //initClassName的class文件或含有initClassName类的jar文件
    ArrayList<String> classList = new ArrayList<>()


    RegisterInfo(){}

    void reset() {
        fileContainsInitClass = null
        classList.clear()
    }

    boolean validate() {
        return interfaceName && registerClassName && registerMethodName
    }

    //用于在console中输出日志
    @Override
    String toString() {
        StringBuilder sb = new StringBuilder('{')
        sb.append('\n\t').append('scanInterface').append('\t\t\t=\t').append(interfaceName)
        sb.append('\n\t').append('scanSuperClasses').append('\t\t=\t[')
        for (int i = 0; i < superClassNames.size(); i++) {
            if (i > 0) sb.append(',')
            sb.append(' \'').append(superClassNames.get(i)).append('\'')
        }
        sb.append(' ]')
        sb.append('\n\t').append('codeInsertToClassName').append('\t=\t').append(initClassName)
        sb.append('\n\t').append('codeInsertToMethodName').append('\t=\t').append(initMethodName)
        sb.append('\n\t').append('registerMethodName').append('\t\t=\tpublic static void ')
                .append(registerClassName).append('.').append(registerMethodName)
        sb.append('\n\t').append('include').append(' = [')
        include.each { i ->
            sb.append('\n\t\t\'').append(i).append('\'')
        }
        sb.append('\n\t]')
        sb.append('\n\t').append('exclude').append(' = [')
        exclude.each { i ->
            sb.append('\n\t\t\'').append(i).append('\'')
        }
        sb.append('\n\t]\n}')
        return sb.toString()
    }

    void init() {
        if (include == null) include = new ArrayList<>()
        if (include.empty) include.add(".*") //如果没有设置则默认为include所有
        if (exclude == null) exclude = new ArrayList<>()
        if (!registerClassName)
            registerClassName = initClassName

        //将interfaceName中的'.'转换为'/'
        if (interfaceName)
            interfaceName = convertDotToSlash(interfaceName)
        //将superClassName中的'.'转换为'/'
        if (superClassNames == null) superClassNames = new ArrayList<>()
        for (int i = 0; i < superClassNames.size(); i++) {
            def superClass = convertDotToSlash(superClassNames.get(i))
            superClassNames.set(i, superClass)
        }
        //注册和初始化的方法所在的类默认为同一个类
        initClassName = convertDotToSlash(initClassName)
        //默认插入到static块中
        if (!initMethodName)
            initMethodName = "<clinit>"
        registerClassName = convertDotToSlash(registerClassName)
        //添加默认的排除项
        DEFAULT_EXCLUDE.each { e ->
            if (!exclude.contains(e))
                exclude.add(e)
        }
        initPattern(include, includePatterns)
        initPattern(exclude, excludePatterns)
    }

    private static String convertDotToSlash(String str) {
        return str ? str.replaceAll('\\.', '/').intern() : str
    }

    private static void initPattern(ArrayList<String> list, ArrayList<Pattern> patterns) {
        list.each { s ->
            patterns.add(Pattern.compile(s))
        }
    }
}