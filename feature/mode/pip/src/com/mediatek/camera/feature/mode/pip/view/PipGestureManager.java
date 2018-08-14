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
package com.mediatek.camera.feature.mode.pip.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.RectF;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;

import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.IAppUiListener.OnGestureListener;
import com.mediatek.camera.common.IAppUiListener.OnPreviewAreaChangedListener;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.pipwrapping.AnimationRect;
import com.mediatek.camera.feature.mode.pip.pipwrapping.GLUtil;
import com.mediatek.camera.feature.mode.pip.pipwrapping.PipOperator;

/**
 * Pip gesture manager implementation.
 */
public class PipGestureManager implements
        IPipGesture,
        OnGestureListener,
        OnPreviewAreaChangedListener {
    public static final Tag TAG = new Tag(PipGestureManager.class.getSimpleName());
    private Activity mActivity;
    private AnimationRect mTopGraphicRect = null;
    private static Object sSyncTopGraphicRect = new Object();
    private final int mRectToTop;
    private float mCurrentRectToTop;
    private float mRotatedRotation = 0;
    private int mKeepLastOrientation = 0;
    private int mKeepLastDisplayRotation = 0;
    // top graphic rectangle animation (translate, scale, rotate)
    public static final int ANIMATION_TRANSLATE = 0;
    public static final int ANIMATION_SCALE = 1;
    public static final int ANIMATION_ROTATE = 2;
    private float mXScale = 1f;
    private float mYScale = 1f;
    private boolean mIsTranslateAnimation = false;
    private boolean mIsScaleRotateAnimation = false;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mRotation = 0;
    private int mKeepPreviewOrientation = 0;
    private int mCurrentPreviewOrientation = 0;
    private GestureCallback mGestureCallback;
    private int mEditButtonSize = 0;
    private RectF mEditButtonRect;
    private RectF mPreviewArea;

    /**
     * Construct pip gesture manager.
     * @param activity the camera activity.
     * @param gestureCallback the gesture callback.
     */
    public PipGestureManager(Activity activity, GestureCallback gestureCallback) {
        mActivity = activity;
        mRectToTop = 100;
        mGestureCallback = gestureCallback;
        mTopGraphicRect = new AnimationRect();
        mEditButtonRect = new RectF();
        setDisplayRotation(GLUtil.getDisplayRotation(activity));
    }

    /**
     * Set preview orientation.
     * @param orientation preview orientation.
     */
    public void setPreviewOrientation(int orientation) {
        mCurrentPreviewOrientation = orientation;
    }

    @Override
    public void setPreviewSize(Size previewSize) {
        int width = previewSize.getWidth();
        int height = previewSize.getHeight();
        LogHelper.d(TAG, "setPreviewSize width = " + width + " height = " + height + " oldWidth = "
                + mPreviewWidth + " oldHeight = " + mPreviewHeight + " mTopGraphicRect = "
                + mTopGraphicRect + " mCurrentPreviewOrientation = " + mCurrentPreviewOrientation
                + " mKeepPreviewOrientation = " + mKeepPreviewOrientation);
        if (mTopGraphicRect == null || (width == mPreviewWidth && height == mPreviewHeight)) {
            return;
        }
        synchronized (sSyncTopGraphicRect) {
            WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);
            int maxEdge = Math.max(point.x, point.y);
            if (mPreviewWidth == 0 && mPreviewHeight == 0) {
                mCurrentRectToTop = mRectToTop * Math.max(width, height) / (float) maxEdge;
                // first time, initialize top graphic rectangle
                float[] topRight = GLUtil.createTopRightRect(width, height, mCurrentRectToTop);
                mTopGraphicRect.setRendererSize(width, height);
                mTopGraphicRect.initialize(topRight[0], topRight[1], topRight[6], topRight[7]);
                mTopGraphicRect.rotate(mRotation);
            } else {
                float animationScaleX = (float) Math.min(width, height)
                        / Math.min(mPreviewWidth, mPreviewHeight);
                float animationScaleY = (float) Math.max(width, height)
                        / Math.max(mPreviewWidth, mPreviewHeight);
                float tempValue = 0f;
                // Translate to new renderer coordinate system(landscape ->
                // portrait or portrait -> landscape)
                mTopGraphicRect.setRendererSize(width, height);
                float centerX = mTopGraphicRect.centerX();
                float centerY = mTopGraphicRect.centerY();
                switch ((mCurrentPreviewOrientation - mKeepPreviewOrientation + 360) % 360) {
                    case 0:
                        break;
                    case 90:
                        tempValue = centerX;
                        centerX = mPreviewHeight - centerY;
                        centerY = tempValue;
                        break;
                    case 180:
                        break;
                    case 270:
                        tempValue = centerX;
                        centerX = centerY;
                        centerY = mPreviewWidth - tempValue;
                        break;
                    default:
                        break;
                }
                // translate to new coordinate system
                mTopGraphicRect.translate(centerX - mTopGraphicRect.centerX(), centerY
                        - mTopGraphicRect.centerY(), false);
                // translate from old renderer coordinate system to new renderer
                // coordinate system
                mTopGraphicRect.translate(mTopGraphicRect.centerX() * animationScaleX
                        - mTopGraphicRect.centerX(), mTopGraphicRect.centerY() * animationScaleY
                        - mTopGraphicRect.centerY(), false);
                // scale by animationScaleX
                mTopGraphicRect.scale(animationScaleX, false);
                // scale to translate by animationScaleY / animationScaleX to
                // match correct top distance
                mTopGraphicRect.scaleToTranslateY(animationScaleY / animationScaleX);
                // rotate
                mRotation += (mCurrentPreviewOrientation - mKeepPreviewOrientation);
                mRotation = AnimationRect.formatRotationValue(mRotation);
                mRotation = AnimationRect.checkRotationLimit(mRotation, mRotatedRotation);
                mTopGraphicRect.rotate(mRotation);
            }
            mKeepPreviewOrientation = mCurrentPreviewOrientation;
            mPreviewWidth = width;
            mPreviewHeight = height;
            mTopGraphicRect.setRendererSize(width, height);
        }
        mEditButtonSize = Math.min(width, height)
                / PipOperator.PIPCustomization.TOP_GRAPHIC_EDIT_BUTTON_RELATIVE_VALUE;
        initEditButtonRect(mTopGraphicRect.getRightBottom()[0],
                mTopGraphicRect.getRightBottom()[1], mEditButtonSize);
    }

    /**
     * when orientation changed, rotate top graphic rectangle.
     * @param orientation g-sensor orientation.
     */
    public void onViewOrientationChanged(int orientation) {
        LogHelper.d(TAG, "onOrientationChanged orientation = " + orientation
                 + " mKeepLastOrientation = " + mKeepLastOrientation);
        synchronized (sSyncTopGraphicRect) {
            if (orientation != mKeepLastOrientation) {
                mRotatedRotation += ((360 - orientation + mKeepLastOrientation) % 360);
                mRotatedRotation = AnimationRect.formatRotationValue(mRotatedRotation);
                rotate(orientation - mKeepLastOrientation);
                mKeepLastOrientation = orientation;
                LogHelper.d(TAG, "onOrientationChanged orientation = " + orientation
                        + " mKeepLastOrientation = " + mKeepLastOrientation
                        + " mRotatedRotation = " + mRotatedRotation);
            }
        }
    }

    /**
     * Set activity's display rotation.
     * @param displayRotation display rotation.
     */
    public void setDisplayRotation(int displayRotation) {
        LogHelper.d(TAG, "setDisplayRotation displayRotation = " + displayRotation);
        synchronized (sSyncTopGraphicRect) {
            if (displayRotation != mKeepLastDisplayRotation) {
                LogHelper.d(TAG, "setDisplayRotation rotate = "
                        + (mKeepLastDisplayRotation - displayRotation));
                // display rotation changes, should rotate by new
                // displayRotation
                rotate(mKeepLastDisplayRotation - displayRotation);
                /**
                 * when camera's activity can be locked to reverse rotation,
                 * should consider: displayRotation switches between standard
                 * rotation and reverse rotation, should translate to new
                 * position.
                 */
                if (Math.abs(mKeepLastDisplayRotation - displayRotation) >= 180) {
                    LogHelper.d(TAG, "setDisplayRotation" + " translate x = "
                            + (mPreviewWidth - 2 * mTopGraphicRect.centerX()) + " y = "
                            + (mPreviewHeight - 2 * mTopGraphicRect.centerY()));
                    initVertexData(mPreviewWidth - 2 * mTopGraphicRect.centerX(), mPreviewHeight
                            - 2 * mTopGraphicRect.centerY(), ANIMATION_TRANSLATE);
                }
                mKeepLastDisplayRotation = displayRotation;
            }
        }
    }

    @Override
    public AnimationRect getTopGraphicRect(int gSensorOrientation) {
        onViewOrientationChanged(gSensorOrientation);
        synchronized (sSyncTopGraphicRect) {
            initEditButtonRect(mTopGraphicRect.getRightBottom()[0],
                    mTopGraphicRect.getRightBottom()[1], mEditButtonSize);
            return mTopGraphicRect.copy();
        }
    }

    @Override
    public void init() {

    }

    @Override
    public void unInit() {

    }

    @Override
    public IAppUiListener.OnPreviewAreaChangedListener getPreviewAreaChangedListener() {
        return this;
    }

    @Override
    public IAppUiListener.OnGestureListener getGestureListener() {
        return this;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        boolean downInPip = false;
        if (mPreviewArea != null) {
            downInPip = doOnDown(
                    event.getX(),
                    event.getY(),
                    (int) mPreviewArea.width(), (int) mPreviewArea.height());
            if (downInPip) {
                mGestureCallback.onTopGraphicPositionMoving();
            }
        }
        return downInPip;
    }

    @Override
    public boolean onUp(MotionEvent event) {
        doOnUp();
        mGestureCallback.onTopGraphicPositionMoving();
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        boolean scrollInPip = doOnScroll(
                dx,
                dy,
                e2.getX() - e1.getX(),
                e2.getY() - e1.getY());
        if (scrollInPip) {
            mGestureCallback.onTopGraphicPositionMoving();
        }
        return scrollInPip;
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        boolean singleTapPip = doOnSingleTapUp(x, y);
        if (singleTapPip) {
            mGestureCallback.onTopGraphicPositionMoving();
        }
        return singleTapPip;
    }

    @Override
    public boolean onLongPress(float x, float y) {
        boolean longPressInPip = doOnLongPress(x, y);
        if (longPressInPip) {
            mGestureCallback.onTopGraphicPositionMoving();
        }
        return longPressInPip;
    }

    @Override
    public boolean onSingleTapConfirmed(float x, float y) {
        return false;
    }

    @Override
    public boolean onDoubleTap(float x, float y) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public void onPreviewAreaChanged(RectF newPreviewArea, Size previewSize) {
        mPreviewArea = newPreviewArea;
    }

    /**
     * Rotate top graphic by degree.
     * @param degrees rotate degree.
     */
    public void rotate(int degrees) {
        LogHelper.d(TAG, "rotate degrees = " + degrees + " mTopGraphicRect = " + mTopGraphicRect);
        if (mTopGraphicRect == null) {
            return;
        }
        mRotation += -degrees;
        mRotation = AnimationRect.formatRotationValue(mRotation);
        mRotation = AnimationRect.checkRotationLimit(mRotation, mRotatedRotation);
        // rotate mVtxRotateMtx
        mTopGraphicRect.rotate(mRotation);
    }

    /**
     * Get top graphic's rect rotation.
     * @return the rect rotation.
     */
    public float getAnimationRectRotation() {
        synchronized (sSyncTopGraphicRect) {
            LogHelper.d(TAG, "getAnimationRectRotation mRotation = " + mRotation);
            return mRotation;
        }
    }

    private boolean doOnDown(float x, float y, int relativeWidth, int relativeHeight) {
        switch (GLUtil.getDisplayRotation(mActivity)) {
            case 0:
                break;
            case 90:
                float temp = x;
                x = relativeHeight - y;
                y = temp;
                break;
            case 180:
                x = relativeWidth - x;
                y = relativeHeight - y;
                break;
            case 270:
                float temp2 = x;
                x = y;
                y = relativeWidth - temp2;
                break;
            default:
                break;
        }
        // map scale, scale display size to original preview size
        boolean mFboPreviewIsLandscape = (GLUtil.getDisplayOrientation(0,
                Integer.parseInt(mGestureCallback.getBottomGraphicCameraId())) % 90 == 0);
        boolean mRelativeFrameIsLandscape =
                (GLUtil.getDisplayRotation(mActivity) % 180 == 0);
        if (mFboPreviewIsLandscape != mRelativeFrameIsLandscape) {
            int tempWidth = relativeWidth;
            relativeWidth = relativeHeight;
            relativeHeight = tempWidth;
        }
        mXScale = (float) mPreviewWidth / relativeWidth;
        mYScale = (float) mPreviewHeight / relativeHeight;
        x = mXScale * x;
        y = mYScale * y;
        LogHelper.d(TAG, "scale: mXScale = " + mXScale + "mYScale = " + mYScale);
        // compute animation type
        mIsTranslateAnimation = mTopGraphicRect.getRectF().contains(x, y);
        mIsScaleRotateAnimation = mEditButtonRect.contains(x, y);
        mTopGraphicRect.setHighLightEnable(mIsTranslateAnimation || mIsScaleRotateAnimation);
        LogHelper.d(TAG, "isTranslateAnimation = " + mIsTranslateAnimation + " isScaleAnimation = "
                + mIsScaleRotateAnimation);
        if (mIsTranslateAnimation || mIsScaleRotateAnimation) {
            mGestureCallback.onTopGraphicPositionMoving();
        }
        return mIsTranslateAnimation || mIsScaleRotateAnimation;
    }

    private boolean doOnScroll(float dx, float dy, float totalX, float totalY) {
        LogHelper.d(TAG, "before onScroll dx = " + dx + " dy = " + dy + " totalX = "
                + totalX + " totalY = " + totalY
                + " isTranslateAnimation = " + mIsTranslateAnimation
                + " isScaleAnimation = " + mIsScaleRotateAnimation);
        if (!mIsTranslateAnimation && !mIsScaleRotateAnimation) {
            return false;
        }
        synchronized (sSyncTopGraphicRect) {
            // transform gestures to portrait, because pip gestures
            // are computed always in portrait coordinate.
            switch (mKeepLastDisplayRotation) {
                case 0:
                    break;
                case 90:
                    float temp2 = dx;
                    dx = -dy;
                    dy = temp2;
                    break;
                case 180:
                    dx = -dx;
                    dy = -dy;
                    break;
                case 270:
                    float temp = dx;
                    dx = dy;
                    dy = -temp;
                    break;
                default:
                    break;
            }
            dx = dx * mXScale;
            dy = dy * mYScale;
            if (mIsScaleRotateAnimation) {
                initVertexData(-dx, -dy, ANIMATION_SCALE);
            } else if (mIsTranslateAnimation) {
                initVertexData(-dx, -dy, ANIMATION_TRANSLATE);
            }
        }
        return mIsTranslateAnimation || mIsScaleRotateAnimation;
    }

    private boolean doOnUp() {
        LogHelper.d(TAG, "doOnUp");
        mTopGraphicRect.setHighLightEnable(false);
        mIsScaleRotateAnimation = false;
        mIsTranslateAnimation = false;
        return false;
    }

    private boolean doOnSingleTapUp(float x, float y) {
        LogHelper.d(TAG, "onSingleTapUp x = " + x + " y = " + y + " isTranslateAnimation = "
                + mIsTranslateAnimation);
        if (mIsTranslateAnimation && !mIsScaleRotateAnimation) {
            mGestureCallback.onTopGraphicSingleTapUp();
        }
        return mIsTranslateAnimation || mIsScaleRotateAnimation;
    }

    private boolean doOnLongPress(float x, float y) {
        if (mIsTranslateAnimation) {
            mGestureCallback.onTopGraphicSingleTapUp();
        }
        return mIsTranslateAnimation || mIsScaleRotateAnimation;
    }

    private void initVertexData(float dx, float dy, int animationType) {
        switch (animationType) {
        case ANIMATION_TRANSLATE:
            mTopGraphicRect.translate(dx, dy, true);
            mTopGraphicRect.rotate(mRotation);
            break;
        case ANIMATION_SCALE:
            // scale
            float newX = mTopGraphicRect.getRightBottom()[0] + dx;
            float newY = mTopGraphicRect.getRightBottom()[1] + dy;
            float oldDistance = (float) Math.sqrt((mTopGraphicRect.centerX() - mTopGraphicRect
                    .getRightBottom()[0])
                    * (mTopGraphicRect.centerX() - mTopGraphicRect.getRightBottom()[0])
                    + (mTopGraphicRect.centerY() - mTopGraphicRect.getRightBottom()[1])
                    * (mTopGraphicRect.centerY() - mTopGraphicRect.getRightBottom()[1]));
            float newDistance = (float) Math.sqrt((mTopGraphicRect.centerX() - newX)
                    * (mTopGraphicRect.centerX() - newX) + (mTopGraphicRect.centerY() - newY)
                    * (mTopGraphicRect.centerY() - newY));
            newDistance = mTopGraphicRect.adjustScaleDistance(newDistance);
            float scaleRatio = newDistance / oldDistance;
            mTopGraphicRect.translate(0, 0, true);
            mTopGraphicRect.scale(scaleRatio, true);
            mTopGraphicRect.rotate(mRotation);
            // rotate
            float degress = 0;
            degress = (float) rotateAngle(dx, dy);
            mRotation += degress;
            mRotation = AnimationRect.formatRotationValue(mRotation);
            mRotation = AnimationRect.checkRotationLimit(mRotation, mRotatedRotation);
            // rotate mVtxRotateMtx
            mTopGraphicRect.rotate(mRotation);
            break;
        case ANIMATION_ROTATE:
            break;
        default:
            break;
        }
    }

    private double rotateAngle(float dx, float dy) {
        double angle;
        double angle1;
        double angle2;
        float centerX = mTopGraphicRect.centerX();
        float centerY = mTopGraphicRect.centerY();
        float right = mTopGraphicRect.getRightBottom()[0];
        float bottom = mTopGraphicRect.getRightBottom()[1];
        float newRight = right + dx;
        float newBottom = bottom + dy;
        angle1 = Math.atan2(bottom - centerY, right - centerX) * 180 / Math.PI;
        angle2 = Math.atan2(newBottom - centerY, newRight - centerX) * 180 / Math.PI;
        angle1 = (angle1 + 360) % 360;
        angle2 = (angle2 + 360) % 360;
        angle = angle2 - angle1;
        return angle;
    }

    private void initEditButtonRect(float rCenterX, float rCenterY, float edge) {
        LogHelper.d(TAG, "initVertexData rCenterX = " + rCenterX +
                " rCenterY = " + rCenterY + " edge = " + edge);
        mEditButtonRect.set(rCenterX - edge / 2, rCenterY - edge / 2, rCenterX + edge / 2, rCenterY
                + edge / 2);
    }
}