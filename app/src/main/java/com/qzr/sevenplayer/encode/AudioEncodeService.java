package com.qzr.sevenplayer.encode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.util.ArrayList;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.encode
 * @ClassName: AudioEncodeService
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/12 10:34
 */
public class AudioEncodeService {

    private static final String TAG = "AudioEncodeService";

    private static AudioEncodeService instance;

    private static final String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    private ArrayList<OnEncodeDataAvailable> bufferAvailableCallback;


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

            bufferAvailableCallback = new ArrayList<>(2);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public synchronized void addCallback(OnEncodeDataAvailable onEncodeDataAvailable) {
        bufferAvailableCallback.add(onEncodeDataAvailable);
    }

}
