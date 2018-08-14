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
 * MediaTek Inc. (C) 2016. All rights reserved.
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

package com.mediatek.camera.tests.common.video;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.test.ActivityInstrumentationTestCase2;

import com.mediatek.camera.CameraActivity;
import com.mediatek.camera.tests.Log;
import com.mediatek.camera.tests.Utils;

import junit.framework.Assert;

/**
 * This class is used for test intent video case.
 */

public class VideoIntentTest extends ActivityInstrumentationTestCase2 {
    private static final String TAG = VideoIntentTest.class.getSimpleName();
    //source ID
    private static final String VIDEO_STOP_BUTTON_ID = "com.mediatek.camera:id/video_stop_shutter";
    private static final String VIDEO_PAUSE_RESUME_ID = "com.mediatek.camera:id/btn_pause_resume";
    private static final String RECORDING_PROGRESS = "com.mediatek.camera:id/recording_progress";
    private static final String RECORDING_TIME = "com.mediatek.camera:id/recording_time";

    //Review resources.
    private static final String VIDEO_RETAKE = "com.mediatek.camera:id/btn_retake";
    private static final String VIDEO_SAVE = "com.mediatek.camera:id/btn_save";
    private static final String VIDEO_PLAY = "com.mediatek.camera:id/btn_play";

    //Content Description
    private static final String VIDEO_START_BUTTON_DESCRIPTION = "Video";

    //Video normal UI object.
    private UiObject mVideoStartButtonObject;
    private UiObject mVideoStopButtonObject;
    private UiObject mVideoResumeButtonObject;
    private UiObject mVideoRecordingProgress;
    private UiObject mRecordingTime;

    //Review UI Object
    private UiObject mRetake;
    private UiObject mSave;
    private UiObject mPlay;

    //Intent test
    private static final long MAX_VIDEO_SIZE_BYTE = 500000;
    private static final int MAX_DURATION_SECONDS = 5;
    private static final int MAX_WAIT_UI_READY_TIME_MS = 60000;
    private Uri mUri;
    private Intent mIntent;
    private Instrumentation mInstrumentation;
    private Activity mActivity;
    private UiDevice mUiDevice;

    /**
     * Creator for test base.
     */
    public VideoIntentTest() {
        super(CameraActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mUiDevice = UiDevice.getInstance(mInstrumentation);
        //launch video by intent to make sure current is in video mode.
        initializeIntent();
        startVideoIntent();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * This case is used for test normal recording function,
     * if there is no exception means the basic function is work well.
     * Q:why don't need check the video number?
     * A:the reason is in this simple steps(StartRecording() ->StopRecording()), the video have
     * not insert to the DB, so don't need check the video number.
     *
     * Step 1: Press the video shutter button to start;
     * Step 2: When started, will press the stop button to stop recording.
     * @throws UiObjectNotFoundException if the object not found will throw this exception.
     */
    public void testRecording() throws UiObjectNotFoundException {
        long startTime = System.currentTimeMillis();
        startRecording();
        stopRecording();
        Log.i(TAG, "[testRecording] spendTime = " + (System.currentTimeMillis() - startTime));
    }

    /**
     * This case is test pause recording function is correct.
     * don't have only exception means work well.
     * @throws UiObjectNotFoundException if the object not found will throw this exception.
     */
    public void testPauseVideoRecording() throws UiObjectNotFoundException {
        long startTime = getCurrentTime();
        startRecording();
        //TODO current can't get the pause button object.
        pauseRecording();
        Log.i(TAG, "[testPauseVideoRecording] spendTime = " + (getCurrentTime() - startTime));
    }

    /**
     * This case is test when start recording, check the recording time and progress bar is showing.
     * if the ui is not showing will be fail.
     * @throws UiObjectNotFoundException if the object not found will throw this exception.
     */
    public void testStartRecordingUI() throws UiObjectNotFoundException {
        long startTime = getCurrentTime();
        startRecording();
        boolean uiNormal = recordingProgressIsShowing() && recordingTimeIsShowing();
        //TODO current progress bar and recording time can not find in UI AutoMator view.
        Log.i(TAG, "[testStartRecordingUI] spendTime: " + (getCurrentTime() - startTime));
    }

    /**
     * Test the UI is correct when stop recording,such as the retake, save and play button is
     * showing, if some one don't show will be fail.
     * @throws UiObjectNotFoundException if the object not found will throw this exception.
     */
    public void testStopRecordingUI() throws UiObjectNotFoundException {
        long time = getCurrentTime();
        startRecording();
        stopRecording();
        boolean isOk = retakeButtonIsShowing() && saveButtonIsShowing() && playButtonIsShowing();
        //TODO how to get all the button info.
        Log.i(TAG, "[testStopRecordingUI] spendTime: " + (getCurrentTime() - time));
    }

    /**
     * Test save video function is work well.
     * at last need check current activity is finished, and the duration is not null.
     * @throws UiObjectNotFoundException if the object not found will throw this exception.
     */
    public void testSaveVideo() throws UiObjectNotFoundException {
        long startTime = getCurrentTime();
        startRecording();
        stopRecording();
        saveButtonIsShowing();
        onSaveButtonClicked();
        verify((CameraActivity) mActivity, mUri);
        Log.i(TAG, "[testSaveVideo] spendTime: " + (getCurrentTime() - startTime));
    }

    /**
     * Test retake function is work well when finish take video.
     * @throws UiObjectNotFoundException if the object not found will throw this exception.
     */
    public void testRetakeVideo() throws UiObjectNotFoundException {
        long startTime = getCurrentTime();
        startRecording();
        stopRecording();
        retakeButtonIsShowing();
        onRetakeButtonClicked();
        assertFalse("retake button/save button/play button can not show", retakeButtonIsShowing()
                || saveButtonIsShowing() || playButtonIsShowing());
        Log.i(TAG, "[testRetakeVideo] spendTime: " + (getCurrentTime() - startTime));
    }

    private void initializeIntent() {
        String path = Environment.getExternalStorageDirectory().toString() + "video.tmp";
        mUri = Uri.parse("content://" + path);
        mIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, MAX_VIDEO_SIZE_BYTE);
        mIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, MAX_DURATION_SECONDS);
        mIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // use low quality to speed up
        mIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
    }

    private void startVideoIntent() {
        setActivityIntent(mIntent);
        mActivity = getActivity();
    }

    private void startRecording() throws UiObjectNotFoundException {
        long startTime = getCurrentTime();
        mVideoStartButtonObject =
                new UiObject(new UiSelector().description(VIDEO_START_BUTTON_DESCRIPTION));
        if (mVideoStartButtonObject.exists() && mVideoStartButtonObject.isClickable()) {
            mVideoStartButtonObject.clickAndWaitForNewWindow();
        }
        Log.d(TAG, "[startRecording] spendTime = " + (getCurrentTime() - startTime));
    }

    private void stopRecording() throws UiObjectNotFoundException {
        long startTime = getCurrentTime();
        mVideoStopButtonObject = new UiObject(new UiSelector().resourceId(VIDEO_STOP_BUTTON_ID));
        if (mVideoStopButtonObject.exists() && mVideoStopButtonObject.isClickable()) {
            mVideoStopButtonObject.clickAndWaitForNewWindow();
        }
        Log.d(TAG, "[stopRecording] spendTime = " + (getCurrentTime() - startTime));
    }

    private void pauseRecording() throws UiObjectNotFoundException {
        long startTime = getCurrentTime();
        mVideoResumeButtonObject = new UiObject(new UiSelector().resourceId(VIDEO_PAUSE_RESUME_ID));
        if (mVideoResumeButtonObject.exists() && mVideoResumeButtonObject.isClickable()) {
            mVideoResumeButtonObject.clickAndWaitForNewWindow();
        }
        Log.d(TAG, "[pauseRecording] spendTime = " + (getCurrentTime() - startTime));
    }

    private boolean recordingTimeIsShowing() {
        Log.d(TAG, "[recordingTimeIsShowing]");
        boolean correct = false;
        mRecordingTime = new UiObject(new UiSelector().resourceId(RECORDING_TIME));
        if (mRecordingTime.exists()) {
            Log.d(TAG, "[recordingTimeIsShowing] true");
            correct = true;
        }
        return correct;
    }

    private boolean recordingProgressIsShowing() {
        Log.d(TAG, "[recordingProgressIsShowing]");
        boolean isShowing = false;
        mVideoRecordingProgress =
                new UiObject(new UiSelector().resourceId(RECORDING_PROGRESS));
        if (mVideoRecordingProgress.exists()) {
            Log.d(TAG, "[recordingProgressIsShowing] true");
            isShowing = true;
        }
        return isShowing;
    }

    private boolean startButtonIsShowing() {
        boolean isShowing = false;
        if (mVideoStartButtonObject == null) {
            mVideoStartButtonObject =
                    new UiObject(new UiSelector().description(VIDEO_START_BUTTON_DESCRIPTION));
        }
        if (mVideoStartButtonObject.exists()) {
            Log.d(TAG, "[startButtonIsShowing] true");
            isShowing = true;
        }
        return isShowing;
    }

    private boolean retakeButtonIsShowing() {
        boolean isShowing = false;
        mRetake = new UiObject(new UiSelector().resourceId(VIDEO_RETAKE));
        if (mRetake.exists()) {
            Log.d(TAG, "[retakeButtonIsShowing] true");
            isShowing = true;
        }
        return isShowing;
    }

    private boolean saveButtonIsShowing() {
        boolean isShowing = false;
        mSave = new UiObject(new UiSelector().resourceId(VIDEO_SAVE));
        if (mSave.exists()) {
            Log.d(TAG, "[saveButtonIsShowing] true");
            isShowing = true;
        }
        return isShowing;
    }

    private boolean playButtonIsShowing() {
        boolean isShowing = false;
        mPlay = new UiObject(new UiSelector().resourceId(VIDEO_PLAY));
        if (mPlay.exists()) {
            Log.d(TAG, "[playButtonIsShowing] true");
            isShowing = true;
        }
        return isShowing;
    }

    private void onPlayButtonClicked() throws UiObjectNotFoundException {
        if (mPlay.exists() && mPlay.isClickable()) {
            mPlay.clickAndWaitForNewWindow();
        }
    }

    private void onSaveButtonClicked() throws UiObjectNotFoundException {
        if (mSave.exists() && mSave.isClickable()) {
            mSave.clickAndWaitForNewWindow();
        }
    }

    private void onRetakeButtonClicked() throws UiObjectNotFoundException {
        if (mRetake.exists() && mRetake.isClickable()) {
            mRetake.clickAndWaitForNewWindow();
        }
    }

    private void verify(CameraActivity activity, Uri uri) {
        assertTrue(activity.isFinishing());
    }


    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private void assertStatusReady(Utils.Checker checker, int timeOutMs) {
        Assert.assertEquals(Utils.waitForTrueWithTimeOut(checker, timeOutMs), false);
    }

    private void checkButtonStatus(UiObject object, String resId) {
        ButtonVisibleChecker checker = new ButtonVisibleChecker(object, resId);
        assertStatusReady(checker, MAX_WAIT_UI_READY_TIME_MS);
    }
    /**
     * This class used for check the button status whether ready or not.
     */
    private class ButtonVisibleChecker implements Utils.Checker {

        private UiObject mUiObject;
        ButtonVisibleChecker(UiObject uiObject, String resId) {
            mUiObject = uiObject;
            if (mUiObject == null) {
                mUiObject = new UiObject(new UiSelector().resourceId(resId));
            }
        }

        /**
         * Check status is ready.
         *
         * @return true if status ready.
         */
        @Override
        public boolean check() {
            return mUiObject.exists();
        }
    }

    //end of intent video
}
