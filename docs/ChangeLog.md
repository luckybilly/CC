
# 更新日志

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