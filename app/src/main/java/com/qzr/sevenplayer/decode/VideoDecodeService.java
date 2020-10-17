package com.qzr.sevenplayer.decode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.nio.ByteBuffer;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.decode
 * @ClassName: VideoDecodeService
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/8/15 10:50
 */
public class VideoDecodeService {
    private static final String TAG = "VideoDecodeService";

    private static final int TIMEOUT_S = 10000;
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private MediaExtractor mMediaExtractor;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private int mFrameRate;

    private boolean mDecoding = false;
    private boolean mTransmit = false;

    public static VideoDecodeService getInstance() {
        return VideoDecodeServiceHolder.videoDecodeService;
    }

    private static class VideoDecodeServiceHolder {
        private static VideoDecodeService videoDecodeService = new VideoDecodeService();
    }

    public VideoDecodeService() {
    }

    public synchronized VideoDecodeService buildVideoDecorderWithParam(String filePath, Surface surface) {
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(filePath);
            int videoTrack = selectMediaTrack(mMediaExtractor);
            if (videoTrack != -1) {
                mMediaExtractor.selectTrack(videoTrack);
            } else {
                throw new RuntimeException("videoTrack is -1");
            }
            mMediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            mMediaCodec.configure(mMediaFormat, surface, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "VideoDecodeService: error");
        }
        return this;
    }

    private int selectMediaTrack(MediaExtractor mMediaExtractor) {
        String mimeType;
        for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
            //获取文件对应track的信息
            mMediaFormat = mMediaExtractor.getTrackFormat(i);
            mFrameRate = mMediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            mimeType = mMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mimeType.startsWith(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                //得到video对应的track
                return i;
            }
        }
        return -1;
    }

    public boolean startVideoDecode() {
        if (mDecoding) {
            Log.e(TAG, "startVideoDecode: is decoding");
            return true;
        }
        try {
            mMediaCodec.start();
            mDecoding = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startVideoDecode: error");
            mMediaCodec.stop();
            releaseVideoDecode();
        }
        return mDecoding;
    }

    public void stopVideoDecoding() {
        mDecoding = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopVideoDecode() {
        mDecoding = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void releaseVideoDecode() {
        mMediaExtractor.release();
        mMediaExtractor = null;
        mMediaCodec.release();
        mMediaCodec = null;
        mTransmit = false;
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(transmit2SurfaceTask);
    }

    public void startTransmit2Surface() {
        if (!mTransmit) {
            mTransmit = true;
            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(transmit2SurfaceTask);
        }
    }

    private Runnable transmit2SurfaceTask = new Runnable() {
        @Override
        public void run() {
            long startMs = System.currentTimeMillis();
            int index = 0;
            while (true) {
                try {
                    if (!mDecoding) {
                        break;
                    }
                    int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                        inputBuffer.clear();
                        int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                        if (sampleSize > 0) {
                            //这个时间戳是最关键的，体现的是音视频呈现的时间。
                            mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, getPts(index++, mFrameRate), 0);
                            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

                            int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
                            while (outputIndex > 0) {
                                //帧控制，一帧解码出来后，计算sysTime和startMs距离==已播放时间
                                while (mBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                                    Thread.sleep(50);
                                }
                                mMediaCodec.releaseOutputBuffer(outputIndex, true);
                                outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
                            }
                            if (!mMediaExtractor.advance()) {
                                break;
                            }
                        } else {
                            break;
                        }
                    } else {
                        Thread.sleep(50);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /*
   frame/rate=time(sec)
     */
    private long getPts(int index, int frameRate) {
        return index * 1000000 / frameRate;
    }

}
