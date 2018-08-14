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
package com.mediatek.camera.feature.setting.audiomode;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ISettingManager.SettingController;
import com.mediatek.camera.common.setting.SettingBase;

/**
 * Audio mode setting item.
 */
public class AudioMode extends SettingBase implements
AudioModeSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AudioMode.class.getSimpleName());
    private static final String KEY_AUDIO_MODE = "key_audio_mode";
    private ISettingChangeRequester mSettingChangeRequester;
    private AudioModeSettingView mSettingView;

    @Override
    public void init(IApp app,
                     ICameraContext cameraContext,
                     SettingController settingController) {
        super.init(app, cameraContext, settingController);
        mSettingView = new AudioModeSettingView(getKey(), app.getActivity());
        mSettingView.setOnValueChangeListener(this);
    }

    @Override
    public void unInit() {

    }

    @Override
    public void addViewEntry() {
        mAppUi.addSettingView(mSettingView);
    }

    @Override
    public void removeViewEntry() {
        mAppUi.removeSettingView(mSettingView);
    }

    @Override
    public void refreshViewEntry() {
        mSettingView.setEntryValues(getEntryValues());
        mSettingView.setValue(getValue());
        mSettingView.setEnabled(getEntryValues().size() > 1);
    }

    @Override
    public void postRestrictionAfterInitialized() {

    }

    @Override
    public SettingType getSettingType() {
        return SettingType.VIDEO;
    }

    @Override
    public String getKey() {
        return KEY_AUDIO_MODE;
    }

    @Override
    public IParametersConfigure getParametersConfigure() {
        AudioModeParametersConfig parametersConfig;
        if (mSettingChangeRequester == null) {
            parametersConfig = new AudioModeParametersConfig(this, mSettingDeviceRequester);
            mSettingChangeRequester = parametersConfig;
        }
        return (AudioModeParametersConfig) mSettingChangeRequester;
    }

    @Override
    public ICaptureRequestConfigure getCaptureRequestConfigure() {
        AudioModeCaptureRequestConfig requestConfig;
        if (mSettingChangeRequester == null) {
            requestConfig = new AudioModeCaptureRequestConfig(this, mSettingDevice2Requester);
            mSettingChangeRequester = requestConfig;
        }
        return (AudioModeCaptureRequestConfig) mSettingChangeRequester;
    }

    /**
     * Invoked after setting's all values are initialized.
     */
    public void onValueInitialized() {
        mSettingView.setValue(getValue());
        mSettingView.setEntryValues(getEntryValues());
    }
    /**
     * Callback when audio mode value changed.
     * @param value The changed audio mode, such as "normal".
     */
    @Override
    public void onValueChanged(String value) {
        LogHelper.i(TAG, "[onValueChanged], value:" + value);
        if (!getValue().equals(value)) {
            setValue(value);
            mDataStore.setValue(getKey(), value, getStoreScope(), false);
            mSettingChangeRequester.sendSettingChangeRequest();
        }
    }

    /**
     * update set value.
     * @param value the default value
     */
    public void updateValue(String value) {
        setValue(mDataStore.getValue(getKey(), value, getStoreScope()));
    }

}
