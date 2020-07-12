package com.qzr.sevenplayer.encode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.qzr.sevenplayer.utils.ThreadPoolProxyFactory;

import java.util.ArrayList;

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

    private boolean mMixing = false;

    private MuxerParam mMuxerParam;

    private MediaMuxer mMediaMuxer;
    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private MediaCodec.BufferInfo mBufferInfo = null;

    private ArrayList<MuxerBuffer> mMuxerBufferArrayList;


    public class MuxerParam {
        public String parenPath;
        public String fileAbsolutePath;
        public String fileName;
        public String fileTypeName;
        public long fileCreateTime;
        public int fps;
        public int width;
        public int height;
        public boolean divide = false;
        public int divideSeq = 0;
    }

    public class MuxerBuffer {
        byte[] data;
        int length;
        int source;
        int flags;
        long timeStamp;
    }

    public MuxerParam getmMuxerParam() {
        return mMuxerParam;
    }

    public Mp4MuxerManager buildMp4MuxerManager(MuxerParam muxerParam) {
        mMuxerParam = muxerParam;
        mFilePath = muxerParam.fileAbsolutePath;
        mMp4Fps = muxerParam.fps;

        try {
            mMediaMuxer = new MediaMuxer(mFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mVideoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, muxerParam.width, muxerParam.height);
            mAudioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 16000, 1);

            mBufferInfo = new MediaCodec.BufferInfo();

            if (mMediaMuxer != null) {
                Log.i(TAG, "buildMp4MuxerManager: success");
            } else {
                Log.i(TAG, "buildMp4MuxerManager: failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public void muxer2File() {
        mMixing = true;
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(write2FileTask);
    }

    private Runnable write2FileTask = new Runnable() {
        @Override
        public void run() {

        }
    };

}
