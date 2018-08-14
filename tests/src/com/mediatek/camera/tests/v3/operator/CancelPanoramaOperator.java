package com.mediatek.camera.tests.v3.operator;


import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

import junit.framework.Assert;

public class CancelPanoramaOperator extends Operator {
    private static final LogUtil.Tag TAG = Utils.getTestTag(CapturePhotoOperator.class
            .getSimpleName());
    public static final int CANCEL_INDEX = 0;
    public static final int CANCEL_THEN_SAVE_QUICKLY_INDEX = 1;

    @Override
    protected void doOperate(int index) {
        UiObject2 panoramaCancel = Utils.findObject(
                By.res("com.mediatek.camera:id/btn_pano_cancel"));
        Assert.assertNotNull(panoramaCancel);
        UiObject2 panoramaSave = Utils.findObject(
                By.res("com.mediatek.camera:id/btn_pano_save"));
        Assert.assertNotNull(panoramaSave);
        switch (index) {
            case CANCEL_INDEX:
                if (panoramaCancel != null) {
                    panoramaCancel.click();
                }
                break;
            case CANCEL_THEN_SAVE_QUICKLY_INDEX:
                if (panoramaCancel != null) {
                    panoramaCancel.click();
                }
                if (panoramaSave != null) {
                    panoramaSave.click();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isSupported(int index) {
        return super.isSupported(index);
    }

    @Override
    public int getOperatorCount() {
        return 2;
    }

    @Override
    public Page getPageBeforeOperate(int index) {
        return Page.PREVIEW;
    }

    @Override
    public Page getPageAfterOperate(int index) {
        return Page.PREVIEW;
    }

    @Override
    public String getDescription(int index) {
        switch (index) {
            case CANCEL_INDEX:
                return "Cancel capturing panorama photo.";
            case CANCEL_THEN_SAVE_QUICKLY_INDEX:
                return "Click cancel button and then save button";
        }
        return null;
    }
}
