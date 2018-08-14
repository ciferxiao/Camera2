/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensor. Without
 * the prior written permission of MediaTek inc. and/or its licensor, any
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
 * NON-INFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
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

package com.mediatek.camera.feature.mode.pip.pipwrapping;

import android.view.Surface;

import com.mediatek.camera.common.utils.Size;

/**
 * Pip capture wrapper.
 */
public interface IPipCaptureWrapper {
    /**
     * Init gpu to process capture.
     *
     * @param inputFormats input formats.
     * @return pixel format that this egl can output.
     */
    int initCapture(int[] inputFormats);
    /**
     * Set capture surface.
     *
     * @param surface the surface used for taking picture.
     */
    void setCaptureSurface(Surface surface);
    /**
     * Set Capture Size.
     * @param bottomCaptureSize bottom picture size
     * @param topCaptureSize top picture size
     */
    void setCaptureSize(Size bottomCaptureSize, Size topCaptureSize);
    /**
     * Get bottom capture surface texture.
     * @return bottom capture surface texture.
     */
    SurfaceTextureWrapper getBottomCapSt();

    /**
     * Get top capture surface texture.
     * @return top capture surface texture.
     */
    SurfaceTextureWrapper getTopCapSt();
    /**
     * Set the jpeg's rotation received from Capture SurfaceTexture.
     * @param isBottomCam is bottom jpeg's rotation.
     * @param rotation received from surface texture's jpeg rotation.
     */
    void setJpegRotation(boolean isBottomCam, int rotation);
    /**
     * Un init capture.
     */
    void unInitCapture();
    /**
     * On picture taken.
     * @param jpegData jpeg buffer.
     */
    void onPictureTaken(byte[] jpegData);

    /**
     * Unlock next capture.
     */
    void unlockNextCapture();

    /**
     * keep capture rotation.
     */
    void keepCaptureRotation();
}
