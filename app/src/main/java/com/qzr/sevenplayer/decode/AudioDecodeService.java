package com.qzr.sevenplayer.decode;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.nio.ByteBuffer;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.decode
 * @ClassName: AudioDecodeService
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/8/15 10:50
 */
public class AudioDecodeService {
    private static final String TAG = "AudioDecodeService";

    private static final int TIMEOUT_S = 10000;
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private MediaExtractor mMediaExtractor;
    private AudioTrack mAudioTrack;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    private boolean mDecoding = false;
    private boolean mTransmit = false;

    public static AudioDecodeService getInstance() {
        return AudioDecodeServiceHolder.audioDecodeService;
    }

    private static class AudioDecodeServiceHolder {
        private static AudioDecodeService audioDecodeService = new AudioDecodeService();
    }

    public AudioDecodeService() {
    }

    public synchronized AudioDecodeService buildAudioDecorderWithParam(String filePath) {
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(filePath);
            int audioTrack = selectMediaTrack(mMediaExtractor);
            if (audioTrack != -1) {
                mMediaExtractor.selectTrack(audioTrack);
            } else {
                throw new RuntimeException("audioTrack is -1");
            }
            int sampleRate = mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int minBuffferSize = AudioTrack.getMinBufferSize(sampleRate, channelCount, AudioFormat.ENCODING_PCM_16BIT);
            minBuffferSize = Math.max(minBuffferSize, 1024);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelCount, AudioFormat.ENCODING_PCM_16BIT, minBuffferSize, AudioTrack.MODE_STREAM);
            mMediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            mMediaCodec.configure(mMediaFormat, null, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "AudioDecodeService: error");
        }
        return this;
    }

    private int selectMediaTrack(MediaExtractor mMediaExtractor) {
        String mimeType;
        for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
            mMediaFormat = mMediaExtractor.getTrackFormat(i);
            mimeType = mMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mimeType.startsWith(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                return i;
            }
        }
        return -1;
    }

    public boolean startAudioDecode() {
        if (mDecoding) {
            Log.e(TAG, "startAudioDecode: is decoding");
            return true;
        }
        try {
            mAudioTrack.play();
            mMediaCodec.start();
            mDecoding = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startAudioDecode: error");
            mAudioTrack.stop();
            mMediaCodec.stop();
            releaseAudioDecode();
        }
        return mDecoding;
    }

    public void stopAudioDecoding() {
        mDecoding = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopAudioDecode() {
        mDecoding = false;
        if (mMediaCodec != null) {
            try {
                mAudioTrack.stop();
                mMediaCodec.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void releaseAudioDecode() {
        mMediaExtractor.release();
        mMediaExtractor = null;
        mMediaCodec.release();
        mMediaCodec = null;
        mAudioTrack.release();
        mAudioTrack = null;
        mTransmit = false;
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(transmit2AudioTrack);
    }

    public void startTransmit2AudioTrack() {
        if (!mTransmit) {
            mTransmit = true;
            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(transmit2AudioTrack);
        }
    }

    private Runnable transmit2AudioTrack = new Runnable() {
        @Override
        public void run() {
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
                            mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);
                            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

                            int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);

                            //每次解码完成的数据不一定能一次吐出，所以用while，保证解码器吐出所有数据
                            while (outputIndex > 0) {
                                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputIndex);
                                ////BufferInfo 内定义了此数据块的大小
                                byte[] chunkPCM = new byte[mBufferInfo.size];
                                outputBuffer.get(chunkPCM);
                                outputBuffer.clear();
                                mAudioTrack.write(chunkPCM, 0, mBufferInfo.size);
                                //此操作一定要做，不然 MediaCodec 用完所有的 Buffer 后 将不能向外输出数据
                                mMediaCodec.releaseOutputBuffer(outputIndex, true);
                                //再次获取数据，如果没有数据输出则 outputIndex=-1 循环结束
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

}
