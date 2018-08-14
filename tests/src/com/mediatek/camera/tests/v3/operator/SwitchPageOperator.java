package com.mediatek.camera.tests.v3.operator;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

public class SwitchPageOperator extends Operator {
    private static final LogUtil.Tag TAG = Utils.getTestTag(
            SwitchPageOperator.class.getSimpleName());

    public static final int INDEX_PREVIEW = 0;
    public static final int INDEX_MODE_LIST = 1;
    public static final int INDEX_SETTINGS = 2;

    private Page mCurrentPage;

    private BySelector mPreviewSelector = By.res("com.mediatek" +
            ".camera:id/preview_surface").enabled(true);
    private BySelector mModeListSelector = By.res("com.mediatek.camera:id/mode_title");
    private BySelector mSettingsSelector = By.text("Settings");

    @Override
    public int getOperatorCount() {
        return 3;
    }

    @Override
    protected void doOperate(int index) {
        initCurrentPage();
        if (mCurrentPage == null) {
            LogHelper.d(TAG, "[doOperate] init current page fail, return");
            return;
        }
        switch (index) {
            case INDEX_PREVIEW:
                gotoPreviewState();
                break;
            case INDEX_MODE_LIST:
                gotoModeListState();
                break;
            case INDEX_SETTINGS:
                gotoSettingsState();
                break;
            default:
                return;
        }
    }

    @Override
    public Page getPageBeforeOperate(int index) {
        return null;
    }

    @Override
    public Page getPageAfterOperate(int index) {
        switch (index) {
            case INDEX_PREVIEW:
                return Page.PREVIEW;
            case INDEX_MODE_LIST:
                return Page.MODE_LIST;
            case INDEX_SETTINGS:
                return Page.SETTINGS;
            default:
                break;
        }
        return null;
    }

    @Override
    public String getDescription(int index) {
        switch (index) {
            case INDEX_MODE_LIST:
                return "Go to mode list";
            case INDEX_PREVIEW:
                return "Go to preview";
            case INDEX_SETTINGS:
                return "Go to settings list";
            default:
                break;
        }
        return null;
    }

    private void initCurrentPage() {
        if (Utils.findObject(mModeListSelector, Utils.TIME_OUT_RIGHT_NOW) != null) {
            mCurrentPage = Page.MODE_LIST;
        } else if (Utils.findObject(mSettingsSelector, Utils.TIME_OUT_RIGHT_NOW) != null) {
            mCurrentPage = Page.SETTINGS;
        } else {
            mCurrentPage = Page.PREVIEW;
        }
    }

    private void gotoPreviewState() {
        switch (mCurrentPage) {
            case SETTINGS:
                Utils.getUiDevice().pressBack();
                break;
            case MODE_LIST:
                Utils.getUiDevice().pressBack();
                break;
            default:
                break;
        }
    }

    private void gotoModeListState() {
        switch (mCurrentPage) {
            case SETTINGS:
                Utils.getUiDevice().pressBack();
            case PREVIEW:
                UiObject2 modeEnter = Utils.findObject(By.res("com.mediatek" +
                        ".camera:id/mode").clickable(true).enabled(true));
                if (modeEnter != null) {
                    modeEnter.click();
                } else {
                    LogHelper.d(TAG, "[gotoModeListState] modeEnter is null, return");
                    return;
                }
            default:
                break;
        }
    }

    private void gotoSettingsState() {
        switch (mCurrentPage) {
            case PREVIEW:
                UiObject2 modeEnter = Utils.findObject(By.res("com.mediatek.camera:id/mode")
                        .clickable(true).enabled(true));
                if (modeEnter != null) {
                    modeEnter.click();
                } else {
                    LogHelper.d(TAG, "[gotoSettingsState] modeEnter is null, return");
                    return;
                }
            case MODE_LIST:
                UiObject2 settingEnter = Utils.findObject(By.res("com.mediatek" +
                        ".camera:id/setting_view").clickable(true));
                if (settingEnter != null) {
                    settingEnter.click();
                } else if (Utils.findObject(mSettingsSelector) != null) {
                    LogHelper.d(TAG, "[gotoSettingsState] already in setting, return");
                } else {
                    LogHelper.d(TAG, "[gotoSettingsState] settingEnter is null, return");
                    return;
                }
            default:
                break;
        }
    }
}
