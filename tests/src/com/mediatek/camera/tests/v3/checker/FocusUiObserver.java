package com.mediatek.camera.tests.v3.checker;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;

import com.mediatek.camera.tests.v3.annotation.CoverPoint;
import com.mediatek.camera.tests.v3.annotation.NotCoverPoint;
import com.mediatek.camera.tests.v3.observer.UiAppearedObserver;

/**
 * Checker used to check focus uI is shown and hidden normally.
 */
@CoverPoint(pointList = {"Check the focus UI is shown and hidden"})
@NotCoverPoint(pointList = {"Not check the position and orientation of the focus UI"})
public class FocusUiObserver extends UiAppearedObserver {

    private static final String RES_FOCUS_VIEW = "com.mediatek.camera:id/focus_view";
    private static final String DESCRIPTION_CAF = "continue focus";
    private static final String DESCRIPTION_TAF = "touch focus";

    public static final int INDEX_NO_AF_UI = 0;
    public static final int INDEX_HAS_CAF_UI = 1;
    public static final int INDEX_HAS_TAF_UI = 2;

    private static BySelector[] sUiSelectorList = {
            By.res(RES_FOCUS_VIEW), By.res(RES_FOCUS_VIEW), By.res(RES_FOCUS_VIEW)
    };

    private static String[] sUiDescriptionList = {"null", DESCRIPTION_CAF, DESCRIPTION_TAF};

    /**
     * Focus UI checker.
     */
    public FocusUiObserver() {
        super(sUiSelectorList, sUiDescriptionList);
    }
}
