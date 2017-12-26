## CC : ComponentCaller (可关联生命周期的组件化开发框架)


##### [![Join the chat at https://gitter.im/billy_home/CC](https://badges.gitter.im/billy_home/CC.svg)](https://gitter.im/billy_home/CC?utm_source=share-link&utm_medium=link&utm_campaign=share-link)  [![Download](https://api.bintray.com/packages/hellobilly/android/cc/images/download.svg)](https://bintray.com/hellobilly/android/cc/_latestVersion) [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

技术方案原理: [![](https://badge.juejin.im/entry/5a2b94e251882531926ea50a/likes.svg?style=flat-square)](https://juejin.im/post/5a2b92e4f265da430d57f047)

## demo演示

[demo下载(包含主工程demo和demo_component_a组件)](https://github.com/luckybilly/CC/blob/master/demo-debug.apk)

[demo_component_b组件单独运行的App(Demo_B)下载](https://github.com/luckybilly/CC/blob/master/demo_component_b-debug.apk)

以上2个app用来演示组件打包在主app内和单独安装时的组件调用，都安装在手机上之后的运行效果如下图所示

        
        由于跨app通信采用系统广播机制，请在权限设置中给Demo_B开启'自启动'权限
        跨app通信仅用于开发期间单独运行组件app与主app间的通信

![image](https://raw.githubusercontent.com/luckybilly/CC/master/image/CC.gif)

## 特点
- 集成简单,仅需4步即可完成集成：


        添加自动注册插件
        添加apply cc-settings.gradle文件
        实现IComponent接口创建一个组件
        使用CC.obtainBuilder("component_name").build().call()调用组件
    
- 功能丰富


        1. 支持组件间相互调用（不只是Activity跳转，支持任意指令的调用/回调）
        2. 支持组件调用与Activity、Fragment的生命周期关联
        3. 支持app间跨进程的组件调用(组件开发/调试时可单独作为app运行)
        4. 支持app间调用的开关及权限设置（满足不同级别的安全需求，默认打开状态且不需要权限）
        5. 支持同步/异步方式调用
        6. 支持同步/异步方式实现组件
        7. 调用方式不受实现方式的限制（例如:可以同步调用另一个组件的异步实现功能。注：不要在主线程同步调用耗时操作）
        8. 支持添加自定义拦截器（按添加的先后顺序执行）
        9. 支持超时设置
        10. 支持手动取消
        11. 编译时自动注册组件(IComponent)，无需手动维护组件注册表(使用ASM修改字节码的方式实现)
        12. 支持动态注册/反注册组件(IDynamicComponent)
        13. 支持组件间传递Fragment、自定义View等（组件在同一个app内时支持、跨app传递非基础类型的对象暂不支持）
            13.1 不仅仅是获取Fragment、自定义View的对象，并支持后续的通信。
        14. 尽可能的解决了使用姿势不正确导致的crash，降低产品线上crash率： 
            14.1 组件调用处、回调处、组件实现处的crash全部在框架内部catch住
            14.2 同步返回或异步回调的CCResult对象一定不为null，避免空指针

- 老项目改造成本低


        有些同学担心原来老项目中代码耦合度高，改造成组件成本太高
        由于CC支持任意功能的调用并支持回调，所以，解决这个问题，CC只需2步：
        1. 创建一个组件类(IComponent实现类),提供原来通过类依赖实现的功能
        2. 然后将原来直接类调用改成CC调用方式
        
        
- 执行过程全程监控


        添加了详细的执行过程日志 
        详细日志打印默认为关闭状态，打开方式：CC.enableVerboseLog(true);
        
- 不需要初始化


        不需要在Application.onCreate中执行初始化方法
        不给负责优化App启动速度的同学添堵
## 目录结构

        - cc                            组件化框架基础库（主要）
        - cc-settings.gradle            组件化开发构建脚本（主要）
        - demo                          demo主程序
        - demo_component_a              demo组件A
        - demo_component_b              demo组件B
        - component_protect_demo        添加跨app组件调用自定义权限限制的demo，在cc-settings-demo-b.gradle被依赖
        - cc-settings-demo-b.gradle     actionProcessor自动注册的配置脚本demo
        - demo-debug.apk                demo安装包(包含demo/demo_component_a)
        - demo_component_b-debug.apk    demo组件B单独运行安装包

## 集成
下面介绍在Android Studio中进行集成的详细步骤

#### 1. 添加引用
1.1 在工程根目录的build.gradle中添加组件自动注册插件
 
```groovy
buildscript {
    dependencies {
        classpath 'com.billy.android:autoregister:1.0.4'
    }
}
```
1.2 在每个module(包括主app)的build.gradle中：

```groovy
apply plugin: 'com.android.library'
//或
apply plugin: 'com.android.application'

//替换成
apply from: 'https://raw.githubusercontent.com/luckybilly/CC/master/cc-settings.gradle'
//注意：最好放在build.gradle中代码的第一行

```
可参考[ComponentA的配置](https://github.com/luckybilly/CC/blob/master/demo_component_a/build.gradle)
 
1.2.1 默认组件为library，若组件module需要以app单独安装到手机上运行，有以下2种方式：

- 在工程根目录的 local.properties 中添加配置
```properties
module_name=true #module_name为具体每个module的名称
```
- 在module的build.gradle中添加 `ext.runAsApp = true`

    注意：需要添加到【步骤1.2】的`apply from: '.......'`之前

#### 2. 快速上手
2.1 定义组件(实现[IComponent](https://github.com/luckybilly/CC/blob/master/cc/src/main/java/com/billy/cc/core/component/IComponent.java)接口，需要保留无参构造方法)
```java
public class ComponentA implements IComponent {
    
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
        return false;
    }
}
```
2.2 调用组件
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
#### 3. 进阶使用

```java
//开启/关闭debug日志打印
CC.enableDebug(trueOrFalse);    
//开启/关闭组件调用详细日志打印
CC.enableVerboseLog(trueOrFalse); 
//开启/关闭跨app调用
CC.enableRemoteCC(trueOrFalse)  
//中止指定callId的组件调用任务
CC.cancel(callId)
//设置Context信息
CC.obtainBuilder("ComponentA")...setContext(context)...build().callAsync()
//关联Activity的生命周期，在onDestroy方法调用后自动执行cancel
CC.obtainBuilder("ComponentA")...cancelOnDestroyWith(activity)...build().callAsync()
//关联v4包Fragment的生命周期，在onDestroy方法调用后自动执行cancel
CC.obtainBuilder("ComponentA")...cancelOnDestroyWith(fragment)...build().callAsync()
//设置ActionName
CC.obtainBuilder("ComponentA")...setActionName(actionName)...build().callAsync()
//超时时间设置 
CC.obtainBuilder("ComponentA")...setTimeout(1000)...build().callAsync()
//参数传递
CC.obtainBuilder("ComponentA")...addParam("name", "billy").addParam("id", 12345)...build().callAsync()


//设置成功的返回信息
CCResult.success(key1, value1).addData(key2, value2)
//设置失败的返回信息
CCResult.error(message).addData(key, value)
//发送结果给调用方 
CC.sendCCResult(cc.getCallId(), ccResult)
//读取调用成功与否
ccResult.isSuccess()
//读取调用状态码(0:success, <0: 组件调用失败, 1:调用到了组件，业务执行判断失败，如调用登录组件最终登录失败)
ccResult.getCode()
//读取调用错误信息
ccResult.getErrorMessage()  
//读取返回的附加信息
Map<String, Object> data = ccResult.getDataMap();
if (data != null) {
    Object value = data.get(key)   
}
// 根据key从map中获取内容的便捷方式（自动完成类型转换，若key不存在则返回null）：
User user = ccResult.getDataItem(key); //读取CCResult.data中的item
Teacher teacher = cc.getParamItem(key); //读取CC.params中的item

```    
状态码清单：
```java
// CC调用成功
public static final int CODE_SUCCESS = 0;
// 组件调用成功，但业务逻辑判定为失败
public static final int CODE_ERROR_BUSINESS = 1;
// 保留状态码：默认的请求错误code
public static final int CODE_ERROR_DEFAULT = -1;
// 没有指定组件名称
public static final int CODE_ERROR_COMPONENT_NAME_EMPTY = -2;
/**
 * result不该为null
 * 例如：组件回调时使用 CC.sendCCResult(callId, null) 或 interceptor返回null
 */
public static final int CODE_ERROR_NULL_RESULT = -3;
// 调用过程中出现exception
public static final int CODE_ERROR_EXCEPTION_RESULT = -4;
// 没有找到组件能处理此次调用请求
public static final int CODE_ERROR_NO_COMPONENT_FOUND = -5;
// context 为null，通过反射获取application失败
public static final int CODE_ERROR_CONTEXT_NULL = -6;
// 跨app调用组件时，LocalSocket连接出错
public static final int CODE_ERROR_CONNECT_FAILED = -7;
// 取消
public static final int CODE_ERROR_CANCELED = -8;
// 超时
public static final int CODE_ERROR_TIMEOUT = -9;
// 未调用CC.sendCCResult(callId, ccResult)方法
public static final int CODE_ERROR_CALLBACK_NOT_INVOKED = -10;
```
- 自定义拦截器

        1. 实现ICCInterceptor接口( 只有一个方法: intercept(Chain chain) )
        2. 调用chain.proceed()方法让调用链继续向下执行, 不调用以阻止本次CC
        2. 在调用chain.proceed()方法之前，可以修改cc的参数
        3. 在调用chain.proceed()方法之后，可以修改返回结果
    可参考demo中的 [MissYouInterceptor.java](https://github.com/luckybilly/CC/blob/master/demo/src/main/java/com/billy/cc/demo/MissYouInterceptor.java)
- 动态注册/反注册组件
    
        定义：区别于静态组件(IComponent)编译时自动注册到ComponentManager，动态组件不会自动注册，通过手动注册/反注册的方式工作
        1. 动态组件需要实现接口: IDynamicComponent
        2. 需要手动调用 CC.registerComponent(component) , 类似于BroadcastReceiver动态注册
        3. 需要手动调用 CC.unregisterComponent(component), 类似于BroadcastReceiver动态反注册
        4. 其它用法跟静态组件一样
- 一个module中可以有多个组件类

        在同一个module中，可以有多个IComponent接口(或IDynamicComponent接口)的实现类
        IComponent接口的实现类会在编译时自动注册到组件管理类ComponentManager中
        IDynamicComponent接口的实现类会在编译时自动注册到组件管理类ComponentManager中
- 一个组件可以处理多个action

        在onCall(CC cc)方法中cc.getActionName()获取action来分别处理

    可参考：[ComponentA](https://github.com/luckybilly/CC/blob/master/demo_component_a/src/main/java/com/billy/cc/demo/component/a/ComponentA.java)
- 自定义的ActionProcessor自动注册到组件

    可参考[ComponentB](https://github.com/luckybilly/CC/blob/master/demo_component_b/src/main/java/com/billy/cc/demo/component/b/ComponentB.java)
    及[cc-settings-demo-b.gradle](https://github.com/luckybilly/CC/blob/master/cc-settings-demo-b.gradle)

- 给跨app组件的调用添加自定义权限限制
    - 新建一个module
    - 在该module的build.gradle中添加依赖： `compile 'com.billy.android:cc:x.x.x'`
    - 在该module的src/main/AndroidManifest.xml中设置权限及权限的级别，参考[component_protect_demo](https://github.com/luckybilly/CC/blob/master/component_protect_demo/src/main/AndroidManifest.xml)
    - 其它每个module都额外依赖此module，或自定义一个全局的cc-settings.gradle，参考[cc-settings-demo-b.gradle](https://github.com/luckybilly/CC/blob/master/cc-settings-demo-b.gradle)
   
- 跨组件获取Fragment、View等对象并支持后续与这些对象通信，以Fragment对象为例：
    - 在组件实现方通过`CCResult.addData(key, fragment)`将Fragment对象返回给调用方
    - 在组件调用方通过如下方式从CCResult中取出Fragment对象
        ```java
        Fragment fragment = (Fragment)CCResult.getDataMap().get(key)
        //或
        Fragment fragment = CCResult.getDataItem(key)
        ```
        
    后续通信方式可参考demo:
    在[ComponentA](https://github.com/luckybilly/CC/blob/master/demo_component_a/src/main/java/com/billy/cc/demo/component/a/ComponentA.java)中提供LifecycleFragment
    在[LifecycleActivity](https://github.com/luckybilly/CC/blob/master/demo/src/main/java/com/billy/cc/demo/lifecycle/LifecycleActivity.java)中获取LifecycleFragment
##### 详情可参考 demo/demo_component_a/demo_component_b 中的示例


## 混淆配置

不需要额外的混淆配置

## 自动注册插件
源码:[AutoRegister](https://github.com/luckybilly/AutoRegister)
原理:[android扫描接口实现类并通过修改字节码自动生成注册表](http://blog.csdn.net/cdecde111/article/details/78074692)
## Q&A

- 无法调用到组件

    
        1. 请按照本文档的集成说明排查
        2. 请确认调用的组件名称(CC.obtainBuilder(componentName)与组件类定定义的名称(getName()的返回值)是否一致
        3. 请确认actionName是否与组件中定义的一致


- 调用异步实现的组件时，IComponentCallback.onResult方法没有执行


        1. 请检查组件实现的代码中是否每个逻辑分支是否最终都会调用CC.sendCCResult(...)方法
            包括if-else/try-catch/switch-case/按返回键或主动调用finish()等情况
        2. 请检查组件实现的代码中该action分支是否返回为true 
            返回值的意义在于告诉CC引擎：调用结果是否异步发送(执行CC.sendCCResult(...)方法)
        
- 跨app调用组件时，onCall方法执行到了startActivity，但页面没打开

    
        1. 请在手机系统的权限管理中对组件所在的app赋予自启动权限
        2. 请检查被调用的app里是否设置了CC.enableRemoteCC(false)，应该设置为true(默认值也为true)

- 使用ActionProcessor来处理多个action，单独组件作为apk运行时能正常工作，打包到主app中则不能正常工作

    ```groovy
    //使用自定义的cc-settings.gradle文件时，主工程也要依赖此gradle文件替换
    apply from: 'https://raw.githubusercontent.com/luckybilly/CC/master/cc-settings.gradle'
    ```
    参考[demo/build.gradle](https://github.com/luckybilly/CC/blob/master/demo/build.gradle)中的配置

- 使用自定义权限后，不能正常进行跨app调用


        每个组件都必须依赖自定义权限的module
        若对该module的依赖在自定义cc-settings.gradle中，则每个组件都要apply这个gradle
参考[demo/build.gradle](https://github.com/luckybilly/CC/blob/master/demo/build.gradle)中的配置

- 如何实现context.startActivityForResult的功能

    
        1. 使用方式同普通页面跳转
        2. 在onCall方法里返回true
        3. 在跳转的Activity中回调信息时，不用setResult, 通过CC.sendCCResult(callId, result)来回调结果
        4. 调用组件时，使用cc.callAsyncCallbackOnMainThread(new IComponentCallback(){...})来接收返回结果

# 更新日志

- 2017.12.23 V0.3.1版

        
        1. 为获取CC和CCResult对象中Map里的对象提供便捷方法，无需再进行类型判断和转换
            WhateEverClass classObj = cc.getParamItem(String key, WhateEverClass defaultValue)
            WhateEverClass classObj = cc.getParamItem(String key)
            WhateEverClass classObj = ccResult.getDataItem(String key, WhateEverClass defaultValue)
            WhateEverClass classObj = ccResult.getDataItem(String key)
        2. demo中新增演示跨组件获取Fragment对象
            
        

- 2017.12.09 V0.3.0版

    
        添加Activity及Fragment生命周期关联的功能并添加对应的demo

- 2017.11.29 V0.2.0版


        将跨进程调用接收LocalSocket信息的线程放入到线程池中
        完善demo
        
- 2017.11.27 V0.1.1版
    
    
        1. 优化超时的处理流程
        2. 优化异步返回CCResult的处理流程

- 2017.11.24 V0.1.0版 初次发布

