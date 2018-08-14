package com.mediatek.camera.tests.v3.operator;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.TestContext;
import com.mediatek.camera.tests.v3.util.Utils;

public class WhiteBalanceOperator extends SettingRadioOptionsOneByOneOperator {
    public static final int INDEX_FIRST_OPTION_AUTO = 0;
    public static final int INDEX_SECOND_OPTION = 1;
    private static final LogUtil.Tag TAG = Utils.getTestTag(WhiteBalanceOperator.class
            .getSimpleName());
    private static final String TITLE = "White balance";

    public WhiteBalanceOperator() {
        super(TITLE, true, false);
    }

    @Override
    public int getOperatorCount() {
        if (Utils.isFeatureSupported("com.mediatek.camera.at.whitebalance")) {
            return super.getOperatorCount();
        } else {
            return 0;
        }
    }

    @Override
    protected void doOperate(int index) {
        super.doOperate(index);
    }

    @Override
    public boolean isSupported(int index) {
        return Utils.isFeatureSupported("com.mediatek.camera.at.whitebalance");
    }

    @Override
    public String getDescription(int index) {
        TestContext.mLatestWhiteBalanceSettingValue = getSettingOptionTitle(index);
        return super.getDescription(index);
    }
}