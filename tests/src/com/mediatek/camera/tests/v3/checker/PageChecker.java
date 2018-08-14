package com.mediatek.camera.tests.v3.checker;

import android.support.test.uiautomator.By;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.annotation.CoverPoint;
import com.mediatek.camera.tests.v3.arch.Checker;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

@CoverPoint(pointList = {"Check UI status of camera"})
public class PageChecker extends Checker {
    private static final LogUtil.Tag TAG = Utils.getTestTag(PageChecker.class
            .getSimpleName());

    public static final int INDEX_PREVIEW = 0;
    public static final int INDEX_MODE_LIST = 1;
    public static final int INDEX_SETTINGS = 2;

    @Override
    public int getCheckCount() {
        return 3;
    }

    @Override
    protected void doCheck(int index) {
        switch (index) {
            case INDEX_PREVIEW:
                Utils.assertObject(By.res("com.mediatek.camera:id/preview_surface"),
                        Utils.TIME_OUT_NORMAL);
                break;
            case INDEX_MODE_LIST:
                Utils.assertObject(By.res("com.mediatek.camera:id/mode_title"),
                        Utils.TIME_OUT_NORMAL);
                break;
            case INDEX_SETTINGS:
                Utils.assertObject(By.text("Settings"), Utils.TIME_OUT_NORMAL);
                break;
        }
    }

    @Override
    public Page getPageBeforeCheck(int index) {
        return null;
    }

    @Override
    public String getDescription(int index) {
        switch (index) {
            case INDEX_PREVIEW:
                return "Check in preview page";
            case INDEX_MODE_LIST:
                return "Check in mode list page";
            case INDEX_SETTINGS:
                return "Check in settings page";
        }
        return null;
    }
}
