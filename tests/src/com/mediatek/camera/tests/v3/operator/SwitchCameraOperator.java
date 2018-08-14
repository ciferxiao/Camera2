package com.mediatek.camera.tests.v3.operator;

public class SwitchCameraOperator extends QuickSwitchOnOffOperator {
    public static final int INDEX_BACK = 0;
    public static final int INDEX_FRONT = 1;

    private boolean mIsNeedAssert = true;

    private static final String[] CONTENT_DESC = {"back", "front"};

    public SwitchCameraOperator() {
        this(true);
    }

    public SwitchCameraOperator(boolean isNeedAssert) {
        mIsNeedAssert = isNeedAssert;
    }

    @Override
    protected String getSwitchIconContentDesc(int index) {
        return CONTENT_DESC[index];
    }

    @Override
    protected String getSwitchIconResourceId() {
        return "com.mediatek.camera:id/camera_switcher";
    }

    @Override
    protected boolean isNeedAssert() {
        return mIsNeedAssert;
    }

    @Override
    public String getDescription(int index) {
        return "Switch to " + CONTENT_DESC[index] + " camera";
    }
}
