## CC : ComponentCaller (基于组件总线的安卓组件化框架)

【[English README](README-en-US.md)】

[![Join the chat at https://gitter.im/billy_home/CC](https://badges.gitter.im/billy_home/CC.svg)](https://gitter.im/billy_home/CC?utm_source=share-link&utm_medium=link&utm_campaign=share-link)

模块|CC|AutoRegister
---|---|---
最新版本| [![Download](https://api.bintray.com/packages/hellobilly/android/cc/images/download.svg)](https://bintray.com/hellobilly/android/cc/_latestVersion)| [![Download](https://api.bintray.com/packages/hellobilly/android/AutoRegister/images/download.svg)](https://bintray.com/hellobilly/android/AutoRegister/_latestVersion)


技术原理: [Wiki](https://github.com/luckybilly/CC/wiki)

## demo演示

[demo下载(包含主工程demo和demo_component_a组件)](https://github.com/luckybilly/CC/raw/master/demo-debug.apk)

[demo_component_b组件单独运行的App(Demo_B)下载](https://github.com/luckybilly/CC/raw/master/demo_component_b-debug.apk)

以上**2个app**用来演示组件打包在主app内和**单独以app运行**时的组件调用，**都安装在手机上**之后的运行效果如下图所示


![image](https://raw.githubusercontent.com/luckybilly/CC/master/image/CC.gif)

## 快速理解CC

定义组件：将自身的业务(页面跳转及服务调用等)封装起来提供给外部调用，并返回执行的结果

调用组件：根据组件名称、业务名称及其它参数调用指定组件的指定业务，并获得执行的结果

组件将业务完全隔离在自身内部，仅暴露组件名称(ComponentName)、业务名称(actionName)、参数列表及返回值等信息给外部调用

## 使用CC的理由


- 集成简单,仅需5步即可完成集成：


        在根目录build.gradle中添加自动注册插件
        添加apply cc-settings.gradle文件
        实现IComponent接口创建一个组件
        使用CC.obtainBuilder("component_name").build().call()调用组件
        在主app中添加对组件module的依赖,如: addComponent 'demo_component_a'
- 完全的代码隔离：CC支持跨app调用组件，开发时组件之间无需互相依赖


        组件以app方式独立运行时不需要依赖任何其它组件，从源头上隔离代码
        无需担心与主app的相互调用，从一开始组件化改造就可以单组件运行
        跟打包在主app中运行是一样的效果，能大大降低组件化改造的难度

- 改造成本低：接入时可基本不改原有代码，原有组件的拆分工作不影响整体组件化改造，[参考文章](https://github.com/luckybilly/CC/wiki/%E8%B0%81%E9%98%BB%E7%A2%8D%E4%BA%86%E4%BD%A0%E5%81%9A%E7%BB%84%E4%BB%B6%E5%8C%96%E5%BC%80%E5%8F%91%EF%BC%9F)
- 组件层面的AOP支持：可以在组件内部AOP完成登录验证和权限验证等功能，调用方无需关注，[参考文章](https://github.com/luckybilly/CC/wiki/CC%E6%A1%86%E6%9E%B6%E5%AE%9E%E8%B7%B5(1)%EF%BC%9A%E5%AE%9E%E7%8E%B0%E7%99%BB%E5%BD%95%E6%88%90%E5%8A%9F%E5%86%8D%E8%BF%9B%E5%85%A5%E7%9B%AE%E6%A0%87%E7%95%8C%E9%9D%A2%E5%8A%9F%E8%83%BD)
- Fragment/View的组件化支持：支持组件调用方式获取及后续的功能调用，业务完全内聚，[参考文章](https://github.com/luckybilly/CC/wiki/CC%E6%A1%86%E6%9E%B6%E5%AE%9E%E8%B7%B5(2)%EF%BC%9AFragment%E5%92%8CView%E7%9A%84%E7%BB%84%E4%BB%B6%E5%8C%96)
- 对Push及jsBridge友好：直接转发对组件的调用即可，[参考文章](https://github.com/luckybilly/CC/wiki/CC%E6%A1%86%E6%9E%B6%E5%AE%9E%E8%B7%B5(3):-%E8%AE%A9jsBridge%E6%9B%B4%E4%BC%98%E9%9B%85)
- 免维护组件列表：使用gradle插件实现组件的自动注册，插拔组件只需修改dependencies依赖即可
- 极低的学习成本，便于推广使用：只需了解一个接口和一个静态方法即可定义组件，只需了解一个链式调用即可调用组件
- 统一的定义方式和调用方式：面向协议来实现和调用组件，类似于移动端跟服务端的通信协议


了解业界开源的一些组件化方案：[多个维度对比一些有代表性的开源android组件化开发方案](https://github.com/luckybilly/AndroidComponentizeLibs) 

## CC功能列表


        1. 支持组件间相互调用（不只是Activity跳转，支持任意指令的调用/回调）
        2. 支持组件调用与Activity、Fragment的生命周期关联
        3. 支持app间跨进程的组件调用(组件开发/调试时可单独作为app运行)
        4. 支持app间调用的开关及权限设置（默认为关闭状态，调用CC.enableRemoteCC(true)打开）
        5. 支持同步/异步方式调用
        6. 支持同步/异步方式实现组件
        7. 调用方式不受实现方式的限制（例如:可以同步调用另一个组件的异步实现功能。注：不要在主线程同步调用耗时操作）
        8. 支持添加自定义拦截器（按添加的先后顺序执行）
        9. 支持超时设置
        10. 支持手动取消
        11. 编译时自动注册组件(IComponent)，无需手动维护组件注册表(使用ASM修改字节码的方式实现)
        12. 支持动态注册/反注册组件(IDynamicComponent)
        13. 支持组件间传递Fragment、自定义View等（在同一个进程内传递）
            13.1 不仅仅是获取Fragment、自定义View的对象，并支持后续的通信。
        14. 尽可能的解决了使用姿势不正确导致的crash，降低产品线上crash率： 
            14.1 组件调用处、回调处、组件实现处的crash全部在框架内部catch住
            14.2 同步返回或异步回调的CCResult对象一定不为null，避免空指针

## 目录结构

        - cc                            组件化框架基础库（主要）
        - cc-settings.gradle            组件化开发构建脚本（主要）
        - demo                          demo主程序
        - demo_base                     demo公共库(base类、util类、公共Bean等)
        - demo_component_a              demo组件A
        - demo_component_b              demo组件B
        - demo_component_kt             demo组件(kotlin)
        - demo_interceptors             demo全局拦截器(如果有多个app并且拦截器不同，可以创建多个module给不同app使用)
        - component_protect_demo        添加跨app组件调用自定义权限限制的demo，在cc-settings-demo-b.gradle被依赖
        - cc-settings-demo-b.gradle     演示如何自定义配置文件，如：添加actionProcessor自动注册的配置
        - demo-debug.apk                demo安装包(包含demo/demo_component_a/demo_component_kt)
        - demo_component_b-debug.apk    demo组件B单独运行安装包

## 集成(共5步)
下面介绍在Android Studio中进行集成的详细步骤

#### 1. 添加引用
在工程根目录的build.gradle中添加组件自动注册插件

```groovy
buildscript {
    dependencies {
        classpath 'com.billy.android:autoregister:x.x.x'
    }
}
```

#### 2. 在每个组件module(包括主app)的build.gradle中：

```groovy
apply plugin: 'com.android.library'
//或
apply plugin: 'com.android.application'

//替换成
//ext.mainApp = true //设置为true，表示此module为主app module，一直以application方式编译
apply from: 'https://raw.githubusercontent.com/luckybilly/CC/master/cc-settings.gradle'
//注意：最好放在build.gradle中代码的第一行

```

默认组件module为library，若组件需要以app单独安装到手机上运行，可以在工程根目录的`local.properties`中添加配置
```properties
module_name=true #module_name为具体每个module的名称，设置为true代表以application方式编译
```

可参考[主app module: demo/build.gradle的配置](https://github.com/luckybilly/CC/blob/master/demo/build.gradle)
和[组件module: demo_component_a/build.gradle的配置](https://github.com/luckybilly/CC/blob/master/demo_component_a/build.gradle)


#### 3. 实现IComponent接口创建组件

创建组件(实现[IComponent](https://github.com/luckybilly/CC/blob/master/cc/src/main/java/com/billy/cc/core/component/IComponent.java)接口，需要保留无参构造方法)
```java
public class ComponentA implements IComponent {
    //需保留无参构造方法
    
    @Override
    public String getName() {
        //组件的名称，调用此组件的方式：
        // CC.obtainBuilder("ComponentA").build().callAsync()
        return "ComponentA";
    }

    @Override
    public boolean onCall(CC cc) {
        Context context = cc.getContext();
        Intent intent = new Intent(context, ActivityComponentA.class);
        if (!(context instanceof Activity)) {
            //调用方没有设置context或app间组件跳转，context为application
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        //发送组件调用的结果（返回信息）
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        //返回值说明
        // false: 组件同步实现（onCall方法执行完之前会将执行结果CCResult发送给CC）
        // true: 组件异步实现（onCall方法执行完之后再将CCResult发送给CC，CC会持续等待组件调用CC.sendCCResult发送的结果，直至超时）
        return false;
    }
}
```
#### 4. 调用组件
```java
//同步调用，直接返回结果
CCResult result = CC.obtainBuilder("ComponentA").build().call();
//或 异步调用，不需要回调结果
String callId = CC.obtainBuilder("ComponentA").build().callAsync();
//或 异步调用，在子线程执行回调
String callId = CC.obtainBuilder("ComponentA").build().callAsync(new IComponentCallback(){...});
//或 异步调用，在主线程执行回调
String callId = CC.obtainBuilder("ComponentA").build().callAsyncCallbackOnMainThread(new IComponentCallback(){...});
```

更多使用方式请戳[这里](docs/Usage.md)

#### 5. 在主app module中按如下方式添加对所有组件module的依赖

注意：组件之间不要互相依赖

```groovy
apply from: 'https://raw.githubusercontent.com/luckybilly/CC/master/cc-settings.gradle'

//...

dependencies {
    addComponent 'demo_component_a' //会默认添加依赖：project(':demo_component_a')
    addComponent 'demo_component_kt', project(':demo_component_kt') //module方式
    addComponent 'demo_component_b', 'com.billy.demo:demo_b:1.1.0'  //maven方式
}
```

用法示例见[demo](https://github.com/luckybilly/CC/blob/master/demo/build.gradle)

按照此方式添加的依赖有以下特点：

- 方便：组件切换library和application方式编译时，只需在local.properties中进行设置，不需要修改app module中的依赖列表
    - 运行主app module时会自动将【设置为以app方式编译的组件module】从依赖列表中排除
- 安全：避免调试时切换library和application方式修改主app中的依赖项被误提交到代码仓库，导致jenkins集成打包时功能缺失
- 隔离：避免直接调用组件中的代码及资源

注意：

CC会优先调用app内部的组件，只有在内部找不到对应组件且设置`CC.enableRemoteCC(true)`时才会尝试进行跨app组件调用

所以，单组件以app运行调试时，如果主app要主动与此组件进行通信，请确保主app中没有包含此组件(Tips:最简单的方式是重新Run一次主app module)

## 状态码清单

| 状态码        | 说明    |
| --------   | :----- |
| 0 | CC调用成功 |
| 1 | CC调用成功，但业务逻辑判定为失败 |
| -1 | 保留状态码：默认的请求错误code |
| -2 | 没有指定组件名称 |
| -3 | result不该为null。例如：组件回调时使用 CC.sendCCResult(callId, null) 或 interceptor返回null |
| -4 | 调用过程中出现exception，请查看logcat |
| -5 | 指定的ComponentName没有找到 |
| -6 | context为null，通过反射获取application失败，出现这种情况可以用CC.init(application)来初始化 |
| -7 | 跨app调用组件时，LocalSocket连接出错 |
| -8 | 已取消 |
| -9 | 已超时 |
| -10 | component.onCall(cc) return false, 未调用CC.sendCCResult(callId, ccResult)方法 |
| -11 | 跨app组件调用时对象传输出错，可能是自定义类型没有共用，请查看Logcat |


## 混淆配置

不需要额外的混淆配置

## 自动注册插件
源码:[AutoRegister](https://github.com/luckybilly/AutoRegister)
原理:[android扫描接口实现类并通过修改字节码自动生成注册表](http://blog.csdn.net/cdecde111/article/details/78074692)


## [更新日志](docs/ChangeLog.md)

## 遇到问题怎么办？

- 先打开CC的日志开关，看完整的调用过程日志，这往往能帮助我们找到问题
```java
CC.enableDebug(true);  //普通调试日志，会提示一些错误信息
CC.enableVerboseLog(true);  //组件调用的详细过程日志，用于跟踪整个调用过程
```
- 看[Wiki](https://github.com/luckybilly/CC/wiki)
- 看[常见问题](docs/Q&A.md)
- 看[issue](https://github.com/luckybilly/CC/issues)
- 加下方的QQ群提问

## QQ群

QQ群号：686844583  

<a target="_blank" href="http://shang.qq.com/wpa/qunwpa?idkey=5fdd1171114b5a1eb80ea0be00b392c2e3e8ab6f278f182a07e959e80d4c9409"><img border="0" src="http://pub.idqqimg.com/wpa/images/group.png" alt="CC交流群" title="CC交流群"></a>

或者扫描下方二维码加群聊

![image](image/CC_QQ.png)


