package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.CameraBasicTest;
import com.mediatek.camera.tests.v3.annotation.module.DngTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.Checker;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.checker.CameraFacingChecker;
import com.mediatek.camera.tests.v3.checker.CameraLaunchedChecker;
import com.mediatek.camera.tests.v3.checker.DngIndicatorChecker;
import com.mediatek.camera.tests.v3.checker.DngModeRestrictionChecker;
import com.mediatek.camera.tests.v3.checker.HdrQuickSwitchChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.SettingSwitchOnOffChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailChecker;
import com.mediatek.camera.tests.v3.observer.DngSavedObserver;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.DngOperator;
import com.mediatek.camera.tests.v3.operator.HdrOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByHomeKeyOperator;
import com.mediatek.camera.tests.v3.operator.SwitchAllModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.SwitchPhotoVideoOperator;

import org.junit.Test;

@FunctionTest
@CameraBasicTest
@DngTest
public class DngTestCase extends BaseCameraTestCase {
    @Test
    public void testDngRestrictionWithHdr() {
        Operator dngOperator = new DngOperator();
        Checker dngChecker = new SettingSwitchOnOffChecker("RAW(.DNG)");
        Operator hdrOperator = new HdrOperator();
        Checker hdrChecker = new HdrQuickSwitchChecker();

        if (!dngOperator.isSupported() || !hdrOperator.isSupported()) {
            return;
        }

        dngOperator.operate(DngOperator.INDEX_SWITCH_ON);
        hdrChecker.check(HdrQuickSwitchChecker.INDEX_OFF);

        hdrOperator.operate(HdrOperator.INDEX_ON);
        dngChecker.check(SettingSwitchOnOffChecker
                .INDEX_SWITCH_OFF);

        if (hdrOperator.isSupported(HdrOperator.INDEX_AUTO)) {
            dngOperator.operate(DngOperator.INDEX_SWITCH_ON);
            hdrChecker.check(HdrQuickSwitchChecker.INDEX_OFF);

            hdrOperator.operate(HdrOperator.INDEX_AUTO);
            dngChecker.check(SettingSwitchOnOffChecker
                    .INDEX_SWITCH_OFF);
        }
    }

    @Test
    public void testDngRestrictionWithMode() {
        if (!new DngOperator().isSupported()) {
            return;
        }
        new MetaCase()
                .addOperator(new SwitchAllModeOperator(false))
                .acrossBegin()
                .addOperator(new SwitchPhotoVideoOperator())
                .acrossEnd()
                .addChecker(new DngModeRestrictionChecker())
                .run();
    }

    @Test
    public void testDngCapture() {
        if (!new DngOperator().isSupported()) {
            return;
        }
        new MetaCase()
                .addOperator(new SwitchCameraOperator())
                .addChecker(new CameraFacingChecker())
                .acrossBegin()
                .addOperator(new DngOperator())
                .addChecker(new DngIndicatorChecker())
                .addChecker(new PreviewChecker())
                .observeBegin(new DngSavedObserver())
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .acrossEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }

    @Test
    public void testDngCaptureWhenPauseAndResume() {
        if (!new DngOperator().isSupported()) {
            return;
        }
        new MetaCase()
                .addOperator(new SwitchCameraOperator())
                .addChecker(new CameraFacingChecker())
                .acrossBegin()
                .addOperator(new DngOperator())
                .addChecker(new DngIndicatorChecker())
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new CameraLaunchedChecker(), CameraLaunchedChecker.INDEX_NORMAL)
                .addChecker(new PreviewChecker())
                .observeBegin(new DngSavedObserver())
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .acrossEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }
}
