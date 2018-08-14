package com.mediatek.camera.tests.v3.operator;

import android.graphics.Rect;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiObject2;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.arch.Operator;
import com.mediatek.camera.tests.v3.arch.Page;
import com.mediatek.camera.tests.v3.util.Utils;

/**
 * Matrix display effect select operator.
 */

public class MatrixDisplayEffectSelectOperator extends Operator {
    private static final LogUtil.Tag TAG = Utils.getTestTag(
            MatrixDisplayEffectSelectOperator.class.getSimpleName());

    @Override
    public void operate(int index) {
        super.operate(index);
    }

    @Override
    public int getOperatorCount() {
        return 15;
    }

    @Override
    public Page getPageBeforeOperate(int index) {
        return Page.PREVIEW;
    }

    @Override
    public Page getPageAfterOperate(int index) {
        return null;
    }

    @Override
    public String getDescription(int index) {
        return "select " + " effect";
    }

    @Override
    protected void doOperate(int index) {
        /*Utils.assertObject(By.res("com.mediatek.camera:id/effect"));
        UiObject2 matrixDisplayIcon = Utils.findObject(
                By.res("com.mediatek.camera:id/effect").clickable(true));
        matrixDisplayIcon.click();*/

        Utils.assertObject(By.res("com.mediatek.camera:id/lomo_effect_layout"));
        UiObject2 matrixDisplayLayout = Utils.findObject(
                By.res("com.mediatek.camera:id/lomo_effect_layout"));
        Rect rect = matrixDisplayLayout.getVisibleBounds();

        int layoutWidth = rect.right - rect.left;
        int layoutHeight = rect.bottom - rect.top;
        int columnWidth = layoutWidth / 3;
        int rowWidth = layoutHeight / 3;
        // click centre point of grid.
        int positionX = (columnWidth * (3 - index / 3) + columnWidth * (3 - index / 3 - 1)) / 2;
        int positionY = (rowWidth * (index % 3) + rowWidth * (index % 3 + 1)) / 2;
        LogHelper.d(TAG, "[doOperate], click position(" + positionX + ", " + positionY + ")");

        if (index == 9 || index == 12) {
            matrixDisplayLayout.scroll(Direction.LEFT,
                    (float) columnWidth / (float) layoutWidth);
        }
        if (positionX < 0) {
            positionX = columnWidth / 2;
            LogHelper.d(TAG, "[doOperate], recalculate click position("
                    + positionX + ", " + positionY + ")");
        }

        Utils.getUiDevice().click(positionX, positionY);
    }
}
