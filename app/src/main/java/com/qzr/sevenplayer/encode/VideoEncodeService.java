package com.qzr.sevenplayer.encode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

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

    private byte mVideoEncodeUseState = 0;
    public final static byte ENCODE_STATUS_NEED_NONE = 0;
    public final static byte ENCODE_STATUS_NEED_RECORD = 1;
    private static final int TIMEOUT_S = 10000;

    private boolean mEncoding = false;
    private boolean mTransmit = false;

    private byte[] sps = null;
    private byte[] pps = null;
    private byte[] outputData;

    int frontIndex = 0;
    int lastIndex = 0;
    private final int LIST_SIZE = 10;
    private ArrayList<byte[]> frameBytes;

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
            mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mMediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            mMediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);

            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            bufferAvailableCallback = new CopyOnWriteArraySet<>();

        } catch (Exception e) {
            Log.e(TAG, "VideoEncodeService: video encoder config error");
            e.printStackTrace();
        }
    }

    public boolean startVideoEncode() {
        if (mEncoding) {
            Log.d(TAG, "startVideoEncode: is encoding!");
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
        Log.d(TAG, "releaseVideoEncode: ");
        mMediaCodec.release();
        mMediaCodec = null;
        lastIndex = frontIndex = 0;
        frameBytes.clear();
        bufferAvailableCallback.clear();
        bufferAvailableCallback = null;
        frameBytes = null;
        instance = null;
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(transmitVideoDataTask);
    }

    public void handleYUVdata(byte[] yuvData) {
        if (yuvData != null) {
            try {
                int inputIndex = mMediaCodec.dequeueInputBuffer(0);
                if (inputIndex >= 0) {
                    long pts = getPts();
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                    inputBuffer.clear();
                    inputBuffer.put(yuvData);
                    mMediaCodec.queueInputBuffer(inputIndex, 0, yuvData.length, pts, 0);
                }

                MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();
                int outputIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);

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
                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputIndex);

                    if (encodeBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        encodeBufferInfo.size = 0;
                    }

                    if (encodeBufferInfo.size > 0) {
                        byte[] outputData = new byte[encodeBufferInfo.size];
                        outputBuffer.get(outputData);
                        outputBuffer.position(encodeBufferInfo.offset);
                        outputBuffer.limit(encodeBufferInfo.offset + encodeBufferInfo.size);
                        enqueueFrame(outputData, encodeBufferInfo.size);
                    }

                    mMediaCodec.releaseOutputBuffer(outputIndex, false);
                    encodeBufferInfo = new MediaCodec.BufferInfo();
                    outputIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "handleYUVdata: YUVData is null");
        }
    }

    private synchronized void enqueueFrame(byte[] outputData, int outBitSize) {
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

    public void startTransmitVideoData() {
        Log.d(TAG, "startTransmitVideoData: ");
        if (!mTransmit) {
            mTransmit = true;
            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(transmitVideoDataTask);
        }
    }

    private Runnable transmitVideoDataTask = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (frameBytes == null) {
                    mTransmit = false;
                } else {
                    Log.i(TAG, "run: transmitVideoDataTask dequeueFrame");
                    byte[] tmpEncodeData = dequeueFrame();
                    if (tmpEncodeData == null && !mEncoding) {
                        mTransmit = false;
                    }
                    if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0 && tmpEncodeData != null) {
                        Iterator<OnEncodeDataAvailable> iterator = bufferAvailableCallback.iterator();
                        while (iterator.hasNext()) {
                            OnEncodeDataAvailable onEncodeDataAvailable = iterator.next();
                            onEncodeDataAvailable.onEncodeBufferAvailable(tmpEncodeData, Mp4MuxerManager.SOURCE_VIDEO);
                        }
                    } else if (tmpEncodeData == null) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!mTransmit) {
                    if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0) {
                        for (OnEncodeDataAvailable onEncodeDataAvailable : bufferAvailableCallback) {
                            if (onEncodeDataAvailable != null) {
                                onEncodeDataAvailable.onEncodeBufferAvailable(null, Mp4MuxerManager.SOURCE_VIDEO);
                            }
                        }
                    }
                    break;
                }
            }
        }
    };

    private synchronized byte[] dequeueFrame() {
        if (validSize() > 0 && frameBytes.size() > 0) {
            byte[] tmp = frameBytes.get(frontIndex);
            frontIndex++;
            if (frontIndex == LIST_SIZE) {
                frontIndex = 0;
            }
            notifyAll();
            return tmp;
        }
        notifyAll();
        return null;
    }

    private int validSize() {
        return (lastIndex + LIST_SIZE - frontIndex) % LIST_SIZE;
    }

    public synchronized byte getmVideoEncodeUseState() {
        return mVideoEncodeUseState;
    }

    private long getPts() {
        return System.nanoTime() / 1000L;
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
            //后进来的回调保证能得到 sps,pps
            onEncodeDataAvailable.onCdsInfoUpdate(sps, pps, Mp4MuxerManager.SOURCE_VIDEO);
        }
    }

    public synchronized void removeCallBack(OnEncodeDataAvailable onEncodeDataAvailable) {
        if (bufferAvailableCallback != null) {
            bufferAvailableCallback.remove(onEncodeDataAvailable);
        }
    }
}
