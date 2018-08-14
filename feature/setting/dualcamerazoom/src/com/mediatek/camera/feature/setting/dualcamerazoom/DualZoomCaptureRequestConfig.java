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
 *   MediaTek Inc. (C) 2016. All rights reserved.
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

package com.mediatek.camera.feature.setting.dualcamerazoom;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.view.Surface;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is for zoom perform for api2.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DualZoomCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure,
        IDualZoomConfig {
    private static final LogUtil.Tag TAG
            = new LogUtil.Tag(DualZoomCaptureRequestConfig.class.getSimpleName());
    private Rect mSensorRect;
    private double mDistanceRatio;
    private OnZoomLevelUpdateListener mZoomUpdateListener;
    private boolean mIsUserInteraction;
    private float mLastZoomRatio = DEFAULT_VALUE;
    private float mBasicZoomRatio;
    private float mCurZoomRatio;
    private float mMaxZoom;
    private float mMinZoom;
    private boolean mIsPinch = false;
    private ISettingManager.SettingDevice2Requester mSettingDevice2Requester;

    /**
     * Constructor of zoom parameter config in api2.
     *
     * @param settingDevice2Requester device requester.
     */
    public DualZoomCaptureRequestConfig(ISettingManager.SettingDevice2Requester
                                                settingDevice2Requester) {
        mSettingDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics characteristics) {
        LogHelper.d(TAG, "[setCameraCharacteristics]");
        mSensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        float maxZoom
                = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        //TODO: check slow motion api2 zoom flow.
        if (mZoomUpdateListener.isZoomTele()) {
            mMinZoom = ZOOM_MIN_VALUE;
        } else {
            mMinZoom = ZOOM_MIN_VALUE;
        }
        mMaxZoom = maxZoom > ZOOM_MAX_VALUE ? ZOOM_MAX_VALUE : maxZoom;
        mZoomUpdateListener.updateMaxZoomSupported(mMaxZoom);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder captureBuilder) {
        if (ZOOM_OFF.equals(mZoomUpdateListener.onGetOverrideValue())) {
            reset(captureBuilder);
            return;
        }
        mCurZoomRatio = calculateZoomRatio(mDistanceRatio);
        // apply crop region
        captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRegionForZoom(mCurZoomRatio));
        mLastZoomRatio = mCurZoomRatio;
        mZoomUpdateListener.onZoomRatioUpdate(mCurZoomRatio);
        LogHelper.d(TAG, "[configCaptureRequest] mCurZoomRatio = " + mCurZoomRatio
                + ", mDistanceRatio = " + mDistanceRatio);
    }

    @Override
    public void configSessionSurface(List<Surface> surfaces) {
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public void sendSettingChangeRequest() {
        if (isZoomValid()) {
            mSettingDevice2Requester.createAndChangeRepeatingRequest();
        }
    }

    @Override
    public void setZoomUpdateListener(OnZoomLevelUpdateListener zoomUpdateListener) {
        mZoomUpdateListener = zoomUpdateListener;
    }

    @Override
    public void onScalePerformed(double distanceRatio) {
        mDistanceRatio = distanceRatio;
    }

    @Override
    public boolean onScaleStatus(boolean isSwitch, boolean isInit) {
        mIsUserInteraction = true;
        //must set to 0, since if not scale, it should not zoom.
        mDistanceRatio = 0;
        //TODO: check slow motion api2 zoom flow.
        if (isSwitch) {
            if (mZoomUpdateListener.isZoomTele() && mLastZoomRatio != ZOOM_MIN_VALUE) {
                mBasicZoomRatio = ZOOM_MIN_VALUE;
                return true;
            } else if (!mZoomUpdateListener.isZoomTele() && mLastZoomRatio != ZOOM_MIN_VALUE) {
                mBasicZoomRatio = ZOOM_MIN_VALUE;
                return true;
            }
        }
        calculateBasicRatio();
        return false;
    }

    @Override
    public void onScaleType(boolean isPinch) {
        mIsPinch = isPinch;
    }

    private boolean isZoomValid() {
        return mCurZoomRatio >= mMinZoom && mCurZoomRatio <= mMaxZoom
                && calculateZoomRatio(mDistanceRatio) != mLastZoomRatio;
    }

    private void calculateBasicRatio() {
        if (mLastZoomRatio == DEFAULT_VALUE) {
            mBasicZoomRatio = ZOOM_MIN_VALUE;
        } else {
            mBasicZoomRatio = mLastZoomRatio;
        }
    }

    /**
     * Calculates sensor crop region for a zoom level (zoom >= 1.0).
     *
     * @param ratio the zoom level.
     * @return Crop region.
     */
    private Rect cropRegionForZoom(float ratio) {
        int xCenter = mSensorRect.width() / 2;
        int yCenter = mSensorRect.height() / 2;
        int xDelta = (int) (0.5f * mSensorRect.width() / ratio);
        int yDelta = (int) (0.5f * mSensorRect.height() / ratio);
        return new Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
    }

    private void reset(CaptureRequest.Builder captureBuilder) {
        LogHelper.d(TAG, "[reset]");
        // apply crop region
        captureBuilder.set(CaptureRequest.SCALER_CROP_REGION,
                cropRegionForZoom(ZOOM_MIN_VALUE));
        mLastZoomRatio = ZOOM_MIN_VALUE;
    }

    private String getPatternRatio(boolean isTele) {
        if (isTele) {
            return String.format(Locale.ENGLISH, PATTERN, mCurZoomRatio * 2) + "X";
        } else {
            return String.format(Locale.ENGLISH, PATTERN, mCurZoomRatio) + "X";
        }
    }

    private float calculateZoomRatio(double distanceRatio) {
        float find = ZOOM_MIN_VALUE; // if not find, return 1.0f.
        float maxRatio = mMaxZoom;
        float minRatio = mMinZoom;
        float curRatio;
        if (mIsPinch) {
            curRatio = (float) (mBasicZoomRatio + DEFAULT_ZOOM_RATIO * distanceRatio);
            if (curRatio <= minRatio) {
                find = minRatio;
            } else if (curRatio >= maxRatio) {
                find = maxRatio;
            } else {
                find = curRatio;
            }
        } else {
            if (distanceRatio > 0) {
                find = minRatio + (float) ((maxRatio - minRatio) * distanceRatio);
            } else {
                find = minRatio + (float) ((maxRatio - minRatio) * Math.abs(distanceRatio));
            }
        }
        return find;
    }
}
