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
package com.mediatek.camera.feature.mode.pip.photo;


import com.google.common.base.Joiner;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;

import java.util.List;

/**
 * Pip photo combination.
 */
public class PipPhotoCombination {
    private static final String KEY_PIP_PHOTO = PipPhotoMode.class.getName();
    private static final String KEY_FACE_DETECTION = "key_face_detection";
    private static final String KEY_DNG = "key_dng";
    private static final String KEY_SCENE_MODE = "key_scene_mode";
    private static final String KEY_ISO = "key_iso";
    private static final String KEY_SELF_TIMER = "key_self_timer";
    private static final String KEY_HDR = "key_hdr";
    private static final String KEY_CSHOT = "key_continuous_shot";
    private static final String KEY_CAMERA_ZOOM = "key_camera_zoom";
    private static final String KEY_DUAL_ZOOM = "key_dual_zoom";
    private static final String KEY_PICTURE_SIZE = "key_picture_size";
    private static final String KEY_EXPOSURE = "key_exposure";
    private static final String KEY_ZSD = "key_zsd";
    private static final String KEY_FOCUS = "key_focus";
    private static final String KEY_COLOR_EFFECT = "key_color_effect";
    private static final String KEY_WHITE_BALANCE = "key_white_balance";
    private static final String KEY_AIS = "key_ais";
    private static final String KEY_ANTI_FLICKER = "key_anti_flicker";
    private static final String KEY_BRIGHTNESS = "key_brightness";
    private static final String KEY_CONTRAST = "key_contrast";
    private static final String KEY_HUE = "key_hue";
    private static final String KEY_SATURATION = "key_saturation";
    private static final String KEY_SHARPNESS = "key_sharpness";
    private static final String KEY_NOISE_REDUCTION = "key_noise_reduction";

    private static RelationGroup sRelation = new RelationGroup();

    static {
        sRelation.setHeaderKey(KEY_PIP_PHOTO);
        sRelation.setBodyKeys(
                KEY_FACE_DETECTION + "," +
                KEY_DNG + "," +
                KEY_SCENE_MODE + "," +
                KEY_ISO + "," +
                KEY_SELF_TIMER + "," +
                KEY_HDR + "," +
                KEY_CSHOT + "," +
                KEY_CAMERA_ZOOM + "," +
                KEY_DUAL_ZOOM + "," +
                KEY_PICTURE_SIZE + "," +
                KEY_EXPOSURE + "," +
                KEY_ZSD + "," +
                KEY_COLOR_EFFECT + "," +
                KEY_WHITE_BALANCE + "," +
                KEY_AIS + "," +
                KEY_FOCUS + "," +
                KEY_ANTI_FLICKER + "," +
                KEY_BRIGHTNESS + "," +
                KEY_CONTRAST + "," +
                KEY_HUE + "," +
                KEY_SATURATION + "," +
                KEY_SHARPNESS + "," +
                KEY_NOISE_REDUCTION);
        sRelation.addRelation(
                new Relation.Builder(KEY_PIP_PHOTO, "on")
                        .addBody(KEY_FACE_DETECTION, "off", "off")
                        .addBody(KEY_DNG, "off", "off")
                        .addBody(KEY_SCENE_MODE, "off", "off")
                        .addBody(KEY_ISO, "auto", "auto")
                        .addBody(KEY_SELF_TIMER, "0", "0")
                        .addBody(KEY_HDR, "off", "off")
                        .addBody(KEY_CSHOT, "off", "off")
                        .addBody(KEY_EXPOSURE, "0", "0")
                        .addBody(KEY_COLOR_EFFECT, "none", "none")
                        .addBody(KEY_WHITE_BALANCE, "auto", "auto")
                        .addBody(KEY_AIS, "off", "off")
                        .addBody(KEY_ANTI_FLICKER, "auto", "auto")
                        .addBody(KEY_BRIGHTNESS, "middle", "middle")
                        .addBody(KEY_CONTRAST, "middle", "middle")
                        .addBody(KEY_HUE, "middle", "middle")
                        .addBody(KEY_SATURATION, "middle", "middle")
                        .addBody(KEY_SHARPNESS, "middle", "middle")
                        .addBody(KEY_NOISE_REDUCTION, "off", "off")
                        .build());
    }

    /**
     * Restriction witch are have setting ui.
     * @param bottom is bottom relation.
     * @param pictureSize override picture size.
     * @param zsdValue zsd value.
     * @return restriction list.
     */
    public static Relation getPipOnRelation(boolean bottom,
                                            Size pictureSize,
                                            String zsdValue) {
        Relation relation = sRelation.getRelation("on", false).copy();
        if (!bottom) {
            relation.addBody(KEY_CAMERA_ZOOM, "off", "off");
            relation.addBody(KEY_DUAL_ZOOM, "off", "off");
            relation.addBody(KEY_FOCUS, "continuous-picture", "continuous-picture");
            relation.addBody(KEY_PICTURE_SIZE,
                    CameraUtil.buildSize(pictureSize),
                    CameraUtil.buildSize(pictureSize));
            relation.addBody(KEY_ZSD, zsdValue, zsdValue);
        } else if (CameraDeviceManagerFactory.CameraApi.API2.equals(
                CameraApiHelper.getCameraApiType(null))) {
            relation.addBody(KEY_ZSD, zsdValue, zsdValue);
        }
        return relation;
    }

    /**
     * Get pip top picture size relation.
     *
     * @param overridePictureSize the override picture size.
     * @param supportedSizes the override picture sizes.
     * @return an relation.
     */
    public static Relation getTopPictureSizeRelation(Size overridePictureSize,
                                                     List<String> supportedSizes) {
        String overridePictureSizes =
                Joiner.on(Relation.BODY_SPLITTER).join(supportedSizes);
        Relation relation = new Relation.Builder(KEY_PIP_PHOTO, "on").build();
        relation.addBody(KEY_PICTURE_SIZE,
                CameraUtil.buildSize(overridePictureSize),
                overridePictureSizes);
        return relation;
    }

    /**
     * Get the top picture size remove relation.
     * @return an relation.
     */
    public static Relation getTopPictureSizeRemoveRelation() {
        Relation relation = new Relation.Builder(KEY_PIP_PHOTO, "on").build();
        relation.addBody(KEY_PICTURE_SIZE, null, null);
        return relation;
    }

    /**
     * Get pip top camera's zsd relation.
     * @param zsdValue the override zsd value.
     * @return the top camera's zsd relation.
     */
    public static Relation getTopZsdRelation(String zsdValue) {
        Relation relation = new Relation.Builder(KEY_PIP_PHOTO, "on").build();
        relation.addBody(KEY_ZSD, zsdValue, zsdValue);
        return relation;
    }
}