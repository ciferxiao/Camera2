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

import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.ISettingManager.SettingController;

import javax.annotation.Nonnull;

/**
 * Setting manager wrapper used to record:
 * 1.camera id
 * 2.setting manager instance
 * 3.setting controller.
 * This class is used for switch pip more easy.
 */
public class SettingManagerWrapper {
    private final String mCameraId;
    private final ISettingManager mSettingManager;
    private final SettingController mSettingController;
    private boolean mIsValid = true;

    /**
     * Construct a setting manager wrapper.
     * @param settingManager setting manager instance.
     */
    public SettingManagerWrapper(@Nonnull ISettingManager settingManager) {
        mSettingManager = settingManager;
        mSettingController = settingManager.getSettingController();
        mCameraId = mSettingController.getCameraId();
    }

    /**
     * Get camera id.
      * @return the camera id.
     */
    public String getCameraId() {
        return mCameraId;
    }

    /**
     * Get setting manager.
      * @return the setting manager.
     */
    public ISettingManager getSettingManager() {
        return mSettingManager;
    }

    /**
     * Get setting controller.
      * @return the setting controller.
     */
    public SettingController getSettingController() {
        return mSettingController;
    }

    /**
     * Return whether this setting manager wrapper is valid.
     * @return whether this setting manager wrapper is valid.
     */
    public boolean isValid() {
        return mIsValid;
    }

    /**
     * Invalidate this setting manager wrapper.
     */
    public void invalidate() {
        mIsValid = false;
    }
}