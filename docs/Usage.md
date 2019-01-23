
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
    //添加拦截器
    CC.obtainBuilder("ComponentA")...addInterceptor(new MyInterceptor())...build().callAsync()
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

- 自定义拦截器[ICCInterceptor](../cc/src/main/java/com/billy/cc/core/component/ICCInterceptor.java)

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
    可参考demo中的 [MissYouInterceptor.java](../demo/src/main/java/com/billy/cc/demo/MissYouInterceptor.java)
- 全局拦截器
    - 全局拦截器是一种特殊的自定义拦截器，顾名思义，它作用于全局每一次CC调用，通过实现[IGlobalCCInterceptor](../cc/src/main/java/com/billy/cc/core/component/IGlobalCCInterceptor.java)接口来定义，cc-register插件会自动将其注册到[GlobalCCInterceptorManager](../cc/src/main/java/com/billy/cc/core/component/GlobalCCInterceptorManager.java)中
    - CC框架内，所有拦截器的执行顺序为：
        - 自定义全局拦截器（按priority从大到小排序）
        - 自定义拦截器（通过在链式调用中添加addInterceptor来设置，按添加顺序的先后来排序）
        - [ValidateInterceptor](../cc/src/main/java/com/billy/cc/core/component/ValidateInterceptor.java)
        - [LocalCCInterceptor](../cc/src/main/java/com/billy/cc/core/component/LocalCCInterceptor.java) / [SubProcessCCInterceptor](../cc/src/main/java/com/billy/cc/core/component/SubProcessCCInterceptor.java) / [RemoteCCInterceptor](../cc/src/main/java/com/billy/cc/core/component/RemoteCCInterceptor.java)
        - [Wait4ResultInterceptor](../cc/src/main/java/com/billy/cc/core/component/Wait4ResultInterceptor.java)
        
- 注册/反注册动态组件
    
        定义：区别于静态组件(IComponent)编译时自动注册到ComponentManager，动态组件不会自动注册，通过手动注册/反注册的方式工作
        1. 动态组件需要实现接口: IDynamicComponent
        2. 需要手动调用 CC.registerComponent(component) , 类似于BroadcastReceiver动态注册
        3. 需要手动调用 CC.unregisterComponent(component), 类似于BroadcastReceiver动态反注册
        4. 其它用法跟静态组件一样
    可参考[LoginUserObserverComponent](../demo/src/main/java/com/billy/cc/demo/MainActivity.java)
- 一个module中可以有多个组件类

        在同一个module中，可以有多个IComponent接口(或IDynamicComponent接口)的实现类
        IComponent接口的实现类会在编译时自动注册到组件管理类ComponentManager中
        IDynamicComponent接口的实现类会在编译时自动注册到组件管理类ComponentManager中
- 一个组件可以处理多个action

        在onCall(CC cc)方法中cc.getActionName()获取action来分别处理

    可参考：[ComponentA](../demo_component_a/src/main/java/com/billy/cc/demo/component/a/ComponentA.java)
- 自定义的ActionProcessor自动注册到组件

    可参考[ComponentB](../demo_component_b/src/main/java/com/billy/cc/demo/component/b/ComponentB.java)
    及[cc-settings-demo.gradle](../cc-settings-demo.gradle)

   
- 跨组件获取Fragment、View等对象并支持后续与这些对象通信，以Fragment对象为例：
    - 在组件实现方通过`CCResult.addData(key, fragment)`将Fragment对象返回给调用方
    - 在组件调用方通过如下方式从CCResult中取出Fragment对象
        ```java
        Fragment fragment = (Fragment)CCResult.getDataMap().get(key)
        //或
        Fragment fragment = CCResult.getDataItem(key)
        ```
        
    后续通信方式可参考demo:
    在[ComponentA](../demo_component_a/src/main/java/com/billy/cc/demo/component/a/ComponentA.java)中提供LifecycleFragment
    在[LifecycleActivity](../demo/src/main/java/com/billy/cc/demo/lifecycle/LifecycleActivity.java)中获取LifecycleFragment

- 跨进程调用时的参数转换工具：
  
  跨进程（包括app内部跨进程和跨app）调用组件时，自定义类型的参数会被转换为json格式进行传递，需要提供一个自定义的json转换工具
  
  实现方式为：在基础库中提供一个实现[IParamJsonConverter](../cc/src/main/java/com/billy/cc/core/component/IParamJsonConverter.java)接口的类即可，cc-register插件自动将其注册到[RemoteParamUtil](../cc/src/main/java/com/billy/cc/core/component/remote/RemoteParamUtil.java)中
  
  参考[GsonParamConverter](../demo_base/src/main/java/com/billy/cc/demo/base/GsonParamConverter.java)

- 开启App内部多进程支持
    注：默认情况下未开启App内部多进程的支持
    - 启用App内部多进程
        可下载`cc-settings-2.gradle`文件到本地根目录，并在文件最后添加：
        ```groovy
        ccregister.multiProcessEnabled = true
        ```
        并在组件类（`IComponent`实现类）上添加一个注解，标明其所在进程（在主进程运行组件无需添加注解）
        __注意：这样做并不是创建新的进程，而是指定此组件在哪个进程运行（如果AndroidManifest.xml中没有对应的进程，此组件无效）__
        ```java
        public class DemoComponent implements IComponent{} //DemoComponent组件在主进程运行
        @SubProcess(":yourProcessName") //指定DemoComponentA组件所在的进程名称为 'packageName:yourProcessName'
        public class DemoComponentA implements IComponent{}
        @SubProcess("a.b.c") //指定DemoComponentB组件所在的进程名称为 'a.b.c'
        public class DemoComponentB implements IComponent{}
        @AllProcess         //指定DemoComponentC组件在主进程和所有子进程内都存在，每个进程调用进程内部的DemoComponentC组件
        public class DemoComponentC implements IComponent{}
        ```
    - 排除App中没有组件的进程名称
        为了支持多进程通信，CCRegister插件会在编译时扫描合并后的`AndroidManifest.xml`文件中所有四大组件
        收集所有子进程名称，为每个子进程生成一个`RemoteProvider`的子类并注册到`AndroidManifest.xml`文件中

            这样做是因为：
            如果放在Transform.transform方法中修改，在扫描完class代码之后再修改AndroidManifest.xml，无效
            欢迎提PR优化
    
        这将导致一些不含有子进程组件的进程也会生成一个没有任何作用的`RemoteProvider`的子类，这会额外带来一点点内存消耗。
        虽然这种内存消耗是可以基本忽略的，但是还是可以通过如下方式添加配置来避免：
        ```groovy
        //在cc-settings-2.gradle中添加以下配置来指定这些进程将不生成对应的RemoteProvider子类
        ccregister.excludeProcessNames = [':processNameA', ':processNameB']
        ```
    - 动态组件不支持`@SubProcess`及`@AllProcess`注解
        动态组件在其被注册的进程中运行，如：在主进程中调用`CC.registerComponent(dynamicComponent);`,dynamicComponent将在主进程中运行
    
- 实现组件时指定onCall方法是否在主线程运行
    在实现IComponent或IDynamicComponent接口的基础上，额外实现接口[IMainThread](../cc/src/main/java/com/billy/cc/core/component/IMainThread.java)
    即可通过`shouldActionRunOnMainThread`方法为不同actionName及参数指定onCall方法是否在主线程运行
    ```java
    public class ComponentB implements IComponent, IMainThread {
        //...
        @Override
        public boolean onCall(CC cc) {
            //...
            return false;
        }
        @Override
        public Boolean shouldActionRunOnMainThread(String actionName, CC cc) {
            //可通过actionName及cc的参数来为不同的情况指定是否在主线程运行
            if ("login".equals(actionName)) {
                return true;
            }
            //返回null表示默认状态：不固定运行的线程（在主线程同步调用时在主线程运行，其它情况下在子线程运行）
            return null;
        }
    }
    ```
    可参考[ComponentB](../demo_component_b/src/main/java/com/billy/cc/demo/component/b/ComponentB.java)
    和[LoginUserObserverComponent](../demo/src/main/java/com/billy/cc/demo/MainActivity.java)

- CCUtil工具类
    CC框架内部提供了一个工具类[CCUtil](../cc/src/main/java/com/billy/cc/core/component/CCUtil.java)，以简化在组件内进行页面跳转的代码：
    - 跳转到指定activity并携带callId及参数： public static void navigateTo(CC cc, Class<? extends Activity> activityClass)
    - 在activity中获取上一步跳转时携带的参数： public static <T> T getNavigateParam(@NonNull Activity activity, String key, T defaultValue)
    - 在activity中获取跳转时携带的callId： public static String getNavigateCallId(@NonNull Activity activity)

- 打aar包
    
    组件module应用了cc-register插件后，默认情况下通过assemble命令打包是打apk包，若要打aar包，可用如下方式来实现：
    
    - 先在local.properties中添加`assemble_aar_for_cc_component=true`
    - 再通过assemble命令或点击android studio上的绿色Run按钮来打 __aar包__
    - 打完aar包之后，将local.properties中`assemble_aar_for_cc_component=true`改为false或者直接注释掉
    - 再通过assemble命令或点击android studio上的绿色Run按钮来打 __apk包__

##### 详情可参考demo代码中的示例
