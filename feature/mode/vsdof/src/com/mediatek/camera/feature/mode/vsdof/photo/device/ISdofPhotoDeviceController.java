/*
 *   Copyright Statement:
 *
 *     This software/firmware and related documentation ("MediaTek Software") are
 *     protected under relevant copyright laws. The information contained herein is
 *     confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 *     the prior written permission of MediaTek inc. and/or its licensors, any
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
 *     NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
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

package com.mediatek.camera.feature.mode.vsdof.photo.device;

import android.view.SurfaceHolder;

import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.vsdof.photo.DeviceInfo;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Define the interaction apis between {@link PhotoMode} and Camera Device,
 * include {@link CameraProxy} and {@link Camera2Proxy}.
 *
 * This make photo mode no need to care the implementation of camera device,
 * photo mode control flow should compatible {@link CameraProxy} and {@link Camera2Proxy}
 */
public interface ISdofPhotoDeviceController {
    // notify for Image before compress when taking capture
    public static final int MTK_CAMERA_MSG_EXT_NOTIFY_IMAGE_UNCOMPRESSED = 0x00000017;
    // Stereo Feature: warning message
    public static final int MTK_CAMERA_MSG_EXT_NOTIFY_STEREO_WARNING   = 0x00000014;
    // Stereo message
    public static final int MTK_CAMERA_MSG_EXT_STEREO_DATA             = 0x00000010;
    //Stereo Camera JPS
    public static final int MTK_CAMERA_MSG_EXT_DATA_JPS                = 0x00000011;
    //Stereo Debug Data
    //int[0]: data type.
    public static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_DBG         = 0x00000012;
    //Stereo Camera Depth Map Data
    public static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_DEPTHMAP    = 0x00000014;
    //Stereo Camera Clear Data
    public static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_CLEAR_IMAGE = 0x00000015;
    //Stereo Camera LDC Data
    public static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_LDC         = 0x00000016;
    //Stereo Camera n3d Data
    public static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_N3D         = 0x00000019;
    //Stereo Camera depth wrapper Data
    public static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_DEPTHWRAPPER = 0x00000020;

    public static final String KEY_VS_DOF_LEVEL = "stereo-dof-level";
    public static final String KEY_REFOCUS_PICTURE_SIZE_VALUES = "refocus-picture-size-values";
    public static final String KEY_VSDOF_MODE = "stereo-vsdof-mode";
    public static final String KEY_STEREO_CAPTURE_MODE = "stereo-image-refocus";
    public static final String KEY_STEREO_DENOISE_MODE = "stereo-denoise-mode";
    public static final String KEY_STEREO_POST_VIEW_SIZE_VALUES = "stereo-postview-size-values";

    /**
     * Used for wrapping data callback.
     */
    class DataCallbackInfo {
        public byte[] data;
        public boolean needUpdateThumbnail;
    }

    /**
     * This interface is used to callback Capture data to mode.
     */
    interface CaptureDataCallback {
        /**
         * Notify jpeg data is generated.
         * @param dataCallbackInfo data callback info.
         */
        void onDataReceived(DataCallbackInfo dataCallbackInfo);

        /**
         * When post view callback will notify.
         * @param data post view data.
         */
        void onPostViewCallback(byte[] data);
        /**
         *Notify Jps data is generated.
         * @param jpsData capture data
         */
        void onJpsCapture(byte[] jpsData);
        /**
         *Notify mask data is generated.
         * @param maskData capture data
         */
        void onMaskCapture(byte[] maskData);
        /**
         *Notify depth data is generated.
         * @param depthMapData map capture data
         */
        void onDepthMapCapture(byte[] depthMapData);
        /**
         *Notify clear image data is generated.
         * @param clearImageData image capture data
         */
        void onClearImageCapture(byte[] clearImageData);
        /**
         * Notify ldc data is generated.
         * @param ldcData capture data
         */
        void onLdcCapture(byte[] ldcData);
        /**
         * Notify n3d data is generated.
         * @param n3dData debug data
         */
        void onN3dCapture(byte[] n3dData);
        /**
         * Notify depth wrapper data is generated.
         * @param depthWrapper depth wrapper data
         */
        void onDepthWrapperCapture(byte[] depthWrapper);
    }


    /**
     * This interface is used to callback stereo warning info.
     */
    interface StereoWarningCallback {
        /**
         * Notify warning info.
         * @param type the warning type.
         */
        void onWarning(int type);
    }

    /**
     * This callback to notify device state.
     */
    interface DeviceCallback {
        /**
         * Notified when camera opened done with camera id.
         * @param cameraId the camera is opened.
         */
        void onCameraOpened(String cameraId);

        /**
         * Notified before do close camera.
         */
        void beforeCloseCamera();

        /**
         * Notified call stop preview immediately.
         */
        void afterStopPreview();

        /**
         * When preview data is received,will fired this function.
         * @param data the preview data.
         * @param format the preview format.
         */
        void onPreviewCallback(byte[] data, int format);
    }

    /**
     * This callback is used for notify camera is opened,you can use it for get parameters..
     */
    interface PreviewSizeCallback {
        /**
         * When camera is opened will be called.
         * @param previewSize current preview size.
         * @param pictureSizes supported picture sizes.
         */
        void onPreviewSizeReady(Size previewSize, List<String> pictureSizes);
    }
    /**
     * open camera with specified camera id.
     * @param info the camera info which will be opened.
     */
    void openCamera(DeviceInfo info);

    /**
     * update preview surface.
     * @param surfaceHolder surface holder instance.
     */
    void updatePreviewSurface(SurfaceHolder surfaceHolder);

    /**
     * Set a callback for device.
     * @param callback the device callback.
     */
    void setDeviceCallback(DeviceCallback callback);

    /**
     * Set a warning callback for device.
     * @param callback the warning callback.
     */
    void setStereoWarningCallback(StereoWarningCallback callback);

    /**
     * For API1 will directly call start preview.
     * For API2 will first create capture session and then set repeating requests.
     */
    void startPreview();

    /**
     * For API1 will directly call stop preview.
     * For API2 will call session's abort captures.
     */
    void stopPreview();

    /**
     * For API1 will directly call takePicture.
     * For API2 will call STILL_CAPTURE capture.
     *
     * @param callback jpeg data callback.
     */
    void takePicture(@Nonnull CaptureDataCallback callback);

    /**
     * update current GSensor orientation.the value will be 0/90/180/270.
     * @param orientation current GSensor orientation.
     */
    void updateGSensorOrientation(int orientation);
    /**
     * close camera.
     * @param sync whether need sync call.
     */
    void closeCamera(boolean sync);

    /**
     * Get the preview size with target ratio.
     * @param targetRatio current ratio.
     * @return current preview size.
     */
    Size getPreviewSize(double targetRatio);

    /**
     * Set a camera opened callback.
     * @param callback camera opened callback.
     */
    void setPreviewSizeReadyCallback(PreviewSizeCallback callback);

    /**
     * Set the new picture size.
     * @param size current picture size.
     */
    void setPictureSize(Size size);

    /**
     * Get the stereo picture size.
     * @return the supported picture sizes.
     */
    String getStereoPictureSize();

    /**
     * Check whether can take picture or not.
     * @return true means can take picture; otherwise can not take picture.
     */
    boolean isReadyForCapture();

    /**
     * When don't need the device controller need destroy the device controller.
     * such as handler.
     */
    void destroyDeviceController();

    /**
     * Set dof level parameter when change.
     * @param level the dof level.
     */
    void setVsDofLevelParameter(String level);

    /**
     * Get the post view size.
     * @return current post view size.
     */
    Size getPostViewSize();
}