## CC : Component Caller (Modularization Architecture for Android)

[中文文档](README.md)

[![Join the chat at https://gitter.im/billy_home/CC](https://badges.gitter.im/billy_home/CC.svg)](https://gitter.im/billy_home/CC?utm_source=share-link&utm_medium=link&utm_campaign=share-link)

Name|CC|cc-register
---|---|---
Version| [![Download](https://api.bintray.com/packages/hellobilly/android/cc/images/download.svg)](https://bintray.com/hellobilly/android/cc/_latestVersion)| [![Download](https://api.bintray.com/packages/hellobilly/android/cc-register/images/download.svg)](https://bintray.com/hellobilly/android/cc-register/_latestVersion)

## demo download

[Main App(contains demo and demo_component_a)](https://github.com/luckybilly/CC/raw/master/demo-debug.apk)

[demo_component_b (Demo_B)](https://github.com/luckybilly/CC/raw/master/demo_component_b-debug.apk)

demo shows cc works on component in or not in main app.
it looks like below via running both of above app on your device and launch demo app.


        Notice: calling across apps is only compat for develop time
        you need to turnon the permission 'auto start' for Demo_B to make it worked if the process of Demo_B is not alive. 

![image](https://raw.githubusercontent.com/luckybilly/CC/master/image/CC.gif)

## What`s different?

- Easy to use, only 4 steps:
        
    - Add [AutoRegister](https://github.com/luckybilly/AutoRegister) plug-in classpath in projectRoot/build.gradle 
    - Add gradle file apply in module/build.gradle
    - Implements a component class for IComponent in the module
        - specified a name for this component in method: getNae()
        - call CC.sendCCResult(cc.getCallId, CCResule.success()) in method: onCall(cc).
    - Then you can call this component at everywhere in you app:
        - CC.obtainBuilder("component_name").build().call()
        - CC.obtainBuilder("component_name").build().callAsync()
    
- Feature-rich


        1. Support inter-component invocation (not just Activity router, call&callback for almost all instructions)
        2. Support component invocation is associated with Activity and Fragment lifecycle (requires: android api level >= 14, support lib version >= 5.1.0)
        3. Support inter-app component invocation (component development/commissioning can be run separately as an app)
        4. Support to switch and permission Settings of the invocation between apps (Meets for the security requirements of different levels, default status: enabled and do not require permission).
        5. Support for synchronous/asynchronous invocation
        6. Supports synchronous/asynchronous implementation of components
        7. The invocation method is unrestricted by implementation (for example, asynchronous implementation of another component can be invoked synchronously. Note: do not use time-consuming operation in the main thread.
        8. Support for adding custom interceptors (executed in the order of addition)
        9. Support for timeout Settings (in milliseconds, 0: no timeout, synchronous invocation set as 1000 ms by default)
        10. Support manual cancellation
        11. Automatic registration of components (IComponent) at compile time without manually maintain the component registry (implemented by using ASM to modify bytecode)
        12. Support for dynamic registration/unregistration components (IDynamicComponent)
        13. Support for non-basic types of objects, such as passing fragments between components
        14. Try to solve the crash that is caused by incorrect usage:
            14.1 component invocation, callback, and component implementation crash are all caught within the cc framework
            14.2 The CCResult object of synchronous return or asynchronous callback must not be null to avoid null pointer

- low cost to convert original code to CC

    Some guys worry about that it's too expensive to convert the code in the old project to a high degree of code coupling
    CC can only take 2 steps to solve this problem:
    1. Create a component class (IComponent implementation class) that provides functionality that was previously implemented by class dependencies
    2. Then change the direct class call to CC call mode
    
- monitor the execution process log
    Developer can monitor execution process logs with Logcat
    CC disabled this function by default, enable it with code: `CC.enableVerboseLog(true);`        
        
## The directory structure

        //core
        - cc                            core library of CC framework
        - cc-settings.gradle            common gradle file for user
        
        //demos
        - demo                          demo main app module
        - demo_component_a              demo ComponentA 
        - demo_component_b              demo ComponentB
        - cc-settings-demo.gradle       actionProcessor自动注册的配置脚本demo
        - demo-debug.apk                demo apk(contains demo and demo_component_a)
        - demo_component_b-debug.apk    apk for demo_component_b only

## How to Use

#### 1. add classpath

```groovy
buildscript {
    dependencies {
        classpath 'com.billy.android:autoregister:x.x.x'
    }
}
```
#### 2. modify build.gradle for all component and main app modules：
```groovy
apply plugin: 'com.android.library'
//or
apply plugin: 'com.android.application'

//replace to
apply from: cc-settings-2.gradle
```

see [demo_component_a/build.gradle](https://github.com/luckybilly/CC/blob/master/demo_component_a/build.gradle)

module is setting as library by default. there are 2 ways to set as application for single launch apk:

2.1 modify local.properties
```properties
demo_component_b=true # run as application for module: demo_component_b
```
2.2 modify module/build.gradle: add `ext.runAsApp = true` before `apply from: '...cc-settings.gradle'`,ext.runAsApp priority is higher than local.properties.
```groovy
ext.runAsApp = true
apply from: cc-settings-2.gradle
```
#### 3. Define a component ([IComponent](https://github.com/luckybilly/CC/blob/master/cc/src/main/java/com/billy/cc/core/component/IComponent.java))
```java
public class ComponentA implements IComponent {
    
    @Override
    public String getName() {
        // specify the name for this component
        return "demo.ComponentA";
    }

    @Override
    public boolean onCall(CC cc) {
        Context context = cc.getContext();
        Intent intent = new Intent(context, ActivityComponentA.class);
        if (!(context instanceof Activity)) {
            // context maybe an application object if caller dose not setContext 
            // or call across apps
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        //send result to caller
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        // onCall return false if result is sent synchronization before this method returned
        return false;
    }
}
```
#### 4. Call component by CC
```java
// Synchronous call, get CCResult by method return
CCResult result = CC.obtainBuilder("demo.ComponentA").build().call();
// Asynchronous call, do not need result callback
String callId = CC.obtainBuilder("demo.ComponentA").build().callAsync();
// Asynchronous call, result callback in thread pool
String callId = CC.obtainBuilder("demo.ComponentA").build().callAsync(new IComponentCallback(){...});
//Asynchronous call, result callback on main thread
String callId = CC.obtainBuilder("demo.ComponentA").build().callAsyncCallbackOnMainThread(new IComponentCallback(){...});
```

#### More: Add dependencies in main app module for all component modules like below:

```groovy
dependencies {
    addComponent 'demo_component_a' //default add dependency: project(':demo_component_a')
    addComponent 'demo_component_kt', project(':demo_component_kt')
    addComponent 'demo_component_b', 'com.billy.demo:demo_b:1.1.0'
}


## Advance usage

```java
// enable/disable debug log
CC.enableDebug(trueOrFalse);    
// enable/disable cc process detail log
CC.enableVerboseLog(trueOrFalse); 
// enable/disable caller across apps
CC.enableRemoteCC(trueOrFalse)  
// cancel a cc by callId
CC.cancel(callId)
// set context for cc
CC.obtainBuilder("demo.ComponentA")...setContext(context)...build().callAsync()
// cc will cancel automaticly on activity destroyed
CC.obtainBuilder("demo.ComponentA")...cancelOnDestroyWith(activity)...build().callAsync()
// cc will cancel automaticly on fragment destroyed
CC.obtainBuilder("demo.ComponentA")...cancelOnDestroyWith(fragment)...build().callAsync()
// set cc actionName
CC.obtainBuilder("demo.ComponentA")...setActionName(actionName)...build().callAsync()
// set cc timeout in milliseconds
CC.obtainBuilder("demo.ComponentA")...setTimeout(1000)...build().callAsync()
// add extenal params
CC.obtainBuilder("demo.ComponentA")...addParam("name", "billy").addParam("id", 12345)...build().callAsync()


// build a success CCResult
CCResult.success(key1, value1).addData(key2, value2)
// build a failed CCResult
CCResult.error(message).addData(key, value)
// send CCResult to caller (you should make sure this method called for each onCall(cc) invoked)
CC.sendCCResult(cc.getCallId(), ccResult)
// get cc result if success or not
ccResult.isSuccess()
// success code(0:success, <0: failed, 1:component reached but result is failed)
ccResult.getCode()
// get error message
ccResult.getErrorMessage()  
// get external data
Map<String, Object> data = ccResult.getDataMap();
if (data != null) {
    Object value = data.get(key)   
}
```
CCResult code list:

| code        | error status    |
| --------   | :----- |
| 0 | success |
| 1 | business failed in component |
| -1 | default error. not used yet |
| -2 | component name is empty |
| -3 | CC.sendCCResult(callId, null) or interceptor returns null |
| -4 | An exception was thrown during cc |
| -5 | no component object found for the specified component_name |
| -6 | context is null and get application failed by reflection |
| -7 | connect failed during cc across apps |
| -8 | cc is canceled |
| -9 | cc is timeout |
| -10 | component.onCall(cc) return false, bus no CCResult found |

- Custom interceptors

    1. Create a class that implements the ICCInterceptor interface
    2. Call the chain.proceed() method to keep the call chain down and not call to block the CC
    2. You can modify the parameters of the CC object before calling chain.proceed()
    3. After calling the chain.proceed() method, you can modify CCResult
    
    see demo: [MissYouInterceptor.java](https://github.com/luckybilly/CC/blob/master/demo/src/main/java/com/billy/cc/demo/MissYouInterceptor.java)
    
- register/unregister dynamic component

Definition: Unlike the static component (IComponent), which is automatically registered to ComponentManager at compile time, 
dynamic components do not automatically register and work through manual registration/unregistration

        1. Dynamic components need to implement interfaces: IDynamicComponent
        2. It is necessary to call CC.registerComponent(component) manually, similar to the BroadcastReceiver dynamic registration
        3. It is necessary to call CC.unregisterComponent(component) manually, similar to the BroadcastReceiver dynamic unregistration
        4. Other usage are the same as static components

- You can have multiple modules include in a module


        In a module, you can have multiple implementation classes for the IComponent interface (or IDynamicComponent interface)
        IComponents are automatically registered to the component management class ComponentManager at compile time
        IDynamicComponents are not

- A component can process multiple actions

        In the onCall(CC cc) method, gets actions to handle separately via cc.getActionName()

    see：[ComponentA](https://github.com/luckybilly/CC/blob/master/demo_component_a/src/main/java/com/billy/cc/demo/component/a/ComponentA.java)
- Auto register Custom ActionProcessor into component

    see[ComponentB](https://github.com/luckybilly/CC/blob/master/demo_component_b/src/main/java/com/billy/cc/demo/component/b/ComponentB.java)
    and[cc-settings-demo.gradle](https://github.com/luckybilly/CC/blob/master/cc-settings-demo.gradle)


##### watch the sourcecode of demo, demo_component_a and demo_component_b for more details

## More usage

You can easily do AOP work with CC, such as:

1. Activity open requires user login: 

- check the user login status before startActivity
- already login: 
    - startActivity immediately and CC.sendCCResult(callId, CCResult.success());
- not login: 
    - start login activity and wait for result
    - login success: startActivity and CC.sendCCResult(callId, CCResult.success());
    - login failed: CC.sendCCResult(callId, CCResult.error("login failed"));

demo: see[LifecycleComponent.java](https://github.com/luckybilly/CC/blob/master/demo/src/main/java/com/billy/cc/demo/LifecycleComponent.java)

2. Pre-load activity data before context.startActivity with [PreLoader](https://github.com/luckybilly/PreLoader)

- define a component for open the activity
```java
public class ComponentA implements IComponent {

    @Override
    public String getName() {
        return "demo.ComponentA";
    }

    @Override
    public boolean onCall(CC cc) {
        int preLoaderId = PreLoader.preLoad(new Loader());
        Intent intent = new Intent(this, PreLoadBeforeLaunchActivity.class);
        intent.putExtra("preLoaderId", preLoaderId);
        startActivity(intent);
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        return false;
    }
}
```

- call that component by CC to open activity
```java
// pre-load is needless here, the logistic of component are all inside that component itself
CC.obtainBuilder("demo.ComponentA").build().call();
```


## Proguard

none

## Contact me

qiyilike@163.com
