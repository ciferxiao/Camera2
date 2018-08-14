package com.mediatek.camera.tests.v3.arch;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.util.Utils;

import java.util.List;

public class TestContext {
    private static final LogUtil.Tag TAG = Utils.getTestTag(TestContext.class.getSimpleName());

    public static String mLatestPhotoPath;
    public static String mLatestVideoPath;
    public static List<String> mLatestCsPhotoPath;
    public static String mLatestDngPath;

    public static long mLatestStartRecordTime;
    public static long mLatestStopRecordTime;
    public static int mLatestRecordVideoDurationInSeconds;

    public static int mLatestPictureSizeSettingWidth;
    public static int mLatestPictureSizeSettingHeight;
    public static int mLatestVideoSizeSettingWidth;
    public static int mLatestVideoSizeSettingHeight;

    public static String mLatestIsoSettingValue;
    public static String mLatestWhiteBalanceSettingValue;
}
