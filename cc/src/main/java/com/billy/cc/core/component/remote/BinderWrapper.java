package com.billy.cc.core.component.remote;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import com.billy.cc.core.component.RemoteCCService;

/**
 * 封装IBinder
 * 用于在{@link RemoteProvider}中通过{@link RemoteCursor}跨进程传递{@link RemoteCCService}对象
 * @author billy.qi
 */
public class BinderWrapper implements Parcelable {

    private final IBinder binder;

    public BinderWrapper(IBinder binder) {
        this.binder = binder;
    }

    public BinderWrapper(Parcel in) {
        this.binder = in.readStrongBinder();
    }

    public IBinder getBinder() {
        return binder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(binder);
    }

    public static final Creator<BinderWrapper> CREATOR = new Creator<BinderWrapper>() {
        @Override
        public BinderWrapper createFromParcel(Parcel source) {
            return new BinderWrapper(source);
        }

        @Override
        public BinderWrapper[] newArray(int size) {
            return new BinderWrapper[size];
        }
    };
}
