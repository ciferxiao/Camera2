package com.mediatek.camera.tests.v3.observer;

import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.tests.v3.arch.TestContext;

public class WhiteBalanceLogObserver extends LogPrintObserver {
    @Override
    protected String getObserveLogTag(int index) {
        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                return "CamAp_WhiteBalancePara";
            case API2:
                return "CamAp_WhiteBalanceCapt";
            default:
                return null;
        }
    }

    @Override
    protected String getObserveLogKey(int index) {
        return "value:" + TestContext.mLatestWhiteBalanceSettingValue
                .replace(" ", "-").toLowerCase().toString();
    }

    @Override
    public int getObserveCount() {
        return 1;
    }
}
