/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.xwr.videocode;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author lth
 * @date   2019.10.30
 * @note   H264硬件编码
 *
 */
@SuppressLint("NewApi")
public class VideoH264Encoder {
    // 编码器
    private MediaCodec mMediaCodec;
    // 编码比特率
    private int mBitRate;
    // 编码帧率
    private int mFrameRate;
    // 关键帧间隔
    private int mFrameInterval;
    // 帧宽度
    private int mWidth;
    // 帧高度
    private int mHeight;
    // 编码信息
    private  MediaCodec.BufferInfo mBufferInfo;
    // 编码空间
    private int COLOR_FORMAI = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
    // 编码缓存
    private byte[] mInput;

    public VideoH264Encoder(int mBitRate, int mFrameRate, int mFrameInterval, int mWidth, int mHeight) {
        this.mBitRate = mBitRate;
        this.mFrameRate = mFrameRate;
        this.mFrameInterval = mFrameInterval;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
    }

    /**
     * 初始化编码器
     * */

    public boolean initAndStart() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAI);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mFrameInterval);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mBufferInfo = new MediaCodec.BufferInfo();
            mInput = new byte[mWidth*mHeight*3/2];
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 编码并返回编码数据
     * @param inData 需要编码的数据
     * @param timeOut 编码超时
     * @return null 编码失败
     * */
    public byte[] offerEncoder(byte[] inData,long timeOut) {
        byte[] rest = null;
        NV21ToNV12(inData,mInput,640,480);
        try {
            // 输入编码数据
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(timeOut);
            if (inputBufferIndex >= 0)
            {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(mInput);
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mInput.length, 0, 0);
            } else {
                return rest;
            }

            // 获取编码数据
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                // MediaCodec is asynchronous, that's why we have a blocking check
                // to see if we have something to do
                int status = mMediaCodec.dequeueOutputBuffer(mBufferInfo, timeOut);
                if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    return rest;
                } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = mMediaCodec.getOutputBuffers();
                } else if (status >= 0) {
                    // encoded sample
                    ByteBuffer data = outputBuffers[status];
                    final int endOfStream = mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    // pass to whoever listens to
                    if (endOfStream == 0){
                        rest = new byte[mBufferInfo.size];
                        data.position(mBufferInfo.offset);
                        data.limit(mBufferInfo.offset + mBufferInfo.size);
                        data.get(rest, 0, mBufferInfo.size);
                    }
                    // releasing buffer is important
                    mMediaCodec.releaseOutputBuffer(status, false);
                    if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }

            } else {
                int status = mMediaCodec.dequeueOutputBuffer(mBufferInfo, timeOut);
                if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    return rest;
                } else if (status >= 0) {
                    // encoded sample
                    ByteBuffer data = mMediaCodec.getOutputBuffer(status);
                    if (data != null) {
                        final int endOfStream = mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                        // pass to whoever listens to
                        if (endOfStream == 0) {
                            rest = new byte[mBufferInfo.size];
                            data.position(mBufferInfo.offset);
                            data.limit(mBufferInfo.offset + mBufferInfo.size);
                            data.get(rest, 0, mBufferInfo.size);
                        }
                        // releasing buffer is important
                        mMediaCodec.releaseOutputBuffer(status, false);
                        if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }
            }


        } catch (Throwable t) {
            t.printStackTrace();
        }
        return rest;
    }

    /**
     * 初始化编码器
     * */
    public void stopAndUninit() {
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 颜色空间转换
     * @param nv21 in 输入
     * @param nv12 out 输出
     * @param height 高度
     * @param width 宽度
     * */
    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

}
