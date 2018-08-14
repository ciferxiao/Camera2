package com.mediatek.camera.tests.v3.observer;

import android.support.test.uiautomator.By;

import com.mediatek.camera.tests.v3.annotation.CoverPoint;

@CoverPoint(pointList = {"Check zoom ratio view has shown"})
public class ZoomUiObserver extends UiAppearedObserver {
    public ZoomUiObserver() {
        super(By.res("com.mediatek.camera:id/auto_hide_hint"), "zoom ratio view");
    }
}
