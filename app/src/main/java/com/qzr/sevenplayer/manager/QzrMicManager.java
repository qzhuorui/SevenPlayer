package com.qzr.sevenplayer.manager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.qzr.sevenplayer.encode.QueueArray;
import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.manager
 * @ClassName: QzrMicManager
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/12 12:59
 */
public class QzrMicManager {

    private static final String TAG = "QzrMicManager";

    private boolean mHasMicBuild = false;
    private boolean mMicStarted = false;

    private AudioRecord mAudioRecord;

    private int mBufferSize;
    private int mMinInputBufferSize;

    private QueueArray mPcmBuffer;

    private CopyOnWriteArraySet<OnPcmDataGetListener> pcmDataListeners;

    public static QzrMicManager getInstance() {
        return QzrMicManagerHolder.qzrMicManager;
    }

    private static class QzrMicManagerHolder {
        private static QzrMicManager qzrMicManager = new QzrMicManager();
    }

    public QzrMicManager() {

    }

    public static class MicParam {
        public int SAMPLE_RATE = 16000; //采样率 8K或16K
        public int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; //音频通道(单声道)
        public int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; //音频格式
        public int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;  //音频源（麦克风）
    }

    public QzrMicManager buildMic() {
        MicParam micParam = new MicParam();
        return buildMicWithParam(micParam);
    }

    public QzrMicManager buildMicWithParam(MicParam micParam) {
        if (mHasMicBuild) {
            return this;
        }
        mHasMicBuild = true;
        mAudioRecord = createAudioRecord(micParam);
        mPcmBuffer = new QueueArray(mMinInputBufferSize * 50 * 10, "PCMBuffer");
        return this;
    }

    private AudioRecord createAudioRecord(MicParam micParam) {
        mBufferSize = micParam.SAMPLE_RATE * 20 / 1000 * (AudioFormat.ENCODING_PCM_16BIT == micParam.AUDIO_FORMAT ? 16 : 8) / 8;
        mMinInputBufferSize = AudioRecord.getMinBufferSize(micParam.SAMPLE_RATE, micParam.CHANNEL_CONFIG, micParam.AUDIO_FORMAT);
        return new AudioRecord(micParam.AUDIO_SOURCE, micParam.SAMPLE_RATE, micParam.CHANNEL_CONFIG, micParam.AUDIO_FORMAT, mMinInputBufferSize);
    }

    public void startMicManager() {
        if (mMicStarted) {
            return;
        }
        pcmDataListeners = new CopyOnWriteArraySet<OnPcmDataGetListener>();

        mPcmBuffer.clearQueue();

        if (mAudioRecord != null) {
            Log.i(TAG, "startMic: startMic");
            mAudioRecord.startRecording();
            mMicStarted = true;

            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(enqueuePcmTask);

            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(feedBackPcmTask);
        }

    }

    public void stopMicManager() {
        if (!mMicStarted) {
            return;
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }
        mPcmBuffer.clearQueue();
        mMicStarted = false;

        ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(enqueuePcmTask);
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(feedBackPcmTask);

        if (pcmDataListeners != null) {
            pcmDataListeners.clear();
            pcmDataListeners = null;
        }
    }

    public void releaseMicManager() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
        mMicStarted = false;
    }

    private Runnable enqueuePcmTask = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "run: enqueuePcmTask");
            int audioDataLen = 0;
            byte[] audioData = new byte[mBufferSize];

            while (mMicStarted) {
                audioDataLen = mAudioRecord.read(audioData, 0, mBufferSize);

                if (audioDataLen == 0) {
                    try {
                        Thread.sleep(2);
                        Log.e(TAG, "run: audioDataLen == 0 sleep");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (audioDataLen > 0) {
                    mPcmBuffer.enqueue(audioData, audioDataLen);
                }
            }
        }
    };

    private Runnable feedBackPcmTask = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "run: feedBackPcmTask");
            while (mMicStarted) {
                byte[] tmp = mPcmBuffer.dequeue(mBufferSize);

                if (pcmDataListeners != null && pcmDataListeners.size() > 0 && tmp != null) {
                    Iterator<OnPcmDataGetListener> iterator = pcmDataListeners.iterator();
                    while (iterator.hasNext()) {
                        OnPcmDataGetListener pcmDataGetListener = iterator.next();
                        pcmDataGetListener.feedPcmData(mBufferSize, tmp);
                    }
                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    public synchronized void addPcmDataGetCallback(OnPcmDataGetListener pcmDataGetListener) {
        if (pcmDataListeners != null) {
            pcmDataListeners.add(pcmDataGetListener);
        }
    }

    public synchronized void removePcmDataGetCallback(OnPcmDataGetListener pcmDataGetListener) {
        if (pcmDataListeners != null && pcmDataGetListener != null) {
            pcmDataListeners.remove(pcmDataGetListener);
        }
    }

    public interface OnPcmDataGetListener {
        public void feedPcmData(int len, byte[] data);
    }

}
