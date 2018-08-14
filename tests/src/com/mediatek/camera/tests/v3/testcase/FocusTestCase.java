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
 * MediaTek Inc. (C) 2017. All rights reserved.
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
package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.CameraFeatureTest;
import com.mediatek.camera.tests.v3.annotation.module.ContinuousFocusTest;
import com.mediatek.camera.tests.v3.annotation.module.TouchFocusTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.checker.CameraExitedChecker;
import com.mediatek.camera.tests.v3.checker.CameraLaunchedChecker;
import com.mediatek.camera.tests.v3.checker.FocusUiObserver;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.observer.ContinuousFocusStateObserver;
import com.mediatek.camera.tests.v3.observer.FocusStateObserver;
import com.mediatek.camera.tests.v3.observer.PhotoSavedObserver;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.ExitCameraOperator;
import com.mediatek.camera.tests.v3.operator.LaunchCameraOperator;
import com.mediatek.camera.tests.v3.operator.OnSingleTapUpOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByHomeKeyOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.SwitchPhotoVideoOperator;
import com.mediatek.camera.tests.v3.operator.ZsdOperator;

import org.junit.Test;

/**
 * Test focus state is right.
 */
@FunctionTest
@CameraFeatureTest
public class FocusTestCase extends BaseCameraTestCase {

    /**
     * Check continuous focus state and UI is right in both back and front camera.
     */
    @Test
    @ContinuousFocusTest
    public void testCafPicStateAndUiBackAndFront() {
        new MetaCase("TEMP_SETTING_0004,TEMP_SETTING_0012,TEMP_SETTING_0013,TEMP_SETTING_0014")
                .addOperator(new ExitCameraOperator())
                .addChecker(new CameraExitedChecker())
                .observeBegin(new ContinuousFocusStateObserver(true))
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_CAF_UI)
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_NORMAL)
                .addChecker(new CameraLaunchedChecker(), CameraLaunchedChecker.INDEX_NORMAL)
                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_BACK)
                .addChecker(new PreviewChecker())
                .observeEnd()
                .observeEnd()
                .observeBegin(new ContinuousFocusStateObserver(true))
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_CAF_UI)
                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_FRONT)
                .addChecker(new PreviewChecker())
                .observeEnd()
                .observeEnd()
                .run();
    }

    /**
     * Check continuous video focus state and UI is right.
     */
    @Test
    @ContinuousFocusTest
    public void testContinuousVideoFocusStateAndUi() {
        new MetaCase("TEMP_SETTING_0001,TEMP_SETTING_0002,TEMP_SETTING_0004")
                .addOperator(new ExitCameraOperator())
                .addChecker(new CameraExitedChecker())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_NORMAL)
                .addChecker(new CameraLaunchedChecker(), CameraLaunchedChecker.INDEX_NORMAL)
                .observeBegin(new ContinuousFocusStateObserver(false))
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_CAF_UI)
                .addOperator(new SwitchPhotoVideoOperator(), SwitchPhotoVideoOperator.INDEX_VIDEO)
                .observeEnd()
                .observeEnd()
                .run();
    }

    /**
     * Check touch focus state and UI is right.
     */
    @Test
    @TouchFocusTest
    public void testTouchFocusStateAndUi() {
        Operator touchFocusOperator = new OnSingleTapUpOperator();
        new MetaCase("TEMP_SETTING_0001,TEMP_SETTING_0002,TEMP_SETTING_0004")
                .observeBegin(new FocusStateObserver())
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_TAF_UI)
                .addOperator(touchFocusOperator)
                .observeEnd()
                .observeEnd()
                .run();
    }

    /**
     * Check can do touch focus after take picture.
     */
    @Test
    @TouchFocusTest
    public void testTouchFocusAfterTakePic() {
        Operator touchFocusOperator = new OnSingleTapUpOperator();
        new MetaCase("TC_Camera_Focus_0028")
                .observeBegin(new FocusStateObserver())
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_TAF_UI)
                .addOperator(touchFocusOperator, 4)
                .observeEnd()
                .observeEnd()
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .addChecker(new PreviewChecker())
                .observeEnd()
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_TAF_UI)
                .addOperator(touchFocusOperator, 8)
                .observeEnd()
                .run();
    }

    /**
     * Step1:Open camera and do TAF.
     * Step2:Slide to video mode and do TAF.
     * Step3:Press home key and open camera again,do TAF.
     * Step4:Slide to photo mode and do TAF.
     * Check TAF should work normally when press home and open camera again.
     */
    @Test
    @TouchFocusTest
    public void testTouchFocusValidWhenOpenCameraAgain() {
        Operator touchFocusOperator = new OnSingleTapUpOperator();
        new MetaCase("TEMP_SETTING_0016")
                .addOperator(new SwitchPhotoVideoOperator(), SwitchPhotoVideoOperator.INDEX_PHOTO)
                .observeBegin(new FocusStateObserver())
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_TAF_UI)
                .addOperator(touchFocusOperator, 0)
                .observeEnd()
                .observeEnd()
                .addOperator(new SwitchPhotoVideoOperator(), SwitchPhotoVideoOperator.INDEX_VIDEO)
                .observeBegin(new FocusStateObserver())
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_TAF_UI)
                .addOperator(touchFocusOperator, 0)
                .observeEnd()
                .observeEnd()
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PreviewChecker())
                .runForTimes(2);
    }

    /**
     * Check can take picture successfully after TAF with zsd on.
     */
    @Test
    @TouchFocusTest
    public void testTakePicAfterTouchFocusWithZsdOn() {
        Operator touchFocusOperator = new OnSingleTapUpOperator();
        int indexZsdOn = 0;
        int takePhotoCount = 10;
        new MetaCase("TC_Camera_Focus_0024")
                .addOperator(new ZsdOperator(), indexZsdOn)
                .observeBegin(new FocusStateObserver())
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_TAF_UI)
                .addOperator(touchFocusOperator, 8)
                .observeEnd()
                .observeEnd()
                .repeatBegin(takePhotoCount)
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .addChecker(new PreviewChecker())
                .observeEnd()
                .repeatEnd()
                .run();
    }

    /**
     * Test no error happens pause and resume camera after touch focus once.
     */
    @Test
    @TouchFocusTest
    public void testStressPauseResumeAfterTaf() {
        Operator touchFocusOperator = new OnSingleTapUpOperator();
        int pauseResumeTimes = 100;
        new MetaCase("TEMP_SETTING_0022")
                .observeBegin(new FocusStateObserver())
                .observeBegin(new FocusUiObserver(), FocusUiObserver.INDEX_HAS_TAF_UI)
                .addOperator(touchFocusOperator)
                .observeEnd()
                .observeEnd()
                .addChecker(new PreviewChecker())
                .repeatBegin(pauseResumeTimes)
                .addOperator(new PauseResumeByHomeKeyOperator())
                .repeatEnd()
                .run();
    }
}
