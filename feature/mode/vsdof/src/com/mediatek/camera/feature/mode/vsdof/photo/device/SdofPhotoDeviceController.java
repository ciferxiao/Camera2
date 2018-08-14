/*
 *   Copyright Statement:
 *
 *     This software/firmware and related documentation ("MediaTek Software") are
 *     protected under relevant copyright laws. The information contained herein is
 *     confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 *     the prior written permission of MediaTek inc. and/or its licensors, any
 *     reproduction, modification, use or disclosure of MediaTek Software, and
 *     information contained herein, in whole or in part, shall be strictly
 *     prohibited.
 *
 *     MediaTek Inc. (C) 2016. All rights reserved.
 *
 *     BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *    THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 *     RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 *     ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 *     WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 *     WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 *     NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 *     RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 *     TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 *     RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 *     OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 *     SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 *     RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 *     STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 *     ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 *     RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 *     MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 *     CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     The following software/firmware and/or related documentation ("MediaTek
 *     Software") have been modified by MediaTek Inc. All revisions are subject to
 *     any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.feature.mode.vsdof.photo.device;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;

import com.google.common.base.Preconditions;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory.CameraApi;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.ISettingManager.SettingDeviceRequester;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.vsdof.SdofUtil;
import com.mediatek.camera.feature.mode.vsdof.photo.DeviceInfo;
import com.mediatek.camera.portability.CameraEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Photo device controller.
 */
public class SdofPhotoDeviceController implements ISdofPhotoDeviceController,
        SettingDeviceRequester {
    private static final Tag TAG = new Tag(SdofPhotoDeviceController.class.getSimpleName());
    private static final Size POST_VIEW_SIZE_DEFAULT = new Size(144, 256);
    private volatile CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private static final String PROPERTY_KEY_CLIENT_APP_MODE = "client.appmode";
    private static final String APP_MODE_NAME_MTK_DUAL_CAMERA = "MtkStereo";
    private String mLevel;

    private final Activity mActivity;
    private final CameraDeviceManager mCameraDeviceManager;
    private ICameraContext mICameraContext;
    private List<String> mSupportedSizes;
    private Size mPostViewSize;
    private HashMap<String, String> mPictureSizeMap = new HashMap<>();

    //for post view update thumbnail
    private int mPostViewCallbackNumber = 0;

    /**
     * this enum is used for tag native camera open state.
     */
    private enum CameraState {
        CAMERA_UNKNOWN, //initialize state.
        CAMERA_OPENING, //between open camera and open done callback.
        CAMERA_OPENED, //when camera open done.
    }

    private Handler mRequestHandler;

    /**
     * Controller camera device.
     *
     * @param activity current activity.
     * @param context  current camera context.
     */
    SdofPhotoDeviceController(@Nonnull Activity activity, @Nonnull ICameraContext context) {
        HandlerThread handlerThread = new HandlerThread("DeviceController");
        handlerThread.start();
        mRequestHandler = new PhotoDeviceHandler(handlerThread.getLooper(), this);
        mICameraContext = context;
        mActivity = activity;
        mCameraDeviceManager = mICameraContext.getDeviceManager(CameraApi.API1);
    }

    @Override
    public void openCamera(DeviceInfo info) {
        boolean sync = info.getNeedOpenCameraSync();
        mRequestHandler.obtainMessage(PhotoDeviceAction.OPEN_CAMERA, info).sendToTarget();
        if (sync) {
            waitDone();
        }
    }

    @Override
    public void updatePreviewSurface(SurfaceHolder surfaceHolder) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.UPDATE_PREVIEW_SURFACE, surfaceHolder)
                .sendToTarget();
    }

    @Override
    public void setDeviceCallback(DeviceCallback callback) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.SET_PREVIEW_CALLBACK, callback)
                .sendToTarget();
    }

    @Override
    public void setStereoWarningCallback(StereoWarningCallback callback) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.SET_STEREO_WARNING_CALLBACK, callback)
                .sendToTarget();
    }

    @Override
    public void startPreview() {
        mRequestHandler.sendEmptyMessage(PhotoDeviceAction.START_PREVIEW);
    }

    @Override
    public void stopPreview() {
        mRequestHandler.sendEmptyMessage(PhotoDeviceAction.STOP_PREVIEW);
        waitDone();
    }

    @Override
    public void takePicture(@Nonnull CaptureDataCallback callback) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.TAKE_PICTURE, callback).sendToTarget();
    }

    @Override
    public void updateGSensorOrientation(int orientation) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.UPDATE_G_SENSOR_ORIENTATION, orientation)
                .sendToTarget();
    }

    @Override
    public void closeCamera(boolean sync) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.CLOSE_CAMERA, sync == true ? 1 : 0)
                .sendToTarget();
        waitDone();
    }

    @Override
    public Size getPreviewSize(double targetRatio) {
        double[] values = new double[3];
        values[0] = targetRatio;
        mRequestHandler.obtainMessage(PhotoDeviceAction.GET_PREVIEW_SIZE, values).sendToTarget();
        waitDone();
        return new Size((int) values[1], (int) values[2]);
    }

    @Override
    public Size getPostViewSize() {
        if (mPostViewSize == null) {
            return POST_VIEW_SIZE_DEFAULT;
        }
        return mPostViewSize;
    }

    @Override
    public void setPreviewSizeReadyCallback(PreviewSizeCallback callback) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.SET_PREVIEW_SIZE_READY_CALLBACK,
                callback).sendToTarget();
    }

    @Override
    public void setPictureSize(Size size) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.SET_PICTURE_SIZE, size).sendToTarget();
    }

    @Override
    public void setVsDofLevelParameter(String level) {
        mRequestHandler.removeMessages(PhotoDeviceAction.SET_VSDOF_LEVEL_PARAMETER);
        mRequestHandler.obtainMessage(PhotoDeviceAction.SET_VSDOF_LEVEL_PARAMETER,
                level).sendToTarget();
    }

    @Override
    public String getStereoPictureSize() {
        return null;
    }

    @Override
    public boolean isReadyForCapture() {
        boolean[] isReady = new boolean[1];
        mRequestHandler.obtainMessage(PhotoDeviceAction.IS_READY_FOR_CAPTURE, isReady)
                .sendToTarget();
        waitDone();
        return isReady[0];
    }

    @Override
    public void requestChangeSettingValue(String key) {
        mRequestHandler.removeMessages(PhotoDeviceAction.REQUEST_CHANGE_SETTING_VALUE);
        mRequestHandler.obtainMessage(PhotoDeviceAction.REQUEST_CHANGE_SETTING_VALUE, key)
                .sendToTarget();
    }

    @Override
    public void requestChangeCommand(String key) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.REQUEST_CHANGE_COMMAND, key).sendToTarget();
    }

    @Override
    public void requestChangeCommandImmediately(String key) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.REQUEST_CHANGE_COMMAND_IMMEDIATELY, key)
                .sendToTarget();
    }

    @Override
    public void destroyDeviceController() {
        mRequestHandler.sendEmptyMessage(PhotoDeviceAction.DESTROY_DEVICE_CONTROLLER);
    }

    @Override
    public void requestChangeSettingValueJustSelf(String key) {

    }

    private void waitDone() {
        final Object waitDoneLock = new Object();
        final Runnable unlockRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (waitDoneLock) {
                    waitDoneLock.notifyAll();
                }
            }
        };

        synchronized (waitDoneLock) {
            mRequestHandler.post(unlockRunnable);
            try {
                waitDoneLock.wait();
            } catch (InterruptedException ex) {
                LogHelper.e(TAG, "waitDone interrupted");
            }
        }
    }

    /**
     * Use for handler device control command.
     */
    private class PhotoDeviceHandler extends Handler {
        private static final String KEY_DISP_ROT_SUPPORTED = "disp-rot-supported";
        private static final String KEY_STEREO_PICTURE_SIZE = "key_stereo_picture_size";
        private static final String FALSE = "false";
        private static final int INTERRUPT_TIME_OUT = 5000;
        private String mCameraId;
        private ISettingManager mSettingManager;
        private ISettingManager.SettingDeviceConfigurator mSettingDeviceConfigurator;
        private Object mWaitCameraOpenDone = new Object();

        private final CameraProxy.StateCallback mCameraProxyStateCallback =
                new CameraDeviceProxyStateCallback();
        private CameraProxy mCameraProxy;
        private Object mCaptureSync = new Object();
        private boolean mIsInCapturing = false;
        private boolean mIsPreviewStarted = false;
        private DeviceCallback mModeDeviceCallback;
        private int mPreviewFormat;
        private long mCaptureStartTime = 0;
        private CaptureDataCallback mCaptureDataCallback;
        private StereoWarningCallback mWarningCallback;
        private int mJpegRotation = 0;
        private volatile int mPreviewWidth;
        private volatile int mPreviewHeight;
        private Size mCurPictureSize;
        private PreviewSizeCallback mCameraOpenedCallback;
        private SurfaceHolder mSurfaceHolder;
        private SettingDeviceRequester mSettingDeviceRequester;
        private boolean mNeedSubSectionInitSetting = false;
        private boolean mNeedQuitHandler = false;

        /**
         * Photo device handler.
         *
         * @param looper current looper.
         */
        public PhotoDeviceHandler(Looper looper, SettingDeviceRequester requester) {
            super(looper);
            mSettingDeviceRequester = requester;
        }

        @Override
        public void handleMessage(Message msg) {
            LogHelper.i(TAG, "[handleMessage] msg = " + PhotoDeviceAction.stringify(msg.what));
            super.handleMessage(msg);
            if (cancelDealMessage(msg.what)) {
                LogHelper.d(TAG, "[handleMessage] - msg = " +
                        PhotoDeviceAction.stringify(msg.what) + "[dismiss]");
                return;
            }
            switch (msg.what) {
                case PhotoDeviceAction.OPEN_CAMERA:
                    doOpenCamera((DeviceInfo) msg.obj);
                    break;
                case PhotoDeviceAction.CLOSE_CAMERA:
                    doCloseCamera((Integer) msg.obj == 1 ? true : false);
                    break;
                case PhotoDeviceAction.START_PREVIEW:
                    doStartPreview();
                    break;
                case PhotoDeviceAction.STOP_PREVIEW:
                    doStopPreview();
                    break;
                case PhotoDeviceAction.TAKE_PICTURE:
                    doTakePicture((CaptureDataCallback) msg.obj);
                    break;
                case PhotoDeviceAction.REQUEST_CHANGE_SETTING_VALUE:
                    String key = (String) msg.obj;
                    if (mCameraProxy == null || mCameraState == CameraState.CAMERA_UNKNOWN) {
                        LogHelper.e(TAG, "camera is closed or in opening state, can't request " +
                                "change setting value,key = " + key);
                        return;
                    }
                    doRequestChangeSettingValue(key);
                    break;
                case PhotoDeviceAction.REQUEST_CHANGE_COMMAND:
                    doRequestChangeCommand((String) msg.obj);
                    break;
                case PhotoDeviceAction.REQUEST_CHANGE_COMMAND_IMMEDIATELY:
                    doRequestChangeCommandImmediately((String) msg.obj);
                    break;
                case PhotoDeviceAction.SET_PICTURE_SIZE:
                    doSetPictureSize((Size) msg.obj);
                    break;
                case PhotoDeviceAction.SET_VSDOF_LEVEL_PARAMETER:
                    setDofLevelParameter((String) msg.obj);
                    break;
                case PhotoDeviceAction.SET_PREVIEW_CALLBACK:
                    mModeDeviceCallback = (DeviceCallback) msg.obj;
                    break;
                case PhotoDeviceAction.SET_STEREO_WARNING_CALLBACK:
                    mWarningCallback = (StereoWarningCallback) msg.obj;
                    break;
                case PhotoDeviceAction.SET_PREVIEW_SIZE_READY_CALLBACK:
                    mCameraOpenedCallback = (PreviewSizeCallback) msg.obj;
                    break;
                case PhotoDeviceAction.GET_PREVIEW_SIZE:
                    doGetPreviewSize(msg);
                    break;
                case PhotoDeviceAction.UPDATE_PREVIEW_SURFACE:
                    doUpdatePreviewSurface((SurfaceHolder) msg.obj);
                    break;
                case PhotoDeviceAction.UPDATE_G_SENSOR_ORIENTATION:
                    mJpegRotation = (Integer) msg.obj;
                    break;
                case PhotoDeviceAction.IS_READY_FOR_CAPTURE:
                    boolean[] isReady = (boolean[]) msg.obj;
                    isReady[0] = isReadyForCapture();
                    break;
                case PhotoDeviceAction.DESTROY_DEVICE_CONTROLLER:
                    doDestroyHandler();
                    break;

                case PhotoDeviceAction.ON_CAMERA_OPENED:
                    doOnOpened((CameraProxy) msg.obj);
                    break;
                case PhotoDeviceAction.ON_CAMERA_DISCONNECTED:
                    doOnDisconnected();
                    break;
                case PhotoDeviceAction.ON_CAMERA_ERROR:
                    doOnError(msg.arg1);
                    break;
                default:
                    LogHelper.e(TAG, "[handleMessage] the message don't defined in " +
                            "photodeviceaction, need check");
                    break;
            }
        }

        private void doOpenCamera(DeviceInfo info) {
            String cameraId = info.getCameraId();
            boolean sync = info.getNeedOpenCameraSync();
            LogHelper.i(TAG, "[doOpenCamera] id: " + cameraId + ", sync = " + sync + ",camera " +
                    "state" + " : " + mCameraState);
            Preconditions.checkNotNull(cameraId);
            if (!canDoOpenCamera(cameraId)) {
                LogHelper.i(TAG, "[doOpenCamera], condition is not ready, return");
                return;
            }
            // Do open camera action.
            mCameraId = cameraId;
            mNeedSubSectionInitSetting = info.getNeedFastStartPreview();
            mCameraState = CameraState.CAMERA_OPENING;
            mSettingManager = info.getSettingManager();
            mSettingManager.updateModeDeviceRequester(mSettingDeviceRequester);
            mSettingDeviceConfigurator = mSettingManager.getSettingDeviceConfigurator();
            try {
                //when open the camera need reset the mCameraProxy to null
                if (sync) {
                    mCameraDeviceManager.openCameraSync(mCameraId, mCameraProxyStateCallback, null);
                } else {
                    mCameraDeviceManager.openCamera(mCameraId, mCameraProxyStateCallback, null);
                }
            } catch (CameraOpenException e) {
                //need show error and finish the activity.
                if (CameraOpenException.ExceptionType.SECURITY_EXCEPTION == e.getExceptionType()) {
                    CameraUtil.showErrorInfoAndFinish(mActivity, CameraUtil
                            .CAMERA_HARDWARE_EXCEPTION);
                }
            }
        }

        private void doCloseCamera(boolean isSwitchCamera) {
            LogHelper.i(TAG, "[doCloseCamera] isSwitchCamera = " + isSwitchCamera + ",state = " +
                    mCameraState + ",camera proxy = " + mCameraProxy);
            if (CameraState.CAMERA_UNKNOWN == mCameraState) {
                return;
            }
            try {
                if (CameraState.CAMERA_OPENING == mCameraState) {
                    synchronized (mWaitCameraOpenDone) {
                        if (!hasDeviceStateCallback()) {
                            mWaitCameraOpenDone.wait();
                        }
                    }
                }
                mCameraState = CameraState.CAMERA_UNKNOWN;
                LogHelper.d(TAG, "[doCloseCamera] begin do close camera");
                checkIsCapturing();
                //Must recycle the camera, otherwise when next time open the camera,
                //will not do open action because camera device proxy use the old one.
                mCameraDeviceManager.recycle(mCameraId);
                if (mModeDeviceCallback != null) {
                    mModeDeviceCallback.beforeCloseCamera();
                }
                if (mCameraProxy != null) {
                    if (isSwitchCamera) {
                        mCameraProxy.close();
                    } else {
                        mCameraProxy.closeAsync();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mCameraId = null;
                mCameraProxy = null;
                mIsPreviewStarted = false;
                mSurfaceHolder = null;
                mPostViewCallbackNumber = 0;
                mIsInCapturing = false;
                if (mNeedQuitHandler) {
                    mRequestHandler.sendEmptyMessage(PhotoDeviceAction.DESTROY_DEVICE_CONTROLLER);
                }
            }
        }

        private void doStartPreview() {
            if (isCameraAvailable()) {
                //set preview callback before start preview.
                mCameraProxy.setOneShotPreviewCallback(mFrameworkPreviewCallback);
                // Start preview.
                mCameraProxy.startPreview();
                mCameraProxy.setVendorDataCallback(MTK_CAMERA_MSG_EXT_NOTIFY_STEREO_WARNING,
                        mStereoWarningCallback);
                mCameraProxy.setVendorDataCallback(MTK_CAMERA_MSG_EXT_NOTIFY_IMAGE_UNCOMPRESSED,
                        mUncompressedImageCallback);
            }
        }

        private void doStopPreview() {
            checkIsCapturing();
            if (isCameraAvailable()) {
                mSettingDeviceConfigurator.onPreviewStopped();
                if (mModeDeviceCallback != null) {
                    mModeDeviceCallback.afterStopPreview();
                }
                mIsPreviewStarted = false;
                mCameraProxy.stopPreviewAsync();
            }
            if (mNeedQuitHandler) {
                mRequestHandler.sendEmptyMessage(PhotoDeviceAction.DESTROY_DEVICE_CONTROLLER);
            }
            mPostViewCallbackNumber = 0;
        }

        private void doTakePicture(CaptureDataCallback callback) {
            LogHelper.d(TAG, "[doTakePicture] mCameraProxy = " + mCameraProxy);
            if (mCameraProxy == null) {
                return;
            }
            synchronized (mCaptureSync) {
                mIsInCapturing = true;
            }
            mCaptureStartTime = System.currentTimeMillis();
            mCaptureDataCallback = callback;
            setCaptureParameters(mJpegRotation);
            mSettingDeviceConfigurator.onPreviewStopped();
            mIsPreviewStarted = false;
            mCameraProxy.setVendorDataCallback(MTK_CAMERA_MSG_EXT_DATA_JPS, mStereoDataCallback);
            mCameraProxy.takePicture(mShutterCallback, mRawCallback, mPostViewCallback,
                    mJpegCallback);
        }

        private void doRequestChangeSettingValue(String key) {
            LogHelper.i(TAG, "[doRequestChangeSettingValue] key = " + key + ",mPreviewWidth = " +
                    mPreviewWidth + ",mPreviewHeight = " + mPreviewHeight);
            if (mPreviewWidth == 0 || mPreviewHeight == 0) {
                return;
            }
            if (mCameraState == CameraState.CAMERA_OPENED && mCameraProxy != null) {
                Camera.Parameters parameters = mCameraProxy.getOriginalParameters(true);
                parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
                boolean needRestartPreview =
                        mSettingDeviceConfigurator.configParameters(parameters);
                setVsdofEnable(parameters);
                if (needRestartPreview) {
                    doStopPreview();
                    mCameraProxy.setParameters(parameters);
                    doStartPreview();
                } else {
                    mCameraProxy.setParameters(parameters);
                }
            }
        }

        private void doRequestChangeCommand(String key) {
            if (mCameraState == CameraState.CAMERA_OPENED && mCameraProxy != null) {
                mSettingDeviceConfigurator.configCommand(key, mCameraProxy);
            }
        }

        private void doRequestChangeCommandImmediately(String key) {
            if (mCameraState == CameraState.CAMERA_OPENED && mCameraProxy != null) {
                mSettingDeviceConfigurator.configCommand(key, mCameraProxy);
            }
        }

        private Size getTargetPreviewSize(double ratio) {
            Camera.Parameters parameters = mCameraProxy.getOriginalParameters(false);
            List<Camera.Size> previewSize = parameters.getSupportedPreviewSizes();
            int length = previewSize.size();
            List<Size> sizes = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                sizes.add(i, new Size(previewSize.get(i).width, previewSize.get(i).height));
            }
            boolean isRotate = isDisplayRotateSupported(parameters);
            Size values = CameraUtil.getOptimalPreviewSize(mActivity, sizes, ratio, isRotate);
            mPreviewWidth = values.getWidth();
            mPreviewHeight = values.getHeight();
            updatePostViewSize(parameters, ratio);
            return values;
        }

        private void updatePostViewSize(Camera.Parameters parameters, double ratio) {
            String sizes = parameters.get(KEY_STEREO_POST_VIEW_SIZE_VALUES);
            List<Size> postViewSizes = SdofUtil.splitSize(sizes);
            if (postViewSizes != null) {
                int length = postViewSizes.size();
                for (int i = 0; i < length; i++) {
                    if (ratio == (double) postViewSizes.get(i).getWidth() /
                            postViewSizes.get(i).getHeight()) {
                        mPostViewSize = new Size(postViewSizes.get(i).getWidth(),
                                postViewSizes.get(i).getHeight());
                        LogHelper.i(TAG, "[updatePostViewSize] mPostViewSize :" + mPostViewSize);
                    }
                }
            }
        }

        private void doGetPreviewSize(Message msg) {
            int oldPreviewWidth = mPreviewWidth;
            int oldPreviewHeight = mPreviewHeight;
            double[] values = (double[]) msg.obj;
            getTargetPreviewSize(values[0]);
            values[1] = mPreviewWidth;
            values[2] = mPreviewHeight;
            boolean isSizeChanged = oldPreviewHeight != mPreviewHeight || oldPreviewWidth !=
                    mPreviewWidth;
            LogHelper.d(TAG, "[getPreviewSize], old size : " + oldPreviewWidth + " X " +
                    oldPreviewHeight + ", new  size :" + mPreviewWidth + " X " +
                    mPreviewHeight + ",is size changed: " + isSizeChanged);
            //if preview size change need do stop preview.
            if (isSizeChanged) {
                doStopPreview();
            }
        }

        private void doSetPictureSize(Size pictureSize) {
            mCurPictureSize = pictureSize;
        }

        private void doUpdatePreviewSurface(SurfaceHolder surfaceHolder) {
            LogHelper.d(TAG, "[doUpdatePreviewSurface],surfaceHolder = " + surfaceHolder + "," +
                    "state " + mCameraState + ",camera proxy = " + mCameraProxy);
            boolean isStateReady = CameraState.CAMERA_OPENED == mCameraState;
            if (isStateReady && mCameraProxy != null) {
                boolean onlySetSurfaceHolder = mSurfaceHolder == null && surfaceHolder != null;
                mSurfaceHolder = surfaceHolder;
                if (onlySetSurfaceHolder) {
                    setSurfaceHolderParameters();
                } else {
                    Camera.Parameters parameters = mCameraProxy.getOriginalParameters(true);
                    mSettingDeviceConfigurator.configParameters(parameters);
                    prePareAndStartPreview(parameters, false);
                }
            }
        }

        private boolean isReadyForCapture() {
            boolean value = true;
            if (mCameraProxy == null || !mIsPreviewStarted || mSurfaceHolder == null) {
                value = false;
            }
            LogHelper.d(TAG, "[isReadyForCapture] proxy is null : " + (mCameraProxy == null) +
                    ",isPreview Started = " + mIsPreviewStarted);
            return value;
        }

        // Need check ,if camera is opened or camera is in opening ,don't need open it.
        // if the camera id is same as before, don't need open it again.
        private boolean canDoOpenCamera(String newCameraId) {
            boolean value = true;
            boolean isStateError = CameraState.CAMERA_UNKNOWN != mCameraState;
            boolean isSameCamera = (mCameraId != null && newCameraId.equalsIgnoreCase(mCameraId));
            if (isStateError || isSameCamera) {
                value = false;
            }
            LogHelper.d(TAG, "[canDoOpenCamera], mCameraState = " + mCameraState + ",new Camera: " +
                    newCameraId + ",current camera : " + mCameraId + ",value = " + value);
            return value;
        }

        private void checkIsCapturing() {
            LogHelper.d(TAG, "[checkIsCapturing] mIsInCapturing = " + mIsInCapturing);
            synchronized (mCaptureSync) {
                if (mIsInCapturing) {
                    try {
                        mCaptureSync.wait(INTERRUPT_TIME_OUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private boolean isCameraAvailable() {
            return CameraState.CAMERA_OPENED == mCameraState && mCameraProxy != null;
        }


        private final Camera.PreviewCallback mFrameworkPreviewCallback = new Camera
                .PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                LogHelper.d(TAG, "[onPreviewFrame] mModeDeviceCallback = " + mModeDeviceCallback);
                mSettingDeviceConfigurator.onPreviewStarted();
                mIsPreviewStarted = true;
                if (mModeDeviceCallback != null) {
                    mModeDeviceCallback.onPreviewCallback(bytes, mPreviewFormat);
                }
            }
        };

        private final Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                long spendTime = System.currentTimeMillis() - mCaptureStartTime;
                LogHelper.d(TAG, "[mShutterCallback], spend time : " + spendTime + "ms");
            }
        };

        private final Camera.PictureCallback mRawCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                long rawTime = System.currentTimeMillis() - mCaptureStartTime;
                LogHelper.d(TAG, "[mRawCallback],spend time: " + rawTime + "ms");
            }
        };

        private final Camera.PictureCallback mPostViewCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                long postViewTime = System.currentTimeMillis() - mCaptureStartTime;
                LogHelper.d(TAG, "[mPostViewCallback],spend time : " + postViewTime + "ms"
                        + ", post view callback num: " + mPostViewCallbackNumber);
                if (bytes != null) {
                    mPostViewCallbackNumber++;
                    if (mCaptureDataCallback != null) {
                        mCaptureDataCallback.onPostViewCallback(bytes);
                    }
                }
            }
        };

        private final Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                long jpegTime = System.currentTimeMillis() - mCaptureStartTime;
                LogHelper.d(TAG, "[mJpegCallback],spend time :" + jpegTime + "ms"
                        + ", post view callback num: " + mPostViewCallbackNumber);
                notifyCaptureDone(bytes, mPostViewCallbackNumber == 0);
                if (mPostViewCallbackNumber > 0) {
                    mPostViewCallbackNumber--;
                }
            }
        };

        /**
         * Uncompressed image data callback.
         */
        private final CameraProxy.VendorDataCallback mUncompressedImageCallback
                = new CameraProxy.VendorDataCallback() {

            @Override
            public void onDataTaken(Message message) {
            }

            @Override
            public void onDataCallback(int msgId, byte[] data, int arg1, int arg2) {
                LogHelper.i(TAG, "[UncompressedImageCallback] onDataCallback");
                mSettingDeviceConfigurator.onPreviewStarted();
                mIsPreviewStarted = true;
                //if current is in capturing, also need notify the capture sync.
                //because jpeg will be callback next time.
                if (mCaptureDataCallback != null) {
                    DataCallbackInfo info = new DataCallbackInfo();
                    info.data = data;
                    info.needUpdateThumbnail = false;
                    mCaptureDataCallback.onDataReceived(info);
                }
            }
        };
        /**
         * Stereo Warning callback.
         */
        private final CameraProxy.VendorDataCallback mStereoWarningCallback
                = new CameraProxy.VendorDataCallback() {

            @Override
            public void onDataTaken(Message message) {
            }

            @Override
            public void onDataCallback(int msgId, byte[] data, int arg1, int arg2) {
                LogHelper.i(TAG, "[StereoWarningCallback] onDataCallback");
                mWarningCallback.onWarning(arg1);
            }
        };

        /**
         * Stereo data callback.
         */
        private final CameraProxy.VendorDataCallback mStereoDataCallback
                = new CameraProxy.VendorDataCallback() {

            @Override
            public void onDataTaken(Message message) {
                LogHelper.d(TAG, "[onDataTaken] message = " + message.what);
            }

            @Override
            public void onDataCallback(int msgId, byte[] data, int arg1, int arg2) {
                switch (arg1) {
                    case MTK_CAMERA_MSG_EXT_DATA_JPS:
                        mCaptureDataCallback.onJpsCapture(data);
                        break;
                    case MTK_CAMERA_MSG_EXT_DATA_STEREO_CLEAR_IMAGE:
                        mCaptureDataCallback.onClearImageCapture(data);
                        break;
                    case MTK_CAMERA_MSG_EXT_DATA_STEREO_DBG:
                        mCaptureDataCallback.onMaskCapture(data);
                        break;
                    case MTK_CAMERA_MSG_EXT_DATA_STEREO_DEPTHMAP:
                        mCaptureDataCallback.onDepthMapCapture(data);
                        break;
                    case MTK_CAMERA_MSG_EXT_DATA_STEREO_LDC:
                        mCaptureDataCallback.onLdcCapture(data);
                        break;
                    case MTK_CAMERA_MSG_EXT_DATA_STEREO_N3D:
                        mCaptureDataCallback.onN3dCapture(data);
                        break;
                    case MTK_CAMERA_MSG_EXT_DATA_STEREO_DEPTHWRAPPER:
                        mCaptureDataCallback.onDepthWrapperCapture(data);
                        break;
                    default:
                        break;
                }
            }
        };

        private void setCaptureParameters(int sensorOrientation) {
            int rotation = CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(mCameraId),
                    sensorOrientation, mActivity);
            if (mCameraProxy != null) {
                Camera.Parameters parameters = mCameraProxy.getParameters();
                mSettingDeviceConfigurator.configParameters(parameters);
                parameters.setPictureSize(mCurPictureSize.getWidth(), mCurPictureSize.getHeight());
                parameters.setRotation(rotation);
                mCameraProxy.setParameters(parameters);
            }
        }

        private void setDofLevelParameter(String level) {
            mLevel = level;
            if (mCameraProxy != null) {
                Camera.Parameters parameters = mCameraProxy.getParameters();
                parameters.set(KEY_VS_DOF_LEVEL, level);
                mCameraProxy.setParameters(parameters);
            }
        }

        private void captureDone() {
            LogHelper.d(TAG, "[captureDone], mIsInCapturing = " + mIsInCapturing);
            if (mPostViewCallbackNumber <= 1) {
                synchronized (mCaptureSync) {
                    if (mIsInCapturing) {
                        mIsInCapturing = false;
                        mCaptureSync.notify();
                    }
                }
            }
        }

        private boolean isDisplayRotateSupported(Camera.Parameters parameters) {
            String supported = parameters.get(KEY_DISP_ROT_SUPPORTED);
            if (supported == null || FALSE.equals(supported)) {
                return false;
            }
            return true;
        }

        private void prePareAndStartPreview(Camera.Parameters parameters, boolean isFromOnOpened) {
            LogHelper.d(TAG, "[prePareAndStartPreview] state : " + mCameraState + "," +
                    "mSurfaceHolder = " + mSurfaceHolder);
            setSurfaceHolderParameters();
            setVsdofEnable(parameters);
            setPreviewParameters(parameters);
            // Start preview.
            mCameraProxy.startPreview();
            mCameraProxy.setVendorDataCallback(MTK_CAMERA_MSG_EXT_NOTIFY_IMAGE_UNCOMPRESSED,
                    mUncompressedImageCallback);
            if (isFromOnOpened) {
                createSettingSecond(parameters);
            }
        }

        private void setPreviewParameters(Camera.Parameters parameters) {
            LogHelper.d(TAG, "[setPreviewParameters] mPreviewWidth = " + mPreviewWidth + "," +
                    "mPreviewHeight = " + mPreviewHeight);
            //set camera preview orientation.
            setDisplayOrientation();
            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            mCameraProxy.setParameters(parameters);
        }

        private void setDisplayOrientation() {
            int displayRotation = CameraUtil.getDisplayRotation(mActivity);
            int displayOrientation = CameraUtil.getDisplayOrientationFromDeviceSpec(displayRotation,
                    Integer.parseInt(mCameraId), mActivity);
            mCameraProxy.setDisplayOrientation(displayOrientation);
        }

        private void updatePreviewSize() {
            String pictureSize = mICameraContext.getDataStore().getValue(
                    KEY_STEREO_PICTURE_SIZE,
                    mSupportedSizes.get(1),
                    mICameraContext.getDataStore().getGlobalScope());
            if (pictureSize != null) {
                String[] pictureSizes = pictureSize.split("x");
                int width = Integer.parseInt(pictureSizes[0]);
                int height = Integer.parseInt(pictureSizes[1]);
                double ratio = (double) width / height;
                getTargetPreviewSize(ratio);
            }
        }

        private void initPictureSizes(Camera.Parameters parameters) {
            String stereoPictureSizes = parameters.get(KEY_REFOCUS_PICTURE_SIZE_VALUES);
            List<Size> supportedSizes = SdofUtil.splitSize(stereoPictureSizes);
            SdofUtil.sortSizeInDescending(supportedSizes);
            mSupportedSizes = SdofUtil.sizeToStr(supportedSizes);
            mCurPictureSize = CameraUtil.getSize(mICameraContext.getDataStore().getValue(
                    KEY_STEREO_PICTURE_SIZE,
                    mSupportedSizes.get(1),
                    mICameraContext.getDataStore().getGlobalScope()));
            String curSizeStr = mCurPictureSize.getWidth() + "x" + mCurPictureSize.getHeight();
            mICameraContext.getDataStore().setValue(
                    KEY_STEREO_PICTURE_SIZE,
                    curSizeStr,
                    mICameraContext.getDataStore().getGlobalScope(), true);
            for (int i = 0; i < mSupportedSizes.size(); i++) {
                if (!mPictureSizeMap.containsKey(mSupportedSizes.get(i))) {
                    Size pictureSize = CameraUtil.getSize(mSupportedSizes.get(i));
                    mPictureSizeMap.put(mSupportedSizes.get(i),
                            pictureSize.getHeight() * pictureSize.getWidth() + "M");
                }
            }
        }

        private void setSurfaceHolderParameters() {
            //set preview callback before start preview.
            if (mSurfaceHolder != null) {
                mCameraProxy.setOneShotPreviewCallback(mFrameworkPreviewCallback);
            }
            // Set preview display.
            try {
                mCameraProxy.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                //if set preview exception, Can't do anything.
                throw new RuntimeException("set preview display exception");
            }
        }

        private void setVsdofEnable(Camera.Parameters params) {
            params.set(KEY_STEREO_CAPTURE_MODE, "on");
            params.set(KEY_VSDOF_MODE, "on");
            params.set(KEY_STEREO_DENOISE_MODE, "off");
            if (mLevel != null) {
                params.set(KEY_VS_DOF_LEVEL, mLevel);
            }
            params.setPictureSize(mCurPictureSize.getWidth(),
                    mCurPictureSize.getHeight());
        }

        private void createSettingSecond(Camera.Parameters parameters) {
            mSettingManager.createSettingsByStage(2);
            mSettingDeviceConfigurator.setOriginalParameters(parameters);
            mSettingDeviceConfigurator.configParameters(parameters);
            mCameraProxy.setParameters(parameters);
        }

        private void doOnOpened(CameraProxy cameraProxy) {
            LogHelper.i(TAG, "[doOnOpened] cameraProxy = " + cameraProxy + mCameraState);
            if (CameraState.CAMERA_OPENING != mCameraState) {
                LogHelper.d(TAG, "[doOnOpened] state is error, don't need do on camera opened");
                return;
            }
            mCameraState = CameraState.CAMERA_OPENED;
            mICameraContext.getFeatureProvider().updateCameraParameters(mCameraId,
                    cameraProxy.getOriginalParameters(false));
            mSettingManager.createAllSettings();

            mSettingDeviceConfigurator.setOriginalParameters(
                    cameraProxy.getOriginalParameters(false));
            Camera.Parameters parameters = cameraProxy.getOriginalParameters(true);
            initPictureSizes(parameters);
            if (mModeDeviceCallback != null) {
                mModeDeviceCallback.onCameraOpened(mCameraId);
            }
            mPreviewFormat = parameters.getPreviewFormat();
            mSettingDeviceConfigurator.configParameters(parameters);
            updatePreviewSize();
            if (mCameraOpenedCallback != null) {
                mCameraOpenedCallback.onPreviewSizeReady(new Size(mPreviewWidth, mPreviewHeight),
                        mSupportedSizes);
            }
            prePareAndStartPreview(parameters, mNeedSubSectionInitSetting);
            mSettingManager.getSettingController().addViewEntry();
            mSettingManager.getSettingController().refreshViewEntry();
        }

        private void doOnDisconnected() {
            mCameraState = CameraState.CAMERA_UNKNOWN;
            //reset the surface holder to null
            mSurfaceHolder = null;

            CameraUtil.showErrorInfoAndFinish(mActivity, CameraUtil.CAMERA_ERROR_SERVER_DIED);
        }

        private void doOnError(int error) {
            //reset the surface holder to null
            mSurfaceHolder = null;
            mCameraState = CameraState.CAMERA_UNKNOWN;
            CameraUtil.showErrorInfoAndFinish(mActivity, error);
        }

        private void doDestroyHandler() {
            LogHelper.d(TAG, "[doDestroyHandler] mCameraState : " + mCameraState
                    + ",mIsPreviewStarted = " + mIsPreviewStarted);
            //first reset the mNeedQuitHandler to false;
            mNeedQuitHandler = false;
            if (CameraState.CAMERA_UNKNOWN == mCameraState || !mIsPreviewStarted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mRequestHandler.getLooper().quitSafely();
                } else {
                    mRequestHandler.getLooper().quit();
                }
            } else {
                mNeedQuitHandler = true;
            }
        }

        private boolean cancelDealMessage(int message) {
            //if have close message in the request handler,so follow message can be cancel execute.
            boolean value = false;
            if (mRequestHandler.hasMessages(PhotoDeviceAction.CLOSE_CAMERA)) {
                switch (message) {
                    case PhotoDeviceAction.START_PREVIEW:
                    case PhotoDeviceAction.STOP_PREVIEW:
                    case PhotoDeviceAction.TAKE_PICTURE:
                    case PhotoDeviceAction.REQUEST_CHANGE_SETTING_VALUE:
                    case PhotoDeviceAction.SET_PICTURE_SIZE:
                    case PhotoDeviceAction.SET_PREVIEW_CALLBACK:
                    case PhotoDeviceAction.SET_PREVIEW_SIZE_READY_CALLBACK:
                    case PhotoDeviceAction.GET_PREVIEW_SIZE:
                    case PhotoDeviceAction.UPDATE_PREVIEW_SURFACE:
                    case PhotoDeviceAction.UPDATE_G_SENSOR_ORIENTATION:
                    case PhotoDeviceAction.REQUEST_CHANGE_COMMAND:
                        value = true;
                        break;
                    default:
                        value = false;
                        break;
                }
            }
            return value;
        }

        private boolean hasDeviceStateCallback() {
            boolean value = mRequestHandler.hasMessages(PhotoDeviceAction.ON_CAMERA_ERROR)
                    || mRequestHandler.hasMessages(PhotoDeviceAction.ON_CAMERA_CLOSED)
                    || mRequestHandler.hasMessages(PhotoDeviceAction.ON_CAMERA_DISCONNECTED)
                    || mRequestHandler.hasMessages(PhotoDeviceAction.ON_CAMERA_OPENED);
            LogHelper.i(TAG, "[hasDeviceStateCallback] value = " + value);
            return value;
        }

        private void notifyCaptureDone(byte[] data, boolean needUpdateThumbnail) {
            captureDone();
            if (mCaptureDataCallback != null) {
                DataCallbackInfo info = new DataCallbackInfo();
                info.data = data;
                info.needUpdateThumbnail = needUpdateThumbnail;
                mCaptureDataCallback.onDataReceived(info);
            }
        }

        /**
         * Open camera device state callback, this callback is send to camera device manager
         * by open camera interface.
         */
        private class CameraDeviceProxyStateCallback extends CameraProxy.StateCallback {

            @Override
            public void onOpened(@Nonnull CameraProxy cameraProxy) {
                LogHelper.i(TAG, "[onOpened]proxy = " + cameraProxy + ",id:" + cameraProxy.getId()
                        + ",state = " + mCameraState);
                synchronized (mWaitCameraOpenDone) {
                    mCameraProxy = cameraProxy;
                    mWaitCameraOpenDone.notifyAll();
                    mRequestHandler.obtainMessage(PhotoDeviceAction.ON_CAMERA_OPENED, cameraProxy)
                            .sendToTarget();
                }
            }

            @Override
            public void onRetry() {
                LogHelper.i(TAG, "[onRetry]");
                CameraEx.setProperty(PROPERTY_KEY_CLIENT_APP_MODE,
                        APP_MODE_NAME_MTK_DUAL_CAMERA);
            }

            @Override
            public void onClosed(@Nonnull CameraProxy cameraProxy) {
                if (mCameraProxy == null || mCameraProxy != cameraProxy) {
                    LogHelper.i(TAG, "[onClosed], proxy is not same as before opened one, return");
                    return;
                }
                LogHelper.i(TAG, "[onClosed], current proxy : " + mCameraProxy + ", closed proxy " +
                        "= " + cameraProxy + ",camera id :" + cameraProxy.getId());
                synchronized (mWaitCameraOpenDone) {
                    mWaitCameraOpenDone.notifyAll();
                }
            }

            @Override
            public void onDisconnected(@Nonnull CameraProxy cameraProxy) {
                if (mCameraProxy != null && mCameraProxy != cameraProxy) {
                    LogHelper.i(TAG, "[onDisconnected], proxy is not same as before opened one, " +
                            "return");
                    return;
                }
                LogHelper.i(TAG, "[onDisconnected], current proxy : " + mCameraProxy + ", closed " +
                        "proxy " + cameraProxy + ",camera id :" + cameraProxy.getId());
                synchronized (mWaitCameraOpenDone) {
                    mWaitCameraOpenDone.notifyAll();
                    mRequestHandler.obtainMessage(PhotoDeviceAction.ON_CAMERA_DISCONNECTED,
                            cameraProxy).sendToTarget();
                }
            }

            @Override
            public void onError(@Nonnull CameraProxy cameraProxy, int error) {
                //if current is in capturing, but close is wait capture done,
                //so this case need notify the capture done. otherwise will be ANR to pause.
                captureDone();
                if (mCameraProxy != null && mCameraProxy != cameraProxy) {
                    LogHelper.i(TAG, "[onError], proxy is not same as before opened one, " +
                            "return");
                    return;
                }
                LogHelper.i(TAG, "[onError], current proxy : " + mCameraProxy + ", closed " +
                        "proxy " + cameraProxy + ",camera id :" + cameraProxy.getId());
                synchronized (mWaitCameraOpenDone) {
                    mWaitCameraOpenDone.notifyAll();
                    mCameraDeviceManager.recycle(mCameraId);
                    mRequestHandler.obtainMessage(PhotoDeviceAction.ON_CAMERA_ERROR, error, 0,
                            cameraProxy).sendToTarget();
                }
            }
        }
    }
}
