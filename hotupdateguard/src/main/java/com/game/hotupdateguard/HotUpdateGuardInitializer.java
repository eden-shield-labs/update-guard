package com.game.hotupdateguard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * HotUpdate Guard Auto Initializer
 * Uses ContentProvider auto-initialization, executes before Application.onCreate()
 */
public class HotUpdateGuardInitializer extends ContentProvider {
    private static final String TAG = "HotUpdateGuard";

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            Log.i(TAG, "HotUpdate Guard initializing...");
            HotUpdateGuard.getInstance(context).initialize();
            Log.i(TAG, "HotUpdate Guard initialization completed");
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
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
