package com.mediatek.camera.tests.v3;

import android.media.ExifInterface;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.CheckerOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.arch.TestContext;
import com.mediatek.camera.tests.v3.util.Utils;

public class ExifChecker extends CheckerOne {
    private static final LogUtil.Tag TAG = Utils.getTestTag(ExifChecker.class.getSimpleName());
    private static final int DEFAULT_INT_VALUE = -123456;
    private static final double DEFAULT_DOUBLE_VALUE = -123456.0d;

    @Override
    protected void doCheck() {
        if (TestContext.mLatestPhotoPath == null) {
            return;
        }

        double focalLength = Utils.getDoubleInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_FOCAL_LENGTH, DEFAULT_DOUBLE_VALUE);
        int whiteBalance = Utils.getIntInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_WHITE_BALANCE, DEFAULT_INT_VALUE);
        double exposureTime = Utils.getDoubleInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_EXPOSURE_TIME, DEFAULT_DOUBLE_VALUE);
        int iso = Utils.getIntInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_ISO_SPEED_RATINGS, DEFAULT_INT_VALUE);
        int flash = Utils.getIntInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_FLASH, DEFAULT_INT_VALUE);
        int width = Utils.getIntInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_IMAGE_WIDTH, DEFAULT_INT_VALUE);
        int height = Utils.getIntInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_IMAGE_LENGTH, DEFAULT_INT_VALUE);
        int orientation = Utils.getIntInExif(TestContext.mLatestPhotoPath,
                ExifInterface.TAG_ORIENTATION, DEFAULT_INT_VALUE);

        LogHelper.d(TAG, "[doCheck] TestContext.mLatestPhotoPath = " + TestContext.mLatestPhotoPath
                + ", focalLength = " + focalLength
                + ", whiteBalance = " + whiteBalance
                + ", exposureTime = " + exposureTime
                + ", iso = " + iso
                + ", flash = " + flash
                + ", width = " + width
                + ", height = " + height
                + ", orientation = " + orientation);

        Utils.assertRightNow(focalLength != DEFAULT_DOUBLE_VALUE);
        Utils.assertRightNow(whiteBalance != DEFAULT_INT_VALUE);
        Utils.assertRightNow(exposureTime != DEFAULT_DOUBLE_VALUE);
        Utils.assertRightNow(iso != DEFAULT_INT_VALUE);
        Utils.assertRightNow(flash != DEFAULT_INT_VALUE);
        Utils.assertRightNow(width != DEFAULT_INT_VALUE);
        Utils.assertRightNow(height != DEFAULT_INT_VALUE);
        Utils.assertRightNow(orientation != DEFAULT_INT_VALUE);
    }

    @Override
    public Page getPageBeforeCheck() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Check exif of latest capture photo, make sure focal length/white balance" +
                "/exposure time/iso/flash/width/height value is valid";
    }
}
