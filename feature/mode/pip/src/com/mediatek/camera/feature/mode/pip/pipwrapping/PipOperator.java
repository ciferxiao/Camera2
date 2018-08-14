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
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Surface;

import com.mediatek.camera.portability.jpeg.encoder.JpegEncoder;
import com.mediatek.camera.common.utils.Size;

/**
 * This class is used to communicate with pip wrapping package.
 * <p>
 * Basic operating flow:
 * <p>
 * 1. start preview path:
 * <p>
 * Note: updatexxxx can be called any time during preview
 * <p>
 * initPIPRenderer ->updateEffectTemplates --> setPreviewTextureSize
 * --> setUpSurfaceTextures -->setPreviewSurface-->getBottomSurfaceTextureWrapper
 * --> getTopSurfaceTexture --> updateTopGraphicPosition --> updateOrientation-->start preview
 * <p>
 * 2. take picture path:
 *    <p>
 *    Firstly, you should register PipOperator.Listener to be notified when jpeg is done.
 *    <p>
 *    After preview is started, send two jpeg by: takePicture --> takePicture and then wait
 *    callback comes back.
 * <p>
 * 3. video recording path:
 * <p>
 *    prepareRecording -->  setRecordingSurface --> startPushVideoBuffer
 * <p>
 * 4. video snap shot path:
 * <p>
 *    takeVideoSnapshot
 */
public class PipOperator implements IPipCaptureWrapper {
    private static final String TAG = PipOperator.class.getSimpleName();
    private Activity mActivity;
    private RendererManager mRendererManager;
    private Listener mListener;
    private JpegEncoder mVssJpegEncoder;

    /**
     * Pip operator listener.
     */
    public interface Listener {
        /**
         * On pip capture taken.
         * @param jpegData jpeg buffer.
         */
        void onPIPPictureTaken(byte[] jpegData);

        /**
         * Unlock next capture.
         */
        void unlockNextCapture();
    }

    /**
     * Construct an instance of pip operator.
     * @param activity the activity.
     * @param listener the listener.
     */
    public PipOperator(Activity activity, Listener listener) {
        mActivity = activity;
        mListener = listener;
    }

    @Override
    public int initCapture(int[] inputFormats) {
        return mRendererManager.initCapture(inputFormats);
    }

    @Override
    public void setCaptureSurface(Surface surface) {
        mRendererManager.setCaptureSurface(surface);
    }

    @Override
    public void setCaptureSize(Size bottomCaptureSize, Size topCaptureSize) {
        mRendererManager.setCaptureSize(bottomCaptureSize, topCaptureSize);
    }

    @Override
    public SurfaceTextureWrapper getBottomCapSt() {
        return mRendererManager.getBottomCapSt();
    }

    @Override
    public SurfaceTextureWrapper getTopCapSt() {
        return mRendererManager.getTopCapSt();
    }

    @Override
    public void setJpegRotation(boolean isBottomCam, int rotation) {
        mRendererManager.setJpegRotation(isBottomCam, rotation);
    }

    @Override
    public void unInitCapture() {
        mRendererManager.unInitCapture();
    }

    @Override
    public void onPictureTaken(byte[] jpegData) {
        Log.d(TAG, "onPIPPictureTaken jpegData = " + jpegData);
        if (mListener != null) {
            mListener.onPIPPictureTaken(jpegData);
        }
    }

    @Override
    public void unlockNextCapture() {
        Log.d(TAG, "canDoStartPreview");
        if (mListener != null) {
            mListener.unlockNextCapture();
        }
    }

    /**
     * initialize pip wrapping renderer, pip GL thread will be created here.
     * <p>
     * when GL thread is already exist, will not create it again.
     *
     * @param rendererCallback the renderer callback.
     */
    public void initPIPRenderer(RendererManager.RendererCallback rendererCallback) {
        Log.i(TAG, "initPIPRenderer");
        if (mRendererManager == null) {
            mRendererManager = new RendererManager(rendererCallback, mActivity);
        }
        mRendererManager.init();
    }

    /**
     * update pip template resource.
     * Note: This function must be called before setUpSurfaceTextures.
     * @param backResourceId bottom graphic template
     * @param frontResourceId top graphic template
     * @param effectFrontHighlightId top graphic highlight template
     * @param editButtonResourceId top graphic edit template
     */
    public void updateEffectTemplates(int backResourceId, int frontResourceId,
            int effectFrontHighlightId, int editButtonResourceId) {
        Log.d(TAG, "updateEffectTemplates");
        mRendererManager.updateEffectTemplates(backResourceId, frontResourceId,
                effectFrontHighlightId, editButtonResourceId);
    }

    /**
     * when exit pip mode, should call this function to recycle resources.
     */
    public void unInitPIPRenderer() {
        mRendererManager.unInit();
    }

    /**
     * Set the bottom/top texture size.
     * Note: This function must be called before setUpSurfaceTextures.
     * @param width  preview texture's width
     * @param height preview texture's height
     */
    public void setPreviewTextureSize(int width, int height) {
        Log.i(TAG, "setTextureSize width = " + width + " height = " + height);
        mRendererManager.setPreviewSize(width, height);
    }

    /**
     * create two surface textures, switch pip by needSwitchPIP
     * <p>
     * By default, bottom surface texture is drawn in bottom graphic.
     * top surface texture is drawn in top graphic.
     */
    public void setUpSurfaceTextures() {
        Log.i(TAG, "setUpSurfaceTextures");
        mRendererManager.setUpSurfaceTextures();
    }

    /**
     * Set a surface to receive pip preview buffer from pip wrapping.
     * @param surface used to receive pip preview buffer
     */
    public void setPreviewSurface(Surface surface) {
        Log.i(TAG, "setPreviewSurface surface = " + surface);
        mRendererManager.setPreviewSurfaceSync(surface);
    }

    /**
     * keep capture rotation
     */
    @Override
    public void keepCaptureRotation() {
        mRendererManager.keepCaptureRotation();
    }

    /**
     * Check whether is during switch pip.
     * @return whether is during switch pip.
     */
    public boolean isDuringSwitchPip() {
        return mRendererManager.isDuringSwitchPip();
    }

    /**
     * notify surface view has been destroyed.
     * @param surface the destroyed surface.
     */
    public void notifySurfaceViewDestroyed(Surface surface) {
        Log.i(TAG, "notifySurfaceViewDestroyed");
        mRendererManager.notifySurfaceViewDestroyed(surface);
    }

    /**
     * This surface texture is used to receive bottom camera device's preview buffer.
     * <p>
     * It will update preview buffer to pip GL thread for processing when onFrameAvailable.
     *
     * @return pip bottom surface texture
     */
    public SurfaceTextureWrapper getBottomSurfaceTextureWrapper() {
        return mRendererManager.getBottomPvSt();
    }

    /**
     * This surface texture is used to receive top camera device's preview buffer.
     * <p>
     * It will update preview buffer to pip GL thread for processing when onFrameAvailable.
     *
     * @return pip top surface texture
     */
    public SurfaceTextureWrapper getTopSurfaceTextureWrapper() {
        return mRendererManager.getTopPvSt();
    }

    /**
     * update top graphic's position.
     *
     * @param topGraphic is an instance of AnimationRect.
     */
    public void updateTopGraphic(AnimationRect topGraphic) {
        mRendererManager.updateTopGraphic(topGraphic);
    }

    /**
     * when G-sensor's orientation changed, should update it to PipOperator.
     *
     * @param newOrientation G-sensor's new orientation
     */
    public void updateGSensorOrientation(int newOrientation) {
        mRendererManager.updateGSensorOrientation(newOrientation);
    }

    /**
     * switch pip.
     */
    public void switchPIP() {
        mRendererManager.switchPip();
    }

    /**
     * Prepare recording renderer, will new a recording thread.
     * <p>
     * Note: before prepareRecording, the recording surface must be set.
     */
    public void prepareRecording() {
        Log.i(TAG, "prepareRecording");
        mRendererManager.prepareRecordSync();
    }

    /**
     * Set a recording surface to receive pip buffer from pip wrapping.
     * @param surface a recording surface used to receive pip buffer.
     * @param orientation recorder orientation
     */
    public void setRecordingSurface(Surface surface, int orientation) {
        Log.i(TAG, "setRecordingSurface surface = " + surface);
        mRendererManager.setRecordSurfaceSync(surface, orientation);
    }

    /**
     * Begin to push pip frame to video recording surface.
     */
    public void startPushVideoBuffer() {
        Log.i(TAG, "startPushVideoBuffer");
        mRendererManager.startRecordSync();
    }

    /**
     * Stop to push pip frame to video recording surface.
     */
    public void stopPushVideoBuffer() {
        Log.i(TAG, "stopPushVideoBuffer");
        mRendererManager.stopRecordSync();
    }

    /**
     * Take a video snap shot by orientation.
     *
     * @param orientation video snap shot orientation
     * @param isBackBottom is back camera.
     */
    public void takeVideoSnapshot(int orientation, boolean isBackBottom) {
        Log.i(TAG, "takeVideoSnapshot orientation = " + orientation);
        boolean isLandscape = (orientation % 180 != 0);
        int max = Math.max(mRendererManager.getPreviewTextureHeight(),
                mRendererManager.getPreviewTextureWidth());
        int min = Math.min(mRendererManager.getPreviewTextureHeight(),
                mRendererManager.getPreviewTextureWidth());
        int width = isLandscape ? max : min;
        int height = isLandscape ? min : max;
        mRendererManager.takeVideoSnapShot(orientation, getVssSurface(width, height));
    }

    /**
     * Get vss surface.
     * @param width the width.
     * @param height the height.
     * @return an surface to receive vss buffer.
     */
    public Surface getVssSurface(int width, int height) {
        mVssJpegEncoder = JpegEncoder.newInstance(mActivity, false);
        Surface jpegInputSurface = mVssJpegEncoder.configInputSurface(mVssJpegCallback,
                width, height, PixelFormat.RGBA_8888);
        mVssJpegEncoder.startEncodeAndReleaseWhenDown();
        return jpegInputSurface;
    }

    /**
     * After camera stop preview.
     */
    public void afterStopPreview() {
        mRendererManager.afterStopPreview();
    }

    private JpegEncoder.JpegCallback mVssJpegCallback = new JpegEncoder.JpegCallback() {
        @Override
        public void onJpegAvailable(byte[] jpegData) {
            if (mListener != null) {
                mListener.onPIPPictureTaken(jpegData);
            }
        }
    };

    /**
     * Pip customization constants.
     */
    public static class PIPCustomization {
        private static final String TAG = "PIPCustomization";
        public static final String MAIN_CAMERA = "main_camera";
        public static final String SUB_CAMERA = "sub_camera";
        // scale
        public static final float TOP_GRAPHIC_MAX_SCALE_VALUE = 1.4f;
        public static final float TOP_GRAPHIC_MIN_SCALE_VALUE = 0.6f;
        // rotate
        public static final float TOP_GRAPHIC_MAX_ROTATE_VALUE = 180f;
        // top graphic edge, default is min(width, height) / 2
        public static final float TOP_GRAPHIC_DEFAULT_EDGE_RELATIVE_VALUE = 1f / 2;
        // edit button edge, default is min(width, height) / 10
        public static final int TOP_GRAPHIC_EDIT_BUTTON_RELATIVE_VALUE = 10;
        public static final float TOP_GRAPHIC_LEFT_TOP_RELATIVE_VALUE =
                TOP_GRAPHIC_DEFAULT_EDGE_RELATIVE_VALUE - 1f /
                    TOP_GRAPHIC_EDIT_BUTTON_RELATIVE_VALUE;
        // top graphic crop preview position
        public static final float TOP_GRAPHIC_CROP_RELATIVE_POSITION_VALUE = 3f / 4;
        // which camera enable FD, default is main camera support fd
        public static final String ENABLE_FACE_DETECTION = MAIN_CAMERA;
        // when take picture, whether sub camera need mirror
        public static final boolean SUB_CAMERA_NEED_HORIZONTAL_FLIP = true;
        private PIPCustomization() {
        }

        /**
         * Whether main camera in bottom support FD.
         *
         * @return Whether main camera in bottom support FD.
         */
        public static boolean isMainCameraEnableFD() {
            boolean enable = false;
            enable = ENABLE_FACE_DETECTION.endsWith(MAIN_CAMERA);
            Log.d(TAG, "isMainCameraEnableFD enable = " + enable);
            return enable;
        }
    }
}