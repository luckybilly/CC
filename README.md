# CC : ComponentCaller 

最新版本:[![Download](https://api.bintray.com/packages/hellobilly/android/cc/images/download.svg)](https://bintray.com/hellobilly/android/cc/_latestVersion)

## 特点
- 轻量级的android组件化开发框架


        aar包不到40KB
- 集成简单,仅需4步即可完成集成：


        添加自动注册插件
        添加apply cc-settings.gradle文件
        实现IComponent接口创建一个组件
        使用CC.obtainBuilder("component_name").build().call()调用组件
    
- 功能丰富


        1. 支持组件间相互调用（不只是Activity跳转，支持任意指令的调用/回调）
        2. 支持app间跨进程的组件调用(组件开发/调试时可单独作为app运行)
        3. 支持app间调用的开关及权限设置（满足不同级别的安全需求，默认打开状态且不需要权限）
        4. 支持同步/异步方式调用
        5. 支持同步/异步方式实现组件
        6. 调用方式不受实现方式的限制（例如:可以同步调用另一个组件的异步实现功能。注：不要在主线程同步调用耗时操作）
        7. 支持添加自定义拦截器（按添加的先后顺序执行）
        8. 支持超时设置
        9. 支持手动取消
        10. 编译时自动注册组件(IComponent)，无需手动维护组件注册表(使用ASM修改字节码的方式实现)
        11. 支持动态注册/反注册组件(IDynamicComponent)
        12. 支持组件间传递Fragment等非基础类型的对象（组件在同一个app内时支持、跨app传递非基础类型的对象暂不支持）

## 目录结构
- cc                        组件化框架基础库（主要）
- cc-settings.gradle        组件化开发构建脚本（主要）
- demo                      demo主程序
- demo_component_a          demo组件A
- demo_component_b          demo组件B
- component_protect_demo    添加跨app组件调用自定义权限限制的demo，在cc-settings-demo-b.gradle被依赖
- cc-settings-demo-b.gradle actionProcessor自动注册的配置脚本demo
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
CC.obtainBuilder("ComponentA").build().callAsync();
//或 异步调用，在子线程执行回调
CC.obtainBuilder("ComponentA").build().callAsync(new IComponentCallback(){...});
//或 异步调用，在主线程执行回调
CC.obtainBuilder("ComponentA").build().callAsyncCallbackOnMainThread(new IComponentCallback(){...});
```
#### 3. 进阶使用

- 设置Context信息    
```java
builder.setContext(activity)
```
- 超时时间设置     
```java
builder.setTimeout(1000)    
```
- 参数传递           
```java
builder.addParam("name", "billy").addParam("id", 12345)
```
- 设置返回信息        
```java
CCResult.success(key1, value1).addData(key2, value2)
```
- 发送结果给调用方     
```java
CC.sendCCResult(cc.getCallId(), ccResult)        
```
- 读取调用状态
```java
ccResult.isSuccess()
```        
- 读取调用错误信息
```java
ccResult.isSuccess()
```     
- 读取返回信息
```java
ccResult.getDataMap().get(key1)
```        
- 开启/关闭跨app调用
```java
CC.enableRemoteCC(trueOrFalse)
```            
- 开启/关闭debug日志打印
```java
CC.enableDebug(trueOrFalse);
```        
- 开启/关闭组件调用详细日志打印
```java
CC.enableVerboseLog(trueOrFalse);
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
    - 在该module的build.gradle中添加依赖： `compile 'com.billy.android:cc:0.1.0'`
    - 在该module的src/main/AndroidManifest.xml中设置权限及权限的级别，参考[component_protect_demo](https://github.com/luckybilly/CC/blob/master/component_protect_demo/src/main/AndroidManifest.xml)
    - 其它每个module都额外依赖此module，或自定义一个全局的cc-settings.gradle，参考[cc-settings-demo-b.gradle](https://github.com/luckybilly/CC/blob/master/cc-settings-demo-b.gradle)
    

##### 详情可参考 demo/demo_component_a/demo_component_b 中的示例


## 混淆配置

不需要额外的混淆配置

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
# 更新日志

- 2017.11.24 V0.1.0版 初次发布

