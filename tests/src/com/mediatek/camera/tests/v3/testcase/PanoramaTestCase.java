package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.tests.v3.annotation.group.CameraFeatureTest;
import com.mediatek.camera.tests.v3.annotation.module.PanoramaTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.CameraExitedChecker;
import com.mediatek.camera.tests.v3.checker.CameraLaunchedChecker;
import com.mediatek.camera.tests.v3.checker.CapturedPhotoLocationChecker;
import com.mediatek.camera.tests.v3.checker.IsoSettingValueChecker;
import com.mediatek.camera.tests.v3.checker.ModeExistedChecker;
import com.mediatek.camera.tests.v3.checker.NoVideoModeChecker;
import com.mediatek.camera.tests.v3.checker.PanoramaModeChecker;
import com.mediatek.camera.tests.v3.checker.PanoramaRestrictionChecker;
import com.mediatek.camera.tests.v3.checker.PanoramaUiChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.TouchFocusChecker;
import com.mediatek.camera.tests.v3.observer.MediaSavedObserver;
import com.mediatek.camera.tests.v3.operator.CancelPanoramaOperator;
import com.mediatek.camera.tests.v3.operator.CaptureOrRecordOperator;
import com.mediatek.camera.tests.v3.operator.ChangeIsoToValueOperator;
import com.mediatek.camera.tests.v3.operator.ConnectWifiOperator;
import com.mediatek.camera.tests.v3.operator.ExitCameraOperator;
import com.mediatek.camera.tests.v3.operator.LaunchCameraOperator;
import com.mediatek.camera.tests.v3.operator.LocationOperator;
import com.mediatek.camera.tests.v3.operator.LongPressShutterOperator;
import com.mediatek.camera.tests.v3.operator.OnSingleTapUpOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByHomeKeyOperator;
import com.mediatek.camera.tests.v3.operator.PermissionOperator;
import com.mediatek.camera.tests.v3.operator.PressShutterOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToNormalPhotoModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToPanoramaModeOperator;
import com.mediatek.camera.tests.v3.operator.VolumeKeyDownOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import org.junit.Test;

/**
 * Panorama test case.
 */

@PanoramaTest
@CameraFeatureTest
public class PanoramaTestCase extends BaseCameraTestCase {
    private static final LogUtil.Tag TAG = Utils.getTestTag(PanoramaTestCase.class
            .getSimpleName());

    /**
     * Test can cancel when capturing panorama picture.
     */
    @Test
    @FunctionTest
    public void testCancelCapturing() {
        new MetaCase("TC_Camera_Auto Panorama_0003")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new PressShutterOperator())
                .addChecker(new PanoramaUiChecker())
                .addOperator(new CancelPanoramaOperator(), CancelPanoramaOperator.CANCEL_INDEX)
                .addChecker(new PreviewChecker())
                .run();
    }

    /**
     * Test switch to normal mode from panorama mode after EV changed.
     */
    @Test
    @FunctionTest
    public void testSwitchToNormalAfterChangeEv() {
        new MetaCase("TC_Camera_Auto Panorama_0006")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                //TODO: should add change EV operator.
                .addOperator(new SwitchToNormalPhotoModeOperator())
                .addChecker(new PreviewChecker())
                .run();
    }

    /**
     * Test ISO setting item in panorama mode.
     */
    @Test
    @FunctionTest
    public void testIsoSettingItem() {
        new MetaCase("TC_Camera_Auto Panorama_0007")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new ChangeIsoToValueOperator("1600"))
                .addChecker(new IsoSettingValueChecker("1600"))
                .addOperator(new SwitchToNormalPhotoModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addChecker(new IsoSettingValueChecker("1600"))
                .run();
    }

    /**
     * Test press volume key during saving panorama picture.
     */
    @Test
    @FunctionTest
    public void testVolumeKeyWhenCapturing() {
        new MetaCase("TC_Camera_Auto Panorama_0008")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new PressShutterOperator())
                .addOperator(new VolumeKeyDownOperator())
                .addChecker(new PanoramaUiChecker())
                .run();
    }

    /**
     * Test panorama should only be always in main sensor.
     */
    @Test
    @FunctionTest
    public void testOnlySupportInMainSensor() {
        new MetaCase("TC_Camera_Auto Panorama_0011")
                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_FRONT)
                .addChecker(new ModeExistedChecker("Panorama"),
                        ModeExistedChecker.INDEX_NOT_EXISTED)
                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_BACK)
                .addChecker(new ModeExistedChecker("Panorama"),
                        ModeExistedChecker.INDEX_EXISTED)
                .run();
    }

    /**
     * Test there is no video entry in panorama mode.
     */
    @Test
    @FunctionTest
    public void testNoVideoEntry() {
        new MetaCase("TC_Camera_Auto Panorama_0012")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addChecker(new NoVideoModeChecker())
                .run();
    }

    /**
     * Test pause/resume in panorama.
     */
    @Test
    @FunctionTest
    public void testPauseResume() {
        new MetaCase("TC_Camera_Auto Panorama_0013")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PanoramaModeChecker(), PanoramaModeChecker.PANORAMA_MODE)
                .addChecker(new PreviewChecker())
                .run();
    }

    /**
     * Test location in panorama.
     */
    @Test
    @FunctionTest
    public void testLocation() {
        new MetaCase("TC_Camera_Auto Panorama_0028")
                .addOperator(new PermissionOperator(), PermissionOperator.INDEX_ENABLE_ALL)
                .addOperator(new ConnectWifiOperator(), ConnectWifiOperator.INDEX_CONNECT)
                .run();

        new MetaCase("TC_Camera_Auto Panorama_0028")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .acrossBegin()
                .addOperator(new LocationOperator())
                .observeBegin(new MediaSavedObserver(), MediaSavedObserver.INDEX_ONE_SAVED)
                .addOperator(new CaptureOrRecordOperator())
                .observeEnd()
                .addChecker(new CapturedPhotoLocationChecker(), new int[]{
                        CapturedPhotoLocationChecker.INDEX_NO_LOCATION,
                        CapturedPhotoLocationChecker.INDEX_HAS_LOCATION,
                        CapturedPhotoLocationChecker.INDEX_HAS_LOCATION,
                        CapturedPhotoLocationChecker.INDEX_HAS_LOCATION
                })
                .acrossEnd()
                .run();
    }

    /**
     * Test supported status on API1 & API2.
     */
    @Test
    @FunctionTest
    public void testSupportedStatusOnAPI1API2() {
        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                new MetaCase("TC_Camera_Auto Panorama_0030")
                        .addChecker(new ModeExistedChecker("Panorama"),
                                ModeExistedChecker.INDEX_EXISTED)
                        .run();
                break;
            case API2:
                new MetaCase("TC_Camera_Auto Panorama_0030")
                        .addChecker(new ModeExistedChecker("Panorama"),
                                ModeExistedChecker.INDEX_NOT_EXISTED)
                        .run();
                break;
        }
    }

    /**
     * Test touch focus in panorama.
     */
    @Test
    @FunctionTest
    public void testTouchFocus() {
        new MetaCase("TC_Camera_Auto Panorama_0032")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new OnSingleTapUpOperator())
                .addChecker(new TouchFocusChecker())
                .addOperator(new PressShutterOperator())
                //TODO: should check no touch focus ui here.
                .addChecker(new PanoramaUiChecker())
                .run();
    }

    /**
     * Test supported status in 3rd app.
     */
    @Test
    @FunctionTest
    public void testSupportedStatusIn3rd() {
        new MetaCase("TC_Camera_Auto Panorama_0033")
                .addOperator(new ExitCameraOperator())
                .addChecker(new CameraExitedChecker())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_INTENT_PHOTO)
                .addChecker(new PreviewChecker())
                .addChecker(new PanoramaModeChecker(), PanoramaModeChecker.NOT_PANORAMA_MODE)
                .run();
    }

    /**
     * Test panorama restriction to other features.
     */
    @Test
    @FunctionTest
    public void testRestriction() {
        new MetaCase("TC_Camera_Auto Panorama_0034")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addChecker(new PanoramaRestrictionChecker())
                .addOperator(new LongPressShutterOperator())
                .run();
    }

    /**
     * Test pause/resume during capturing panorama picture.
     */
    @Test
    @FunctionTest
    public void testPauseResumeWhenCapturing() {
        new MetaCase("TC_Camera_Auto Panorama_0045")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new PressShutterOperator())
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PanoramaModeChecker(), PanoramaModeChecker.PANORAMA_MODE)
                .addChecker(new PreviewChecker())
                .run();
    }

    /**
     * Test exit camera during capturing panorama picture.
     */
    @Test
    @FunctionTest
    public void testExitWhenCapturing() {
        new MetaCase("TC_Camera_Auto Panorama_0046")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new PressShutterOperator())
                .addOperator(new ExitCameraOperator())
                .addChecker(new CameraExitedChecker())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_NORMAL)
                .addChecker(new CameraLaunchedChecker(), CameraLaunchedChecker.INDEX_NORMAL)
                .run();
    }

    /**
     * Test click cancel button and then click save button quickly when capturing panorama.
     */
    @Test
    @FunctionTest
    public void testClickCancelSaveButtonQuickly() {
        new MetaCase("TC_Camera_Auto Panorama_0050")
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new PressShutterOperator())
                .addChecker(new PanoramaUiChecker())
                .addOperator(new CancelPanoramaOperator(),
                        CancelPanoramaOperator.CANCEL_THEN_SAVE_QUICKLY_INDEX)
                .addChecker(new PreviewChecker())
                .run();
    }
}
