package com.mediatek.camera.tests.v3.checker;

import android.database.Cursor;
import android.provider.MediaStore;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.Checker;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.arch.TestContext;
import com.mediatek.camera.tests.v3.util.Utils;

public class CapturedPhotoLocationChecker extends Checker {
    public static final int INDEX_HAS_LOCATION = 0;
    public static final int INDEX_NO_LOCATION = 1;

    private static final LogUtil.Tag TAG = Utils.getTestTag(CapturedPhotoLocationChecker.class
            .getSimpleName());

    private static final String[] PROJECTION_IMAGE = new String[]{
            MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.LATITUDE,
            MediaStore.Images.ImageColumns.LONGITUDE
    };
    private static final int INDEX_LAT = 1;
    private static final int INDEX_LON = 2;
    private static final String WHERE_CLAUSE_IMAGE = MediaStore.Images.ImageColumns.DATA +
            " = ?";

    @Override
    public int getCheckCount() {
        return 2;
    }

    @Override
    public Page getPageBeforeCheck(int index) {
        return null;
    }

    @Override
    public String getDescription(int index) {
        if (index == INDEX_HAS_LOCATION) {
            return "Check there is location info of captured photo in media database";
        } else if (index == INDEX_NO_LOCATION) {
            return "Check there is not location info of captured photo in media database";
        } else {
            return null;
        }
    }

    @Override
    protected void doCheck(int index) {
        Cursor cursor = Utils.getTargetContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PROJECTION_IMAGE,
                WHERE_CLAUSE_IMAGE,
                new String[]{String.valueOf(TestContext.mLatestPhotoPath)},
                null);
        Utils.assertRightNow(cursor != null && cursor.moveToFirst() && cursor.getCount() == 1);
        double lat = cursor.getDouble(INDEX_LAT);
        double lon = cursor.getDouble(INDEX_LON);
        cursor.close();
        LogHelper.d(TAG, "[doCheck] read from media db, latitude = " + lat + ", longitude = " +
                lon);
        if (index == INDEX_HAS_LOCATION) {
            Utils.assertRightNow(lat != 0 || lon != 0);
        } else if (index == INDEX_NO_LOCATION) {
            Utils.assertRightNow(lat == 0 && lon == 0);
        }
    }
}
