/*
 *   Copyright Statement:
 *
 *     This software/firmware and related documentation ("MediaTek Software") are
 *     protected under relevant copyright laws. The information contained herein is
 *     confidential and proprietary to MediaTek Inc. and/or its licensor. Without
 *     the prior written permission of MediaTek inc. and/or its licensor, any
 *     reproduction, modification, use or disclosure of MediaTek Software, and
 *     information contained herein, in whole or in part, shall be strictly
 *     prohibited.
 *
 *     MediaTek Inc. (C) 2016. All rights reserved.
 *
 *     BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *    THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 *     RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 *     ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 *     WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 *     WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 *     NON-INFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 *     RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 *     TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 *     RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 *     OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 *     SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 *     RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 *     STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 *     ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 *     RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 *     MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 *     CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     The following software/firmware and/or related documentation ("MediaTek
 *     Software") have been modified by MediaTek Inc. All revisions are subject to
 *     any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.feature.mode.pip.device.v2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory.CameraApi;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.sound.ISoundPlayback;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice;
import com.mediatek.camera.feature.mode.pip.device.PipCaptureExecutor;
import com.mediatek.camera.feature.mode.pip.pipwrapping.IPipCaptureWrapper;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;

import java.util.ArrayList;

import javax.annotation.Nonnull;

/**
 * Pip device controller with API2.
 */
public class PipDevice2 implements IPipDevice {
    private static final Tag TAG = new Tag(PipDevice2.class.getSimpleName());
    private final Activity mActivity;
    private final ICameraContext mCameraContext;
    private final CameraDeviceManager mCameraDeviceManager;
    private final PipDevice2Controller mFirstDeviceController;
    private final PipDevice2Controller mSecondDeviceController;
    private PipOpenCameraAsyncTask mPipOpenCameraTask;
    private PipCaptureExecutor mPipCaptureExecutor;
    private ImageReader mFirstImageReader;
    private ImageReader mSecondImageReader;
    /**
     * Construct an instance of {@link IPipDevice}.
     *
     * @param app the camera app.
     * @param context the camera context.
     * @param pipCaptureWrapper pip capture wrapper.
     */
    public PipDevice2(@Nonnull IApp app,
                     @Nonnull ICameraContext context,
                     @Nonnull IPipCaptureWrapper pipCaptureWrapper) {
        mActivity = app.getActivity();
        mCameraContext = context;
        mCameraDeviceManager = context.getDeviceManager(CameraApi.API2);
        mFirstDeviceController = new PipDevice2Controller(app, mCameraDeviceManager);
        mSecondDeviceController = new PipDevice2Controller(app, mCameraDeviceManager);
        mPipOpenCameraTask = new PipOpenCameraAsyncTask();
        mPipCaptureExecutor = new PipCaptureExecutor(mActivity, pipCaptureWrapper);
    }

    @Override
    public void openCamera(@Nonnull ISettingManager firstSettingManager,
                           @Nonnull ISettingManager secondSettingManager) {
        if (mPipOpenCameraTask.isCancelled()) {
            mPipOpenCameraTask = new PipOpenCameraAsyncTask();
        }
        if (AsyncTask.Status.PENDING != mPipOpenCameraTask.getStatus()) {
            LogHelper.i(TAG, "Skip open camera, because camera is opening or opened.");
            return;
        }
        mPipCaptureExecutor.init();
        mPipOpenCameraTask.execute(firstSettingManager, secondSettingManager);
    }

    @Override
    public void updateModeType(@Nonnull ICameraMode.ModeType modeType) {
        mFirstDeviceController.updateModeType(modeType);
        mSecondDeviceController.updateModeType(modeType);
    }

    @Override
    public void setPipDeviceCallback(@Nonnull IPipDeviceCallback pipDeviceCallback) {
        mFirstDeviceController.setPipDeviceCallback(pipDeviceCallback);
        mSecondDeviceController.setPipDeviceCallback(pipDeviceCallback);
    }

    @Override
    public ArrayList<Size> getSupportedPreviewSizes(Object previewTarget,
                                                    @Nonnull String cameraId) {
        if (cameraId.equals(mFirstDeviceController.getCameraId())) {
            return mFirstDeviceController.getSupportedPreviewSizes(previewTarget);
        } else {
            return mSecondDeviceController.getSupportedPreviewSizes(previewTarget);
        }
    }

    @Override
    public void closeCamera(@NonNull String cameraId) {
        mPipOpenCameraTask.cancel(false);
        mPipOpenCameraTask.blockUntilCameraOpened();
        mPipCaptureExecutor.unInit();
        if (cameraId.equals(mFirstDeviceController.getCameraId())) {
            mFirstDeviceController.closeCameraAsync();
            LogHelper.i(TAG, "[closeCamera]- id: " + cameraId);
            return;
        }
        if (cameraId.equals(mSecondDeviceController.getCameraId())) {
            mSecondDeviceController.closeCameraAsync();
            LogHelper.i(TAG, "[closeCamera]- id: " + cameraId);
            return;
        }
    }

    @Override
    public void startPreview(@NonNull SurfaceTextureWrapper firstPreviewSurface,
                             @NonNull SurfaceTextureWrapper secondPreviewSurface) {
        LogHelper.i(TAG, "[startPreview] bottom:" + firstPreviewSurface +
                ", top:" + secondPreviewSurface);
        mFirstDeviceController.startPreview(firstPreviewSurface);
        mSecondDeviceController.startPreview(secondPreviewSurface);
    }

    @Override
    public void stopPreview() {
        mFirstDeviceController.stopPreviewAsync();
        mSecondDeviceController.stopPreviewAsync();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void takePicture(@NonNull Size bottomPictureSize, @NonNull Size topPictureSize) {
        mPipCaptureExecutor.setUpCapture(bottomPictureSize, topPictureSize);
        Size firstCamSize =
                mFirstDeviceController.isBottomCamera() ?
                        bottomPictureSize :
                        topPictureSize;
        Size secondCamSize =
                mFirstDeviceController.isBottomCamera() ?
                        topPictureSize :
                        bottomPictureSize;
        Handler firstHandler =
                mFirstDeviceController.isBottomCamera() ?
                        mFirstDeviceController.getRespondHandler() :
                        mSecondDeviceController.getRespondHandler();
        Handler secondHandler =
                mFirstDeviceController.isBottomCamera() ?
                        mSecondDeviceController.getRespondHandler() :
                        mFirstDeviceController.getRespondHandler();
        setUpImageReader(true, firstHandler, firstCamSize);
        setUpImageReader(false, secondHandler, secondCamSize);
        mFirstDeviceController.takePictureAsync(mFirstImageReader.getSurface());
        mSecondDeviceController.takePictureAsync(mSecondImageReader.getSurface());
    }

    @Override
    public boolean isReadyForCapture() {
        return mFirstDeviceController.isReadyForCapture() &&
                mSecondDeviceController.isReadyForCapture();
    }

    @Override
    public void requestChangeSettingValue(@Nonnull String cameraId) {
        if (cameraId.equals(mFirstDeviceController.getCameraId())) {
            mFirstDeviceController.createAndChangeRepeatingRequest();
            return;
        }
        mSecondDeviceController.createAndChangeRepeatingRequest();
    }

    @Override
    public void updateBottomCameraId(@Nonnull String bottomCameraId) {
        mFirstDeviceController.updateBottomCameraId(bottomCameraId);
        mSecondDeviceController.updateBottomCameraId(bottomCameraId);
    }

    @Override
    public void release() {
        mFirstDeviceController.release();
        mSecondDeviceController.release();
    }

    /**
     * An AsyncTask used to open camera in background thread.
     */
    private class PipOpenCameraAsyncTask extends AsyncTask<ISettingManager, Void, Void> {
        private final ConditionVariable mOpenCameraCondition = new ConditionVariable();
        /**
         * Create pip open camera async task.
         */
        PipOpenCameraAsyncTask() {
            // close camera too quickly when this task is not started, we do not do open camera.
            mOpenCameraCondition.open();
        }

        /**
         * When camera is opening, block until open done.
         * if don't execute doInBackground, can't block,
         * Because don't execute condition close
         */
        public void blockUntilCameraOpened() {
            mOpenCameraCondition.block();
        }

        @Override
        protected void onPreExecute() {
            // mOpenCameraCondition.close();
        }

        @Override
        protected Void doInBackground(ISettingManager... settingManagers) {
            mOpenCameraCondition.close();
            LogHelper.i(TAG, "<doInBackground> open camera +");
            IPerformanceProfile pProfile = PerformanceTracker.create(TAG, "openCamera").start();
            if (!isCancelled()) {
                mFirstDeviceController.openCamera(settingManagers[0]);
            }
            pProfile.mark("open first camera done.");
            if (!isCancelled()) {
                mSecondDeviceController.openCamera(settingManagers[1]);
            }
            pProfile.mark("open second camera done.");
            mOpenCameraCondition.open();
            pProfile.stop();
            LogHelper.i(TAG, "<doInBackground> open camera -");
            return null;
        }
    }

    private void setUpImageReader(boolean isFirst, Handler handler, Size size) {
        int width = Math.max(size.getWidth(), size.getHeight());
        int height = Math.min(size.getWidth(), size.getHeight());
        ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(
                isFirst ? mFirstImageAvailableListener : mSecondImageAvailableListener, handler);
        if (isFirst) {
            mFirstImageReader = imageReader;
        } else {
            mSecondImageReader = imageReader;
        }
    }

    private void releaseImageReader(boolean isFirst) {
        if (isFirst) {
            mFirstImageReader.close();
        } else {
            mSecondImageReader.close();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private OnImageAvailableListener mFirstImageAvailableListener =
            new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            LogHelper.i(TAG, "[onImageAvailable] first camera, is bottom:"
                    + mFirstDeviceController.isBottomCamera());
            mCameraContext.getSoundPlayback().play(ISoundPlayback.SHUTTER_CLICK);
            Image image = reader.acquireNextImage();
            byte[] jpegData = CameraUtil.acquireJpegBytesAndClose(image);
            mPipCaptureExecutor.offerJpegData(jpegData, mFirstDeviceController.isBottomCamera());
            releaseImageReader(true);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private OnImageAvailableListener mSecondImageAvailableListener =
            new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            LogHelper.i(TAG, "[onImageAvailable] second camera, is bottom:"
                    + mSecondDeviceController.isBottomCamera());
            Image image = reader.acquireNextImage();
            byte[] jpegData = CameraUtil.acquireJpegBytesAndClose(image);
            mPipCaptureExecutor.offerJpegData(jpegData, mSecondDeviceController.isBottomCamera());
            releaseImageReader(false);
        }
    };
}