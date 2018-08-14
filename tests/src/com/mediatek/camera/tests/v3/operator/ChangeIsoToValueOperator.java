package com.mediatek.camera.tests.v3.operator;

import com.mediatek.camera.tests.v3.arch.TestContext;
import com.mediatek.camera.tests.v3.util.Utils;

public class ChangeIsoToValueOperator extends SettingRadioButtonOperator {
    private String mTargetIsoValue;

    public ChangeIsoToValueOperator(String targetIsoValue) {
        mTargetIsoValue = targetIsoValue;
    }

    @Override
    protected void doOperate(int index) {
        super.doOperate(index);
        TestContext.mLatestIsoSettingValue = mTargetIsoValue;
    }

    @Override
    protected int getSettingOptionsCount() {
        return 1;
    }

    @Override
    protected String getSettingTitle() {
        return "ISO";
    }

    @Override
    protected String getSettingOptionTitle(int index) {
        return mTargetIsoValue;
    }

    @Override
    protected boolean isUseOptionTitleAsSummary() {
        return true;
    }

    @Override
    public boolean isSupported(int index) {
        return Utils.isFeatureSupported("com.mediatek.camera.at.iso");
    }
}
