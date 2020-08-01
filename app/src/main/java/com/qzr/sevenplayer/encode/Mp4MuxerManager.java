package com.qzr.sevenplayer.encode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.encode
 * @ClassName: Mp4MuxerManager
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 13:21
 */
public class Mp4MuxerManager {

    private static final String TAG = "Mp4MuxerManager";

    private String mFilePath;
    private int mMp4Fps;

    private boolean mStarted = false;
    private boolean mMixing = false;

    private MuxerParam mMuxerParam;

    private MediaMuxer mMediaMuxer;
    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private MediaCodec.BufferInfo mBufferInfo = null;

    private long lastAudioTimestamp = 0;
    private long lastVideoTimestamp = 0;

    private int writeVideoIndex = -1;
    private int writeAudioIndex = -1;

    private Queue<MuxerBuffer> mixDataQueue;

    public static int SOURCE_AUDIO = 0;
    public static int SOURCE_VIDEO = 1;

    private Lock muxerWriteLock = new ReentrantLock();
    private final Object dataQueueSync = new Object();

    public class MuxerParam {
        public String fileName;
        public String fileTypeName;
        public long fileCreateTime;
        public String fileAbsolutePath;
        public int fps;
    }

    public class MuxerBuffer {
        byte[] data;
        int length;
        int source;
        int flags;
        long timeStamp;
    }

    public Mp4MuxerManager buildMp4MuxerManager(MuxerParam muxerParam) {
        mMuxerParam = muxerParam;
        mFilePath = muxerParam.fileAbsolutePath;
        mMp4Fps = muxerParam.fps;

        try {
            mMediaMuxer = new MediaMuxer(mFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mVideoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080);
            mAudioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 16000, 1);

            mBufferInfo = new MediaCodec.BufferInfo();

            mixDataQueue = new ArrayBlockingQueue<>(100);
        } catch (Exception e) {
            Log.e(TAG, "buildMp4MuxerManager: error");
            e.printStackTrace();
        }
        return this;
    }

    public synchronized void startMp4MuxerManager() {
        if (mMediaMuxer != null) {
            muxerWriteLock.lock();
            try {
                mMediaMuxer.start();
                mStarted = true;
            } catch (Exception e) {
                e.printStackTrace();
                releaseMp4MuxerManager();
                mMediaMuxer = null;
            }
            muxerWriteLock.unlock();
        }
    }

    public synchronized void stopMp4MuxerManager() {
        muxerWriteLock.lock();
        if (mMediaMuxer != null) {
            mMixing = false;
        }
        muxerWriteLock.unlock();

        synchronized (dataQueueSync) {
            dataQueueSync.notify();
        }
    }

    public synchronized void releaseMp4MuxerManager() {
        if (mMediaMuxer != null) {
            muxerWriteLock.lock();
            try {
                mMediaMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMediaMuxer = null;
            mVideoFormat = null;
            mAudioFormat = null;
            mBufferInfo = null;
            mStarted = false;
            mMixing = false;
            muxerWriteLock.unlock();
        }
    }

    public void buildSpsPpsParam(byte[] sps, byte[] pps) {
        byte[] spsTmp = new byte[sps.length];
        System.arraycopy(sps, 0, spsTmp, 0, sps.length);

        byte[] ppsTmp = new byte[pps.length];
        System.arraycopy(pps, 0, ppsTmp, 0, pps.length);

        mVideoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(spsTmp));
        mVideoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(ppsTmp));

        writeVideoIndex = mMediaMuxer.addTrack(mVideoFormat);
    }

    public void buildAdtsParam(byte[] adts) {
        byte[] adtsTmp = new byte[adts.length];
        System.arraycopy(adts, 0, adtsTmp, 0, adts.length);

        mAudioFormat.setByteBuffer("csd-0", ByteBuffer.wrap(adtsTmp));

        writeAudioIndex = mMediaMuxer.addTrack(mAudioFormat);
    }

    private boolean isKeyFrame(byte[] output) {
        if ((output[0] == 0x0) && (0x0 == output[1]) && (output[2] == 0x0) && (0x01 == output[3])) {
            if ((byte) (output[4] & 0x1f) == 5) {
                return true;
            }
        } else if ((output[0] == 0x0) && (0x0 == output[1]) && (output[2] == 0x01)) {

            if ((byte) (output[3] & 0x1f) == 5) {
                return true;
            }
        }
        return false;
    }

    private long getVideoTimeStamp() {
        return System.currentTimeMillis() * 1000;
    }

    private long getAudioTimeStamp() {
        return System.currentTimeMillis() * 1000;
    }

    public void muxerMix2Data(byte[] data, int length, int source) {
        if (!mStarted) {
            return;
        }
        if (mMediaMuxer != null) {
            MuxerBuffer muxerBuffer = new MuxerBuffer();
            if (source == SOURCE_VIDEO) {
                if (isKeyFrame(data)) {
                    muxerBuffer.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                } else {
                    muxerBuffer.flags = MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
                }
            }
            muxerBuffer.length = length;
            muxerBuffer.data = data;
            muxerBuffer.source = source;
            if (source == SOURCE_VIDEO) {
                muxerBuffer.timeStamp = getVideoTimeStamp();
            } else {
                muxerBuffer.timeStamp = getAudioTimeStamp();
            }
            synchronized (dataQueueSync) {
                Log.d(TAG, "muxerMix2Data: put data:" + (source == 1 ? "video" : "audio"));
                mixDataQueue.offer(muxerBuffer);
                dataQueueSync.notify();
            }
        }
    }

    public void muxerMix2File() {
        mMixing = true;
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(write2FileTask);
    }

    private Runnable write2FileTask = new Runnable() {
        @Override
        public void run() {
            boolean keyFlag = false;
            lastAudioTimestamp = 0;
            lastVideoTimestamp = 0;
            while (mMixing) {
                MuxerBuffer muxerBuffer = null;
                synchronized (dataQueueSync) {
                    int size = mixDataQueue.size();
                    if (size <= 0) {
                        try {
                            //sleep 不会释放锁，wait会释放
                            dataQueueSync.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    muxerBuffer = mixDataQueue.poll();
                }

                if (muxerBuffer != null && mBufferInfo != null && muxerBuffer.length > 0) {
                    ByteBuffer inputBuffer = ByteBuffer.allocate(muxerBuffer.length);
                    inputBuffer.put(muxerBuffer.data);

                    mBufferInfo.flags = muxerBuffer.flags;
                    mBufferInfo.size = muxerBuffer.length;
                    mBufferInfo.presentationTimeUs = muxerBuffer.timeStamp;
                    mBufferInfo.offset = 0;

                    if (muxerBuffer.source == SOURCE_VIDEO) {
                        if (keyFlag) {
                            syncWriteBuffer(writeVideoIndex, inputBuffer, mBufferInfo);
                        } else {
                            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                syncWriteBuffer(writeVideoIndex, inputBuffer, mBufferInfo);
                                keyFlag = true;
                            }
                        }
                    } else if (muxerBuffer.source == SOURCE_AUDIO) {
                        syncWriteBuffer(writeAudioIndex, inputBuffer, mBufferInfo);
                    }
                    inputBuffer.clear();
                }
            }
        }
    };

    private void syncWriteBuffer(int mediaIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        muxerWriteLock.lock();
        if (mediaIndex == writeVideoIndex) {
            if (lastVideoTimestamp != 0) {
                if (lastVideoTimestamp >= bufferInfo.presentationTimeUs) {
                    bufferInfo.presentationTimeUs = lastVideoTimestamp + 1000;
                    Log.e(TAG, "syncWriteBuffer: video time error");
                }
            }
        } else if (mediaIndex == writeAudioIndex) {
            if (lastAudioTimestamp != 0) {
                if (lastAudioTimestamp >= bufferInfo.presentationTimeUs) {
                    bufferInfo.presentationTimeUs = lastAudioTimestamp + 1000;
                    Log.e(TAG, "syncWriteBuffer: audio time error");
                }
            }
        }

        if (mMixing && mStarted) {
            try {
                mMediaMuxer.writeSampleData(mediaIndex, byteBuffer, bufferInfo);
                if (mediaIndex == writeVideoIndex) {
                    Log.i(TAG, "syncWriteBuffer: write video");
                } else {
                    Log.i(TAG, "syncWriteBuffer: write audio");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "syncWriteBuffer: mediamuxer write error1");
            }
        } else {
            Log.e(TAG, "syncWriteBuffer: mediamuxer write error2");
        }

        if (mediaIndex == writeVideoIndex) {
            lastVideoTimestamp = bufferInfo.presentationTimeUs;
        } else {
            lastAudioTimestamp = bufferInfo.presentationTimeUs;
        }
        muxerWriteLock.unlock();
    }

}
