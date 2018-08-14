package com.mediatek.camera.tests.v3.checker;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.tests.v3.arch.CheckerOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

public class SettingRadioOptionChecker extends CheckerOne {
    private String mTitle;
    private String mOption;

    public SettingRadioOptionChecker(String title, String option) {
        mTitle = title;
        mOption = option;
    }

    @Override
    protected void doCheck() {
        UiObject2 radioEnter = Utils.findObject(By.res("android:id/title").text(mTitle));
        Utils.assertRightNow(radioEnter != null);

        String option = radioEnter.getParent().findObject(By.res("android:id/summary")).getText();
        Utils.assertRightNow(mOption.equals(option));
    }

    @Override
    public Page getPageBeforeCheck() {
        return Page.SETTINGS;
    }

    @Override
    public String getDescription() {
        return "Check the value of setting [" + mTitle + "] is " + mOption;
    }
}
