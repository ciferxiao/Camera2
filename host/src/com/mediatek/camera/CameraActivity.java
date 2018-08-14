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

package com.mediatek.camera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener.OnThumbnailClickedListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.mode.IModeListener;
import com.mediatek.camera.common.mode.ModeManager;
import com.mediatek.camera.common.rcs.MedicalMirror;
import com.mediatek.camera.common.rcs.PreferencesUtils;
import com.mediatek.camera.common.rcs.RcsMirrorInterface;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.PriorityConcurrentSkipListMap;
import com.mediatek.camera.portability.WifiDisplayStatusEx;
import com.mediatek.camera.portability.pq.PictureQuality;
import com.mediatek.camera.rcs.UsbReceiver;
import com.mediatek.camera.rcs.ruler.RulerFrameLayout;
import com.mediatek.camera.rcs.ruler.RulerView;
import com.mediatek.camera.rcs.ui.ToastView;
import com.mediatek.camera.ui.CameraAppUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;



import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.format.Time;
import android.widget.TextView;

/**
 * Camera app's activity.
 * used to manager the app's life cycle, transfer system information
 * (such as key event, configuration change event ....).
 * Create app common UI and add to the activity view tree.
 */
public class CameraActivity extends PermissionActivity implements IApp {
    private static final Tag TAG = new Tag(CameraActivity.class.getSimpleName());
    private static final int MSG_CLEAR_SCREEN_ON_FLAG = 0;
    private static final int MSG_SET_SCREEN_ON_FLAG = 1;
    private static final int DELAY_MSG_SCREEN_SWITCH = 2 * 60 * 1000; // 2min
    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;
    private static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
    private static final String IS_CAMERA = "isCamera";

    private static final int MSG_RCS_MIRROR = 2;
    private static final int MSG_TAKE_VIDEO = 3;
    private static final int DELAY_MSG_MIRROR_TIMER = 1000;
    private RcsMirrorInterface mRcsMirrorInterface;
    private static final int KEY_MIRROR_INSERT_CODE = 305;
    private static final int KEY_MIRROR_PULL_OUT_CODE = 304;
    private static final int KEY_ROLLER_CLOCKWISE_CODE = 306;
    private static final int KEY_ROLLER_COUNTER_CLOCKWISE_CODE = 307;
    private RulerView mRulerView;
    private ImageView mSurfaceImg;
    private ImageView mRuleUnit;

    private RulerFrameLayout mRulerFrameLayout;
    private PreferencesUtils mPreferencesUtils;
    private int focusProgressValue;
    private int exposureProgressValue;
    private UsbReceiver mUsbReceiver;
    private boolean isCurrentVideo = true;
    private ICameraMode mICameraMode;
    private ToastView mToastView;

    private CameraAppUI mCameraAppUI;
    private PriorityConcurrentSkipListMap<String, KeyEventListener> mKeyEventListeners =
            new PriorityConcurrentSkipListMap<String, KeyEventListener>(true);

    private PriorityConcurrentSkipListMap<String, BackPressedListener> mBackPressedListeners =
            new PriorityConcurrentSkipListMap<String, BackPressedListener>(true);

    private IModeListener mIModeListener;
    private boolean mIsResumed;

    private final List<OnOrientationChangeListener>
            mOnOrientationListeners = new ArrayList<>();
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private OrientationEventListener mOrientationListener;
    protected Uri mUri;
    
    //xiao add for picture start
    private final static String helper = "预览主界面";
    private String RCSSettings;
    public static boolean misInstruction = false;
    private String patientresult ;
    private String mCurrentRecordName;
    private String mLastName;
    //相机返回患者信息的名字
    private String mCameraFristName;
    private String mCameraLastName;
    private String patientbirth;
    private String patientgender;
    private String shootingtime ;
    public int mCurrentOrgan;
    //xiao add for picture end
    
    private final String systemboottime = "2018-01-01";
    
    //xiao add for draw ruler 
    RulerMultiple ruler;
    private ScreenStatusReceiver mScreenStatusReceiver;


    protected OnThumbnailClickedListener mThumbnailClickedListener =
            new OnThumbnailClickedListener() {
                @Override
                public void onThumbnailClicked() {
                    goToGallery(mUri);
                }
            };

    @Override
    protected void onNewIntentTasks(Intent newIntent) {
        super.onNewIntentTasks(newIntent);
    }

    @Override
    protected void onCreateTasks(Bundle savedInstanceState) {
        if (!isThirdPartyIntent(this)) {
            CameraAppService.launchCamera(this);
        }
        
        //xiao add for 判断初始时间
        String nowtime = getNowTime();
        boolean isSametime = compareTwoTime(nowtime,systemboottime);
        Log.d("xiaotime","nowtime == " + nowtime);
        Log.d("xiaotime","systemboottime == " + systemboottime);
        Log.d("xiaotime","isSametime == " + isSametime);
        if(isSametime){
            Log.d("xiaotime","nowtime == " + nowtime);
            gotoSettingTime();
        }
        
        
        IPerformanceProfile profile = PerformanceTracker.create(TAG, "onCreate").start();
        super.onCreateTasks(savedInstanceState);
        if (CameraUtil.isTablet() || WifiDisplayStatusEx.isWfdEnabled(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            setRequestedOrientation(CameraUtil.calculateCurrentScreenOrientation(this));
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mRcsMirrorInterface = new RcsMirrorInterface();
        mPreferencesUtils = new PreferencesUtils(this);
        mRulerView = new RulerView(this);
        mUsbReceiver =new UsbReceiver(this);
        mOrientationListener = new OrientationEventListenerImpl(this);
        mToastView = new ToastView(this);
        //xiao add for draw ruler 
        ruler = new RulerMultiple();
        
        //create common ui module.
        mCameraAppUI = new CameraAppUI(this);
        profile.mark("CameraAppUI initialized.");
        mCameraAppUI.onCreate();
        profile.mark("CameraAppUI.onCreate done.");
        mIModeListener = new ModeManager();
        mIModeListener.create(this);
        mICameraMode = mIModeListener.getICameraMode();
        profile.mark("ModeManager.create done.");
        profile.stop();
        initRcsView();

    }


    private void initRcsView(){
        mRulerFrameLayout = (RulerFrameLayout) findViewById(R.id.rule_layout);
        mRulerView = (RulerView) findViewById(R.id.ruler_view);
        mSurfaceImg = (ImageView) findViewById(R.id.surface_img);
        mRuleUnit = (ImageView) findViewById(R.id.rule_unit);
    }

    /**
     * 初始化镜头View
     */
    private void initializeMirrorView(){
        Intent mCameraIntent = new Intent(this,CameraActivity.class);
        startActivity(mCameraIntent);
        initialMirrorView();
        finish();
    }

    private void initialMirrorView(){
        if (isEyeMirror()){
            initEyeMirrorView();
        }else if (isEarMirror()){
            initEarMirrorView();
        }else if (isSkinMirror()){
            initSkinMirrorView();
        }else if (isCommonMirror()){
            initCommonMirrorView();
        }else {
            initCommonMirrorView();
        }
    }


    private void initEyeMirrorView(){
        setViewShow(mSurfaceImg);
        setViewHide(mRulerView);
        setViewHide(mRuleUnit);
    }
    private void initEarMirrorView(){
        setViewShow(mSurfaceImg);
        setViewHide(mRulerView);
        setViewHide(mRuleUnit);
    }
    private void initSkinMirrorView(){
        setViewShow(mSurfaceImg);
        setViewShow(mRulerView);
        setViewShow(mRuleUnit);
    }

    private void initCommonMirrorView(){
        setViewHide(mSurfaceImg);
        setViewHide(mRulerView);
        setViewHide(mRuleUnit);
    }


    public void onRefreshRuleUnit(){
        Log.i("wwwwddd"," getCurrentRuleUnit() == " + getCurrentRuleUnit());
        if (isSkinMirror()){
            setViewShow(mRuleUnit);
            if (getCurrentRuleUnit()==0){
                if (mRuleUnit!=null){
                    mRuleUnit.setImageResource(R.drawable.mm);
                }
            }else if (getCurrentRuleUnit()==1){
                if (mRuleUnit!=null){
                    mRuleUnit.setImageResource(R.drawable.inchpng);
                }
            }
        }
    }



    @Override
    protected void onStartTasks() {
        super.onStartTasks();
        Log.i("view_ssss","onStartTasks1111111111111");
    }


    @Override
    protected void onResumeTasks() {
        IPerformanceProfile profile = PerformanceTracker.create(TAG, "onResume").start();
        mIsResumed = true;
        mOrientationListener.enable();
        super.onResumeTasks();
        PictureQuality.enterCameraMode();
        mIModeListener.resume();
        mUsbReceiver.usbregisterReceiver(this);
        registSreenStatusReceiver();
        mUsbReceiver.isConnectUsb();
        profile.mark("ModeManager resume done.");
        mRcsMirrorInterface.onResume();
        mCameraAppUI.onResume();
        profile.mark("CameraAppUI resume done.");
        mCameraAppUI.setThumbnailClickedListener(mThumbnailClickedListener);
        keepScreenOnForAWhile();
        profile.stop();
        initialMirrorView();
        exposureProgressValue = getPreExposureProgress();
        focusProgressValue = getPreFocusProgress();
        
        ruler.setBooleanValue(false);
        onRefreshRuleUnit();
        
    }

    @Override
    public ICameraMode getICameraMode() {
        return mIModeListener.getICameraMode();
    }

    @Override
    public boolean isPatientsInfoDisplay() {
         return Settings.System.getInt(getContentResolver(),
                Settings.System.RCS_SETTINGS_PATIENTINFO, 0) != 0;
    }

    @Override
    public boolean isLeftHandMode() {
        return Settings.System.getInt(this.getContentResolver(),
                Settings.System.RCS_HANDS_SWITCH, 1) == 0 ? true : false;
    }

    @Override
    public int providerStartPreView() {
        int startPreviewTime = Settings.System.getInt(getContentResolver(),
                Settings.System.RCS_PHOTO_PAUSE, 0) ;
        if (startPreviewTime==0){
            return 0;
        }else if (startPreviewTime==1){
            return 0;
        }else if (startPreviewTime==2){
            return 100;
        }else if (startPreviewTime==3){
            return 30000;
        }
        return 0;
    }

    @Override
    public void removeStartPreviewHandler() {
        if (mICameraMode!=null){
            mICameraMode.removeStartPreviewHandler();
        }
    }

    @Override
    public void setPreFocusProgress(int values) {
            if (isEyeMirror()){
                mPreferencesUtils.putInt(CameraUtil.EYE_KEY_FOCUS_PROGRESS_VALUE, values);
            }else if (isEarMirror()){
                mPreferencesUtils.putInt(CameraUtil.EAR_KEY_FOCUS_PROGRESS_VALUE, values);
            }else if (isSkinMirror()){
                mPreferencesUtils.putInt(CameraUtil.SKIN_KEY_FOCUS_PROGRESS_VALUE, values);
            }else if (isCommonMirror()){
                mPreferencesUtils.putInt(CameraUtil.COMMON_KEY_FOCUS_PROGRESS_VALUE, values);
            }
    }

    @Override
    public void setPreExposureProgress(int values) {
            if (isEyeMirror()){
                mPreferencesUtils.putInt(CameraUtil.EYE_KEY_EXPOSURE_PROGRESS_VALUE, values);
            }else if (isEarMirror()){
                mPreferencesUtils.putInt(CameraUtil.EAR_KEY_EXPOSURE_PROGRESS_VALUE, values);
            }else if (isSkinMirror()){
                mPreferencesUtils.putInt(CameraUtil.SKIN_KEY_EXPOSURE_PROGRESS_VALUE, values);
            }else if (isCommonMirror()){
                mPreferencesUtils.putInt(CameraUtil.COMMON_KEY_EXPOSURE_PROGRESS_VALUE, values);
            }
    }

    @Override
    public void setPreInfraredProgress(int values) {
        mPreferencesUtils.putInt(CameraUtil.EYE_KEY_INFRARED_PROGRESS_VALUE, values);
    }

    @Override
    public int getPreFocusProgress() {
            if (isEyeMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EYE_KEY_FOCUS_PROGRESS_VALUE);
            }else if (isEarMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EAR_KEY_FOCUS_PROGRESS_VALUE);
            }else if (isSkinMirror()){
                return mPreferencesUtils.getInt(CameraUtil.SKIN_KEY_FOCUS_PROGRESS_VALUE);
            }else if (isCommonMirror()){
                return mPreferencesUtils.getInt(CameraUtil.COMMON_KEY_FOCUS_PROGRESS_VALUE);
            }
            return 0;
    }

    @Override
    public int getPreExposureProgress() {
            if (isEyeMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EYE_KEY_EXPOSURE_PROGRESS_VALUE);
            }else if (isEarMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EAR_KEY_EXPOSURE_PROGRESS_VALUE);
            }else if (isSkinMirror()){
                return mPreferencesUtils.getInt(CameraUtil.SKIN_KEY_EXPOSURE_PROGRESS_VALUE);
            }else if (isCommonMirror()){
                return mPreferencesUtils.getInt(CameraUtil.COMMON_KEY_EXPOSURE_PROGRESS_VALUE);
            }
            return 0;
    }

    @Override
    public int getPreInfraredProgress() {
        return mPreferencesUtils.getInt(CameraUtil.EYE_KEY_INFRARED_PROGRESS_VALUE);
    }

    @Override
    public boolean noUseDefaultValue() {
        return Settings.System.getInt(this.getContentResolver(),
                Settings.System.RCS_MIRROR_DEFAULT, 0) == 0 ? true : false;
    }


    @Override
    public int getCurrentLight() {
        return Settings.System.getInt(this.getContentResolver(),
                Settings.System.RCS_SETTINGS_ILLUMINATION, 0);
    }

    @Override
    public int getCurrentRuleUnit() {
        return Settings.System.getInt(this.getContentResolver(),
                Settings.System.RCS_SETTINGS_RULERUNIT, 0);
    }

    @Override
    public void showToast() {
        if (mToastView!=null){
            mToastView.showText(this,"",1000,true);
        }
    }

    @Override
    public void setUVCVideoViewTrueState() {
        mCameraAppUI.getRcsShotManager().setUVCVideoViewTrueState();
    }
    
     @Override
    public void setUVCVideoViewFalseState() {
        mCameraAppUI.getRcsShotManager().setUVCVideoViewFalseState();
    }

    @Override
    protected void onPauseTasks() {
        mIsResumed = false;
        super.onPauseTasks();
        PictureQuality.exitCameraMode();
        mIModeListener.pause();
        mCameraAppUI.onPause();
        mOrientationListener.disable();
        synchronized (mOnOrientationListeners) {
            mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
        }
        resetScreenOn();
    }

    @Override
    protected void onStopTasks() {
        super.onStopTasks();
        /*Log.d("xiao_aaa","getBooleanValue"+ruler.getBooleanValue());
        if( !ruler.getBooleanValue() ){
            Log.d("xiao_aaa","finish-============");
            finish();//xiao add for 左右手模式
        }*/

    }

    @Override
    protected void onDestroyTasks() {
        super.onDestroyTasks();
        mIModeListener.destroy();
        mCameraAppUI.onDestroy();
        mUsbReceiver.unRegisterUsbActionReceiver(this);
        unregisterReceiver(mScreenStatusReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //add Rcs 强制横屏 liuxi start 20180713 start
        FrameLayout root = (FrameLayout) findViewById(R.id.app_ui);
        LogHelper.d(TAG, "onConfigurationChanged orientation = " + newConfig.orientation);
        if (root != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
               // root.setOrientation(0, false);
            } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
               // root.setOrientation(90, false);
            }
            mCameraAppUI.onConfigurationChanged(newConfig);
        }
        //add Rcs 强制横屏 liuxi start 20180713 end
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Iterator iterator = mKeyEventListeners.entrySet().iterator();
        KeyEventListener listener = null;
        while (iterator.hasNext()) {
            Map.Entry map = (Map.Entry) iterator.next();
            listener = (KeyEventListener) map.getValue();
            if (listener != null && listener.onKeyDown(keyCode, event)) {
                return true;
            }
        }
        if (keyCode==KEY_MIRROR_INSERT_CODE){
            setNoUseDefalutValue();
            mMainHandler.removeMessages(MSG_RCS_MIRROR);
            mMainHandler.sendEmptyMessageDelayed(MSG_RCS_MIRROR,DELAY_MSG_MIRROR_TIMER);
        }
        if (keyCode==KEY_MIRROR_PULL_OUT_CODE){
            setNoUseDefalutValue();
            mMainHandler.removeMessages(MSG_RCS_MIRROR);
            mMainHandler.sendEmptyMessageDelayed(MSG_RCS_MIRROR,DELAY_MSG_MIRROR_TIMER);
        }

        if(keyCode==306){
            if (photoMode()&&isShotPhotoMode()){
                Log.i("ccc_bbb"," photoMode()isShotPhotoMode() == ----- 306 " );
                mCameraAppUI.triggerShutterButtonClick(0);
            }
            if (videoMode()&&isShotVideoMode()){
                Log.i("ccc_bbb"," videoMode()isShotVideoMode() == ----- 306 " );
                if(isCurrentVideo){
                     mCameraAppUI.triggerShutterButtonClick(1);
                     isCurrentVideo = false;
                     mMainHandler.sendEmptyMessageDelayed(MSG_TAKE_VIDEO,3000);
                }
            }
            if (isExposureMode()){
                Log.i("ccc_bbb"," isExposureMode() == ----- 306 " );
                exposureProgressValue =exposureProgressValue-1;
                    if (exposureProgressValue<0){
                        exposureProgressValue = 0;
                    }
                mCameraAppUI.getRcsOrganManager().setExposureBarProgress(exposureProgressValue);
            }
            if (isManualFocusMode()){
                Log.i("ccc_bbb"," isManualFocusMode() == ----- 306 " );
                focusProgressValue = focusProgressValue-30;
                if (focusProgressValue<=0){
                    focusProgressValue = 0;
                }
                mCameraAppUI.getRcsOrganManager().setFocusBarProgress(focusProgressValue);
            }
        }
        if(keyCode==307){
            if (photoMode()&&isShotPhotoMode()){
                Log.i("ccc_bbb"," photoMode() ==isShotPhotoMode() ----- 307 " );
                mCameraAppUI.triggerShutterButtonClick(0);
            }
            if (videoMode()&&isShotVideoMode()){
                Log.i("ccc_bbb"," videoMode() ==isShotVideoMode() ----- 307 " );
                if(isCurrentVideo){
                     mCameraAppUI.triggerShutterButtonClick(1);
                     isCurrentVideo = false;
                     mMainHandler.sendEmptyMessageDelayed(MSG_TAKE_VIDEO,5000);
                }
            }
            if (isExposureMode()){
                Log.i("ccc_bbb"," isExposureMode() === ----- 307 " );
                exposureProgressValue =exposureProgressValue+1;
                if (isEyeMirror()){
                    if (exposureProgressValue>32){
                        exposureProgressValue = 32;
                    }
                }else{
                    if (exposureProgressValue>6){
                        exposureProgressValue = 6;
                    }
                }
                mCameraAppUI.getRcsOrganManager().setExposureBarProgress(exposureProgressValue);
            }
            if (isManualFocusMode()){
                Log.i("ccc_bbb"," isManualFocusMode() === ----- 307 " );
                focusProgressValue = focusProgressValue+30;
                if (focusProgressValue>=900){
                    focusProgressValue = 900;
                }
                mCameraAppUI.getRcsOrganManager().setFocusBarProgress(focusProgressValue);
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    private void setNoUseDefalutValue(){
        Log.i("no_sss","noUseDefaultValue == " +noUseDefaultValue());
        if (noUseDefaultValue()){
            setPreExposureProgress(0);
            setPreFocusProgress(0);
            setMirrorPhotoMode(0);
            setMirrorShotMode(0);
            setMirrorExposureMode(1);
            setMirrorFocusMode(0);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Iterator iterator = mKeyEventListeners.entrySet().iterator();
        KeyEventListener listener = null;
        while (iterator.hasNext()) {
            Map.Entry map = (Map.Entry) iterator.next();
            listener = (KeyEventListener) map.getValue();
            if (listener != null && listener.onKeyUp(keyCode, event)) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
	    Intent intent = new Intent();
	    ComponentName mComp = new ComponentName("com.example.zf_instructions","com.example.zf_instructions.MainActivity");
	    intent.setComponent(mComp);
	    intent.putExtra("key",helper);
	    startActivity(intent);
    
        /*Iterator iterator = mBackPressedListeners.entrySet().iterator();
        BackPressedListener listener = null;
        while (iterator.hasNext()) {
            Map.Entry map = (Map.Entry) iterator.next();
            listener = (BackPressedListener) map.getValue();
            if (listener != null && listener.onBackPressed()) {
                return;
            }
        }*/
        super.onBackPressed();
    }


    @Override
    public void onUserInteraction() {
        if (mIModeListener == null || !mIModeListener.onUserInteraction()) {
            super.onUserInteraction();
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public IAppUi getAppUi() {
        return mCameraAppUI;
    }

    @Override
    public void enableKeepScreenOn(boolean enabled) {
        LogHelper.d(TAG, "enableKeepScreenOn enabled " + enabled);
        if (mIsResumed) {
            mMainHandler.removeMessages(MSG_SET_SCREEN_ON_FLAG);
            Message msg = Message.obtain();
            msg.arg1 = enabled ? 1 : 0;
            msg.what = MSG_SET_SCREEN_ON_FLAG;
            mMainHandler.sendMessage(msg);
        }

    }

    @Override
    public void notifyNewMedia(Uri uri, boolean needNotify) {
        mUri = uri;
    }

    @Override
    public boolean notifyCameraSelected(String newCameraId) {
        return mIModeListener.onCameraSelected(newCameraId);
    }

    @Override
    public void registerKeyEventListener(KeyEventListener keyEventListener, int priority) {
        if (keyEventListener == null) {
            LogHelper.e(TAG, "registerKeyEventListener error [why null]");
        }
        mKeyEventListeners.put(mKeyEventListeners.getPriorityKey(priority, keyEventListener),
                keyEventListener);
    }

    @Override
    public void registerBackPressedListener(BackPressedListener backPressedListener,
            int priority) {
        if (backPressedListener == null) {
            LogHelper.e(TAG, "registerKeyEventListener error [why null]");
        }
        mBackPressedListeners.put(mBackPressedListeners.getPriorityKey(priority,
                backPressedListener), backPressedListener);
    }

    @Override
    public void unRegisterKeyEventListener(KeyEventListener keyEventListener) {
        if (keyEventListener == null) {
            LogHelper.e(TAG, "unRegisterKeyEventListener error [why null]");
        }
        if (mKeyEventListeners.containsValue(keyEventListener)) {
            mKeyEventListeners.remove(mKeyEventListeners.findKey(keyEventListener));
        }
    }

    @Override
    public void unRegisterBackPressedListener(BackPressedListener backPressedListener) {
        if (backPressedListener == null) {
            LogHelper.e(TAG, "unRegisterBackPressedListener error [why null]");
        }
        if (mBackPressedListeners.containsValue(backPressedListener)) {
            mBackPressedListeners.remove(mBackPressedListeners.findKey(backPressedListener));
        }
    }

    @Override
    public void registerOnOrientationChangeListener(OnOrientationChangeListener listener) {
        synchronized (mOnOrientationListeners) {
            if (!mOnOrientationListeners.contains(listener)) {
                if (mOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                    listener.onOrientationChanged(mOrientation);
                }
                mOnOrientationListeners.add(listener);
            }
        }
    }

    @Override
    public void unregisterOnOrientationChangeListener(OnOrientationChangeListener listener) {
        synchronized (mOnOrientationListeners) {
            if (mOnOrientationListeners.contains(listener)) {
                mOnOrientationListeners.remove(listener);
            }
        }
    }

    @Override
    public int getGSensorOrientation() {
        synchronized (mOnOrientationListeners) {
            return mOrientation;
        }
    }

    @Override
    public RcsMirrorInterface getRcsMirrorInterface() {
        if (mRcsMirrorInterface==null){
            return new RcsMirrorInterface();
        }
        return mRcsMirrorInterface;
    }

    @Override
    public PreferencesUtils getPreferencesUtils() {
        if (mPreferencesUtils==null){
            return new PreferencesUtils(this);
        }
        return mPreferencesUtils;
    }

    @Override
    public boolean isEyeMirror() {
        return getCameraMirrorState()==EyeMedicalMirror();
    }

    @Override
    public boolean isEarMirror() {
        return getCameraMirrorState()==EarMedicalMirror();
    }

    @Override
    public boolean isSkinMirror() {
        return getCameraMirrorState()==SkinMedicalMirror();
    }

    @Override
    public boolean isCommonMirror() {
        return getCameraMirrorState()==CommonMedicalMirror();
    }

    @Override
    public boolean isUnMedicalMirror() {
        return getCameraMirrorState()==UnMedicalMirror();
    }

    @Override
    public int getCurrentMirrorFocusMode() {
            if (isEyeMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EYE_KEY_FOCUS_MODE_RCS);
            }else if (isEarMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EAR_KEY_FOCUS_MODE_RCS);
            }else if (isSkinMirror()){
                return mPreferencesUtils.getInt(CameraUtil.SKIN_KEY_FOCUS_MODE_RCS);
            }else if (isCommonMirror()){
                return mPreferencesUtils.getInt(CameraUtil.COMMON_KEY_FOCUS_MODE_RCS);
            }
            return 0;
    }

    @Override
    public int getCurrentMirrorExposureMode() {
            if (isEyeMirror()){
                return mPreferencesUtils.getExposureInt(CameraUtil.EYE_KEY_EXPOSURE_MODE_RCS);
            }else if (isEarMirror()){
                return mPreferencesUtils.getExposureInt(CameraUtil.EAR_KEY_EXPOSURE_MODE_RCS);
            }else if (isSkinMirror()){
                return mPreferencesUtils.getExposureInt(CameraUtil.SKIN_KEY_EXPOSURE_MODE_RCS);
            }else if (isCommonMirror()){
                return mPreferencesUtils.getExposureInt(CameraUtil.COMMON_KEY_EXPOSURE_MODE_RCS);
            }
            return 0;
    }

    @Override
    public int getCurrentMirrorShotMode() {
            if (isEyeMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EYE_KEY_SHOT_MODE_RCS);
            }else if (isEarMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EAR_KEY_SHOT_MODE_RCS);
            }else if (isSkinMirror()){
                return mPreferencesUtils.getInt(CameraUtil.SKIN_KEY_SHOT_MODE_RCS);
            }else if (isCommonMirror()){
                return mPreferencesUtils.getInt(CameraUtil.COMMON_KEY_SHOT_MODE_RCS);
            }
            return 0;
    }

    @Override
    public int getCurrentMirrorPhotoMode() {
            if (isEyeMirror()){
                return mPreferencesUtils.getInt(CameraUtil.EYE_KEY_PHOTO_MODE_RCS);
            }else if (isEarMirror()){
                return  mPreferencesUtils.getInt(CameraUtil.EAR_KEY_PHOTO_MODE_RCS);
            }else if (isSkinMirror()){
                return mPreferencesUtils.getInt(CameraUtil.SKIN_KEY_PHOTO_MODE_RCS);
            }else if (isCommonMirror()){
                return mPreferencesUtils.getInt(CameraUtil.COMMON_KEY_PHOTO_MODE_RCS);
            }
            return 0;
    }

    @Override
    public int getCurrentEarMirrorOrganMode() {
        return mPreferencesUtils.getInt(CameraUtil.EAR_KEY_ORGAN);
    }

    @Override
    public int getCurrentEyeMirrorOrganMode() {
        return mPreferencesUtils.getInt(CameraUtil.EYE_KEY_ORGAN);
    }

    @Override
    public void setMirrorFocusMode(int value) {
            if (isEyeMirror()){
                mPreferencesUtils.putInt(CameraUtil.EYE_KEY_FOCUS_MODE_RCS, value);
            }else if (isEarMirror()){
                mPreferencesUtils.putInt(CameraUtil.EAR_KEY_FOCUS_MODE_RCS, value);
            }else if (isSkinMirror()){
                mPreferencesUtils.putInt(CameraUtil.SKIN_KEY_FOCUS_MODE_RCS, value);
            }else if (isCommonMirror()){
                mPreferencesUtils.putInt(CameraUtil.COMMON_KEY_FOCUS_MODE_RCS, value);
            }
    }

    @Override
    public void setMirrorExposureMode(int value) {
            if (isEyeMirror()){
                mPreferencesUtils.putInt(CameraUtil.EYE_KEY_EXPOSURE_MODE_RCS, value);
            }else if (isEarMirror()){
                mPreferencesUtils.putInt(CameraUtil.EAR_KEY_EXPOSURE_MODE_RCS, value);
            }else if (isSkinMirror()){
                mPreferencesUtils.putInt(CameraUtil.SKIN_KEY_EXPOSURE_MODE_RCS, value);
            }else if (isCommonMirror()){
                mPreferencesUtils.putInt(CameraUtil.COMMON_KEY_EXPOSURE_MODE_RCS, value);
            }
    }

    @Override
    public void setMirrorShotMode(int value) {
            if (isEyeMirror()){
                mPreferencesUtils.putInt(CameraUtil.EYE_KEY_SHOT_MODE_RCS, value);
            }else if (isEarMirror()){
                mPreferencesUtils.putInt(CameraUtil.EAR_KEY_SHOT_MODE_RCS, value);
            }else if (isSkinMirror()){
                mPreferencesUtils.putInt(CameraUtil.SKIN_KEY_SHOT_MODE_RCS, value);
            }else if (isCommonMirror()){
                mPreferencesUtils.putInt(CameraUtil.COMMON_KEY_SHOT_MODE_RCS, value);
            }
    }

    @Override
    public void setMirrorPhotoMode(int value) {
            if (isEyeMirror()){
                mPreferencesUtils.putInt(CameraUtil.EYE_KEY_PHOTO_MODE_RCS, value);
            }else if (isEarMirror()){
                mPreferencesUtils.putInt(CameraUtil.EAR_KEY_PHOTO_MODE_RCS, value);
            }else if (isSkinMirror()){
                mPreferencesUtils.putInt(CameraUtil.SKIN_KEY_PHOTO_MODE_RCS, value);
            }else if (isCommonMirror()){
                mPreferencesUtils.putInt(CameraUtil.COMMON_KEY_PHOTO_MODE_RCS, value);
            }
    }

    @Override
    public void setEarMirrorOrganMode(int value) {
           mPreferencesUtils.putInt(CameraUtil.EAR_KEY_ORGAN, value);
    }

    @Override
    public void setEyeMirrorOrganMode(int value) {
           mPreferencesUtils.putInt(CameraUtil.EYE_KEY_ORGAN, value);
    }

    @Override
    public boolean isAutoFocus() {
         return mRcsMirrorInterface.isAutoFocus()||getCurrentMirrorFocusMode()==0;
    }

    @Override
    public boolean isManualFocus() {
        return mRcsMirrorInterface.isManualFocus()||getCurrentMirrorFocusMode()==1;
    }


    @Override
    public MedicalMirror.MirrorId EarMedicalMirror() {
        return MedicalMirror.MirrorId.STATE_MIRROR_EAR;
    }

    @Override
    public MedicalMirror.MirrorId EyeMedicalMirror() {
        return MedicalMirror.MirrorId.STATE_MIRROR_EYE;
    }

    @Override
    public MedicalMirror.MirrorId CommonMedicalMirror() {
        return MedicalMirror.MirrorId.STATE_MIRROR_COMMON;
    }

    @Override
    public MedicalMirror.MirrorId SkinMedicalMirror() {
        return MedicalMirror.MirrorId.STATE_MIRROR_SKIN;
    }

    @Override
    public MedicalMirror.MirrorId UnMedicalMirror() {
        return MedicalMirror.MirrorId.STATE_MIRROR_CLOSED;
    }

    @Override
    public MedicalMirror.MirrorId getCameraMirrorState() {
        return mRcsMirrorInterface.getMirrorState();
    }

    @Override
    public void enableGSensorOrientation() {
        mOrientationListener.enable();
    }

    @Override
    public void disableGSensorOrientation() {
        mOrientationListener.disable();
    }

    /**
     * start gallery activity to browse the file withe specified uri.
     *
     * @param uri The specified uri of file to browse.
     */
    protected void goToGallery(Uri uri) {
        if (uri == null) {
            LogHelper.d(TAG, "uri is null, can not go to gallery");
            return;
        }
        String mimeType = getContentResolver().getType(uri);
        LogHelper.d(TAG, "[goToGallery] uri: " + uri + ", mimeType = " + mimeType);
        Intent intent = new Intent(REVIEW_ACTION);
        intent.setDataAndType(uri, mimeType);
        intent.putExtra(IS_CAMERA, true);
        // add this for screen pinning
        ActivityManager activityManager = (ActivityManager)
                getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activityManager.LOCK_TASK_MODE_PINNED == activityManager
                    .getLockTaskModeState()) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            }
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            LogHelper.e(TAG, "[startGalleryActivity] Couldn't view ", ex);
        }
    }

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            LogHelper.d(TAG, "handleMessage what = " + msg.what + " arg1 = " + msg.arg1);
            switch (msg.what) {
                case MSG_CLEAR_SCREEN_ON_FLAG:
                    getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                case MSG_SET_SCREEN_ON_FLAG:
                    boolean enabled = msg.arg1 == 1;
                    if (enabled) {
                        keepScreenOn();
                    } else {
                        keepScreenOnForAWhile();
                    }
                    break;
                case MSG_RCS_MIRROR:
                    initializeMirrorView();
                    break;
                case MSG_TAKE_VIDEO:
                    isCurrentVideo = true;
                    break;
                default:
                    break;
            }
        };
    };

    private void resetScreenOn() {
        mMainHandler.removeMessages(MSG_SET_SCREEN_ON_FLAG);
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnForAWhile() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_ON_FLAG,
                DELAY_MSG_SCREEN_SWITCH);
    }

    private void keepScreenOn() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * The implementer of OrientationEventListener.
     */
    private class OrientationEventListenerImpl extends OrientationEventListener {
        public OrientationEventListenerImpl(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }
            synchronized (mOnOrientationListeners) {
                final int roundedOrientation = roundOrientation(orientation, mOrientation);
                if (mOrientation != roundedOrientation) {
                    mOrientation = roundedOrientation;
                    LogHelper.i(TAG, "mOrientation = " + mOrientation);
                    for (OnOrientationChangeListener l : mOnOrientationListeners) {
                        l.onOrientationChanged(mOrientation);
                    }
                }
            }
        }
    }

    private static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    /**
     * Judge current is launch by intent.
     * @param activity the launch activity.
     * @return true means is launch by intent; otherwise is false.
     */
    private boolean isThirdPartyIntent(Activity activity) {
        Intent intent = activity.getIntent();
        String action = intent.getAction();
        boolean value = MediaStore.ACTION_IMAGE_CAPTURE.equals(action) ||
                MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action) ||
                MediaStore.ACTION_VIDEO_CAPTURE.equals(action);
        return value;
    }


    public void setViewHide(View viewHide){
        if (viewHide!=null){
            viewHide.setVisibility(View.GONE);
        }
    }

    public void setViewShow(View viewShow){
        if (viewShow!=null){
            viewShow.setVisibility(View.VISIBLE);
        }
    }

    public CameraAppUI getCameraAppUI(){
        return mCameraAppUI;
    }

    public boolean photoMode(){
        int photoMode = getCurrentMirrorPhotoMode();
        Log.i("RcsShotManager"," photoMode == " + photoMode);
        return photoMode==0?true:false;
    }

    public boolean videoMode(){
        int videoMode = getCurrentMirrorPhotoMode();
        Log.i("RcsShotManager"," videoMode == " + videoMode);
        return videoMode==1?true:false;
    }

    public boolean isShotPhotoMode(){
        int shotMode = getCurrentMirrorShotMode();
        Log.i("RcsShotManager"," shotMode 111111== " + shotMode);
        return shotMode==0?true:false;
    }

    public boolean isShotVideoMode(){
        int  shotMode = getCurrentMirrorShotMode();
         Log.i("RcsShotManager"," shotMode 22222222== " + shotMode);
        return shotMode==1?true:false;
    }

    public boolean isExposureMode(){
        int exposure = getCurrentMirrorExposureMode();
        return exposure==0?true:false;
    }

    public boolean isManualFocusMode(){
        int  manual = getCurrentMirrorFocusMode();
        return manual==1?true:false;
    }

    public RulerView getRulerView() {
        return mRulerView;
    }


    //-----------------------------------------------------------xiao add for RCS picture name start--------------------------------------------
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
         if(resultCode == 1){
			if(data == null){
				return;
			}
            patientresult = data.getStringExtra("result");
            mCurrentRecordName = data.getStringExtra("name");
	        mCameraFristName = data.getStringExtra("firstname");
	        mCameraLastName = data.getStringExtra("lastname");
	        patientbirth = data.getStringExtra("birth_fetch");
	        patientgender = data.getStringExtra("gender_fetch"); 
	        mPreferencesUtils.putString(CameraUtil.KEY_PATIENT_RESULT,patientresult);
	        mPreferencesUtils.putString(CameraUtil.KEY_FRIST_NAME,mCameraFristName);
            mPreferencesUtils.putString(CameraUtil.KEY_LAST_NAME,mCameraLastName);
            mPreferencesUtils.putString(CameraUtil.KEY_BRITH_FETCH,patientbirth);
            mPreferencesUtils.putString(CameraUtil.KEY_GENDER_FETCH,patientgender);
	        if(!TextUtils.isEmpty(patientresult)){
    	        //mpatient = new Patient(mCameraFristName,mCameraLastName,patientbirth,patientgender);
	        }
	        //判断表中是否有相同数据
	        //mpatientlist = patientdao.selectAll();  
	        
	        //第一次加入数据
	        /*if(mpatientlist.size() == 0 ){
	            Log.d("mCameraFristName == result 222" , "   " + mpatientlist);
	            patientdao.addUser(mpatient);
	            databaseid = 1;
	        }
	        
	        for(int i = 0 ; i< mpatientlist.size();i ++ ){
                if(mpatientlist.get(i) == mpatient){
                    databaseid = i;
                    Log.d("mCameraFristName == i " , "1111 " + i);
                }else{
                    databaseid = mpatientlist.size();
                    Log.d("mCameraFristName == i " , "2222 " + i);
                    patientdao.addUser(mpatient);
                }
            }*/
        }
    }
    
    /**
     * 患者信息
     */
    @Override
    public void gotoPatient(){
        Intent mIntent = new Intent();
	    ComponentName mComp = new ComponentName("com.example.zf_rcs","com.example.zf_rcs.MainActivity");
	    mIntent.setComponent(mComp);
        /*if(databaseid > 0){
            mpatientlist = patientdao.selectAll();
            if(mpatientlist.size() == 1){
                mpatient = mpatientlist.get(0);
            }else{
                mpatient = mpatientlist.get(databaseid);
            }
        }*/
	        //Add RCS 照片上信息的保存 liuxi 20180610 
	        mIntent.putExtra("cam_fname",mPreferencesUtils.getString(CameraUtil.KEY_FRIST_NAME));
	        mIntent.putExtra("cam_lname",mPreferencesUtils.getString(CameraUtil.KEY_LAST_NAME));
	        mIntent.putExtra("cam_birth",mPreferencesUtils.getString(CameraUtil.KEY_BRITH_FETCH));
	        mIntent.putExtra("cam_gender",mPreferencesUtils.getString(CameraUtil.KEY_GENDER_FETCH));
	        ruler.setBooleanValue(true);
	        startActivityForResult(mIntent,1);
	}
    /**
     * 
     * @return 照片上的显示的信息
     */
    @Override
    public String getPatientInfo(){
    	patientresult = mPreferencesUtils.getString(CameraUtil.KEY_PATIENT_RESULT);
        shootingtime = mRcsMirrorInterface.getDateToString();
        ruler.setShoottime(shootingtime);
        if(isSkinMirror()){
            if (getCurrentRuleUnit()==0){
                return "Shooting time: " + shootingtime + ";" + patientresult +" Unit : mm";
            }else if (getCurrentRuleUnit()==1){
                return "Shooting time: " + shootingtime + ";" + patientresult +" Unit : inch";
            }
                return "Shooting time: " + shootingtime + ";" + patientresult ;
        }else{
        	if (mCurrentOrgan==1) {
        		return "Shooting time: " + shootingtime + ";" + patientresult  + "Position: F L";
		    }else if(mCurrentOrgan==2){
			    return "Shooting time: " + shootingtime + ";" + patientresult +"Position: F R";
		    }else{
			    return "Shooting time: " + shootingtime + ";" + patientresult ;
		    }
        }

    }
    
    /**
     * 
     * @return 获取患者姓和名(Camera界面显示)
     */
     @Override
     public String getPatientName(){
	    String mCurrentRecordName = "";
	    //mpatientlist = patientdao.selectAll();
	    /*if(mpatientlist.size() > 0){
	        mpatient = mpatientlist.get(mpatientlist.size() - 1);
            if(Settings.System.getInt(getContentResolver(),
                        Settings.System.RCS_SETTINGS_RECORDNAME,0) == 0){
                mCurrentRecordName = mpatient.getFirstname() + " " + mpatient.getLastname();
            }else{
                mCurrentRecordName = mpatient.getLastname() + " " + mpatient.getFirstname();
            }
	    }*/
        /*if(Settings.System.getInt(getContentResolver(),
                    Settings.System.RCS_SETTINGS_RECORDNAME,0) == 0){
            mCurrentRecordName  = mPreferencesUtils.getString(Util.KEY_FRIST_NAME)+" "+mPreferencesUtils.getString(Util.KEY_LAST_NAME);
        }else{*/
            mCurrentRecordName  = mPreferencesUtils.getString(CameraUtil.KEY_LAST_NAME)+" "+mPreferencesUtils.getString(CameraUtil.KEY_FRIST_NAME);
        //}
	    return mCurrentRecordName;
    }
    
    @Override
	public String getCurrentPictureName(){
        //shootingtime = mRcsMirrorInterface.getDateToString();
        shootingtime = ruler.getShoottime();
        if(TextUtils.isEmpty(shootingtime)){
            shootingtime = mRcsMirrorInterface.getDateToString();
        }
		if (isEyeMirror()) {
			return getEyeMirrorPictureName();
		}else if (isEarMirror()) {
			return getEarMirrorPictureName();
		}else if(isSkinMirror()){
			return getSkinMirrorPictureName();
		}else if (isCommonMirror()) {
			return getCommonMirrorPictureName();
		}else {
			return getNoMirrorPictureName();
		}
	}
	
    @Override
	public String getCurrentVideoName(){
	    String videoshootingtime = mRcsMirrorInterface.getDateToString();
		if (isEyeMirror()) {
			if(Settings.System.getInt(getContentResolver(),
                    Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+videoshootingtime+"F"+getCurrentOrgan();
	        }
	        return  videoshootingtime+getPictureName()+"F"+getCurrentOrgan();
		}else if (isEarMirror()) {
			if(Settings.System.getInt(getContentResolver(),
                Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+videoshootingtime+"O"+getCurrentOrgan();
	        }
	        return  videoshootingtime+getPictureName()+"O"+getCurrentOrgan();
		}else if(isSkinMirror()){
			if(Settings.System.getInt(getContentResolver(),
                Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	            return getPictureName()+videoshootingtime+"D";
	        }
	            return  videoshootingtime+getPictureName()+"D";
		}else if (isCommonMirror()) {
			if(Settings.System.getInt(getContentResolver(),
                Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+videoshootingtime+"G";
	        }
	        return  videoshootingtime+getPictureName()+"G";
		}else {
		    if(Settings.System.getInt(getContentResolver(),
                Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	            return getPictureName()+videoshootingtime;
	        }
	            return  videoshootingtime+getPictureName();
		}
	}
	
	
	@Override
	public Bitmap getBitmap(){
	    return BitmapFactory.decodeResource(this.getResources(),R.drawable.rcs_photo_bm);
	}

    /***
    * 
    * @return 四种镜头下图库照片的命名
    */
   private String getPictureName(){
        if(mPreferencesUtils.getString(CameraUtil.KEY_LAST_NAME) == null){
            return "";
        }
        Log.d("xiaoname " ,"last name  == " + mPreferencesUtils.getString(CameraUtil.KEY_LAST_NAME));
            return mPreferencesUtils.getString(CameraUtil.KEY_LAST_NAME);
    }
    
	private String getEyeMirrorPictureName(){
	    if(Settings.System.getInt(getContentResolver(),
                    Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+shootingtime+"F"+getCurrentOrgan();
	    }
	    return  shootingtime+getPictureName()+"F"+getCurrentOrgan();
	
	}
	private String getEarMirrorPictureName(){
		    if(Settings.System.getInt(getContentResolver(),
                    Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+shootingtime+"O"+getCurrentOrgan();
	    }
	    return  shootingtime+getPictureName()+"O"+getCurrentOrgan();
	}
	
	
	private String getSkinMirrorPictureName(){
			    if(Settings.System.getInt(getContentResolver(),
                    Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+shootingtime+"D";
	    }
	    return  shootingtime+getPictureName()+"D";
	}
	
	private String getCommonMirrorPictureName(){
			    if(Settings.System.getInt(getContentResolver(),
                    Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+shootingtime+"G";
	    }
	    return  shootingtime+getPictureName()+"G";
	}
	
	private String getNoMirrorPictureName(){
		    if(Settings.System.getInt(getContentResolver(),
                    Settings.System.RCS_SETTINGS_SHOOTINGTIME,0) == 0){
	        return getPictureName()+shootingtime;
	    }
	    return  shootingtime+getPictureName();
	}
	
	/**
     * 
     * @return 照片上显示器官的部位
     */
    private String getCurrentOrgan(){
    	if (isEyeMirror()){
            if (getCurrentEyeMirrorOrganMode()==0){
                return "L";
            }else if (getCurrentEyeMirrorOrganMode()==1){
                return 	"R";
            }
        }else if (isEarMirror()){
            if (getCurrentEarMirrorOrganMode()==0){
                return "L";
            }else if (getCurrentEarMirrorOrganMode()==1){
                return 	"R";
            }
        }
        return "";
    }
    
    //xiao add for draw ruler start 
    @Override
    public float getCurrentLeftX(){
        return mRulerFrameLayout.getCurrentLeftX();
    }
    @Override
    public float getCurrentLeftY(){
        return mRulerFrameLayout.getCurrentLeftY();
    }
    @Override
    public float getCurrentRightY(){
        return mRulerFrameLayout.getCurrentRightY();
    }
     @Override
    public float getCurrentRightX(){
        return mRulerFrameLayout.getCurrentRightX();
    }
    @Override
    public float getValueK(){
        return mRulerView.getValueK();
    }
    
    private String multipleValue ;    
    private float multiple;
    @Override
    public void setvalues(String string){
        multipleValue = string;    
        ruler.setValue(multipleValue);
        //Log.d("xiao333"," values multipleValue ==" + multipleValue );
        if(TextUtils.isEmpty(multipleValue)){
            multipleValue = "x1.0";
        }
        
        multiple = Float.parseFloat(multipleValue.substring(1,multipleValue.length()));
        
        mRulerView.setZoomValue(multiple,true);
        mRulerView.postInvalidate();
    }
    @Override
    public String getvalues(){
        multipleValue = ruler.getValue();
        
        if(TextUtils.isEmpty(multipleValue)){
            multipleValue = "x1.0";
        }
        //Log.d("xiao333"," getvalues multipleValue ==" + multipleValue );
        return multipleValue.substring(1,multipleValue.length());
    }
    //xiao add for draw ruler end
    
    //xiao add for uvc interface
    public boolean isUvcConnected(){


       return ruler.getBooleanValue();
    }
    
    public void setUvcConnectState(boolean isConnected){
        ruler.setBooleanValue(isConnected);
    }
    
        
    class RulerMultiple{
        private String multiple;
        private String shoottime;
        private boolean isPatient;
        private boolean isUvcConnected;
        
        
        public String getValue(){
            return multiple;
        } 
        
        public void setValue(String string){
            this.multiple = string;
        }
        
        public String getShoottime(){
            return shoottime;
        } 
        
        public void setShoottime(String string){
            this.shoottime = string;
        }
        
        public boolean getBooleanValue(){
            return isPatient;
        } 
        
        public void setBooleanValue(boolean string){
            this.isPatient = string;
        }
        
        public boolean getUVCValue(){
            return isUvcConnected;
        } 
        
        public void setUVCValue(boolean string){
            this.isUvcConnected = string;
        }
        
    }
     
 //-----------------------------------------------------------xiao add for RCS picture name end--------------------------------------------
 
 //-----------------------------------------------------------xiao add for RCS set time start ---------------------------------------------------
    private boolean compareTwoTime(String starTime, String endString) {
        boolean isDayu = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date parse = dateFormat.parse(starTime);
            Date parse1 = dateFormat.parse(endString);
            long diff = parse1.getTime() - parse.getTime();
            Log.d("xiaotime","starTime ==" + starTime + "  endString == "+endString);
            Log.d("xiaotime","diff ==" + diff);
            if (diff >= 0) {
                isDayu = true;
            } else {
                isDayu = false;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return isDayu;
    }
 
    private void gotoSettingTime(){
        new AlertDialog.Builder(this)
                .setTitle("TimeSetting")
                .setMessage("Current time is the factory reset time ,please go to set right time")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                	    Intent mIntent = new Intent();
                        ComponentName mComp = new ComponentName("com.example.zf_rcs_settings","com.example.zf_rcs_settings.DateSetting");
                        mIntent.setComponent(mComp);
                        startActivity(mIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create().show();
    }
     /**
     * @return当前时间
     */
    public String getNowTime() {
        String timeString = null;
        Time time = new Time();
        time.setToNow();
        String year = thanTen(time.year);
        String month = thanTen(time.month + 1);
        String monthDay = thanTen(time.monthDay);
        String hour = thanTen(time.hour);
        String minute = thanTen(time.minute);
        timeString = year + "-" + month + "-" + monthDay;
        return timeString;
    }
    
    /**
     * 十一下加零
     *
     * @param str
     * @return
     */
    public String thanTen(int str) {
        String string = null;
        if (str < 10) {
            string = "0" + str;
        } else {
            string = "" + str;
        }
        return string;
    }
 //-----------------------------------------------------------xiao add for RCS set time start --------------------------------------------------- 

    //add wangchao
    /**
     * 获取Bitmap
     * 用于pc显示
     */
    @Override
    public Bitmap getMainPicBitmap() {
        return BitmapFactory.decodeResource(this.getResources(),R.drawable.rcs_test);
    }

    private void registSreenStatusReceiver() {
        mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter screenStatusIF = new IntentFilter();
        screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
        screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStatusReceiver, screenStatusIF);
    }

    class ScreenStatusReceiver extends BroadcastReceiver {
        String SCREEN_ON = "android.intent.action.SCREEN_ON";
        String SCREEN_OFF = "android.intent.action.SCREEN_OFF";
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SCREEN_ON.equals(intent.getAction())) {
                if(isEarMirror() || isSkinMirror() || isCommonMirror()){
                    mRcsMirrorInterface.setMirrorFillLightOn();
                }else if(isEyeMirror()){
                    if (getCurrentLight()==1){
                        mRcsMirrorInterface.setMirrorFillLightOn();
                    }else if (getCurrentLight()==0){
                        mRcsMirrorInterface.setMirrorFlashLightOn();
                    }
                }
            }else if (SCREEN_OFF.equals(intent.getAction())) {
                mRcsMirrorInterface.setMirrorAllLightOff();
            }
        }
    }

}
