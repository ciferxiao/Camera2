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

package com.mediatek.camera.common.mode.photo;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.EmbossMaskFilter;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextPaint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUi.AnimationData;
import com.mediatek.camera.common.IAppUiListener.ISurfaceStatusListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.CameraSysTrace;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory.CameraApi;
import com.mediatek.camera.common.memory.IMemoryManager;
import com.mediatek.camera.common.memory.MemoryManagerImpl;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.mode.photo.device.DeviceControllerFactory;
import com.mediatek.camera.common.mode.photo.device.IDeviceController;
import com.mediatek.camera.common.mode.photo.device.IDeviceController.DataCallbackInfo;
import com.mediatek.camera.common.mode.photo.device.IDeviceController.DeviceCallback;
import com.mediatek.camera.common.mode.photo.device.IDeviceController.PreviewSizeCallback;
import com.mediatek.camera.common.mode.photo.device.IDeviceController.JpegCallback;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.relation.StatusMonitor.StatusChangeListener;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingManagerFactory;
import com.mediatek.camera.common.storage.MediaSaver.MediaSaverListener;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

//xiao add for bitmap 
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import java.io.ByteArrayOutputStream;
import android.view.View.MeasureSpec;

import android.graphics.drawable.Drawable;
import android.graphics.PointF;
import java.util.List;
import android.graphics.Path;
import com.mediatek.camera.common.rcs.RulerCalculator;
import android.util.Log;
/**
 * Normal photo mode that is used to take normal picture.
 */
public class PhotoMode extends CameraModeBase implements JpegCallback,
        DeviceCallback, PreviewSizeCallback, IMemoryManager.IMemoryListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PhotoMode.class.getSimpleName());
    private static final String KEY_MATRIX_DISPLAY_SHOW = "key_matrix_display_show";
    private static final String KEY_PICTURE_SIZE = "key_picture_size";
    private static final String KEY_DNG = "key_dng";
    private static final String JPEG_CALLBACK = "jpeg callback";
    private static final String POST_VIEW_CALLBACK = "post view callback";

    private static final int POST_VIEW_FORMAT = ImageFormat.NV21;
    private static final long DNG_IMAGE_SIZE = 45 * 1024 * 1024;

    protected static final String PHOTO_CAPTURE_START = "start";
    protected static final String PHOTO_CAPTURE_STOP = "stop";
    protected static final String KEY_PHOTO_CAPTURE = "key_photo_capture";

    protected IDeviceController mIDeviceController;
    protected PhotoModeHelper mPhotoModeHelper;
    protected int mCaptureWidth;
    // make sure the picture size ratio = mCaptureWidth / mCaptureHeight not to NAN.
    protected int mCaptureHeight = Integer.MAX_VALUE;
    //the reason is if surface is ready, let it to set to device controller, otherwise
    //if surface is ready but activity is not into resume ,will found the preview
    //can not start preview.
    protected volatile boolean mIsResumed = true;
    private String mCameraId;

    private ISurfaceStatusListener mISurfaceStatusListener = new SurfaceChangeListener();
    private ISettingManager mISettingManager;
    private MemoryManagerImpl mMemoryManager;
    private byte[] mPreviewData;
    private int mPreviewFormat;
    private int mPreviewWidth;
    private int mPreviewHeight;
    //make sure it is in capturing to show the saving UI.
    private int mCapturingNumber = 0;
    private boolean mIsMatrixDisplayShow = false;
    private Object mPreviewDataSync = new Object();
    private Object mCaptureNumberSync = new Object();
    private HandlerThread mAnimationHandlerThread;
    private Handler mAnimationHandler;
    private StatusChangeListener mStatusChangeListener = new MyStatusChangeListener();
    private IMemoryManager.MemoryAction mMemoryState = IMemoryManager.MemoryAction.NORMAL;
    protected StatusMonitor.StatusResponder mPhotoStatusResponder;
    
    //xiao add for bitmap
    private Bitmap mPhotoBitmap; 
	String[] info;
	
	////RCS100 add for ruler on photo start
	private RulerCalculator rulerCalculator;
    private PointF mLeftPoint ;
    private int mPathSpace = 10;
	private Paint rulerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final float photoheight = 2448;
	private final float photowidth = 3264;
	private final float Radius = photoheight/1.3f;
	
	private final float multiplewidth = photowidth/1280;
	private final float multipleheight = photoheight/720;
	
	private PointF finalleft;
	private PointF finalright;
	private float  mRulerDistance;
	////RCS100 add for ruler on photo end
	
    @Override
    public void init(@Nonnull IApp app, @Nonnull ICameraContext cameraContext,
            boolean isFromLaunch) {
        LogHelper.d(TAG, "[init]+");
        super.init(app, cameraContext, isFromLaunch);
        mIApp.getAppUi().applyAllUIEnabledImmediately(false);
        mPhotoModeHelper = new PhotoModeHelper(cameraContext);
        createAnimationHandler();

        mCameraId = getCameraIdByFacing(mDataStore.getValue(
                KEY_CAMERA_SWITCHER, null, mDataStore.getGlobalScope()));

        // Device controller must be initialize before set preview size, because surfaceAvailable
        // may be called immediately when setPreviewSize.
        DeviceControllerFactory deviceControllerFactory = new DeviceControllerFactory();
        mIDeviceController = deviceControllerFactory.createDeviceController(app.getActivity(),
                mCameraApi, mICameraContext,mIApp);//xiao add for uvc
        initSettingManager(mCameraId);
        initStatusMonitor();
        mMemoryManager = new MemoryManagerImpl(app.getActivity());
        prepareAndOpenCamera(false, mCameraId, isFromLaunch);
        LogHelper.d(TAG, "[init]- ");
    }

    //add RCS 曝光功能增加 liuxi 20180725 start
    @Override
    public void requestChangeExposureValue(int value){
        Log.i("ccc_ddd","value 1111111111 " + value);
        mIDeviceController.requestChangeExposureValue(value);
    }
    //add RCS 曝光功能增加 liuxi 20180725 end


    //add RCS 30秒情况下,点击屏幕取消拍照暂停 liuxi 20180726 start
    @Override
    public void removeStartPreviewHandler() {
        mIDeviceController.removeStartPreviewHandler();
    }
    //add RCS 30秒情况下,点击屏幕取消拍照暂停 liuxi 20180726 end

    @Override
    public void resume(@Nonnull DeviceUsage deviceUsage) {
        super.resume(deviceUsage);
        mIsResumed = true;
        initSettingManager(mCameraId);
        initStatusMonitor();
        mMemoryManager.addListener(this);
        mMemoryManager.initStateForCapture(
                mICameraContext.getStorageService().getCaptureStorageSpace());
        mMemoryState = IMemoryManager.MemoryAction.NORMAL;

        
        prepareAndOpenCamera(false, mCameraId, false);
        
        //xiao add for uvc
        mIDeviceController.onresume();
        //enable all the ui when resume done.
        mIApp.getAppUi().applyAllUIEnabled(true);
    }

    @Override
    public void pause(@Nullable DeviceUsage nextModeDeviceUsage) {
        LogHelper.i(TAG, "[pause]+");
        super.pause(nextModeDeviceUsage);
        
        mIDeviceController.onpaused();//xiao add for uvc start
        
        mIsResumed = false;
        mMemoryManager.removeListener(this);
        //clear the surface listener
        mIApp.getAppUi().clearPreviewStatusListener(mISurfaceStatusListener);
        //camera animation.
        // this animation is no need right now so just mark it
        // if some day need can open it.
//        synchronized (mPreviewDataSync) {
//            if (mNeedCloseCameraIds != null && mPreviewData != null) {
//                startChangeModeAnimation();
//            }
//        }
        if (mNeedCloseCameraIds.size() > 0) {
            prePareAndCloseCamera(needCloseCameraSync(), mCameraId);
            recycleSettingManager(mCameraId);
        } else {
            //clear the all callbacks.
            clearAllCallbacks(mCameraId);
            //do stop preview
            mIDeviceController.stopPreview();
        }
        LogHelper.i(TAG, "[pause]-");
    }

    @Override
    public void unInit() {
        super.unInit();
        destroyAnimationHandler();
        mIDeviceController.destroyDeviceController();
    }

    @Override
    public boolean onCameraSelected(@Nonnull String newCameraId) {
        LogHelper.i(TAG, "[onCameraSelected] ,new id:" + newCameraId + ",current id:" + mCameraId);
        //first need check whether can switch camera or not.
        if (canSelectCamera(newCameraId)) {
            //trigger switch camera animation in here
            //must before mCamera = newCameraId, otherwise the animation's orientation and
            // whether need mirror is error.
            synchronized (mPreviewDataSync) {
                startSwitchCameraAnimation();
            }
            doCameraSelect(mCameraId, newCameraId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onShutterButtonFocus(boolean pressed) {
        return true;
    }

    @Override
    public boolean onShutterButtonClick() {
        //Storage case
        boolean storageReady = mICameraContext.getStorageService().getCaptureStorageSpace() > 0;
        boolean isDeviceReady = mIDeviceController.isReadyForCapture();
        LogHelper.i(TAG, "onShutterButtonClick, is storage ready : " + storageReady + "," +
                "isDeviceReady = " + isDeviceReady);

        if (storageReady && isDeviceReady && mMemoryState != IMemoryManager.MemoryAction.STOP) {
            //trigger capture animation
            //startCaptureAnimation();//M: 拍照闪黑问题 liuxi 20180716
            mPhotoStatusResponder.statusChanged(KEY_PHOTO_CAPTURE, PHOTO_CAPTURE_START);
            updateModeDeviceState(MODE_DEVICE_STATE_CAPTURING);
            mIApp.getAppUi().applyAllUIEnabled(false);
            mIDeviceController.updateGSensorOrientation(mIApp.getGSensorOrientation());
            mIDeviceController.takePicture(this);
            mIApp.showToast();
        }
        return true;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    public void onDataReceived(DataCallbackInfo dataCallbackInfo) {
        //when mode receive the data, need save it.
        byte[] jpegData = dataCallbackInfo.data;
        boolean needUpdateThumbnail = dataCallbackInfo.needUpdateThumbnail;
        boolean needRestartPreview = dataCallbackInfo.needRestartPreview;
        LogHelper.d(TAG, "onDataReceived, data = " + jpegData + ",mIsResumed = " + mIsResumed +
                ",needUpdateThumbnail = " + needUpdateThumbnail + ",needRestartPreview = " +
                needRestartPreview);
        if (jpegData != null) {
            CameraSysTrace.onEventSystrace(JPEG_CALLBACK, true);
        }
        //save file first,because save file is in other thread, so will improve the shot to shot
        //performance.
        if (jpegData != null) {
            saveData(jpegData);
        }
        //if camera is paused, don't need do start preview and other device related actions.
        if (mIsResumed) {
            //first do start preview in API1.
            if (mCameraApi == CameraApi.API1) {
                if (needRestartPreview && !mIsMatrixDisplayShow) {
                    mIDeviceController.startPreview();
                }
            }
        }
        //update thumbnail
        if (jpegData != null && needUpdateThumbnail) {
            //updateThumbnail(jpegData);//xiao add for thumbnailview
        }
        if (jpegData != null) {
            CameraSysTrace.onEventSystrace(JPEG_CALLBACK, false);
        }
    }

    @Override
    public void onPostViewCallback(byte[] data) {
        LogHelper.d(TAG, "[onPostViewCallback] data = " + data + ",mIsResumed = " + mIsResumed);
        CameraSysTrace.onEventSystrace(POST_VIEW_CALLBACK, true);
        if (data != null && mIsResumed) {
            //will update the thumbnail
            int rotation = CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(mCameraId),
                    mIApp.getGSensorOrientation(), mIApp.getActivity());
            Bitmap bitmap = BitmapCreator.createBitmapFromYuv(data, POST_VIEW_FORMAT,
                    mPreviewWidth, mPreviewHeight, mIApp.getAppUi().getThumbnailViewWidth(),
                    rotation);
            //mIApp.getAppUi().updateThumbnail(bitmap);
        }
        CameraSysTrace.onEventSystrace(POST_VIEW_CALLBACK, false);
    }

    @Override
    protected ISettingManager getSettingManager() {
        return mISettingManager;
    }

    @Override
    public void onCameraOpened(String cameraId) {
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
            //Notify preview started.
            if (!mIsMatrixDisplayShow) {
                mIApp.getAppUi().applyAllUIEnabled(true);
            }
            mIApp.getAppUi().onPreviewStarted(mCameraId);
            if (mPreviewData == null) {
                stopAllAnimations();
            }
            updateModeDeviceState(MODE_DEVICE_STATE_PREVIEWING);

            mPreviewData = data;
            mPreviewFormat = format;
        }
    }


    @Override
    public void onPreviewSizeReady(Size previewSize) {
        LogHelper.d(TAG, "[onPreviewSizeReady] previewSize: " + previewSize.toString());
        updatePictureSizeAndPreviewSize(previewSize);
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
        StatusMonitor statusMonitor = mICameraContext.getStatusMonitor(mCameraId);
        statusMonitor.registerValueChangedListener(KEY_PICTURE_SIZE, mStatusChangeListener);
        statusMonitor.registerValueChangedListener(KEY_MATRIX_DISPLAY_SHOW, mStatusChangeListener);

        //before open camera, prepare the device callback and size changed callback.
        mIDeviceController.setDeviceCallback(this);
        mIDeviceController.setPreviewSizeReadyCallback(this);
        //prepare device info.
        DeviceInfo info = new DeviceInfo();
        info.setCameraId(mCameraId);
        info.setSettingManager(mISettingManager);
        info.setNeedOpenCameraSync(needOpenCameraSync);
        info.setNeedFastStartPreview(needFastStartPreview);
        mIDeviceController.openCamera(info);
    }

    private void prePareAndCloseCamera(boolean needSync, String cameraId) {
        clearAllCallbacks(cameraId);
        mIDeviceController.closeCamera(needSync);
        mIsMatrixDisplayShow = false;
        //reset the preview size and preview data.
        mPreviewData = null;
        mPreviewWidth = 0;
        mPreviewHeight = 0;
    }

    private void clearAllCallbacks(String cameraId) {
        mIDeviceController.setPreviewSizeReadyCallback(null);
        StatusMonitor statusMonitor = mICameraContext.getStatusMonitor(cameraId);
        statusMonitor.unregisterValueChangedListener(KEY_PICTURE_SIZE, mStatusChangeListener);
        statusMonitor.unregisterValueChangedListener(
                KEY_MATRIX_DISPLAY_SHOW, mStatusChangeListener);
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
    }

    private MediaSaverListener mMediaSaverListener = new MediaSaverListener() {

        @Override
        public void onFileSaved(Uri uri) {
            mIApp.notifyNewMedia(uri, true);
            synchronized (mCaptureNumberSync) {
                mCapturingNumber--;
                if (mCapturingNumber == 0) {
                    mMemoryState = IMemoryManager.MemoryAction.NORMAL;
                    mIApp.getAppUi().hideSavingDialog();
                    mIApp.getAppUi().applyAllUIVisibility(View.VISIBLE);
                }
            }
            LogHelper.d(TAG, "[onFileSaved] uri = " + uri + ", mCapturingNumber = "
                    + mCapturingNumber);
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
        AnimationData data = prepareAnimationData(mPreviewData, mPreviewWidth,
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
        animationData.mOrientation = mPhotoModeHelper.getCameraInfoOrientation(mCameraId);
        animationData.mIsMirror = mPhotoModeHelper.isMirror(mCameraId);
        return animationData;
    }

    private void updatePictureSizeAndPreviewSize(Size previewSize) {
        ISettingManager.SettingController controller = mISettingManager.getSettingController();
        String size = controller.queryValue(KEY_PICTURE_SIZE);
        if (size != null && mIsResumed) {
            String[] pictureSizes = size.split("x");
            mCaptureWidth = Integer.parseInt(pictureSizes[0]);
            mCaptureHeight = Integer.parseInt(pictureSizes[1]);
            mIDeviceController.setPictureSize(new Size(mCaptureWidth, mCaptureHeight));
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

    private void initStatusMonitor() {
        StatusMonitor statusMonitor = mICameraContext.getStatusMonitor(mCameraId);
        mPhotoStatusResponder = statusMonitor.getStatusResponder(KEY_PHOTO_CAPTURE);
    }


    private void saveData(byte[] jpegData) {
        if (jpegData != null) {
            //check memory to decide whether it can take next picture.
            //if not, show saving
            ISettingManager.SettingController controller = mISettingManager.getSettingController();
            String dngState = controller.queryValue(KEY_DNG);
            
            //xiao add for draw bitmap 
            if (mIApp.isEarMirror() || mIApp.isEyeMirror()) {
            	jpegData = getEarPhoto(jpegData);
		    }else if (mIApp.isSkinMirror()) {
			    jpegData = getSkinPhoto(jpegData);
		    }else if (mIApp.isCommonMirror()) {
                jpegData = getCommmonPhoto(jpegData);
		    }
            
            long saveDataSize = jpegData.length;
            if (dngState != null && "on".equalsIgnoreCase(dngState)) {
                saveDataSize = saveDataSize + DNG_IMAGE_SIZE;
            }
            synchronized (mCaptureNumberSync) {
                mCapturingNumber ++;
                mMemoryManager.checkOneShotMemoryAction(saveDataSize);
            }
            String fileDirectory = mICameraContext.getStorageService().getFileDirectory();
            Size exifSize = CameraUtil.getSizeFromExif(jpegData);
            
            //xiao add for picture name & bitmap
            String filename = getPicturename();
            
            ContentValues contentValues = mPhotoModeHelper.createContentValues(jpegData,
                    fileDirectory, exifSize.getWidth(), exifSize.getHeight(),filename);
                    
                    
            mICameraContext.getMediaSaver().addSaveRequest(jpegData, contentValues, null,
                    mMediaSaverListener);
            //reset the switch camera to null
            synchronized (mPreviewDataSync) {
                mPreviewData = null;
            }
        }
    }

    private void updateThumbnail(byte[] jpegData) {
        Bitmap bitmap = BitmapCreator.createBitmapFromJpeg(jpegData, mIApp.getAppUi()
                .getThumbnailViewWidth());
        //mIApp.getAppUi().updateThumbnail(bitmap);
    }

    @Override
    public void onMemoryStateChanged(IMemoryManager.MemoryAction state) {
        if (state == IMemoryManager.MemoryAction.STOP && mCapturingNumber != 0) {
            //show saving
            LogHelper.d(TAG, "memory low, show saving");
            mIApp.getAppUi().showSavingDialog(null, true);
            mIApp.getAppUi().applyAllUIVisibility(View.INVISIBLE);
        }
    }

    /**
     * surface changed listener.
     */
    private class SurfaceChangeListener implements ISurfaceStatusListener {

        @Override
        public void surfaceAvailable(SurfaceHolder surfaceHolder, int width, int height) {
            LogHelper.d(TAG, "surfaceAvailable,device controller = " + mIDeviceController
                    + ",w = " + width + ",h = " + height);
            if (mIDeviceController != null && mIsResumed) {
                mIDeviceController.updatePreviewSurface(surfaceHolder);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int width, int height) {
            LogHelper.d(TAG, "surfaceChanged, device controller = " + mIDeviceController
                    + ",w = " + width + ",h = " + height);
            if (mIDeviceController != null && mIsResumed) {
                mIDeviceController.updatePreviewSurface(surfaceHolder);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            LogHelper.d(TAG, "surfaceDestroyed,device controller = " + mIDeviceController);
        }
    }

    /**
     * Status change listener implement.
     */
    private class MyStatusChangeListener implements StatusChangeListener {
        @Override
        public void onStatusChanged(String key, String value) {
            LogHelper.d(TAG, "[onStatusChanged] key = " + key + ",value = " + value);
            if (KEY_PICTURE_SIZE.equalsIgnoreCase(key)) {
                String[] sizes = value.split("x");
                mCaptureWidth = Integer.parseInt(sizes[0]);
                mCaptureHeight = Integer.parseInt(sizes[1]);
                mIDeviceController.setPictureSize(new Size(mCaptureWidth, mCaptureHeight));
                Size previewSize = mIDeviceController.getPreviewSize((double) mCaptureWidth /
                        mCaptureHeight);
                int width = previewSize.getWidth();
                int height = previewSize.getHeight();
                if (width != mPreviewWidth || height != mPreviewHeight) {
                    onPreviewSizeChanged(width, height);
                }
            } else if (KEY_MATRIX_DISPLAY_SHOW.equals(key)) {
                mIsMatrixDisplayShow = "true".equals(value);
            }
        }
    }
    
//--------------------------------------------------------------xiao add for filename & bitmap start---------------------------------------------------------------

    private String getPicturename(){
        return mIApp.getCurrentPictureName(); 
    }
    
	private Bitmap Bytes2Bimap(byte[] b) {
		android.util.Log.i("camera_rcs", "b :: " + b);
		if (b.length != 0) {
			return BitmapFactory.decodeByteArray(b, 0, b.length);
		} else {
			return null;
		}
	}
	
    private byte[] Bitmap2Bytes(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		
		return baos.toByteArray();
	}
	
		/* 
	 * RCS100 byte 转换成Bitmap(获取照片的Bitmap)
	 */
    private byte[] getEarPhoto(byte[] photoData){
    	 String patientInfoString = mIApp.getPatientInfo();
    	 
    	 mPhotoBitmap = Bytes2Bimap(photoData);
    	 
    	 Bitmap alterBitmap=Bitmap.createBitmap(mPhotoBitmap.getWidth(), mPhotoBitmap.getHeight(), mPhotoBitmap.getConfig());	
         Canvas canvas = new Canvas(alterBitmap);
         Matrix m1Matrix = new Matrix();
         m1Matrix.setTranslate(-418, -6);
         canvas.drawBitmap(mPhotoBitmap,m1Matrix, null);
         
         Bitmap photo_bm = mIApp.getBitmap();//BitmapFactory.decodeResource(mIApp.getActivity().getResources(),R.drawable.photo_bm);
         android.util.Log.d("xiao111","patientinfo ==222" + patientInfoString);
         
         Matrix mMatrix = new Matrix();
         canvas.drawBitmap(photo_bm,mMatrix, null);
         Bitmap mPatientBm  =  drawTextToBitmap(alterBitmap,patientInfoString); 
 		 photoData = Bitmap2Bytes(mPatientBm);
         mPhotoBitmap.recycle();
         alterBitmap.recycle();
         photo_bm.recycle();
         mPatientBm.recycle();//防止内存溢出
 		 return photoData;
    }
    
    private byte[] getSkinPhoto(byte[] photoData){
         String patientInfoString = mIApp.getPatientInfo();
    	 mPhotoBitmap = Bytes2Bimap(photoData);
    	 Bitmap alterBitmap=Bitmap.createBitmap(mPhotoBitmap.getWidth(), mPhotoBitmap.getHeight(), mPhotoBitmap.getConfig());	
         Canvas canvas = new Canvas(alterBitmap);
         Matrix m1Matrix = new Matrix();
         m1Matrix.setTranslate(-418, -6);
         canvas.drawBitmap(mPhotoBitmap,m1Matrix, null);
         
         //xiao add for ruler on photo start 
         String multipleValue = mIApp.getvalues();
        
         float multiplevalues = Float.parseFloat(multipleValue);
         Log.d("xiao333","multiplevalues == " + multiplevalues);
         mRulerDistance = 22.45f *multiplevalues;
         Log.d("xiao333","mRulerDistance == " + mRulerDistance);
		 rulerCalculator = new RulerCalculator((int)(Radius), (int)(Radius),100, (int)(mRulerDistance * 3.4), 25);
		 
		 
		 mLeftPoint = new PointF();
		 finalright = new PointF();
		 
         mLeftPoint.x = (mIApp.getCurrentLeftX()-275)*multiplewidth;
         mLeftPoint.y = (mIApp.getCurrentLeftY()*multipleheight);
         
         float k = mIApp.getValueK();
         float b = 0;
         float minus = 0;
         
         b = mLeftPoint.y - k * mLeftPoint.x;
         
         float pointmaxy = mLeftPoint.y; 
         float pointmaxx = mLeftPoint.x + 2448;
         
         if(k != 0){
            pointmaxx = 2448;
            pointmaxy = pointmaxx * k + b;
         }
         
         rulerPaint.setColor(Color.BLACK);  
         rulerPaint.setStrokeWidth(10);
         rulerPaint.setStyle(Paint.Style.STROKE);
         
         finalright.x = pointmaxx;
         finalright.y = pointmaxy;
         
         drawRuler(canvas,mLeftPoint,finalright);
         //xiao add for ruler on photo end
         
         //////////////////////////////////////////////////////////////////////////////////////
         Bitmap photo_bm = mIApp.getBitmap();
         Matrix mMatrix = new Matrix();
         canvas.drawBitmap(photo_bm,mMatrix, null);
         Bitmap mPatientBm  =  drawTextToBitmap(alterBitmap,patientInfoString); 
         //rule_bm.recycle();
         mPhotoBitmap.recycle();
 		 photoData = Bitmap2Bytes(mPatientBm);
 		 alterBitmap.recycle();
 		 mPatientBm.recycle();
    
        return photoData;
    }


    private byte[] getCommmonPhoto(byte[] photoData){
        String patientInfoString = mIApp.getPatientInfo();
        mPhotoBitmap = Bytes2Bimap(photoData);
        Bitmap alterBitmap=Bitmap.createBitmap(mPhotoBitmap.getWidth(), mPhotoBitmap.getHeight(), mPhotoBitmap.getConfig());
        Canvas canvas = new Canvas(alterBitmap);
        Matrix m1Matrix = new Matrix();
        canvas.drawBitmap(mPhotoBitmap,m1Matrix, null);
        Bitmap mPatientBm  =  drawCommonTextToBitmap(alterBitmap,patientInfoString);
        photoData = Bitmap2Bytes(mPatientBm);
        mPhotoBitmap.recycle();
        mPatientBm.recycle();//防止内存溢出
        return photoData;
    }
    
    
    public Bitmap drawTextToBitmap(Bitmap mbitmap, String gText) {
        Canvas canvas = new Canvas(mbitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize((int) (95));
        paint.setDither(true); //获取跟清晰的图像采样
        //对位图进行滤波处理，如果该项设置为true，则图像在动画进行中会滤掉对Bitmap图像的优化操作，加快显示
        paint.setFilterBitmap(true);
        Rect bounds = new Rect();
        if (!TextUtils.isEmpty(gText) && gText!=null) {
            info = gText.split(";");
	        int count = info.length;
	        int maxlength = 0;
	        int x = 100;
	        int y = 100;
	        int textsize = 180;
	        for(int i=0;i<count;i++){
	            paint.getTextBounds(info[i],0,info[i].length(),bounds);
	            //canvas.drawText(info[i],x * 17 + 200 ,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
	            switch(i){
	                case 0:
	                maxlength = (int)paint.measureText(info[0]);
	                canvas.drawText(info[0],x * 17 + 60,y + textsize*i + 10,paint);//x - 左 ,y - 上
	                break;
	                case 1: 
	                int length = (int)paint.measureText(info[1]);
	                int muselength = maxlength - length ;
	                canvas.drawText(info[1],x * 17 + 60 + muselength ,y + textsize*i + 10,paint);//x - 左 ,y - 上
	                break;
	                case 2:
	                int length1 = (int)paint.measureText(info[2]);
	                int muselength1 = maxlength - length1 ;
	                 canvas.drawText(info[2],x * 17 + 60 + muselength1,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
	                break;
	                case 3:
	                int length2 = (int)paint.measureText(info[3]);
	                int muselength2 = maxlength - length2 ;
	                 canvas.drawText(info[3],x * 17 + 60 + muselength2,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
	                break;
	                case 4:
	                int length3 = (int)paint.measureText(info[4]);
	                int muselength3 = maxlength - length3 ;
	                 canvas.drawText(info[4],x * 17 + 60 + muselength3,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
	                break;
	                case 5:
	                int length4 = (int)paint.measureText(info[5]);
	                int muselength4 = maxlength - length4 ;
                    canvas.drawText(info[5],x * 17 + 60 + muselength4,y + textsize*i + 10,paint);//x - 左 ,y - 上
	                break;
	            
	            }
	        }
        }
        return mbitmap;
    }

    public Bitmap drawCommonTextToBitmap(Bitmap mbitmap, String gText) {
        Canvas canvas = new Canvas(mbitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        float[] direction = new float[]{ 1, 1, 1 };
        //设置环境光亮度
        float light = 0.4f;
        // 选择要应用的反射等级
        float specular = 6;
        // 向mask应用一定级别的模糊
        float blur = 3.5f;
        EmbossMaskFilter emboss=new EmbossMaskFilter(direction,light,specular,blur);
        paint.setMaskFilter(emboss);
        paint.setTextSize(100);
        paint.setDither(true); //获取跟清晰的图像采样
        //对位图进行滤波处理，如果该项设置为true，则图像在动画进行中会滤掉对Bitmap图像的优化操作，加快显示
        paint.setFilterBitmap(true);
        Rect bounds = new Rect();
        if (!TextUtils.isEmpty(gText) && gText!=null) {
            info = gText.split(";");
            int count = info.length;
            int maxlength = 0;
            int x = 100;
            int y = 100;
            int textsize = 180;
            for(int i=0;i<count;i++){
                paint.getTextBounds(info[i],0,info[i].length(),bounds);
                //canvas.drawText(info[i],x * 17 + 200 ,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
                switch(i){
                    case 0:
                        maxlength = (int)paint.measureText(info[0]);
                        canvas.drawText(info[0],x * 17 + 60,y + textsize*i + 10,paint);//x - 左 ,y - 上
                        break;
                    case 1:
                        int length = (int)paint.measureText(info[1]);
                        int muselength = maxlength - length ;
                        canvas.drawText(info[1],x * 17 + 60 + muselength ,y + textsize*i + 10,paint);//x - 左 ,y - 上
                        break;
                    case 2:
                        int length1 = (int)paint.measureText(info[2]);
                        int muselength1 = maxlength - length1 ;
                        canvas.drawText(info[2],x * 17 + 60 + muselength1,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
                        break;
                    case 3:
                        int length2 = (int)paint.measureText(info[3]);
                        int muselength2 = maxlength - length2 ;
                        canvas.drawText(info[3],x * 17 + 60 + muselength2,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
                        break;
                    case 4:
                        int length3 = (int)paint.measureText(info[4]);
                        int muselength3 = maxlength - length3 ;
                        canvas.drawText(info[4],x * 17 + 60 + muselength3,y + textsize*i + 10 ,paint);//x - 左 ,y - 上
                        break;
                    case 5:
                        int length4 = (int)paint.measureText(info[5]);
                        int muselength4 = maxlength - length4 ;
                        canvas.drawText(info[5],x * 17 + 60 + muselength4,y + textsize*i + 10,paint);//x - 左 ,y - 上
                        break;

                }
            }
        }
        return mbitmap;
    }
    
    private void drawRuler(final Canvas canvas,final PointF mLeftPoint,final PointF mRightPoint) {
    	Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (rulerCalculator != null) {
		            rulerCalculator.calculate(
		                    RulerCalculator.point2PointO(mRightPoint),
		                    RulerCalculator.point2PointO(mLeftPoint));
		            // 画尺子边框
		            List<PointF> rulerPoints = RulerCalculator
		                    .pointfs2Points(rulerCalculator.getRulerPoints());
		            if (rulerPoints != null && rulerPoints.size() == 4) {
		                // 画线顺序：上下左右
		                Path pathRuler2 = new Path();
		                pathRuler2.moveTo(rulerPoints.get(1).x , rulerPoints.get(1).y);
		                pathRuler2.lineTo(rulerPoints.get(3).x , rulerPoints.get(3).y);
		                canvas.drawPath(pathRuler2, rulerPaint);
		            }
		            // 画尺子刻度
		            List<PointF> bottomScalePoints = RulerCalculator
		                    .pointfs2Points(rulerCalculator.getRulerBottomScalePoints());
		            List<PointF> top1ScalePoints = RulerCalculator
		                    .pointfs2Points(rulerCalculator.getRulerTop1ScalePoints());
		            List<PointF> top2ScalePoints = RulerCalculator
		                    .pointfs2Points(rulerCalculator.getRulerTop2ScalePoints());
		            // 画尺子小刻度
		            if (bottomScalePoints != null && bottomScalePoints.size() > 0
		                    && top1ScalePoints != null && top1ScalePoints.size() > 0) {
		                for (int i = 0; i < bottomScalePoints.size(); i++) {
		                    try {
		                        Path path = new Path();
		                        path.moveTo(top1ScalePoints.get(i).x,
		                                top1ScalePoints.get(i).y);
		                        path.lineTo(bottomScalePoints.get(i).x,
		                                bottomScalePoints.get(i).y);
		                        canvas.drawPath(path, rulerPaint);
		                    } catch (Exception e) {
		                        e.printStackTrace();
		                    }
		                }
		            }
		            // 画尺子大刻度
		            if (bottomScalePoints != null && bottomScalePoints.size() > 0
		                    && top2ScalePoints != null && top2ScalePoints.size() > 0) {
		                for (int i = 0; i < top2ScalePoints.size(); i++) {
		                    int index = (i + 1) * 5 - 1;
		                    if (bottomScalePoints.size() > index) {
		                        Path path = new Path();
		                        path.moveTo(top2ScalePoints.get(i).x ,
		                                top2ScalePoints.get(i).y);
		                        path.lineTo(bottomScalePoints.get((i + 1) * 5 - 1).x ,
		                                bottomScalePoints.get((i + 1) * 5 - 1).y);
		                        canvas.drawPath(path, rulerPaint);
		                    }
		                }
		            }
		        }
			}
		};
		runnable.run();
    }
    
//--------------------------------------------------------------xiao add for filename & bitmap end---------------------------------------------------------------
}
