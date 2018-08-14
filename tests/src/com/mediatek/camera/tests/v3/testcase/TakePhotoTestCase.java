package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.CameraBasicTest;
import com.mediatek.camera.tests.v3.annotation.module.NormalPhotoTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.annotation.type.SanityTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.CameraFacingChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailShownInGalleryChecker;
import com.mediatek.camera.tests.v3.observer.PhotoSavedObserver;
import com.mediatek.camera.tests.v3.operator.BackToCameraOperator;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.GoToGalleryOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;

import org.junit.Test;

public class TakePhotoTestCase extends BaseCameraTestCase {
    /**
     * test take photo.
     * Step 1: take main sensor begin
     * Step 2: click photo button
     * Step 3: check capture image number
     * Step 4: take sub sensor begin
     * Step 5: switch to sub sensor
     * Step 6: go to gallery
     */
    @Test
    @SanityTest
    @CameraBasicTest
    @FunctionTest
    @NormalPhotoTest
    public void testTakePhoto() {
        new MetaCase("TC_029")
                .addOperator(new SwitchCameraOperator())
                .addChecker(new CameraFacingChecker())
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .addOperator(new GoToGalleryOperator())
                .addChecker(new ThumbnailShownInGalleryChecker())
                .addOperator(new BackToCameraOperator())
                .addChecker(new PreviewChecker())
                .run();
    }
}
