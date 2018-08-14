/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
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
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
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

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.gles.egl.EglCore;
import com.mediatek.camera.feature.mode.pip.pipwrapping.PipOperator.PIPCustomization;

import java.nio.FloatBuffer;

/**
 * Render preview buffer to preview surface.
 */
public class ScreenRenderer extends Renderer implements Runnable {
    private static final Tag TAG = new Tag(ScreenRenderer.class.getSimpleName());
    private FloatBuffer mVtxBuf; // vertex coordinates
    private FloatBuffer mTexCoordinateBuf; // texture coordinates
    private FloatBuffer mTopGraphicPositionBuf = null;
    private int mRenderTexWidth = -1;
    private int mRenderTexHeight = -1;
    private int mTextureRotation = 0;

    private float[] mPosMtx = GLUtil.createIdentityMtx();
    private float[] mPMtx = GLUtil.createIdentityMtx(); // project
    private float[] mVMtx = GLUtil.createIdentityMtx(); // view
    private float[] mMMtx = GLUtil.createIdentityMtx(); // mode
    private float[] mTexRotateMtx = GLUtil.createIdentityMtx(); // rotate texture

    private ResourceRenderer mEditTexRenderer;
    private ResourceRenderer mPressedTexRenderer; //highlight press effects

    // A surface is used to receive preview buffer, will create EGLSurface by it
    private Surface mPreviewSurface = null;
    private WindowSurface mPreviewEGLSurface;
    private EglCore mEglCore;
    private int mEditTexSize = 0;
    private int mProgram = -1; // program id
    private int mPositionHandle = -1; // vertex position handle
    private int mTexRotateMtxHandle = -1;
    private int mTexCoordinateHandle = -1; // texture position handle
    private int mPosMtxHandle = -1;
    private int mSamplerHandle = -1; // sampler handle

    private ScreenHandler mScreenHandler;
    private Object mReadyFence = new Object(); // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private EGLContext mSharedEGLContext = null;
    private boolean mIsEGLSurfaceReady = false;
    private ConditionVariable mUpdateEGLSurfaceSync = new ConditionVariable();
    private ConditionVariable mReleaseScreenSurfaceSync = new ConditionVariable();
    private ConditionVariable mDrawLockableConditionVariable = new ConditionVariable();

    /**
     * Screen renderer.This can be called from non-gl thread.
     *
     * @param activity the activity.
     */
    public ScreenRenderer(Activity activity) {
        super(activity);
        mEditTexRenderer = new ResourceRenderer(activity);
        mPressedTexRenderer = new ResourceRenderer(activity);
        mTexCoordinateBuf = createFloatBuffer(mTexCoordinateBuf, GLUtil.createTexCoordinate());
        // new a thread to share EGLContext with pip wrapping GL Thread
        new Thread(this, "PIP-ScreenRenderer").start();
    }

    /**
     * Initialize screen renderer program and shader,
     * get shared egl context and init sub render.
     * <p>
     * Note: this should be called in GL Thread
     */
    public void init() {
        LogHelper.d(TAG, "init: " + this);
        // initialize program and shader
        initGL();
        // get shared egl context
        mSharedEGLContext = EGL14.eglGetCurrentContext();
        // initialize edit texture renderer
        mEditTexRenderer.init();
        mPressedTexRenderer.init();
    }

    /**
     * Update screen effect template.
     * @param highLightId high light id.
     * @param editButtonId edit button id.
     */
    public void updateScreenEffectTemplate(int highLightId, int editButtonId) {
        if (highLightId > 0) {
            mPressedTexRenderer.updateTemplate(highLightId);
        }
        if (editButtonId > 0) {
            mEditTexRenderer.updateTemplate(editButtonId);
        }
    }

    @Override
    public void setRendererSize(int width, int height) {
        LogHelper.d(TAG, "setRendererSize width = " + width + " height = " + height);
        if (!isMatchingSurfaceSize(width, height)) {
            mIsEGLSurfaceReady = false;
        }
        super.setRendererSize(width, height);
    }

    @Override
    protected void setSurface(Surface surface, boolean scaled, boolean rotated) {
        LogHelper.d(TAG, "setSurface scaled = " + scaled + " rotated = " + rotated +
                ", mPreviewSurface = " + mPreviewSurface);
        if (skipUpdateSurface(surface)) {
            LogHelper.i(TAG, "the same surface skip update mPreviewSurface = " + mPreviewSurface);
            mIsEGLSurfaceReady = true;
            return;
        }
        mIsEGLSurfaceReady = false;
        super.setSurface(surface, scaled, rotated);
        if (surface == null) {
            throw new RuntimeException("ScreenRenderer setSurface to null!!!!!");
        }
        mPreviewSurface = surface;
        waitRendererThreadActive();
        updateEGLSurface();
        mRenderTexWidth = mPreviewEGLSurface.getWidth();
        mRenderTexHeight = mPreviewEGLSurface.getHeight();
        mTextureRotation = getDisplayRotation(getActivity());
        updateRendererSize(Math.min(mRenderTexWidth, mRenderTexHeight),
                Math.max(mRenderTexWidth, mRenderTexHeight));
        mIsEGLSurfaceReady = true;
    }

    @Override
    public void release() {
        LogHelper.d(TAG, "release: " + this);
        synchronized (mReadyFence) {
            if (mScreenHandler != null) {
                mScreenHandler.removeCallbacksAndMessages(null);
                mReleaseScreenSurfaceSync.close();
                mScreenHandler.obtainMessage(ScreenHandler.MSG_RELEASE_SURFACE).sendToTarget();
                mReleaseScreenSurfaceSync.block(2000);
            }
        }
        super.setRendererSize(-1, -1);
    }

    /**
     * draw screen frame.
     * @param topGraphicRect top graphic animation rect.
     * @param texId texture id.
     * @param highlight whether need high light.
     */
    public void draw(AnimationRect topGraphicRect, int texId, boolean highlight) {
        synchronized (mReadyFence) {
            if (mScreenHandler != null && mIsEGLSurfaceReady) {
                mDrawLockableConditionVariable.close();
                mScreenHandler.removeMessages(ScreenHandler.MSG_FRAME_AVAILABLE);
                mScreenHandler.obtainMessage(ScreenHandler.MSG_FRAME_AVAILABLE, texId,
                        highlight ? 1 : 0, topGraphicRect).sendToTarget();
                mDrawLockableConditionVariable.block(100);
            }
        }
    }

    /**
     * Notify surface's status.
     * @param surface the surface.
     */
    public void notifySurfaceDestroyed(Surface surface) {
        LogHelper.d(TAG, "[notifySurfaceDestroyed] surface:" + surface);
        synchronized (mReadyFence) {
            if (mScreenHandler != null && mPreviewEGLSurface != null
                    && surface == mPreviewEGLSurface.getSurface()) {
                mScreenHandler.removeCallbacksAndMessages(null);
                mReleaseScreenSurfaceSync.close();
                mScreenHandler.obtainMessage(ScreenHandler.MSG_SURFACE_DESTROYED).sendToTarget();
                mReleaseScreenSurfaceSync.block(2000);
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            LogHelper.d(TAG, "Screen renderer thread started!");
            mScreenHandler = new ScreenHandler();
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();
        LogHelper.d(TAG, "Screen renderer thread exiting!");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mScreenHandler = null;
        }
    }

    private boolean isMatchingSurfaceSize(int width, int height) {
        LogHelper.d(TAG, "isMatchingSurfaceSize mRenderTexWidth:" + mRenderTexWidth
                + " mRenderTexHeight:" + mRenderTexHeight);
        int max = Math.max(width, height);
        int min = Math.min(width, height);
        int surfaceMax = Math.max(mRenderTexWidth, mRenderTexHeight);
        int surfaceMin = Math.min(mRenderTexWidth, mRenderTexHeight);
        return Math.abs(((float) max / min) - ((float) surfaceMax / surfaceMin)) <= 0.02;
    }

    private boolean skipUpdateSurface(Surface surface) {
        int disPlayRotation = getDisplayRotation(getActivity());
        boolean rotationIsLandscape = (disPlayRotation == 90 || disPlayRotation == 270);
        boolean renderSizeIsLandscape = mRenderTexWidth > mRenderTexHeight;
        boolean skipRotation = (renderSizeIsLandscape == rotationIsLandscape);
        boolean skipSurface = surface.equals(mPreviewSurface);
        boolean skipMinimalSize = (mRenderTexWidth > 2) && (mRenderTexHeight > 2);
        boolean skipSurfaceSize = (mRenderTexWidth == getRendererWidth()
                && mRenderTexHeight == getRendererHeight());
        boolean skipUnValidSurface = !(surface.isValid());
        return (skipSurface && skipMinimalSize && skipRotation && skipSurfaceSize)
                || skipUnValidSurface;
    }

    private void waitRendererThreadActive() {
        synchronized (mReadyFence) {
            if (mRunning) {
                LogHelper.i(TAG, "screen renderer already running!");
                return;
            }
            mRunning = true;
            while (!mReady) {
                try {
                    LogHelper.d(TAG,
                            "wait for screen renderer thread ready, current mReady = " + mReady);
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            // when screen renderer thread started, be sure not block first frame
            mDrawLockableConditionVariable.open();
        }
    }

    private void updateRendererSize(int width, int height) {
        LogHelper.d(TAG, "updateRendererSize width = " + width + " height = " + height);
        resetMatrix();
        Matrix.orthoM(mPMtx, 0, 0, width, 0, height, -1, 1);
        initVertexData(width, height);
        mEditTexSize = Math.min(width, height)
                / PIPCustomization.TOP_GRAPHIC_EDIT_BUTTON_RELATIVE_VALUE;
        mEditTexRenderer.setRendererSize(width, height);
        mPressedTexRenderer.setRendererSize(width, height);
    }

    private int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * Screen renderer handler.
     */
    private class ScreenHandler extends Handler {
        public static final int MSG_SETUP_SURFACE = 0;
        public static final int MSG_RELEASE_SURFACE = 1;
        public static final int MSG_UPDATE_EGL_SURFACE = 2;
        public static final int MSG_FRAME_AVAILABLE = 3;
        public static final int MSG_SURFACE_DESTROYED = 4;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SETUP_SURFACE:
                doSetupEGLSurface();
                break;
            case MSG_RELEASE_SURFACE:
                doReleaseSurface();
                mReleaseScreenSurfaceSync.open();
                break;
            case MSG_SURFACE_DESTROYED:
                releaseEglSurface();
                mReleaseScreenSurfaceSync.open();
                break;
            case MSG_UPDATE_EGL_SURFACE:
                doUpdateEGLSurface();
                mUpdateEGLSurfaceSync.open();
                break;
            case MSG_FRAME_AVAILABLE:
                boolean highlight = msg.arg2 > 0;
                try {
                    doDraw((AnimationRect) msg.obj, msg.arg1, highlight);
                } catch (IllegalStateException e) {
                    LogHelper.e(TAG, "gl error, ignore this doDraw pass");
                }
                break;
            default:
                break;
            }
        }

        private void doSetupEGLSurface() {
                LogHelper.d(TAG, "handleSetupSurface  mEglCore = " + mEglCore +
                        " mPreviewEGLSurface = " + mPreviewEGLSurface
                        + " mPreviewSurface = " + mPreviewSurface);
                if (mEglCore == null) {
                    mEglCore = new EglCore(mSharedEGLContext,
                        EglCore.CONSTRUCTOR_FLAG_TRY_GLES3 | EglCore.CONSTRUCTOR_FLAG_RECORDABLE,
                        new int[]{PixelFormat.RGB_888, PixelFormat.RGBA_8888, ImageFormat.YV12});
                }
                if (mPreviewEGLSurface == null) {
                    mPreviewEGLSurface = new WindowSurface(mEglCore, mPreviewSurface);
                    mPreviewEGLSurface.makeCurrent();
                }
            }

        private void doUpdateEGLSurface() {
                LogHelper.d(TAG, "updateEGLSurface mPreviewEGLSurface = " + mPreviewEGLSurface);
                if (mPreviewEGLSurface != null) {
                    // release old egl surface
                    mPreviewEGLSurface.makeNothingCurrent();
                    mPreviewEGLSurface.releaseEglSurface();
                    // create new egl surface
                    mPreviewEGLSurface = new WindowSurface(mEglCore, mPreviewSurface);
                    mPreviewEGLSurface.makeCurrent();
                } else {
                    doSetupEGLSurface();
                }
            }

        private void doReleaseSurface() {
                releaseEglSurface();
                if (mEglCore != null) {
                    mEglCore.release();
                    mEglCore = null;
                }
                mEditTexRenderer.release();
                mPressedTexRenderer.release();
                mIsEGLSurfaceReady = false;
                mPreviewSurface = null;
                mProgram = -1;
                Looper looper = Looper.myLooper();
                if (looper != null) {
                    looper.quit();
                }
            }

        private void releaseEglSurface() {
                LogHelper.d(TAG, "releaseEglSurface");
                if (mPreviewEGLSurface != null) {
                    mPreviewEGLSurface.makeNothingCurrent();
                    mPreviewEGLSurface.releaseEglSurface();
                    mPreviewEGLSurface = null;
                }
            }

        private void doDraw(AnimationRect topGraphicRect, int texId, boolean highlight) {
                if (getRendererWidth() <= 0 || getRendererHeight() <= 0 ||
                        mPreviewEGLSurface == null) {
                    return;
                }
                GLUtil.checkGlError("ScreenDraw_Start");
                GLES20.glViewport(0, 0, mRenderTexWidth, mRenderTexHeight);
                GLES20.glClearColor(0f, 0f, 0f, 1f);
                GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                //use program
                GLES20.glUseProgram(mProgram);
                //vertex
                mVtxBuf.position(0);
                GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3,
                        mVtxBuf);
                mTexCoordinateBuf.position(0);
                GLES20.glVertexAttribPointer(mTexCoordinateHandle, 2, GLES20.GL_FLOAT, false, 4 * 2,
                        mTexCoordinateBuf);
                GLES20.glEnableVertexAttribArray(mPositionHandle);
                GLES20.glEnableVertexAttribArray(mTexCoordinateHandle);
                //matrix
                GLES20.glUniformMatrix4fv(mPosMtxHandle, 1, false, mPosMtx, 0);
                GLES20.glUniformMatrix4fv(mTexRotateMtxHandle, 1, false, mTexRotateMtx, 0);
                //sampler
                GLES20.glUniform1i(mSamplerHandle, 0);
                //texture
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
                // draw
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 6);
                // draw edit texture
                if (topGraphicRect != null) {
                    topGraphicRect.changeCoordinateSystem(mRenderTexWidth, mRenderTexHeight,
                            mTextureRotation);
                    mEditTexRenderer.draw(topGraphicRect.getRightBottom()[0],
                            topGraphicRect.getRightBottom()[1], mEditTexSize, null, null);
                }
                // draw highlight texture
                if (topGraphicRect != null && highlight) {
                    mTopGraphicPositionBuf = createFloatBuffer(mTopGraphicPositionBuf,
                            GLUtil.createTopRightRect(topGraphicRect));
                    mTopGraphicPositionBuf.position(0);
                    mPressedTexRenderer.draw(0, 0, 0, mTopGraphicPositionBuf, null);
                }
                //swap buffer
                mPreviewEGLSurface.swapBuffers();
                mDrawLockableConditionVariable.open();
                debugFrameRate(TAG);
    //            mPreviewEGLSurface.saveFrame(new File("/storage/sdcard0/piptest.jpg"));
                GLUtil.checkGlError("ScreenDraw_End");
            }
    }

    private void initGL() {
        if (mProgram != -1) {
            return;
        }
        GLUtil.checkGlError("initGL_Start");
        final String vertexShader =
                "attribute vec4 aPosition;\n"
              + "attribute vec4 aTexCoord;\n"
              + "uniform   mat4 uPosMtx;\n"
              + "uniform   mat4 uTexRotateMtx;\n"
              + "varying   vec2 vTexCoord;\n"
              + "void main() {\n"
              + "  gl_Position = uPosMtx * aPosition;\n"
              + "  vTexCoord   = (uTexRotateMtx * aTexCoord).xy;\n"
              + "}\n";
        final String fragmentShader =
                "precision mediump float;\n"
              + "uniform sampler2D uSampler;\n"
              + "varying vec2      vTexCoord;\n"
              + "void main() {\n"
              + "  gl_FragColor = texture2D(uSampler, vTexCoord);\n"
              + "}\n";
        mProgram         = GLUtil.createProgram(vertexShader, fragmentShader);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
        //matrix
        mPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        mTexRotateMtxHandle = GLES20.glGetUniformLocation(mProgram, "uTexRotateMtx");
        //sampler
        mSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLUtil.checkGlError("initGL_E");
    }

    private void resetMatrix() {
        mPosMtx = GLUtil.createIdentityMtx();
        mPMtx = GLUtil.createIdentityMtx();
        mVMtx = GLUtil.createIdentityMtx();
        mMMtx = GLUtil.createIdentityMtx();
        mTexRotateMtx = GLUtil.createIdentityMtx();
    }

    private void updateEGLSurface() {
        synchronized (mReadyFence) {
            mUpdateEGLSurfaceSync.close();
            if (mScreenHandler != null) {
                mScreenHandler.removeMessages(ScreenHandler.MSG_FRAME_AVAILABLE);
                mScreenHandler.obtainMessage(ScreenHandler.MSG_UPDATE_EGL_SURFACE).sendToTarget();
            }
            mUpdateEGLSurfaceSync.block();
        }
    }

    private void initVertexData(float width, float height) {
        android.opengl.Matrix.translateM(mTexRotateMtx, 0,
                mTexRotateMtx, 0, .5f, .5f, 0);
        android.opengl.Matrix.rotateM(mTexRotateMtx, 0,
                -mTextureRotation, 0, 0, 1);
        android.opengl.Matrix.translateM(mTexRotateMtx, 0, -.5f, -.5f, 0);
        mVtxBuf = createFloatBuffer(mVtxBuf, GLUtil.createFullSquareVtx(width, height));
        Matrix.multiplyMM(mPosMtx, 0, mMMtx, 0, mVMtx, 0);
        Matrix.multiplyMM(mPosMtx, 0, mPMtx, 0, mPosMtx, 0);
    }
}