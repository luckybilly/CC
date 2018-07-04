package com.billy.cc.core.component.remote;

import android.os.Parcel;
import android.os.Parcelable;

import com.billy.cc.core.component.CC;

import java.util.Map;

/**
 * 跨进程传递的CC对象
 * @author billy.qi
 * @since 18/6/24 11:29
 */
public class RemoteCC implements Parcelable {

    private Map<String, Object> params;

    private String componentName;
    private String actionName;
    private String callId;
    private boolean resultRequired;

    public RemoteCC(CC cc) {
        this.componentName = cc.getComponentName();
        this.actionName = cc.getActionName();
        this.callId = cc.getCallId();
        this.params = RemoteParamUtil.toRemoteMap(cc.getParams());
        this.resultRequired = cc.resultRequired();
    }

    public Map<String, Object> getParams() {
        return RemoteParamUtil.toLocalMap(params);
    }

    protected RemoteCC(Parcel in) {
        componentName = in.readString();
        actionName = in.readString();
        callId = in.readString();
        resultRequired = in.readByte() != 0;
        params = in.readHashMap(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(componentName);
        dest.writeString(actionName);
        dest.writeString(callId);
        dest.writeByte((byte) (resultRequired ? 1 : 0));
        dest.writeMap(params);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RemoteCC> CREATOR = new Creator<RemoteCC>() {
        @Override
        public RemoteCC createFromParcel(Parcel in) {
            return new RemoteCC(in);
        }

        @Override
        public RemoteCC[] newArray(int size) {
            return new RemoteCC[size];
        }
    };

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

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public boolean isResultRequired() {
        return resultRequired;
    }

    public void setResultRequired(boolean resultRequired) {
        this.resultRequired = resultRequired;
    }
}
