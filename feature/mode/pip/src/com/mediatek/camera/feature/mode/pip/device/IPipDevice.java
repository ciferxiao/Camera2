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

package com.mediatek.camera.feature.mode.pip.device;

import android.support.annotation.NonNull;

import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.pipwrapping.SurfaceTextureWrapper;

import java.util.ArrayList;

import javax.annotation.Nonnull;

/**
 * Pip device manager, will have API1 and API2 implementation.
 */
public interface IPipDevice {

    /**
     * This interface used to notify mode device's state.
     */
    interface IPipDeviceCallback {
        /**
         * Notified when one camera opened.
         * @param cameraId which camera is opened.
         */
        void onCameraOpened(String cameraId);
        /**
         * Notified call stop preview immediately.
         * @param cameraId which camera is opened.
         */
        void afterStopPreview(String cameraId);
    }

    /**
     * Open two cameras in sequence.
     *
     * @param firstSettingManager first camera's setting manager instance.
     * @param secondSettingManager second camera's setting manager instance.
     */
    void openCamera(@Nonnull ISettingManager firstSettingManager,
                    @Nonnull ISettingManager secondSettingManager);

    /**
     * Update current mode type, photo or video.
     * @param modeType current mode type.
     */
    void updateModeType(@Nonnull ICameraMode.ModeType modeType);

    /**
     * Set pip device callback to receive device state.
     *
     * @param pipDeviceCallback pip device callback.
     */
    void setPipDeviceCallback(@Nonnull IPipDeviceCallback pipDeviceCallback);

    /**
     * Get supported preview sizes.
     *
     * @param previewTarget SurfaceTexture or SurfaceHolder instance.
     * @param cameraId the camera id want to query supported preview sizes.
     * @return the supported preview size list.
     */
    ArrayList<Size> getSupportedPreviewSizes(Object previewTarget, @Nonnull String cameraId);

    /**
     * Close camera by id, if want to close two cameras, need call this method twice.
     *
     * @param cameraId the specified camera wanted to be closed.
     */
    void closeCamera(@NonNull String cameraId);

    /**
     * Start preview with SurfaceTexture.
     *
     * @param firstPreviewSurface first surfaceTexture with it's size.
     * @param secondPreviewSurface second surfaceTexture with it's size.
     */
    void startPreview(@NonNull SurfaceTextureWrapper firstPreviewSurface,
                      @NonNull SurfaceTextureWrapper secondPreviewSurface);

    /**
     * Stop two camera's preview.
     */
    void stopPreview();

    /**
     * Take picture with picture size.
     *
     * @param bottomPictureSize the bottom camera picture size.
     * @param topPictureSize the top camera picture size.
     */
    void takePicture(@NonNull Size bottomPictureSize,
                     @NonNull Size topPictureSize);


    /**
     * Check is ready (preview is started) for capture or not.
     * @return true, preview started for capture, or false.
     */
    boolean isReadyForCapture();

    /**
     * Trigger a setting value change.
     *
     * @param cameraId which camera wants to change setting value.
     */
    void requestChangeSettingValue(@Nonnull String cameraId);

    /**
     * Update bottom camera id to device.
     *
     * @param bottomCameraId bottom camera id.
     */
    void updateBottomCameraId(@Nonnull String bottomCameraId);

    /**
     * Release pip device.
     */
    void release();
}