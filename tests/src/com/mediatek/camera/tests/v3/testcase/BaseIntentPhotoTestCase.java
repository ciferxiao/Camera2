package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.CameraExitedChecker;
import com.mediatek.camera.tests.v3.checker.CameraLaunchedChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.operator.ClearImagesVideosOperator;
import com.mediatek.camera.tests.v3.operator.ClearSharePreferenceOperator;
import com.mediatek.camera.tests.v3.operator.ExitCameraOperator;
import com.mediatek.camera.tests.v3.operator.LaunchCameraOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import org.junit.After;
import org.junit.Before;

public abstract class BaseIntentPhotoTestCase {
    private static final LogUtil.Tag TAG = Utils.getTestTag(
            BaseIntentPhotoTestCase.class.getSimpleName());

    @Before
    public void setUp() {
        LogHelper.d(TAG, "[setUp]");
        new MetaCase()
                .addOperator(new ClearImagesVideosOperator())
                .addOperator(new ClearSharePreferenceOperator())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_INTENT_PHOTO)
                .addChecker(new CameraLaunchedChecker(), CameraLaunchedChecker.INDEX_INTENT_PHOTO)
                .addChecker(new PreviewChecker())
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
