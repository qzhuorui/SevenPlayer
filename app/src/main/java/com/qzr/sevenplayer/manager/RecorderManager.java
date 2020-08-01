package com.qzr.sevenplayer.manager;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.util.Log;
import android.widget.Toast;

import com.qzr.sevenplayer.base.BaseApplication;
import com.qzr.sevenplayer.base.MessageWhat;
import com.qzr.sevenplayer.encode.AudioEncodeService;
import com.qzr.sevenplayer.encode.Mp4MuxerManager;
import com.qzr.sevenplayer.encode.OnEncodeDataAvailable;
import com.qzr.sevenplayer.encode.VideoEncodeService;
import com.qzr.sevenplayer.utils.HandlerProcess;
import com.qzr.sevenplayer.utils.PermissionHelper;
import com.qzr.sevenplayer.utils.StorageUtil;
import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.manager
 * @ClassName: RecorderManager
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 9:35
 */
public class RecorderManager implements QzrCameraManager.TakePicDataCallBack, HandlerProcess.HandlerCallback, OnEncodeDataAvailable {

    private static final String TAG = "RecorderManager";

    private boolean hasBuild = false;
    public boolean mEncodeStarted = false;
    public boolean mRecording2File = false;

    private int mCurTrackCount;
    private int mNeedTrackCount;

    private VideoEncodeService mVideoEncodeService;
    private AudioEncodeService mAudioEncodeService;
    private Mp4MuxerManager mMp4MuxerManager;


    public static RecorderManager getInstance() {
        return RecorderManagerHolder.recorderManager;
    }

    private static class RecorderManagerHolder {
        private static RecorderManager recorderManager = new RecorderManager();
    }

    public RecorderManager() {
    }

    public class RecorderParam {
        public Date date = new Date();
        public String fileNamePrefix = StorageUtil.spFormat.get().format(date);
        public String videoFilePath = StorageUtil.getOutPutVideoFile().getAbsolutePath();
    }

    public RecorderManager buildRecorder() {
        RecorderParam param = new RecorderParam();
        return buildRecorderWithParam(param);
    }

    private synchronized RecorderManager buildRecorderWithParam(RecorderParam param) {
        if (hasBuild) {
            return this;
        }
        hasBuild = true;
        mCurTrackCount = 0;
        mNeedTrackCount = 2;//video 轨道数为2

        /**
         * 构建MediaMuxer
         */
        mMp4MuxerManager = new Mp4MuxerManager();
        Mp4MuxerManager.MuxerParam muxerParam = mMp4MuxerManager.new MuxerParam();
        muxerParam.fps = 30;
        muxerParam.fileName = param.fileNamePrefix + "_VIDEO";
        muxerParam.fileTypeName = ".mp4";
        muxerParam.fileCreateTime = System.currentTimeMillis();
        muxerParam.fileAbsolutePath = param.videoFilePath;
        mMp4MuxerManager.buildMp4MuxerManager(muxerParam);

        /**
         * 构建VideoEncodec
         */
        mVideoEncodeService = VideoEncodeService.getInstance();
        mVideoEncodeService.addCallBack(this);

        /**
         * 构建AudioEncodec
         */
        mAudioEncodeService = AudioEncodeService.getInstance();
        mAudioEncodeService.addCallback(this);

        return this;
    }

    public void takePicture() {
        HandlerProcess.getInstance().postBG(MessageWhat.TAKE_PIC, 0, this);
    }

    private void savePicture(Bitmap bitmap) {
        File picture = StorageUtil.getOutPutImageFile();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(picture.getPath());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean startRecord() {
        //检查权限，存储
        if (!checkMicPermission()) {
            return false;
        }
        if (!hasBuild) {
            throw new RuntimeException("recorder manager not build");
        }

        //start Video encoder
        if (!startVideoModule()) {
            Log.d(TAG, "startRecord: startVideoModule is error");
            releaseRecord();
            return false;
        }

        //start Audio encoder
        if (!startAudioModule()) {
            Log.d(TAG, "startRecord: startAudioModule is error");
            releaseVideoModule();
            releaseRecord();
            return false;
        }

        mEncodeStarted = true;

        //muxer mix to file
        startRecordMix2File();

        //feedback pcm data
        startMicRun();

        return true;
    }

    public synchronized boolean stopRecord() {
        mEncodeStarted = false;
        if (mVideoEncodeService == null) {
            return false;
        }

        /**
         * Video。无其他地方使用，直接释放encoder
         */
        mVideoEncodeService.removeEncoderUseStatus(VideoEncodeService.ENCODE_STATUS_NEED_RECORD);
        if (mVideoEncodeService.getmVideoEncodeUseState() == VideoEncodeService.ENCODE_STATUS_NEED_NONE) {
            mVideoEncodeService.stopVideoEncoding();
            mVideoEncodeService.removeCallBack(this);
            mVideoEncodeService.stopVideoEncode();
            mVideoEncodeService.releaseVideoEncode();
            mVideoEncodeService = null;
        } else {
            mVideoEncodeService.removeCallBack(this);
            mCurTrackCount--;
        }

        /**
         * Audio
         */
        mAudioEncodeService.stopAudioEncoding();
        mAudioEncodeService.removeCallback(this);
        mAudioEncodeService.stopAudioEncode();
        mAudioEncodeService.releaseAudioEncode();
        mAudioEncodeService = null;

        releaseRecord();

        QzrMicManager.getInstance().removePcmDataGetCallback(mAudioEncodeService);
        QzrMicManager.getInstance().stopMicManager();

        stopRecordMix2File();

        return true;
    }

    private boolean releaseRecord() {
        hasBuild = false;
        return true;
    }

    private boolean startAudioModule() {
        return mAudioEncodeService.startAudioEncode();
    }

    private boolean startVideoModule() {
        if (mVideoEncodeService == null) {
            Log.d(TAG, "startVideoModule: mVideoEncodeService is null");
            return false;
        }
        if (mVideoEncodeService.getmVideoEncodeUseState() == VideoEncodeService.ENCODE_STATUS_NEED_NONE) {
            //开启video编码
            boolean state = mVideoEncodeService.startVideoEncode();
            if (!state) {
                return false;
            }
        }
        //改变videoEncodec状态
        mVideoEncodeService.addmVideoEncodeUseState(VideoEncodeService.ENCODE_STATUS_NEED_RECORD);
        return true;
    }

    private void releaseVideoModule() {
        mVideoEncodeService.removeEncoderUseStatus(VideoEncodeService.ENCODE_STATUS_NEED_RECORD);
        if (mVideoEncodeService.getmVideoEncodeUseState() == VideoEncodeService.ENCODE_STATUS_NEED_NONE) {
            mVideoEncodeService.stopVideoEncoding();
            mVideoEncodeService.stopVideoEncode();
        }
    }

    private void startRecordMix2File() {
        mRecording2File = true;
        if (mMp4MuxerManager != null) {
            mMp4MuxerManager.muxerMix2File();
        }
    }

    private void stopRecordMix2File() {
        if (!mRecording2File) {
            return;
        }
        mRecording2File = false;
        if (mMp4MuxerManager != null) {
            mMp4MuxerManager.stopMp4MuxerManager();
            mMp4MuxerManager.releaseMp4MuxerManager();
            mMp4MuxerManager = null;
            hasBuild = false;
        }
    }

    private void startMicRun() {
        AudioManager am = (AudioManager) BaseApplication.getContext().getSystemService(Context.AUDIO_SERVICE);
        am.setMicrophoneMute(true);
        HandlerProcess.getInstance().postDelayedOnBg(new Runnable() {
            @Override
            public void run() {
                AudioManager am = (AudioManager) BaseApplication.getContext().getSystemService(Context.AUDIO_SERVICE);
                am.setMicrophoneMute(false);
            }
        }, 1500);
        QzrMicManager.getInstance().buildMic().startMicManager();
        QzrMicManager.getInstance().addPcmDataGetCallback(mAudioEncodeService);
    }

    private boolean checkMicPermission() {
        boolean micPermission = PermissionHelper.hasPermission(BaseApplication.getContext(), Manifest.permission.RECORD_AUDIO);
        if (!micPermission) {
            Toast.makeText(BaseApplication.getContext(), "无麦克风权限", Toast.LENGTH_SHORT).show();
        }
        return micPermission;
    }

    private void startTransmit() {
        if (mMp4MuxerManager == null) {
            return;
        }
        //设置过track，才能start muxer
        mMp4MuxerManager.startMp4MuxerManager();
        if (mVideoEncodeService != null) {
            mVideoEncodeService.startTransmitVideoData();
        }
        if (mAudioEncodeService != null) {
            mAudioEncodeService.startTransmitAudioData();
        }
    }

    @Override
    public void handleMsg(int what, Object o) {
        switch (what) {
            case MessageWhat.TAKE_PIC: {
                QzrCameraManager.getInstance().takePicture(this, 1920, 1080);
                break;
            }
            default:
                break;
        }
    }

    /**
     * 拍照数据回调
     *
     * @param data
     * @param dwidth
     * @param dheight
     */
    @Override
    public void takePicOnCall(final byte[] data, int dwidth, int dheight) {
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                savePicture(bitmap);
                bitmap.recycle();
            }
        });
    }

    /**
     * 编解码相关回调
     *
     * @param csd0
     * @param csd1
     * @param source
     */
    @Override
    public void onCdsInfoUpdate(byte[] csd0, byte[] csd1, int source) {
        if (mMp4MuxerManager == null) {
            return;
        }
        if (source == Mp4MuxerManager.SOURCE_VIDEO) {
            Log.d(TAG, "onCdsInfoUpdate: video");
            byte[] tmpSps = new byte[csd0.length];
            System.arraycopy(csd0, 0, tmpSps, 0, csd0.length);

            byte[] tmpPps = new byte[csd1.length];
            System.arraycopy(csd1, 0, tmpPps, 0, csd1.length);

            if (mMp4MuxerManager != null) {
                mMp4MuxerManager.buildSpsPpsParam(tmpSps, tmpPps);
            }
        } else {
            Log.d(TAG, "onCdsInfoUpdate: audio");
            byte[] tmpAdts = new byte[csd0.length];
            System.arraycopy(csd0, 0, tmpAdts, 0, csd0.length);
            mMp4MuxerManager.buildAdtsParam(tmpAdts);
        }

        mCurTrackCount++;

        if (mCurTrackCount >= mNeedTrackCount) {
            startTransmit();
        }
    }

    /**
     * 编解码相关回调
     *
     * @param data
     * @param source
     */
    @Override
    public void onEncodeBufferAvailable(byte[] data, int source) {
        Log.d(TAG, "onEncodeBufferAvailable: " + (source == 1 ? "video" : "audio") + " data: " + (data != null));
        if (mCurTrackCount <= 0) {
            stopRecordMix2File();
        }
        if (mMp4MuxerManager != null && mEncodeStarted && data != null) {
            byte[] tmpData = new byte[data.length];
            System.arraycopy(data, 0, tmpData, 0, data.length);
            mMp4MuxerManager.muxerMix2Data(tmpData, tmpData.length, source);
        }
    }

}
