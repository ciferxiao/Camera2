package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.tests.v3.annotation.group.CameraFeatureTest;
import com.mediatek.camera.tests.v3.annotation.module.AisTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.annotation.type.StabilityTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.AisExistedChecker;
import com.mediatek.camera.tests.v3.checker.CameraExitedChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.SettingItemExistedChecker;
import com.mediatek.camera.tests.v3.checker.SettingSwitchOnOffChecker;
import com.mediatek.camera.tests.v3.observer.AisObserver;
import com.mediatek.camera.tests.v3.operator.ExitCameraOperator;
import com.mediatek.camera.tests.v3.operator.HdrOperator;
import com.mediatek.camera.tests.v3.operator.LaunchCameraOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByBackKeyOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByHomeKeyOperator;
import com.mediatek.camera.tests.v3.operator.SettingSwitchButtonOperator;
import com.mediatek.camera.tests.v3.operator.SleepOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToNormalPhotoModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToNormalVideoModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToPanoramaModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToPipPhotoModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToPipVideoModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchToSlowMotionModeOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import org.junit.Test;

/**
 * AIS test case.
 */

@CameraFeatureTest
@AisTest
public class AisTestCase extends BaseCameraTestCase {
    private static final String AIS = "Anti-shake";
    private static final int SLEEP_TIME = 1;

    /**
     * Test AIS setting must enable in support mode.
     */
    @Test
    @FunctionTest
    public void testAisExistedOrNotInEveryMode() {
        new MetaCase("TC_Camera_AIS_0001")
                .addOperator(new SwitchToNormalPhotoModeOperator())
                .addChecker(new AisExistedChecker(false), SettingItemExistedChecker.INDEX_EXISTED)
                .addOperator(new SwitchToPanoramaModeOperator())
                .addChecker(new AisExistedChecker(false),
                        SettingItemExistedChecker.INDEX_NOT_EXISTED)
                .addOperator(new SwitchToNormalVideoModeOperator())
                .addChecker(new AisExistedChecker(false),
                        SettingItemExistedChecker.INDEX_NOT_EXISTED)
                .addOperator(new SwitchToPipPhotoModeOperator())
                .addChecker(new AisExistedChecker(false),
                        SettingItemExistedChecker.INDEX_NOT_EXISTED)
                .addOperator(new SwitchToPipVideoModeOperator())
                .addChecker(new AisExistedChecker(false),
                        SettingItemExistedChecker.INDEX_NOT_EXISTED)
                .addOperator(new SwitchToSlowMotionModeOperator())
                .addChecker(new AisExistedChecker(false),
                        SettingItemExistedChecker.INDEX_NOT_EXISTED)
                .run();
    }

    /**
     * Stress test for AIS switch on/off.
     */
    @Test
    @StabilityTest
    public void testSwitchAisStress() {
        new MetaCase("TC_Camera_AIS_0003")
                .observeBegin(new AisObserver())
                .addOperator(new SettingSwitchButtonOperator(AIS))
                .observeEnd()
                .addOperator(new SleepOperator(SLEEP_TIME))
                .runForTimes(Utils.STABILITY_REPEAT_TIMES);
    }

    /**
     * Test AIS can be shown in front camera normal mode.
     */
    @Test
    @FunctionTest
    public void testAisExistedInEveryCamera() {
        new MetaCase("TC_Camera_AIS_0004")
                .addOperator(new SwitchCameraOperator())
                .addChecker(new AisExistedChecker(false), SettingItemExistedChecker.INDEX_EXISTED)
                .run();
    }

    /**
     * Test pause/resume camera, AIS value can be remembered.
     */
    @Test
    @FunctionTest
    public void testRememberAisSettingAfterPauseResumeByHomeKey() {
        new MetaCase("TC_Camera_AIS_0005")
                .addOperator(new SettingSwitchButtonOperator(AIS),
                        SettingSwitchButtonOperator.INDEX_SWITCH_ON)
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new SettingSwitchOnOffChecker(AIS),
                        SettingSwitchOnOffChecker.INDEX_SWITCH_ON)
                .run();
    }

    /**
     * Test relaunch camera, AIS value can be remembered.
     */
    @Test
    @FunctionTest
    public void testRememberAisSettingAfterRelaunch() {
        new MetaCase("TC_Camera_AIS_0006")
                .addOperator(new SettingSwitchButtonOperator(AIS),
                        SettingSwitchButtonOperator.INDEX_SWITCH_ON)
                .addOperator(new PauseResumeByBackKeyOperator())
                .addChecker(new SettingSwitchOnOffChecker(AIS),
                        SettingSwitchOnOffChecker.INDEX_SWITCH_ON)
                .run();
    }

    /**
     * Test AIS value can be remembered in each camera device.
     */
    @Test
    @FunctionTest
    public void testRememberIsoSettingForEachCamera() {
        new MetaCase("TC_Camera_AIS_0007")
                .addOperator(new SwitchCameraOperator(), new int[]{SwitchCameraOperator
                        .INDEX_BACK, SwitchCameraOperator.INDEX_FRONT})
                .addOperator(new SettingSwitchButtonOperator(AIS),
                        SettingSwitchButtonOperator.INDEX_SWITCH_ON)
                .addOperator(new SwitchCameraOperator(), new int[]{SwitchCameraOperator
                        .INDEX_FRONT, SwitchCameraOperator.INDEX_BACK})
                .addChecker(new SettingSwitchOnOffChecker(AIS),
                        SettingSwitchOnOffChecker.INDEX_SWITCH_OFF)
                .addOperator(new SwitchCameraOperator(), new int[]{SwitchCameraOperator
                        .INDEX_BACK, SwitchCameraOperator.INDEX_FRONT})
                .addChecker(new SettingSwitchOnOffChecker(AIS),
                        SettingSwitchOnOffChecker.INDEX_SWITCH_ON)
                .addOperator(new SettingSwitchButtonOperator(AIS),
                        SettingSwitchButtonOperator.INDEX_SWITCH_OFF)
                .run();
    }

    /**
     * Test AIS default value.
     */
    @Test
    @FunctionTest
    public void testAisDefaultValue() {
        new MetaCase("TC_Camera_AIS_0008")
                .addOperator(new SwitchCameraOperator())
                .addChecker(new SettingSwitchOnOffChecker(AIS),
                        SettingSwitchOnOffChecker.INDEX_SWITCH_OFF)
                .run();
    }

    /**
     * Test supported status in 3rd app.
     */
    @Test
    @FunctionTest
    public void testSupportedStatusIn3rd() {
        new MetaCase("TC_Camera_AIS_0009")
                .addOperator(new ExitCameraOperator())
                .addChecker(new CameraExitedChecker())
                .addOperator(new LaunchCameraOperator(), LaunchCameraOperator.INDEX_INTENT_PHOTO)
                .addChecker(new PreviewChecker())
                .addChecker(new AisExistedChecker(true),
                        SettingItemExistedChecker.INDEX_NOT_EXISTED)
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
                new MetaCase("TC_Camera_AIS_0010")
                        .addChecker(new AisExistedChecker(false),
                                SettingItemExistedChecker.INDEX_EXISTED)
                        .run();
                break;
            case API2:
                new MetaCase("TC_Camera_AIS_0010")
                        .addChecker(new AisExistedChecker(false),
                                SettingItemExistedChecker.INDEX_EXISTED)
                        .run();
                break;
            default:
                break;
        }
    }

    /**
     * Test AIS disabled when hdr is on/auto.
     */
    @Test
    @FunctionTest
    public void testAisDisabledWhenHdrOnAuto() {
        new MetaCase("TC_Camera_AIS_0011")
                .addOperator(new HdrOperator(),
                        new int[]{HdrOperator.INDEX_ON, HdrOperator.INDEX_AUTO})
                .addChecker(new AisExistedChecker(true),
                        SettingItemExistedChecker.INDEX_NOT_EXISTED)
                .run();
    }
}
