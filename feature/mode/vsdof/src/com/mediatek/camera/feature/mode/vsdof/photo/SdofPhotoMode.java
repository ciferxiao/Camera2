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

package com.mediatek.camera.feature.mode.vsdof.photo;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;

import com.mediatek.accessor.StereoInfoAccessor;
import com.mediatek.accessor.data.StereoCaptureInfo;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUi.AnimationData;
import com.mediatek.camera.common.IAppUiListener.ISurfaceStatusListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingManagerFactory;
import com.mediatek.camera.common.storage.MediaSaver.MediaSaverListener;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.vsdof.photo.device.DeviceControllerFactory;
import com.mediatek.camera.feature.mode.vsdof.photo.device.ISdofPhotoDeviceController;
import com.mediatek.camera.feature.mode.vsdof.photo.view.SdofPictureSizeSettingView;
import com.mediatek.camera.feature.mode.vsdof.view.SdofViewCtrl;
import com.mediatek.camera.portability.CameraEx;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Sdof photo mode implementation.
 */
public class SdofPhotoMode extends CameraModeBase implements
        ISdofPhotoDeviceController.DeviceCallback, ISdofPhotoDeviceController.PreviewSizeCallback {
    private static final Tag TAG = new Tag(SdofPhotoMode.class.getSimpleName());

    private static final String PROPERTY_KEY_CLIENT_APP_MODE = "client.appmode";
    private static final String APP_MODE_NAME_MTK_DUAL_CAMERA = "MtkStereo";
    private static final String KEY_STEREO_PICTURE_SIZE = "key_stereo_picture_size";
    private static final String KEY_PICTURE_SIZE = "key_picture_size";
    private static final int POST_VIEW_FORMAT = ImageFormat.NV21;
    private static final int MSG_WRITE_XMP = 0;
    private static final int VS_DOF_CALLBACK_NUM = 6;
    private int mCurrentNum = 0;
    private byte[] mJpsData;
    private byte[] mMaskAndConfigData;
    private byte[] mDepthMap;
    private byte[] mClearImage;
    private byte[] mLdcData;
    private byte[] mDepthWrapper;
    private byte[] mN3dData;
    private byte[] mOriginalJpegData;
    private byte[] mXmpJpegData;
    private StereoInfoAccessor mAccessor;
    private SaveHandler mSaveHandler;
    private long mDateTaken;

    protected ISdofPhotoDeviceController mISdofPhotoDeviceController;
    protected SdofPhotoHelper mSdofPhotoHelper;
    protected int mCaptureWidth;
    // make sure the picture size ratio = mCaptureWidth / mCaptureHeight not to NAN.
    protected int mCaptureHeight = Integer.MAX_VALUE;
    //the reason is if surface is ready, let it to set to device controller, otherwise
    //if surface is ready but activity is not into resume ,will found the preview
    //can not start preview.
    private volatile boolean mIsResumed = true;
    private String mCameraId;
    private boolean mIsDualCameraReady = true;
    private boolean mIsStereoCapture = true;
    private boolean mIsNeedUpdateThumb = true;

    private ISurfaceStatusListener mISurfaceStatusListener = new SurfaceChangeListener();
    private ISettingManager mISettingManager;
    private byte[] mPreviewData;
    private int mPreviewFormat;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private Object mPreviewDataSync = new Object();
    private HandlerThread mAnimationHandlerThread;
    private Handler mAnimationHandler;
    private List<String> mSupportSizes;

    private SdofPictureSizeSettingView mSdofPictureSizeSettingView;
    private SdofViewCtrl mSdofViewCtrl = new SdofViewCtrl();

    @Override
    public void init(@Nonnull IApp app, @Nonnull ICameraContext cameraContext,
                     boolean isFromLaunch) {
        LogHelper.d(TAG, "[init]+");
        super.init(app, cameraContext, isFromLaunch);
        mIApp.getAppUi().applyAllUIEnabledImmediately(false);
        mSdofPhotoHelper = new SdofPhotoHelper(cameraContext);
        createAnimationHandler();
        mAccessor = new StereoInfoAccessor();
        HandlerThread ht = new HandlerThread("Stereo Save Handler Thread");
        ht.start();
        mSaveHandler = new SaveHandler(ht.getLooper());
        mCameraId = getCameraIdByFacing(mDataStore.getValue(
                KEY_CAMERA_SWITCHER, null, mDataStore.getGlobalScope()));

        // Device controller must be initialize before set preview size, because surfaceAvailable
        // may be called immediately when setPreviewSize.
        DeviceControllerFactory deviceControllerFactory = new DeviceControllerFactory();
        mISdofPhotoDeviceController
                = deviceControllerFactory.createDeviceController(app.getActivity(),
                mCameraApi, mICameraContext);
        initSettingManager(mCameraId);
        prepareAndOpenCamera(false, mCameraId, false);
        mSdofViewCtrl.setViewChangeListener(mViewChangeListener);
        mSdofViewCtrl.init(app);
        mSdofViewCtrl.onOrientationChanged(mIApp.getGSensorOrientation());
        mSdofPictureSizeSettingView = new SdofPictureSizeSettingView(
                mPictureSizeChangeListener,
                app.getActivity(), cameraContext.getDataStore());
        LogHelper.d(TAG, "[init]- ");
    }

    @Override
    public void resume(@Nonnull DeviceUsage deviceUsage) {
        LogHelper.i(TAG, "[resume]+");
        super.resume(deviceUsage);
        mIsResumed = true;
        mCurrentNum = 0;
        initSettingManager(mCameraId);
        prepareAndOpenCamera(false, mCameraId, false);

        //enable all the ui when resume done.
        mIApp.getAppUi().applyAllUIEnabled(true);
        LogHelper.d(TAG, "[resume]-");
    }

    @Override
    public void pause(@Nullable DeviceUsage nextModeDeviceUsage) {
        LogHelper.i(TAG, "[pause]+");
        super.pause(nextModeDeviceUsage);
        boolean needCloseCamera =
                mNeedCloseCameraIds == null ? true : mNeedCloseCameraIds.size() > 0;
        boolean isSwitchMode = mNeedCloseCameraIds != null;
        mIsResumed = false;
        //clear the surface listener
        mIApp.getAppUi().clearPreviewStatusListener(mISurfaceStatusListener);
        //camera animation.
        synchronized (mPreviewDataSync) {
            if (isSwitchMode && mPreviewData != null) {
                startChangeModeAnimation();
            }
        }
        if (needCloseCamera) {
            prePareAndCloseCamera(true, mCameraId);
            recycleSettingManager(mCameraId);
        } else {
            //clear the all callbacks.
            clearAllCallbacks(mCameraId);
            //do stop preview
            mISdofPhotoDeviceController.stopPreview();
        }
        LogHelper.d(TAG, "[pause]-");
    }

    @Override
    public void unInit() {
        LogHelper.i(TAG, "[unInit]+");
        super.unInit();
        if (mSaveHandler != null) {
            mSaveHandler.getLooper().quit();
        }
        mIApp.getAppUi().removeSettingView(mSdofPictureSizeSettingView);
        mSdofViewCtrl.unInit();
        destroyAnimationHandler();
        mISdofPhotoDeviceController.destroyDeviceController();
        LogHelper.d(TAG, "[unInit]-");
    }

    @Override
    public void onOrientationChanged(int orientation) {
        mSdofViewCtrl.onOrientationChanged(orientation);
    }

    @Override
    public DeviceUsage getDeviceUsage(@Nonnull DataStore dataStore, DeviceUsage oldDeviceUsage) {
        ArrayList<String> openedCameraIds = new ArrayList<>();
        String cameraId = getCameraIdByFacing(dataStore.getValue(
                KEY_CAMERA_SWITCHER, null, dataStore.getGlobalScope()));
        openedCameraIds.add(cameraId);
        updateModeDefinedCameraApi();
        return new DeviceUsage(DeviceUsage.DEVICE_TYPE_STEREO_VSDOF, mCameraApi, openedCameraIds);
    }

    @Override
    public boolean onShutterButtonFocus(boolean pressed) {
        return true;
    }

    @Override
    public boolean onShutterButtonClick() {
        //Storage case
        boolean storageReady = mICameraContext.getStorageService().getCaptureStorageSpace() > 0;
        boolean isDeviceReady = mISdofPhotoDeviceController.isReadyForCapture();
        LogHelper.i(TAG, "onShutterButtonClick, is storage ready : " + storageReady + "," +
                "isDeviceReady = " + isDeviceReady);

        if (storageReady && isDeviceReady) {
            mIsStereoCapture = mIsDualCameraReady;
            //trigger capture animation
            startCaptureAnimation();
            updateModeDeviceState(MODE_DEVICE_STATE_CAPTURING);
            mIApp.getAppUi().applyAllUIEnabled(false);
            mISdofPhotoDeviceController.updateGSensorOrientation(mIApp.getGSensorOrientation());
            mISdofPhotoDeviceController.takePicture(mCaptureDataCallback);
        }
        return true;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    protected ISettingManager getSettingManager() {
        return mISettingManager;
    }

    @Override
    public void onCameraOpened(String cameraId) {
        LogHelper.d(TAG, "[onCameraOpened]");
        Relation relation = SdofPhotoRestriction.getRestriction().getRelation("on", false);
        String pictureSizeId = mISettingManager.getSettingController()
                .queryValue(KEY_PICTURE_SIZE);
        relation.addBody(KEY_PICTURE_SIZE, pictureSizeId, pictureSizeId);
        mISettingManager.getSettingController().postRestriction(relation);

        mISdofPhotoDeviceController.setStereoWarningCallback(mWarningCallback);
        updateModeDeviceState(MODE_DEVICE_STATE_OPENED);
    }

    @Override
    public void beforeCloseCamera() {
        updateModeDeviceState(MODE_DEVICE_STATE_CLOSED);
    }

    @Override
    public void afterStopPreview() {
        updateModeDeviceState(MODE_DEVICE_STATE_OPENED);
    }

    @Override
    public void onPreviewCallback(byte[] data, int format) {
        // Because we want use the one preview frame for doing switch camera animation
        // so will dismiss the later frames.
        // The switch camera data will be restore to null when camera close done.
        if (!mIsResumed) {
            return;
        }
        synchronized (mPreviewDataSync) {
            if (mPreviewData == null) {
                //Notify preview started.
                mIApp.getAppUi().applyAllUIEnabled(true);
                mIApp.getAppUi().onPreviewStarted(mCameraId);
                stopAllAnimations();
                updateModeDeviceState(MODE_DEVICE_STATE_PREVIEWING);
            }

            mPreviewData = data;
            mPreviewFormat = format;
        }
    }

    @Override
    public void onPreviewSizeReady(Size previewSize, List<String> pictureSizes) {
        LogHelper.i(TAG, "[onPreviewSizeReady] previewSize: " + previewSize.toString());
        mSupportSizes = pictureSizes;
        // update video quality view
        mSdofPictureSizeSettingView.setEntryValues(pictureSizes);
        mSdofPictureSizeSettingView.setDefaultValue(pictureSizes.get(0));

        mIApp.getAppUi().addSettingView(mSdofPictureSizeSettingView);
        updatePictureSizeAndPreviewSize(previewSize);
    }

    private final SdofPictureSizeSettingView.Listener mPictureSizeChangeListener
            = new SdofPictureSizeSettingView.Listener() {
        @Override
        public void onSizeChanged(String newSize) {
            String[] sizes = newSize.split("x");
            mCaptureWidth = Integer.parseInt(sizes[0]);
            mCaptureHeight = Integer.parseInt(sizes[1]);
            mISdofPhotoDeviceController.setPictureSize(new Size(mCaptureWidth, mCaptureHeight));
            Size previewSize = mISdofPhotoDeviceController.getPreviewSize((double) mCaptureWidth
                    / mCaptureHeight);
            int width = previewSize.getWidth();
            int height = previewSize.getHeight();
            if (width != mPreviewWidth || height != mPreviewHeight) {
                onPreviewSizeChanged(width, height);
            }
        }
    };

    /**
     * This class used for write jpeg to xmp and saving.
     */
    private class SaveHandler extends Handler {
        SaveHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_WRITE_XMP:
                    StereoDataGroup mDataGroup = (StereoDataGroup) msg.obj;
                    StereoCaptureInfo captureInfo = new StereoCaptureInfo();
                    String fileName = mSdofPhotoHelper.getFileName(mDataGroup.getCaptureTime());
                    captureInfo.debugDir = fileName;
                    captureInfo.jpsBuffer = mDataGroup.getJpsData();
                    captureInfo.jpgBuffer = mDataGroup.getOriginalJpegData();
                    captureInfo.configBuffer = mDataGroup.getMaskAndConfigData();
                    captureInfo.clearImage = mDataGroup.getClearImage();
                    captureInfo.depthMap = mDataGroup.getDepthMap();
                    captureInfo.ldc = mDataGroup.getLdcData();
                    captureInfo.debugBuffer = mDataGroup.getN3dDebugData();
                    captureInfo.depthBuffer = mDataGroup.getDepthWrapper();
                    mXmpJpegData = mAccessor.writeStereoCaptureInfo(captureInfo);
                    LogHelper.i(TAG, "save by xmp jpeg:" + mXmpJpegData);
                    if (mXmpJpegData != null) {
                        saveFile(mXmpJpegData, mDataGroup.getCaptureTime(),
                                true, mIsNeedUpdateThumb);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private ISdofPhotoDeviceController.CaptureDataCallback mCaptureDataCallback
            = new ISdofPhotoDeviceController.CaptureDataCallback() {
        @Override
        public void onDataReceived(ISdofPhotoDeviceController.DataCallbackInfo dataInfo) {
            //when mode receive the data, need save it.
            LogHelper.d(TAG, "onDataReceived, data = " + dataInfo.data
                    + ",mIsResumed = " + mIsResumed + ", isStereo = " + mIsStereoCapture);
            byte[] jpegData = dataInfo.data;
            if (jpegData == null) {
                //Notify preview started.
                mIApp.getAppUi().applyAllUIEnabled(true);
                mIApp.getAppUi().onPreviewStarted(mCameraId);
                updateModeDeviceState(MODE_DEVICE_STATE_PREVIEWING);
                return;
            }
            //save file first,because save file is in other thread, so will improve the shot to shot
            //performance.
            mDateTaken = System.currentTimeMillis();
            mIsNeedUpdateThumb = dataInfo.needUpdateThumbnail;
            if (!mIsStereoCapture) {
                saveFile(jpegData, mDateTaken, false, mIsNeedUpdateThumb);
            }
            mOriginalJpegData = jpegData;
            notifyMergeData();
        }

        @Override
        public void onPostViewCallback(byte[] data) {
            LogHelper.d(TAG, "[onPostViewCallback] data = " + data);
            if (data != null) {
                //will update the thumbnail
                int rotation = CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(mCameraId),
                        mIApp.getGSensorOrientation(), mIApp.getActivity());
                Size postViewSize = mISdofPhotoDeviceController.getPostViewSize();
                Bitmap bitmap = BitmapCreator.createBitmapFromYuv(data, POST_VIEW_FORMAT,
                        postViewSize.getWidth(), postViewSize.getHeight(),
                        mIApp.getAppUi().getThumbnailViewWidth(),
                        rotation);
                mIApp.getAppUi().updateThumbnail(bitmap);
            }
        }

        @Override
        public void onJpsCapture(byte[] jpsData) {
            mJpsData = jpsData;
            notifyMergeData();
        }

        @Override
        public void onMaskCapture(byte[] maskData) {
            mMaskAndConfigData = maskData;
            notifyMergeData();
        }

        @Override
        public void onDepthMapCapture(byte[] depthMapData) {
            mDepthMap = depthMapData;
            notifyMergeData();
        }

        @Override
        public void onClearImageCapture(byte[] clearImageData) {
            mClearImage = clearImageData;
            notifyMergeData();
        }

        @Override
        public void onLdcCapture(byte[] ldcData) {
            mLdcData = ldcData;
            notifyMergeData();
        }

        @Override
        public void onN3dCapture(byte[] n3dData) {
            mN3dData = n3dData;
            notifyMergeData();
        }


        @Override
        public void onDepthWrapperCapture(byte[] depthWrapper) {
            mDepthWrapper = depthWrapper;
            notifyMergeData();
        }
    };

    private void notifyMergeData() {
        LogHelper.i(TAG, "[notifyMergeData] mCurrentNum = " + mCurrentNum
                + ", isStereo = " + mIsStereoCapture);
        mCurrentNum++;
        if (mCurrentNum == VS_DOF_CALLBACK_NUM) {
            if (mIsStereoCapture) {
                mDateTaken = System.currentTimeMillis();
                StereoDataGroup mDataGroup = new StereoDataGroup(mDateTaken,
                        mOriginalJpegData, mJpsData, mMaskAndConfigData,
                        mDepthMap, mClearImage, mLdcData, mN3dData, mDepthWrapper);
                mSaveHandler.obtainMessage(MSG_WRITE_XMP, mDataGroup).sendToTarget();
            }
            mCurrentNum = 0;
        }
    }

    private void saveFile(byte[] jpegData, long time, boolean isStereo, boolean needUpdateThumb) {
        LogHelper.i(TAG, "[saveFile] isStereo " + isStereo + ", update thumb " + needUpdateThumb);
        String fileDirectory = mICameraContext.getStorageService().getFileDirectory();
        Size exifSize = CameraUtil.getSizeFromExif(jpegData);
        ContentValues contentValues = mSdofPhotoHelper.createContentValues(jpegData,
                fileDirectory, exifSize.getWidth(), exifSize.getHeight(), time, isStereo);
        mICameraContext.getMediaSaver().addSaveRequest(jpegData, contentValues, null,
                mMediaSaverListener);
        //reset the switch camera to null
        synchronized (mPreviewDataSync) {
            mPreviewData = null;
        }
        if (needUpdateThumb) {
            //update thumbnail
            Bitmap bitmap = BitmapCreator.createBitmapFromJpeg(jpegData, mIApp.getAppUi()
                    .getThumbnailViewWidth());
            mIApp.getAppUi().updateThumbnail(bitmap);
        }
    }

    private void onPreviewSizeChanged(int width, int height) {
        //Need reset the preview data to null if the preview size is changed.
        synchronized (mPreviewDataSync) {
            mPreviewData = null;
        }
        mPreviewWidth = width;
        mPreviewHeight = height;
        mIApp.getAppUi().setPreviewSize(mPreviewWidth, mPreviewHeight, mISurfaceStatusListener);
    }

    private void prepareAndOpenCamera(boolean needOpenCameraSync, String cameraId,
                                      boolean needFastStartPreview) {
        mCameraId = cameraId;

        //before open camera, prepare the preview callback and size changed callback.
        mISdofPhotoDeviceController.setDeviceCallback(this);
        mISdofPhotoDeviceController.setPreviewSizeReadyCallback(this);
        //prepare device info.
        DeviceInfo info = new DeviceInfo();
        info.setCameraId(mCameraId);
        info.setSettingManager(mISettingManager);
        info.setNeedOpenCameraSync(needOpenCameraSync);

        LogHelper.i(TAG, "[setProperty] stereo camera mode");
        CameraEx.setProperty(PROPERTY_KEY_CLIENT_APP_MODE,
                APP_MODE_NAME_MTK_DUAL_CAMERA);
        mISdofPhotoDeviceController.openCamera(info);
    }

    private void prePareAndCloseCamera(boolean needSync, String cameraId) {
        clearAllCallbacks(cameraId);
        mISdofPhotoDeviceController.closeCamera(needSync);
        //reset the preview size and preview data.
        mPreviewData = null;
        mPreviewWidth = 0;
        mPreviewHeight = 0;
    }

    private void clearAllCallbacks(String cameraId) {
        mISdofPhotoDeviceController.setPreviewSizeReadyCallback(null);
    }

    private void initSettingManager(String cameraId) {
        SettingManagerFactory smf = mICameraContext.getSettingManagerFactory();
        mISettingManager = smf.getInstance(
                cameraId,
                getModeKey(),
                ModeType.PHOTO,
                mCameraApi);
    }

    private void recycleSettingManager(String cameraId) {
        mICameraContext.getSettingManagerFactory().recycle(cameraId);
    }

    private void createAnimationHandler() {
        mAnimationHandlerThread = new HandlerThread("Animation_handler");
        mAnimationHandlerThread.start();
        mAnimationHandler = new Handler(mAnimationHandlerThread.getLooper());
    }

    private void destroyAnimationHandler() {
        if (mAnimationHandlerThread.isAlive()) {
            mAnimationHandlerThread.quit();
            mAnimationHandler = null;
        }
    }

    private boolean canSelectCamera(@Nonnull String newCameraId) {
        boolean value = true;

        if (newCameraId == null || mCameraId.equalsIgnoreCase(newCameraId)) {
            value = false;
        }
        LogHelper.d(TAG, "[canSelectCamera] +: " + value);
        return value;
    }

    private void doCameraSelect(String oldCamera, String newCamera) {
        mIApp.getAppUi().applyAllUIEnabledImmediately(false);
        mIApp.getAppUi().onCameraSelected(newCamera);
        prePareAndCloseCamera(true, oldCamera);
        recycleSettingManager(oldCamera);
        initSettingManager(newCamera);
        prepareAndOpenCamera(false, newCamera, false);

        mIApp.getAppUi().applyAllUIEnabledImmediately(true);
    }

    private MediaSaverListener mMediaSaverListener = new MediaSaverListener() {

        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(TAG, "[onFileSaved] uri = " + uri);
            mIApp.notifyNewMedia(uri, true);
        }
    };

    private ISdofPhotoDeviceController.StereoWarningCallback mWarningCallback
            = new ISdofPhotoDeviceController.StereoWarningCallback() {
        @Override
        public void onWarning(int type) {
            LogHelper.i(TAG, "[StereoWarningCallback onWarning] " + type);
            switch (type) {
                case SdofViewCtrl.DUAL_CAMERA_LOW_LIGHT:
                    mIsDualCameraReady = false;
                    break;
                case SdofViewCtrl.DUAL_CAMERA_READY:
                    mIsDualCameraReady = true;
                    break;
                case SdofViewCtrl.DUAL_CAMERA_TOO_CLOSE:
                    mIsDualCameraReady = false;
                    break;
                case SdofViewCtrl.DUAL_CAMERA_LENS_COVERED:
                    mIsDualCameraReady = false;
                    break;
                default:
                    LogHelper.w(TAG, "Warning message don't need to show");
                    break;
            }
            mSdofViewCtrl.showWarningView(type);
        }
    };

    private void stopAllAnimations() {
        LogHelper.d(TAG, "[stopAllAnimations]");
        if (mAnimationHandler == null) {
            return;
        }
        //clear the old one.
        mAnimationHandler.removeCallbacksAndMessages(null);
        mAnimationHandler.post(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(TAG, "[stopAllAnimations] run");
                //means preview is started, so need notify switch camera animation need stop.
                stopSwitchCameraAnimation();
                //need notify change mode animation need stop if is doing change mode.
                stopChangeModeAnimation();
                //stop the capture animation if is doing capturing.
                stopCaptureAnimation();
            }
        });
    }

    private void startSwitchCameraAnimation() {
        // Prepare the animation data.
        IAppUi.AnimationData data = prepareAnimationData(mPreviewData, mPreviewWidth,
                mPreviewHeight, mPreviewFormat);
        // Trigger animation start.
        mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_SWITCH_CAMERA, data);
    }

    private void stopSwitchCameraAnimation() {
        mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_CAMERA);
    }

    private void startChangeModeAnimation() {
        AnimationData data = prepareAnimationData(mPreviewData, mPreviewWidth,
                mPreviewHeight, mPreviewFormat);
        mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_SWITCH_MODE, data);
    }

    private void stopChangeModeAnimation() {
        mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_MODE);
    }

    private void startCaptureAnimation() {
        mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_CAPTURE, null);
    }

    private void stopCaptureAnimation() {
        mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_CAPTURE);
    }

    private AnimationData prepareAnimationData(byte[] data, int width, int height, int format) {
        // Prepare the animation data.
        AnimationData animationData = new AnimationData();
        animationData.mData = data;
        animationData.mWidth = width;
        animationData.mHeight = height;
        animationData.mFormat = format;
        animationData.mOrientation = mSdofPhotoHelper.getCameraInfoOrientation(mCameraId);
        animationData.mIsMirror = mSdofPhotoHelper.isMirror(mCameraId);
        return animationData;
    }

    private void updatePictureSizeAndPreviewSize(Size previewSize) {
        String pictureSize = mICameraContext.getDataStore().getValue(
                KEY_STEREO_PICTURE_SIZE,
                mSupportSizes.get(0),
                mICameraContext.getDataStore().getGlobalScope());
        if (pictureSize != null && mIsResumed) {
            String[] pictureSizes = pictureSize.split("x");
            mCaptureWidth = Integer.parseInt(pictureSizes[0]);
            mCaptureHeight = Integer.parseInt(pictureSizes[1]);
            mISdofPhotoDeviceController.setPictureSize(new Size(mCaptureWidth, mCaptureHeight));
            int width = previewSize.getWidth();
            int height = previewSize.getHeight();
            LogHelper.d(TAG, "[updatePictureSizeAndPreviewSize] picture size: " + mCaptureWidth +
                    " X" + mCaptureHeight + ",current preview size:" + mPreviewWidth + " X " +
                    mPreviewHeight + ",new value :" + width + " X " + height);
            if (width != mPreviewWidth || height != mPreviewHeight) {
                onPreviewSizeChanged(width, height);
            }
        }

    }

    /**
     * surface changed listener.
     */
    private class SurfaceChangeListener implements ISurfaceStatusListener {

        @Override
        public void surfaceAvailable(SurfaceHolder surfaceHolder, int width, int height) {
            LogHelper.d(TAG, "surfaceAvailable,device controller = " + mISdofPhotoDeviceController
                    + ",w = " + width + ",h = " + height);
            if (mISdofPhotoDeviceController != null && mIsResumed) {
                mISdofPhotoDeviceController.updatePreviewSurface(surfaceHolder);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int width, int height) {
            LogHelper.d(TAG, "surfaceChanged, device controller = " + mISdofPhotoDeviceController
                    + ",w = " + width + ",h = " + height);
            if (mISdofPhotoDeviceController != null && mIsResumed) {
                mISdofPhotoDeviceController.updatePreviewSurface(surfaceHolder);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            LogHelper.d(TAG, "surfaceDestroyed,device controller = " + mISdofPhotoDeviceController);
        }
    }

    private SdofViewCtrl.ViewChangeListener mViewChangeListener
            = new SdofViewCtrl.ViewChangeListener() {
        @Override
        public void onVsDofLevelChanged(String level) {
            mISdofPhotoDeviceController.setVsDofLevelParameter(level);
        }

        @Override
        public void onTouchPositionChanged(String value) {
        }
    };
}
