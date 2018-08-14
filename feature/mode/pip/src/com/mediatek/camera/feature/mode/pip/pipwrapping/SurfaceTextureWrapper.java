/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensor. Without
 * the prior written permission of MediaTek inc. and/or its licensor, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.feature.mode.pip.pipwrapping;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;

import junit.framework.Assert;

import javax.annotation.Nonnull;

/**
 * A wrapper for surface texture.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SurfaceTextureWrapper implements OnFrameAvailableListener {
    private static final Tag TAG = new Tag(SurfaceTextureWrapper.class.getSimpleName());
    private SurfaceTexture mSurfaceTexture    = null;
    private int mWidth = -1;
    private int mHeight = -1;
    private int mTextureId = -12345;
    private float[] mSTTransformMatrix = new float[16];
    private long mSTTimeStamp = 0L;
    private HandlerThread mHandlerThread;
    private Handler mProcessHandler;
    private String mIdentifyTag;

    /**
     * Construct surface texture wrapper.
     * @param identifyTag identify tag.
     * @param surfaceTexId surface texture id.
     * @param processHandler process handler.
     */
    public SurfaceTextureWrapper(@Nonnull String identifyTag,
                                 @Nonnull int surfaceTexId,
                                 @Nonnull Handler processHandler) {
        Assert.assertTrue(surfaceTexId >= 0);
        mTextureId = surfaceTexId;
        mProcessHandler = processHandler;
        mIdentifyTag = identifyTag;
        mSurfaceTexture = new SurfaceTexture(surfaceTexId);
        mHandlerThread = new HandlerThread("Pip-" + identifyTag);
        mHandlerThread.start();
        mSurfaceTexture.setOnFrameAvailableListener(this, new Handler(mHandlerThread.getLooper()));
    }

    /**
     * Set default buffer size.
     * @param width the buffer's width.
     * @param height the buffer's height.
     */
    public void setDefaultBufferSize(int width, int height) {
        if (mWidth == width && mHeight == height && mSurfaceTexture != null) {
            LogHelper.i(TAG, "[setDefaultBufferSize] skip, w:" + width + ", h:" + height);
            return;
        }
        LogHelper.i(TAG, "[setDefaultBufferSize] w:" + width + ",h:" + height);
        mWidth = width;
        mHeight = height;
        mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
    }

    /**
     * Get the width of the buffer.
     * @return the width of the buffer.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Get the height of the buffer.
     * @return the height of the buffer.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Get the texture id.
     * @return the texture id.
     */
    public int getTextureId() {
        return mTextureId;
    }

    /**
     * Get surface texture.
     * @return the surface texture.
     */
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    /**
     * Get the buffer's transform matrix.
     * @return the buffer's transform matrix.
     */
    public float[] getBufferTransformMatrix() {
        return mSTTransformMatrix;
    }

    /**
     * Get buffer's time stamp.
     * @return the buffer's time stamp.
     */
    public long getTimeStamp() {
        return mSTTimeStamp;
    }

    /**
     * Update surface texture image, Note: this method must be called in GL Thread.
     */
    public void updateTexImage() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
            mSTTimeStamp = mSurfaceTexture.getTimestamp();
            mSurfaceTexture.getTransformMatrix(mSTTransformMatrix);
        }
    }

    /**
     * Reset surface texture status.
     */
    public void resetSTStatus() {
        mSTTimeStamp       = 0L;
        mSTTransformMatrix = new float[16];
    }

    /**
     * Release surface texture wrapper.
     */
    public void release() {
        LogHelper.d(TAG, "[release]");
        resetSTStatus();
        mWidth = 0;
        mHeight = 0;
        if (mTextureId >= 0) {
            GLUtil.deleteTextures(new int[]{mTextureId});
            mTextureId = -12345;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.setOnFrameAvailableListener(null);
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mHandlerThread.isAlive()) {
            mHandlerThread.quitSafely();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if ("Bottom".equals(mIdentifyTag)) {
            mProcessHandler.removeMessages(
                    RendererManager.PipRendererHandler.MSG_NEW_BOTTOM_FRAME_ARRIVED);
            mProcessHandler.obtainMessage(
                    RendererManager.PipRendererHandler.MSG_NEW_BOTTOM_FRAME_ARRIVED,
                    this).sendToTarget();
        } else {
            mProcessHandler.removeMessages(
                    RendererManager.PipRendererHandler.MSG_NEW_TOP_FRAME_ARRIVED);
            mProcessHandler.obtainMessage(
                    RendererManager.PipRendererHandler.MSG_NEW_TOP_FRAME_ARRIVED,
                    this).sendToTarget();
        }
    }
}