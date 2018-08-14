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

package com.mediatek.camera.feature.mode.pip.photo;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;

import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.StatusMonitor.StatusChangeListener;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.PipMode;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice.IPipDeviceCallback;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pip photo mode implementation.
 */
public class PipPhotoMode extends PipMode {
    private static final Tag TAG = new Tag(PipPhotoMode.class.getSimpleName());
    private static final String KEY_PICTURE_SIZE = "key_picture_size";
    private static final String KEY_ZSD = "key_zsd";
    private PhotoHelper mPhotoHelper;
    private volatile boolean mIsPaused = false;

    @Override
    public void init(@Nonnull IApp app, @Nonnull ICameraContext cameraContext,
            boolean isFromLaunch) {
        IPerformanceProfile profile = PerformanceTracker.create(TAG, "init");
        profile.start();
        mPhotoHelper = new PhotoHelper(cameraContext);
        super.init(app, cameraContext, isFromLaunch);
        profile.stop();
    }

    @Override
    public void resume(@Nonnull DeviceUsage deviceUsage) {
        IPerformanceProfile profile = PerformanceTracker.create(TAG, "resume");
        profile.start();
        super.resume(deviceUsage);
        profile.stop();
        mIsPaused = false;
    }

    @Override
    public void pause(@Nullable DeviceUsage nextModeDeviceUsage) {
        IPerformanceProfile profile = PerformanceTracker.create(TAG,
                "pause:" + mNeedCloseCameraIds);
        mIsPaused = true;
        profile.start();
        super.pause(nextModeDeviceUsage);
        mOpenedCameraIds.clear();
        profile.stop();
    }

    @Override
    public void unInit() {
        IPerformanceProfile profile = PerformanceTracker.create(TAG, "unInit");
        profile.start();
        super.unInit();
        profile.stop();
    }

    @Override
    public boolean onShutterButtonClick() {
        if (mOpenedCameraIds.size() == 2) {
            return takePicture();
        }
        return false;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        if (mOpenedCameraIds.size() == 2) {
            return takePicture();
        }
        return false;
    }

    @Override
    public void onPIPPictureTaken(byte[] jpegData) {
        LogHelper.i(TAG, "[onPIPPictureTaken]");
        Size jpegSize = CameraUtil.getSizeFromExif(jpegData);
        int jpegRotation = CameraUtil.getOrientationFromExif(jpegData);
        ContentValues contentValues = createPhotoContentValues(jpegSize, jpegRotation);
        mCameraContext.getMediaSaver().addSaveRequest(
                jpegData,
                contentValues,
                null,
                mMediaSaverListener);
        if (jpegData != null) {
            //update thumbnail, pip not update thumbnail currently, so we decode full image.
            Bitmap bitmap = BitmapCreator.decodeBitmapFromJpeg(jpegData, mIApp.getAppUi()
                    .getThumbnailViewWidth());
            mIApp.getAppUi().updateThumbnail(bitmap);
        }
    }

    @Override
    public void onPipSwitchedInRenderer() {
        LogHelper.i(TAG, "[onPipSwitchedInRenderer]+");
        getSettingController(mTopCameraId).postRestriction(
                PipPhotoCombination.getTopPictureSizeRemoveRelation());

        String topPictureSzStr = getSettingController(mTopCameraId).queryValue(KEY_PICTURE_SIZE);
        Size topOverrideSize = null;
        List<String> topPictureSizes = null;
        // if aspect ratio is not the same, select a max picture size.
        if (!CameraUtil.isTheSameAspectRatio(mCurrentPreviewSize,
                                              CameraUtil.getSize(topPictureSzStr))) {
            topOverrideSize = getTopCameraPictureSize(mCurrentPreviewSize, false);
            topPictureSizes =
                getSettingController(mTopCameraId).querySupportedPlatformValues(KEY_PICTURE_SIZE);
            LogHelper.i(TAG, "[onPipSwitchedInRenderer] topPictureSz:" + topPictureSzStr +
                                ",override size:" + topOverrideSize +
                                ",top supported picture sizes:" + topPictureSizes);
        }
        super.onPipSwitchedInRenderer();
        postAllRestrictions(mBottomCameraId);
        if (topOverrideSize != null && topPictureSizes != null) {
            getSettingController(mBottomCameraId).postRestriction(
                PipPhotoCombination.getTopPictureSizeRelation(topOverrideSize, topPictureSizes));
        }
        postAllRestrictions(mTopCameraId);
        mPipDevice.updateBottomCameraId(mBottomCameraId);
        updateModeState(PipModeState.PIP_MODE_STATUS_PREVIEWING);
        LogHelper.d(TAG, "[onPipSwitchedInRenderer]-");
    }

    @Override
    public void doStartPreview() {
        LogHelper.i(TAG, "[doStartPreview]");
        SurfaceTextureWrapper firstSurfaceTextureWrapper =
                mPipSwitched ? mPipController.getTopSurfaceTextureWrapper() :
                        mPipController.getBottomSurfaceTextureWrapper();
        SurfaceTextureWrapper secondSurfaceTextureWrapper =
                mPipSwitched ? mPipController.getBottomSurfaceTextureWrapper() :
                        mPipController.getTopSurfaceTextureWrapper();
        mPipDevice.startPreview(firstSurfaceTextureWrapper, secondSurfaceTextureWrapper);
        //mIApp.getAppUi().applyAllUIEnabled(true);
        updateModeState(PipModeState.PIP_MODE_STATUS_PREVIEWING);
        updateModeDeviceState(MODE_DEVICE_STATE_PREVIEWING);
    }

    @Override
    public boolean onBackPressed() {
        return super.onBackPressed();
    }

    @Override
    protected Tag getTag() {
        return TAG;
    }

    @Override
    protected ModeType getModeType() {
        return ModeType.PHOTO;
    }

    @Override
    protected IPipDeviceCallback getPipDeviceCallback() {
        return this;
    }

    @Override
    protected void registerSettingStatusListener() {
        mCameraContext.getStatusMonitor(mBottomCameraId).registerValueChangedListener(
                KEY_PICTURE_SIZE, mSettingStatusChangeListener);
        mCameraContext.getStatusMonitor(mBottomCameraId).registerValueChangedListener(
                KEY_ZSD, mSettingStatusChangeListener);
    }

    @Override
    protected void unRegisterSettingStatusListener() {
        mCameraContext.getStatusMonitor(mBottomCameraId).unregisterValueChangedListener(
                KEY_PICTURE_SIZE, mSettingStatusChangeListener);
        mCameraContext.getStatusMonitor(mBottomCameraId).unregisterValueChangedListener(
                KEY_ZSD, mSettingStatusChangeListener);
    }

    @Override
    public void onCameraOpened(String cameraId) {
        if (mIsPaused) {
            LogHelper.i(TAG, "[onCameraOpened] mode have paused!");
            return;
        }
        super.onCameraOpened(cameraId);
        postAllRestrictions(cameraId);
        if (!mOpenedCameraIds.contains(cameraId)) {
            mOpenedCameraIds.add(cameraId);
        }
        if (mOpenedCameraIds.size() == 2) {
            updateModeState(PipModeState.PIP_MODE_STATUS_DEVICE_OPENED);
            updateModeDeviceState(MODE_DEVICE_STATE_OPENED);
        }
    }

    @Override
    protected Size getPreviewSize() {
        String pictureSizeStr =
                getSettingController(mBottomCameraId).queryValue(KEY_PICTURE_SIZE);
        ArrayList<Size> bottomPreviewSizes =
                mPipDevice.getSupportedPreviewSizes(SurfaceHolder.class, mBottomCameraId);
        ArrayList<Size> topPreviewSize =
                mPipDevice.getSupportedPreviewSizes(SurfaceHolder.class, mTopCameraId);

        ArrayList<Size> supportedPreviewSizes = getSupportedPreviewSizes(bottomPreviewSizes,
                topPreviewSize);
        LogHelper.d(TAG, "[getPreviewSize] pictureSize:" + pictureSizeStr +
                ",bottomPreviewSizes:" + bottomPreviewSizes +
                ",topPreviewSize:" + topPreviewSize);
        if (pictureSizeStr == null ||
                supportedPreviewSizes == null || supportedPreviewSizes.size() == 0) {
            return null;
        }
        Size pictureSize = CameraUtil.getSize(pictureSizeStr);
        return CameraUtil.getOptimalPreviewSize(
                mApp.getActivity(),
                CameraUtil.filterSupportedSizes(supportedPreviewSizes, new Size(1920, 1080)),
                ((double) pictureSize.getWidth()) / pictureSize.getHeight(),
                false);
    }

    private void postAllRestrictions(String cameraId) {
        String bottomZsdValue = getSettingController(mBottomCameraId).queryValue(KEY_ZSD);
        Size topPictureSize = getTopCameraPictureSize(mCurrentPreviewSize, true);
        getSettingController(cameraId).postRestriction(
                PipPhotoCombination.getPipOnRelation(
                        cameraId.equals(mBottomCameraId),
                        topPictureSize,
                        bottomZsdValue));
    }

    private boolean takePicture() {
        LogHelper.i(TAG, "[takePicture]+,modeState:" + getModeState());
        if (mPipController.isDuringSwitchPip()) {
            LogHelper.w(TAG, "[takePicture] skip, is during switch pip!");
            return false;
        }
        if (mCameraContext.getStorageService().getCaptureStorageSpace() <= 0) {
            LogHelper.w(TAG, "[takePicture] skip, storage is full!");
            return false;
        }
        if (!mPipDevice.isReadyForCapture()) {
            LogHelper.w(TAG, "[takePicture] skip, preview is stopped!");
            return false;
        }
        if (getModeState() == PipModeState.PIP_MODE_STATUS_CAPTURING) {
            return false;
        }
        updateModeState(PipModeState.PIP_MODE_STATUS_CAPTURING);
        mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_CAPTURE, null);
        // mIApp.getAppUi().applyAllUIEnabled(false);
        updateModeDeviceState(MODE_DEVICE_STATE_CAPTURING);
        String bottomPictureSize =
                getSettingController(mBottomCameraId).queryValue(KEY_PICTURE_SIZE);
        String topPictureSize =
                getSettingController(mTopCameraId).queryValue(KEY_PICTURE_SIZE);
        Size bottomPz = CameraUtil.getSize(bottomPictureSize);
        Size topPz = CameraUtil.getSize(topPictureSize);
        int gSensorOrientation = mIApp.getGSensorOrientation();
        if ((gSensorOrientation % 180 == 0)
                || (gSensorOrientation == OrientationEventListener.ORIENTATION_UNKNOWN)) {
            //portrait
            bottomPz = new Size(bottomPz.getHeight(), bottomPz.getWidth());
            topPz = new Size(topPz.getHeight(), topPz.getWidth());
        }
        mPipDevice.takePicture(bottomPz, topPz);
        LogHelper.d(TAG, "[takePicture]-");
        return true;
    }

    private ContentValues createPhotoContentValues(Size jpegSize, int jpegOrientation) {
        ContentValues values = new ContentValues();
        long dateTaken = System.currentTimeMillis();
        String title = mPhotoHelper.createPhotoFileTitle(dateTaken);
        String fileName = mPhotoHelper.createPhotoFileName(title);
        String path = mICameraContext.getStorageService().getFileDirectory() + '/' + fileName;
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.ImageColumns.WIDTH, jpegSize.getWidth());
        values.put(MediaStore.Images.ImageColumns.HEIGHT, jpegSize.getHeight());
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, jpegOrientation);
        values.put(MediaStore.Images.ImageColumns.DATA, path);
        Location location = mICameraContext.getLocation();
        if (location != null) {
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location.getLatitude());
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location.getLongitude());
        }
        return values;
    }

    private Size getTopCameraPictureSize(Size previewSize, boolean needMinSize) {
        if (previewSize == null) {
            return null;
        }
        List<String> topSupportedPictureSizes = getSettingController(mTopCameraId).
                querySupportedPlatformValues(KEY_PICTURE_SIZE);
        if (topSupportedPictureSizes == null) {
            return null;
        }
        Size topPictureSize =
                CameraUtil.getSizeByTargetRatio(topSupportedPictureSizes, previewSize, needMinSize);
        LogHelper.d(TAG, "[getTopCameraPictureSize]previewSize:" + previewSize +
            ",topPictureSize:" + topPictureSize + ",topSupportedPictureSizes:" +
                topSupportedPictureSizes);
        return topPictureSize;
    }

    private ArrayList<Size> getSupportedPreviewSizes(ArrayList<Size> bottomPreviewSizes,
                                                     ArrayList<Size> topPreviewSizes) {
        if (bottomPreviewSizes == null || topPreviewSizes == null ||
                bottomPreviewSizes.size() == 0 ||
                topPreviewSizes.size() == 0) {
            return null;
        }
        ArrayList<Size> previewSizes = new ArrayList<>();
        for (Size bottom : bottomPreviewSizes) {
            for (Size top : topPreviewSizes) {
                if (bottom.getWidth() == top.getWidth() &&
                        bottom.getHeight() == top.getHeight()) {
                    previewSizes.add(bottom);
                    break;
                }
            }
        }
        return previewSizes;
    }

    private final StatusChangeListener mSettingStatusChangeListener = new StatusChangeListener() {
        @Override
        public void onStatusChanged(String key, String value) {
            LogHelper.i(TAG, "[onStatusChanged] key:" + key + ",value:" + value);
            Relation relation = null;
            boolean needChangeTopCamSettingValue = false;
            if (KEY_PICTURE_SIZE.equals(key)) {
                boolean previewSizeChanged = updatePreviewSize(getPreviewSize());
                if (previewSizeChanged) {
                    Size topPictureSize = getTopCameraPictureSize(mCurrentPreviewSize, true);
                    List<String > overridePictureSizes = new ArrayList<>();
                    overridePictureSizes.add(CameraUtil.buildSize(topPictureSize));
                    relation = PipPhotoCombination.getTopPictureSizeRelation(
                            topPictureSize,
                            overridePictureSizes);
                }
            }
            if (KEY_ZSD.equals(key)) {
                relation = PipPhotoCombination.getTopZsdRelation(value);
                needChangeTopCamSettingValue = true;
            }
            if (relation != null) {
                getSettingController(mTopCameraId).postRestriction(relation);
            }
            if (needChangeTopCamSettingValue) {
                mPipDevice.requestChangeSettingValue(mTopCameraId);
            }
        }
    };

    private final MediaSaver.MediaSaverListener mMediaSaverListener =
            new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(TAG, "[onFileSaved] uri = " + uri);
            mIApp.notifyNewMedia(uri, true);
        }
    };
}