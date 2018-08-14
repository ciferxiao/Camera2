package com.mediatek.camera.tests.v3.observer;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.tests.v3.util.Utils;

/**
 * Observer used to check focus state is right.
 */
public class ContinuousFocusStateObserver extends AbstractLogObserver {
    private static final LogUtil.Tag TAG = Utils.getTestTag(ContinuousFocusStateObserver.class
            .getSimpleName());
    private static final String LOG_TAG_FOCUS = "CamAp_Focus-0";
    private static final String LOG_TAG_AF_MGR = "af_mgr_v3";
    private int mExpectedIndex = 0;
    private boolean mIsContinuousFocusDone = false;
    private boolean mNeedWaitContinuousFocusDone = false;
    private boolean mIsContinuousPicMode = false;

    private static final String[] LOG_TAG_LIST_API1 = new String[]{
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS
    };
    private static final String[] LOG_KEY_LIST_API1 = new String[]{
            "ctl_afmode(4)",
            "EVENT_CMD_CHANGE_MODE",
            "EVENT_SEARCHING_START",
            "state = PASSIVE_SCAN",
            "EVENT_SEARCHING_END",
            "onFocusStateUpdate"
    };

    private static final String[] LOG_TAG_LIST_CAF_VIDEO_API1 = new String[]{
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS
    };
    private static final String[] LOG_KEY_LIST_CAF_VIDEO_API1 = new String[]{
            "ctl_afmode(3)",
            "EVENT_CMD_CHANGE_MODE",
            "EVENT_SEARCHING_START",
            "state = PASSIVE_SCAN",
            "EVENT_SEARCHING_END",
            "onFocusStateUpdate"
    };

    private static final String[] LOG_TAG_LIST_API2 = new String[]{
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS
    };
    private static final String[] LOG_KEY_LIST_API2 = new String[]{
            "ctl_afmode(3)",
            "EVENT_CMD_CHANGE_MODE",
            "state = PASSIVE_UNFOCUSED",
            "EVENT_SEARCHING_START",
            "state = PASSIVE_SCAN",
            "EVENT_SEARCHING_END",
            "state ="
    };

    private static final String[] LOG_TAG_LIST_CAF_VIDEO_API2 = new String[]{
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS,
            LOG_TAG_AF_MGR,
            LOG_TAG_FOCUS
    };
    private static final String[] LOG_KEY_LIST_CAF_VIDEO_API2 = new String[]{
            "ctl_afmode(4)",
            "EVENT_CMD_CHANGE_MODE",
            "EVENT_SEARCHING_START",
            "state = PASSIVE_SCAN",
            "EVENT_SEARCHING_END",
            "onFocusStateUpdate"
    };

    /**
     * Constructor of focus state observer.
     *
     * @param isContinuousPicMode Whether current focus mode is continuous-picture or
     *                            continuous-video.
     */
    public ContinuousFocusStateObserver(boolean isContinuousPicMode) {
        mIsContinuousPicMode = isContinuousPicMode;
    }

    @Override
    protected String[] getObservedTagList(int index) {
        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                if (mIsContinuousPicMode) {
                    return LOG_TAG_LIST_API1;
                } else {
                    return LOG_TAG_LIST_CAF_VIDEO_API1;
                }
            case API2:
                if (mIsContinuousPicMode) {
                    return LOG_TAG_LIST_API2;
                } else {
                    return LOG_TAG_LIST_CAF_VIDEO_API2;
                }
            default:
                return null;
        }
    }

    @Override
    protected String[] getObservedKeyList(int index) {
        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                if (mIsContinuousPicMode) {
                    return LOG_KEY_LIST_API2;
                } else {
                    return LOG_KEY_LIST_CAF_VIDEO_API1;
                }
            case API2:
                if (mIsContinuousPicMode) {
                    return LOG_KEY_LIST_API2;
                } else {
                    return LOG_KEY_LIST_CAF_VIDEO_API2;
                }
            default:
                return null;
        }
    }

    @Override
    protected boolean isAlreadyFindTarget(int index) {
        return mExpectedIndex == getObservedTagList(index).length;
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
            LogHelper.d(TAG, "onLogComing ,line " + line);
            if (line.contains(LOG_TAG_FOCUS) && line.contains("state = PASSIVE_SCAN")) {
                mNeedWaitContinuousFocusDone = true;
            }
            if (line.contains(LOG_TAG_FOCUS) && line.contains("onFocusStateUpdate") &&
                    isContinuousFocusDoneCallback(line) && mNeedWaitContinuousFocusDone) {
                mIsContinuousFocusDone = true;
            }
            mExpectedIndex++;
        }
    }

    @Override
    protected void onObserveBegin(int index) {
        mExpectedIndex = 0;
        mIsContinuousFocusDone = false;
        mNeedWaitContinuousFocusDone = false;
    }

    @Override
    protected void onObserveEnd(int index) {
        LogHelper.d(TAG, "onObserveEnd mNeedWaitContinuousFocusDone " +
                mNeedWaitContinuousFocusDone);
        if (mNeedWaitContinuousFocusDone) {
            Utils.assertRightNow(mExpectedIndex == getObservedTagList(index).length);
            Utils.assertRightNow(mIsContinuousFocusDone);
        }
    }

    @Override
    public String getDescription(int index) {
        StringBuilder sb = new StringBuilder("Observe this group tag has printed out as order, ");
        for (int i = 0; i < getObservedTagList(index).length; i++) {
            sb.append("\n");
            sb.append("[TAG-" + i + "] ");
            sb.append(getObservedTagList(index)[i]);
            sb.append(",[KEY-" + i + "] ");
            sb.append(getObservedKeyList(index)[i]);
            sb.append("\n");
        }
        return sb.toString();
    }

    private boolean isContinuousFocusDoneCallback(String line) {
        return line.contains("state = PASSIVE_FOCUSED") || line.contains("state = " +
                "PASSIVE_UNFOCUSED");
    }
}