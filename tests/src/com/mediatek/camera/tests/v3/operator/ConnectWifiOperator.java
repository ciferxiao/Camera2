package com.mediatek.camera.tests.v3.operator;

import android.content.Intent;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

public class ConnectWifiOperator extends Operator {
    public static final int INDEX_CONNECT = 0;
    public static final int INDEX_DISCONNECT = 1;

    private static final LogUtil.Tag TAG = Utils.getTestTag(ConnectWifiOperator.class
            .getSimpleName());
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
        if (index == INDEX_CONNECT) {
            return "Connect to wifi";
        } else if (index == INDEX_DISCONNECT) {
            return "Disconnect wifi";
        } else {
            return null;
        }
    }

    @Override
    protected void doOperate(int index) {
        if (index == INDEX_CONNECT) {
            switchOn();
        } else if (index == INDEX_DISCONNECT) {
            switchOff();
        }
    }

    private void switchOn() {
        // launch setting
        Intent intent = Utils.getContext().getPackageManager()
                .getLaunchIntentForPackage(SYSTEM_SETTING_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear out any previous instances
        Utils.getContext().startActivity(intent);
        Utils.assertObject(By.pkg(SYSTEM_SETTING_PACKAGE));

        // open wifi page
        UiObject2 wifi = Utils.scrollOnScreenToFind(By.text("Wi‑Fi"));
        Utils.assertRightNow(wifi != null);
        wifi.click();

        // if is switch on, not switch on again
        // if not switch off, switch on it
        UiObject2 switchButton = Utils.findObject(By.clazz("android.widget.Switch"));
        Utils.assertRightNow(switchButton != null);
        if (switchButton.isChecked() == false) {
            LogHelper.d(TAG, "[doOperate] Switch on wifi");
            switchButton.click();
            Utils.assertObject(By.clazz("android.widget.Switch").checked(true));
        }

        // if has connected to one, return
        if (Utils.waitObject(By.text("Connected"), Utils.TIME_OUT_SHORT)) {
            LogHelper.d(TAG, "[doOperate] Already connected to one network, return");
            Utils.pressBackUtilFindNoObject(By.pkg(SYSTEM_SETTING_PACKAGE), 5);
            return;
        }

        // if not connected one, connect
        UiObject2 wifiName = Utils.scrollOnScreenToFind(By.text(Utils.WIFI_NAME));
        Utils.assertRightNow(wifiName != null);
        LogHelper.d(TAG, "[doOperate] Click wifi name [" + Utils.WIFI_NAME + "]");
        wifiName.click();

        // input password
        UiObject2 wifiPassWord = Utils.findObject(By.clazz("android.widget.EditText"));
        Utils.assertRightNow(wifiPassWord != null);
        LogHelper.d(TAG, "[doOperate] Inout password [" + Utils.WIFI_PASSWORD + "]");
        wifiPassWord.setText(Utils.WIFI_PASSWORD);

        // click connect
        UiObject2 connect = Utils.findObject(By.text("CONNECT").clazz("android.widget.Button")
                .enabled(true));
        Utils.assertRightNow(connect != null);
        LogHelper.d(TAG, "[doOperate] Click CONNECT button");
        connect.click();

        // check connected
        Utils.assertObject(By.text("Connected"));
        LogHelper.d(TAG, "[doOperate] Wifi connected");

        // exit settings
        Utils.pressBackUtilFindNoObject(By.pkg(SYSTEM_SETTING_PACKAGE), 5);
    }

    private void switchOff() {
        // launch setting
        Intent intent = Utils.getContext().getPackageManager()
                .getLaunchIntentForPackage(SYSTEM_SETTING_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear out any previous instances
        Utils.getContext().startActivity(intent);
        Utils.assertObject(By.pkg(SYSTEM_SETTING_PACKAGE));

        // open wifi page
        UiObject2 wifi = Utils.scrollOnScreenToFind(By.text("Wi‑Fi"));
        Utils.assertRightNow(wifi != null);
        wifi.click();

        // if is switch off, not switch off again
        // if not switch off, switch off it
        UiObject2 switchButton = Utils.findObject(By.clazz("android.widget.Switch"));
        Utils.assertRightNow(switchButton != null);
        if (switchButton.isChecked() == true) {
            LogHelper.d(TAG, "[doOperate] Switch off wifi");
            switchButton.click();
            Utils.assertObject(By.clazz("android.widget.Switch").checked(false));
        }

        // exit settings
        Utils.pressBackUtilFindNoObject(By.pkg(SYSTEM_SETTING_PACKAGE), 5);
    }
}
