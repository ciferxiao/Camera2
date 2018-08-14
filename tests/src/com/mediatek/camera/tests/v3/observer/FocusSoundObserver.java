package com.mediatek.camera.tests.v3.observer;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.tests.v3.util.Utils;

/**
 * Observer used to check focus sound is right.
 */
public class FocusSoundObserver extends AbstractLogObserver {
    private static final LogUtil.Tag TAG = Utils.getTestTag(FocusSoundObserver.class
            .getSimpleName());
    private static final String LOG_TAG_FOCUS = "CamAp_Focus-0";
    private int mExpectedIndex = 0;
    private boolean mPlayFocusSound = false;

    public static final int INDEX_NO_FOCUS_SOUND = 0;
    public static final int INDEX_HAS_FOCUS_SOUND = 1;

    @Override
    protected String[] getObservedTagList(int index) {
        return new String[]{LOG_TAG_FOCUS};
    }

    @Override
    protected String[] getObservedKeyList(int index) {
        return new String[]{"onFocusStateUpdate"};
    }

    @Override
    protected boolean isAlreadyFindTarget(int index) {
        return true;
    }

    @Override
    public int getObserveCount() {
        return 1;
    }


    @Override
    protected void onLogComing(int index, String line) {
        if (line.contains(getObservedTagList(index)[mExpectedIndex])
                && line.contains(getObservedKeyList(index)[mExpectedIndex])) {
            LogHelper.d(TAG, "onLogComing + ,line " + line);
            if (isTouchFocusDoneCallback(line) && needPlayFocusSound(line)) {
                mPlayFocusSound = true;
            }
            mExpectedIndex++;
        }
    }

    @Override
    protected void onObserveEnd(int index) {
        Utils.assertRightNow(mExpectedIndex == getObservedTagList(index).length);
        if (index == INDEX_NO_FOCUS_SOUND) {
            Utils.assertRightNow(!mPlayFocusSound);
        } else {
            Utils.assertRightNow(mPlayFocusSound);
        }
    }

    @Override
    protected void onObserveBegin(int index) {
        mExpectedIndex = 0;
        mPlayFocusSound = false;
    }

    @Override
    public String getDescription(int index) {
        StringBuilder sb = new StringBuilder("Observe this group tag has printed out as order, ");
        for (int i = 0; i < getObservedTagList(index).length; i++) {
            sb.append("[TAG-" + i + "] ");
            sb.append(getObservedTagList(index)[i]);
            sb.append(",[KEY-" + i + "] ");
            sb.append(getObservedKeyList(index)[i]);
            sb.append("\n");
        }
        return sb.toString();
    }

    private boolean isTouchFocusDoneCallback(String line) {
        return line.contains("state = ACTIVE_FOCUSED") || line.contains("state = ACTIVE_UNFOCUSED");
    }

    private boolean needPlayFocusSound(String line) {
        return line.contains("mNeedPlayFocusSound true") &&
                line.contains("mNeedTriggerShutterButton false");
    }
}
