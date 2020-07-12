package com.qzr.sevenplayer.encode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.support.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArraySet;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.encode
 * @ClassName: VideoEncodeService
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 12:34
 */
public class VideoEncodeService {

    private static final String TAG = "VideoEncodeService";

    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;

    private static VideoEncodeService instance;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    private CopyOnWriteArraySet<OnEncodeDataAvailable> bufferAvailableCallback;


    public synchronized static VideoEncodeService getInstance() {
        if (instance == null || instance.mMediaCodec == null) {
            return VideoEncodeServiceHolder.videoEncodeService;
        }
        return instance;
    }

    private static class VideoEncodeServiceHolder {
        private static VideoEncodeService videoEncodeService = new VideoEncodeService();
    }

    public VideoEncodeService() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, 1920, 1080);

            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 8000);
            mMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatSurface);
            mMediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            mMediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);

            mMediaCodec.setCallback(mediacodecCallback);
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            bufferAvailableCallback = new CopyOnWriteArraySet<>();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MediaCodec.Callback mediacodecCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {

        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

        }
    };

    public synchronized void releaseVideoEncode() {
        mMediaCodec.release();
        mMediaCodec = null;
        bufferAvailableCallback.clear();
        bufferAvailableCallback = null;
        instance = null;
    }

    public synchronized void addCallBack(OnEncodeDataAvailable onEncodeDataAvailable) {
        bufferAvailableCallback.add(onEncodeDataAvailable);
    }
}
