package com.billy.cc.core.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于跨进程传递的CC对象
 * @author billy.qi
 * @since 18/6/3 02:22
 */
class RemoteCC implements Serializable {

    private static final long serialVersionUID = 1L;

    private HashMap<String, RemoteParamUtil.BaseParam> params;

    private String componentName;
    private String actionName;
    private long timeout;
    private String callId;

    RemoteCC(CC cc) {
        componentName = cc.getComponentName();
        actionName = cc.getActionName();
        timeout = cc.getTimeout();
        callId = cc.getCallId();
        //将参数列表转换为可跨app传递的格式
        params = RemoteParamUtil.toRemoteMap(cc.getParams());
    }

    public Map<String, Object> getParams() {
        //重新转换成本地对象
        return RemoteParamUtil.toLocalMap(params);
    }

    public void setParams(HashMap<String, RemoteParamUtil.BaseParam> params) {
        this.params = params;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

}
