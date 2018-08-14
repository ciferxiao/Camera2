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
 * MediaTek Inc. (C) 2016. All rights reserved.
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

import android.graphics.Matrix;
import android.graphics.RectF;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.mode.pip.pipwrapping.PipOperator.PIPCustomization;

/**
 * Animation rect used to describe pip top graphic's position.
 */
public class AnimationRect {
    private static final float MAX_SCALE_VALUE = PIPCustomization.TOP_GRAPHIC_MAX_SCALE_VALUE;
    private static final float MIN_SCALE_VALUE = PIPCustomization.TOP_GRAPHIC_MIN_SCALE_VALUE;
    private static float sRotationLimitedMax = PIPCustomization.TOP_GRAPHIC_MAX_ROTATE_VALUE;
    private static float sRotationLimitedMin = -PIPCustomization.TOP_GRAPHIC_MAX_ROTATE_VALUE;
    private float mCurrentScaleValue = 1.0f;
    private Matrix mAnimationMatrix;
    private float mOriginalDistance = 0f;
    private RectF mRectF;
    private int mPreviewWidth = -1;
    private int mPreviewHeight = -1;
    private float mCurrentRotationValue = 0f;
    private float[] mLeftTop = new float[] { 0f, 0f };
    private float[] mRightTop = new float[] { 0f, 0f };
    private float[] mLeftBottom = new float[] { 0f, 0f };
    private float[] mRightBottom = new float[] { 0f, 0f };
    private boolean mIsHighlightEnable = false;

    /**
     * Get original distance.
     * @return the original distance.
     */
    public float getOriginalDistance() {
        return mOriginalDistance;
    }

    /**
     * Set original distance.
     * @param originalDistance the original distance.
     */
    public void setOriginalDistance(float originalDistance) {
        mOriginalDistance = originalDistance;
    }

    /**
     * Get current scale value.
     * @return the current scale value.
     */
    public float getCurrentScaleValue() {
        return mCurrentScaleValue;
    }

    /**
     * Set current scale value.
     * @param currentScaleValue the current scale value.
     */
    public void setCurrentScaleValue(float currentScaleValue) {
        mCurrentScaleValue = currentScaleValue;
    }

    /**
     * Get left top value.
     * @return the left top value.
     */
    public float[] getLeftTop() {
        return mLeftTop;
    }

    /**
     * Set left top value.
     * @param mLeftTop the left top value.
     */
    public void setLeftTop(float[] mLeftTop) {
        this.mLeftTop[0] = mLeftTop[0];
        this.mLeftTop[1] = mLeftTop[1];
    }

    /**
     * Get right top value.
     * @return the right top value.
     */
    public float[] getRightTop() {
        return mRightTop;
    }

    /**
     * Set right top value.
     * @param mRightTop the right top value.
     */
    public void setRightTop(float[] mRightTop) {
        this.mRightTop[0] = mRightTop[0];
        this.mRightTop[1] = mRightTop[1];
    }

    /**
     * Get left bottom value.
     * @return the left bottom value.
     */
    public float[] getLeftBottom() {
        return mLeftBottom;
    }

    /**
     * Set left bottom value.
     * @param mLeftBottom the left bottom value.
     */
    public void setLeftBottom(float[] mLeftBottom) {
        this.mLeftBottom[0] = mLeftBottom[0];
        this.mLeftBottom[1] = mLeftBottom[1];
    }

    /**
     * Get right bottom value.
     * @return the right bottom value.
     */
    public float[] getRightBottom() {
        return mRightBottom;
    }

    /**
     * Set right bottom value.
     * @param mRightBottom the right bottom value.
     */
    public void setRightBottom(float[] mRightBottom) {
        this.mRightBottom[0] = mRightBottom[0];
        this.mRightBottom[1] = mRightBottom[1];
    }

    /**
     * Construct a AnimationRect instance.
     */
    public AnimationRect() {
        mAnimationMatrix = new Matrix();
        mRectF = new RectF();
    }

    /**
     * Get preview width.
     * @return the preview width.
     */
    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    /**
     * Get preview height.
     * @return the preview height.
     */
    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    /**
     * Set Renderer size.
     * @param width renderer size width.
     * @param height renderer sie height.
     */
    public void setRendererSize(int width, int height) {
        // reduce edge / 2
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    /**
     * Initialize with let top right bottom value.
     * @param mLeft left value.
     * @param mTop top value.
     * @param mRight right value.
     * @param mBottom bottom value.
     */
    public void initialize(float mLeft, float mTop, float mRight, float mBottom) {
        mRectF.set(mLeft, mTop, mRight, mBottom);
        setVertex(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom);
        mOriginalDistance = (float) Math.sqrt((centerX() - mRightBottom[0])
                * (centerX() - mRightBottom[0]) + (centerY() - mRightBottom[1])
                * (centerY() - mRightBottom[1]));
    }

    /**
     * Adjust scale distance.
     * @param newDistance new distance.
     * @return the distance.
     */
    public float adjustScaleDistance(float newDistance) {
        if (newDistance < 3 * mOriginalDistance / 4) {
            return 3 * mOriginalDistance / 4;
        } else if (newDistance > mOriginalDistance * 4 / 3) {
            return mOriginalDistance * 4 / 3;
        }
        return newDistance;
    }

    /**
     * Translate by dx and dy.
     * @param dx x distance.
     * @param dy y distance.
     * @param checkTranslate whether need to check edge.
     */
    public void translate(float dx, float dy, boolean checkTranslate) {
        mAnimationMatrix.reset();
        mAnimationMatrix.setTranslate(dx, dy);
        mAnimationMatrix.mapRect(mRectF);
        if (checkTranslate) {
            checkTranslate();
        }
        setVertex(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom);
    }

    /**
     * Get RectF.
     * @return the RectF.
     */
    public RectF getRectF() {
        return mRectF;
    }

    /**
     * Get current rotation value.
     * @return the rotation value.
     */
    public float getCurrentRotationValue() {
        return mCurrentRotationValue;
    }

    /**
     * Set high light enable.
     * @param highlight whether enable high light.
     */
    public void setHighLightEnable(boolean highlight) {
        mIsHighlightEnable = highlight;
    }

    /**
     * Get high light status.
     * @return the high light status.
     */
    public boolean getHighLightStatus() {
        return mIsHighlightEnable;
    }

    /**
     * Get the center x.
     * @return the center x.
     */
    public float centerX() {
        return (mRightTop[0] + mLeftBottom[0]) / 2;
    }

    /**
     * Get the center y.
     * @return the center y.
     */
    public float centerY() {
        return (mRightTop[1] + mLeftBottom[1]) / 2;
    }

    /**
     * Scale the AnimationRect.
     * @param scale the scale value.
     * @param checkScale whether check the edge.
     */
    public void scale(float scale, boolean checkScale) {
        if (checkScale) {
            float scaleValue = mCurrentScaleValue * scale;
            // check max scale value
            if (scale > 1) {
                scale = scaleValue > getMaxScaleValue() ? 1f : scale;
            }
            // check minimal scale value
            if (scale < 1) {
                scale = scaleValue < getMinScaleValue() ? 1f : scale;
            }
            mCurrentScaleValue = mCurrentScaleValue * scale;
        }
        mAnimationMatrix.reset();
        mAnimationMatrix.setScale(scale, scale, mRectF.centerX(), mRectF.centerY());
        mAnimationMatrix.mapRect(mRectF);
        setVertex(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom);
        mOriginalDistance = (float) Math.sqrt((centerX() - mRightBottom[0])
                * (centerX() - mRightBottom[0]) + (centerY() - mRightBottom[1])
                * (centerY() - mRightBottom[1]));
    }

    /**
     * Scale to translate Y.
     * @param scaleY the y scale value.
     */
    public void scaleToTranslateY(float scaleY) {
        float[] rt = new float[] { mRightTop[0], mRightTop[1] };
        mAnimationMatrix.reset();
        mAnimationMatrix.setScale(1, scaleY, mRectF.centerX(), mRectF.centerY());
        mAnimationMatrix.mapPoints(rt);
        translate(0, rt[1] - mRightTop[1], false);
    }

    /**
     * Rotate the degrees.
     * @param degrees the degrees need to rotate.
     */
    public void rotate(float degrees) {
        rotate(degrees, mRectF.centerX(), mRectF.centerY());
    }

    /**
     * Rotate by degrees in the point of (centerX, centerY).
     * @param degrees the rotate degree.
     * @param centerX the center x.
     * @param centerY the center y.
     */
    public void rotate(float degrees, float centerX, float centerY) {
        setVertex(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom);
        mAnimationMatrix.reset();
        mAnimationMatrix.setRotate(degrees, centerX, centerY);
        mAnimationMatrix.mapPoints(mLeftTop);
        mAnimationMatrix.mapPoints(mRightTop);
        mAnimationMatrix.mapPoints(mLeftBottom);
        mAnimationMatrix.mapPoints(mRightBottom);
        mCurrentRotationValue = degrees;
    }

    /**
     * Copy an instance of AnimationRect.
     * @return an copy of AnimationRect.
     */
    public AnimationRect copy() {
        AnimationRect resultRect = new AnimationRect();
        resultRect.mCurrentScaleValue = this.mCurrentScaleValue;
        resultRect.mAnimationMatrix.set(this.mAnimationMatrix);
        resultRect.mOriginalDistance = this.mOriginalDistance;
        resultRect.mRectF.set(this.mRectF);
        resultRect.mPreviewWidth = this.mPreviewWidth;
        resultRect.mPreviewHeight = this.mPreviewHeight;
        resultRect.mCurrentRotationValue = this.mCurrentRotationValue;
        resultRect.setLeftTop(this.getLeftTop());
        resultRect.setRightTop(this.getRightTop());
        resultRect.setLeftBottom(this.getLeftBottom());
        resultRect.setRightBottom(this.getRightBottom());
        resultRect.setHighLightEnable(this.mIsHighlightEnable);
        return resultRect;
    }

    /**
     * Change to land scape coordinate system.
     * @param width the width.
     * @param height the height.
     * @param rotation the rotation.
     */
    public void changeToLandscapeCoordinateSystem(int width, int height, int rotation) {
        int portraitWidth = Math.min(width, height);
        int portraitHeight = Math.max(width, height);
        changePortraitCoordinateSystem(portraitWidth, portraitHeight);

        float centerX = centerX();
        float centerY = centerY();
        float newCenterX = 0;
        float newCenterY = 0;

        switch (rotation) {
        case 90:
            newCenterX = centerY;
            newCenterY = portraitWidth - centerX;
            break;
        case 270:
            newCenterX = portraitHeight - centerY;
            newCenterY = centerX;
            break;
        default:
            break;
        }

        translate(newCenterX - centerX,
                newCenterY - centerY,
                false);
        rotate(mCurrentRotationValue - rotation);
    }

    /**
     * Change to portrait coordinate system.
     * @param newWidth the width.
     * @param newHeight the height.
     */
    public void changePortraitCoordinateSystem(int newWidth, int newHeight) {
        float portraitWidht = (float) Math.min(newWidth, newHeight);
        float portraitHeight = (float) Math.max(newWidth, newHeight);
        float scaleX = portraitWidht / Math.min(mPreviewWidth, mPreviewHeight);
        float scaleY = portraitHeight / Math.max(mPreviewWidth, mPreviewHeight);
        float centerX = centerX();
        float centerY = centerY();
        float newCenterX = scaleX * centerX;
        float newCenterY = scaleY * centerY;
        // translate to new center
        translate(newCenterX - centerX,
                  newCenterY - centerY,
                  false);
        // scale by animationScaleX
        scale(scaleX, false);
        rotate(mCurrentRotationValue);
        setRendererSize((int) portraitWidht, (int) portraitHeight);
    }

    /**
     * Change coordinate system.
     * @param newWidth the width.
     * @param newHeight the height.
     * @param rotation the rotation.
     */
    public void changeCoordinateSystem(int newWidth, int newHeight, int rotation) {
        float animationScaleX = (float) Math.min(newWidth, newHeight)
                / Math.min(mPreviewWidth, mPreviewHeight);
        float animationScaleY = (float) Math.max(newWidth, newHeight)
                / Math.max(mPreviewWidth, mPreviewHeight);
        // keep original centerX and centerY
        float centerX = centerX();
        float centerY = centerY();
        float tempValue;
        switch (rotation) {
        case 0:
            break;
        case 90:
            tempValue = centerX;
            centerX = centerY;
            centerY = mPreviewWidth - tempValue;
            break;
        case 180:
            if (CameraUtil.isTablet()) {
                centerX = mPreviewWidth - centerX;
                centerY = mPreviewHeight - centerY;
            }
            break;
        case 270:
            tempValue = centerX;
            centerX = mPreviewHeight - centerY;
            centerY = tempValue;
            break;
        default:
            break;
        }
        // translate to new coordinate system
        translate(centerX - centerX(), centerY - centerY(), false);
        // translate from old renderer coordinate system to new renderer coordinate system
        translate(centerX * animationScaleX - centerX,
                  centerY * animationScaleY - centerY,
                  false);
        // scale by animationScaleX
        scale(animationScaleX, false);
        // scale to translate by animationScaleY / animationScaleX to match correct top distance
        scaleToTranslateY(animationScaleY / animationScaleX);
        // compute rotation
//        rotate(-rotation);
        float rotationRotate = formatRotationValue(360 - rotation);
//        rotationRotate = AnimationRect.checkRotationLimit(rotationRotate, mCurrentRotationValue);
        // rotate by current orienation
        rotate(mCurrentRotationValue + rotationRotate);
//        rotate(rotationRotate);
    }

    /**
     * Format rotation value.
     * @param rotation the original rotation.
     * @return the formatted rotation.
     */
    public static float formatRotationValue(float rotation) {
        if (rotation > 180) {
            rotation = rotation - 360;
        }
        if (rotation < -180) {
            rotation = rotation + 360;
        }
        rotation = rotation % 360;
        return rotation;
    }

    /**
     * Check rotation limitation.
     * @param rotation the rotation.
     * @param rotatedRotation rotated rotation.
     * @return the result rotation.
     */
    public static float checkRotationLimit(float rotation, float rotatedRotation) {
        // same direction should -, reverse direction should +
        boolean rotatedClockwise = rotatedRotation > 0;
        boolean currentRotatedClockwise = rotation > 0;
        float currentRotatedRotation = (rotatedClockwise ==
                currentRotatedClockwise) ? rotatedRotation
                : -rotatedRotation;
        rotation -= currentRotatedRotation;
        if (rotation < sRotationLimitedMin) {
            rotation = sRotationLimitedMin;
        }
        if (rotation > sRotationLimitedMax) {
            rotation = sRotationLimitedMax;
        }
        rotation += currentRotatedRotation;
        return rotation;
    }

    /**
     * Set vertex value.
     * @param left left value.
     * @param top top value.
     * @param right right value.
     * @param bottom bottom value.
     */
    private void setVertex(float left, float top, float right, float bottom) {
        mLeftTop[0] = left;
        mLeftTop[1] = top;
        mRightTop[0] = right;
        mRightTop[1] = top;
        mLeftBottom[0] = left;
        mLeftBottom[1] = bottom;
        mRightBottom[0] = right;
        mRightBottom[1] = bottom;
    }

    private void checkTranslate() {
        if (mPreviewWidth <= 0 || mPreviewHeight <= 0) {
            return;
        }
        // check left
        if (mRectF.left < 0) {
            mAnimationMatrix.reset();
            mAnimationMatrix.setTranslate(-mRectF.left, 0);
            mAnimationMatrix.mapRect(mRectF);
        }
        // check right
        if (mRectF.right > mPreviewWidth) {
            mAnimationMatrix.reset();
            mAnimationMatrix.setTranslate(mPreviewWidth - mRectF.right, 0);
            mAnimationMatrix.mapRect(mRectF);
        }
        // check top
        if (mRectF.top < 0) {
            mAnimationMatrix.reset();
            mAnimationMatrix.setTranslate(0, -mRectF.top);
            mAnimationMatrix.mapRect(mRectF);
        }
        // check bottom
        if (mRectF.bottom > mPreviewHeight) {
            mAnimationMatrix.reset();
            mAnimationMatrix.setTranslate(0, mPreviewHeight - mRectF.bottom);
            mAnimationMatrix.mapRect(mRectF);
        }
    }
    /**
     * Get max scale value.
     * @return the max scale value.
     */
    private float getMaxScaleValue() {
        float maxScaleValue = Math.min(getXMaxScaleValue(), getYMaxScaleValue());
        maxScaleValue = mCurrentScaleValue * maxScaleValue;
        return maxScaleValue > MAX_SCALE_VALUE ? MAX_SCALE_VALUE : maxScaleValue;
    }

    private float getMinScaleValue() {
        return MIN_SCALE_VALUE;
    }

    private float getScaleToOutterRect() {
        return (float) Math.sqrt(4 * mOriginalDistance * mOriginalDistance / centerX() * centerX());
    }

    private float getXMaxScaleValue() {
        return Math.min(
                (float) Math.sqrt((centerX() * centerX())
                        / ((centerX() - mLeftTop[0]) * (centerX() - mLeftTop[0]))),
                (float) Math.sqrt(((mPreviewWidth - centerX()) * (mPreviewWidth - centerX()))
                        / ((mRightBottom[0] - centerX())) * ((mRightBottom[0] - centerX()))));
    }

    private float getYMaxScaleValue() {
        return Math.min(
                (float) Math.sqrt((centerY() * centerY())
                        / ((centerY() - mLeftTop[1]) * (centerY() - mLeftTop[1]))),
                (float) Math.sqrt(((mPreviewHeight - centerY()) * (mPreviewHeight - centerY()))
                        / ((mRightBottom[1] - centerY())) * ((mRightBottom[1] - centerY()))));
    }
}