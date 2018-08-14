package com.mediatek.camera.tests.v3.operator;

import android.content.Intent;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

import java.util.List;

public class PermissionOperator extends Operator {
    public static final int INDEX_ENABLE_ALL = 0;
    public static final int INDEX_DISABLE_ALL = 1;

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
        if (index == INDEX_ENABLE_ALL) {
            return "Enable all permission of camera in settings";
        } else if (index == INDEX_DISABLE_ALL) {
            return "Disable all permission of camera in settings";
        } else {
            return null;
        }
    }

    @Override
    protected void doOperate(int index) {
        // launch setting
        Intent intent = Utils.getContext().getPackageManager()
                .getLaunchIntentForPackage(SYSTEM_SETTING_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear out any previous instances
        Utils.getContext().startActivity(intent);
        Utils.assertObject(By.pkg(SYSTEM_SETTING_PACKAGE));

        // open apps page
        UiObject2 apps = Utils.scrollOnScreenToFind(By.text("Apps"));
        Utils.assertRightNow(apps != null);
        apps.click();

        // open camera page
        UiObject2 camera = Utils.scrollOnScreenToFind(By.text("Camera"));
        Utils.assertRightNow(camera != null);
        camera.click();

        // open permissions
        UiObject2 permission = Utils.findObject(By.text("Permissions"));
        Utils.assertRightNow(permission != null);
        permission.click();
        Utils.assertObject(By.clazz("android.widget.Switch"));

        // disable all or enable all
        List<UiObject2> switchBtns = Utils.getUiDevice().findObjects(
                By.clazz("android.widget.Switch").checked(index == INDEX_DISABLE_ALL));
        for (UiObject2 switchBtn : switchBtns) {
            switchBtn.click();
            if (index == INDEX_DISABLE_ALL) {
                UiObject2 deny = Utils.findObject(By.text("DENY ANYWAY"), Utils.TIME_OUT_SHORT);
                if (deny != null) {
                    deny.click();
                    Utils.assertNoObject(By.text("DENY ANYWAY"));
                }
            }
        }

        // exit settings
        Utils.pressBackUtilFindNoObject(By.pkg(SYSTEM_SETTING_PACKAGE), 4);
    }
}
