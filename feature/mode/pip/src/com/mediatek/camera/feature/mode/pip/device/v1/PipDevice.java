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

package com.mediatek.camera.feature.mode.pip.device.v1;

import android.app.Activity;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.support.annotation.NonNull;

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
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice;
import com.mediatek.camera.feature.mode.pip.device.PipCaptureExecutor;
import com.mediatek.camera.feature.mode.pip.pipwrapping.IPipCaptureWrapper;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;
import com.mediatek.camera.portability.SystemProperties;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Nonnull;

/**
 * Pip device management implement with API1.
 */
public class PipDevice implements IPipDevice {
    private static final Tag TAG = new Tag(PipDevice.class.getSimpleName());
    private final Activity mActivity;
    private final CameraDeviceManager mCameraDeviceManager;
    private final PipDeviceController mFirstDeviceController;
    private final PipDeviceController mSecondDeviceController;
    private PipOpenCameraAsyncTask mPipOpenCameraTask;
    private PipCaptureExecutor mPipCaptureExecutor;
    private static int sIsSaveRawJpegEnable = SystemProperties.getInt(
            "camera.pip.save.raw.jpeg.enable", 0);

    /**
     * Construct an instance of {@link IPipDevice}.
     *
     * @param app the camera app.
     * @param context the camera context.
     * @param pipCaptureWrapper pip capture wrapper.
     */
    public PipDevice(@Nonnull IApp app,
                     @Nonnull ICameraContext context,
                     @Nonnull IPipCaptureWrapper pipCaptureWrapper) {
        mActivity = app.getActivity();

        mCameraDeviceManager = context.getDeviceManager(CameraApi.API1);

        mFirstDeviceController = new PipDeviceController(app, context, mCameraDeviceManager);
        mSecondDeviceController = new PipDeviceController(app, context, mCameraDeviceManager);
        mPipOpenCameraTask = new PipOpenCameraAsyncTask();
        mPipCaptureExecutor = new PipCaptureExecutor(app.getActivity(), pipCaptureWrapper);
        // mPipCaptureExecutor.init();
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
    public void closeCamera(@Nonnull String cameraId) {
        mPipOpenCameraTask.cancel(false);
        mPipOpenCameraTask.blockUntilCameraOpened();
        mPipCaptureExecutor.unInit();
        if (cameraId.equals(mSecondDeviceController.getCameraId())) {
            mSecondDeviceController.closeCamera();
            LogHelper.i(TAG, "[closeCamera]- id: " + cameraId);
            return;
        }
        if (cameraId.equals(mFirstDeviceController.getCameraId())) {
            mFirstDeviceController.closeCameraAsync();
            LogHelper.i(TAG, "[closeCamera]- id: " + cameraId);
            return;
        }
    }

    @Override
    public void startPreview(@Nonnull SurfaceTextureWrapper firstPreviewSurface,
                             @Nonnull SurfaceTextureWrapper secondPreviewSurface) {
        LogHelper.d(TAG, "[startPreview] bottom:" + firstPreviewSurface +
                                         ", top:" + secondPreviewSurface);
        mFirstDeviceController.startPreview(firstPreviewSurface);
        mSecondDeviceController.startPreview(secondPreviewSurface);
    }

    @Override
    public void stopPreview() {
        mFirstDeviceController.stopPreviewAsync();
        mSecondDeviceController.stopPreviewAsync();
    }

    @Override
    public void takePicture(@NonNull Size bottomPictureSize, @NonNull Size topPictureSize) {
        mPipCaptureExecutor.setUpCapture(bottomPictureSize, topPictureSize);
        if (mFirstDeviceController.isBottomCamera()) {
            mFirstDeviceController.takePictureAsync(mBottomJpegPictureCallback);
            mSecondDeviceController.takePictureAsync(mTopJpegPictureCallback);
        } else {
            mFirstDeviceController.takePictureAsync(mTopJpegPictureCallback);
            mSecondDeviceController.takePictureAsync(mBottomJpegPictureCallback);
        }
    }

    @Override
    public boolean isReadyForCapture() {
        return mFirstDeviceController.isReadyForCapture()
                && mSecondDeviceController.isReadyForCapture();
    }

    @Override
    public void requestChangeSettingValue(@Nonnull String cameraId) {
        if (cameraId.equals(mFirstDeviceController.getCameraId())) {
            mFirstDeviceController.requestChangeSettingValue(null);
            return;
        }
        mSecondDeviceController.requestChangeSettingValue(null);
    }

    @Override
    public void updateBottomCameraId(@Nonnull String bottomCameraId) {
        mFirstDeviceController.updateBottomCameraId(bottomCameraId);
        mSecondDeviceController.updateBottomCameraId(bottomCameraId);
    }

    @Override
    public void release() {
        mPipCaptureExecutor.unInit();
    }

    private void saveJpeg(byte[] jpegData, String path) {
        LogHelper.d(TAG, "[saveJpeg]path = " + path);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(jpegData);
            out.close();
        } catch (IOException e) {
            LogHelper.e(TAG, "[saveJpeg]Failed to write image,exception:", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LogHelper.e(TAG, "[saveJpeg], io exception:", e);
                }
            }
        }
    }

     private final Camera.PictureCallback mBottomJpegPictureCallback =
             new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            if (sIsSaveRawJpegEnable > 0) {
                saveJpeg(bytes, "/sdcard/pip-first-raw.jpg");
            }
            mPipCaptureExecutor.offerJpegData(bytes, true);
        }
    };

    private final Camera.PictureCallback mTopJpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            if (sIsSaveRawJpegEnable > 0) {
                saveJpeg(bytes, "/sdcard/pip-second-raw.jpg");
            }
            mPipCaptureExecutor.offerJpegData(bytes, false);
        }
    };

    /**
     * An AsyncTask used to open camera in background thread.
     */
    private class PipOpenCameraAsyncTask extends AsyncTask<ISettingManager, Void, Void> {
        private final ConditionVariable mOpenCameraCondition = new ConditionVariable();

        /**
         * Create a pip open camera task.
         */
        PipOpenCameraAsyncTask() {
            mOpenCameraCondition.open();
        }

        /**
         * When camera is opening, block until open done.
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
            return null;
        }
    }
}