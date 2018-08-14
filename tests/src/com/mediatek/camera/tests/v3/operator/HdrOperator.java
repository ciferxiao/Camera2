package com.mediatek.camera.tests.v3.operator;

import com.mediatek.camera.tests.v3.util.Utils;

public class HdrOperator extends QuickSwitchOptionsOperator {
    public static final int INDEX_AUTO = 0;
    public static final int INDEX_ON = 1;
    public static final int INDEX_OFF = 2;

    private static final String SWITCH_ICON_RESOURCE = "com.mediatek.camera:id/hdr_icon";
    private static final String[] OPTION_RESOURCES = {
            "com.mediatek.camera:id/hdr_auto",
            "com.mediatek.camera:id/hdr_on",
            "com.mediatek.camera:id/hdr_off"};
    private static final String[] OPTIONS_TAG = {
            "com.mediatek.camera.at.hdr.auto",
            "com.mediatek.camera.at.hdr.on",
            "com.mediatek.camera.at.hdr.off",
    };

    @Override
    protected int getOptionsCount() {
        return 3;
    }

    @Override
    protected String getSwitchIconResourceId() {
        return SWITCH_ICON_RESOURCE;
    }

    @Override
    protected String getOptionsResourceId(int index) {
        return OPTION_RESOURCES[index];
    }

    @Override
    protected String getSwitchIconDescription(int index) {
        switch (index) {
            case INDEX_AUTO:
                return "HDR auto";
            case INDEX_ON:
                return "HDR on";
            case INDEX_OFF:
                return "HDR off";
        }
        return null;
    }

    @Override
    public boolean isSupported(int index) {
        return Utils.isFeatureSupported(OPTIONS_TAG[index]);
    }

    @Override
    public String getDescription(int index) {
        switch (index) {
            case INDEX_AUTO:
                return "Switch hdr as auto";
            case INDEX_OFF:
                return "Switch hdr as off";
            case INDEX_ON:
                return "Switch hdr as on";
        }
        return null;
    }
}
