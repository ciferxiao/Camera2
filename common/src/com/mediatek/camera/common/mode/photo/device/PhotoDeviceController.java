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

package com.mediatek.camera.common.mode.photo.device;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.common.base.Preconditions;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory.CameraApi;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.mode.photo.DeviceInfo;
import com.mediatek.camera.common.rcs.MedicalMirror;
import com.mediatek.camera.common.rcs.RcsMirrorInterface;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.ISettingManager.SettingDeviceRequester;
import com.mediatek.camera.common.utils.BitmapUtils;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;


//xiao add for adb connect start
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.Socket;
import java.net.ServerSocket;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.List;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import android.content.Context;
import android.widget.Toast;

import com.mediatek.camera.common.app.IApp;

//xiao add for adb connect end

/**
 * Photo device controller.
 */
public class PhotoDeviceController implements IDeviceController, SettingDeviceRequester {
    private static final Tag TAG = new Tag(PhotoDeviceController.class.getSimpleName());
    private static final int FIRST_PREVIEW_BLACK_ON = 1;
    private static final int FIRST_PREVIEW_BLACK_OFF = 0;
    private static final String KEY_FIRST_PREVIEW_FRAME = "first-preview-frame-black";
    // notify for Image before compress when taking capture
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_IMAGE_UNCOMPRESSED = 0x00000017;
    private volatile CameraState mCameraState = CameraState.CAMERA_UNKNOWN;


    private final Activity mActivity;
    private final CameraDeviceManager mCameraDeviceManager;
    private ICameraContext mICameraContext;
    private RcsMirrorInterface mRcsMirrorInterface;
    
    
    //xiao add for adb connected start
    private AvcEncoder avcEncoder;
    private int screenWidth = 960, screenHeight = 720;//xiao changed for 4:3
    int framerate = 25;
    int biterate = 3*screenWidth*screenHeight;
    private InetAddress address;
    private DatagramSocket datagramSocket;
    private NetSendTask netSendTask;
    byte[] h264;
    byte[] jpeg;
    public static ServerSocket serverSocket;
    public static boolean isUsbConnected = false;
    public static Socket socket;
    public static InputStream socketInputStream = null;
    public static OutputStream socketOuputStream = null;
    
    private boolean is_udp_send_type = false;
    private int previewCount = 0;
    private int sendCount = 0;
    private boolean isOnPause = false;
    private boolean isFirstinit = true ;
    private boolean isPicturesending = false; 
    
    private IApp mIApp;
    //xiao add for adb connected end

    //add wangchao for uvc
    private int inputWidth = 960;
    private int intputHeight = 720;
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
     * Controller camera device
     *
     * @param activity current activity.
     * @param context  current camera context.
     */
    PhotoDeviceController(@Nonnull Activity activity, @Nonnull ICameraContext context,IApp miapp) {
        HandlerThread handlerThread = new HandlerThread("DeviceController");
        handlerThread.start();
        mRequestHandler = new PhotoDeviceHandler(handlerThread.getLooper(), this);
        mICameraContext = context;
        mActivity = activity;
        
        mIApp = miapp;//xiao add for uvc
        
        mCameraDeviceManager = mICameraContext.getDeviceManager(CameraApi.API1);
        mRcsMirrorInterface =  new RcsMirrorInterface();
        
        avcEncoder = new AvcEncoder(screenWidth, screenHeight, framerate, biterate);
        
        //xiao add for uvc start 
        netSendTask = new NetSendTask();
        netSendTask.init();
        netSendTask.start();
        //xiao add for uvc end
        
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
    public void startPreview() {
        //add RCS 增加拍照暂停功能 20180726  liuxi start
        //mRequestHandler.sendEmptyMessage(PhotoDeviceAction.START_PREVIEW);
        mRequestHandler.sendEmptyMessageDelayed(PhotoDeviceAction.START_PREVIEW,mICameraContext.providerStartPreViewTime());
        //add RCS 增加拍照暂停功能 20180726 liuxi end
    }

    @Override
    public void stopPreview() {
        mRequestHandler.sendEmptyMessage(PhotoDeviceAction.STOP_PREVIEW);
        waitDone();
    }

    @Override
    public void takePicture(@Nonnull JpegCallback callback) {
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
    public void setPreviewSizeReadyCallback(PreviewSizeCallback callback) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.SET_PREVIEW_SIZE_READY_CALLBACK,
                callback).sendToTarget();
    }

    @Override
    public void setPictureSize(Size size) {
        mRequestHandler.obtainMessage(PhotoDeviceAction.SET_PICTURE_SIZE, size).sendToTarget();
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
    public void requestChangeSettingValueJustSelf(String key) {
        //if the handler has the key which don't execute, need remove this.
        mRequestHandler.removeMessages(PhotoDeviceAction
                .REQUEST_CHANGE_SETTING_VALUE_JUST_SELF, key);
        mRequestHandler.obtainMessage(PhotoDeviceAction.REQUEST_CHANGE_SETTING_VALUE_JUST_SELF,
                key).sendToTarget();
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
    //xiao add for uvc start
    @Override
    public void onpaused() {
        mRequestHandler.sendEmptyMessage(PhotoDeviceAction.MSG_RCS_UVC_PAUSE);
    }
    
    @Override
    public void onresume() {
        mRequestHandler.sendEmptyMessage(PhotoDeviceAction.MSG_RCS_UVC_RESUME);
    }
    
    //xiao add for uvc end
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

    //add RCS 曝光功能增加 liuxi 20180725 start
    @Override
    public void requestChangeExposureValue(int value){
        LogHelper.e(TAG," value = " + value );
        mRequestHandler.removeMessages(PhotoDeviceAction.MSG_EXPOSURE_SETTING_VALUES);
        mRequestHandler.obtainMessage(PhotoDeviceAction.MSG_EXPOSURE_SETTING_VALUES, value).sendToTarget();
    }
    //add RCS 曝光功能增加 liuxi 20180725 end

    //add RCS 30秒情况下,点击屏幕取消拍照暂停 liuxi 20180726 start
    @Override
    public void removeStartPreviewHandler() {
        Log.i("photo_ddddddddd"," " );
        mRequestHandler.removeMessages(PhotoDeviceAction.START_PREVIEW);
        mRequestHandler.sendEmptyMessage(PhotoDeviceAction.START_PREVIEW);
    }
    //add RCS 30秒情况下,点击屏幕取消拍照暂停 liuxi 20180726 end

    /**
     * Use for handler device control command.
     */
    private class PhotoDeviceHandler extends Handler {
        private static final String KEY_DISP_ROT_SUPPORTED = "disp-rot-supported";
        private static final String KEY_PICTURE_SIZE = "key_picture_size";
        private static final String KEY_CONTINUOUS_SHOT = "key_continuous_shot";
        private static final String KEY_ZSD = "key_zsd";
        private static final String FALSE = "false";
        private static final String VALUE_ON = "on";
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
        private JpegCallback mJpegReceivedCallback;
        private int mJpegRotation = 0;
        private volatile int mPreviewWidth;
        private volatile int mPreviewHeight;
        private PreviewSizeCallback mCameraOpenedCallback;
        private SurfaceHolder mSurfaceHolder;
        private SettingDeviceRequester mSettingDeviceRequester;
        private boolean mNeedSubSectionInitSetting = false;
        private boolean mNeedQuitHandler = false;

        //for post view update thumbnail
        private int mPostViewCallbackNumber = 0;

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
                    Log.i("succcccc", "CLOSE_CAMERA 1222222222222222 "  );
                    doCloseCamera((Integer) msg.obj == 1 ? true : false);

                    break;
                case PhotoDeviceAction.START_PREVIEW:
                    doStartPreview();
                    break;
                case PhotoDeviceAction.STOP_PREVIEW:
                    doStopPreview();
                    break;
                case PhotoDeviceAction.TAKE_PICTURE:
                    doTakePicture((JpegCallback) msg.obj);
                    break;
                case PhotoDeviceAction.REQUEST_CHANGE_SETTING_VALUE:
                    String key = (String) msg.obj;
                    restoreStateForCShot(key);
                    if (mCameraProxy == null || mCameraState == CameraState.CAMERA_UNKNOWN) {
                        LogHelper.e(TAG, "camera is closed or in opening state, can't request " +
                                "change setting value,key = " + key);
                        return;
                    }
                    doRequestChangeSettingValue(key);
                    break;
                case PhotoDeviceAction.REQUEST_CHANGE_SETTING_VALUE_JUST_SELF:
                    String selfKey = (String) msg.obj;
                    if (mCameraProxy == null || mCameraState == CameraState.CAMERA_UNKNOWN) {
                        LogHelper.e(TAG, "camera is closed or in opening state, can't request " +
                                "change self setting value,key = " + selfKey);
                        return;
                    }
                    doRequestChangeSettingSelf(selfKey);
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
                case PhotoDeviceAction.SET_PREVIEW_CALLBACK:
                    mModeDeviceCallback = (DeviceCallback) msg.obj;
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
                //add RCS 曝光功能增加 liuxi 20180725 start
                case PhotoDeviceAction.MSG_EXPOSURE_SETTING_VALUES:
                    int value = (int) msg.obj;
                    if (mCameraProxy == null || mCameraState == CameraState.CAMERA_UNKNOWN) {
                        LogHelper.e(TAG, "camera is closed or in opening state, can't request " +
                                "change setting value,key = " + value);
                        return;
                    }
                    doRequestChangeExposureValue(value);
                 //add RCS 曝光功能增加 liuxi 20180725 end
                    break;
                //xiao add for uvc start    
                case PhotoDeviceAction.MSG_RCS_UVC_PAUSE:
                    Log.d("xiao222","photodevicecontroller uvc onpaused");
                    savePackageFile("photodevicecontroller uvc onpaused");
                    //h264 = null;
                    if(mCameraProxy != null ){
                        mCameraProxy.setPreviewCallback(null); // xiao add for RCS UVC close camera bug
                    }
                    
                    isOnPause = true;
                    try {
                        if (socket != null) {
                            socket.close();
                            socket = null;
                            if (socketInputStream != null){
                                socketInputStream.close();
                            }

                            if (socketOuputStream != null) {
                                socketOuputStream.flush();
                                socketOuputStream.close();
                            }
                        }
                        if (serverSocket != null) {
                            serverSocket.close();
                            serverSocket = null;
                        }
                        //h264 = null;//xiao test
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                //xiao add for uvc end
                case PhotoDeviceAction.MSG_RCS_UVC_RESUME:
                    isOnPause = false;
                    netSendTask.start();
                    savePackageFile("resume isonpause == " + isOnPause);
                    break;
                default:
                    LogHelper.e(TAG, "[handleMessage] the message don't defined in " +
                            "photodeviceaction, need check");
                    break;
            }
        }

        //add RCS 曝光功能增加 liuxi 20180725 start
        private void doRequestChangeExposureValue(int value) {
            LogHelper.e(TAG,"[doRequestChangeSettingValue] 11111111  value = " + value + ",mPreviewWidth = " +
                    mPreviewWidth + ",mPreviewHeight = " + mPreviewHeight);
            if (mPreviewWidth == 0 || mPreviewHeight == 0) {
                return;
            }
            if (mCameraState == CameraState.CAMERA_OPENED && mCameraProxy != null) {
                Camera.Parameters parameters = mCameraProxy.getOriginalParameters(true);
                parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
                LogHelper.e(TAG,"[doRequestChangeSettingValue] 2222222222 value = " + value );
                parameters.setExposureCompensation(value);
                LogHelper.e(TAG,"[doRequestChangeSettingValue]mICameraContext.isEarMirror() = " + mICameraContext.isEarMirror());
                // add  RCS  Preview放大1.7倍 liuxi 20180725  start
                if (mICameraContext.isEarMirror()){
                    parameters.setZoom(5);
                }
                // add  RCS  Preview放大1.7倍 liuxi 20180725  end
                mCameraProxy.setParameters(parameters);
            }
        }
        //add RCS 曝光功能增加 liuxi 20180725 end

        //add RCS xiaojinggong UVC关闭 20180726 start
        private void socketRelase(){
            
        }
        //add RCS xiaojinggong UVC关闭 20180726 end

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
            resetPostViewNumber();
            try {
                //when open the camera need reset the mCameraProxy to null
                if (sync) {
                    mCameraDeviceManager.openCameraSync(mCameraId, mCameraProxyStateCallback, null);
                } else {
                    mCameraDeviceManager.openCamera(mCameraId, mCameraProxyStateCallback, null);
                }
            }  catch (CameraOpenException e) {
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
                mCameraId = null;
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
                if (mNeedQuitHandler) {
                    mRequestHandler.sendEmptyMessage(PhotoDeviceAction.DESTROY_DEVICE_CONTROLLER);
                }
                mIsInCapturing = false;
                resetPostViewNumber();
            }
        }

        private void doStartPreview() {
            if (isCameraAvailable()) {
                Log.d("xiao333"," take picture 44444");
                //set preview callback before start preview.
                mCameraProxy.setOneShotPreviewCallback(mFrameworkPreviewCallback);
                // Start preview.
                mCameraProxy.startPreview();
                mCameraProxy.setVendorDataCallback(MTK_CAMERA_MSG_EXT_NOTIFY_IMAGE_UNCOMPRESSED,
                        mUncompressedImageCallback);
            }
        }

        private void doStopPreview() {
            Log.d("xiao333"," take picture 11111");//没走
        
            checkIsCapturing();
            if (isCameraAvailable()) {
                mSettingDeviceConfigurator.onPreviewStopped();
                if (mModeDeviceCallback != null) {
                    mModeDeviceCallback.afterStopPreview();
                }
                mIsPreviewStarted = false;
            }
            if (mNeedQuitHandler) {
                mRequestHandler.sendEmptyMessage(PhotoDeviceAction.DESTROY_DEVICE_CONTROLLER);
            }
            resetPostViewNumber();
        }

        private void doTakePicture(JpegCallback callback) {
            Log.d("xiao333"," take picture 22222");
            LogHelper.d(TAG, "[doTakePicture] mCameraProxy = " + mCameraProxy);
            if (mCameraProxy == null) {
                return;
            }
            synchronized (mCaptureSync) {
                mIsInCapturing = true;
            }
            mCaptureStartTime = System.currentTimeMillis();
            mJpegReceivedCallback = callback;
            Log.i("lx_ssss"," mJpegRotation : " + mJpegRotation);
            setCaptureParameters(mJpegRotation);
            mSettingDeviceConfigurator.onPreviewStopped();
            mIsPreviewStarted = false;
            //mCameraProxy.takePicture(mShutterCallback, mRawCallback, mPostViewCallback,
                   /// mJpegCallback);//M:拍照方法 liuxi 20180718
            mCameraProxy.takePicture(null,null,null,mJpegCallback);
            
            
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
                if (needRestartPreview) {
                    doStopPreview();
                    mCameraProxy.setParameters(parameters);
                    doStartPreview();
                } else {
                    mCameraProxy.setParameters(parameters);
                }
            }
        }

        private void doRequestChangeSettingSelf(String key) {
            LogHelper.i(TAG, "[doRequestChangeSettingSelf] key = " + key + ",mPreviewWidth = " +
                    mPreviewWidth + ",mPreviewHeight = " + mPreviewHeight);
            if (mPreviewWidth == 0 || mPreviewHeight == 0) {
                return;
            }
            if (mCameraState == CameraState.CAMERA_OPENED && mCameraProxy != null) {
                Camera.Parameters parameters = mCameraProxy.getParameters();
                boolean restart = mSettingDeviceConfigurator.configParametersByKey(parameters, key);
                if (restart) {
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

        private void doSetPictureSize(Size size) {
            //do nothing.
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

            return values;
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

        /**
         * During capturing (mIsInCapturing is true) if start CShot will lead photo device
         * can't receive jpeg call back and cause ANR.
         * @param key
         */
        private void restoreStateForCShot(String key) {
            if (KEY_CONTINUOUS_SHOT.equals(key)) {
                synchronized (mCaptureSync) {
                    if (mIsInCapturing) {
                        mIsInCapturing = false;
                        mCaptureSync.notify();
                    }
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
                        mCaptureSync.wait();
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
                //xiao add for uvc bytes
                android.util.Log.d("xiao_frame", "photodevicecontroller bytes ==22222222 " );
                mCameraProxy.setPreviewCallback(mUVCPreviewCallback);
                
            }
        };
        
        private final Camera.PreviewCallback mUVCPreviewCallback = new Camera
                .PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                //将原始数据流转化成bitmap
                //byte[] uvcShowData = getUVCShowData(bytes);
                if(isUsbConnected){
                    //byte[] bytebyMirror = getBackBytebyMirror(uvcShowData, bytes);
                    Uvcconnect(bytes);
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
                LogHelper.d(TAG, "[onDataTaken] message = " + message.what);
            }

            @Override
            public void onDataCallback(int msgId, byte[] data, int arg1, int arg2) {
                LogHelper.d(TAG, "[UncompressedImageCallback] onDataCallback " + data);
                //if current is in capturing, also need notify the capture sync.
                //because jpeg will be callback next time.
                if (mJpegReceivedCallback != null) {
                    //android.util.Log.d("xiao111","data == " + data);
                
                    DataCallbackInfo info = new DataCallbackInfo();
                    info.data = data;
                    info.needUpdateThumbnail = false;
                    info.needRestartPreview = false;
                    mJpegReceivedCallback.onDataReceived(info);
                }
                //notify preview is ready
                if (mFrameworkPreviewCallback != null) {
                    mFrameworkPreviewCallback.onPreviewFrame(null, null);//xiao never used
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
            }
        };

        private final Camera.PictureCallback mPostViewCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                long postViewTime = System.currentTimeMillis() - mCaptureStartTime;
                LogHelper.d(TAG, "[mPostViewCallback],spend time : " + postViewTime + "ms," +
                        "data : " + bytes + ",mPostViewCallbackNumber = " +
                        mPostViewCallbackNumber);
                if (bytes != null) {
                    mPostViewCallbackNumber++;
                    if (mJpegReceivedCallback != null) {
                        mJpegReceivedCallback.onPostViewCallback(bytes);
                    }
                }
            }
        };

        private final Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                long jpegTime = System.currentTimeMillis() - mCaptureStartTime;
                LogHelper.d(TAG, "[mJpegCallback],spend time :" + jpegTime + "ms" + "," +
                        "mPostViewCallbackNumber = " + mPostViewCallbackNumber);
                notifyCaptureDone(bytes, mPostViewCallbackNumber == 0, true);
                if (mPostViewCallbackNumber > 0) {
                    mPostViewCallbackNumber--;
                }
            }
        };

        private void setCaptureParameters(int sensorOrientation) {
            //M:RCS 检眼镜和检耳镜下照片旋转的问题 liuxi 20180716 start
            //int rotation = CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(mCameraId),
                    //sensorOrientation, mActivity);
            int rotation = 0;
            if (mRcsMirrorInterface.getMirrorState()== MedicalMirror.MirrorId.STATE_MIRROR_EYE
                    || mRcsMirrorInterface.getMirrorState()== MedicalMirror.MirrorId.STATE_MIRROR_EAR){
                 rotation = 180;
            }else{
                 rotation = 0;
            }
            //M:RCS 检眼镜和检耳镜下照片旋转的问题 liuxi 20180716 end
            if (mCameraProxy != null) {
                Camera.Parameters parameters = mCameraProxy.getParameters();
                mSettingDeviceConfigurator.configParameters(parameters);
                parameters.setRotation(rotation);
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
            ISettingManager.SettingController controller = mSettingManager.getSettingController();
            String pictureSize = controller.queryValue(KEY_PICTURE_SIZE);
            if (pictureSize != null) {
                String[] pictureSizes = pictureSize.split("x");
                int width = Integer.parseInt(pictureSizes[0]);
                int height = Integer.parseInt(pictureSizes[1]);
                double ratio = (double) width / height;
                getTargetPreviewSize(ratio);
            }
        }

        private void setSurfaceHolderParameters() {
            //set preview callback before start preview.
            if (mSurfaceHolder != null) {
                //onresume 后执行
                mCameraProxy.setOneShotPreviewCallback(mFrameworkPreviewCallback);
                //xiao add for RCS UVC
                //
            }
            // Set preview display.
            try {
                mCameraProxy.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                //if set preview exception, Can't do anything.
                throw new RuntimeException("set preview display exception");
            }
        }

        private void createSettingSecond(Camera.Parameters parameters) {
            mSettingManager.createSettingsByStage(2);
            mSettingDeviceConfigurator.setOriginalParameters(parameters);
            boolean needRestartPreview = mSettingDeviceConfigurator.configParameters(parameters);
            if (parameters != null) {
                parameters.set(KEY_FIRST_PREVIEW_FRAME, FIRST_PREVIEW_BLACK_OFF);
            }
            if (needRestartPreview) {
                mCameraProxy.stopPreview();
                mCameraProxy.setParameters(parameters);
                mCameraProxy.startPreview();
            } else {
                mCameraProxy.setParameters(parameters);
            }
        }

        private void doOnOpened(CameraProxy cameraProxy) {
            LogHelper.i(TAG, "[doOnOpened] cameraProxy = " + cameraProxy + mCameraState);
            if (CameraState.CAMERA_OPENING != mCameraState) {
                LogHelper.d(TAG, "[doOnOpened] state is error, don't need do on camera opened");
                return;
            }
            mCameraState = CameraState.CAMERA_OPENED;
            if (mModeDeviceCallback != null) {
                mModeDeviceCallback.onCameraOpened(mCameraId);
            }
            mICameraContext.getFeatureProvider().updateCameraParameters(mCameraId,
                    cameraProxy.getOriginalParameters(false));
            if (mNeedSubSectionInitSetting) {
                mSettingManager.createSettingsByStage(1);
            } else {
                mSettingManager.createAllSettings();
            }
            mSettingDeviceConfigurator.setOriginalParameters(
                    cameraProxy.getOriginalParameters(false));

            Camera.Parameters parameters = cameraProxy.getOriginalParameters(true);
            mPreviewFormat = parameters.getPreviewFormat();
            mSettingDeviceConfigurator.configParameters(parameters);
            if (mNeedSubSectionInitSetting && parameters != null) {
                parameters.set(KEY_FIRST_PREVIEW_FRAME, FIRST_PREVIEW_BLACK_ON);
            }
            updatePreviewSize();
            if (mCameraOpenedCallback != null) {
                mCameraOpenedCallback.onPreviewSizeReady(new Size(mPreviewWidth, mPreviewHeight));
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
            Log.d("xiao222", "6366666666666666");   
            
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
            LogHelper.d(TAG, "[hasDeviceStateCallback] value = " + value);
            return value;
        }


        private void notifyCaptureDone(byte[] data, boolean needUpdateThumbnail, boolean
                needRestartPreview) {
            captureDone();
            if (mJpegReceivedCallback != null) {
                DataCallbackInfo info = new DataCallbackInfo();
                info.data = data;
                info.needUpdateThumbnail = needUpdateThumbnail;
                info.needRestartPreview = needRestartPreview;
                mJpegReceivedCallback.onDataReceived(info);
                Log.d("xiao333"," take picture 3333");
                //xiao add for uvc take picture start
                if(isUsbConnected){
                    writeThread = new WriteThread(data);
                    writeThread.start();
                    //writeThread.getPictureByte(bytebyMirror);
                }
                
                //xiao add for uvc take picture end
            }
        }

        private void resetPostViewNumber() {
            mPostViewCallbackNumber = 0;
        }

        private boolean isZsdOn() {
            boolean isZsdOn = false;
            ISettingManager.SettingController controller = mSettingManager.getSettingController();
            String zsdValue = controller.queryValue(KEY_ZSD);
            if (VALUE_ON.equalsIgnoreCase(zsdValue)) {
                isZsdOn = true;
            }
            LogHelper.i(TAG, "[isZsdOn] : " + isZsdOn);
            return isZsdOn;
        }

        /**
         * Open camera device state callback, this callback is send to camera device manager
         * by open camera interface.
         */
        private class CameraDeviceProxyStateCallback extends CameraProxy.StateCallback {

            @Override
            public void onOpened(@Nonnull CameraProxy cameraProxy) {
                Log.i("succcccc","[onOpened]proxy = " + cameraProxy + " state = " + mCameraState);
                synchronized (mWaitCameraOpenDone) {
                    Log.i("succcccc","[onOpened]proxy22222222222 = " + cameraProxy + " state = " + mCameraState);
                    mCameraProxy = cameraProxy;
                    mWaitCameraOpenDone.notifyAll();
                    mRequestHandler.obtainMessage(PhotoDeviceAction.ON_CAMERA_OPENED, cameraProxy)
                            .sendToTarget();
                }
            }

            @Override
            public void onClosed(@Nonnull CameraProxy cameraProxy) {
                LogHelper.i(TAG, "[onClosed] current proxy : " + mCameraProxy + " closed proxy " +
                        "= " + cameraProxy);
                if (mCameraProxy != null && mCameraProxy == cameraProxy) {
                    synchronized (mWaitCameraOpenDone) {
                        mWaitCameraOpenDone.notifyAll();
                    }
                }
            }

            @Override
            public void onDisconnected(@Nonnull CameraProxy cameraProxy) {
                LogHelper.i(TAG, "[onDisconnected] current proxy : " + mCameraProxy + " closed " +
                        " proxy " + cameraProxy);
                if (mCameraProxy != null && mCameraProxy == cameraProxy) {
                    synchronized (mWaitCameraOpenDone) {
                        mWaitCameraOpenDone.notifyAll();
                        mRequestHandler.obtainMessage(PhotoDeviceAction.ON_CAMERA_DISCONNECTED,
                                cameraProxy).sendToTarget();
                    }
                }
            }

            @Override
            public void onError(@Nonnull CameraProxy cameraProxy, int error) {

                LogHelper.i(TAG, "[onError] current proxy : " + mCameraProxy + " closed " +
                        " proxy " + cameraProxy);
                //if current is in capturing, but close is wait capture done,
                //so this case need notify the capture done. otherwise will be ANR to pause.
                captureDone();
                if ((mCameraProxy != null && mCameraProxy == cameraProxy)
                        || error == CameraUtil.CAMERA_OPEN_FAIL) {
                    synchronized (mWaitCameraOpenDone) {
                        mWaitCameraOpenDone.notifyAll();
                        mCameraDeviceManager.recycle(mCameraId);
                        mRequestHandler.obtainMessage(PhotoDeviceAction.ON_CAMERA_ERROR, error, 0,
                                cameraProxy).sendToTarget();
                    }
                }
            }
        }
//--------------------------------------------------xiao add for UVC start ---------------------------------------------------------------------------

        private void Uvcconnect(byte[] data){
            h264 = new byte[960*720*3/2];
            // 摄像头数据转h264
            int ret = avcEncoder.offerEncoder(data, h264);
            if (ret > 0 && h264 != null) {//isPicturesending
               //发送h264
               savePackageFile("h264 ==" + h264 + "  ret ==" + ret);
               netSendTask.pushBuf(h264, ret);
            }
            
            if(isFirstinit){
                initPictureSocket();
            }
        }

    }


    //xiao add for RCS UVC 发送数据线程 start
class NetSendTask extends Thread {
    private ArrayList<ByteBuffer> mList;
    public Handler handler;
    public int packet_count = 0;

    public void init() {
        try {
            datagramSocket = new DatagramSocket();
            //设置IP
            address = InetAddress.getByName("192.168.1.101");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        mList = new ArrayList<ByteBuffer>();

    }

    //添加数据
    public void pushBuf(byte[] buf, int len) {
        if (is_udp_send_type) {
            // original H264 stream
            ByteBuffer buffer = ByteBuffer.allocate(len);
            buffer.put(buf, 0, len);
            mList.add(buffer);
        } else {
            // add length in front of H264 stream
            ByteBuffer buffer = ByteBuffer.allocate(len + 4);
            buffer.put((byte)((len >> 24) & 0xFF));
            buffer.put((byte)((len >> 16) & 0xFF));
            buffer.put((byte)((len >> 8) & 0xFF));
            buffer.put((byte)(len & 0xFF));
            buffer.put(buf, 0, len);
            mList.add(buffer);
        }
        sendCount++;
    }

    @Override
    public void run() {
        savePackageFile("NetSendTask start 111111");
        while (true) {
            if (isOnPause) {
                isUsbConnected = false;
                mIApp.setUvcConnectState(false);
                isFirstinit = true;
                return;
            }

            adbConnect();

            if (mList.size() <= 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            while (mList.size() > 0) {
                ByteBuffer sendBuf = mList.get(0);
                try {
                    if (!isUsbConnected) {
                        // adbSendData connect failed, ignore the data
                        mList.remove(0);
                        continue;
                    }

                    if (socket != null && !isPicturesending) {
                        socketOuputStream = socket.getOutputStream();
                        socketOuputStream.write(sendBuf.array(), 0, sendBuf.capacity());
                    }

                } catch (Throwable t) {
                    t.printStackTrace();
                    isUsbConnected = false;
                    mIApp.setUvcConnectState(false);
                    isFirstinit = true;
                }



                //移除已经发送的数据
                if(mList.size() > 0){
                    mList.remove(0);
                }
            }

        }
    }

    void adbConnect() {
        try {
            if (serverSocket == null) {
                serverSocket = new ServerSocket(7000);
            }
            if (!isUsbConnected) {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                
                socket = serverSocket.accept();
                socketInputStream = socket.getInputStream();
                socketOuputStream = socket.getOutputStream();

                byte[] buf = new byte[1024];
                int len = socketInputStream.read(buf);
                int i = 0;
                for (i = 0; i < len ; i++) {
                }

                if (buf[0] == 0x31 && buf[1] == 0x32 && buf[2] == 0x33)	{
                    
                    isUsbConnected = true;// uvc 判断链接
                    mIApp.setUvcConnectState(true);
                    
                    buf[0] = 0x34;
                    buf[1] = 0x35;
                    buf[2] = 0x36;
                    socketOuputStream.write(buf, 0, 3);
                }
            }

        } catch (IOException e) {
            isUsbConnected = false;
            isFirstinit = true;
        }
    }
  }
    //xiao add for RCS UVC 发送数据线程 end 
  
    //xiao add for checking take picture button click start 
    class ServerSocket_thread extends Thread{
        public Handler mhandler;
        private Message message;
        private String strings = "null";

        public void setHandler(Handler handler) {
            this.mhandler = handler;
            message = new Message();
        }

        @Override
        public void run() {
            while(isUsbConnected){
                if(isOnPause){
                    isUsbConnected =false;
                    mIApp.setUvcConnectState(false);
                    isFirstinit = true;
                    return;
                }
                try{
                    int len = -1;
                    byte[] buf = new byte[1024];
                    if(socketInputStream != null && socket != null){
                        len = socketInputStream.read(buf);
                    }
                    if(len > 0){
                    strings = new String(buf,0,len);
                    }
                    if(strings.equals("TakePic")){
                        message = new Message();
                        strings = "null";
                        message.what = 0x01;
                        mhandler.sendEmptyMessage(message.what);
                    }
                    if(isUsbConnected == false){
                        socketInputStream.close();
                        socketOuputStream.close();
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    //xiao add for checking take picture button click end
  
    //xiao add for UVC take picture start
    private WriteThread writeThread = null;

    Handler picHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if(msg.what == 0x01){
                boolean isCapture = true;
                //mCameraActivity.takeUVCpicture();
                mIApp.getAppUi().triggerShutterButtonClick(0);
            }
        }
    };
    //111111111111111111111111111111111111111111

    public void initPictureSocket(){
        ServerSocket_thread serversocket_thread = new ServerSocket_thread();
        serversocket_thread.start();
        serversocket_thread.setHandler(picHandler);
        isFirstinit = false ;
    }


    class WriteThread extends Thread {
        private byte[] picbyte;

        WriteThread(byte[] buf){
            this.picbyte = buf;
        }
        @Override
        public void run(){
            try {
                String xxx = "xiaotext" ;
                if (socket != null && picbyte != null) {
                isPicturesending = true ; 

                    int len = picbyte.length;
                    ByteBuffer buffer = ByteBuffer.allocate(len + 4);
                    buffer.put((byte)((len >> 24) & 0xFF));
                    buffer.put((byte)((len >> 16) & 0xFF));
                    buffer.put((byte)((len >> 8) & 0xFF));
                    buffer.put((byte)(len & 0xFF));
                    buffer.put(picbyte, 0, len);
                    socketOuputStream = socket.getOutputStream();//新建一个sos
                    socketOuputStream.write(buffer.array(), 0, buffer.capacity());//TODO 发送的照片数据流
                    isPicturesending  = false;
                    //socketOuputStream.write(picbyte);//TODO 发送的照片数据流
                    socketOuputStream.flush();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    //xiao add for UVC take picture end
  
  static String filename ="adblog";
  private void savePackageFile(String logstrings) {
        String msg = logstrings + " \n";
        FileOutputStream outputStream;
        try {
            outputStream = mIApp.getActivity().openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write(msg.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
  }  
//--------------------------------------------------xiao add for UVC end ---------------------------------------------------------------------------

//--------------------------------------------add wangchao for uvc to pc------------------------------------------------------------
    private byte[] getUVCShowData(byte[] data){
        Bitmap firstBitmap = BitmapUtils.yuvToBitmap(data,inputWidth,intputHeight);
        Bitmap SecondBitmap = mIApp.getMainPicBitmap();
        Bitmap bitmap = BitmapUtils.mergeBitmap(firstBitmap, SecondBitmap);
        return BitmapUtils.getNV21(inputWidth, intputHeight, bitmap);
    }
    /***
     * 获取镜头下的数据
     * @param merger
     * @param bytes
     * @return
     */
    private byte[] getBackBytebyMirror(byte[] merger, byte[] bytes) {
        if (mIApp.isCommonMirror() || mIApp.isUnMedicalMirror()){
            return bytes;
        }
        return merger;
    }
//--------------------------------------------add wangchao for uvc to pc------------------------------------------------------------


}
