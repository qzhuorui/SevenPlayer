package com.qzr.sevenplayer.view;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.qzr.sevenplayer.R;
import com.qzr.sevenplayer.base.BaseActivity;
import com.qzr.sevenplayer.base.MessageWhat;
import com.qzr.sevenplayer.manager.QzrCameraManager;
import com.qzr.sevenplayer.manager.RecorderManager;
import com.qzr.sevenplayer.service.CameraSensor;
import com.qzr.sevenplayer.utils.HandlerProcess;
import com.qzr.sevenplayer.widget.FocusViewWidget;

import butterknife.BindView;

public class RecorderActivity extends BaseActivity implements TextureView.SurfaceTextureListener, View.OnTouchListener, CameraSensor.CameraSensorListener, View.OnClickListener, View.OnLongClickListener, HandlerProcess.HandlerCallback {

    private static final String TAG = "RecorderActivity";

    @BindView(R.id.texture_view)
    TextureView textureView;
    @BindView(R.id.btn_video)
    Button btnVideo;
    @BindView(R.id.focus_view)
    FocusViewWidget focusViewWidget;

    private boolean isFocusing = false;
    private CameraSensor mCameraSensor;

    private boolean isRecording = false;
    private RecorderManager recorderManager;


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void beforeSetContentView() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_recorder;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initView() {
        mCameraSensor = new CameraSensor(this);
        mCameraSensor.setCameraSensorListener(this);

        textureView.setOnTouchListener(this);

        btnVideo.setOnClickListener(this);
        btnVideo.setOnLongClickListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        QzrCameraManager.getInstance().buildCamera(surface).startPreView();
        //不设置camera callback，则onPreviewFrame不会进去
        QzrCameraManager.getInstance().setPreViewCallBack();
        mCameraSensor.startCameraSensor();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        QzrCameraManager.getInstance().stopPreView();
        focusViewWidget.cancelFocus();
        mCameraSensor.stopCameraSensor();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            focus((int) event.getX(), (int) event.getY(), false);
            return true;
        }
        return false;
    }

    private void focus(int x, int y, final boolean autoFocus) {
        if (isFocusing) {
            return;
        }
        isFocusing = true;

        Point focusPoint = new Point(x, y);
        Size screenSize = new Size(textureView.getWidth(), textureView.getHeight());

        if (!autoFocus) {
            focusViewWidget.beginFocus(x, y);
        }

        QzrCameraManager.getInstance().activeCameraFocus(focusPoint, screenSize, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                isFocusing = false;
                if (!autoFocus) {
                    focusViewWidget.endFocus(success);
                }
            }
        });
    }

    @Override
    public void onRock() {
        focus(textureView.getWidth() / 2, textureView.getHeight() / 2, true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_video: {
                RecorderManager.getInstance().takePicture();
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (isRecording) {
            HandlerProcess.getInstance().post(MessageWhat.STOP_RECORDER, 0, this);
        } else {
            HandlerProcess.getInstance().post(MessageWhat.START_RECORDER, 0, this);
        }
        return true;
    }

    @Override
    public void handleMsg(int what, Object o) {
        switch (what) {
            case MessageWhat.STOP_RECORDER: {
                RecorderManager.getInstance().stopRecord();
                isRecording = false;
                break;
            }
            case MessageWhat.START_RECORDER: {
                recorderManager = RecorderManager.getInstance().buildRecorder();
                recorderManager.startRecord();
                isRecording = true;
                break;
            }
            default:
                break;
        }
    }

}
