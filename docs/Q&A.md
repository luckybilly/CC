
## 常见问题

- 运行demo时调用ComponentB出现页面打不开

	- 2.0.0版以前，请参考[issue](https://github.com/luckybilly/CC/issues/5)
	- 2.0.0版以后，有些高版本手机的权限管理中有"后台弹出界面"的权限设置（比如vivo手机：设置-更多设置-权限管理），给CC和Demo_B开启这个权限即可


- 无法调用到组件
    
    1. 请按照README文档的集成说明排查
    2. 请确认调用的组件名称(CC.obtainBuilder(componentName)与组件类定定义的名称(getName()的返回值)是否一致
    3. 请确认actionName是否与组件中定义的一致
    4. 开发阶段，若跨app调用失败(错误码: -5)，请确认在调用组件之前是否启用了跨app组件调用功能：CC.enableRemoteCC(true);
    5. 请检查gradle console，查看cc-register打印的自动注册日志,示例如下：
    ```
    insert register code to file:/Users/billy/Documents/github/CC/demo/build/intermediates/transforms/cc-register/release/0.jar
    com/billy/cc/demo/component/a/ComponentA
    com/billy/cc/demo/component/kt/KtComponent
    com/billy/cc/demo/lifecycle/LifecycleComponent
    com/billy/cc/demo/component/jsbridge/JsBridgeComponent
    com/billy/cc/demo/component/jsbridge/WebComponent
    generate code into:com/billy/cc/core/component/ComponentManager.class
    
    insert register code to file:/Users/billy/Documents/github/CC/demo/build/intermediates/transforms/cc-register/release/0.jar
    com/billy/cc/demo/interceptors/LogInterceptor
    generate code into:com/billy/cc/core/component/GlobalCCInterceptorManager.class
    
    insert register code to file:/Users/billy/Documents/github/CC/demo/build/intermediates/transforms/cc-register/release/0.jar
    com/billy/cc/demo/base/GsonParamConverter
    generate code into:com/billy/cc/core/component/remote/RemoteParamUtil.class
    cc-register insert code cost time: 59 ms
    cc-register generated a provider: /Users/billy/Documents/github/CC/demo/build/intermediates/transforms/cc-register/release/26/com/billy/cc/core/providers/CC_Provider_web.class
    cc-register cost time: 260 ms

    ```

- 组件作为app独立运行调试，在主app中调用该组件时，该组件中新增/修改的代码未生效


        出现这个问题的原因是：
            主APP打包时已经将该独立运行的组件包含在内了，调用组件时优先调用app内部的组件，从而忽略了独立运行的组件
        解决方法：
        1. 在local.properties中新增一行配置 modulename=true //注：modulename为独立运行组件的module名称
        2. 重新打包运行一次主app module（目的是为了将独立运行的组件module从主app的依赖列表中排除）

- 调用异步实现的组件时，IComponentCallback.onResult方法没有执行


        1. 请检查组件实现的代码中是否每个逻辑分支是否最终都会调用CC.sendCCResult(...)方法
            包括if-else/try-catch/switch-case/按返回键或主动调用finish()等情况
        2. 请检查组件实现的代码中该action分支是否返回为true 
            返回值的意义在于告诉CC引擎：调用结果是否异步发送(执行CC.sendCCResult(...)方法)
        
- 跨app调用组件时，onCall方法执行到了startActivity，但页面没打开

    
        1. 请在手机系统的权限管理中对组件所在的app赋予自启动权限
        2. 请检查被调用的app里是否设置了CC.enableRemoteCC(false)，应该设置为true(默认值为false)

- 使用ActionProcessor来处理多个action，单独组件作为apk运行时能正常工作，打包到主app中则不能正常工作

    ```groovy
    //某个组件使用自定义的cc-settings-2.gradle文件添加了额外的配置时，主工程也要使用相同的gradle文件
    apply from: rootProject.file(cc-settings-2.gradle)
    ```
    参考[demo/build.gradle](https://github.com/luckybilly/CC/blob/master/demo/build.gradle)中的配置

- 如何实现context.startActivityForResult的功能

    
        1. 使用方式同普通页面跳转
        2. 在onCall方法里返回true
        3. 在跳转的Activity中回调信息时，不用setResult, 通过CC.sendCCResult(callId, result)来回调结果
        4. 调用组件时，使用cc.callAsyncCallbackOnMainThread(new IComponentCallback(){...})来接收返回结果
        
- ComponentName和ActionName这些常量字符串如何管理
	
	
		建议采用以下2种方式中的一种来进行管理：
		1. 每个组件内部维护一个常量类，将组件自己的ComponentName、ActionName及用到的其它组件的字符串统一管理
		2. 创建一个公共module，为每个组件创建一个常量类，包含对应组件的名称及action字符串

- 组件之间通信的自定义类型如何处理
    - 下沉到公共库中，作为公共类型
        - 适用于使用频次高或对性能要求特别敏感的场景
    - json传递 & bean冗余：即在每个用到此类型的组件，通过冗余bean的方式来传递
        - 适用于使用频次较低且对性能要求不是十分敏感的场景
        
    demo_component_b中的模拟登录功能[演示了自定义类型的传递](../demo_component_b/src/main/java/com/billy/cc/demo/component/b/LoginActivity.java#L89:20)
        
未完待续...