/*
 * Copyright Statement:
 *
 *   This software/firmware and related documentation ("MediaTek Software") are
 *   protected under relevant copyright laws. The information contained herein is
 *   confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 *   the prior written permission of MediaTek inc. and/or its licensors, any
 *   reproduction, modification, use or disclosure of MediaTek Software, and
 *   information contained herein, in whole or in part, shall be strictly
 *   prohibited.
 *
 *   MediaTek Inc. (C) 2017. All rights reserved.
 *
 *   BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *   THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 *   RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 *   ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 *   WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 *   NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 *   RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *   INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 *   TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 *   RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 *   OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 *   SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 *   RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 *   STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 *   ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 *   RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 *   MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 *   CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *   The following software/firmware and/or related documentation ("MediaTek
 *   Software") have been modified by MediaTek Inc. All revisions are subject to
 *   any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.tests.helper;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service to re log from the logcat service.
 */
public class LoggerService {
    private static final String TAG = "CamHelper_" + LoggerService.class.getSimpleName();

    private static LoggerService sLoggerService;
    private ILoggerService mILoggerService;
    private ILoggerCallbackImpl mILoggerCallback;
    private boolean mLogServiceIsRunning;

    private Map<LoggerCallback, String[]> mLogCallbacks = new HashMap<>();

    /**
     * Interface to callback log status and content.
     */
    public interface LoggerCallback {
        /**
         * Callback when log catching is started.
         */
        void onLogStarted();

        /**
         * Callback when log catching is stopped.
         */
        void onLogStopped();

        /**
         * Callback when the file used to save log in logcat service is changed.
         *
         * @param filePath The changed log file path.
         */
        void onRecordingFileChanged(String filePath);

        /**
         * Callback when logcat service catching new log.
         *
         * @param log The catching log string.
         */
        void onLogReceived(String log);
    }

    private LoggerService() {
        if (mILoggerService == null) {
            try {
                Class c = Class.forName("android.os.ServiceManager");
                Method method = c.getMethod("getService", String.class);
                IBinder binder = (IBinder) method.invoke(null, "CameraTestHelper");
                mILoggerService = ILoggerService.Stub.asInterface(binder);
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "android.os.ServiceManager don't find");
            } catch (NoSuchMethodException e) {
                Log.d(TAG, "method getService(String) don't find" +
                        " in android.os.ServiceManager class");
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        mILoggerCallback = new LoggerService.ILoggerCallbackImpl();
    }

    /**
     * Get an static instance of {@link LoggerService}.
     *
     * @return An static instance of {@link LoggerService}.
     */
    public static LoggerService getInstance() {
        if (sLoggerService == null) {
            sLoggerService = new LoggerService();
        }
        return sLoggerService;
    }

    /**
     * Set log path to save catching log.
     *
     * @param logPath The path to save log.
     */
    public void setLogPath(String logPath) {
        try {
            mILoggerService.setLogPath(logPath);
        } catch (RemoteException e) {
            Log.d(TAG, "invoke setLogPath() failed");
        }
    }

    /**
     * Notify logcat service to start catch log. If the logcat service has started before
     * and don't stop currently, just create a new tag array that is combination of input
     * tag and before tags, and update the new tag array to logcat service for catching
     * more log.
     *
     * @param logTags The tag to filter log.
     * @param callback The instance of {@link LoggerCallback} to callback log.
     * @return True if it starts succeed.
     */
    public boolean startCatchingLog(String[] logTags, LoggerCallback callback) {
        Log.d(TAG, "[startCatchingLog], logTags:" + logTags + ", callback:" + callback);
        boolean succeed;
        mLogCallbacks.put(callback, logTags);

        List<String> tagsInList = new ArrayList<>();

        for (String[] logTagsInMap : mLogCallbacks.values()) {
            for (String logTag : logTagsInMap) {
                if (!tagsInList.contains(logTag)) {
                    tagsInList.add(logTag);
                }
            }
        }

        succeed = setLogTag(tagsInList.toArray(new String[tagsInList.size()]));
        if (!mLogServiceIsRunning) {
            succeed = startLog();
        }

        return succeed;
    }

    /**
     * Notify logcat service to stop catching the log filtered by input tag. If there is
     * no tag for catching log, it will notify logcat service to stop logcat process and
     * don't catch any log.
     *
     * @param logTags The tag to filter log.
     * @param callback The instance of {@link LoggerCallback} to callback log.
     * @return True if it stops succeed.
     */
    public boolean stopCatchingLog(String[] logTags, LoggerCallback callback) {
        Log.d(TAG, "[stopCatchingLog], logTags:" + logTags + ", callback:" + callback);
        boolean succeed;
        mLogCallbacks.remove(callback);

        if (mLogCallbacks.size() == 0) {
            succeed = stopLog();
        } else {
            List<String> tagsInList = new ArrayList<>();

            for (String[] logTagsInMap : mLogCallbacks.values()) {
                for (String logTag : logTagsInMap) {
                    if (!tagsInList.contains(logTag)) {
                        tagsInList.add(logTag);
                    }
                }
            }

            succeed = setLogTag(tagsInList.toArray(new String[tagsInList.size()]));
        }
        return succeed;
    }

    /**
     * Set log tag to filter log.
     *
     * @param tags The filter tag.
     * @return True if set succeed.
     */
    private boolean setLogTag(String[] tags) {
        try {
            mILoggerService.setLogTag(tags);
        } catch (RemoteException e) {
            Log.d(TAG, "invoke setLogReceiveCallback() failed");
            return false;
        }
        return true;
    }

    /**
     * Call logcat service to start logcat process to catch log.
     *
     * @return True if it starts success, otherwise, return false.
     */
    private boolean startLog() {
        try {
            mILoggerService.setLogCallback(mILoggerCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "invoke setLogReceiveCallback() failed");
            return false;
        }

        try {
            mLogServiceIsRunning = mILoggerService.startLog();
        } catch (RemoteException e) {
            Log.e(TAG, "invoke startLog() failed");
            return false;
        }
        return true;
    }

    /**
     * Call logcat service to stop logcat process to catch log.
     *
     * @return True if it stops success, otherwise, return false.
     */
    private boolean stopLog() {
        try {
            mILoggerService.stopLog();
            mLogServiceIsRunning = false;
        } catch (RemoteException e) {
            Log.e(TAG, "invoke stopLog() failed");
            return false;
        }

        try {
            mILoggerService.setLogCallback(null);
        } catch (RemoteException e) {
            Log.e(TAG, "invoke setLogReceiveCallback() failed");
            return false;
        }

        return false;
    }

    /**
     * Callback implements to receive log status from logcat service.
     */
    private class ILoggerCallbackImpl extends ILoggerCallback.Stub {

        @Override
        public void onLogStarted() {
            Log.d(TAG, "[onLogStarted]");
            Set<LoggerCallback> callbacks = mLogCallbacks.keySet();
            Iterator<LoggerCallback> iterator = callbacks.iterator();
            while (iterator.hasNext()) {
                LoggerCallback callback = iterator.next();
                callback.onLogStarted();
            }
        }

        @Override
        public void onLogStopped() {
            Log.d(TAG, "[onLogStopped]");
            Set<LoggerCallback> callbacks = mLogCallbacks.keySet();
            Iterator<LoggerCallback> iterator = callbacks.iterator();
            while (iterator.hasNext()) {
                LoggerCallback callback = iterator.next();
                callback.onLogStopped();
            }
        }

        @Override
        public void onRecordingFileChanged(final String filePath) {
            Log.d(TAG, "[onRecordingFileChanged]");
            Set<LoggerCallback> callbacks = mLogCallbacks.keySet();
            Iterator<LoggerCallback> iterator = callbacks.iterator();
            while (iterator.hasNext()) {
                LoggerCallback callback = iterator.next();
                callback.onRecordingFileChanged(filePath);
            }
        }

        @Override
        public void onLogReceived(final String log) {
            Set<LoggerCallback> callbacks = mLogCallbacks.keySet();
            Iterator<LoggerCallback> iterator = callbacks.iterator();
            while (iterator.hasNext()) {
                LoggerCallback callback = iterator.next();
                String[] logTags = mLogCallbacks.get(callback);
                for (String tag : logTags) {
                    if (log.contains(tag)) {
                        callback.onLogReceived(log);
                        break;
                    }
                }
            }
        }
    }
}
