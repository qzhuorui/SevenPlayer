package com.qzr.sevenplayer.encode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.qzr.sevenplayer.manager.QzrMicManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    private byte[] csd0 = null;
    private byte[] outputData;
    private ArrayList<byte[]> frameBytes;

    private ArrayList<OnEncodeDataAvailable> bufferAvailableCallback;

    private boolean mEncoding = false;
    private boolean mTransmit = false;

    int mFrontIndex = 0;
    int mLastIndex = 0;
    private final int LIST_SIZE = 10;

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
            frameBytes = new ArrayList<>(LIST_SIZE);
            bufferAvailableCallback = new ArrayList<>(2);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean startAudioEncode() {
        if (mEncoding) {
            Log.i(TAG, "startAudioEncode: is encoding!");
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

    public void stopAudioEncode(){
        if (!mEncoding){
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
        bufferAvailableCallback.clear();
        bufferAvailableCallback = null;
    }

    private void handlePCMdata(byte[] pcmBuffer) {
        int inputIndex;
        int outPutIndex;

        int outBitSize;

        ByteBuffer inputBuffer;
        ByteBuffer outputBuffer;

        ByteBuffer[] encodeInputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mMediaCodec.getOutputBuffers();
        MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();

        inputIndex = mMediaCodec.dequeueInputBuffer(0);//从input缓冲区申请  buffer的编号  index
        if (inputIndex < 0) {
            return;
        }
        inputBuffer = encodeInputBuffers[inputIndex];
        inputBuffer.clear();//清除数据，可能有上次写入的数据
        inputBuffer.limit(pcmBuffer.length);
        inputBuffer.put(pcmBuffer);//PCM数据填充进 inputBuffer

        mMediaCodec.queueInputBuffer(inputIndex, 0, pcmBuffer.length, 0, 0);//将 buffer 入 MediaCodec队列

        outPutIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);//申请处理完后的输出buffer
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
            outBitSize = encodeBufferInfo.size;
            outputData = new byte[outBitSize];

            outputBuffer = encodeOutputBuffers[outPutIndex];//拿到输出buffer
            outputBuffer.position(encodeBufferInfo.offset);
            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);

            outputBuffer.get(outputData, 0, outBitSize);//将编码得到的aac数据，取出到byte[]中

            if (outBitSize != 0 && encodeBufferInfo.flags != 2) {
                enqueueFrame(outputData, outBitSize);
            }

            outputBuffer.position(encodeBufferInfo.offset);
            mMediaCodec.releaseOutputBuffer(outPutIndex, false);
            outPutIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
        }
    }

    private synchronized void enqueueFrame(byte[] buffer, int pos) {
        byte[] tmp = new byte[pos];
        System.arraycopy(buffer, 0, tmp, 0, pos);

        while (true) {
            if ((mLastIndex + 1) % LIST_SIZE == mFrontIndex) {
                try {
                    wait();
                    Log.d(TAG, "enqueueFrame: wait");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (frameBytes.size() == LIST_SIZE) {
                    frameBytes.set(mLastIndex, tmp);
                } else {
                    frameBytes.add(tmp);
                }
                mLastIndex++;
                if (mLastIndex == LIST_SIZE) {
                    mLastIndex = 0;
                }
                break;
            }
        }
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
