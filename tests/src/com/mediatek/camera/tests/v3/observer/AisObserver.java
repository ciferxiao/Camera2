package com.mediatek.camera.tests.v3.observer;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.util.Utils;

/**
 * Class to observer effect parameters.
 */

public class AisObserver extends AbstractLogObserver {
    private static final LogUtil.Tag TAG = Utils.getTestTag(
            AisObserver.class.getSimpleName());

    private static final String LOG_TAG = "MtkCam/ParamsManager";
    private static final String KEY_STRING = "mfb=";
    private boolean mPass = false;

    private static final String[] EFFECT_NAME = {
            "ais",
            "off",
    };

    @Override
    protected String[] getObservedTagList(int index) {
        return new String[]{LOG_TAG};
    }

    @Override
    protected String[] getObservedKeyList(int index) {
        return new String[]{KEY_STRING};
    }

    @Override
    public int getObserveCount() {
        return 2;
    }

    @Override
    public String getDescription(int index) {
        return "Check AIS: " + EFFECT_NAME[index];
    }

    @Override
    protected void onObserveEnd(int index) {
        Utils.assertRightNow(mPass);
    }

    @Override
    protected void onObserveBegin(int index) {
        mPass = false;
    }

    @Override
    protected boolean isAlreadyFindTarget(int index) {
        return mPass;
    }

    @Override
    protected void onLogComing(int index, String line) {
        int beginIndex = line.indexOf(KEY_STRING) + KEY_STRING.length();
        int endIndex = line.indexOf(";", beginIndex);
        String subString = line.substring(beginIndex, endIndex);
        LogHelper.d(TAG, "[onLogComing], mfb=" + subString + ", index = " + index);
        mPass = EFFECT_NAME[index].equals(subString);
    }
}
