package com.mediatek.camera.tests.v3.arch;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.annotation.CoverPoint;
import com.mediatek.camera.tests.v3.annotation.NotCoverPoint;
import com.mediatek.camera.tests.v3.checker.PageChecker;
import com.mediatek.camera.tests.v3.operator.SwitchPageOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import java.lang.annotation.Annotation;

public abstract class Checker {
    public final void check(int index) {
        // [Check] prepare condition before check
        Page checkerBeforePage = getPageBeforeCheck(index);
        if (checkerBeforePage != null) {
            gotoPage(checkerBeforePage);
            checkPage(checkerBeforePage);
        }
        // [Check] check
        LogHelper.d(getLogTag(), "[check] " + getDescription(index));
        long timeBegin = System.currentTimeMillis();
        doCheck(index);
        long timeEnd = System.currentTimeMillis();
        LogHelper.d(getLogTag(), "[check] cost " + (timeEnd - timeBegin)
                + " ms");
    }

    public float getCheckCoverage() {
        Annotation[] annotations = this.getClass().getAnnotations();
        int coverPointNum = 0;
        int notCoverPointNum = 0;
        for (Annotation a : annotations) {
            if (a instanceof CoverPoint) {
                coverPointNum += ((CoverPoint) a).pointList().length;
            } else if (a instanceof NotCoverPoint) {
                notCoverPointNum += ((NotCoverPoint) a).pointList().length;
            }
        }
        float coverage;
        if (coverPointNum == 0 && notCoverPointNum == 0) {
            coverage = 1.f;
        } else if (coverPointNum == 0 && notCoverPointNum != 0) {
            coverage = 0.f;
        } else if (coverPointNum != 0 && notCoverPointNum == 0) {
            coverage = 1.f;
        } else {
            coverage = (float) coverPointNum / (float) (coverPointNum + notCoverPointNum);
        }
        return coverage;
    }

    public boolean isSupported(int index) {
        return true;
    }

    public final boolean isSupported() {
        int count = getCheckCount();
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

    public abstract int getCheckCount();

    public abstract Page getPageBeforeCheck(int index);

    public abstract String getDescription(int index);

    protected abstract void doCheck(int index);
}
