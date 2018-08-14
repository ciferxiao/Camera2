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
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.ISettingManager.SettingController;
import com.mediatek.camera.common.setting.ISettingManager.SettingDevice2Configurator;
import com.mediatek.camera.common.setting.ISettingManager.SettingDevice2Requester;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice.IPipDeviceCallback;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;
import com.mediatek.camera.portability.MMSdkConfig;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Pip device controller with API2.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PipDevice2Controller implements SettingDevice2Requester {
    private Tag mTag = new Tag("Pip-Device2Ctrl");
    private final IApp mApp;
    private final CameraManager mCameraManager;
    private final CameraDeviceManager mCameraDeviceManager;
    private String mCameraId;
    private int mCurrentRepeatingTemplate = Camera2Proxy.TEMPLATE_PREVIEW;
    private IPipDeviceCallback mPipDeviceCallback;
    private CameraCharacteristics mCameraCharacteristics;
    private SettingDevice2Configurator mSettingDeviceConfigurator;
    private SettingController mSettingController;
    private Device2StateCallback mDeviceStateCallback;
    private SessionStateCallback mSessionStateCallback;
    private RepeatingCaptureCallbackImpl mRepeatingCaptureCallback;
    private String mBottomCameraId;
    private final Object mDeviceStateSync = new Object();
    private Camera2Proxy mCamera2Proxy;
    private volatile  Camera2CaptureSessionProxy mCamera2CaptureSessionProxy;
    private SurfaceTextureWrapper mPreviewSurfaceTextureWrapper;
    private ArrayList<Size> mSupportedPreviewSizes = new ArrayList<>();
    private List<Surface> mSessionSurfaceList = new ArrayList<>();
    private boolean mIsProcessCapture;
    private HandlerThread mSessionHandlerThread;
    private Handler mSessionHandler;

    /**
     * Construct an instance of {@link PipDevice2Controller}.
     *
     * @param app the camera app.
     * @param cameraDeviceManager specify CameraDeviceManager used to open camera.
     */
    public PipDevice2Controller(IApp app, CameraDeviceManager cameraDeviceManager) {
        mApp = app;
        mCameraDeviceManager = cameraDeviceManager;
        mCameraManager =
                (CameraManager) mApp.getActivity().getSystemService(Context.CAMERA_SERVICE);
        mDeviceStateCallback = new Device2StateCallback();
        mSessionStateCallback = new SessionStateCallback();
        mRepeatingCaptureCallback = new RepeatingCaptureCallbackImpl();
        prepareSessionHandler();
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
     * Update current mode type, photo or video.
     * @param modeType current mode type.
     */
    public void updateModeType(@Nonnull ICameraMode.ModeType modeType) {
        if (ICameraMode.ModeType.PHOTO.equals(modeType)) {
            mCurrentRepeatingTemplate = Camera2Proxy.TEMPLATE_PREVIEW;
            return;
        }
        if (ICameraMode.ModeType.VIDEO.equals(modeType)) {
            mCurrentRepeatingTemplate = Camera2Proxy.TEMPLATE_RECORD;
            return;
        }
    }

    /**
     * Open camera with setting manager.
     * @param settingManager the setting manager instance.
     */
    public void openCamera(@Nonnull ISettingManager settingManager) {
        mCameraId = settingManager.getSettingController().getCameraId();
        mTag = new Tag("Pip-Device2Ctrl-" + mCameraId);
        LogHelper.i(mTag, "[openCamera]");
        try {
            mSettingController = settingManager.getSettingController();
            settingManager.updateModeDevice2Requester(this);
            mSettingDeviceConfigurator = settingManager.getSettingDevice2Configurator();
            mCameraCharacteristics =
                    mCameraManager.getCameraCharacteristics(mCameraId);
            settingManager.createAllSettings();
            mSettingDeviceConfigurator.setCameraCharacteristics(mCameraCharacteristics);
            mCameraDeviceManager.openCameraSync(mCameraId, mDeviceStateCallback, null);
        } catch (CameraOpenException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
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
                configRepeatingRequest();
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
        if (mCameraCharacteristics == null) {
            return null;
        }
        if (mSupportedPreviewSizes.size() > 0) {
            return mSupportedPreviewSizes;
        }
        StreamConfigurationMap s = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        android.util.Size[] originalSizes = s.getOutputSizes(SurfaceHolder.class);
        mSupportedPreviewSizes = new ArrayList<>(originalSizes.length);
        for (android.util.Size size : originalSizes) {
            mSupportedPreviewSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
        return mSupportedPreviewSizes;
    }

    /**
     * close current camera.
     */
    public void closeCameraAsync() {
        LogHelper.i(mTag, "[closeCamera]");
        synchronized (mDeviceStateSync) {
            if (mCamera2Proxy != null && mCameraId != null) {
                mCameraDeviceManager.recycle(mCameraId);
                mCamera2Proxy.closeAsync();
                mCamera2CaptureSessionProxy = null;
                mPreviewSurfaceTextureWrapper = null;
                mCamera2Proxy = null;
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
        LogHelper.i(mTag, "[startPreview] previewSurface:" + previewSurface);
        synchronized (mDeviceStateSync) {
            mPreviewSurfaceTextureWrapper = previewSurface;
            if (mCamera2Proxy != null) {
                createCaptureSession(null);
            }
        }
    }

    /**
     * Stop preview.
     */
    public void stopPreviewAsync() {
        synchronized (mDeviceStateSync) {
            LogHelper.i(mTag, "[stopPreview]");
            if (mCamera2Proxy == null || mCamera2CaptureSessionProxy == null) {
                return;
            }
            abortCaptureSession();
        }
    }

    /**
     * Take picture with jpeg surface..
     *
     * @param captureSurface capture surface.
     */
    public void takePictureAsync(Surface captureSurface) {
        LogHelper.i(mTag, "[takePicture]");
        synchronized (mDeviceStateSync) {
            if (mCamera2Proxy == null) {
                return;
            }
            abortCaptureSession();
            createCaptureSession(captureSurface);
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

    /**
     * Get the respond handler.
     * @return the respond handler.
     */
    public Handler getRespondHandler() {
        synchronized (mDeviceStateSync) {
            if (mCamera2Proxy == null) {
                return null;
            }
            return mCamera2Proxy.getRespondHandler();
        }
    }

    /**
     * release controller context.
     */
    public void release() {
        if (mSessionHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mSessionHandler.getLooper().quitSafely();
            } else {
                mSessionHandler.getLooper().quit();
            }
        }
    }

    /**
     * whether can can take picture in current status.
     * @return ture or false
     */
    public boolean isReadyForCapture() {
        return mCamera2CaptureSessionProxy != null;
    }

    @Override
    public void createAndChangeRepeatingRequest() {
        synchronized (mDeviceStateSync) {
            LogHelper.d(mTag, "[createAndChangeRepeatingRequest]");
            configRepeatingRequest();
        }
    }

    @Override
    public CaptureRequest.Builder createAndConfigRequest(int templateType) {
        synchronized (mDeviceStateSync) {
            LogHelper.i(mTag, "[createAndConfigRequest]");
            if (mCamera2Proxy == null) {
                return null;
            }
            CaptureRequest.Builder builder = null;
            try {
                builder = mCamera2Proxy.createCaptureRequest(templateType);
                mSettingDeviceConfigurator.configCaptureRequest(builder);
                for (Surface surface: mSessionSurfaceList) {
                    builder.addTarget(surface);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            return builder;
        }
    }

    @Override
    public Camera2CaptureSessionProxy getCurrentCaptureSession() {
        synchronized (mDeviceStateCallback) {
            return mCamera2CaptureSessionProxy;
        }
    }

    @Override
    public void requestRestartSession() {
        LogHelper.i(mTag, "[requestRestartSession]");
        synchronized (mDeviceStateSync) {
            abortCaptureSession();
            createCaptureSession(null);
        }
    }

    @Override
    public int getRepeatingTemplateType() {
        return mCurrentRepeatingTemplate;
    }

    private void prepareSessionHandler() {
        mSessionHandlerThread = new HandlerThread("camera_session");
        mSessionHandlerThread.start();
        mSessionHandler = new Handler(mSessionHandlerThread.getLooper());
    }

    private void configStillCaptureRequest() {
        if (mCamera2CaptureSessionProxy == null || mCamera2Proxy == null) {
            LogHelper.i(mTag, "[configStillCaptureRequest] mCamera2CaptureSessionProxy:" +
                    mCamera2CaptureSessionProxy + ",mCamera2Proxy:" + mCamera2Proxy);
            return;
        }
        LogHelper.i(mTag, "[configStillCaptureRequest]+");
        try {
            CaptureRequest.Builder builder =
                    mCamera2Proxy.createCaptureRequest(Camera2Proxy.TEMPLATE_STILL_CAPTURE);
            mSettingDeviceConfigurator.configCaptureRequest(builder);
            builder.set(CaptureRequest.JPEG_ORIENTATION,
                    CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(mCameraId),
                            mApp.getGSensorOrientation(),
                            mApp.getActivity()));
            for (Surface surface: mSessionSurfaceList) {
                builder.addTarget(surface);
            }
            mCamera2CaptureSessionProxy.capture(builder.build(),
                    new StillCaptureCallbackImpl(), null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(mTag, "[configStillCaptureRequest]-");
    }

    private void configRepeatingRequest() {
        if (mCamera2CaptureSessionProxy == null || mCamera2Proxy == null) {
            LogHelper.i(mTag, "[configRepeatingRequest] mCamera2CaptureSessionProxy:" +
                    mCamera2CaptureSessionProxy + ",mCamera2Proxy:" + mCamera2Proxy);
            return;
        }
        LogHelper.i(mTag, "[configRepeatingRequest]+");
        try {
            CaptureRequest.Builder builder =
                    mCamera2Proxy.createCaptureRequest(mCurrentRepeatingTemplate);
            if (builder == null) {
                return;
            }
            mSettingDeviceConfigurator.configCaptureRequest(builder);
            for (Surface surface: mSessionSurfaceList) {
                builder.addTarget(surface);
            }
            mCamera2CaptureSessionProxy.setRepeatingRequest(builder.build(),
                    mRepeatingCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(mTag, "[configRepeatingRequest]-");
    }

    private void abortCaptureSession() {
        if (mCamera2Proxy == null || mCamera2CaptureSessionProxy == null) {
            LogHelper.w(mTag, "[abortCaptureSession] proxy:" + mCamera2Proxy +
                    ", proxy:" + mCamera2CaptureSessionProxy);
            return;
        }
        LogHelper.i(mTag, "[abortCaptureSession]+");
        try {
            mCamera2CaptureSessionProxy.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCamera2CaptureSessionProxy = null;
        LogHelper.d(mTag, "[abortCaptureSession]-");
    }

    private void createCaptureSession(Surface captureSurface) {
        if (mPreviewSurfaceTextureWrapper == null || mCamera2Proxy == null ||
                mPreviewSurfaceTextureWrapper.getTextureId() < 0) {
            LogHelper.w(mTag, "[createCaptureSession]Fail proxy:" + mCamera2Proxy +
                    ", pv surface:" + mPreviewSurfaceTextureWrapper);
            return;
        }
        LogHelper.d(mTag, "[createCaptureSession]+");
        abortCaptureSession();
        mSessionSurfaceList.clear();
        mSettingDeviceConfigurator.configSessionSurface(mSessionSurfaceList);
        mSessionSurfaceList.add(new Surface(mPreviewSurfaceTextureWrapper.getSurfaceTexture()));
        if (captureSurface != null) {
            mSessionSurfaceList.add(captureSurface);
            mIsProcessCapture = true;
        }
        try {
            preSetSession();
            mCamera2Proxy.createCaptureSession(mSessionSurfaceList, mSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.i(mTag, "[createCaptureSession]- surface size:" + mSessionSurfaceList.size());
    }

    private void preSetSession() {
        MMSdkConfig.preSetSession(Integer.parseInt(mCameraId),
                createAndConfigRequest(Camera2Proxy.TEMPLATE_PREVIEW).build());
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

    /**
     * An implementation of {@link Camera2Proxy.StateCallback}.
     */
    private class Device2StateCallback extends Camera2Proxy.StateCallback {
        @Override
        public void onError(@Nonnull Camera2Proxy camera2Proxy, int error) {
            LogHelper.i(mTag, "[onError] id:" + camera2Proxy.getId() + ",error:" + error);
            mCameraDeviceManager.recycle(mCameraId);
            CameraUtil.showErrorInfoAndFinish(mApp.getActivity(), error);
        }

        @Override
        public void onDisconnected(@Nonnull Camera2Proxy camera2proxy) {
            LogHelper.i(mTag, "[onDisconnected] id:" + camera2proxy.getId());
        }

        @Override
        public void onClosed(@Nonnull Camera2Proxy camera2Proxy) {
            LogHelper.i(mTag, "[onClosed] device id:" + camera2Proxy.getId());
        }

        @Override
        public void onOpened(@Nonnull Camera2Proxy camera2proxy) {
            synchronized (mDeviceStateSync) {
                LogHelper.i(mTag, "[onOpened] id:" + camera2proxy.getId());
                mCamera2Proxy = camera2proxy;
                if (mPipDeviceCallback != null) {
                    mPipDeviceCallback.onCameraOpened(mCameraId);
                }
                createCaptureSession(null);
                updateSettingUiVisibility();
            }
        }
    }

    /**
     * Session state callback.
     */
    private class SessionStateCallback extends Camera2CaptureSessionProxy.StateCallback {

        @Override
        public void onSurfacePrepared(@Nonnull Camera2CaptureSessionProxy session,
                                      @Nonnull Surface surface) {
            super.onSurfacePrepared(session, surface);
            LogHelper.i(mTag, "[onSurfacePrepared]");
        }

        @Override
        public void onConfigured(@Nonnull Camera2CaptureSessionProxy session) {
            synchronized (mDeviceStateSync) {
                LogHelper.i(mTag, "[onConfigured] process capture:" + mIsProcessCapture +
                        ",session:" + session);
                mCamera2CaptureSessionProxy = session;
                if (mIsProcessCapture) {
                    configStillCaptureRequest();
                    mIsProcessCapture = false;
                } else {
                    configRepeatingRequest();
                }
            }
        }

        @Override
        public void onConfigureFailed(@Nonnull Camera2CaptureSessionProxy session) {
            LogHelper.e(mTag, "[onConfigureFailed]");
        }

        @Override
        public void onReady(@Nonnull Camera2CaptureSessionProxy session) {
            super.onReady(session);
            LogHelper.i(mTag, "[onReady]");
        }

        @Override
        public void onActive(@Nonnull Camera2CaptureSessionProxy session) {
            super.onActive(session);
            LogHelper.i(mTag, "[onActive]");
        }

        @Override
        public void onClosed(@Nonnull Camera2CaptureSessionProxy session) {
            super.onClosed(session);
            LogHelper.i(mTag, "[onClosed] session.");
        }
    }

    /**
     * API2 capture callback implementation.
     */
    private class RepeatingCaptureCallbackImpl extends CameraCaptureSession.CaptureCallback {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                     long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            mSettingDeviceConfigurator.getRepeatingCaptureCallback().onCaptureStarted(
                    session,
                    request,
                    timestamp,
                    frameNumber);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            mSettingDeviceConfigurator.getRepeatingCaptureCallback().onCaptureProgressed(
                    session,
                    request,
                    partialResult
            );
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            mSettingDeviceConfigurator.getRepeatingCaptureCallback().onCaptureCompleted(
                    session,
                    request,
                    result
            );
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            mSettingDeviceConfigurator.getRepeatingCaptureCallback().onCaptureFailed(
                    session,
                    request,
                    failure
            );
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId,
                                               long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            mSettingDeviceConfigurator.getRepeatingCaptureCallback().onCaptureSequenceCompleted(
                    session,
                    sequenceId,
                    frameNumber
            );
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
            mSettingDeviceConfigurator.getRepeatingCaptureCallback().onCaptureSequenceAborted(
                    session,
                    sequenceId
            );
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request,
                                        Surface target, long frameNumber) {
            super.onCaptureBufferLost(session, request, target, frameNumber);
            mSettingDeviceConfigurator.getRepeatingCaptureCallback().onCaptureBufferLost(
                    session,
                    request,
                    target,
                    frameNumber
            );
        }
    }

    /**
     * API2 capture callback implementation.
     */
    private class StillCaptureCallbackImpl extends CameraCaptureSession.CaptureCallback {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                     long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            LogHelper.i(mTag, "[onCaptureStarted] still capture");
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            LogHelper.i(mTag, "[onCaptureCompleted] still capture");
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId,
                                               long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request,
                                        Surface target, long frameNumber) {
            super.onCaptureBufferLost(session, request, target, frameNumber);
        }
    }
}