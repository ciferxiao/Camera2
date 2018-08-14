package com.mediatek.camera.tests.v3.operator;

import android.content.Context;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.OperatorOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.ReflectUtils;
import com.mediatek.camera.tests.v3.util.Utils;

import java.lang.reflect.Constructor;

public class CaptureOrRecordUntilFullOperator extends OperatorOne {
    private static final LogUtil.Tag TAG = Utils.getTestTag(CaptureOrRecordUntilFullOperator
            .class.getSimpleName());
    private static long CAPTURE_RESERVE_BYTE;
    private static long RECORD_RESERVE_BYTE;

    @Override
    protected void doOperate() {
        long reserveByte;
        UiObject2 shutter = Utils.findObject(By.res("com.mediatek.camera:id/shutter_image"));
        Utils.assertRightNow(shutter != null);
        if (shutter.getContentDescription().equals("Picture")) {
            reserveByte = CAPTURE_RESERVE_BYTE;
            LogHelper.d(TAG, "[doOperate] picture, target reserve " + reserveByte
                    + " byte, about " + (reserveByte / 1024 / 1024) + " MB");
        } else {
            reserveByte = RECORD_RESERVE_BYTE;
            LogHelper.d(TAG, "[doOperate] video, target reserve " + reserveByte
                    + " byte, about " + (reserveByte / 1024 / 1024) + " MB");
        }

        while (Utils.getReserveSpaceInByte() >= reserveByte) {
            new CaptureOrRecordOperator().operate(0);
            long currentReserveByte = Utils.getReserveSpaceInByte();
            LogHelper.d(TAG, "[doOperate] reserve space of " + Utils.getDefaultStoragePath()
                    + " is " + currentReserveByte + " byte, about "
                    + (currentReserveByte / 1024 / 1024) + " MB");
        }
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
        return "Capture photos, or record videos until storage full";
    }

    static {
        Constructor cons = ReflectUtils.getConstructor(
                "com.mediatek.camera.common.storage.Storage", Context.class);
        Object storage = ReflectUtils.createInstance(cons, Utils.getContext());
        CAPTURE_RESERVE_BYTE = (long) ReflectUtils.callMethodOnObject(
                storage, "getCaptureThreshold");
        RECORD_RESERVE_BYTE = (long) ReflectUtils.callMethodOnObject(
                storage, "getRecordThreshold");
    }
}
