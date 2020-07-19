package com.qzr.sevenplayer.encode;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.qzr.sevenplayer.base.MessageWhat;
import com.qzr.sevenplayer.utils.HandlerProcess;

import java.nio.ByteBuffer;
import java.util.ArrayList;
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
public class VideoEncodeService implements Camera.PreviewCallback, HandlerProcess.HandlerCallback {

    private static final String TAG = "VideoEncodeService";

    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;

    private static VideoEncodeService instance;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    private CopyOnWriteArraySet<OnEncodeDataAvailable> bufferAvailableCallback;

    private byte mVideoEncodeUseState = 0;
    public final static byte ENCODE_STATUS_NEED_NONE = 0;
    public final static byte ENCODE_STATUS_NEED_RECORD = 1;

    private boolean mEncoding = false;
    private boolean mTransmit = false;

    private byte[] sps = null;
    private byte[] pps = null;
    private byte[] outputData;

    int frontIndex = 0;
    int lastIndex = 0;
    private final int LIST_SIZE = 10;
    private ArrayList<byte[]> frameBytes;

    private Runnable handleYUVTask;

    public synchronized static VideoEncodeService getInstance() {
        if (instance == null || instance.mMediaCodec == null) {
            instance = new VideoEncodeService();
        }
        return instance;
    }

    public VideoEncodeService() {
        try {
            frameBytes = new ArrayList<>(LIST_SIZE);

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, 1920, 1080);

            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 8000);
            mMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatSurface);
            mMediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            mMediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);

            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            bufferAvailableCallback = new CopyOnWriteArraySet<>();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean startVideoEncode() {
        if (mEncoding) {
            Log.i(TAG, "startVideoEncode: is encoding!");
            return true;
        }
        try {
            mMediaCodec.start();
            mEncoding = true;
        } catch (Exception e) {
            Log.e(TAG, "startVideoEncode: mediaCodec.start() is error");
            e.printStackTrace();
            mMediaCodec.stop();
            releaseVideoEncode();
        }
        return mEncoding;
    }

    public void stopVideoEncoding() {
        mEncoding = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopVideoEncode() {
        mEncoding = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void releaseVideoEncode() {
        mMediaCodec.release();
        mMediaCodec = null;
        bufferAvailableCallback.clear();
        bufferAvailableCallback = null;
        instance = null;
    }

    private void handleYUVdata(byte[] yuvData) {
        // TODO: 2020/7/19 YUV颜色转换
        if (yuvData == null) {
            Log.d(TAG, "handleYUVdata: yuvData is null");
            return;
        }

        int inputIndex;
        int outputIndex;

        ByteBuffer inputBuffer;
        ByteBuffer outputBuffer;

        int outBitSize;

        //outtime，可以使用mMediaCodec.getInputBuffer(inputIndex)替换
        ByteBuffer[] encodeInputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mMediaCodec.getOutputBuffers();
        MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();

        inputIndex = mMediaCodec.dequeueInputBuffer(0);
        if (inputIndex < 0) {
            return;
        }
        inputBuffer = encodeInputBuffers[inputIndex];
        inputBuffer.clear();
        inputBuffer.limit(yuvData.length);
        inputBuffer.put(yuvData);
        mMediaCodec.queueInputBuffer(inputIndex, 0, yuvData.length, 0, 0);

        outputIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat format = mMediaCodec.getOutputFormat();
            ByteBuffer csdTmp = format.getByteBuffer("csd-0");
            sps = csdTmp.array();
            csdTmp = format.getByteBuffer("csd-1");
            pps = csdTmp.array();

            if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0) {
                for (OnEncodeDataAvailable onEncodeDataAvailable : bufferAvailableCallback) {
                    if (onEncodeDataAvailable != null) {
                        onEncodeDataAvailable.onCdsInfoUpdate(sps, pps, Mp4MuxerManager.SOURCE_VIDEO);
                    }
                }
            }
        }
        while (outputIndex >= 0) {
            outBitSize = encodeBufferInfo.size;
            outputData = new byte[outBitSize];

            outputBuffer = encodeOutputBuffers[outputIndex];
            outputBuffer.position(encodeBufferInfo.offset);
            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
            outputBuffer.get(outputData, 0, outBitSize);

            if (outBitSize != 0 && encodeBufferInfo.flags != 2) {
                enqueueFrame(outputData, outBitSize);
            }

            outputBuffer.position(encodeBufferInfo.offset);
            mMediaCodec.releaseOutputBuffer(outputIndex, false);
            outputIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
        }
    }

    private void enqueueFrame(byte[] outputData, int outBitSize) {
        byte[] tmp = new byte[outBitSize];
        System.arraycopy(outputData, 0, tmp, 0, outBitSize);
        while (true) {
            if ((lastIndex + 1) % LIST_SIZE == frontIndex) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (frameBytes.size() == LIST_SIZE) {
                    frameBytes.set(lastIndex, tmp);
                } else {
                    frameBytes.add(tmp);
                }
                lastIndex++;
                if (lastIndex == LIST_SIZE) {
                    lastIndex = 0;
                }
                break;
            }
        }
    }

    public synchronized byte getmVideoEncodeUseState() {
        return mVideoEncodeUseState;
    }

    public synchronized void addmVideoEncodeUseState(byte value) {
        mVideoEncodeUseState = (byte) (mVideoEncodeUseState | value);
    }

    public synchronized void removeEncoderUseStatus(byte value) {
        mVideoEncodeUseState = (byte) (mVideoEncodeUseState & (~value));
    }

    public synchronized void addCallBack(OnEncodeDataAvailable onEncodeDataAvailable) {
        bufferAvailableCallback.add(onEncodeDataAvailable);
        if (sps != null) {
            onEncodeDataAvailable.onCdsInfoUpdate(sps, pps, Mp4MuxerManager.SOURCE_VIDEO);
        }
    }

    public synchronized void removeCallBack(OnEncodeDataAvailable onEncodeDataAvailable) {
        if (bufferAvailableCallback != null) {
            bufferAvailableCallback.remove(onEncodeDataAvailable);
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!mEncoding) {
            return;
        }
        HandlerProcess.getInstance().postBG(MessageWhat.START_HANDLE_YUV, data, 0, this);
    }

    @Override
    public void handleMsg(int what, Object o) {
        switch (what) {
            case MessageWhat.START_HANDLE_YUV: {
                handleYUVdata((byte[]) o);
                break;
            }
            default:
                break;
        }
    }
}
