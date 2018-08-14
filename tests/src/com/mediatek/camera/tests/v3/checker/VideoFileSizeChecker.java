package com.mediatek.camera.tests.v3.checker;

import android.media.MediaPlayer;

import com.mediatek.camera.tests.v3.arch.CheckerOne;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.arch.TestContext;
import com.mediatek.camera.tests.v3.util.Utils;

import java.io.IOException;


public class VideoFileSizeChecker extends CheckerOne {

    @Override
    protected void doCheck() {
        if (TestContext.mLatestVideoPath != null) {
            // read info from file
            try {
                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(TestContext.mLatestVideoPath);
                mediaPlayer.prepare();
                int fileWidth = mediaPlayer.getVideoWidth();
                int fileHeight = mediaPlayer.getVideoHeight();
                assertVideoSize(fileWidth, fileHeight);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    public Page getPageBeforeCheck() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Check the video size of latest captured video";
    }

    private void assertVideoSize(int readWidth, int readHeight) {
        boolean mapping =
                readWidth == TestContext.mLatestVideoSizeSettingWidth
                        && readHeight == TestContext.mLatestVideoSizeSettingHeight;
        boolean wrapMapping =
                readWidth == TestContext.mLatestVideoSizeSettingHeight
                        && readHeight == TestContext.mLatestVideoSizeSettingWidth;
        Utils.assertRightNow(mapping || wrapMapping);
    }
}