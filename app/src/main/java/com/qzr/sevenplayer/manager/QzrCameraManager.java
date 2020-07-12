package com.qzr.sevenplayer.manager;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.qzr.sevenplayer.base.BaseApplication;
import com.qzr.sevenplayer.utils.CameraUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.manager
 * @ClassName: CameraManager
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/4 10:30
 */
public class QzrCameraManager implements Camera.ErrorCallback {

    private static final String TAG = "QzrCameraManager";

    private int previewErrorCnt = 0;

    private boolean cameraBuild = false;
    private boolean previewing = false;

    private Camera mCamera = null;
    private SurfaceTexture mSurfaceTexture;

    public static QzrCameraManager getInstance() {
        return QzrCameraManagerHolder.qzrCameraManager;
    }

    private static class QzrCameraManagerHolder {
        private static QzrCameraManager qzrCameraManager = new QzrCameraManager();
    }

    public static class CameraParams {
        public int preViewWidth = 1080;
        public int preViewHeight = 1920;
        public int picWidth = 1080;
        public int picHeight = 1920;
        public int fps = 30;
        public int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    public QzrCameraManager buildCamera(SurfaceTexture surfaceTexture) {
        Log.i(TAG, "buildCamera: ");
        CameraParams params = new CameraParams();
        return buildCameraWithParam(params, surfaceTexture);
    }

    public synchronized QzrCameraManager buildCameraWithParam(CameraParams cameraParams, SurfaceTexture surfaceTexture) {
        if (cameraBuild) {
            return this;
        }

        mSurfaceTexture = surfaceTexture;
        cameraBuild = true;

        if (mCamera != null) {
            releaseCamera();
        }
        mCamera = openCamera(cameraParams.cameraId);

        if (previewing) {
            return this;
        }

        Camera.Parameters parameters = mCamera.getParameters();

        mCamera.setDisplayOrientation(90);

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(parameters);

        //预览
        Camera.Size bestPreviewSize = CameraUtil.getOptimalSize(parameters.getSupportedPreviewSizes(), cameraParams.preViewWidth, cameraParams.preViewHeight);
        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        mCamera.setParameters(parameters);

        //拍照
        Camera.Size bestPicSize = CameraUtil.getOptimalSize(parameters.getSupportedPictureSizes(), cameraParams.picWidth, cameraParams.picHeight);
        parameters.setPreviewSize(bestPicSize.width, bestPicSize.height);
        mCamera.setParameters(parameters);

        //fps
        //        parameters.setPreviewFrameRate(cameraParams.fps);
        //        mCamera.setParameters(parameters);
        //
        //抗锯齿
        //        parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
        //        mCamera.setParameters(parameters);

        //低功耗
        //        parameters.set("low-power-mode", "enable");
        //        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (Exception e) {
            e.printStackTrace();
            cameraBuild = false;
        }

        mCamera.setErrorCallback(this);

        return this;
    }

    private Camera openCamera(int cameraId) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        try {
            mCamera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mCamera;
    }

    public void reStartCamera() {
        stopPreView();
        Log.i(TAG, "reStartCamera: ");
        buildCamera(mSurfaceTexture).startPreView();
    }

    public void releaseCamera() {
        cameraBuild = false;
        if (mCamera != null) {
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mCamera = null;
    }

    public synchronized boolean startPreView() {
        if (!cameraBuild) {
            return false;
        }
        if (mCamera != null) {
            Log.i(TAG, "startPreView: ");
            try {
                mCamera.startPreview();
                previewing = true;
                previewErrorCnt = 0;
            } catch (Exception e) {
                e.printStackTrace();
                releaseCamera();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                previewErrorCnt++;
                if (previewErrorCnt <= 3) {
                    if (mSurfaceTexture != null) {
                        return buildCamera(mSurfaceTexture).startPreView();
                    }
                } else {
                    previewErrorCnt = 0;
                    previewing = false;
                }
                Log.e(TAG, "startPreView error: " + e.getMessage());
            }
        }
        return previewing;
    }

    public synchronized void stopPreView() {
        if (previewing) {
            if (mCamera != null) {
                Log.i(TAG, "stopPreView: ");
                try {
                    mCamera.setPreviewTexture(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.stopPreview();
            }
        }
        previewing = false;
        releaseCamera();
    }

    public boolean isPreviewing() {
        return previewing;
    }

    public void takePicture(final TakePicDataCallBack dataCallBack, final int width, final int height) {
        // TODO: 2020/7/11 后续增加前后置camera判断
        if (previewing) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                // TODO: 2020/7/11 超分辨率，需要插值法
                parameters.setPictureSize(width, height);
                mCamera.setParameters(parameters);

                mCamera.takePicture(null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Toast.makeText(BaseApplication.getContext(), "拍照成功", Toast.LENGTH_SHORT).show();
                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        if (data != null) {
                            dataCallBack.takePicOnCall(data, width, height);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(BaseApplication.getContext(), "拍照失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean activeCameraFocus(Point focusPoint, Size screenSize, Camera.AutoFocusCallback focusCallback) {
        if (mCamera == null) {
            Log.d(TAG, "activeCameraFocus: camera is null");
        }
        Point cameraFocusPoint = CameraUtil.convertToCameraPoint(screenSize, focusPoint);
        Rect cameraFocusRect = CameraUtil.convertToCameraRect(cameraFocusPoint, 100);

        Camera.Parameters parameters = mCamera.getParameters();

        if (Build.VERSION.SDK_INT > 14) {
            if (parameters.getMaxNumFocusAreas() <= 0) {
                return focus(focusCallback);
            }
            clearCameraFocus();
            List<Camera.Area> focusAreas = new ArrayList<>();
            //100是权重
            focusAreas.add(new Camera.Area(cameraFocusRect, 100));
            parameters.setFocusAreas(focusAreas);
            //设置感光区域
            parameters.setMeteringAreas(focusAreas);
            try {
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                Log.d(TAG, "activeCameraFocus: error");
                e.printStackTrace();
                return false;
            }
        }
        return focus(focusCallback);
    }

    private void clearCameraFocus() {
        if (mCamera == null) {
            throw new RuntimeException("mCamera is null");
        }
        mCamera.cancelAutoFocus();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusAreas(null);
        parameters.setMeteringAreas(null);
        try {
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean focus(Camera.AutoFocusCallback autoFocusCallback) {
        if (mCamera == null) {
            return false;
        }
        mCamera.cancelAutoFocus();
        mCamera.autoFocus(autoFocusCallback);
        return true;
    }

    @Override
    public void onError(int error, Camera camera) {
        switch (error) {
            case Camera.CAMERA_ERROR_SERVER_DIED:
                Log.e(TAG, "CAMERA_ERROR_SERVER_DIED");
            case Camera.CAMERA_ERROR_UNKNOWN:
                Log.e(TAG, "CAMERA_ERROR_UNKNOWN");
                if (previewing) {
                    //restartCamera();
                }
                break;
            default:
        }
    }

    public interface TakePicDataCallBack {
        void takePicOnCall(byte[] data, int dwidth, int dheight);
    }

}
