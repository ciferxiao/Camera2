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
package com.mediatek.camera.feature.mode.pip;

import android.app.Activity;
import android.view.Surface;

import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.pipwrapping.AnimationRect;
import com.mediatek.camera.feature.mode.pip.pipwrapping.IPipCaptureWrapper;
import com.mediatek.camera.feature.mode.pip.pipwrapping.PipOperator;
import com.mediatek.camera.feature.mode.pip.pipwrapping.RendererManager.RendererCallback;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;
import com.mediatek.camera.feature.mode.pip.view.IPipView;
import com.mediatek.camera.feature.mode.pip.view.PipView;

/**
 * Pip controller.
 */
public class PipController implements
        PipOperator.Listener,
        IPipView.PipViewCallback {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PipController.class.getSimpleName());
    private Activity mActivity;
    private PipOperator mPipOperator;
    private IPipView mPipView;
    private Listener mListener;
    private Object mSyncLock = new Object();

    /**
     * pip controller callback.
     */
    public interface Listener {
        /**
         * On pip picture taken.
         * @param jpegData the jpeg buffer.
         */
        void onPIPPictureTaken(byte[] jpegData);

        /**
         * Can do start preview after take picture.
         */
        void doStartPreview();
        /**
         * Notify when pip switch called in renderer.
         */
        void onPipSwitchedInRenderer();
        /**
         * Notify when first frame swapped.
         *
         * @param timestamp buffer's timestamp.
         */
        void onFirstFrameAvailable(long timestamp);
        /**
         * Whether bottom is back camera.
         * @return whether bottom is back camera.
         */
        boolean bottomGraphicIsBackCamera();
        /**
         * Get bottom graphic's camera id.
         * @return bottom graphic camera's id.
         */
        String getBottomGraphicCameraId();

        /**
         * whether current status can switch pip.
         * @return if right status return true.
         */
        boolean canSwitchPip();
    }

    @Override
    public void onTopGraphicMoving(AnimationRect newPositionRect) {
        mPipOperator.updateTopGraphic(newPositionRect);
    }

    @Override
    public void onPipSwitchCalled() {
        LogHelper.d(TAG, "onPipSwitchCalled");
        if (mPipOperator != null && mListener.canSwitchPip()) {
            mPipOperator.switchPIP();
        }
    }

    @Override
    public String getBottomGraphicCameraId() {
        return mListener.getBottomGraphicCameraId();
    }

    /**
     * offer pip jpeg data.
     * @param jpegData jpeg data.
     */
    @Override
    public void onPIPPictureTaken(byte[] jpegData) {
        LogHelper.d(TAG, "onPIPPictureTaken jpegData = " + jpegData +
                " mListener = " + mListener);
        if (mListener != null) {
            mListener.onPIPPictureTaken(jpegData);
        }
    }

    @Override
    public void unlockNextCapture() {
        LogHelper.d(TAG, "canDoStartPreview mListener = " + mListener);
        if (mListener != null) {
            mListener.doStartPreview();
        }
    }

    /**
     * Set listener.
     * @param listener the listener.
     */
    public void setListener(Listener listener) {
        LogHelper.d(TAG, "setListener");
        mListener = listener;
    }

    /**
     * Set a surface to receive pip preview buffer from pip wrapping.
     * @param surface used to receive pip preview buffer
     */
    public void setPreviewSurface(Surface surface) {
        LogHelper.d(TAG, "setPreviewSurface mPipOperator = " + mPipOperator);
        synchronized (mSyncLock) {
            if (mPipOperator != null) {
                mPipOperator.setPreviewSurface(surface);
            }
        }
    }

    /**
     * Check whether is during switch pip.
     * @return whether is during switch pip.
     */
    public boolean isDuringSwitchPip() {
        synchronized (mSyncLock) {
            if (mPipOperator != null) {
                return mPipOperator.isDuringSwitchPip();
            }
        }
        return false;
    }

    /**
     * notify surface view has been destroyed.
     * @param surface the destroyed surface.
     */
    public void notifySurfaceViewDestroyed(Surface surface) {
        synchronized (mSyncLock) {
            if (mPipOperator != null) {
                mPipOperator.notifySurfaceViewDestroyed(surface);
            }
        }
    }

    /**
     * Get pip capture wrapper.
     * @return the pip capture wrapper.
     */
    public IPipCaptureWrapper getPipCaptureWrapper() {
        return mPipOperator;
    }

    /**
     * Set the bottom/top texture size.
     * Note: This function must be called before setUpSurfaceTextures.
     * @param size  preview texture's size.
     */
    public void setPreviewTextureSize(Size size) {
        if (size == null) {
            return;
        }
        LogHelper.d(TAG, "setTextureSize width = " + size.getWidth() +
                " height = " + size.getHeight());
        Size newSize = new Size(Math.min(size.getWidth(), size.getHeight()),
                Math.max(size.getWidth(), size.getHeight()));
        synchronized (mSyncLock) {
            if (mPipOperator != null && mPipView != null) {
                mPipOperator.setUpSurfaceTextures();
                mPipOperator.setPreviewTextureSize(newSize.getWidth(), newSize.getHeight());
                mPipView.setRendererSize(newSize);
                mPipOperator.updateTopGraphic(mPipView.getCurrentTopGraphicPosition());
            }
        }
    }

    /**
     * This surface texture is used to receive bottom camera device's preview buffer.
     * <p>
     * It will update preview buffer to pip GL thread for processing when onFrameAvailable
     * @return pip bottom surface texture
     */
    public SurfaceTextureWrapper getBottomSurfaceTextureWrapper() {
        synchronized (mSyncLock) {
            if (mPipOperator != null) {
                return mPipOperator.getBottomSurfaceTextureWrapper();
            }
        }
        return null;
    }

    /**
     * This surface texture is used to receive top camera device's preview buffer.
     * <p>
     * It will update preview buffer to pip GL thread for processing when onFrameAvailabe
     * @return pip top surface texture
     */
    public SurfaceTextureWrapper getTopSurfaceTextureWrapper() {
        synchronized (mSyncLock) {
            if (mPipOperator != null) {
                return mPipOperator.getTopSurfaceTextureWrapper();
            }
        }
        return null;
    }

    /**
     * Prepare recording renderer, will new a recording thread.
     * <p>
     * Note: before prepareRecording, the recording surface must be set.
     */
    public void prepareRecording() {
        LogHelper.d(TAG, "prepareRecording");
        if (mPipOperator != null) {
            mPipOperator.prepareRecording();
        }
    }

    /**
     * Set a recording surface to receive pip buffer from pip wrapping.
     * @param surface a recording surface used to receive pip buffer.
     * @param orientation the vertex orientation.
     */
    public void setRecordingSurface(Surface surface, int orientation) {
        LogHelper.d(TAG,
                "setRecordingSurface surface = " + surface + " orientation = " + orientation);
        if (mPipOperator != null) {
            mPipOperator.setRecordingSurface(surface, orientation);
        }
    }

    /**
     * Begin to push pip frame to video recording surface.
     */
    public void startPushVideoBuffer() {
        LogHelper.d(TAG, "startPushVideoBuffer");
        if (mPipOperator != null) {
            mPipOperator.startPushVideoBuffer();
        }
    }

    /**
     * Stop to push pip frame to video recording surface.
     */
    public void stopPushVideoBuffer() {
        LogHelper.d(TAG, "stopPushVideoBuffer");
        if (mPipOperator != null) {
            mPipOperator.stopPushVideoBuffer();
        }
    }

    /**
     * Take a video snap shot by orientation.
     * @param orientation video snap shot orientation
     * @param isBackBottom is back camera in bottom.
     */
    public void takeVideoSnapshot(int orientation, boolean isBackBottom) {
        LogHelper.d(TAG, "takeVideoSnapshot orientation = " + orientation + " isBackBottom = "
                + isBackBottom);
        if (mPipOperator != null) {
            mPipOperator.takeVideoSnapshot(orientation, isBackBottom);
        }
    }

    /**
     * On view orientation changed.
     *
     * @param viewOrientation the view new orientation.
     */
    public void onViewOrientationChanged(int viewOrientation) {
        LogHelper.d(TAG, "onViewOrientationChanged orientation = " + viewOrientation);
        // notify pip gesture orientation changed
        if (mPipView != null) {
            mPipView.updateOrientation(viewOrientation);
        }
        if (mPipOperator != null) {
            mPipOperator.updateGSensorOrientation(viewOrientation);
            mPipOperator.updateTopGraphic(mPipView.getCurrentTopGraphicPosition());
        }
    }

    /**
     * Init pip controller.
     * @param app the app.
     */
    public void init(IApp app) {
        LogHelper.d(TAG, "init mPipOperator = " + mPipOperator);
        mActivity = app.getActivity();
        synchronized (mSyncLock) {
            if (mPipView == null) {
                mPipView = new PipView(this, app);
            }
            if (mPipOperator == null) {
                mPipOperator = new PipOperator(mActivity, this);
                mPipOperator.initPIPRenderer(mRendererCallback);
            }
            updateEffectTemplates(
                    R.drawable.rear_01,
                    R.drawable.front_01,
                    R.drawable.front_01_focus,
                    R.drawable.plus);
        }
    }

    /**
     * Un init pip controller.
     */
    public void unInit() {
        LogHelper.d(TAG, "[unInit]+");
        synchronized (mSyncLock) {
            if (mPipOperator != null) {
                mPipOperator.unInitPIPRenderer();
                mPipOperator = null;
            }
            if (mPipView != null) {
                mPipView.release();
                mPipView = null;
            }
        }
        LogHelper.d(TAG, "[unInit]-");
    }

    /**
     * After stop preview.
     */
    public void afterStopPreview() {
        if (mPipOperator != null) {
            mPipOperator.afterStopPreview();
        }
    }

    /**
     * update pip template resource.
     * Note: This function must be called before setUpSurfaceTextures.
     * @param backResourceId bottom graphic template
     * @param frontResourceId top graphic template
     * @param effectFrontHighlightId top graphic highlight template
     * @param editButtonResourceId top graphic edit template
     */
    private void updateEffectTemplates(int backResourceId, int frontResourceId,
            int effectFrontHighlightId, int editButtonResourceId) {
        LogHelper.d(TAG, "updateEffectTemplates");
        if (mPipOperator != null) {
            mPipOperator.updateEffectTemplates(backResourceId, frontResourceId,
                    effectFrontHighlightId, editButtonResourceId);
        }
    }

    private final RendererCallback mRendererCallback = new RendererCallback() {
        @Override
        public void onFirstFrameAvailable(long timestamp) {
            if (mListener != null) {
                mListener.onFirstFrameAvailable(timestamp);
            }
        }
        @Override
        public void onPipSwitchedInRenderer() {
            if (mListener != null) {
                mListener.onPipSwitchedInRenderer();;
            }
        }

        @Override
        public boolean bottomGraphicIsBackCamera() {
            if (mListener != null) {
                return mListener.bottomGraphicIsBackCamera();
            }
            return false;
        }
    };
}