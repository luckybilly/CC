package com.billy.cc.core.component;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 用于跨进程传递的CCResult
 * @author billy.qi
 * @since 18/6/3 02:22
 */
class RemoteCCResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private HashMap<String, RemoteParamUtil.BaseParam> params;

    private boolean success;
    private String errorMessage;
    private int code;

    RemoteCCResult(CCResult result) {
        setCode(result.getCode());
        setErrorMessage(result.getErrorMessage());
        setSuccess(result.isSuccess());
        params = RemoteParamUtil.toRemoteMap(result.getDataMap());
    }

    CCResult toCCResult() {
        CCResult result = new CCResult();
        result.setCode(getCode());
        result.setErrorMessage(getErrorMessage());
        result.setSuccess(isSuccess());
        result.setDataMap(RemoteParamUtil.toLocalMap(params));
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
