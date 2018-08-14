package com.mediatek.camera.tests.v3.observer;

import com.mediatek.camera.tests.v3.util.Utils;

public class ZoomRatioObserver extends LogKeyValueObserver {
    private static final String LOG_TAG = "CamAp_ZoomParameterCon";
    private static final String ZOOM_RATIO_LOG_PREFIX = "mZoomLevel = ";
    private static final int ZOOM_RATIO_NUM_LENGTH_MAX = "10".length();
    private static final int ZOOM_RATIO_NUM_LENGTH_MIN = "1".length();

    private int mTargetZoomLevel;
    private boolean mAppeared = false;

    public ZoomRatioObserver(int zoomLevel) {
        mTargetZoomLevel = zoomLevel;
    }

    @Override
    protected String getObservedTag(int index) {
        return LOG_TAG;
    }

    @Override
    protected String getObservedKey(int index) {
        return ZOOM_RATIO_LOG_PREFIX;
    }

    @Override
    protected ValueType getValueType(int index) {
        return ValueType.INTEGER;
    }

    @Override
    protected int getMinValueStringLength(int index) {
        return ZOOM_RATIO_NUM_LENGTH_MIN;
    }

    @Override
    protected int getMaxValueStringLength(int index) {
        return ZOOM_RATIO_NUM_LENGTH_MAX;
    }

    @Override
    protected void onObserveBegin(int index) {
        mAppeared = false;
    }

    @Override
    protected void onValueComing(int index, Object value) {
        if (((Integer) value) == mTargetZoomLevel) {
            mAppeared = true;
        } else {
            mAppeared = false;
        }
    }

    @Override
    protected void onObserveEnd(int index) {
        Utils.assertRightNow(mAppeared);
    }

    @Override
    protected boolean isAlreadyFindTarget(int index) {
        return mAppeared;
    }

    @Override
    public int getObserveCount() {
        return 1;
    }

    @Override
    public String getDescription(int index) {
        return "Observe has zoomed to ratio " + mTargetZoomLevel;
    }
}
