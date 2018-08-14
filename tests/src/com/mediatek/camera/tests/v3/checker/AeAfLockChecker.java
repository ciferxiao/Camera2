package com.mediatek.camera.tests.v3.checker;

import android.support.test.uiautomator.By;

import com.mediatek.camera.tests.v3.annotation.CoverPoint;
import com.mediatek.camera.tests.v3.annotation.NotCoverPoint;
import com.mediatek.camera.tests.v3.arch.Checker;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

/**
 * Checker used to check AE/AF Lock UI is right.
 */
@CoverPoint(pointList = {"Check the AE/AF Lock Ui and indicator is shown normally"})
@NotCoverPoint(pointList = {"Not check the position and orientation of the focus UI"})
public class AeAfLockChecker extends Checker {
    private static final String RES_EV_BAR = "com.mediatek.camera:id/ev_seekbar";

    @Override
    public int getCheckCount() {
        return 1;
    }

    @Override
    public Page getPageBeforeCheck(int index) {
        return Page.PREVIEW;
    }


    @Override
    protected void doCheck(int index) {
        Utils.assertObject(By.res(RES_EV_BAR));
        Utils.assertObject(By.text("AE/AF Lock"));
    }

    @Override
    public String getDescription(int index) {
        return "Check touch focus ring, ev seek bar, AE/AF Lock ui is showing";
    }
}
