package com.qzr.sevenplayer.view;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;

import com.qzr.sevenplayer.R;
import com.qzr.sevenplayer.base.BaseActivity;
import com.qzr.sevenplayer.manager.DecorderManager;

import butterknife.BindView;

public class DecorderActivity extends BaseActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "DecorderActivity";

    @BindView(R.id.txv_fileWindow)
    TextureView fileWindow;

    String filePathStr;

    DecorderManager decorderManager;

    @Override
    public void beforeSetContentView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_decorder;
    }

    @Override
    public void initView() {
        initData();
        fileWindow.setRotation(90);
        fileWindow.setScaleX((float) 1920 / 1080);
        fileWindow.setScaleY((float) 1080 / 1920);
        fileWindow.setSurfaceTextureListener(this);
        decorderManager = DecorderManager.getInstance();
    }

    private void initData() {
        filePathStr = getIntent().getExtras().getString("filePath");
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        decorderManager.buildMediaDecode(filePathStr, new Surface(surface));
        decorderManager.startMediaDecode();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        decorderManager.stopMediaDecode();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
