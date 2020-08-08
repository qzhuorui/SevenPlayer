package com.qzr.sevenplayer.encode;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.encode
 * @ClassName: MuxerBean
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/8/8 12:14
 */
public class MuxerBean {
    private ByteBuffer byteBuffer;
    private MediaCodec.BufferInfo bufferInfo;
    private boolean isVideo;

    public MuxerBean(byte[] bytes, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        this.byteBuffer = ByteBuffer.wrap(bytes);
        this.bufferInfo = bufferInfo;
        this.isVideo = isVideo;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return bufferInfo;
    }

    public boolean isVideo() {
        return isVideo;
    }
}
