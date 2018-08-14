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

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.PreferenceFragment;

import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;

import java.util.ArrayList;
import java.util.List;

/**
 * Audio mode setting view.
 */
public class AudioModeSettingView implements ICameraSettingView,
        AudioModeSelector.OnItemClickListener {
    private static final LogUtil.Tag TAG =
            new LogUtil.Tag(AudioModeSettingView.class.getSimpleName());
    private List<String> mEntryValues = new ArrayList<>();
    private static final String AUDIO_MODE_INDOOR = "indoor";
    private AudioModeSelector mAudioModeSelector;
    private OnValueChangeListener mListener;
    private String mSelectedValue;
    private Activity mActivity;
    private Preference mPref;
    private String mSummary;
    private String mKey;
    private boolean mEnabled;

    /**
     * Listener to listen audio mode value changed.
     */
    public interface OnValueChangeListener {
        /**
         * Callback when audio mode value changed.
         * @param value The changed audio mode, such as "normal".
         */
        void onValueChanged(String value);
    }
    /**
     * audio mode setting view constructor.
     * @param key The key of audio mode
     * @param activity the activity.
     */
    public AudioModeSettingView(String key , Activity activity) {
        mKey = key;
        mActivity = activity;
    }

    @Override
    public void loadView(PreferenceFragment fragment) {
        LogHelper.d(TAG, "[loadView]");
        mActivity = fragment.getActivity();

        if (mAudioModeSelector == null) {
            mAudioModeSelector = new AudioModeSelector();
            mAudioModeSelector.setOnItemClickListener(this);

        }

        fragment.addPreferencesFromResource(R.xml.audio_mode_preference);
        mPref = (Preference) fragment.findPreference(mKey);
        mPref.setRootPreference(fragment.getPreferenceScreen());
        mPref.setId(R.id.audio_mode_setting);
        mPref.setContentDescription(mActivity.getResources()
                .getString(R.string.audio_mode_content_description));
        mPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                mAudioModeSelector.setValue(mSelectedValue);
                mAudioModeSelector.setEntryValues(mEntryValues);

                FragmentTransaction transaction = mActivity.getFragmentManager()
                        .beginTransaction();
                transaction.addToBackStack(null);
                transaction.replace(R.id.setting_container,
                        mAudioModeSelector, "audio_mode_selector").commit();
                return true;
            }
        });
        mPref.setEnabled(mEnabled);
        if (AUDIO_MODE_INDOOR.equals(mSelectedValue)) {
            mSummary = mActivity.getResources().getString(R.string.audio_mode_indoor_title);
        } else {
            mSummary = mActivity.getResources().getString(R.string.audio_mode_normal_title);
        }
    }

    @Override
    public void refreshView() {
        LogHelper.d(TAG, "[refreshView]");
        mPref.setSummary(mSummary);
        mPref.setEnabled(mEnabled);
    }

    @Override
    public void unloadView() {

    }

    @Override
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Set listener to listen the changed audio mode value.
     * @param listener The instance of {@link OnValueChangeListener}.
     */
    public void setOnValueChangeListener(OnValueChangeListener listener) {
        mListener = listener;
    }

    /**
     * Set the default selected value.
     * @param value The default selected value.
     */
    public void setValue(String value) {
        mSelectedValue = value;
    }

    /**
     * Set the audio mode supported.
     * @param entryValues The audio mode supported.
     */
    public void setEntryValues(List<String> entryValues) {
        mEntryValues = entryValues;
    }

    /**
     * Callback when item clicked.
     * @param value The audio mode clicked.
     */
    @Override
    public void onItemClick(String value) {
        mSelectedValue = value;
        if (AUDIO_MODE_INDOOR.equals(value)) {
            mSummary = mActivity.getResources().getString(R.string.audio_mode_indoor_title);
        } else {
            mSummary = mActivity.getResources().getString(R.string.audio_mode_normal_title);
        }
        mListener.onValueChanged(value);
    }
}
