package com.qzr.sevenplayer.encode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.qzr.sevenplayer.manager.QzrMicManager;
import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.encode
 * @ClassName: AudioEncodeService
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/12 10:34
 */
public class AudioEncodeService implements QzrMicManager.OnPcmDataGetListener {

    private static final String TAG = "AudioEncodeService";

    private static AudioEncodeService instance;

    private static final String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int TIMEOUT_S = 10000;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    private byte[] csd0 = null;
    private Queue<MuxerBean> encodeDataQueue;

    private ArrayList<OnEncodeDataAvailable> bufferAvailableCallback;

    private boolean mEncoding = false;
    private boolean mTransmit = false;

    public synchronized static AudioEncodeService getInstance() {
        if (instance == null || instance.mMediaCodec == null) {
            instance = new AudioEncodeService();
        }
        return instance;
    }

    public AudioEncodeService() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 16000, 1);

            int bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 32000);
            mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
            mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);

            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            encodeDataQueue = new LinkedList<>();
            bufferAvailableCallback = new ArrayList<>(2);

        } catch (Exception e) {
            Log.e(TAG, "AudioEncodeService: audio encoder config error");
            e.printStackTrace();
        }
    }

    public boolean startAudioEncode() {
        if (mEncoding) {
            Log.e(TAG, "startAudioEncode: is encoding!");
            return true;
        }
        try {
            mMediaCodec.start();
            mEncoding = true;
        } catch (Exception e) {
            Log.e(TAG, "startAudioEncode: mMediaCodec.start() is error");
            e.printStackTrace();
        }
        return mEncoding;
    }

    public void stopAudioEncoding() {
        mEncoding = false;
    }

    public void stopAudioEncode() {
        if (!mEncoding) {
            return;
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            mEncoding = false;
            mMediaCodec.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releaseAudioEncode() {
        mMediaCodec.release();
        mMediaCodec = null;
        encodeDataQueue.clear();
        bufferAvailableCallback.clear();
        bufferAvailableCallback = null;
        encodeDataQueue = null;
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(transmitAudioDataTask);
    }

    private void handlePCMdata(byte[] pcmBuffer) {
        if (pcmBuffer != null) {
            try {
                int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);//从input缓冲区申请  buffer的编号  index
                if (inputIndex >= 0) {
                    long pts = getPts();
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                    inputBuffer.clear();//清除数据，可能有上次写入的数据
                    inputBuffer.put(pcmBuffer);//PCM数据填充进 inputBuffer
                    inputBuffer.limit(pcmBuffer.length);
                    mMediaCodec.queueInputBuffer(inputIndex, 0, pcmBuffer.length, pts, 0);//将 buffer 入 MediaCodec队列
                }

                MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();
                int outPutIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, TIMEOUT_S);//申请处理完后的输出buffer

                if (outPutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat mediaFormat = mMediaCodec.getOutputFormat();
                    ByteBuffer byteBuffer = mediaFormat.getByteBuffer("csd-0");
                    csd0 = byteBuffer.array();

                    if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0) {
                        for (OnEncodeDataAvailable onEncodeDataAvailable : bufferAvailableCallback) {
                            if (onEncodeDataAvailable != null) {
                                onEncodeDataAvailable.onCdsInfoUpdate(csd0, null, Mp4MuxerManager.SOURCE_AUDIO);
                            }
                        }
                    }
                }

                while (outPutIndex >= 0) {
                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outPutIndex);//拿到输出buffer

                    if (encodeBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        encodeBufferInfo.size = 0;
                    }

                    if (encodeBufferInfo.size > 0) {
                        byte[] outputData = new byte[encodeBufferInfo.size];
                        outputBuffer.get(outputData);//将编码得到的aac数据，取出到byte[]中
                        outputBuffer.position(encodeBufferInfo.offset);
                        outputBuffer.limit(encodeBufferInfo.offset + encodeBufferInfo.size);
                        encodeBufferInfo.presentationTimeUs = getPts();
                        enqueueFrame(new MuxerBean(outputData, encodeBufferInfo, false));
                    }

                    mMediaCodec.releaseOutputBuffer(outPutIndex, false);
                    encodeBufferInfo = new MediaCodec.BufferInfo();
                    outPutIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, TIMEOUT_S);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "handlePCMdata: PCMData is null");
        }
    }

    private synchronized void enqueueFrame(MuxerBean muxerBean) {
        encodeDataQueue.offer(muxerBean);
    }

    private long getPts() {
        return System.nanoTime() / 1000L;
    }

    private long getAudioTimeStamp() {
        return System.currentTimeMillis() * 1000;
    }

    public void startTransmitAudioData() {
        Log.i(TAG, "startTransmitAudioData: ");
        if (!mTransmit) {
            mTransmit = true;
            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(transmitAudioDataTask);
        }
    }

    private Runnable transmitAudioDataTask = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (encodeDataQueue == null) {
                    mTransmit = false;
                } else {
                    MuxerBean tmpEncodeData = dequeueFrame();
                    if (tmpEncodeData == null && !mEncoding) {
                        mTransmit = false;
                    }
                    if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0 && tmpEncodeData != null) {
                        for (OnEncodeDataAvailable onEncodeDataAvailable : bufferAvailableCallback) {
                            if (onEncodeDataAvailable != null) {
                                onEncodeDataAvailable.onEncodeBufferAvailable(tmpEncodeData, Mp4MuxerManager.SOURCE_AUDIO);
                            }
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
                                onEncodeDataAvailable.onEncodeBufferAvailable(null, Mp4MuxerManager.SOURCE_AUDIO);
                            }
                        }
                    }
                    break;
                }
            }
        }
    };

    private synchronized MuxerBean dequeueFrame() {
        return encodeDataQueue.poll();
    }

    public synchronized void addCallback(OnEncodeDataAvailable onEncodeDataAvailable) {
        bufferAvailableCallback.add(onEncodeDataAvailable);
    }

    public synchronized void removeCallback(OnEncodeDataAvailable available) {
        bufferAvailableCallback.remove(available);
    }

    @Override
    public void feedPcmData(int len, byte[] data) {
        if (!mEncoding) {
            return;
        }
        byte[] buffer = new byte[len];
        System.arraycopy(data, 0, buffer, 0, len);
        handlePCMdata(buffer);
    }

}
