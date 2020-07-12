package com.qzr.sevenplayer.view;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.qzr.sevenplayer.R;
import com.qzr.sevenplayer.base.BaseActivity;
import com.qzr.sevenplayer.interf.PermissionInterface;
import com.qzr.sevenplayer.utils.PermissionHelper;

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

    @BindView(R.id.btn_preview)
    Button btnPreview;
    @BindView(R.id.btn_play)
    Button btnPlay;

    private PermissionHelper permissionHelper;

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
                jump2Activity(RecorderActivity.class);
                break;
            }
            case R.id.btn_play: {
                Toast.makeText(mContext, "暂未开发", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    private void jump2Activity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
    }

    private static int REQUEST_CAMERA_PERMISSION = 100;
    private static int REQUEST_READ_STORAGE_PERMISSION = 100 + 1;
    private static int REQUEST_WRITE_STORAGE_PERMISSION = 100 + 2;

    private void permissionGet() {
        permissionHelper = new PermissionHelper(this, this);
        permissionHelper.requestPermission(Manifest.permission.CAMERA, REQUEST_CAMERA_PERMISSION);
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


}
