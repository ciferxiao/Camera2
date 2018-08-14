package com.mediatek.camera.tests.v3.arch;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.checker.PageChecker;
import com.mediatek.camera.tests.v3.operator.SwitchPageOperator;
import com.mediatek.camera.tests.v3.util.Utils;

public abstract class Operator {
    public void operate(int index) {
        // [Operate] prepare condition before operate
        Page operatorBeforePage = getPageBeforeOperate(index);
        Page operatorAfterPage = getPageAfterOperate(index);
        if (operatorBeforePage != null) {
            gotoPage(operatorBeforePage);
            checkPage(operatorBeforePage);
        }

        // [Operate] operate
        LogHelper.d(getLogTag(), "[operate] " + getDescription(index));
        long timeBegin = System.currentTimeMillis();
        doOperate(index);
        long timeEnd = System.currentTimeMillis();
        LogHelper.d(getLogTag(), "[operate] cost " + (timeEnd - timeBegin)
                + " ms");

        // [Operate] check page after operate
        if (operatorAfterPage != null) {
            checkPage(operatorAfterPage);
        }
    }

    public boolean isSupported(int index) {
        return true;
    }

    public final boolean isSupported() {
        int count = getOperatorCount();
        for (int i = 0; i < count; i++) {
            if (isSupported(i)) {
                return true;
            }
        }
        return false;
    }

    private static void gotoPage(Page page) {
        SwitchPageOperator pageSwitchOperator = new SwitchPageOperator();
        switch (page) {
            case PREVIEW:
                pageSwitchOperator.operate(SwitchPageOperator.INDEX_PREVIEW);
                break;
            case MODE_LIST:
                pageSwitchOperator.operate(SwitchPageOperator.INDEX_MODE_LIST);
                break;
            case SETTINGS:
                pageSwitchOperator.operate(SwitchPageOperator.INDEX_SETTINGS);
                break;
        }
    }

    private static void checkPage(Page page) {
        PageChecker pageChecker = new PageChecker();
        switch (page) {
            case PREVIEW:
                pageChecker.check(PageChecker.INDEX_PREVIEW);
                break;
            case MODE_LIST:
                pageChecker.check(PageChecker.INDEX_MODE_LIST);
                break;
            case SETTINGS:
                pageChecker.check(PageChecker.INDEX_SETTINGS);
                break;
        }
    }

    private final LogUtil.Tag getLogTag() {
        return Utils.getTestTag(getClass().getSimpleName());
    }

    public abstract int getOperatorCount();

    public abstract Page getPageBeforeOperate(int index);

    public abstract Page getPageAfterOperate(int index);

    public abstract String getDescription(int index);

    protected abstract void doOperate(int index);
}