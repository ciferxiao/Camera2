package com.mediatek.camera.tests.v3.util;

import android.os.Environment;
import android.os.SystemClock;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.TestContext;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Condition {
    private static final LogUtil.Tag TAG = Utils.getTestTag(Condition.class.getSimpleName());
    private static final String NOT_CONNECT_CAMERA = "Can't connect to the camera. Please make" +
            " sure to close other apps that may use camera or flashlight.";

    private UiDevice mUiDevice;

    public abstract boolean isSatisfied();

    public Condition() {
        mUiDevice = Utils.getUiDevice();
    }

    public boolean waitMe(int timeout) {
        long timeOutPoint = SystemClock.elapsedRealtime() + timeout;
        while (SystemClock.elapsedRealtime() <= timeOutPoint) {
            if (isSatisfied()) {
                return true;
            } else {
                waitSafely(20);
            }
        }
        return false;
    }

    public void assertMe(int timeout, String failMessage) {
        if (waitMe(timeout) == true) {
            return;
        }
        String dirPath = Environment.getExternalStorageDirectory() + "/mtklog/mobilelog/";
        File dirFile = new File(dirPath);
        File latestLogFolder = null;
        if (dirFile.exists() && dirFile.isDirectory()) {
            File[] logFolders = dirFile.listFiles();
            long lastModifiedTime = -1;
            for (File logFolder : logFolders) {
                if (logFolder.isDirectory() && lastModifiedTime <= logFolder.lastModified()) {
                    lastModifiedTime = logFolder.lastModified();
                    latestLogFolder = logFolder;
                }
            }
        }
        if (latestLogFolder == null) {
            dirPath = Environment.getExternalStorageDirectory() + "/CameraAssertFail/";
        } else {
            dirPath = latestLogFolder.getAbsolutePath() + "/CameraAssertFail/";
        }
        File file = new File(dirPath);
        file.exists();
        file.mkdirs();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss");
        Date date = new Date();
        String time = sdf.format(date);

        // dump latest photos/videos
        LogHelper.d(TAG, "[assertMe] dump latest photos/videos to " + dirPath);
        if (TestContext.mLatestPhotoPath != null) {
            Utils.copyFile(TestContext.mLatestPhotoPath,
                    dirPath + Utils.getFileName(TestContext.mLatestPhotoPath));
        }
        if (TestContext.mLatestVideoPath != null) {
            Utils.copyFile(TestContext.mLatestVideoPath,
                    dirPath + Utils.getFileName(TestContext.mLatestVideoPath));
        }
        if (TestContext.mLatestDngPath != null) {
            Utils.copyFile(TestContext.mLatestDngPath,
                    dirPath + Utils.getFileName(TestContext.mLatestDngPath));
        }
        if (TestContext.mLatestCsPhotoPath != null) {
            for (String photo : TestContext.mLatestCsPhotoPath) {
                Utils.copyFile(photo,
                        dirPath + Utils.getFileName(photo));
            }
        }

        // dump window hierarchy
        String targetPath = dirPath + time + ".uix";
        File hierarchyDumpFile = new File(targetPath);
        try {
            mUiDevice.dumpWindowHierarchy(hierarchyDumpFile);
            LogHelper.d(TAG, "[assertMe] dump window hierarchy to " + targetPath);
        } catch (IOException e) {
            LogHelper.d(TAG, "[assertMe] Fail to dump window hierarchy", e);
        }

        // dump screen shot
        targetPath = dirPath + time + ".png";
        LogHelper.d(TAG, "[assertMe] dump screenshot to " + targetPath);
        boolean successToDumpScreenShot = takeScreenShot(targetPath);

        // reboot
        if (Utils.waitObject(By.text(NOT_CONNECT_CAMERA), Utils.TIME_OUT_SHORT_SHORT)) {
            LogHelper.d(TAG, "[assertMe] Asset fail due to not connect camera, try to reboot " +
                    "device");
            Utils.rebootDevice();
        }

        if (successToDumpScreenShot) {
            Assert.assertTrue(failMessage + "<Success to save screenshot at " + targetPath + ">",
                    false);
        } else {
            Assert.assertTrue(failMessage + "<Fail to save screenshot at " + targetPath + ">",
                    false);
        }
    }

    public void assertMe(int timeout) {
        assertMe(timeout, null);
    }

    private boolean takeScreenShot(String targetPath) {
        return mUiDevice.takeScreenshot(new File(targetPath));
    }

    private void waitSafely(long millseconds) {
        mUiDevice.waitForIdle();
        try {
            synchronized (mUiDevice) {
                mUiDevice.wait(millseconds);
            }
        } catch (InterruptedException e) {
        }
    }
}
