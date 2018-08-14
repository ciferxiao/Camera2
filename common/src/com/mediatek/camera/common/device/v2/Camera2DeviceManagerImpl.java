/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2016. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.common.device.v2;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.common.base.Preconditions;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory.CameraApi;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.CameraOpenException.ExceptionType;
import com.mediatek.camera.common.device.CameraStateCallback;
import com.mediatek.camera.common.utils.CameraUtil;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An implement of CameraDeviceMangager with android.hardware.camera2.
 */
public class Camera2DeviceManagerImpl extends CameraDeviceManager {
    private final CameraManager mCameraManager;
    // If don't keep the proxy, the proxy will be GC when system is low memory.
    ConcurrentHashMap<String, Camera2ProxyCreatorImpl> mProxyCreatorMap = new ConcurrentHashMap<>();
    private final Context mContext;

    /**
     * CameraDeviceManager implement for camera2.
     *
     * @param context
     *            the context used to initialize CameraManager.
     */
    public Camera2DeviceManagerImpl(Context context) {
        mContext = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void openCamera(@Nonnull String cameraId, @Nonnull CameraStateCallback callback,
                    @Nullable Handler handler) throws CameraOpenException {
        checkPreconditionsAndOpen(cameraId, callback, handler, false);
    }

    @Override
    public void openCameraSync(@Nonnull String cameraId, @Nonnull CameraStateCallback callback,
                    @Nullable Handler handler) throws CameraOpenException {
        checkPreconditionsAndOpen(cameraId, callback, handler, true);
    }

    @Override
    public void recycle(@Nonnull String cameraId) {
        //camera is closed by proxy.close(),so here don't need calling this.
        // need remove all the proxy ine map.
        mProxyCreatorMap.remove(cameraId);
    }

    private void checkPreconditionsAndOpen(String cameraId, CameraStateCallback callback,
                    Handler handler, boolean isSync) throws CameraOpenException {
        Preconditions.checkNotNull(cameraId, "open camera, the id must not null");
        Preconditions.checkNotNull(callback, "open camera, the state callback must not null");
        checkDevicePolicy();
        Camera2ProxyCreatorImpl proxyCreator = mProxyCreatorMap.get(cameraId);
        if (proxyCreator == null) {
            proxyCreator = new Camera2ProxyCreatorImpl(cameraId, handler);
            mProxyCreatorMap.put(cameraId, proxyCreator);
        }
        if (isSync) {
            proxyCreator.doOpenCameraSync(callback);
        } else {
            proxyCreator.doOpenCamera(callback);
        }
    }

    private void checkDevicePolicy() throws CameraOpenException {
        DevicePolicyManager dpm = (DevicePolicyManager) mContext
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.getCameraDisabled(null)) {
            throw new CameraOpenException(ExceptionType.SECURITY_EXCEPTION);
        }
    }

    /**
     * Camera2 proxy creator implement.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class Camera2ProxyCreatorImpl extends CameraProxyCreator {
        private final Tag mTag;
        private Camera2Handler mRequestHandler;
        private final Handler mRespondHandler;

        private Camera2Proxy mCamera2Proxy;
        private Camera2Proxy.StateCallback mDeviceStateCallback;
        private Handler mDeviceStateHandler;
        private final Object mOpenCameraSync = new Object();
        private CameraDevice mCameraDevice;

        Camera2ProxyCreatorImpl(String cameraId, Handler handler) {
            super(CameraApi.API2, cameraId);
            mTag = new Tag(Camera2ProxyCreatorImpl.class.getSimpleName() + "_" + cameraId);
            mRespondHandler = new Handler(mRespondThread.getLooper());
            // Because handler maybe null,so need use the respond handler to invoke device callback.
            if (handler == null) {
                mDeviceStateHandler = mRespondHandler;
            } else {
                mDeviceStateHandler = handler;
            }
        }

        private void doOpenCamera(CameraStateCallback callback) {
            LogHelper.i(mTag, "[doOpenCamera]");
            mDeviceStateCallback = (Camera2Proxy.StateCallback) callback;
            mDeviceStateHandler.post(mOpenCameraRunnable);
        }

        private void doOpenCameraSync(CameraStateCallback callback) {
            LogHelper.i(mTag, "[doOpenCameraSync]+");
            mDeviceStateCallback = (Camera2Proxy.StateCallback) callback;
            // need first check the open camera thread looper is not equals
            // the callback handler looper.
            if (mDeviceStateHandler.getLooper() == Looper.myLooper()) {
                throw new IllegalArgumentException("Cann't open camera sync with the same looper");
            }
            synchronized (mOpenCameraSync) {
                try {
                    mDeviceStateHandler.post(mOpenCameraRunnable);
                    mOpenCameraSync.wait();
                } catch (InterruptedException e) {
                    mOpenCameraSync.notify();
                }
            }
            LogHelper.i(mTag, "[doOpenCameraSync]-");
        }

        private final Runnable mOpenCameraRunnable = new Runnable() {
            @Override
            public void run() {
                if (mCamera2Proxy == null) {
                    try {
                        mCameraManager.openCamera(mCameraId, mStateCallback, mRespondHandler);
                    } catch (CameraAccessException e) {
                        dealOpenException(e);
                    }
                } else {
                    LogHelper.i(mTag, "[mOpenCameraRunnable] camera is opened,just notify");
                    mDeviceStateHandler.post(mCallOnOpened);
                }
            }
        };

        private void dealOpenException(final CameraAccessException e) {
            if (isNeedRetryOpen()) {
                retryOpenCamera();
                return;
            }
            mDeviceStateHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceStateCallback.onError(mCamera2Proxy, CameraUtil.CAMERA_OPEN_FAIL);
                }
            });
        }

        private boolean isNeedRetryOpen() {
            if (mRetryCount < OPEN_RETRY_COUNT) {
                mRetryCount++;
                return true;
            }
            return false;
        }

        private void retryOpenCamera() {
            try {
                Thread.sleep(RETRY_OPEN_SLEEP_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            doOpenCamera(mDeviceStateCallback);
        }

        private Camera2Handler.IDeviceInfoListener mDeviceInfoListener =
                new Camera2Handler.IDeviceInfoListener() {
                    @Override
                    public void onClosed() {
                        LogHelper.i(mTag, "[onClosed]");
                    }

                    @Override
                    public void onError() {
                        LogHelper.i(mTag, "[onError]");
                        mStateCallback.onError(mCameraDevice,
                                                   StateCallback.ERROR_CAMERA_IN_USE);
                    }
                };



        private final StateCallback mStateCallback = new StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                LogHelper.i(mTag, "onOpened,camera = " + camera);
                mRetryCount = 0;
                createHandlerAndProxy(camera);
                mCameraDevice = camera;
                // Notify current camera is opened done.
                mDeviceStateHandler.post(mCallOnOpened);
            }

            @Override
            public void onClosed(CameraDevice camera) {
                super.onClosed(camera);
                //Why need cameraDevcie == null case?
                //because if old camera onclosed before new camera onOpened case,so don't need care.
                if (mCameraDevice == null || mCameraDevice != camera) {
                    LogHelper.e(mTag, "[onClosed] but the closed camera is not same as before " +
                            "opened camera");
                    return;
                }
                LogHelper.d(mTag, "onClosed,camera = " + camera);
                mDeviceStateHandler.post(mCallOnClosed);
                destroyHandlerThreads();
            }

            @Override
            public void onError(CameraDevice camera, final int error) {
                LogHelper.e(mTag, "onError,camera = " + camera + ",error = " + error);
                // if camera in use and need retry open,first will sleep 1000ms.
                if (mCameraDevice == null && isNeedRetryOpen()) {
                    LogHelper.i(mTag, "In retry process don't notify error info. mRetryCount = "
                                            + mRetryCount);
                    return;
                }

                createHandlerAndProxy(camera);
                mDeviceStateHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceStateCallback.onError(mCamera2Proxy, error);
                        notifyOpenCameraSync();
                        mCameraDevice = null;
                    }
                });
                mProxyCreatorMap.clear();
                // release camera relative resource
                destroyHandlerThreads();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                LogHelper.d(mTag, "onDisconnected,camera = " + camera);
                createHandlerAndProxy(camera);
                mDeviceStateHandler.post(mCallOnDisconnected);
            }
        };

        private final Runnable mCallOnOpened = new Runnable() {
            @Override
            public void run() {
                LogHelper.d(mTag, "mCallOnOpened run, proxy = " + mCamera2Proxy);
                mDeviceStateCallback.onOpened(mCamera2Proxy);
                notifyOpenCameraSync();
            }
        };

        private final Runnable mCallOnClosed = new Runnable() {
            @Override
            public void run() {
                LogHelper.d(mTag, "mCallOnClosed run, proxy = " + mCamera2Proxy);
                mDeviceStateCallback.onClosed(mCamera2Proxy);
                mCameraDevice = null;
            }
        };

        private final Runnable mCallOnDisconnected = new Runnable() {
            @Override
            public void run() {
                LogHelper.d(mTag, "mCallOnDisconnected run, proxy = " + mCamera2Proxy);
                mDeviceStateCallback.onDisconnected(mCamera2Proxy);
                notifyOpenCameraSync();
                mCameraDevice = null;
            }
        };

        private void createHandlerAndProxy(CameraDevice camera) {
            // If camera changed need update the hander and proxy.
            if (camera == mCameraDevice) {
                return;
            }
            // Create the request handler when open done.
            mRequestHandler = new Camera2Handler(mCameraId, mRequestThread.getLooper(),
                            mRespondHandler, camera, mDeviceInfoListener);
            mCamera2Proxy = new Camera2Proxy(mCameraId, camera, mRequestHandler, mRespondHandler);
            mRequestHandler.updateCamera2Proxy(mCamera2Proxy);
        }

        private void notifyOpenCameraSync() {
            // should notify the open camera sync thread.
            synchronized (mOpenCameraSync) {
                mOpenCameraSync.notifyAll();
            }
        }
    }
}