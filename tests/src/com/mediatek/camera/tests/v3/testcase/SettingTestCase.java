package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.CameraFeatureTest;
import com.mediatek.camera.tests.v3.annotation.module.SettingTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.operator.ChangeAllSettingOneByOneOperator;
import com.mediatek.camera.tests.v3.operator.SwitchAllModeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.SwitchPhotoVideoOperator;

import org.junit.Test;

public class SettingTestCase extends BaseCameraTestCase {
    @Test
    @FunctionTest
    @CameraFeatureTest
    @SettingTest
    public void testChangeAllSettingOneByOne() {
        new MetaCase()
                .addOperator(new SwitchAllModeOperator(false))
                .acrossBegin()
                .addOperator(new SwitchPhotoVideoOperator())
                .acrossEnd()
                .acrossBegin()
                .addOperator(new SwitchCameraOperator(false))
                .acrossEnd()
                .addOperator(new ChangeAllSettingOneByOneOperator())
                .addOperator(new SwitchCameraOperator(false), SwitchCameraOperator.INDEX_BACK)
                .run();
    }
}
