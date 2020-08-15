package com.qzr.sevenplayer.view;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;

import com.qzr.sevenplayer.R;
import com.qzr.sevenplayer.base.BaseActivity;
import com.qzr.sevenplayer.manager.DecorderManager;

import butterknife.BindView;

public class DecorderActivity extends BaseActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "DecorderActivity";

    @BindView(R.id.tv_fileName)
    TextView fileName;
    @BindView(R.id.txv_fileWindow)
    TextureView fileWindow;

    String filePathStr;

    DecorderManager decorderManager;

    @Override
    public void beforeSetContentView() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_decorder;
    }

    @Override
    public void initView() {
        initData();
        fileWindow.setRotation(90);
        fileWindow.setSurfaceTextureListener(this);
        decorderManager = DecorderManager.getInstance();
    }

    private void initData() {
        filePathStr = getIntent().getExtras().getString("filePath");
        fileName.setText(filePathStr);
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
