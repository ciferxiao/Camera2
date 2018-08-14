package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.CameraExitedChecker;
import com.mediatek.camera.tests.v3.checker.CameraLaunchedChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailChecker;
import com.mediatek.camera.tests.v3.operator.ClearImagesVideosOperator;
import com.mediatek.camera.tests.v3.operator.ClearSharePreferenceOperator;
import com.mediatek.camera.tests.v3.operator.ExitCameraOperator;
import com.mediatek.camera.tests.v3.operator.LaunchCameraOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import org.junit.After;
import org.junit.Before;

public abstract class BaseCameraTestCase {
    private static final LogUtil.Tag TAG = Utils.getTestTag(
            BaseCameraTestCase.class.getSimpleName());

    @Before
    public void setUp() {
        LogHelper.d(TAG, "[setUp]");
        new MetaCase()
                .addOperator(new ClearImagesVideosOperator())
                .addOperator(new ClearSharePreferenceOperator())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_NORMAL)
                .addChecker(new CameraLaunchedChecker(), CameraLaunchedChecker.INDEX_NORMAL)
                .addChecker(new PreviewChecker())
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_NO_THUMB)
                .run();

    }

    @After
    public void tearDown() {
        LogHelper.d(TAG, "[tearDown]");
        new MetaCase()
                .addOperator(new ExitCameraOperator())
                .addChecker(new CameraExitedChecker())
                .addOperator(new ClearImagesVideosOperator())
                .addOperator(new ClearSharePreferenceOperator())
                .run();
    }
}
