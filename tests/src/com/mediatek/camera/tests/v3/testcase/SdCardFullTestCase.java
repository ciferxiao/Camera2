package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.CameraBasicTest;
import com.mediatek.camera.tests.v3.annotation.group.DualCameraTest;
import com.mediatek.camera.tests.v3.annotation.group.PipTest;
import com.mediatek.camera.tests.v3.annotation.module.NormalPhotoTest;
import com.mediatek.camera.tests.v3.annotation.module.PanoramaTest;
import com.mediatek.camera.tests.v3.annotation.module.PipPhotoTest;
import com.mediatek.camera.tests.v3.annotation.module.PipVideoTest;
import com.mediatek.camera.tests.v3.annotation.module.SlowMotionTest;
import com.mediatek.camera.tests.v3.annotation.module.StereoCaptureTest;
import com.mediatek.camera.tests.v3.annotation.module.StorageTest;
import com.mediatek.camera.tests.v3.annotation.module.VideoRecorderTest;
import com.mediatek.camera.tests.v3.annotation.module.VsdofTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.StorageHintChecker;
import com.mediatek.camera.tests.v3.observer.MediaSavedObserver;
import com.mediatek.camera.tests.v3.operator.CaptureOrRecordOperator;
import com.mediatek.camera.tests.v3.operator.SwitchAllModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchPhotoVideoOperator;

import org.junit.Test;

public class SdCardFullTestCase extends BaseSdCardFullTestCase {
    @Test
    @FunctionTest
    @CameraBasicTest
    @DualCameraTest
    @PipTest
    @StorageTest
    @NormalPhotoTest
    @VideoRecorderTest
    @PipPhotoTest
    @PipVideoTest
    @PanoramaTest
    @SlowMotionTest
    @VsdofTest
    @StereoCaptureTest
    public void testCaptureWhenSdCardFull() {
        new MetaCase("TEMP_MODE_0031")
                .addOperator(new SwitchAllModeOperator(false))
                .acrossBegin()
                .addOperator(new SwitchPhotoVideoOperator())
                .acrossEnd()
                .addChecker(new StorageHintChecker(), StorageHintChecker.INDEX_SHOW)
                .addChecker(new PreviewChecker())
                .observeBegin(new MediaSavedObserver(), MediaSavedObserver.INDEX_NO_SAVED)
                .addOperator(new CaptureOrRecordOperator())
                .observeEnd()
                .run();
    }
}
