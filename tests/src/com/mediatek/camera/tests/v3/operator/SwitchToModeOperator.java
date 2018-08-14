package com.mediatek.camera.tests.v3.operator;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.tests.v3.arch.OperatorOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

public class SwitchToModeOperator extends OperatorOne {
    private String mModeName;

    public SwitchToModeOperator(String modeName) {
        mModeName = modeName;
    }

    @Override
    protected void doOperate() {
        UiObject2 modeEntry = Utils.findObject(By.res("com.mediatek.camera:id/text_view").text
                (getModeName()));
        Utils.assertRightNow(modeEntry != null);

        modeEntry.click();
    }

    @Override
    public Page getPageBeforeOperate() {
        return Page.MODE_LIST;
    }

    @Override
    public Page getPageAfterOperate() {
        return Page.PREVIEW;
    }

    @Override
    public String getDescription() {
        return "Switch to " + getModeName() + " mode";
    }

    public String getModeName() {
        return mModeName;
    }
}
