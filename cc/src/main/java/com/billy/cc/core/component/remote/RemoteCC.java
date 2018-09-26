package com.billy.cc.core.component.remote;

import android.os.Parcel;
import android.os.Parcelable;

import com.billy.cc.core.component.CC;
import com.billy.cc.core.component.CCUtil;

import org.json.JSONObject;

import java.util.Map;

import static com.billy.cc.core.component.CCUtil.put;

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
    private boolean isMainThreadSyncCall;

    private Map<String, Object> localParams;

    public RemoteCC(CC cc) {
        this(cc, false);
    }

    public RemoteCC(CC cc, boolean isMainThreadSyncCall) {
        this.componentName = cc.getComponentName();
        this.actionName = cc.getActionName();
        this.callId = cc.getCallId();
        this.params = RemoteParamUtil.toRemoteMap(cc.getParams());
        this.isMainThreadSyncCall = isMainThreadSyncCall;
    }

    public Map<String, Object> getParams() {
        if (localParams == null) {
            localParams = RemoteParamUtil.toLocalMap(params);
        }
        return localParams;
    }

    protected RemoteCC(Parcel in) {
        componentName = in.readString();
        actionName = in.readString();
        callId = in.readString();
        isMainThreadSyncCall = in.readByte() != 0;
        params = in.readHashMap(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(componentName);
        dest.writeString(actionName);
        dest.writeString(callId);
        dest.writeByte((byte) (isMainThreadSyncCall ? 1 : 0));
        dest.writeMap(params);
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        put(json, "componentName", componentName);
        put(json, "actionName", actionName);
        put(json, "callId", callId);
        put(json, "isMainThreadSyncCall", isMainThreadSyncCall);
        put(json, "params", CCUtil.convertToJson(params));
        return json.toString();
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

    public boolean isMainThreadSyncCall() {
        return isMainThreadSyncCall;
    }

    public void setMainThreadSyncCall(boolean mainThreadSyncCall) {
        isMainThreadSyncCall = mainThreadSyncCall;
    }
}
