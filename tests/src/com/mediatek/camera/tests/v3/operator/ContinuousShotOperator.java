package com.mediatek.camera.tests.v3.operator;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.OperatorOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

public class ContinuousShotOperator extends OperatorOne {
    private static final LogUtil.Tag TAG = Utils.getTestTag(ContinuousShotOperator.class
            .getSimpleName());
    private static final int DURATION_MILLISECONDS = 5000;

    @Override
    protected void doOperate() {
        Utils.assertObject(By.res("com.mediatek.camera:id/shutter_root").descContains("PhotoMode"));
        UiObject2 shutter = Utils.findObject(By.res("com.mediatek.camera:id/shutter_image")
                .clickable(true));
        Utils.longPress(shutter, DURATION_MILLISECONDS);
        LogHelper.d(TAG, "[doOperate] click down, wait saving");
        Utils.assertNoObject(By.res("com.mediatek.camera:id/dialog_progress"),
                Utils.TIME_OUT_LONG_LONG);
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
        return "Long press shutter button to do continuous shot";
    }

    @Override
    public boolean isSupported(int index) {
        return Utils.isFeatureSupported("com.mediatek.camera.at.cs");
    }
}
