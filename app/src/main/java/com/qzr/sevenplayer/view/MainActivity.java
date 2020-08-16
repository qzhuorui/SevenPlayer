package com.qzr.sevenplayer.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.qzr.sevenplayer.R;
import com.qzr.sevenplayer.base.BaseActivity;
import com.qzr.sevenplayer.interf.PermissionInterface;
import com.qzr.sevenplayer.utils.PermissionHelper;

import java.io.File;

import butterknife.BindView;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.view
 * @ClassName: MainActivity
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/5 9:18
 */
public class MainActivity extends BaseActivity implements PermissionInterface, View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_CONTENT = 1;

    @BindView(R.id.btn_preview)
    Button btnPreview;
    @BindView(R.id.btn_play)
    Button btnPlay;

    private PermissionHelper permissionHelper;

    String pathStr = null;

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        permissionGet();
    }

    @Override
    public void beforeSetContentView() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void initView() {
        btnPreview.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_preview: {
                jump2Activity(RecorderActivity.class, null);
                break;
            }
            case R.id.btn_play: {
                jump2FileContent();
                break;
            }
        }
    }

    private void jump2Activity(Class activity, Bundle bundle) {
        Intent intent = new Intent(this, activity);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    private void jump2FileContent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_CONTENT);
    }

    private static int REQUEST_CAMERA_PERMISSION = 100;
    private static int REQUEST_READ_STORAGE_PERMISSION = 100 + 1;
    private static int REQUEST_WRITE_STORAGE_PERMISSION = 100 + 2;
    private static int REQUEST_RECORD_AUDIO = 100 + 3;

    private void permissionGet() {
        permissionHelper = new PermissionHelper(this, this);
        permissionHelper.requestPermission(Manifest.permission.CAMERA, REQUEST_CAMERA_PERMISSION);
        permissionHelper.requestPermission(Manifest.permission.RECORD_AUDIO, REQUEST_RECORD_AUDIO);
        permissionHelper.requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_STORAGE_PERMISSION);
        permissionHelper.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_STORAGE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionHelper.requestPermissionResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void requestPermissionSuccess(int callBackCode) {

    }

    @Override
    public void requestPermissionFail(int callBackCode) {
        Log.e(TAG, "requestPermissionFail: code: " + callBackCode);
    }

    @SuppressLint("ObsoleteSdkInt")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Uri:content://com.android.providers.media.documents/document/video%3A284453
        if (resultCode == Activity.RESULT_OK) {
            assert data != null;
            Uri uri = data.getData();
            assert uri != null;
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                pathStr = uri.getPath();
                Log.i(TAG, "onActivityResult: pathStr1: " + pathStr);
                return;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                //4.4上
                pathStr = getPath(this, uri);
                Log.i(TAG, "onActivityResult: pathStr2: " + pathStr);
            } else {
                Toast.makeText(mContext, "系统版本过低", Toast.LENGTH_SHORT).show();
            }
            if (!TextUtils.isEmpty(pathStr)) {
                Bundle bundle = new Bundle();
                bundle.putString("filePath", pathStr);
                if (pathStr.contains("video")) {
                    jump2Activity(DecorderActivity.class, bundle);
                } else {
                    Toast.makeText(mContext, "暂不支持查看图片", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getPath(Context context, Uri uri) {
        //DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                //ExternalStorageDocument
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + File.separator + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                //DownloadsProvider
                String id = DocumentsContract.getDocumentId(uri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                //MediaProvider
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;

                if ("image".equalsIgnoreCase(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equalsIgnoreCase(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equalsIgnoreCase(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = "_id=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //MediaStore (and general)
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
