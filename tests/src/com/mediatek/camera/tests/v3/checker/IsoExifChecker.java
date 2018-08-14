package com.mediatek.camera.tests.v3.checker;

import android.media.ExifInterface;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.annotation.CoverPoint;
import com.mediatek.camera.tests.v3.arch.CheckerOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.arch.TestContext;
import com.mediatek.camera.tests.v3.util.Utils;

@CoverPoint(pointList = {"Check iso value in exif"})
public class IsoExifChecker extends CheckerOne {
    private static final LogUtil.Tag TAG = Utils.getTestTag(IsoExifChecker.class.getSimpleName());

    @Override
    public Page getPageBeforeCheck() {
        return null;
    }

    @Override
    public String getDescription() {
        if (TestContext.mLatestIsoSettingValue.toLowerCase().equals("auto")) {
            return "Check latest captured photo ISO value in exif is > 0";
        } else {
            return "Check latest captured photo ISO value in exif is "
                    + TestContext.mLatestIsoSettingValue;
        }
    }

    @Override
    protected void doCheck() {
        if (TestContext.mLatestIsoSettingValue == null) {
            LogHelper.d(TAG, "[doCheck] TestContext.mLatestIsoSettingValue is null, return");
            return;
        }

        int isoValue = Utils.getIntInExif(TestContext.mLatestPhotoPath, ExifInterface
                .TAG_ISO_SPEED_RATINGS, -1);
        LogHelper.d(TAG, "[doCheck] isoValue = " + isoValue
                + ", TestContext.mLatestIsoSettingValue = " + TestContext.mLatestIsoSettingValue);
        if (TestContext.mLatestIsoSettingValue.toLowerCase().equals("auto")) {
            Utils.assertRightNow(isoValue > 0);
        } else {
            Utils.assertRightNow(isoValue == Integer.valueOf(TestContext.mLatestIsoSettingValue));
        }
    }
}
