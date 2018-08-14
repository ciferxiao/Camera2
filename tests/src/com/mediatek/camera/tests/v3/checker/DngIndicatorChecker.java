package com.mediatek.camera.tests.v3.checker;

import com.mediatek.camera.tests.v3.annotation.CoverPoint;

@CoverPoint(pointList = {"Check dng_indicator icon is shown or hidden"})
public class DngIndicatorChecker extends IndicatorChecker {
    @Override
    protected String getIndicatorIconResourceId() {
        return "com.mediatek.camera:id/dng_indicator";
    }
}
