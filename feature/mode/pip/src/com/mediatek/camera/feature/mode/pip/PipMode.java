/*
 *   Copyright Statement:
 *
 *     This software/firmware and related documentation ("MediaTek Software") are
 *     protected under relevant copyright laws. The information contained herein is
 *     confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 *     the prior written permission of MediaTek inc. and/or its licensors, any
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
 *     NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
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

package com.mediatek.camera.feature.mode.pip;

import android.os.AsyncTask;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;

import com.mediatek.camera.common.IAppUiListener.ISurfaceStatusListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.ISettingManager.SettingController;
import com.mediatek.camera.common.setting.SettingManagerFactory;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.PipController.Listener;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice.IPipDeviceCallback;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pip basic mode, photo and video mode should extends this.
 * This mode will do the common things between photo mode and video mode.
 */
public abstract class PipMode extends CameraModeBase
                              implements IPipDeviceCallback,
                                         Listener {
    private SettingManagerWrapper mFirstSettingManagerWrapper;
    private SettingManagerWrapper mSecondSettingManagerWrapper;
    protected String mBottomCameraId = BACK_CAMERA_ID;
    protected String mTopCameraId = FRONT_CAMERA_ID;
    protected boolean mPipSwitched = false;
    protected SettingManagerFactory mSettingManagerFactory;
    protected PipContext mPipContext;
    protected IPipDevice mPipDevice;
    protected IApp mApp;
    protected PipController mPipController;
    protected Size mCurrentPreviewSize;
    protected ICameraContext mCameraContext;
    private SurfaceChangeListener mSurfaceChangeListener = new SurfaceChangeListener();
    private boolean mNeedUnInit;
    protected CopyOnWriteArrayList<String> mOpenedCameraIds = new CopyOnWriteArrayList<>();
    protected CopyOnWriteArrayList<String> mStopPreviewCameraIds = new CopyOnWriteArrayList<>();
    private PipModeState mModeStatus = PipModeState.PIP_MODE_STATUS_UNKNOWN;

    protected enum PipModeState {
        PIP_MODE_STATUS_UNKNOWN,
        PIP_MODE_STATUS_DEVICE_OPENED,
        PIP_MODE_STATUS_PREVIEWING,
        PIP_MODE_STATUS_CAPTURING,
        // prepare for recording
        PIP_MODE_STATUS_PRE_RECORDING,
        // begin recording
        PIP_MODE_STATUS_RECORDING,
        // stop recording, after stop, will be previewing status
        PIP_MODE_STATUS_STOP_RECORDING,
        PIP_MODE_STATUS_PAUSE_RECORDING,
        PIP_MODE_STATUS_DEVICE_CLOSED,
        PIP_MODE_STATUS_UNINIT,
    }

    @Override
    public void init(@Nonnull IApp app, @Nonnull ICameraContext cameraContext,
            boolean isFromLaunch) {
        super.init(app, cameraContext, isFromLaunch);
        LogHelper.i(getTag(), "[init]+");
        mCameraContext = cameraContext;
        mApp = app;

        updateModeState(PipModeState.PIP_MODE_STATUS_UNKNOWN);
        // mApp.getAppUi().applyAllUIEnabledImmediately(false);
        initSettingManagerWrappers();
        mPipContext = PipContextFactory.getPipContext(app.getActivity());
        mPipController = mPipContext.getPipController(mApp);
        mPipController.setListener(this);
        mPipDevice = mPipContext.getPipDevice(app, mCameraContext, mCameraApi);
        mPipDevice.updateModeType(getModeType());
        mPipDevice.setPipDeviceCallback(getPipDeviceCallback());
        mPipDevice.updateBottomCameraId(mBottomCameraId);
        mPipDevice.openCamera(mFirstSettingManagerWrapper.getSettingManager(),
                mSecondSettingManagerWrapper.getSettingManager());
        updatePreviewSize(getPreviewSize());
        registerSettingStatusListener();
        LogHelper.d(getTag(), "[init]-");
    }

    @Override
    public void resume(@Nonnull DeviceUsage deviceUsage) {
        LogHelper.i(getTag(), "[resume]");
        super.resume(deviceUsage);
        if (mApp.getGSensorOrientation() == OrientationEventListener.ORIENTATION_UNKNOWN) {
            mPipController.onViewOrientationChanged(0);
        }
        mNeedUnInit = false;
        initSettingManagerWrappers();
        mPipDevice.openCamera(mFirstSettingManagerWrapper.getSettingManager(),
                mSecondSettingManagerWrapper.getSettingManager());
        updatePreviewSize(getPreviewSize());
    }

    @Override
    public void pause(@Nullable DeviceUsage nextModeDeviceUsage) {
        super.pause(nextModeDeviceUsage);
        mCurrentPreviewSize = null;
        // Activity pause
        if (mNeedCloseCameraIds == null) {
            mNeedUnInit = true;
            mCameraContext.getSettingManagerFactory().recycle(
                    mFirstSettingManagerWrapper.getCameraId());
            mFirstSettingManagerWrapper.invalidate();
            mPipDevice.closeCamera(mFirstSettingManagerWrapper.getCameraId());
            mCameraContext.getSettingManagerFactory().recycle(
                    mSecondSettingManagerWrapper.getCameraId());
            mSecondSettingManagerWrapper.invalidate();
            mPipDevice.closeCamera(mSecondSettingManagerWrapper.getCameraId());
            updateModeState(PipModeState.PIP_MODE_STATUS_DEVICE_CLOSED);
            updateModeDeviceState(MODE_DEVICE_STATE_CLOSED);
            return;
        }
        // pip <-> pip
        if (mNeedCloseCameraIds.size() == 0) {
            mPipDevice.stopPreview();
            return;
        }
        // pip <-> non-pip
        if (mNeedCloseCameraIds.size() > 0) {
            mNeedUnInit = true;
            for (String cameraId : mNeedCloseCameraIds) {
                mCameraContext.getSettingManagerFactory().recycle(cameraId);
                if (cameraId.equals(mFirstSettingManagerWrapper.getCameraId())) {
                    mFirstSettingManagerWrapper.invalidate();
                } else if (cameraId.equals(mSecondSettingManagerWrapper.getCameraId())) {
                    mSecondSettingManagerWrapper.invalidate();
                }
                mPipDevice.closeCamera(cameraId);
            }
            mPipDevice.stopPreview();
            updateModeState(PipModeState.PIP_MODE_STATUS_DEVICE_CLOSED);
            updateModeDeviceState(MODE_DEVICE_STATE_CLOSED);
        }
    }

    @Override
    public void unInit() {
        super.unInit();
        unRegisterSettingStatusListener();
        if (mNeedUnInit) {
            PipContextFactory.releasePipContext(mApp.getActivity());
        }
    }

    @Override
    public boolean onCameraSelected(@Nonnull String newCameraId) {
        if (canSwitchPip()) {
            mPipController.onPipSwitchCalled();
        }
        return false;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        super.onOrientationChanged(orientation);
        //when register orientation change listener, it may call onOrientationChanged immediately
        //but this time pip device is not created, so we check it firstly.
        if (mPipDevice == null) {
            return;
        }
        mPipController.onViewOrientationChanged(orientation);
    }

    @Override
    public DeviceUsage getDeviceUsage(@Nonnull DataStore dataStore, DeviceUsage oldDeviceUsage) {
        ArrayList<String> openedCameraIds = new ArrayList<>();
        if (oldDeviceUsage != null) {
            mBottomCameraId = oldDeviceUsage.getCameraIdList().get(0);
            mTopCameraId = BACK_CAMERA_ID.equals(mBottomCameraId) ? FRONT_CAMERA_ID :
                BACK_CAMERA_ID;
        }
        LogHelper.d(getTag(), "[getDeviceUsage]mBottomCameraId:" + mBottomCameraId
                + ",mTopCameraId:" + mTopCameraId);
        openedCameraIds.add(mBottomCameraId);
        openedCameraIds.add(mTopCameraId);
        updateModeDefinedCameraApi();
        return new DeviceUsage(DeviceUsage.DEVICE_TYPE_NORMAL, mCameraApi, openedCameraIds);
    }

    @Override
    protected void updateModeDefinedCameraApi() {
        mCameraApi = CameraApiHelper.getCameraApiType(getClass().getSimpleName());
    }

    @Override
    public void onCameraOpened(final String cameraId) {
        LogHelper.i(getTag(), "[onCameraOpened] id:" + cameraId);
        updatePreviewSize(getPreviewSize());
    }

    @Override
    public void afterStopPreview(String cameraId) {
        if (cameraId != null && !mStopPreviewCameraIds.contains(cameraId)) {
            mStopPreviewCameraIds.add(cameraId);
        }
        if (mStopPreviewCameraIds.size() == 2) {
            updateModeState(PipModeState.PIP_MODE_STATUS_DEVICE_OPENED);
            updateModeDeviceState(MODE_DEVICE_STATE_OPENED);
            mStopPreviewCameraIds.clear();
        }
        mPipController.afterStopPreview();
    }

    /**
     * Get tag used to print log.
     * @return the instance of Tag.
     */
    protected abstract Tag getTag();

    /**
     * Get the mode type.
     * @return the mode type.
     */
    protected abstract ModeType getModeType();

    /**
     * Get pip device callback.
     * @return the pip device callback.
     */
    protected abstract IPipDeviceCallback getPipDeviceCallback();

    /**
     * Register setting status listener.
     */
    protected abstract void registerSettingStatusListener();

    /**
     * Unregister setting status listener.
     */
    protected abstract void unRegisterSettingStatusListener();
    /**
     * Get preview size.
     *
     * @return the preview size.
     */
    protected abstract Size getPreviewSize();

    protected boolean updatePreviewSize(Size newPreviewSize) {
        if (newPreviewSize == null || newPreviewSize.equals(mCurrentPreviewSize)) {
            return false;
        }
        LogHelper.i(getTag(),
                "[updatePreviewSize] (old->new):" + mCurrentPreviewSize + "->" + newPreviewSize);
        mPipDevice.stopPreview();
        mCurrentPreviewSize = newPreviewSize;
        mIApp.getAppUi().setPreviewSize(
                mCurrentPreviewSize.getWidth(),
                mCurrentPreviewSize.getHeight(), mSurfaceChangeListener);
        return true;
    }

    protected SettingController getSettingController(String cameraId) {
        if (cameraId.equals(mFirstSettingManagerWrapper.getCameraId())) {
            return mFirstSettingManagerWrapper.getSettingController();
        } else {
            return mSecondSettingManagerWrapper.getSettingController();
        }
    }

    private void initSettingManagerWrappers() {
        if (mFirstSettingManagerWrapper != null && mFirstSettingManagerWrapper.isValid()) {
            return;
        }
        if (mPipSwitched) {
            doCreateSettingManagerWrapper(mTopCameraId, mBottomCameraId);
        } else {
            doCreateSettingManagerWrapper(mBottomCameraId, mTopCameraId);
        }
    }

    private void doCreateSettingManagerWrapper(String firstCameraId, String secondCameraId) {
        mSettingManagerFactory = mCameraContext.getSettingManagerFactory();
        mFirstSettingManagerWrapper = new SettingManagerWrapper(
                mSettingManagerFactory.getInstance(
                        firstCameraId,
                        getModeKey(),
                        getModeType(),
                        mCameraApi));
        mSecondSettingManagerWrapper = new SettingManagerWrapper(
                mSettingManagerFactory.getInstance(
                        secondCameraId,
                        getModeKey(),
                        getModeType(),
                        mCameraApi));
    }

    @Override
    public void onPIPPictureTaken(byte[] jpegData) {

    }

    @Override
    public void doStartPreview() {

    }

    @Override
    public boolean canSwitchPip() {
        LogHelper.d(getTag(), "[canSwitchPip] status:" + getModeDeviceStatus());
        if (getModeDeviceStatus() == MODE_DEVICE_STATE_PREVIEWING ||
                getModeDeviceStatus() == MODE_DEVICE_STATE_RECORDING) {
            applyAllUIEnable(false);
            return true;
        }
        return false;
    }

    @Override
    public void onPipSwitchedInRenderer() {
        doSwitchPip();
    }

    @Override
    public void onFirstFrameAvailable(long timestamp) {
        LogHelper.i(getTag(), "[onFirstFrameAvailable] time stamp:" + timestamp);
        if (mIApp != null && getModeState() != PipModeState.PIP_MODE_STATUS_CAPTURING) {
            String originalCamId = getCameraIdByFacing(mDataStore.getValue(
                    KEY_CAMERA_SWITCHER, null, mDataStore.getGlobalScope()));
            updateModeState(PipModeState.PIP_MODE_STATUS_PREVIEWING);
            // mIApp.getAppUi().applyAllUIEnabled(true);
            mIApp.getAppUi().onPreviewStarted(originalCamId);
            updateModeDeviceState(MODE_DEVICE_STATE_PREVIEWING);
        }
    }

    protected void updateModeState(PipModeState modeStatus) {
        LogHelper.d(getTag(), "[updateModeState] mModeStatus:" + mModeStatus
                + ",modeStatus:" + modeStatus);
        mModeStatus = modeStatus;
        switch (modeStatus) {
        case PIP_MODE_STATUS_UNKNOWN:
        case PIP_MODE_STATUS_CAPTURING:
            applyAllUIEnable(false);
            break;
        case PIP_MODE_STATUS_PREVIEWING:
            applyAllUIEnable(true);
            break;
        default:
            break;
        }
    }

    protected void applyAllUIEnable(boolean enable) {
        boolean isMainTHread = Thread.currentThread().getName().equals("main");
        if (isMainTHread) {
            mApp.getAppUi().applyAllUIEnabledImmediately(enable);
        } else {
            mApp.getAppUi().applyAllUIEnabled(enable);
        }
    }

    protected PipModeState getModeState() {
        return mModeStatus;
    }

    @Override
    public boolean bottomGraphicIsBackCamera() {
        return BACK_CAMERA_ID.equals(mBottomCameraId);
    }

    @Override
    public String getBottomGraphicCameraId() {
        return mBottomCameraId;
    }

    @Override
    protected ISettingManager getSettingManager() {
        if (mFirstSettingManagerWrapper == null) {
            return null;
        }
        return mFirstSettingManagerWrapper.getSettingManager();
    }

    @Override
    protected void updateModeDeviceState(String state) {
        super.updateModeDeviceState(state);
        if (mSecondSettingManagerWrapper == null) {
            return;
        }
        mSecondSettingManagerWrapper.getSettingManager().updateModeDeviceStateToSetting(
                getClass().getSimpleName(), state);
    }

    private void doSwitchPip() {
        String tempCameraId = mBottomCameraId;
        unRegisterSettingStatusListener();
        mBottomCameraId = mTopCameraId;
        mTopCameraId = tempCameraId;
        registerSettingStatusListener();
        mPipSwitched = !mPipSwitched;
    }

    /**
     * Pip surface change listener.
     */
    private class SurfaceChangeListener implements ISurfaceStatusListener {
        @Override
        public void surfaceAvailable(SurfaceHolder surfaceHolder, int width, int height) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    IPerformanceProfile profile =
                            PerformanceTracker.create(getTag(), "surfaceAvailable");
                    profile.start();

                    if (mCurrentPreviewSize == null) {
                        profile.stop();
                        LogHelper.i(getTag(), "[surfaceAvailable]- current preview size is null!");
                        return null;
                    }
                    if (width != mCurrentPreviewSize.getWidth() ||
                            height != mCurrentPreviewSize.getHeight()) {
                        profile.stop();
                        LogHelper.i(getTag(), "[surfaceAvailable]- size != mCurrentPreviewSize!");
                        return null;
                    }
                    mPipController.setPreviewSurface(surfaceHolder.getSurface());
                    profile.mark("setPreviewSurface");
                    mPipController.setPreviewTextureSize(mCurrentPreviewSize);
                    profile.mark("setPreviewTextureSize");
                    SurfaceTextureWrapper bottomSurfaceTextureWrapper =
                            mPipController.getBottomSurfaceTextureWrapper();
                    profile.mark("getBottomSurfaceTextureWrapper");
                    SurfaceTextureWrapper topSurfaceTextureWrapper =
                            mPipController.getTopSurfaceTextureWrapper();
                    profile.mark("getTopSurfaceTextureWrapper");
                    if (mPipSwitched) {
                        mPipDevice.startPreview(topSurfaceTextureWrapper,
                                bottomSurfaceTextureWrapper);
                    } else {
                        mPipDevice.startPreview(bottomSurfaceTextureWrapper,
                                topSurfaceTextureWrapper);
                    }
                    profile.mark("startPreview");
                    profile.stop();
                    return null;
                }
            } .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int width, int height) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    IPerformanceProfile profile =
                            PerformanceTracker.create(getTag(), "surfaceChanged");
                    profile.start();

                    if (mCurrentPreviewSize == null) {
                        profile.stop();
                        LogHelper.i(getTag(), "[surfaceChanged]- current preview size is null!");
                        return null;
                    }
                    if (width != mCurrentPreviewSize.getWidth() ||
                            height != mCurrentPreviewSize.getHeight()) {
                        profile.stop();
                        LogHelper.i(getTag(), "[surfaceChanged]- size != mCurrentPreviewSize!");
                        return null;
                    }
                    mPipController.setPreviewSurface(surfaceHolder.getSurface());
                    profile.mark("setPreviewSurface");
                    mPipController.setPreviewTextureSize(mCurrentPreviewSize);
                    profile.mark("setPreviewTextureSize");
                    SurfaceTextureWrapper bottomSurfaceTextureWrapper =
                            mPipController.getBottomSurfaceTextureWrapper();
                    profile.mark("getBottomSurfaceTextureWrapper");
                    SurfaceTextureWrapper topSurfaceTextureWrapper =
                            mPipController.getTopSurfaceTextureWrapper();
                    profile.mark("getTopSurfaceTextureWrapper");
                    if (mPipSwitched) {
                        mPipDevice.startPreview(topSurfaceTextureWrapper,
                                bottomSurfaceTextureWrapper);
                    } else {
                        mPipDevice.startPreview(bottomSurfaceTextureWrapper,
                                topSurfaceTextureWrapper);
                    }
                    profile.mark("startPreview");
                    profile.stop();
                    return null;
                }
            } .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mPipController.notifySurfaceViewDestroyed(surfaceHolder.getSurface());
            if (mCurrentPreviewSize == null) {
                LogHelper.i(getTag(), "[surfaceDestroyed] mCurrentPreviewSize is null!");
                return;
            }
            int width = surfaceHolder.getSurfaceFrame().width();
            int height = surfaceHolder.getSurfaceFrame().height();
            if (width != mCurrentPreviewSize.getWidth() ||
                    height != mCurrentPreviewSize.getHeight()) {
                LogHelper.i(getTag(), "[surfaceDestroyed] size != mCurrentPreviewSize!");
                return;
            }
            mPipDevice.stopPreview();
            LogHelper.i(getTag(), "[surfaceDestroyed]");
        }
    }
}