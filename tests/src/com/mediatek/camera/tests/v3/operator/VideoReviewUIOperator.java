package com.mediatek.camera.tests.v3.operator;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.OperatorOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

/**
 * operator the intent video for review.
 */

public class VideoReviewUIOperator extends OperatorOne {
    private static final LogUtil.Tag TAG = Utils.getTestTag(VideoReviewUIOperator.class
            .getSimpleName());

    @Override
    protected void doOperate() {

        UiObject2 play = Utils.findObject(
                By.res("com.mediatek.camera:id/btn_play"), Utils.TIME_OUT_SHORT);
        Utils.assertRightNow(play != null);
        play.click();
        Utils.waitSafely(Utils.TIME_OUT_NORMAL);
        UiObject2 button = Utils.findObject(
                By.res("com.mediatek.camera:id/btn_retake"), Utils.TIME_OUT_SHORT);
        Utils.assertRightNow(button != null);
        button.click();
    }

    @Override
    public Page getPageBeforeOperate() {
        return null;
    }

    @Override
    public Page getPageAfterOperate() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Play intent video and retake";
    }
}
