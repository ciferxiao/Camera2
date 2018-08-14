package com.mediatek.camera.tests.v3.operator;

import com.mediatek.camera.tests.v3.arch.OperatorOne;
import com.mediatek.camera.tests.v3.arch.Page;

public class SwitchToPanoramaModeOperator extends OperatorOne {
    @Override
    protected void doOperate() {
        new SwitchToModeOperator("Panorama").operate(0);
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
        return "Switch to panorama mode";
    }
}
