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

package com.mediatek.camera.feature.mode.pip.view;

import android.graphics.Bitmap;

import javax.annotation.Nonnull;

/**
 * The wrapper for pip top graphic's templates.
 */
public class PipTemplateWrapper {
    private Bitmap mTemplateBitmap;
    private Bitmap mCoverBitmap;
    private Bitmap mHighLightBitmap;
    private Bitmap mEditBitmap;

    /**
     * Construct a PipTemplateWrapper with specified bitmaps.
     *
     * @param templateBitmap template bitmap.
     * @param coverBitmap cover bitmap.
     * @param highLightBitmap high light bitmap.
     * @param editBitmap edit button bitmap.
     */
    public PipTemplateWrapper(@Nonnull Bitmap templateBitmap,
                              @Nonnull Bitmap coverBitmap,
                              @Nonnull Bitmap highLightBitmap,
                              @Nonnull Bitmap editBitmap) {
        mTemplateBitmap = templateBitmap;
        mCoverBitmap = coverBitmap;
        mHighLightBitmap = highLightBitmap;
        mEditBitmap = editBitmap;
    }

    /**
     * Get the bitmap of back template.
     *
     * @return the bitmap of template.
     */
    public Bitmap getTemplateBitmap() {
        return mTemplateBitmap;
    }

    /**
     * Get the cover's bitmap.
     *
     * @return the bitmap of cover.
     */
    public Bitmap getCoverBitmap() {
        return mCoverBitmap;
    }

    /**
     * Get the high light's bitmap.
     *
     * @return the bitmap of high light frame.
     */
    public Bitmap getHighLightBitmap() {
        return mHighLightBitmap;
    }

    /**
     * Get the edit button's bitmap.
     *
     * @return the bitmap of edit button.
     */
    public Bitmap getEditBitmap() {
        return mEditBitmap;
    }

    /**
     * Recycle all bitmaps, this should be called when updateTexImage to GPU.
     */
    public void recycleAllBitmap() {
        if (mTemplateBitmap != null && mTemplateBitmap.isRecycled()) {
            mTemplateBitmap.recycle();
        }
        if (mCoverBitmap != null && mCoverBitmap.isRecycled()) {
            mCoverBitmap.recycle();
        }
        if (mHighLightBitmap != null && mHighLightBitmap.isRecycled()) {
            mHighLightBitmap.recycle();
        }
        if (mEditBitmap != null && mEditBitmap.isRecycled()) {
            mEditBitmap.recycle();
        }
    }
}