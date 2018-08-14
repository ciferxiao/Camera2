package com.mediatek.camera.feature.setting;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.View;

import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ISettingManager.SettingDeviceRequester;
import com.mediatek.camera.common.setting.ISettingManager.SettingController;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Switch Camera setting item.
 *
 */
public class CameraSwitcher extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CameraSwitcher.class.getSimpleName());

    private static final String CAMERA_FACING_BACK = "back";
    private static final String CAMERA_FACING_FRONT = "front";
    private static final String CAMERA_DEFAULT_FACING = CAMERA_FACING_BACK;

    private static final String KEY_CAMERA_SWITCHER = "key_camera_switcher";
    private String mFacing;
    private View mSwitcherView;

    @Override
    public void init(IApp app,
                     ICameraContext cameraContext,
                     SettingController settingController) {
        super.init(app, cameraContext, settingController);
        mFacing = mDataStore.getValue(KEY_CAMERA_SWITCHER, CAMERA_DEFAULT_FACING, getStoreScope());

        int numOfCameras = Camera.getNumberOfCameras();
        if (numOfCameras > 1) {
            List<String> camerasFacing = getCamerasFacing(numOfCameras);
            if (camerasFacing.size() == 0) {
                return;
            }
            if (camerasFacing.size() == 1) {
                mFacing = camerasFacing.get(0);
                setValue(mFacing);
                return;
            }

            setSupportedPlatformValues(camerasFacing);
            setSupportedEntryValues(camerasFacing);
            setEntryValues(camerasFacing);

            mSwitcherView = initView();
            mAppUi.addToQuickSwitcher(mSwitcherView, 0);
        } else if (numOfCameras == 1) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(0, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                mFacing = CAMERA_FACING_BACK;
            } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                mFacing = CAMERA_FACING_FRONT;
            }
        }
        setValue(mFacing);
    }

    @Override
    public void unInit() {
        if (mSwitcherView != null) {
            mSwitcherView.setOnClickListener(null);
            mAppUi.removeFromQuickSwitcher(mSwitcherView);
        }
    }

    @Override
    public void postRestrictionAfterInitialized() {

    }

    @Override
    public void refreshViewEntry() {
        if (mSwitcherView != null) {
            if (getEntryValues().size() <= 1) {
                mSwitcherView.setVisibility(View.GONE);
            } else {
                mSwitcherView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public SettingType getSettingType() {
        return SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return KEY_CAMERA_SWITCHER;
    }

    @Override
    public IParametersConfigure getParametersConfigure() {
        return null;
    }

    @Override
    public ICaptureRequestConfigure getCaptureRequestConfigure() {
        return null;
    }

    @Override
    public String getStoreScope() {
        return mDataStore.getGlobalScope();
    }

    private List<String> getCamerasFacing(int numOfCameras) {
        List<String> camerasFacing = new ArrayList<>();
        for (int i = 0; i < numOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);

            String facing = null;
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                facing = CAMERA_FACING_BACK;
            } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                facing = CAMERA_FACING_FRONT;
            }

            if (!camerasFacing.contains(facing)) {
                camerasFacing.add(facing);
            }
        }
        return camerasFacing;
    }

    private View initView() {
        Activity activity = mApp.getActivity();
        View switcher = activity.getLayoutInflater().inflate(R.layout.camera_switcher, null);

        switcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nextFacing = mFacing.equals(CAMERA_FACING_BACK)
                        ? CAMERA_FACING_FRONT : CAMERA_FACING_BACK;
                LogHelper.d(TAG, "[onClick], switch camera to " + nextFacing);
                String newCameraId = mFacing.equals(CAMERA_FACING_BACK)
                        ? CameraUtil.getCamIdsByFacing(false).get(0)
                        : CameraUtil.getCamIdsByFacing(true).get(0);
                boolean success = mApp.notifyCameraSelected(newCameraId);
                if (success) {
                    LogHelper.d(TAG, "[onClick], switch camera success.");
                    mFacing = nextFacing;
                    mDataStore.setValue(KEY_CAMERA_SWITCHER, mFacing, getStoreScope(), true);
                }
                mSwitcherView.setContentDescription(mFacing);
            }
        });
        switcher.setContentDescription(mFacing);
        return switcher;
    }
}
