package com.mediatek.camera.tests.v3.checker;

import android.support.test.uiautomator.By;

import com.mediatek.camera.tests.v3.arch.Checker;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;


public class PanoramaUiChecker extends Checker {
    @Override
    public float getCheckCoverage() {
        return super.getCheckCoverage();
    }

    @Override
    public boolean isSupported(int index) {
        return super.isSupported(index);
    }

    @Override
    public int getCheckCount() {
        return 1;
    }

    @Override
    public Page getPageBeforeCheck(int index) {
        return Page.PREVIEW;
    }

    @Override
    public String getDescription(int index) {
        return "Check panorama ui is showing";
    }

    @Override
    protected void doCheck(int index) {
        Utils.assertObject(By.res("com.mediatek.camera:id/btn_pano_cancel"));
        Utils.assertObject(By.res("com.mediatek.camera:id/btn_pano_save"));
        Utils.assertNoObject(By.res("com.mediatek.camera:id/shutter_root"));
    }
}
