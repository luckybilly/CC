
### CC进阶使用

- 正式上线时禁用跨app调用组件

可在主app的application.onCreate中添加如下代码来禁用对跨app组件调用的支持

```java
//跨app调用支持：debug时启用，release时禁用
CC.enableRemoteCC(BuildConfig.DEBUG);
```
- 开启/关闭CC调试日志
```java
CC.enableDebug(trueOrFalse);
```

- 开启/关闭CC调用执行过程的跟踪日志
```java
CC.enableVerboseLog(trueOrFalse); 
```

- 根据callId取消CC调用
```java
CC.cancel(callId);
```

- 在组件中返回CCResult
```java
//设置成功的返回信息
CCResult ccResult = CCResult.success(key1, value1).addData(key2, value2);
//设置失败的返回信息
CCResult ccResult = CCResult.error(message).addData(key, value);
//发送结果给调用方 
CC.sendCCResult(cc.getCallId(), ccResult)

```    

- 发起链式调用时的参数设置

以下示例代码中的callAsync方法(异步调用)可以换成call方法(同步调用)

```java
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
```

- 解析组件调用的结果：CCResult
```java
//读取调用成功与否
ccResult.isSuccess()
//读取调用状态码(状态码对应的说明见README中的状态码清单)
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

```    

- 自定义拦截器

        1. 实现ICCInterceptor接口( 只有一个方法: intercept(Chain chain) )
        2. 调用chain.proceed()方法让调用链继续向下执行, 不调用以阻止本次CC
        2. 在调用chain.proceed()方法之前，可以修改cc的参数
        3. 在调用chain.proceed()方法之后，可以修改返回结果
        
    拦截器中获取/修改CC中的参数：
    ```java
    Map<String, Object> params = cc.getParams();//获取参数列表
    params.put(key, value); //修改参数
    Teacher teacher = cc.getParamItem(key); //语法糖：读取params中的item
    ```
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
