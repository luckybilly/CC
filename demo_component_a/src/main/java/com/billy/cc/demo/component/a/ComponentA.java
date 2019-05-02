package com.billy.cc.demo.component.a;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.CCUtil;
import com.billy.cc.core.component.IComponent;

/**
 * @author billy.qi
 */
public class ComponentA implements IComponent {
    @Override
    public String getName() {
        //组件的名称，调用此组件的方式：
        // CC.obtainBuilder("ComponentA")...build().callAsync()
        return "demo.ComponentA";
    }

    /**
     * 组件被调用时的入口
     * 要确保每个逻辑分支都会调用到CC.sendCCResult，
     * 包括try-catch,if-else,switch-case-default,startActivity
     * @param cc 组件调用对象，可从此对象中获取相关信息
     * @return true:将异步调用CC.sendCCResult(...),用于异步实现相关功能，例如：文件加载、网络请求等
     *          false:会同步调用CC.sendCCResult(...),即在onCall方法return之前调用，否则将被视为不合法的实现
     */
    @Override
    public boolean onCall(CC cc) {
        String actionName = cc.getActionName();
        switch (actionName) {
            case "showActivityA":
                openActivity(cc);
                break;
            case "getLifecycleFragment":
                //demo for provide fragment object to other component
                getLifecycleFragment(cc);
                break;
            case "lifecycleFragment.addText":
                lifecycleFragmentDoubleText(cc);
                break;
            case "getInfo":
                getInfo(cc);
                break;
            case "testTimeout":
               testTimeout(cc);
            default:
                //这个逻辑分支上没有调用CC.sendCCResult(...),是一种错误的示例
                //并且方法的返回值为false，代表不会异步调用CC.sendCCResult(...)
                //在LocalCCInterceptor中将会返回错误码为-10的CCResult
                break;
        }
        return false;
    }

    private void lifecycleFragmentDoubleText(CC cc) {
        LifecycleFragment lifecycleFragment = cc.getParamItem("fragment");
        if (lifecycleFragment != null) {
            String text = cc.getParamItem("text", "");
            lifecycleFragment.addText(text);
            CC.sendCCResult(cc.getCallId(), CCResult.success());
        } else {
            CC.sendCCResult(cc.getCallId(), CCResult.error("no fragment params"));
        }
    }

    private void getLifecycleFragment(CC cc) {
        CC.sendCCResult(cc.getCallId(), CCResult.successWithNoKey(new LifecycleFragment()));
    }

    private void getInfo(CC cc) {
        String userName = "billy";
        CC.sendCCResult(cc.getCallId(), CCResult.success("userName", userName));
    }

    private void openActivity(CC cc) {
        CCUtil.navigateTo(cc, ActivityA.class);
        CC.sendCCResult(cc.getCallId(), CCResult.success());
    }

    /**
     * 测试超时
     * 注意：如果处于程序调试状态和CC.DEBUG是true，两个都满足情况下，
     * 不执行超时 timeout(){@link CC#timeout()}。
     * 默认超时是{@link CC#DEFAULT_TIMEOUT} 毫秒。
     * @param cc 组件调用对象
     */
    private void testTimeout(CC cc) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        CC.sendCCResult(cc.getCallId(), CCResult.success());
    }

}
