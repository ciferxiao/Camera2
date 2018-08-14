package com.mediatek.camera.tests.v3.operator;

public class SceneModeOperator extends SettingRadioButtonOperator {
    public static final int INDEX_OFF = 0;
    public static final int INDEX_AUTO = 1;
    public static final int INDEX_NIGHT = 2;
    public static final int INDEX_SUNSET = 3;
    public static final int INDEX_PARTY = 4;
    public static final int INDEX_PORTRAIT = 5;
    public static final int INDEX_LANDSCAPE = 6;
    public static final int INDEX_NIGHT_PORTRAIT = 7;
    public static final int INDEX_THEATRE = 8;
    public static final int INDEX_BEACH = 9;
    public static final int INDEX_SNOW = 10;
    public static final int INDEX_STEADY_PHOTO = 11;
    public static final int INDEX_FIREWORKS = 12;
    public static final int INDEX_SPORTS = 13;
    public static final int INDEX_CANDLE_LIGHTS = 14;

    private static final String[] OPTIONS = {
            "Off",
            "Auto",
            "Night",
            "Sunset",
            "Party",
            "Portrait",
            "Landscape",
            "Night portrait",
            "Theatre",
            "Beach",
            "Snow",
            "Steady photo",
            "Fireworks",
            "Sports",
            "Candle light"
    };

    @Override
    protected int getSettingOptionsCount() {
        return OPTIONS.length;
    }

    @Override
    protected String getSettingTitle() {
        return "Scene mode";
    }

    @Override
    protected String getSettingOptionTitle(int index) {
        return OPTIONS[index];
    }

    @Override
    protected boolean isUseOptionTitleAsSummary() {
        return true;
    }
}
