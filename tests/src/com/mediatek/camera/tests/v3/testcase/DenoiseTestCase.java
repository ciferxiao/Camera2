package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.DualCameraTest;
import com.mediatek.camera.tests.v3.annotation.module.DualCameraDeNoiseTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailChecker;
import com.mediatek.camera.tests.v3.observer.PhotoSavedObserver;
import com.mediatek.camera.tests.v3.observer.VideoSavedObserver;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.RecordVideoOperator;
import com.mediatek.camera.tests.v3.operator.SwitchPhotoVideoOperator;

import org.junit.Test;

public class DenoiseTestCase extends BaseCameraTestCase {

    @Test
    @FunctionTest
    @DualCameraTest
    @DualCameraDeNoiseTest
    public void testCaptureAndRecordAlternateDenoise() {
        new MetaCase()
                .addOperator(new SwitchPhotoVideoOperator(),
                        SwitchPhotoVideoOperator.INDEX_VIDEO)
                .observeBegin(new VideoSavedObserver())
                .addOperator(new RecordVideoOperator().setDuration(10))
                .observeEnd()
                .addOperator(new SwitchPhotoVideoOperator(),
                        SwitchPhotoVideoOperator.INDEX_PHOTO)
                .repeatBegin(10)
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .addChecker(new PreviewChecker())
                .repeatEnd()
                .run();
    }
}
