/*
 * Copyright Statement:
 *
 *   This software/firmware and related documentation ("MediaTek Software") are
 *   protected under relevant copyright laws. The information contained herein is
 *   confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 *   the prior written permission of MediaTek inc. and/or its licensors, any
 *   reproduction, modification, use or disclosure of MediaTek Software, and
 *   information contained herein, in whole or in part, shall be strictly
 *   prohibited.
 *
 *   MediaTek Inc. (C) 2017. All rights reserved.
 *
 *   BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *   THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 *   RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 *   ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 *   WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 *   NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 *   RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *   INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 *   TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 *   RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 *   OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 *   SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 *   RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 *   STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 *   ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 *   RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 *   MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 *   CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *   The following software/firmware and/or related documentation ("MediaTek
 *   Software") have been modified by MediaTek Inc. All revisions are subject to
 *   any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.tests.v3.testcase;

import android.support.test.runner.AndroidJUnit4;

import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.tests.v3.annotation.group.CameraFeatureTest;
import com.mediatek.camera.tests.v3.annotation.module.MatrixDisplayTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.annotation.type.PerformanceTest;
import com.mediatek.camera.tests.v3.annotation.type.StabilityTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.MatrixDisplayClosedChecker;
import com.mediatek.camera.tests.v3.checker.MatrixDisplayIconChecker;
import com.mediatek.camera.tests.v3.checker.MatrixDisplayOpenedChecker;
import com.mediatek.camera.tests.v3.observer.MatrixDisplayEffectObserver;
import com.mediatek.camera.tests.v3.observer.MatrixDisplayFpsObserver;
import com.mediatek.camera.tests.v3.operator.BackToCameraOperator;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.ExitCameraOperator;
import com.mediatek.camera.tests.v3.operator.LaunchCameraOperator;
import com.mediatek.camera.tests.v3.operator.MatrixDisplayEffectSelectOperator;
import com.mediatek.camera.tests.v3.operator.MatrixDisplayOpeningOperator;
import com.mediatek.camera.tests.v3.operator.MatrixDisplayScrollOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByBackKeyOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByHomeKeyOperator;
import com.mediatek.camera.tests.v3.operator.SleepOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.SwitchPhotoVideoOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToPipPhotoModeOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@CameraFeatureTest
@MatrixDisplayTest
public class MatrixDisplayTestCase extends BaseCameraTestCase {
    private static final int SLEEP_TIME = 15;

    @Test
    @FunctionTest
    public void testSupportedStatusOnAPI1API2() {
        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                new MetaCase("TC_Camera_Matrix_Display_0001")
                        .addChecker(new MatrixDisplayIconChecker(),
                                MatrixDisplayIconChecker.INDEX_MATRIX_SHOW)
                        .run();
                break;

            case API2:
                new MetaCase("TC_Camera_Matrix_Display_0001")
                        .addChecker(new MatrixDisplayIconChecker(),
                                MatrixDisplayIconChecker.INDEX_MATRIX_SHOW)
                        .run();
                break;

            default:
                break;
        }
    }

    @Test
    @FunctionTest
    public void testDisableMatrixDisplayInUnspportedMode() {
        new MetaCase("TC_Camera_Matrix_Display_0002")
                .addOperator(new SwitchToPipPhotoModeOperator())
                .addChecker(new MatrixDisplayIconChecker(),
                        MatrixDisplayIconChecker.INDEX_MATRIX_HIDE)
                .run();
    }

    @Test
    @FunctionTest
    public void testOpenAndCloseMatrixDisplay() {
        new MetaCase("TC_Camera_Matrix_Display_0003, TC_Camera_Matrix_Display_0008")
                .addOperator(new SwitchCameraOperator())
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new BackToCameraOperator())
                .addChecker(new MatrixDisplayClosedChecker())
                .run();
    }

    @Test
    @FunctionTest
    public void testSelectEffect() {
        new MetaCase("TC_Camera_Matrix_Display_0004")
                .addOperator(new MatrixDisplayOpeningOperator())
                .acrossBegin()
                .observeBegin(new MatrixDisplayEffectObserver())
                .addOperator(new MatrixDisplayEffectSelectOperator())
                .addOperator(new SleepOperator(SLEEP_TIME / 5))
                .observeEnd()
                .acrossEnd()
                .run();
    }

    @Test
    @FunctionTest
    public void testScrollMatrixDisplay() {
        new MetaCase("TC_Camera_Matrix_Display_0006")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new MatrixDisplayScrollOperator())
                .addChecker(new MatrixDisplayClosedChecker())
                .run();
    }

    @Test
    @FunctionTest
    public void testDisableMatrixDisplayIn3rdParty() {
        new MetaCase("TC_Camera_Matrix_Display_0009")
                .addChecker(new MatrixDisplayIconChecker(),
                        MatrixDisplayIconChecker.INDEX_MATRIX_SHOW)
                .addOperator(new ExitCameraOperator())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_INTENT_PHOTO)
                .addChecker(new MatrixDisplayIconChecker(),
                        MatrixDisplayIconChecker.INDEX_MATRIX_HIDE)
                .addOperator(new ExitCameraOperator())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_INTENT_VIDEO)
                .addChecker(new MatrixDisplayIconChecker(),
                        MatrixDisplayIconChecker.INDEX_MATRIX_HIDE)
                .run();
    }

    @Test
    @PerformanceTest
    public void testMatrixDisplayFps() {
        new MetaCase("TC_Camera_Matrix_Display_0011")
                .observeBegin(new MatrixDisplayFpsObserver())
                .addOperator(new MatrixDisplayOpeningOperator())
                .addOperator(new SleepOperator(SLEEP_TIME))
                .observeEnd()
                .run();
    }

    @Test
    @FunctionTest
    public void testOpenAndCloseMatrixDisplayInVideoMode() {
        new MetaCase("TC_Camera_Matrix_Display_0013, TC_Camera_Matrix_Display_0014")
                .addOperator(new SwitchPhotoVideoOperator(), SwitchPhotoVideoOperator.INDEX_VIDEO)
                .acrossBegin()
                .addOperator(new SwitchCameraOperator())
                .acrossEnd()
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new BackToCameraOperator())
                .addChecker(new MatrixDisplayClosedChecker())
                .run();
    }

    @Test
    @FunctionTest
    public void testOpenMatrixDisplayInDifferentPreviewRatio() {
        new MetaCase()
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new BackToCameraOperator())
                .addChecker(new MatrixDisplayClosedChecker())
                .run();
    }

    @Test
    @FunctionTest
    public void testEffectIsWorkAfterPauseAndResumed() {
        new MetaCase("TC_Camera_Matrix_Display_0015")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new MatrixDisplayEffectSelectOperator(), 6)
                .addChecker(new MatrixDisplayClosedChecker())
                .observeBegin(new MatrixDisplayEffectObserver(), 6)
                .addOperator(new PauseResumeByHomeKeyOperator())
                .observeEnd()
                .run();
    }

    @Test
    @FunctionTest
    public void testOpenMatrixDisplayAndQuicklyClickShutter() {
        new MetaCase("TC_Camera_Matrix_Display_0016")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addOperator(new CapturePhotoOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .run();
    }

    @Test
    @StabilityTest
    public void testRepeatingOpenAndCloseMatrixDisplay() {
        // repeating 3000 times
        new MetaCase("TC_Camera_Matrix_Display_0020")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new BackToCameraOperator())
                .runForTimes(Utils.STABILITY_REPEAT_TIMES);
    }

    @Test
    @FunctionTest
    public void testEffectAsyncBetweenCamera() {
        new MetaCase("TC_Camera_Matrix_Display_0022")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new MatrixDisplayEffectSelectOperator(), 6)
                .observeBegin(new MatrixDisplayEffectObserver(), 0)
                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_FRONT)
                .observeEnd()
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new MatrixDisplayEffectSelectOperator(), 7)
                .observeBegin(new MatrixDisplayEffectObserver(), 6)
                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_BACK)
                .observeEnd()
                .run();
    }

    @Test
    @StabilityTest
    public void testRepeatingPauseAndResumeMatrixDisplay() {
        // repeating 3000 times
        new MetaCase("TC_Camera_Matrix_Display_0024")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new PauseResumeByHomeKeyOperator())
                .runForTimes(Utils.STABILITY_REPEAT_TIMES);
    }

    @Test
    @FunctionTest
    public void testMatrixDisplayDefaultValue() {
        new MetaCase("TC_Camera_Matrix_Display_0025")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .observeBegin(new MatrixDisplayEffectObserver(), 0)
                .addOperator(new BackToCameraOperator())
                .observeEnd()
                .run();
    }

    @Test
    @FunctionTest
    public void testEffectTurnToNoneAfterDestroyed() {
        new MetaCase("TC_Camera_Matrix_Display_0026")
                .addOperator(new MatrixDisplayOpeningOperator())
                .addChecker(new MatrixDisplayOpenedChecker())
                .addOperator(new MatrixDisplayEffectSelectOperator(), 6)
                .addChecker(new MatrixDisplayClosedChecker())
                .observeBegin(new MatrixDisplayEffectObserver(), 0)
                .addOperator(new PauseResumeByBackKeyOperator())
                .observeEnd()
                .run();
    }
}
