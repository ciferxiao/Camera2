package com.mediatek.camera.tests.v3.observer;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.tests.v3.util.Utils;

/**
 * Observer used to check focus state is right.
 */
public class FocusStateObserver extends AbstractLogObserver {
    private static final LogUtil.Tag TAG = Utils.getTestTag(FocusStateObserver.class
            .getSimpleName());
    private static final String LOG_TAG_FOCUS = "CamAp_Focus-0";
    private static final String LOG_TAG_HANDLER = "CamAp_API1-Handler-0";
    private static final String LOG_TAG_AF_MGR = "af_mgr_v3";
    private static final String LOG_TAG_FRAMEWORK = "CameraFramework";
    private int mExpectedIndex = 0;
    private boolean mIsTouchFocusDone = false;

    private static final String[] LOG_TAG_LIST_API1 = new String[]{
            LOG_TAG_FOCUS,
            LOG_TAG_FOCUS,
            LOG_TAG_HANDLER,
            LOG_TAG_HANDLER,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_FRAMEWORK,
            LOG_TAG_FOCUS
    };
    private static final String[] LOG_KEY_LIST_API1 = new String[]{
            "[onSingleTapUp] +",
            "[onSingleTapUp]-",
            "[autofocus]+",
            "[autofocus]-",
            "ctl_afmode(1)",
            "EVENT_CMD_CHANGE_MODE",
            "EVENT_CMD_SET_AF_REGION",
            "EVENT_CMD_AUTOFOCUS",
            "handleMessage: 4",
            "onFocusStateUpdate"
    };

    private static final String[] LOG_TAG_LIST_API2 = new String[]{
            LOG_TAG_FOCUS,
            LOG_TAG_FOCUS,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS,
            LOG_TAG_FOCUS
    };
    private static final String[] LOG_KEY_LIST_API2 = new String[]{
            "[onSingleTapUp] +",
            "[onSingleTapUp]-",
            "ctl_afmode(1)",
            "EVENT_CMD_CHANGE_MODE",
            "EVENT_CMD_SET_AF_REGION",
            "EVENT_CMD_AUTOFOCUS",
            "onFocusStateUpdate",
            "state ="
    };

    @Override
    protected String[] getObservedTagList(int index) {
        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                return LOG_TAG_LIST_API1;
            case API2:
                return LOG_TAG_LIST_API2;
            default:
                return null;
        }
    }

    @Override
    protected String[] getObservedKeyList(int index) {
        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                return LOG_KEY_LIST_API1;
            case API2:
                return LOG_KEY_LIST_API2;
            default:
                return null;
        }
    }

    @Override
    protected boolean isAlreadyFindTarget(int index) {
        return mExpectedIndex == getObservedTagList(index).length && mIsTouchFocusDone;
    }

    @Override
    public int getObserveCount() {
        return 1;
    }


    @Override
    protected void onLogComing(int index, String line) {
        if (mExpectedIndex >= getObservedTagList(index).length) {
            return;
        }
        if (line.contains(getObservedTagList(index)[mExpectedIndex])
                && line.contains(getObservedKeyList(index)[mExpectedIndex])) {
            LogHelper.d(TAG, "onLogComing + ,line " + line);
            if (line.contains(LOG_TAG_FOCUS) && line.contains("onFocusStateUpdate") &&
                    isTouchFocusDoneCallback(line)) {
                mIsTouchFocusDone = true;
            }
            mExpectedIndex++;
        }
    }

    @Override
    protected void onObserveEnd(int index) {
        Utils.assertRightNow(mExpectedIndex == getObservedTagList(index).length);
        Utils.assertRightNow(mIsTouchFocusDone);
    }

    @Override
    protected void onObserveBegin(int index) {
        mExpectedIndex = 0;
        mIsTouchFocusDone = false;
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
}
