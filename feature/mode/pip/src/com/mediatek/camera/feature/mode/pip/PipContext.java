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

package com.mediatek.camera.feature.mode.pip;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory.CameraApi;
import com.mediatek.camera.feature.mode.pip.device.IPipDevice;
import com.mediatek.camera.feature.mode.pip.device.v1.PipDevice;
import com.mediatek.camera.feature.mode.pip.device.v2.PipDevice2;

import javax.annotation.Nonnull;

/**
 * PipContext used to manage pip view, device, renderer instances.
 * Keep these instance are singleton in the same Context.
 */
public class PipContext {
    private static final Tag TAG = new Tag(PipContext.class.getSimpleName());
    private PipController mPipController;
    private IPipDevice mPipDevice;

    /**
     * Get {@link IPipDevice} instance.
     *
     * @param app the camera app.
     * @param cameraContext the camera context.
     * @param cameraApi the camera api.
     * @return an instance of IPipDevice.
     */
    public IPipDevice getPipDevice(@Nonnull IApp app,
                                   @Nonnull ICameraContext cameraContext,
                                   @Nonnull CameraApi cameraApi) {
        LogHelper.d(TAG, "[getPipDevice] camera api:" + cameraApi);
        if (CameraApi.API1 == cameraApi) {
            mPipDevice = new PipDevice(
                    app,
                    cameraContext,
                    getPipController(app).getPipCaptureWrapper());
        } else {
            mPipDevice = new PipDevice2(
                    app,
                    cameraContext,
                    getPipController(app).getPipCaptureWrapper()
            );
        }
        return mPipDevice;
    }

    /**
     * Get {@link PipController} instance.
     * @param app the app.
     * @return an instance of {@link PipController}.
     */
    public PipController getPipController(@Nonnull IApp app) {
        if (mPipController == null) {
            mPipController = new PipController();
            mPipController.init(app);
        }
        return mPipController;
    }

    /**
     * Release pip context.
     */
    public void release() {
        LogHelper.d(TAG, "[release]");
        if (mPipController != null) {
            mPipController.unInit();
            mPipController = null;
        }
        if (mPipDevice != null) {
            mPipDevice.release();
            mPipDevice = null;
        }
    }
}