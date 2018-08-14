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

package com.mediatek.camera.feature.mode.pip.video;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ConditionVariable;
import android.provider.MediaStore;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;

import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.mode.video.videoui.IVideoUI;
import com.mediatek.camera.common.sound.ISoundPlayback;
import com.mediatek.camera.common.storage.IStorageService.IStorageStateListener;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.PipMode;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice;
import com.mediatek.camera.feature.mode.pip.photo.PhotoHelper;
import com.mediatek.camera.feature.mode.pip.video.view.PipVideoQualitySettingView;
import com.mediatek.camera.feature.mode.pip.video.view.PipVideoQualitySettingView.Listener;
import com.mediatek.camera.portability.MediaRecorderEx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pip video mode implementation.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class PipVideoMode extends PipMode {
    private static final Tag TAG = new Tag(PipVideoMode.class.getSimpleName());
    private static final String KEY_VIDEO_QUALITY = "key_video_quality";
    private static final String KEY_MICROPHONE = "key_microphone";
    private static final String KEY_VIDEO_STATUS = "key_video_status";
    private static final Size PIP_MAX_VIDEO_SIZE = new Size(1920, 1080);
    private static final Size PIP_MIN_VIDEO_SIZE = new Size(640, 480);
    private MediaRecorder mMediaRecorder;
    private int mRecordingRotation;
    private StartRecordingAsyncTask mStartRecordingAsyncTask;
    private StopRecordingAsyncTask mStopRecordingAsyncTask;
    private String mVideoFilePath;
    private final PipMediaSaverListener mVideoFileSavedListener = new PipMediaSaverListener();
    private IVideoUI mVideoUi;
    private PipVideoHelper mPipVideoHelper;
    private PhotoHelper mPhotoHelper;
    private boolean mRecordingPaused;
    private boolean mVssCapturing;
    private Map<String, CamcorderProfile> mSupportedVideoProfileMap = new ConcurrentHashMap<>();
    private String mCurrentVideoQuality;
    private CamcorderProfile mCurrentProfile;
    private PipVideoQualitySettingView mPipVideoQualitySettingView;
    private volatile boolean mIsPaused = false;

    @Override
    public void init(@Nonnull IApp app, @Nonnull ICameraContext cameraContext,
            boolean isFromLaunch) {
        mPipVideoHelper = new PipVideoHelper(cameraContext);
        mPhotoHelper = new PhotoHelper(cameraContext);
        mPipVideoQualitySettingView = new PipVideoQualitySettingView(
                mVideoQualityChangeListener,
                app.getActivity(), cameraContext.getDataStore());
        mVideoUi = app.getAppUi().getVideoUi();
        mVideoUi.initVideoUI(configUISpec());
        super.init(app, cameraContext, isFromLaunch);
    }

    @Override
    public void resume(@Nonnull DeviceUsage deviceUsage) {
        super.resume(deviceUsage);
        mCameraContext.getStorageService().registerStorageStateListener(mStorageStateListener);
        mIsPaused = false;
    }

    @Override
    public void pause(@Nullable DeviceUsage nextModeDeviceUsage) {
        mIsPaused = true;
        // super mode will do stop preview or close camera, so we must stop recording firstly.
        stopRecording(true);
        super.pause(nextModeDeviceUsage);
        mCameraContext.getStorageService().unRegisterStorageStateListener(mStorageStateListener);
        mOpenedCameraIds.clear();
    }

    @Override
    public void unInit() {
        super.unInit();
        mApp.getAppUi().removeSettingView(mPipVideoQualitySettingView);
        if (mVideoUi != null) {
            mVideoUi.unInitVideoUI();
            mVideoUi = null;
        }
    }

    @Override
    public boolean onUserInteraction() {
        switch (getModeState()) {
            case PIP_MODE_STATUS_PRE_RECORDING:
            case PIP_MODE_STATUS_RECORDING:
            case PIP_MODE_STATUS_PAUSE_RECORDING:
                return true;
            default:
                super.onUserInteraction();
                return true;
        }
    }

    @Override
    public boolean onShutterButtonClick() {
        LogHelper.d(TAG, "[onShutterButtonClick],state:" + getModeState());
        switch(getModeState()) {
        case PIP_MODE_STATUS_PREVIEWING:
            if (mOpenedCameraIds.size() == 2) {
                return startRecording();
            }
            break;
        case PIP_MODE_STATUS_RECORDING:
        case PIP_MODE_STATUS_PAUSE_RECORDING:
            return stopRecording(true);
        default:
            break;
        }
        return false;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        LogHelper.d(TAG, "[onShutterButtonLongPressed]");
        if (mOpenedCameraIds.size() == 2) {
            return startRecording();
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
        return stopRecording(true);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        super.onOrientationChanged(orientation);
        if (mVideoUi == null) {
            return;
        }
        mVideoUi.updateOrientation(orientation);
    }

    @Override
    public void onPIPPictureTaken(byte[] jpegData) {
        LogHelper.d(TAG, "[onPIPPictureTaken]");
        mVssCapturing = false;
        ContentValues contentValues = createPhotoContentValues(jpegData);
        mCameraContext.getMediaSaver().addSaveRequest(jpegData, contentValues, null, null);
    }

    @Override
    public void onPipSwitchedInRenderer() {
        super.onPipSwitchedInRenderer();
        postAllRestrictions(mBottomCameraId);
        postAllRestrictions(mTopCameraId);
        mPipDevice.updateBottomCameraId(mBottomCameraId);
        applyAllUIEnable(true);
    }

    @Override
    protected Tag getTag() {
        return TAG;
    }

    @Override
    protected ModeType getModeType() {
        return ModeType.VIDEO;
    }

    @Override
    protected IPipDevice.IPipDeviceCallback getPipDeviceCallback() {
        return this;
    }

    @Override
    public void onCameraOpened(String cameraId) {
        if (mIsPaused) {
            LogHelper.i(TAG, "[onCameraOpened] mode have paused!");
            return;
        }
        postAllRestrictions(cameraId);
        initSupportedVideoProfiles();
        if (!mOpenedCameraIds.contains(cameraId)) {
            mOpenedCameraIds.add(cameraId);
        }
        if (mOpenedCameraIds.size() == 2) {
            updateModeState(PipModeState.PIP_MODE_STATUS_DEVICE_OPENED);
            updateModeDeviceState(MODE_DEVICE_STATE_OPENED);
        }
        if (mCurrentVideoQuality == null || mSupportedVideoProfileMap.size() == 0) {
            return;
        }
        super.onCameraOpened(cameraId);
    }

    @Override
    protected Size getPreviewSize() {
        if (mSupportedVideoProfileMap.size() == 0 || mCurrentVideoQuality == null) {
            return null;
        }
        mCurrentProfile = mSupportedVideoProfileMap.get(mCurrentVideoQuality);
        return new Size(mCurrentProfile.videoFrameWidth, mCurrentProfile.videoFrameHeight);
    }

    @Override
    protected void registerSettingStatusListener() {
    }

    @Override
    protected void unRegisterSettingStatusListener() {
    }

    private void postAllRestrictions(String cameraId) {
        getSettingController(cameraId).postRestriction(
                PipVideoCombination.getPipOnRelation(
                        cameraId.equals(mBottomCameraId),
                        mCameraApi,
                        mStartRecordingAsyncTask != null));
        String videoQualityId = getSettingController(
                cameraId).queryValue(KEY_VIDEO_QUALITY);
        getSettingController(cameraId).postRestriction(
                PipVideoCombination.getPipVideoQualityRelation(videoQualityId));
    }

    private void initSupportedVideoProfiles() {
        if (mSupportedVideoProfileMap.size() > 0 || mPipVideoHelper == null ||
                mPipVideoQualitySettingView == null) {
            LogHelper.w(TAG, "initSupportedVideoProfiles with illegal state!" +
                    " mSupportedVideoProfileMap size:" + mSupportedVideoProfileMap.size() +
                    " mPipVideoQualitySettingView :" + mPipVideoQualitySettingView);
            return;
        }
        List<String> bottomCamSupportedVideoQualities =
                getSettingController(
                        "0").querySupportedPlatformValues(KEY_VIDEO_QUALITY);
        if (bottomCamSupportedVideoQualities == null ||
                bottomCamSupportedVideoQualities.size() <= 0) {
            return;
        }
        List<String> topCamSupportedVideoQualities =
                getSettingController("1").querySupportedPlatformValues(KEY_VIDEO_QUALITY);
        if (topCamSupportedVideoQualities == null ||
                topCamSupportedVideoQualities.size() <= 0) {
            return;
        }
        // filter supported video qualities
        HashMap<String, CamcorderProfile> bottomCameraProfileMap =
                mPipVideoHelper.filterSupportedVideoProfiles(
                        bottomCamSupportedVideoQualities,
                        "0",
                        PIP_MAX_VIDEO_SIZE,
                        PIP_MIN_VIDEO_SIZE);
        HashMap<String, CamcorderProfile> topCameraProfileMap =
                mPipVideoHelper.filterSupportedVideoProfiles(
                        bottomCamSupportedVideoQualities,
                        "1",
                        PIP_MAX_VIDEO_SIZE,
                        PIP_MIN_VIDEO_SIZE);
        topCameraProfileMap.putAll(bottomCameraProfileMap);
        mSupportedVideoProfileMap.putAll(topCameraProfileMap);

        // find default video quality
        String videoQuality = mDataStore.getValue(PipVideoQualitySettingView.KEY_PIP_VIDEO_QUALITY,
                new ArrayList<>(mSupportedVideoProfileMap.keySet()).get(0),
                mDataStore.getGlobalScope());
        CamcorderProfile defaultProfile = mSupportedVideoProfileMap.get(videoQuality);
        mCurrentVideoQuality =
                mPipVideoHelper.filterVideoQuality(
                    new ArrayList<>(mSupportedVideoProfileMap.keySet()),
                    defaultProfile.videoFrameWidth + "x" + defaultProfile.videoFrameHeight);
        // update video quality view
        mPipVideoQualitySettingView.setEntryValues(
                new ArrayList<>(mSupportedVideoProfileMap.keySet()));
        mPipVideoQualitySettingView.setDefaultValue(mCurrentVideoQuality);

        mApp.getAppUi().addSettingView(mPipVideoQualitySettingView);
        LogHelper.d(TAG, "[initSupportedVideoProfiles] current quality:" + mCurrentVideoQuality +
                    ", supported qualities:" + mSupportedVideoProfileMap.keySet());
    }

    private boolean prepareMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        boolean isRecordAudio = "on".equals(
                getSettingController(mBottomCameraId).queryValue(KEY_MICROPHONE));
        if (isRecordAudio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setAudioChannels(mCurrentProfile.audioChannels);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(mCurrentProfile.fileFormat);
        mMediaRecorder.setVideoFrameRate(mCurrentProfile.videoFrameRate);
        mMediaRecorder.setVideoSize(
                mCurrentProfile.videoFrameWidth,
                mCurrentProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(mCurrentProfile.videoBitRate);
        mMediaRecorder.setVideoEncoder(mCurrentProfile.videoCodec);
        if (isRecordAudio) {
            mMediaRecorder.setAudioEncoder(mCurrentProfile.audioCodec);
            mMediaRecorder.setAudioEncodingBitRate(mCurrentProfile.audioBitRate);
            mMediaRecorder.setAudioSamplingRate(mCurrentProfile.audioSampleRate);
        }
        Location loc = mCameraContext.getLocation();
        if (loc != null) {
            mMediaRecorder.setLocation((long) loc.getLatitude(), (long) loc.getLongitude());
        }
        mMediaRecorder.setMaxFileSize(mCameraContext.getStorageService().getRecordStorageSpace());
        mRecordingRotation = CameraUtil.getRecordingRotation(mApp.getGSensorOrientation(), 0);
        mMediaRecorder.setOutputFile(mPipVideoHelper.getVideoTempFilePath());
        // because of preview buffer,orientation should be considered again
        // should always get the back camera Id as reference
        mMediaRecorder.setOrientationHint(mRecordingRotation);
        mPipVideoHelper.setMediaRecorderParameterEx(mMediaRecorder);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            LogHelper.e(TAG, "[prepareMediaRecorder] prepare failed", e);
            releaseMediaRecorder(true);
            return false;
        }
        mMediaRecorder.setOnInfoListener(mOnInfoListener);
        mMediaRecorder.setOnErrorListener(mOnErrorListener);
        return true;
    }

    private void releaseMediaRecorder(boolean forceCleanTempFile) {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (forceCleanTempFile) {
            File f = new File(mPipVideoHelper.getVideoTempFilePath());
            if (f.delete()) {
                LogHelper.d(TAG, "temp video file deleted!");
            }
        }
        mVideoFilePath = null;
    }

    private void pauseRecording() {
        LogHelper.d(TAG, "[pauseRecording] +");
        try {
            mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_PAUSE_RECORDING);
            MediaRecorderEx.pause(mMediaRecorder);
            mRecordingPaused = true;
        } catch (IllegalStateException e) {
            mVideoUi.showInfo(IVideoUI.VIDEO_RECORDING_NOT_AVAILABLE);
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[pauseRecording] -");
    }

    private void resumeRecording() {
        LogHelper.d(TAG, "[resumeRecording] +");
        try {
            mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_RESUME_RECORDING);
            MediaRecorderEx.resume(mMediaRecorder);
            mRecordingPaused = false;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[resumeRecording] -");
    }

    private boolean startRecording() {
        LogHelper.i(TAG, "[startRecording]+");
        if (mCameraContext.getStorageService().getRecordStorageSpace() <= 0) {
            LogHelper.w(TAG, "[startRecording] skip, storage is full!");
            return false;
        }
        if (mStartRecordingAsyncTask == null) {
            mStartRecordingAsyncTask = new StartRecordingAsyncTask();
        }

        if (AsyncTask.Status.PENDING != mStartRecordingAsyncTask.getStatus()) {
            LogHelper.w(TAG, "[startRecording]- skip, recoding is started!");
            return false;
        }

        if (getModeState() != PipModeState.PIP_MODE_STATUS_PREVIEWING) {
            LogHelper.d(TAG, "[startRecording] fail, current status:" + getModeState());
            return false;
        }
        mApp.getAppUi().applyAllUIEnabledImmediately(false);
        mApp.getAppUi().applyAllUIVisibility(View.GONE);
        mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_PRE_RECORDING);
        updateModeState(PipModeState.PIP_MODE_STATUS_PRE_RECORDING);
        mStartRecordingAsyncTask.execute();
        mStopRecordingAsyncTask = null;
        LogHelper.d(TAG, "[startRecording]-");
        return true;
    }

    //Note: this method should be called in UI thread, so we dot care multi-thread case.
    private boolean stopRecording(boolean needSaveVideo) {
        LogHelper.i(TAG, "[stopRecording]+");
        if (mStartRecordingAsyncTask == null) {
            LogHelper.w(TAG, "[stopRecording]- recording not started!");
            return false;
        }
        if (mStopRecordingAsyncTask != null) {
            LogHelper.w(TAG, "[stopRecording]- skip for recording is stopping!");
            return false;
        }

        mStartRecordingAsyncTask.cancel(false);
        mStartRecordingAsyncTask.blockUntilStartingDone();
        mStartRecordingAsyncTask = null;

        mStopRecordingAsyncTask = new StopRecordingAsyncTask();
        updateModeState(PipModeState.PIP_MODE_STATUS_STOP_RECORDING);
        mStopRecordingAsyncTask.execute(needSaveVideo);
        LogHelper.d(TAG, "[stopRecording]-");
        return true;
    }

    private ContentValues createVideoContentValues() {
        ContentValues values = new ContentValues();
        long dateTaken = System.currentTimeMillis();
        String title = mPipVideoHelper.createVideoFileTitle(dateTaken);
        String fileName = mPipVideoHelper.createVideoFileName(title, mCurrentProfile.fileFormat);
        mVideoFilePath = mCameraContext.getStorageService().getFileDirectory() + '/' + fileName;
        long duration = mPipVideoHelper.getVideoDuration(mPipVideoHelper.getVideoTempFilePath());
        String mime = mPipVideoHelper.convertOutputFormatToMimeType(mCurrentProfile.fileFormat);
        values.put(MediaStore.Video.Media.DURATION, duration);
        values.put(MediaStore.Video.Media.TITLE, title);
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Video.Media.MIME_TYPE, mime);
        values.put(MediaStore.Video.Media.DATA, mVideoFilePath);
        values.put(MediaStore.Video.Media.WIDTH, mCurrentProfile.videoFrameWidth);
        values.put(MediaStore.Video.Media.HEIGHT, mCurrentProfile.videoFrameHeight);
        values.put(MediaStore.Video.Media.RESOLUTION,
                mCurrentProfile.videoFrameWidth + "x" + mCurrentProfile.videoFrameHeight);
        values.put(MediaStore.Video.Media.SIZE,
                mPipVideoHelper.getVideoSize(mPipVideoHelper.getVideoTempFilePath()));
        Location location = mCameraContext.getLocation();
        if (location != null) {
            values.put(MediaStore.Video.Media.LATITUDE, location.getLatitude());
            values.put(MediaStore.Video.Media.LONGITUDE, location.getLongitude());
        }
        if (CameraUtil.isColumnExistInDB(mApp.getActivity(),
                CameraUtil.TableList.VIDEO_TABLE, "orientation")) {
            values.put("orientation", mRecordingRotation);
        }
        return values;
    }

    private ContentValues createPhotoContentValues(byte[] jpegData) {
        ContentValues values = new ContentValues();
        long dateTaken = System.currentTimeMillis();
        String title = mPhotoHelper.createPhotoFileTitle(dateTaken);
        String fileName = mPhotoHelper.createPhotoFileName(title);
        String path = mICameraContext.getStorageService().getFileDirectory() + '/' + fileName;
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        Size exifSize = CameraUtil.getSizeFromExif(jpegData);
        values.put(MediaStore.Images.ImageColumns.WIDTH, exifSize.getWidth());
        values.put(MediaStore.Images.ImageColumns.HEIGHT, exifSize.getHeight());
        // gpu will do rotate, so always set to 0
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, 0);
        values.put(MediaStore.Images.ImageColumns.DATA, path);
        Location location = mICameraContext.getLocation();
        if (location != null) {
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location.getLatitude());
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location.getLongitude());
        }
        return values;
    }

    /**
     * Start recording async task.
     */
    private class StartRecordingAsyncTask extends AsyncTask<Boolean, Void, Boolean> {
        private final ConditionVariable mStartRecordingCondition = new ConditionVariable();

        StartRecordingAsyncTask() {
            mStartRecordingCondition.open();
        }

        /**
         * When recording is starting, block until start done.
         */
        public void blockUntilStartingDone() {
            mStartRecordingCondition.block();
        }

        @Override
        protected void onPreExecute() {
            LogHelper.d(TAG, "[StartRecordingAsyncTask.onPreExecute]");
            // mStartRecordingCondition.close();
            postRecordingStatus(true);
            mApp.enableKeepScreenOn(true);
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {
            boolean fail = false;
            LogHelper.d(TAG, "[StartRecordingAsyncTask] doInBackground+");
            mStartRecordingCondition.close();
            getSettingController(mBottomCameraId).postRestriction(
                    PipVideoCombination.getRecordingStatusRelation(true));
            mPipDevice.requestChangeSettingValue(mBottomCameraId);

            mPipVideoHelper.pauseAudioPlayBack(mApp);
            if (!prepareMediaRecorder()) {
                fail = true;
                return fail;
            }
            mPipController.prepareRecording();
            mPipController.setRecordingSurface(mMediaRecorder.getSurface(), 0);
            mCameraContext.getSoundPlayback().play(ISoundPlayback.START_VIDEO_RECORDING);
            mPipController.startPushVideoBuffer();
            try {
                if (mMediaRecorder != null) {
                    mMediaRecorder.start();
                }
            } catch (RuntimeException e) {
                LogHelper.e(TAG, "Media recorder start with runtime exception!");
                releaseMediaRecorder(true);
                postRecordingStatus(false);
                mStartRecordingCondition.open();
                fail = true;
                return fail;
            }
            mStartRecordingCondition.open();
            LogHelper.d(TAG, "[StartRecordingAsyncTask] doInBackground-");
            return fail;
        }

        @Override
        protected void onPostExecute(Boolean fail) {
            super.onPostExecute(fail);
            LogHelper.d(TAG, "[onPostExecute] start recording.");
            mVideoUi.updateOrientation(mApp.getGSensorOrientation());
            if (fail) {
                LogHelper.w(TAG, "[onPostExecute] start recording fail,need update ui.");
                mApp.getAppUi().applyAllUIEnabled(true);
                mApp.getAppUi().applyAllUIVisibility(View.VISIBLE);
            } else {
                //If we can not receive first frame, begin to start ASAP.
                if (!MediaRecorderEx.isCanUseParametersExtra()) {
                    mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_RECORDING);
                    updateModeState(PipModeState.PIP_MODE_STATUS_RECORDING);
                }
            }
        }
    }

    /**
     * Stop recording async task.
     */
    private class StopRecordingAsyncTask extends AsyncTask<Boolean, Void, Boolean> {
        private final ConditionVariable mStopRecordingCondition = new ConditionVariable();
        StopRecordingAsyncTask() {
            mStopRecordingCondition.close();
        }

        /**
         * When recording is starting, block until start done.
         */
        public void blockUntilStartingDone() {
            mStopRecordingCondition.block();
        }

        @Override
        protected void onPreExecute() {
            LogHelper.d(TAG, "[StopRecordingAsyncTask.onPreExecute]");
            mStopRecordingCondition.close();
            mApp.getAppUi().showSavingDialog(null, true);
            mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_PREVIEW);
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {
            mPipController.stopPushVideoBuffer();
            boolean fail = false;
            try {
                if (mMediaRecorder != null) {
                    mMediaRecorder.stop();
                }
            } catch (RuntimeException e) {
                LogHelper.e(TAG, "stop fail",  e);
                fail = true;
            } finally {
                releaseMediaRecorder(fail);
                mPipVideoHelper.releaseAudioFocus(mApp);
                if (booleans[0] && !fail) {
                    ContentValues contentValues = createVideoContentValues();
                    mCameraContext.getMediaSaver().addSaveRequest(
                            contentValues,
                            mPipVideoHelper.getVideoTempFilePath(),
                            mVideoFileSavedListener);
                    mCameraContext.getSoundPlayback().play(ISoundPlayback.STOP_VIDEO_RECORDING);
                }
                postRecordingStatus(false);
                getSettingController(mBottomCameraId).postRestriction(
                        PipVideoCombination.getRecordingStatusRelation(false));
                mPipDevice.requestChangeSettingValue(mBottomCameraId);
                return fail;
            }
        }

        @Override
        protected void onPostExecute(Boolean fail) {
            LogHelper.d(TAG, "[onPostExecute] stop recording fail:" + fail);
            mApp.enableKeepScreenOn(false);
            if (fail) {
                mApp.getAppUi().applyAllUIEnabled(true);
                mApp.getAppUi().hideSavingDialog();
                mApp.getAppUi().applyAllUIVisibility(View.VISIBLE);
                updateModeState(PipModeState.PIP_MODE_STATUS_PREVIEWING);
            }
            mRecordingPaused = false;
            mStopRecordingCondition.open();
        }
    }

    /**
     * Pip media saver listener.
     */
    private class PipMediaSaverListener implements MediaSaver.MediaSaverListener {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.i(TAG, "[onFileSaved]");
            IAppUi appUi = mApp.getAppUi();
            Bitmap bitmap = BitmapCreator.createBitmapFromVideo(
                    mVideoFilePath,
                    appUi.getThumbnailViewWidth());
            if (bitmap != null) {
                appUi.updateThumbnail(bitmap);
            }
            mApp.notifyNewMedia(uri, true);
            appUi.applyAllUIEnabled(true);
            appUi.hideSavingDialog();
            appUi.applyAllUIVisibility(View.VISIBLE);
            updateModeState(PipModeState.PIP_MODE_STATUS_PREVIEWING);
        }
    }

    private IVideoUI.UISpec configUISpec() {
        IVideoUI.UISpec spec = new IVideoUI.UISpec();
        spec.isSupportedPause = true;
        spec.recordingTotalSize = 0;
        spec.stopListener = mStopRecordingListener;
        spec.isSupportedVss = true;
        spec.vssListener = mVssListener;
        spec.pauseResumeListener = mPauseResumeListener;
        return spec;
    }

    private void postRecordingStatus(boolean recoding) {
        String statusStr = recoding ? "recording" : "preview";
        mCameraContext.getStatusMonitor(mBottomCameraId)
                .getStatusResponder(KEY_VIDEO_STATUS)
                .statusChanged(KEY_VIDEO_STATUS, statusStr);
        mCameraContext.getStatusMonitor(mTopCameraId)
                .getStatusResponder(KEY_VIDEO_STATUS)
                .statusChanged(KEY_VIDEO_STATUS, statusStr);
        if (recoding) {
            updateModeDeviceState(MODE_DEVICE_STATE_RECORDING);
        } else {
            updateModeDeviceState(MODE_DEVICE_STATE_PREVIEWING);
        }
    }

    private final Listener mVideoQualityChangeListener = new Listener() {
        @Override
        public void onQualityChanged(String newQuality) {
            mCurrentVideoQuality = newQuality;
            updatePreviewSize(getPreviewSize());
        }
    };

    private final OnClickListener mStopRecordingListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(TAG, "stop recording clicked!");
            stopRecording(true);
        }
    };

    private final OnClickListener mVssListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(TAG, "vss clicked!");
            if (mVssCapturing) {
                LogHelper.w(TAG, "vss is capturing, ignore this trigger.");
                return;
            }
            mVssCapturing = true;
            int gSensorOrientation = mIApp.getGSensorOrientation();
            if (gSensorOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                gSensorOrientation = 0;
            }
            mPipController.takeVideoSnapshot(gSensorOrientation, true);
            mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_CAPTURE, null);
        }
    };

    private final OnClickListener mPauseResumeListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(TAG, "pause resume clicked!");
            if (mRecordingPaused) {
                resumeRecording();
                updateModeState(PipModeState.PIP_MODE_STATUS_RECORDING);
            } else {
                updateModeState(PipModeState.PIP_MODE_STATUS_PAUSE_RECORDING);
                pauseRecording();
            }
        }
    };

    private final OnInfoListener mOnInfoListener = new OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            LogHelper.d(TAG, "[onInfo] what = " + what + " extra = " + extra);
            switch (what) {
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                    stopRecording(true);
                    break;
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                    stopRecording(true);
                    mVideoUi.showInfo(IVideoUI.REACH_SIZE_LIMIT);
                    break;
                case PipVideoHelper.MEDIA_INFO_START_TIMER:
                    mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_RECORDING);
                    updateModeState(PipModeState.PIP_MODE_STATUS_RECORDING);
                    mIApp.getAppUi().setUIEnabled(mIApp.getAppUi().SHUTTER_BUTTON, true);
                    break;
                case PipVideoHelper.MEDIA_INFO_WRITE_SLOW:
                    mVideoUi.showInfo(IVideoUI.VIDEO_BAD_PERFORMANCE_AUTO_STOP);
                    stopRecording(true);
                    break;
                case PipVideoHelper.MEDIA_INFO_FPS_ADJUSTED:
                case PipVideoHelper.MEDIA_INFO_BITRATE_ADJUSTED:
                    mVideoUi.showInfo(IVideoUI.BAD_PERFORMANCE_DROP_QUALITY);
                    break;
                default:
                    break;
            }
        }
    };

    private final OnErrorListener mOnErrorListener = new OnErrorListener() {
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            LogHelper.e(TAG, "[onError] what = " + what + ". extra = " + extra);
            if (MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN == what
                    || PipVideoHelper.MEDIA_ENCODER_ERROR == extra) {
                stopRecording(true);
            }
        }
    };

    private final IStorageStateListener mStorageStateListener = new IStorageStateListener() {
        @Override
        public void onStateChanged(int storageState, Intent intent) {
            if (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
                LogHelper.d(TAG, "[onStateChanged] ACTION_MEDIA_EJECT");
                stopRecording(false);
            }
        }
    };
}