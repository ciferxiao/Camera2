package com.mediatek.camera.tests.v3.operator;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.OperatorOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;


public class CapturePhotoOperator extends OperatorOne {
    private static final LogUtil.Tag TAG = Utils.getTestTag(CapturePhotoOperator.class
            .getSimpleName());

    @Override
    protected void doOperate() {
        boolean isPanoramaMode = Utils.waitObject(By.res("com.mediatek.camera:id/shutter_root")
                .descContains("PanoramaMode"), Utils.TIME_OUT_RIGHT_NOW);

        UiObject2 shutter = Utils.findObject(By.res("com.mediatek.camera:id/shutter_image")
                .clickable(true));
        shutter.click();

        if (isPanoramaMode) {
            UiObject2 panoramaSave = Utils.findObject(By.res("com.mediatek.camera:id/btn_pano_save")
                    , Utils.TIME_OUT_SHORT);
            Utils.assertRightNow(panoramaSave != null);

            long timeout = System.currentTimeMillis() + Utils.TIME_OUT_NORMAL;
            while (Utils.waitObject(By.res("com.mediatek.camera:id/btn_pano_save"),
                    Utils.TIME_OUT_SHORT_SHORT)) {
                if (System.currentTimeMillis() > timeout) {
                    Utils.assertRightNow(false);
                }
                panoramaSave = Utils.findObject(By.res("com.mediatek.camera:id/btn_pano_save")
                        , Utils.TIME_OUT_SHORT_SHORT);
                if (panoramaSave != null) {
                    panoramaSave.click();
                }
            }
        }
    }

    @Override
    public Page getPageBeforeOperate() {
        return Page.PREVIEW;
    }

    @Override
    public Page getPageAfterOperate() {
        return Page.PREVIEW;
    }

    @Override
    public String getDescription() {
        return "Capture one photo, if panorama mode, capture photo without moving";
    }

}
