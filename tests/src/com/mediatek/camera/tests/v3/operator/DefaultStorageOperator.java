package com.mediatek.camera.tests.v3.operator;

import android.content.Intent;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

public class DefaultStorageOperator extends Operator {
    public static final int INDEX_INTERNAL_STORAGE = 0;
    public static final int INDEX_SD_CARD = 1;

    private static final String[] DEFAULT_STORAGE = new String[]{
            "Internal shared storage",
            "SD card"
    };

    private static final String SYSTEM_SETTING_PACKAGE = "com.android.settings";

    @Override
    public int getOperatorCount() {
        return 2;
    }

    @Override
    public Page getPageBeforeOperate(int index) {
        return null;
    }

    @Override
    public Page getPageAfterOperate(int index) {
        return null;
    }

    @Override
    public String getDescription(int index) {
        return "Change default storage to " + DEFAULT_STORAGE[index] + " in system settings";
    }

    @Override
    protected void doOperate(int index) {
        // launch settings
        Intent intent = Utils.getContext().getPackageManager()
                .getLaunchIntentForPackage(SYSTEM_SETTING_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear out any previous instances
        Utils.getContext().startActivity(intent);
        Utils.assertObject(By.pkg(SYSTEM_SETTING_PACKAGE));

        // open storage page
        UiObject2 storage = Utils.scrollOnScreenToFind(By.text("Storage"));
        Utils.assertRightNow(storage != null);
        storage.click();

        //choose default storage
        Utils.assertObject(By.text("Default write disk"));
        UiObject2 defaultStorage = Utils.findObject(By.text(DEFAULT_STORAGE[index]));
        Utils.assertRightNow(defaultStorage != null);

        UiObject2 defaultRadio = defaultStorage.getParent().getParent().findObject(
                By.clazz("android.widget.RadioButton"));
        Utils.assertRightNow(defaultRadio != null);
        defaultRadio.click();

        // exit settings
        Utils.pressBackUtilFindNoObject(By.pkg(SYSTEM_SETTING_PACKAGE), 2);
    }
}
