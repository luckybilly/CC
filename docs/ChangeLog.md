
# 更新日志

[点击这里查看最新的更新日志](https://luckybilly.github.io/CC-website/#/changelog)

- 2019.01.30 V2.1.2

~~~
1. 修复主线程同步调用跨进程组件时超时设置失效的问题
2. 升级cc-register插件到1.0.7：修复gradle版本号有多个小数点（如：4.10.1）时cc-register插件报错的问题
3. 新增一个错误码：-12（组件不支持该actionName）
      在IComponent.onCall(cc)方法中通过CC.sendCCResult(callId, CCResult.errorUnsupportedActionName())来返回该error
4. CC支持不使用Key设置一个param，CCResult支持不使用Key设置一个data (建议：仅在只有1个参数的时候使用)：

      //通过setParamWithNoKey添加一个无key的参数（只支持1个）
      CC.obtainBuilder("ComponentA")...setParamWithNoKey("billy")...build().callAsync();
      //对应的取值方式为
      String name = cc.getParamItemWithNoKey();
      //支持取值时提供一个默认值
      String name = cc.getParamItemWithNoKey("");
      
      //通过successWithNoKey构建一个无key返回值的CCResult（只支持1个）
      CCResult.successWithNoKey("billy");
      //对应的取值方式为
      String name = result.getDataItemWithNoKey();
      //支持取值时提供一个默认值
      String name = result.getDataItemWithNoKey("");
~~~
- 2018.10.05 V2.1.0


        1. 在定义组件时可通过实现IMainThread接口指定不同action被调用时component.onCall方法是否在主线程运行
        2. 使用CCUtil.navigateTo、CCUtil.getNavigateCallId及CCUtil.getNavigateParam等工具方法来简化页面跳转相关的代码

链接：

[IMainThread](../cc/src/main/java/com/billy/cc/core/component/IMainThread.java)
[CCUtil](../cc/src/main/java/com/billy/cc/core/component/CCUtil.java)

     
- 2018.09.16 V2.0.0 全新升级


        1. 重构跨进程通信机制，新增支持应用内部跨进程组件调用
        2. 新增通过拦截器（继承BaseForwardInterceptor）转发组件调用，可用于A/B-Test
        3. 自动注册插件从通用的AutoRegister改为CC定制版的cc-register
        4. 大幅简化cc-settings.gradle，将大部分功能移至cc-register插件中完成
        5. 优化组件独立运行的步骤：可直接在Android studio中点击运行按钮（主app需要排除当前独立运行的组件，还是通过local.properties中添加module_name=true来实现）
        
详情请看[升级指南](../2.0升级指南.MD)

- 2018.06.04 V1.1.0 重大更新


        1. 新增支持全局拦截器： 
            实现IGlobalCCInterceptor接口即可，插件会自动完成注册 (配合最新的cc-settings.gradle文件使用)
            CC调用时，可通过withoutGlobalInterceptor()对当前CC禁用所有全局拦截器
        2. 跨app调用时，新增支持自定义类型的参数
            实现IParamJsonConverter接口即可，插件会自动完成注册 (配合最新的cc-settings.gradle文件使用)
            自定义Bean的类型无需实现Serializable/Parcelable接口
            需要跨app传递的bean类型需要下沉到公共库，通信双方都依赖此库以实现类型发送和接受
            参考：LoginActivity
        3. 新增一种状态码： -11
            只会在跨app调用组件时发生，代表参数传递错误，可以通过查看Logcat了解详细信息
        4. 跨app调用默认状态改为关闭，可手动打开： CC.enableRemoteCC(true)
        5. 修改cc-settings.gradle
            增加IGlobalCCInterceptor和IParamJsonConverter的自动注册配置
            将autoregister的参数配置改为可添加的方式(原来是覆盖式),参考：cc-settings-demo.gradle

链接：

全局拦截器：
[IGlobalCCInterceptor](../cc/src/main/java/com/billy/cc/core/component/IGlobalCCInterceptor.java)

跨app调用时的参数转换工具：
[IParamJsonConverter](../cc/src/main/java/com/billy/cc/core/component/IParamJsonConverter.java)

[LoginActivity](../demo_component_b/src/main/java/com/billy/cc/demo/component/b/LoginActivity.java)

[cc-settings-demo.gradle](../cc-settings-demo.gradle)

- 2018.05.17 V1.0.0版 Fix issue [#23](https://github.com/luckybilly/CC/issues/23)


        修复跨app调用组件时传递的参数为null导致`cc.getParamItem(key)`抛异常的问题

- 2018.04.06 更新cc-settings.gradle


        1. 废弃ext.runAsApp参数设置，（目前仍然兼容其功能，但不再推荐使用）
        2. 新增使用ext.mainApp=true来标记主app module
        3. 新增依赖组件的方式（功能见README，用法示例见demo/build.gradle）： 
            dependencies {
                addComponent 'demo_component_a' //会默认添加依赖：project(':demo_component_a')
                addComponent 'demo_component_kt', project(':demo_component_kt') //module方式
                addComponent 'demo_component_b', 'com.billy.demo:demo_b:1.1.0'  //maven方式
            }
        

- 2018.02.09 V0.5.0版

        
        在组件作为app运行时，通过显式调用如下代码来解决在部分设备上无法被其它app调用的问题
        CC.enableRemoteCC(true);//建议在Application.onCreate方法中调用

- 2018.02.07 V0.4.0版

        
        异步调用时也支持超时设置(setTimeout)

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