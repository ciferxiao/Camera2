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

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.support.annotation.NonNull;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.ISettingManager.SettingController;
import com.mediatek.camera.common.setting.ISettingManager.SettingDeviceConfigurator;
import com.mediatek.camera.common.setting.ISettingManager.SettingDeviceRequester;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice.IPipDeviceCallback;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;

/**
 * Pip device controller with API1.
 */
public class PipDeviceController implements SettingDeviceRequester {
    private Tag mTag = new Tag("Pip-DeviceCtrl");
    private final IApp mApp;
    private final ICameraContext mCameraContext;
    private final CameraDeviceManager mCameraDeviceManager;
    private CopyOnWriteArrayList<Size> mSupportedPreviewSizes = new CopyOnWriteArrayList<>();
    private IPipDeviceCallback mPipDeviceCallback;
    private ISettingManager mSettingManager;
    private SettingDeviceConfigurator mSettingDeviceConfigurator;
    private SettingController mSettingController;
    private DeviceStateCallback mDeviceStateCallback;
    private String mCameraId;
    private String mBottomCameraId;
    private CameraProxy mCameraProxy;
    private SurfaceTextureWrapper mSurfaceTextureWrapper;
    private boolean mPreviewStarted;
    private boolean mForceCloseZsd;
    private Object mDeviceStateSync = new Object();

    /**
     * Construct an instance of {@link PipDeviceController}.
     * @param app the camera app.
     * @param cameraContext the camera context.
     * @param cameraDeviceManager specify CameraDeviceManager used to open camera.
     */
    PipDeviceController(IApp app,
                        ICameraContext cameraContext,
                        CameraDeviceManager cameraDeviceManager) {
        mApp = app;
        mCameraContext = cameraContext;
        mDeviceStateCallback = new DeviceStateCallback();
        mCameraDeviceManager = cameraDeviceManager;
    }

    /**
     * Update current mode type, photo or video.
     * @param modeType current mode type.
     */
    public void updateModeType(@Nonnull ICameraMode.ModeType modeType) {
        if (ICameraMode.ModeType.VIDEO.equals(modeType)) {
            mForceCloseZsd = true;
            return;
        }
        mForceCloseZsd = false;
    }

    /**
     * Set pip device callback.
     *
     * @param pipDeviceCallback pip device callback.
     */
    public void setPipDeviceCallback(IPipDeviceCallback pipDeviceCallback) {
        mPipDeviceCallback = pipDeviceCallback;
    }

    /**
     * Open camera with specified id, this method is a synchronous method.
     *
     * @param settingManager this cameraId related setting manager instance.
     */
    public void openCamera(@Nonnull ISettingManager settingManager) {
        mPreviewStarted = false;
        try {
            mCameraId = settingManager.getSettingController().getCameraId();
            mTag = new Tag("Pip-DeviceCtrl-" + mCameraId);
            mSettingManager = settingManager;
            settingManager.updateModeDeviceRequester(this);
            mSettingDeviceConfigurator = settingManager.getSettingDeviceConfigurator();
            mSettingController = settingManager.getSettingController();
            mCameraDeviceManager.openCameraSync(mCameraId, mDeviceStateCallback, null);
        } catch (CameraOpenException e) {
            e.printStackTrace();
        }
    }

    /**
     * This will be called when initialize and switch pip.
     *
     * @param bottomCameraId current bottom camera id.
     */
    public void updateBottomCameraId(@Nonnull String bottomCameraId) {
        synchronized (mDeviceStateSync) {
            if (mBottomCameraId == null) {
                mBottomCameraId = bottomCameraId;
                return;
            }
            if (!mBottomCameraId.equals(bottomCameraId)) {
                mBottomCameraId = bottomCameraId;
                updateSettingUiVisibility();
                doConfigSettingValue(false, mSurfaceTextureWrapper);
            }
        }
    }

    /**
     * Get supported preview sizes.
     *
     * @param previewTarget SurfaceTexture or SurfaceHolder instance.
     * @return the supported preview size list.
     */
    public ArrayList<Size> getSupportedPreviewSizes(Object previewTarget) {
        if (mSupportedPreviewSizes.size() > 0) {
            return new ArrayList<>(mSupportedPreviewSizes);
        }
        synchronized (mDeviceStateSync) {
            if (mCameraProxy != null) {
                List<Camera.Size> originalPreviewSizes =
                        mCameraProxy.getOriginalParameters(false).getSupportedPreviewSizes();
                for (Camera.Size size : originalPreviewSizes) {
                    mSupportedPreviewSizes.add(new Size(size.width, size.height));
                }
            }
        }
        return new ArrayList<>(mSupportedPreviewSizes);
    }

    /**
     * close current camera async.
     */
    public void closeCameraAsync() {
        LogHelper.d(mTag, "[closeCameraAsync]");
        synchronized (mDeviceStateSync) {
            if (mCameraProxy != null && mCameraId != null) {
                mCameraDeviceManager.recycle(mCameraId);
                mCameraProxy.closeAsync();
                mCameraProxy = null;
                mSurfaceTextureWrapper = null;
            }
        }
    }

    /**
     * close current camera sync.
     */
    public void closeCamera() {
        LogHelper.d(mTag, "[closeCamera]");
        synchronized (mDeviceStateSync) {
            if (mCameraProxy != null && mCameraId != null) {
                mCameraDeviceManager.recycle(mCameraId);
                mCameraProxy.close();
                mCameraProxy = null;
                mSurfaceTextureWrapper = null;
            }
        }
    }

    /**
     * Start preview with specified SurfaceTexture, this method is synchronous method.
     *
     * @param previewSurface the specified SurfaceTextureWrapper.
     * @param forceUpdate force update preview surface texture, this need when do start preview
     *                    when camera is during opening.
     */
    public void startPreview(@NonNull SurfaceTextureWrapper previewSurface) {
        synchronized (mDeviceStateSync) {
            mSurfaceTextureWrapper = previewSurface;
            if (mCameraProxy != null) {
                doConfigSettingValue(true, mSurfaceTextureWrapper);
            }
        }
    }

    /**
     * Stop preview.
     */
    public void stopPreviewAsync() {
        if (!mPreviewStarted) {
            return;
        }
        doStopPreview();
        mSurfaceTextureWrapper = null;
    }

    /**
     * Check is ready (preview is started) for capture or not.
     * @return true, preview started for capture, or false.
     */
    public boolean isReadyForCapture() {
        return mPreviewStarted;
    }

    /**
     * Take picture with jpeg callback.
     *
     * @param jpegCallback the picture callback used to receive jpeg data.
     */
    public void takePictureAsync(@NonNull Camera.PictureCallback jpegCallback) {
        synchronized (mDeviceStateSync) {
            if (mCameraProxy != null) {
                doConfigSettingValue(false, mSurfaceTextureWrapper);
                mCameraProxy.takePictureAsync(
                        mBottomCameraId.equals(mCameraId) ? mShutterCallback : null,
                        mRawPictureCallback,
                        mPostViewPictureCallback,
                        jpegCallback);
                mPreviewStarted = false;
            }
        }
    }

    /**
     * Get current camera id.
     *
     * @return the camera id.
     */
    public String getCameraId() {
        return mCameraId;
    }

    /**
     * This device controller controls bottom camera.
     * @return whether is bottom camera.
     */
    public boolean isBottomCamera() {
        if (mBottomCameraId == null) {
            return false;
        }
        return mBottomCameraId.equals(mCameraId);
    }

    @Override
    public void requestChangeSettingValue(String key) {
        synchronized (mDeviceStateSync) {
            LogHelper.d(mTag, "[requestChangeSettingValue] request key:" + key);
            doConfigSettingValue(false, mSurfaceTextureWrapper);
        }
    }

    @Override
    public void requestChangeCommand(String key) {
        synchronized (mDeviceStateSync) {
            if (mCameraProxy != null) {
                mSettingDeviceConfigurator.configCommand(key, mCameraProxy);
            }
        }
    }

    /**
     * Request to change Parameters and just configure it by setting.
     *
     * @param key The key of setting.
     */
    @Override
    public void requestChangeSettingValueJustSelf(String key) {

    }

    @Override
    public void requestChangeCommandImmediately(String key) {
        synchronized (mDeviceStateSync) {
            if (mCameraProxy != null) {
                mSettingDeviceConfigurator.configCommand(key, mCameraProxy);
            }
        }
    }

    private void doConfigSettingValue(boolean forceRestartPreview,
                                      SurfaceTextureWrapper stWrapper) {
        if (mCameraProxy == null || stWrapper == null || stWrapper.getTextureId() < 0) {
            LogHelper.w(mTag, "[doConfigSettingValue]Fail, proxy:" + mCameraProxy +
                                ", surface texture:" + stWrapper);
            return;
        }
        LogHelper.d(mTag, "[doConfigSettingValue]+");
        Parameters parameters = mCameraProxy.getOriginalParameters(true);
        if (parameters == null) {
            LogHelper.e(mTag, "Why parameter is null???");
            return;
        }
        boolean needRestartPreview =
                mSettingDeviceConfigurator.configParameters(parameters) || forceRestartPreview;
        configFrameRate(parameters);
        int width = Math.max(stWrapper.getWidth(),
                stWrapper.getHeight());
        int height = Math.min(stWrapper.getWidth(),
                stWrapper.getHeight());
        parameters.setPreviewSize(width, height);
        parameters.setRotation(
                CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(mCameraId),
                        mApp.getGSensorOrientation(), mApp.getActivity()));
        if (needRestartPreview) {
            doStopPreview();
            mCameraProxy.setParameters(parameters);
            doStartPreview(stWrapper);
        } else {
            mCameraProxy.setParameters(parameters);
        }
        LogHelper.d(mTag, "[doConfigSettingValue]-");
    }

    private void doStartPreview(SurfaceTextureWrapper surfaceTextureWrapper) {
        LogHelper.d(mTag, "[doStartPreview]+ preview started:" + mPreviewStarted);
        if (mPreviewStarted) {
            LogHelper.i(mTag, "[doStartPreview]- ignore start preview.");
            return;
        }
        try {
            mCameraProxy.setDisplayOrientation(CameraUtil.getDisplayOrientation(0,
                    Integer.parseInt(mCameraId)));
            mCameraProxy.setPreviewTexture(surfaceTextureWrapper.getSurfaceTexture());
            mCameraProxy.startPreview();
            mSettingDeviceConfigurator.onPreviewStarted();
            mPreviewStarted = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogHelper.d(mTag, "[doStartPreview]-");
    }

    private void doStopPreview() {
        synchronized (mDeviceStateSync) {
            LogHelper.d(mTag, "[doStopPreview]+ mPreviewStarted:" + mPreviewStarted);
            if (mCameraProxy == null || !mPreviewStarted) {
                LogHelper.w(mTag, "[doStopPreview]- pv status:" + mPreviewStarted);
                return;
            }
            mCameraProxy.stopPreviewAsync();
            mPreviewStarted = false;
            mSettingDeviceConfigurator.onPreviewStopped();
            if (mPipDeviceCallback != null) {
                mPipDeviceCallback.afterStopPreview(mCameraId);
            }
            LogHelper.d(mTag, "[doStopPreview]-");
        }
    }

    private void configFrameRate(Camera.Parameters parameters) {
        if (mForceCloseZsd) {
            parameters.set("zsd-mode", "off");
        }
        configDynamicFrameRate(parameters);
        String zsdValue = parameters.get("zsd-mode");
        String pipFrameRatesStr = parameters.get(
                "on".equals(zsdValue) ? "pip-fps-zsd-on" : "pip-fps-zsd-off");
        List<Integer> pipFrameRateArray = CameraUtil.splitInt(pipFrameRatesStr);
        if (pipFrameRateArray != null && pipFrameRatesStr.length() > 0) {
            Integer maxFrameRate = Collections.max(pipFrameRateArray);
            parameters.setPreviewFrameRate(maxFrameRate);
            LogHelper.d(mTag, "[configFrameRate] frame rate:" + maxFrameRate);
        }
    }

    private void configDynamicFrameRate(Camera.Parameters parameters) {
        String dynamicSupport = parameters.get("dynamic-frame-rate-supported");
        if ("true".equals(dynamicSupport)) {
            parameters.set("dynamic-frame-rate", "false");
        }
    }

    private void updateSettingUiVisibility() {
        if (mBottomCameraId == null || mCameraId == null) {
            return;
        }
        if (mCameraId.equals(mBottomCameraId)) {
            mSettingController.addViewEntry();
            mSettingController.refreshViewEntry();
        } else {
            mSettingController.removeViewEntry();
        }
    }

    private final Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            LogHelper.d(mTag, "[onShutter]");
        }
    };

    private final Camera.PictureCallback mRawPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            LogHelper.d(mTag, "[onPictureTaken] raw picture callback");
        }
    };

    private final Camera.PictureCallback mPostViewPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            LogHelper.d(mTag, "[onPictureTaken] postView picture callback");
        }
    };
    /**
     * An implementation of {@link CameraProxy.StateCallback}.
     */
    private class DeviceStateCallback extends CameraProxy.StateCallback {
        @Override
        public void onOpened(@Nonnull CameraProxy cameraProxy) {
            LogHelper.d(mTag, "[onOpened]+ id:" + cameraProxy.getId());
            synchronized (mDeviceStateSync) {
                mCameraProxy = cameraProxy;
                mCameraContext.getFeatureProvider().updateCameraParameters(mCameraId,
                        mCameraProxy.getParameters());
            }
            mSettingManager.createAllSettings();
            mSettingDeviceConfigurator.setOriginalParameters(
                    cameraProxy.getOriginalParameters(false));
            if (mPipDeviceCallback != null) {
                mPipDeviceCallback.onCameraOpened(mCameraId);
            }
            doConfigSettingValue(true, mSurfaceTextureWrapper);
            updateSettingUiVisibility();
            LogHelper.d(mTag, "[onOpened]-");
        }

        @Override
        public void onClosed(@Nonnull CameraProxy cameraProxy) {}

        @Override
        public void onDisconnected(@Nonnull CameraProxy cameraProxy) {
            LogHelper.d(mTag, "[onDisconnected] id:" + cameraProxy.getId());
        }

        @Override
        public void onError(@Nonnull CameraProxy cameraProxy, int error) {
            LogHelper.d(mTag, "[onError] id:" + cameraProxy.getId() + ",error:" + error);
            // mCameraDeviceManager.recycle(mCameraId);
            CameraUtil.showErrorInfoAndFinish(mApp.getActivity(), error);
        }
    }
}