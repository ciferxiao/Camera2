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
package com.mediatek.camera.feature.mode.pip.video;


import com.mediatek.camera.common.device.CameraDeviceManagerFactory.CameraApi;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

/**
 * Pip video combination.
 */
public class PipVideoCombination {
    private static final String KEY_PIP_VIDEO = PipVideoMode.class.getName();
    private static final String KEY_SCENE_MODE = "key_scene_mode";
    private static final String KEY_NOISE_REDUCTION = "key_noise_reduction";
    private static final String KEY_NOISE_EIS = "key_eis";
    private static final String KEY_AUDIO_MODE = "key_audio_mode";
    private static final String KEY_HDR = "key_hdr";
    private static final String KEY_EXPOSURE = "key_exposure";
    private static final String KEY_CAMERA_ZOOM = "key_camera_zoom";
    private static final String KEY_DUAL_ZOOM = "key_dual_zoom";
    private static final String KEY_FOCUS = "key_focus";
    private static final String KEY_VIDEO_QUALITY = "key_video_quality";
    private static final String KEY_ZSD = "key_zsd";
    private static final String KEY_COLOR_EFFECT = "key_color_effect";
    private static final String KEY_WHITE_BALANCE = "key_white_balance";
    private static final String KEY_FLASH = "key_flash";
    private static final String KEY_ANTI_FLICKER = "key_anti_flicker";
    private static final String KEY_BRIGHTNESS = "key_brightness";
    private static final String KEY_CONTRAST = "key_contrast";
    private static final String KEY_HUE = "key_hue";
    private static final String KEY_SATURATION = "key_saturation";
    private static final String KEY_SHARPNESS = "key_sharpness";

    private static RelationGroup sRelation = new RelationGroup();

    static {
        sRelation.setHeaderKey(KEY_PIP_VIDEO);
        sRelation.setBodyKeys(
                        KEY_SCENE_MODE + "," +
                        KEY_NOISE_REDUCTION + "," +
                        KEY_NOISE_EIS + "," +
                        KEY_AUDIO_MODE + "," +
                        KEY_HDR + "," +
                        KEY_EXPOSURE + "," +
                        KEY_CAMERA_ZOOM + "," +
                        KEY_DUAL_ZOOM + "," +
                        KEY_FOCUS + "," +
                        KEY_VIDEO_QUALITY + "," +
                        KEY_COLOR_EFFECT + "," +
                        KEY_WHITE_BALANCE + "," +
                        KEY_FLASH + "," +
                        KEY_ZSD + "," +
                        KEY_ANTI_FLICKER + "," +
                        KEY_BRIGHTNESS + "," +
                        KEY_CONTRAST + "," +
                        KEY_HUE + "," +
                        KEY_SATURATION + "," +
                        KEY_SHARPNESS);
        sRelation.addRelation(
                new Relation.Builder(KEY_PIP_VIDEO, "on")
                        .addBody(KEY_SCENE_MODE, "off", "off")
                        .addBody(KEY_NOISE_REDUCTION, "off", "off")
                        .addBody(KEY_NOISE_EIS, "off", "off")
                        .addBody(KEY_AUDIO_MODE, "normal", "normal")
                        .addBody(KEY_HDR, "off", "off")
                        .addBody(KEY_EXPOSURE, "0", "0")
                        .addBody(KEY_ZSD, "off", "off")
                        .addBody(KEY_COLOR_EFFECT, "none", "none")
                        .addBody(KEY_WHITE_BALANCE, "auto", "auto")
                        .addBody(KEY_ANTI_FLICKER, "auto", "auto")
                        .addBody(KEY_BRIGHTNESS, "middle", "middle")
                        .addBody(KEY_CONTRAST, "middle", "middle")
                        .addBody(KEY_HUE, "middle", "middle")
                        .addBody(KEY_SATURATION, "middle", "middle")
                        .addBody(KEY_SHARPNESS, "middle", "middle")
                        .build());
    }

    /**
     * Get relation by whether is bottom device.
     *
     * @param bottom true indicate bottom device.
     * @param mCurrentCameraApi the current camera api.
     * @param isDuringRecording is during recording.
     * @return the relation.
     */
    public static Relation getPipOnRelation(boolean bottom,
                                            CameraApi mCurrentCameraApi,
                                            boolean isDuringRecording) {

        Relation relation = sRelation.getRelation("on", false).copy();
        if (CameraApi.API1.equals(mCurrentCameraApi)) {
            relation.addBody(KEY_FLASH, "off", "off");
        }
        if (!bottom) {
            relation.addBody(KEY_CAMERA_ZOOM, "off", "off");
            relation.addBody(KEY_DUAL_ZOOM, "off", "off");
            relation.addBody(KEY_FOCUS, "continuous-video", "continuous-video");
        } else {
            String currentFocusMode = isDuringRecording ? "auto" : "continuous-video";
            String supportedFocusModes = isDuringRecording ? "auto" : "continuous-video,auto";
            relation.addBody(KEY_FOCUS, currentFocusMode, supportedFocusModes);
        }
        return relation;
    }

    /**
     * Get pip video quality relation.
     * @param currentVideoQuality current video quality.
     * @return video quality relation.
     */
    public static Relation getPipVideoQualityRelation(String currentVideoQuality) {
        Relation relation = new Relation.Builder(KEY_PIP_VIDEO, "on").build();
        relation.addBody(KEY_VIDEO_QUALITY, currentVideoQuality, currentVideoQuality);
        return relation;
    }

    /**
     * Get recording status relation.
     * @param isRecording is during recording.
     * @return recording status relation.
     */
    public static Relation getRecordingStatusRelation(boolean isRecording) {
        Relation relation = new Relation.Builder(KEY_PIP_VIDEO, "on").build();
        String currentFocusMode = isRecording ? "auto" : "continuous-video";
        String supportedFocusModes = isRecording ? "auto" : "continuous-video,auto";
        relation.addBody(KEY_FOCUS, currentFocusMode, supportedFocusModes);
        return relation;
    }

}