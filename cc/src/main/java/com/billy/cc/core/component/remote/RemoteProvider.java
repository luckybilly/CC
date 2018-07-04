package com.billy.cc.core.component.remote;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.billy.cc.core.component.CC;

/**
 * @author billy.qi
 * @since 18/6/24 11:38
 */
public class RemoteProvider extends ContentProvider {

    public static final String[] PROJECTION_MAIN = {"cc"};

    public static final String URI_SUFFIX = "com.billy.cc.core.remote";

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (CC.isRemoteCCEnabled()) {
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
