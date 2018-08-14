package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.CameraBasicTest;
import com.mediatek.camera.tests.v3.annotation.module.VideoRecorderTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.VideoReviewUIChecker;
import com.mediatek.camera.tests.v3.operator.StartRecordOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.VideoReviewUIOperator;

import org.junit.Test;

public class IntentVideoWithoutUriTestCase extends BaseIntentVideoWithoutUriTestCase {
    @Test
    @FunctionTest
    @CameraBasicTest
    @VideoRecorderTest
    public void testIntentVideoWithoutUri() {
        new MetaCase()
                .addOperator(new SwitchCameraOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new StartRecordOperator(true))
                .addChecker(new VideoReviewUIChecker())
                .addOperator(new VideoReviewUIOperator())
                .run();
    }
}
