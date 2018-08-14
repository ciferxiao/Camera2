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
import com.mediatek.camera.tests.v3.annotation.module.StrobeTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.CapturedPhotoPictureSizeChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailChecker;
import com.mediatek.camera.tests.v3.observer.PhotoSavedObserver;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.FlashOperator;
import com.mediatek.camera.tests.v3.operator.PictureSizeOperator;
import com.mediatek.camera.tests.v3.operator.ZsdOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import org.junit.Test;

/**
 * Test capture with strobe off/on/auto. Test Run command : adb shell am instrument -e class com
 * .mediatek.camera.tests.v3.testcase.StrobeTestCase -w com.mediatek.camera.tests/android.support
 * .test.runner.AndroidJUnitRunner
 */
@FunctionTest
@CameraFeatureTest
@StrobeTest
public class StrobeTestCase extends BaseCameraTestCase {

    /**
     * Check photo can be taken successfully with flash on/auto/off, zsd on/off and screen size
     * between full screen size and standard size.
     */
    @Test
    public void testCapturePhoto() {
        // when support de-noise, zsd will be set as on dy default, there is not zsd item in
        // setting, so not add ZsdOperator when de-noise
        if (Utils.isDenoiseSupported()) {
            new MetaCase()
                    .addOperator(new FlashOperator())
                    .acrossBegin()
                    .addOperator(new ZsdOperator())
                    .acrossEnd()
                    .acrossBegin()
                    .addOperator(new PictureSizeOperator())
                    .acrossEnd()
                    .addChecker(new PreviewChecker())
                    .observeBegin(new PhotoSavedObserver(1))
                    .addOperator(new CapturePhotoOperator())
                    .observeEnd()
                    .addChecker(new PreviewChecker())
                    .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                    .addChecker(new CapturedPhotoPictureSizeChecker())
                    .run();
        } else {
            new MetaCase()
                    .addOperator(new FlashOperator())
                    .acrossBegin()
                    .addOperator(new PictureSizeOperator())
                    .acrossEnd()
                    .addChecker(new PreviewChecker())
                    .observeBegin(new PhotoSavedObserver(1))
                    .addOperator(new CapturePhotoOperator())
                    .observeEnd()
                    .addChecker(new PreviewChecker())
                    .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                    .addChecker(new CapturedPhotoPictureSizeChecker())
                    .run();
        }

    }
}
