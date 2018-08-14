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
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;

import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * The renderer used for resources.
 */
public class ResourceRenderer extends Renderer {
    // position
    private FloatBuffer mVtxBuf;
    private FloatBuffer mTexCoordinateBuf;
    // matrix
    private float[] mPosMtx = GLUtil.createIdentityMtx();
    private float[] mMMtx = GLUtil.createIdentityMtx();
    private float[] mVMtx = GLUtil.createIdentityMtx();
    private float[] mPMtx = GLUtil.createIdentityMtx();
    private int mProgram = -1;
    private int mPositionHandle = -1;
    private int mTexCoordinateHandle = -1;
    private int mPosMtxHandle = -1;
    private int mResourceSamplerHandle = -1;
    private int mTexRotateMtxHandle = -1;
    private RectF mResourceRect;
    private int mResourceId    = -1;
    private int mResourceTexId = -12345;

    final String mVertexShader =
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTexCoord;\n" +
            "uniform   mat4 uPosMtx;\n" +
            "uniform   mat4 uTexRotateMtx;\n" +
            "varying   vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uPosMtx * aPosition;\n" +
            "  vTexCoord   = (uTexRotateMtx * aTexCoord).xy;\n" +
            "}\n";
    final String mFragmentShader =
            "precision mediump float;\n" +
            "uniform sampler2D uResourceSampler;\n" +
            "varying vec2               vTexCoord;\n" +
            "void main() {\n" +
            "        gl_FragColor = texture2D(uResourceSampler, vTexCoord);\n" +
            "}\n";

    /**
     * Construct a resource renderer.
     * @param activity the camera activity.
     */
    public ResourceRenderer(Activity activity) {
        super(activity);
        mTexCoordinateBuf = createFloatBuffer(mTexCoordinateBuf, GLUtil.createTexCoordinate());
        mResourceRect = new RectF();
    }

    /**
     * Init the resource renderer.
     */
    public void init() {
        initProgram();
    }

    /**
     * Update template resource id.
     *
     * @param resourceId the source id.
     */
    public void updateTemplate(int resourceId) {
        if (resourceId == mResourceId) {
            return;
        }
        releaseResource();
        try {
            mResourceTexId = initBitmapTexture(resourceId, false);
        } catch (IOException e) {
        }
    }

    /**
     * release resource.
     */
    @Override
    public void release() {
        releaseResource();
    }

    /**
     * Get resource rect.
     * @return the resource rect.
     */
    public RectF getResourceRect() {
        return mResourceRect;
    }

    @Override
    public void setRendererSize(int width, int height) {
        if (width == getRendererWidth() && height == getRendererHeight()) {
            return;
        }
        resetMatrix();
        super.setRendererSize(width, height);
        Matrix.orthoM(mPMtx, 0, 0, getRendererWidth(), 0, getRendererHeight(), -1, 1);
        Matrix.translateM(mMMtx, 0, 0, getRendererHeight(), 0);
        Matrix.scaleM(mMMtx, 0, mMMtx, 0, 1, -1, 1);
        Matrix.multiplyMM(mPosMtx, 0, mMMtx, 0, mVMtx, 0);
        Matrix.multiplyMM(mPosMtx, 0, mPMtx, 0, mPosMtx, 0);
    }

    /**
     * Draw source with opengl command.
     * @param rCenterX rect's center x position.
     * @param rCenterY rect's center y position.
     * @param edge rect's edge.
     * @param vtxBuf vertex buffer.
     * @param texRotateMtx rotate matrix.
     */
    public void draw(float rCenterX, float rCenterY, float edge,
            FloatBuffer vtxBuf, float[] texRotateMtx) {
        if (getRendererWidth() <= 0 || getRendererHeight() <= 0) {
            return;
        }
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLUtil.checkGlError("ResourceRenderer draw start");
        // use program
        GLES20.glUseProgram(mProgram);
        // position
        if (vtxBuf == null) {
            initVertexData(rCenterX, rCenterY, edge);
            mVtxBuf.position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3,
                    mVtxBuf);
        } else {
            vtxBuf.position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, 3,
                    GLES20.GL_FLOAT, false, 4 * 3, vtxBuf);
        }

        mTexCoordinateBuf.position(0);
        GLES20.glVertexAttribPointer(mTexCoordinateHandle, 2, GLES20.GL_FLOAT, false, 4 * 2,
                mTexCoordinateBuf);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordinateHandle);
        // matrix
        GLES20.glUniformMatrix4fv(mPosMtxHandle, 1, false, mPosMtx, 0);
        GLES20.glUniformMatrix4fv(mTexRotateMtxHandle, 1, false,
                texRotateMtx == null ? GLUtil.createIdentityMtx() : texRotateMtx, 0);
        // sampler
        GLES20.glUniform1i(mResourceSamplerHandle, 0);
        // texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mResourceTexId);
        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 6);
        GLUtil.checkGlError("ResourceRendrer draw end");
    }

    private void initVertexData(float rCenterX, float rCenterY, float edge) {
        mVtxBuf = createFloatBuffer(mVtxBuf,
                GLUtil.createSquareVtxByCenterEdge(rCenterX, rCenterY, edge));
        mResourceRect.set(rCenterX - edge / 2, rCenterY - edge / 2, rCenterX + edge / 2, rCenterY
                + edge / 2);
    }

    private void initProgram() {
        mProgram = createProgram(mVertexShader, mFragmentShader);
        // position
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
        // matrix
        mPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        mTexRotateMtxHandle = GLES20.glGetUniformLocation(mProgram, "uTexRotateMtx");
        // sampler
        mResourceSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uResourceSampler");
    }

    private void resetMatrix() {
        mPosMtx = GLUtil.createIdentityMtx();
        mPMtx = GLUtil.createIdentityMtx();
        mVMtx = GLUtil.createIdentityMtx();
        mMMtx = GLUtil.createIdentityMtx();
    }

    private void releaseResource() {
        if (mResourceTexId > 0) {
            releaseBitmapTexture(mResourceTexId);
            mResourceTexId = -12345;
        }
    }
}