## CC : ComponentCaller


CC是一套Android的组件化框架，由CC核心API类库和cc-register插件组成

模块|CC|cc-register
:---:|:---:|:---:
当前最新版本| [![Download](https://api.bintray.com/packages/hellobilly/android/cc/images/download.svg)](https://bintray.com/hellobilly/android/cc/_latestVersion)| [![Download](https://api.bintray.com/packages/hellobilly/android/cc-register/images/download.svg)](https://bintray.com/hellobilly/android/cc-register/_latestVersion)

[华丽丽的文档][docs]

<div align=center><img style="width:auto;" src="https://github.com/luckybilly/CC/raw/master/image/icon.png"/></div>

### CC的特色
- 一静一动，开发时运行2个app：
  - 静：主App (通过跨App的方式单组件App内的组件)
  - 动：单组件App (通过跨App的方式调用主App内的组件)
- 支持[渐进式组件化改造][7]
  - <font color=red>解耦只是过程，而不是前提</font>

### 一句话介绍CC：
CC是一套基于组件总线的、支持渐进式改造的、支持跨进程调用的、完整的Android组件化框架

- 基于组件总线： 
    - 不同于市面上种类繁多的路由框架，CC采用了基于组件总线的架构，不依赖于路由([路由 VS 总线][1])
- 支持渐进式改造： 
    - 接入CC后可立即用以组件的方式开发新业务，可单独运行调试开发，通过跨app的方式调用项目中原有功能
    - 不需要修改项目中现有的代码，只需要新增一个IComponent接口的实现类（组件类）即可支持新组件的调用
    - 模块解耦不再是前提，将陡峭的组件化改造实施曲线拉平
- 支持跨进程调用： 
    - 支持应用内跨进程调用组件，支持跨app调用组件
    - 调用方式与同一个进程内的调用方式完全一致
    - 无需bindService、无需自定义AIDL，无需接口下沉
- 完整：
    - CC框架下组件提供的服务可以是几乎所有功能，包括但不限于页面跳转、提供服务、获取数据、数据存储等
    - CC提供了配套插件cc-register，完成了自定义的组件类、全局拦截器类及json转换工具类的自动注册，
    - cc-register同时还提供了代码隔离、debug代码分离、组件单独调试等各种组件化开发过程中需要的功能

CC的设计灵感来源于服务端的服务化架构，将组件之间的关系拍平，不互相依赖但可以互相调用，不需要再管理复杂的依赖树。

了解业界开源的一些组件化方案：[多个维度对比一些有代表性的开源android组件化开发方案](https://github.com/luckybilly/AndroidComponentizeLibs) 

## demo演示

[demo下载(主工程,包含ComponentB之外的所有组件)](https://github.com/luckybilly/CC/raw/master/demo-debug.apk)

[demo_component_b组件单独运行的App(Demo_B)下载](https://github.com/luckybilly/CC/raw/master/demo_component_b-debug.apk)

以上**2个app**用来演示组件打包在主app内和**单独以app运行**时的组件调用，**都安装在手机上**之后的运行效果如下图所示

<div align=center><img style="width:auto;" src="https://github.com/luckybilly/CC/raw/master/image/CC.gif"/></div>


## 目录结构

        - cc                            组件化框架基础库（主要）
        - cc-register                   CC框架配套的gradle插件（主要）
        - cc-settings-2.gradle          组件化开发构建脚本（主要）
        - demo                          demo主程序（调用其它组件，并演示了动态组件的使用）
        - demo_base                     demo公共库(base类、util类、公共Bean等)
        - demo_component_a              demo组件A
        - demo_component_b              demo组件B（上方提供下载的apk在打包时local.properties中添加了demo_component_b=true）
        - demo_component_jsbridge       demo组件(面向组件封装的jsBridge，并演示了如何进行跨进程组件调用)
        - demo_component_kt             demo组件(kotlin)
        - demo_interceptors             demo全局拦截器(如果有多个app并且拦截器不同，可以创建多个module给不同app使用)
        - cc-settings-demo.gradle       演示如何自定义配置文件，如：添加actionProcessor自动注册的配置
        - demo-debug.apk                demo安装包(包含demo/demo_component_a/demo_component_kt)
        - demo_component_b-debug.apk    demo组件B单独运行安装包


## 创建组件

创建一个组件很简单：只要创建一个`IComponent`接口的实现类，在onCall方法中实现组件暴露的服务即可

```java
public class ComponentA implements IComponent {
  @Override
  public String getName() {
      //指定组件的名称
      return "ComponentA";
  }

  @Override
  public boolean onCall(CC cc) {
    //在此处将组件内部的服务暴露给外部调用
    //组件内部的逻辑与外部完全解耦
    String actionName = cc.getActionName();
    switch (actionName) {
      case "showActivity": //响应actionName为"showActivity"的组件调用
        //跳转到页面：ActivityA
        CCUtil.navigateTo(cc, ActivityA.class);
        //返回处理结果给调用方
        CC.sendCCResult(cc.getCallId(), CCResult.success());
        break;
      default:
        //其它actionName当前组件暂时不能响应，可以通过如下方式返回状态码为-12的CCResult给调用方
        CC.sendCCResult(cc.getCallId(), CCResult.errorUnsupportedActionName());
        break;
    }
    return false;
  }
}

```

## 调用组件

CC 使用简明的流式语法API，因此它允许你在一行代码搞定组件调用：

"CC"也是本框架主入口API类的类名，是由ComponentCaller缩写而来，其核心职能是:**组件的调用者**。

```java
CC.obtainBuilder("ComponentA")
  .setActionName("showActivity")
  .build()
  .call();
```
也可以这样
```java
CC.obtainBuilder("ComponentA")
  .setActionName("showActivity")
  .build()
  .callAsync();
```
或者这样
```java
CC.obtainBuilder("ComponentA")
  .setActionName("showActivity")
  .build()
  .callAsyncCallbackOnMainThread(new IComponentCallback() {
        @Override
        public void onResult(CC cc, CCResult result) {
          String toast = result.isSuccess() ? "success" : "failed";
          Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
        }
    });
```

## 开始使用

请[看文档][docs]、[看文档][docs]、[看文档][docs]


## 混淆配置

不需要额外的混淆配置

## 自动注册插件
CC专用版：[cc-register](cc-register)，fork自[AutoRegister](https://github.com/luckybilly/AutoRegister)，在自动注册的基础上添加了一些CC专用的业务

通用版：

源码:[AutoRegister](https://github.com/luckybilly/AutoRegister)
原理:[android扫描接口实现类并通过修改字节码自动生成注册表](http://blog.csdn.net/cdecde111/article/details/78074692)


## 版本更新日志

请点击：[更新日志][changelog]

## 遇到问题怎么办？

- 先打开CC的日志开关，看完整的调用过程日志，这往往能帮助我们找到问题
```java
CC.enableDebug(true);  //普通调试日志，会提示一些错误信息
CC.enableVerboseLog(true);  //组件调用的详细过程日志，用于跟踪整个调用过程
```
- 看[文档][docs]
- [看issue](https://github.com/luckybilly/CC/issues)了解开源社区上其它小伙伴提出的问题及解答过程，很可能就有你现在遇到的问题
- [提issue](https://github.com/luckybilly/CC/issues/new),如果以上还没有解决你的问题，请[提一个issue](https://github.com/luckybilly/CC/issues/new)，这很可能是个新的问题，提issue能帮助到后面遇到相同问题的朋友
- 加下方的QQ群提问

## QQ群

QQ群号：686844583  

<a target="_blank" href="http://shang.qq.com/wpa/qunwpa?idkey=5fdd1171114b5a1eb80ea0be00b392c2e3e8ab6f278f182a07e959e80d4c9409"><img border="0" src="http://pub.idqqimg.com/wpa/images/group.png" alt="CC交流群" title="CC交流群"></a>

或者扫描下方二维码加群聊

![image](image/CC_QQ.png)

[1]: https://luckybilly.github.io/CC-website/#/article-router_vs_bus
[7]: https://luckybilly.github.io/CC-website/#/article-componentize-gradually
[issue]: https://github.com/luckybilly/CC/issues/new
[docs]: https://luckybilly.github.io/CC-website/
[changelog]: https://luckybilly.github.io/CC-website/#/changelog
