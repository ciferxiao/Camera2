package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.DualCameraTest;
import com.mediatek.camera.tests.v3.annotation.module.StereoCaptureTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailChecker;
import com.mediatek.camera.tests.v3.checker.VSDOFBarChecker;
import com.mediatek.camera.tests.v3.observer.DofValueObserver;
import com.mediatek.camera.tests.v3.observer.PhotoSavedObserver;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.DragVSDOFBarOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByBackKeyOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByHomeKeyOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToModeOperator;

import org.junit.Test;

public class StereoCaptureTestCase extends BaseCameraTestCase {

    @Test
    @FunctionTest
    @DualCameraTest
    @StereoCaptureTest
    public void testStereoCaptureSwitchToDualCameraMode() {
        new MetaCase()
                .addOperator(new SwitchToModeOperator("Stereo"))
                .run();
    }

    @Test
    @FunctionTest
    @DualCameraTest
    @StereoCaptureTest
    public void testStereoCaptureTakePhotoZSDStress() {
        new MetaCase()
                .addOperator(new SwitchToModeOperator("Stereo"))
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .addChecker(new PreviewChecker())
                .runForTimes(10);
    }

    @Test
    @FunctionTest
    @DualCameraTest
    @StereoCaptureTest
    public void testStereoCaptureHomeKey() {
        new MetaCase()
                .addOperator(new SwitchToModeOperator("Stereo"))
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PreviewChecker())
                .run();
    }

    @Test
    @FunctionTest
    @DualCameraTest
    @StereoCaptureTest
    public void testStereoCaptureBackKey() {
        new MetaCase()
                .addOperator(new SwitchToModeOperator("Stereo"))
                .addOperator(new PauseResumeByBackKeyOperator())
                .addChecker(new PreviewChecker())
                .run();
    }

    @Test
    @FunctionTest
    @DualCameraTest
    @StereoCaptureTest
    public void testStereoCaptureSwitchMode() {
        new MetaCase()
                .addOperator(new SwitchToModeOperator("Stereo"))
                .addChecker(new PreviewChecker())
                .addOperator(new SwitchToModeOperator("Normal"))
                .addChecker(new PreviewChecker())
                .runForTimes(5);
    }

    @Test
    @FunctionTest
    @DualCameraTest
    @StereoCaptureTest
    public void testStereoCaptureVSDOFBar() {
        new MetaCase()
                .addOperator(new SwitchToModeOperator("Stereo"))
                .addChecker(new PreviewChecker())
                .addChecker(new VSDOFBarChecker(), VSDOFBarChecker.INDEX_HAS_VSDOFBAR)
                .run();
    }

    @Test
    @FunctionTest
    @DualCameraTest
    @StereoCaptureTest
    public void testStereoCaptureDragVSDOFBar() {
        new MetaCase()
                .addOperator(new SwitchToModeOperator("Stereo"))
                .addChecker(new PreviewChecker())
                .addChecker(new VSDOFBarChecker(), VSDOFBarChecker.INDEX_HAS_VSDOFBAR)
                .acrossBegin()
                .observeBegin(new DofValueObserver())
                .addOperator(new DragVSDOFBarOperator())
                .observeEnd()
                .acrossEnd()
                .run();
    }
}
