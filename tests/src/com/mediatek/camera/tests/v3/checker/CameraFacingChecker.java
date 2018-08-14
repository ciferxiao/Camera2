package com.mediatek.camera.tests.v3.checker;

import com.mediatek.camera.tests.v3.annotation.CoverPoint;
import com.mediatek.camera.tests.v3.annotation.NotCoverPoint;

@CoverPoint(pointList = {"Check the content description of camera_switcher icon"})
@NotCoverPoint(pointList = {"Not check preview content is really from front or back"})
public class CameraFacingChecker extends QuickSwitchChecker {
    public static final int INDEX_BACK = 0;
    public static final int INDEX_FRONT = 1;

    @Override
    protected String getSwitchIconResourceId() {
        return "com.mediatek.camera:id/camera_switcher";
    }

    @Override
    protected String getSwitchIconDescription(int index) {
        switch (index) {
            case INDEX_BACK:
                return "back";
            case INDEX_FRONT:
                return "front";
        }
        return null;
    }

    @Override
    protected int getSwitchIconStatusCount() {
        return 2;
    }

    @Override
    public String getDescription(int index) {
        switch (index) {
            case INDEX_BACK:
                return "Check camera is facing back";
            case INDEX_FRONT:
                return "Check camera is facing front";
        }
        return null;
    }
}
