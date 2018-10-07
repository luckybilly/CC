package com.billy.cc.core.component.remote;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.IBinder;

import com.billy.cc.core.component.RemoteCCService;

/**
 * 用于跨进程通信的游标，通过Extras跨进程传递bundle，在bundle中传递IBinder
 * @author billy.qi
 * @since 18/6/24 11:40
 */
public class RemoteCursor extends MatrixCursor {
    private static final String KEY_BINDER_WRAPPER = "BinderWrapper";

    static final String[] DEFAULT_COLUMNS = {"cc"};

    //-------------------------单例模式 start --------------
    /** 单例模式Holder */
    private static class CCCursorHolder {
        private static final RemoteCursor INSTANCE = new RemoteCursor(DEFAULT_COLUMNS, RemoteCCService.getInstance());
    }
    private RemoteCursor(String[] columnNames, IBinder binder) {
        super(columnNames);
        binderExtras.putParcelable(KEY_BINDER_WRAPPER, new BinderWrapper(binder));
    }
    /** 获取CCCursor在当前进程中的单例对象 */
    public static RemoteCursor getInstance() {
        return RemoteCursor.CCCursorHolder.INSTANCE;
    }
    //-------------------------单例模式 end --------------

    private Bundle binderExtras = new Bundle();

    @Override
    public Bundle getExtras() {
        return binderExtras;
    }

    public static IRemoteCCService getRemoteCCService(Cursor cursor) {
        if (null == cursor) {
            return null;
        }
        Bundle bundle = cursor.getExtras();
        bundle.setClassLoader(BinderWrapper.class.getClassLoader());
        BinderWrapper binderWrapper = bundle.getParcelable(KEY_BINDER_WRAPPER);
        if (binderWrapper != null) {
            IBinder binder = binderWrapper.getBinder();
            return IRemoteCCService.Stub.asInterface(binder);
        }
        return null;
    }

}
