package com.mediatek.camera.tests.v3.operator;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.OperatorOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.arch.TestContext;
import com.mediatek.camera.tests.v3.util.Condition;
import com.mediatek.camera.tests.v3.util.Utils;

public class RecordVideoOperator extends OperatorOne {
    private static final LogUtil.Tag TAG = Utils.getTestTag(RecordVideoOperator.class
            .getSimpleName());
    private int mDurationInSecond = 5;

    public RecordVideoOperator setDuration(int s) {
        mDurationInSecond = s;
        return this;
    }

    @Override
    protected void doOperate() {
        // begin record
        UiObject2 shutter = Utils.findObject(By.res("com.mediatek.camera:id/shutter_image").desc
                ("Video"));
        Utils.assertRightNow(shutter != null);
        shutter.click();

        // wait duration
        Utils.assertCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return getDurationFromText() >= mDurationInSecond;
            }
        }, mDurationInSecond * 1000 + Utils.TIME_OUT_LONG);

        // end record
        UiObject2 shutterStop = Utils.findObject(By.res("com.mediatek" +
                ".camera:id/video_stop_shutter"));
        Utils.assertRightNow(shutterStop != null);
        shutterStop.click();

        TestContext.mLatestRecordVideoDurationInSeconds = mDurationInSecond;
    }

    @Override
    public Page getPageBeforeOperate() {
        return Page.PREVIEW;
    }

    @Override
    public Page getPageAfterOperate() {
        return Page.PREVIEW;
    }

    @Override
    public String getDescription() {
        return "Record video for " + mDurationInSecond + " s";
    }

    private String getTextOfDuration() {
        int min = mDurationInSecond / 60;
        int second = mDurationInSecond % 60;
        StringBuilder sb = new StringBuilder();
        if (min < 10) {
            sb.append("0").append(min);
        } else {
            sb.append(min);
        }
        sb.append(":");
        if (second < 10) {
            sb.append("0").append(second);
        } else {
            sb.append(second);
        }
        return sb.toString();
    }

    private int getDurationFromText() {
        UiObject2 recordingTime = Utils.findObject(By.res("com.mediatek.camera:id/recording_time"));
        if (recordingTime == null) {
            return 0;
        }
        String text = recordingTime.getText();
        String minutes = text.substring(0, text.indexOf(":"));
        String seconds = text.substring(text.indexOf(":") + 1, text.length());
        return Integer.valueOf(minutes) * 60 + Integer.valueOf(seconds);
    }
}
