package com.billy.cc.core.component.remote;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;

import com.billy.cc.core.component.CC;

import static android.os.Binder.getCallingUid;

/**
 * 通过ContentProvider实现跨进程通信 <br>
 * 通信原理：  <br>
 *  1. 通过ContentProvider的query获取RemoteCursor单例对象 <br>
 *  2. 通过RemoteCursor.getExtras()获取bundle <br>
 *  3. 通过bundle传递Parcelable <br>
 *  4. 通过Parcelable封装IBinder (RemoteCCService) <br>
 *  5. 最终跨进程将RemoteCCService对象传递给调用方 <br>
 * @author billy.qi
 * @since 18/6/24 11:38
 */
public class RemoteProvider extends ContentProvider {

    public static final String[] PROJECTION_MAIN = {"cc"};

    public static final String URI_SUFFIX = "com.billy.cc.core.remote";

    @Override
    public boolean onCreate() {
        CC.log("RemoteProvider onCreated! class:%s", this.getClass().getName());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (CC.isRemoteCCEnabled() || getCallingUid() == Process.myUid()) {
            //获取当前ContentProvider所在进程中的RemoteCursor单例对象
            return RemoteCursor.getInstance();
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
