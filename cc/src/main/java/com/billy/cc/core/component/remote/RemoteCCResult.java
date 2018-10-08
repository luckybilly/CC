package com.billy.cc.core.component.remote;

import android.os.Parcel;
import android.os.Parcelable;

import com.billy.cc.core.component.CCResult;
import com.billy.cc.core.component.CCUtil;

import org.json.JSONObject;

import java.util.Map;

import static com.billy.cc.core.component.CCUtil.put;

/**
 * 用于跨进程传递的CCResult
 * @author billy.qi
 * @since 18/6/3 02:22
 */
public class RemoteCCResult implements Parcelable {

    private Map<String, Object> data;

    private boolean success;
    private String errorMessage;
    private int code;

    public RemoteCCResult(CCResult result) {
        setCode(result.getCode());
        setErrorMessage(result.getErrorMessage());
        setSuccess(result.isSuccess());
        data = RemoteParamUtil.toRemoteMap(result.getDataMap());
    }

    public CCResult toCCResult() {
        CCResult result = new CCResult();
        result.setCode(getCode());
        result.setErrorMessage(getErrorMessage());
        result.setSuccess(isSuccess());
        result.setDataMap(RemoteParamUtil.toLocalMap(data));
        return result;
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        put(json, "success", success);
        put(json, "code", code);
        put(json, "errorMessage", errorMessage);
        put(json, "data", CCUtil.convertToJson(data));
        return json.toString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (success ? 1 : 0));
        dest.writeString(errorMessage);
        dest.writeInt(code);
        dest.writeMap(data);
    }

    private RemoteCCResult(Parcel in) {
        success = in.readByte() != 0;
        errorMessage = in.readString();
        code = in.readInt();
        data = in.readHashMap(getClass().getClassLoader());
    }

    public static final Creator<RemoteCCResult> CREATOR = new Creator<RemoteCCResult>() {
        @Override
        public RemoteCCResult createFromParcel(Parcel in) {
            return new RemoteCCResult(in);
        }

        @Override
        public RemoteCCResult[] newArray(int size) {
            return new RemoteCCResult[size];
        }
    };
}
