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
 * MediaTek Inc. (C) 2014. All rights reserved.
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
package com.mediatek.camera.feature.mode.pip.pipwrapping;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.mediatek.camera.common.debug.CameraSysTrace;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.gles.egl.EglCore;
import com.mediatek.camera.common.utils.AtomAccessor;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.pipwrapping.PipOperator.PIPCustomization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pip renderer manager.
 */
public class RendererManager {
    private static final Tag TAG = new Tag(RendererManager.class.getSimpleName());
    private static final String BOTTOM = "pip_bottom";
    private static final String TOP = "pip_top";
    private final Activity mActivity;
    private final RendererCallback mRendererCallback;

    private int mCurrentOrientation;
    private int mCaptureOrientation;
    private int mPreviewTexWidth = -1;
    private int mPreviewTexHeight = -1;
    private boolean mIsBottomHasHighFrameRate = true;
    private boolean mPIPSwitched = false;
    private AnimationRect mPreviewTopGraphicRect = null;
    private AnimationRect mCaptureTopGraphicRect = null;

    private HandlerThread mPreviewEglThread;
    private PreviewRendererHandler mPreviewEglHandler;

    private HandlerThread mCaptureEglThread;
    private CaptureRendererHandler mCaptureEglHandler;

    private int mBackResId = 0;
    private int mFrontResId = 0;
    private int mHighlightResId = 0;
    private int mEditBtnResId = 0;

    private RecorderRenderer mRecorderRenderer;
    private boolean mNeedNotifyFirstFrame = true;
    private AtomAccessor mAtomAccessor = new AtomAccessor();

    /**
     * Renderer callback.
     */
    public interface RendererCallback {
        /**
         * Notify when first frame swapped.
         *
         * @param timestamp buffer's timestamp.
         */
        void onFirstFrameAvailable(long timestamp);

        /**
         * Notify when pip switch called in renderer.
         */
        void onPipSwitchedInRenderer();

        /**
         * Whether bottom is back camera.
         * @return whether bottom is back camera.
         */
        boolean bottomGraphicIsBackCamera();
    }

    /**
     * Construct a renderer manager.
     * @param rendererCallback the render callback.
     * @param activity the activity.
     */
    public RendererManager(RendererCallback rendererCallback, Activity activity) {
        mRendererCallback = rendererCallback;
        mActivity = activity;
    }

    /**
     * Init renderer manager.
     */
    public void init() {
        LogHelper.d(TAG, "[init]+");
        if (mPreviewEglHandler == null) {
            createPreviewGLThread();
        }
        initScreenRenderer();
        LogHelper.d(TAG, "[init]-");
    }

    /**
     * Un init renderer manger.
     */
    public void unInit() {
        LogHelper.d(TAG, "[unInit]+");
        if (mPreviewEglHandler != null) {
            doReleaseAndQuitThread(mPreviewEglHandler, mPreviewEglThread);
            mPreviewEglHandler = null;
            mPreviewEglThread = null;
            mPreviewTexWidth = -1;
            mPreviewTexHeight = -1;
        }
        if (mCaptureEglHandler != null) {
            doReleaseAndQuitThread(mCaptureEglHandler, mCaptureEglThread);
            mCaptureEglHandler = null;
            mCaptureEglThread = null;
        }
        LogHelper.d(TAG, "[unInit]-");
    }

    /**
     * update pip template resource.
     * Note: if resource id is the same with previous, call this function has no use.
     * @param backResId bottom graphic template
     * @param frontResId top graphic template
     * @param highlightResId top graphic highlight template
     * @param editBtnResId top graphic edit template
     */
    public void updateEffectTemplates(int backResId, int frontResId,
            int highlightResId, int editBtnResId) {
        if ((mBackResId) == backResId && (mFrontResId == frontResId)
                && (mHighlightResId == highlightResId)) {
            return;
        }
        if (mPreviewEglHandler != null) {
            mBackResId = backResId;
            mFrontResId = frontResId;
            mHighlightResId = highlightResId;
            mEditBtnResId = editBtnResId;
            mPreviewEglHandler.removeMessages(PipRendererHandler.MSG_UPDATE_TEMPLATE);
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_UPDATE_TEMPLATE);
            mAtomAccessor.sendAtomMessage(mPreviewEglHandler, msg);
        }
    }

    /**
     * Set pip preview texture's size.
     * <p>
     * Note: pip bottom and top texture's size must be the same for switch pip.
     * @param width bottom/top texture's width
     * @param height bottom/top texture's height
     */
    public void setPreviewSize(int width, int height) {
        LogHelper.d(TAG, "[setPreviewTextureSize]+ width = " + width
                + " height = " + height);
        if (mPreviewTexWidth == width && mPreviewTexHeight == height) {
            LogHelper.i(TAG, "setPreviewTextureSize same size set, ignore!");
            return;
        }
        if (mPreviewEglHandler != null) {
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_UPDATE_RENDERER_SIZE,
                        width, height, null);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
        LogHelper.d(TAG, "[setPreviewTextureSize]-");
    }

    /**
     * create two surface textures, switch pip by needSwitchPIP
     * <p>
     * By default, bottom surface texture is drawn in bottom graphic.
     * top surface texture is drawn in top graphic.
     */
    public void setUpSurfaceTextures() {
        LogHelper.d(TAG, "[setUpSurfaceTextures]+");
        boolean needUpdate = false;
        // press home key exit pip and resume again, template update action will not happen
        // here call update template for this case.
        // update template should not block ui thread
        if (mPreviewEglHandler != null && !mPreviewEglHandler.hasMessages(
                PipRendererHandler.MSG_UPDATE_TEMPLATE)) {
            needUpdate = true;
        }
        setupPIPTextures();
        if (needUpdate && mPreviewEglHandler != null) {
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_UPDATE_TEMPLATE);
            mAtomAccessor.sendAtomMessage(mPreviewEglHandler, msg);
        }
        LogHelper.d(TAG, "[setUpSurfaceTextures]-");
    }

    /**
     * Set preview surface to receive pip preview buffer.
     * <p>
     * Note: this must be called after setPreviewTextureSize
     * @param surface used to receive preview buffer
     */
    public void setPreviewSurfaceSync(Surface surface) {
        LogHelper.d(TAG, "setPreviewSurfaceSync");
        if (mPreviewEglHandler != null && surface != null) {
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_SET_PREVIEW_SURFACE, surface);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
    }

    /**
     * Check whether is during switch pip.
     * @return whether is during switch pip.
     */
    public boolean isDuringSwitchPip() {
        if (mPreviewEglHandler != null) {
            return mPreviewEglHandler.hasMessages(PipRendererHandler.MSG_SWITCH_PIP);
        }
        return false;
    }

    /**
     * Set preview surface to receive pip preview buffer.
     * <p>
     * Note: this must be called after setPreviewTextureSize
     * @param surface the surface used for preview
     */
    public void notifySurfaceViewDestroyed(Surface surface) {
        if (mPreviewEglHandler != null) {
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_PREVIEW_SURFACE_DESTORY, surface);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
    }

    /**
     * Get bottom surface texture.
     * @return bottom graphic surface texture
     */
    public SurfaceTextureWrapper getBottomPvSt() {
        LogHelper.d(TAG, "getBottomPvSt mIsPIPSwitched = " + mPIPSwitched);
        return mPIPSwitched ? getTopSurfaceTexture() : getBottomSurfaceTexture();
    }

    /**
     * Get top surface texture.
     * @return top graphic surface texture
     */
    public SurfaceTextureWrapper getTopPvSt() {
        LogHelper.d(TAG, "getTopPvSt mIsPIPSwitched = " + mPIPSwitched);
        return mPIPSwitched ? getBottomSurfaceTexture() : getTopSurfaceTexture();
    }

    /**
     * update top graphic's position.
     * @param topGraphic the top graphic's position
     */
    public void updateTopGraphic(AnimationRect topGraphic) {
        mPreviewTopGraphicRect = topGraphic;
    }

    /**
     * when G-sensor's orientation changed, should update it to PipOperator.
     * @param newOrientation G-sensor's new orientation
     */
    public void updateGSensorOrientation(int newOrientation) {
        LogHelper.d(TAG, "updateOrientation newOrientation = " + newOrientation);
        mCurrentOrientation = newOrientation;
    }

    /**
     * keep capture orientation and top graphci rect.
     * Will use these data when capture
     */
    public void keepCaptureRotation() {
        mCaptureOrientation = mCurrentOrientation;
        mCaptureTopGraphicRect = mPreviewTopGraphicRect.copy();
    }

    /**
     * Get preview texture width.
     * @return the preview texture width.
     */
    public int getPreviewTextureWidth() {
        return mPreviewTexWidth;
    }

    /**
     * Get preview texture height.
     * @return the preview texture height.
     */
    public int getPreviewTextureHeight() {
        return mPreviewTexHeight;
    }

    /**
     * Prepare recording renderer.
     */
    public void prepareRecordSync() {
        if (mRecorderRenderer == null) {
            mRecorderRenderer = new RecorderRenderer(mActivity);
        }
        if (mPreviewEglHandler != null) {
            mPreviewEglHandler.removeMessages(PipRendererHandler.MSG_SETUP_VIDEO_RENDER);
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_SETUP_VIDEO_RENDER);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
    }
    /**
     * Set recording surface to receive pip buffer.
     * Note: this must be called after setPreviewTextureSize
     * @param surface used for receiving video buffer
     * @param orientation recorder orientation
     */
    public void setRecordSurfaceSync(Surface surface, int orientation) {
        if (mPreviewEglHandler != null) {
            Message msg = mPreviewEglHandler.obtainMessage(
            PipRendererHandler.MSG_SET_RECORDING_SURFACE, orientation, 0, surface);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
    }

    /**
     * Start push video buffer to encoder's surface.
     */
    public void startRecordSync() {
        if (mRecorderRenderer != null) {
            mRecorderRenderer.startRecording();
        }
    }

    /**
     * Stop push video buffer to encoder's surface.
     */
    public void stopRecordSync() {
        if (mRecorderRenderer != null) {
            mRecorderRenderer.stopRecording();
            mRecorderRenderer = null;
        }
    }

    /**
     * switch pip.
     */
    public void switchPip() {
        LogHelper.d(TAG, "switchPip");
        if (mPreviewEglHandler != null) {
            mPreviewEglHandler.removeMessages(
                    PipRendererHandler.MSG_SWITCH_PIP);
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_SWITCH_PIP);
            mAtomAccessor.sendAtomMessage(mPreviewEglHandler, msg);
        }
    }

    /**
     * Take a video snap shot by orientation.
     * @param orientation video snap shot orientation
     * @param vssSurface the surface used to receive
     */
    public void takeVideoSnapShot(int orientation, Surface vssSurface) {
        LogHelper.d(TAG, "takeVideoSnapShot orientation = " + orientation);
        if (mPreviewEglHandler != null) {
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_TAKE_VSS, orientation, 0, vssSurface);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
    }

    /**
     * Get bottom capture surface texture.
     * @return bottom capture surface texture.
     */
    public SurfaceTextureWrapper getBottomCapSt() {
        return mCaptureEglHandler == null ? null : mCaptureEglHandler.getBottomSt();
    }

    /**
     * Get top capture surface texture.
     * @return top capture surface texture.
     */
    public SurfaceTextureWrapper getTopCapSt() {
        return mCaptureEglHandler == null ? null : mCaptureEglHandler.getTopSt();
    }

    /**
     * Init capture.
     *
     * @param inputFormats input formats.
     * @return pixel format that this egl can output.
     */
    public int initCapture(int[] inputFormats) {
        checkAndCreateCaptureGLThread(inputFormats);
        return mCaptureEglHandler.getPixelFormat();
    }

    /**
     * Set capture surface.
     *
     * @param surface the surface used for taking picture.
     */
    public void setCaptureSurface(Surface surface) {
        if (mCaptureEglHandler != null) {
            Message msg = mCaptureEglHandler.obtainMessage(
                    CaptureRendererHandler.MSG_SETUP_CAPTURE_SURFACE, surface);
            mAtomAccessor.sendAtomMessageAndWait(mCaptureEglHandler, msg);
        }
    }

    /**
    * Set Capture Size.
    * @param bottomCaptureSize bottom picture size
    * @param topCaptureSize top picture size
    */
    public void setCaptureSize(Size bottomCaptureSize, Size topCaptureSize) {
        LogHelper.d(TAG, "[setCaptureSize] bottom:" + bottomCaptureSize + ",top:" + topCaptureSize);
        if (mCaptureEglHandler != null) {
            Map<String, Size> pictureSizeMap = new HashMap<>();
            pictureSizeMap.put(BOTTOM, bottomCaptureSize);
            pictureSizeMap.put(TOP, topCaptureSize);
            Message msg = mCaptureEglHandler.obtainMessage(
                   CaptureRendererHandler.MSG_SETUP_PICTURE_TEXTURES, pictureSizeMap);
            mAtomAccessor.sendAtomMessageAndWait(mCaptureEglHandler, msg);
        }
    }

    /**
     * Set the jpeg's rotation received from Capture SurfaceTexture.
     * @param isBottomCam is bottom jpeg's rotation.
     * @param rotation received from surface texture's jpeg rotation.
     */
    public void setJpegRotation(boolean isBottomCam, int rotation) {
        LogHelper.d(TAG, "[setJpegRotation] isBottomCam:" + isBottomCam + ", rotation:" + rotation);
        if (mCaptureEglHandler != null) {
            mCaptureEglHandler.setJpegRotation(isBottomCam, rotation);
        }
    }

    /**
     * Un init capture.
     */
    public void unInitCapture() {
        if (mCaptureEglHandler != null) {
            doReleaseAndQuitThread(mCaptureEglHandler, mCaptureEglThread);
            mCaptureEglHandler = null;
            mCaptureEglThread = null;
        }
    }

    /**
     * After stop preview, need notify first frame after start preview.
     */
    public void afterStopPreview() {
        mNeedNotifyFirstFrame = true;
    }

    private void initScreenRenderer() {
        if (mPreviewEglHandler != null) {
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_INIT_SCREEN_RENDERER);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
    }

    private void setupPIPTextures() {
        LogHelper.d(TAG, "setupPIPTextures");
        if (mPreviewEglHandler != null) {
            // here should not remove frame message, must consume all frames
            // otherwise frame will not come to ap if previous frames are not consumed.
            Message msg = mPreviewEglHandler.obtainMessage(
                    PipRendererHandler.MSG_SETUP_PIP_TEXTURES);
            mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
        }
    }

    private SurfaceTextureWrapper getBottomSurfaceTexture() {
        return mPreviewEglHandler == null ? null : mPreviewEglHandler.getBottomSt();
    }

    private SurfaceTextureWrapper getTopSurfaceTexture() {
        return mPreviewEglHandler == null ? null : mPreviewEglHandler.getTopSt();
    }

    private void createPreviewGLThread() {
        mPreviewEglThread = new HandlerThread("Pip-PreviewGLThread");
        mPreviewEglThread.start();
        Looper looper = mPreviewEglThread.getLooper();
        if (looper == null) {
            throw new RuntimeException("why looper is null?");
        }
        mPreviewEglHandler = new PreviewRendererHandler(looper);
        Message msg = mPreviewEglHandler.obtainMessage(PipRendererHandler.MSG_INIT);
        mAtomAccessor.sendAtomMessageAndWait(mPreviewEglHandler, msg);
    }

    private void checkAndCreateCaptureGLThread(int[] formats) {
        if (mCaptureEglHandler == null) {
            mCaptureEglThread = new HandlerThread("Pip-CaptureGLThread");
            mCaptureEglThread.start();
            Looper looper = mCaptureEglThread.getLooper();
            if (looper == null) {
                throw new RuntimeException("why looper is null?");
            }
            mCaptureEglHandler = new CaptureRendererHandler(looper);
            Message msg = mCaptureEglHandler.obtainMessage(PipRendererHandler.MSG_INIT, formats);
            mAtomAccessor.sendAtomMessageAndWait(mCaptureEglHandler, msg);
        }
    }

    private void doReleaseAndQuitThread(Handler handler, HandlerThread thread) {
        handler.removeCallbacksAndMessages(null);
        Message msg = handler.obtainMessage(PipRendererHandler.MSG_RELEASE);
        mAtomAccessor.sendAtomMessageAndWait(handler, msg);
        Looper looper = thread.getLooper();
        if (looper != null) {
            looper.quit();
        }
    }

    /**
     * An abstract handler for reusing preview and capture handler's common code.
     */
    public abstract class PipRendererHandler extends Handler {
        public static final int MSG_INIT = 0;
        public static final int MSG_RELEASE = 1;
        public static final int MSG_UPDATE_TEMPLATE = 2;
        public static final int MSG_UPDATE_RENDERER_SIZE = 3;
        public static final int MSG_SETUP_VIDEO_RENDER = 4;
        public static final int MSG_SWITCH_PIP = 6;
        public static final int MSG_SETUP_PIP_TEXTURES = 7;
        public static final int MSG_NEW_BOTTOM_FRAME_ARRIVED = 8;
        public static final int MSG_NEW_TOP_FRAME_ARRIVED = 9;
        public static final int MSG_PREVIEW_SURFACE_DESTORY = 10;
        public static final int MSG_TAKE_VSS = 11;
        public static final int MSG_INIT_SCREEN_RENDERER = 13;
        public static final int MSG_SET_PREVIEW_SURFACE = 14;
        public static final int MSG_SET_RECORDING_SURFACE = 15;
        public static final int MSG_COUNT = 16;

        private static final int UNVALID_TEXTURE_ID = -12345;

        private final HandlerThread mFrameListener = new HandlerThread("PIP-PreviewSTFListener");
        private Handler mSurfaceTextureHandler;

        private BottomGraphicRenderer mPreviewBottomGraphicRenderer;
        private TopGraphicRenderer mPreviewTopGraphicRenderer;
        private SurfaceTextureWrapper mBottomPrvSt;
        private SurfaceTextureWrapper mTopPrvSt;
        private int mPreviewFboTexId = UNVALID_TEXTURE_ID;

        private ScreenRenderer mScreenRenderer;
        private FrameBuffer mPreviewFrameBuffer;

        private List<CaptureRenderer> mVssRendererList = new CopyOnWriteArrayList<>();

        private float[] mBottomTransformMatrix = new float[16];
        private float[] mTopCamTransformMatrix = new float[16];
        private long mLatestBottomCamTimeStamp = 0L;
        private long mLatestTopCamTimeStamp = 0L;
        private long mBottomCamTimeStamp = 0L;
        private long mTopCamTimeStamp = 0L;

        protected EglCore mEglCore;
        protected EGLSurface mOffScreenSurface = null;
        private PipRendererHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_TEMPLATE:
                doUpdateTemplate();
                break;
            case MSG_UPDATE_RENDERER_SIZE:
                mPreviewTexWidth = msg.arg1;
                mPreviewTexHeight = msg.arg2;
                doUpdateRenderSize();
                break;
            case MSG_SETUP_VIDEO_RENDER:
                setUpRenderForRecord();
                break;
            case MSG_SWITCH_PIP:
                doSwitchPIP();
                break;
            case MSG_INIT_SCREEN_RENDERER:
                doInitScreenRenderer();
                break;
            case MSG_SETUP_PIP_TEXTURES:
                doSetupPIPTextures();
                break;
            case MSG_SET_PREVIEW_SURFACE:
                doUpdatePreviewSurface((Surface) msg.obj);
                break;
            case MSG_SET_RECORDING_SURFACE:
                doUpdateRecordingSurface((Surface) msg.obj, msg.arg1);
                break;
            case MSG_NEW_BOTTOM_FRAME_ARRIVED:
                doProcessPreviewFrame((SurfaceTextureWrapper) msg.obj, true);
                break;
            case MSG_NEW_TOP_FRAME_ARRIVED:
                doProcessPreviewFrame((SurfaceTextureWrapper) msg.obj, false);
                break;
            case MSG_PREVIEW_SURFACE_DESTORY:
                if (mScreenRenderer != null) {
                    mScreenRenderer.notifySurfaceDestroyed((Surface) msg.obj);
                }
                break;
            case MSG_TAKE_VSS:
                doVideoSnapShot(msg.arg1, (Surface) msg.obj);
                break;
            case MSG_RELEASE:
                doRelease();
                break;
            default:
                break;
            }
        }

        /**
         * Get bottom surface texture.
         * @return bottom surface texture.
         */
        public SurfaceTextureWrapper getBottomSt() {
            LogHelper.d(TAG, "getBottomSt mBottomPrvSt:" + mBottomPrvSt);
            return mBottomPrvSt;
        }

        /**
         * Get top surface texture.
         * @return top surface texture.
         */
        public SurfaceTextureWrapper getTopSt() {
            LogHelper.d(TAG, "getTopSt mTopPrvSt:" + mTopPrvSt);
            return mTopPrvSt;
        }

        /**
         * Init egl core.
         */
        protected void initEglCore() {
            int[] supportedFormats = new int[2];
            supportedFormats[0] = PixelFormat.RGBA_8888;
            supportedFormats[1] = PixelFormat.RGB_888;
            mEglCore = new EglCore(null, EglCore.CONSTRUCTOR_FLAG_TRY_GLES3, supportedFormats);
        }

        /**
         * un init egl core.
         */
        protected void unInitEglCore() {
            LogHelper.d(TAG, "[release]+");
            if (mEglCore != null) {
                mEglCore.releaseEglSurface(mOffScreenSurface);
                mEglCore.makeNothingCurrent();
                mEglCore.release();
                mEglCore = null;
            }
            LogHelper.d(TAG, "[release]-");
        }

        private void doUpdateTemplate() {
            LogHelper.d(TAG, "doUpdateTemplate backResourceId = " + mBackResId
                    + " frontResourceId = " + mFrontResId + " fronthighlight = "
                    + mHighlightResId);
            if (mPreviewTopGraphicRenderer != null) {
                mPreviewTopGraphicRenderer.initTemplateTexture(
                        mBackResId, mFrontResId);
            }
            if (mScreenRenderer != null) {
                mScreenRenderer.updateScreenEffectTemplate(
                        mHighlightResId, mEditBtnResId);
            }
            LogHelper.d(TAG, "doUpdateTemplate end");
        }

        private void doUpdateRenderSize() {
            LogHelper.d(TAG, "doUpdateRenderSize mPreviewTexWidth = " +
                    mPreviewTexWidth + " mPreviewTexHeight = " + mPreviewTexHeight);
            // start frame available thread
            if (!mFrameListener.isAlive()) {
                mFrameListener.start();
                mSurfaceTextureHandler = new Handler(mFrameListener.getLooper());
            }

            if (mBottomPrvSt == null) {
                // initialize bottom surface texture wrapper
                int[] textures = GLUtil.generateTextureIds(1);
                GLUtil.bindPreviewTexture(textures[0]);
                mBottomPrvSt = new SurfaceTextureWrapper("Bottom", textures[0], this);
                mBottomPrvSt.setDefaultBufferSize(
                        Math.max(mPreviewTexWidth, mPreviewTexHeight),
                        Math.min(mPreviewTexWidth, mPreviewTexHeight));
            }
            mBottomPrvSt.setDefaultBufferSize(
                    Math.max(mPreviewTexWidth, mPreviewTexHeight),
                    Math.min(mPreviewTexWidth, mPreviewTexHeight));

            if (mTopPrvSt == null) {
                // initialize top surface texture
                int[] textures = GLUtil.generateTextureIds(1);
                GLUtil.bindPreviewTexture(textures[0]);
                mTopPrvSt = new SurfaceTextureWrapper("Top", textures[0], this);
                mTopPrvSt.setDefaultBufferSize(
                        Math.max(mPreviewTexWidth, mPreviewTexHeight),
                        Math.min(mPreviewTexWidth, mPreviewTexHeight));
            }
            mTopPrvSt.setDefaultBufferSize(
                    Math.max(mPreviewTexWidth, mPreviewTexHeight),
                    Math.min(mPreviewTexWidth, mPreviewTexHeight));

            if (mPreviewFrameBuffer != null) {
                mPreviewFrameBuffer.setRendererSize(mPreviewTexWidth, mPreviewTexHeight);
            }
            if (mPreviewBottomGraphicRenderer != null) {
                mPreviewBottomGraphicRenderer.setRendererSize(
                        mPreviewTexWidth, mPreviewTexHeight, false);
            }
            if (mPreviewTopGraphicRenderer != null) {
                mPreviewTopGraphicRenderer.setRendererSize(mPreviewTexWidth, mPreviewTexHeight);
            }
            if (mScreenRenderer != null) {
                mScreenRenderer.setRendererSize(mPreviewTexWidth, mPreviewTexHeight);
            }
        }

        private void setUpRenderForRecord() {
            if (mRecorderRenderer != null) {
                mRecorderRenderer.init();
            }
        }

        private void doSwitchPIP() {
            doSwitchTextures();
            mPIPSwitched = !mPIPSwitched;
            mRendererCallback.onPipSwitchedInRenderer();
        }

        private void doSwitchTextures() {
            // switch matrix
            float[] matrix = mTopCamTransformMatrix;
            mTopCamTransformMatrix = mBottomTransformMatrix;
            mBottomTransformMatrix = matrix;
        }

        private void doInitScreenRenderer() {
            mScreenRenderer = new ScreenRenderer(mActivity);
            mScreenRenderer.init();
        }

        private void doSetupPIPTextures() {
            LogHelper.d(TAG, "doInitiSurfaceTextures mPreviewFrameBuffer = " + mPreviewFrameBuffer);
            resetTimeStamp();
            if (mPreviewFrameBuffer == null) {
                // initialize preview frame buffer
                mPreviewFrameBuffer = new FrameBuffer();
                mPreviewFboTexId = mPreviewFrameBuffer.getFboTexId();
                // initialize bottom graphic renderer
                mPreviewBottomGraphicRenderer = new BottomGraphicRenderer(mActivity);
                mPreviewBottomGraphicRenderer.init();
                // initialize top graphic renderer
                mPreviewTopGraphicRenderer = new TopGraphicRenderer(mActivity);
                mPreviewTopGraphicRenderer.init();
                // in pip mode press home key to exit camera, and enter
                // again should restore pip state
                if (mPIPSwitched) {
                    doSwitchTextures();
                }
            }
            mIsBottomHasHighFrameRate = true; //Util.isBottomHasHighFrameRate(mActivity);
        }

        private void doUpdatePreviewSurface(Surface surface) {
            if (mScreenRenderer != null) {
                mScreenRenderer.setSurface(surface, true, true);
                mNeedNotifyFirstFrame = true;
            }
        }

        private void doReleasePIPTexturesAndRenderers() {
            LogHelper.d(TAG, "doReleasePIPSurfaceTextures");
            doReleasePIPTextures();
            releasePIPRenderers();
        }

        private void doUpdateRecordingSurface(Surface surface, int orientation) {
            if (mRecorderRenderer != null) {
                mRecorderRenderer.setRendererSize(mPreviewTexWidth, mPreviewTexHeight, orientation);
                mRecorderRenderer.setRecordingSurface(surface);
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        private void releasePIPRenderers() {
            LogHelper.d(TAG, "releasePIPRenderers");
            mPreviewBottomGraphicRenderer = null;
            if (mPreviewTopGraphicRenderer != null) {
                mPreviewTopGraphicRenderer.release();
                mPreviewTopGraphicRenderer = null;
            }
            if (mRecorderRenderer != null) {
                mRecorderRenderer.releaseSurface();
                mRecorderRenderer = null;
            }
            if (mScreenRenderer != null) {
                mScreenRenderer.release();
                mScreenRenderer = null;
            }
            if (mSurfaceTextureHandler != null) {
                mFrameListener.quitSafely();
                mSurfaceTextureHandler = null;
            }
        }

        private void doVideoSnapShot(int orientation, Surface vssSurface) {
            CaptureRenderer captureRenderer = new CaptureRenderer(mActivity);
            captureRenderer.init();
            captureRenderer.setCaptureSize(mPreviewTexWidth, mPreviewTexHeight, orientation);
            captureRenderer.setCaptureSurface(vssSurface);
            mVssRendererList.add(captureRenderer);
        }

        private void doRelease() {
            LogHelper.d(TAG, "doRelease");
            if (mTopPrvSt != null) {
                mTopPrvSt.release();
                mTopPrvSt = null;
            }
            if (mBottomPrvSt != null) {
                mBottomPrvSt.release();
                mBottomPrvSt = null;
            }
            doReleasePIPTexturesAndRenderers();
        }

        private void doReleasePIPTextures() {
            LogHelper.d(TAG, "_doReleasePIPTextures");
            if (mPreviewFrameBuffer != null) {
                mPreviewFrameBuffer.release();
                mPreviewFrameBuffer = null;
                mPreviewFboTexId = UNVALID_TEXTURE_ID;
            }
        }

        private void doProcessPreviewFrame(SurfaceTextureWrapper stWrapper, boolean isBottom) {
            CameraSysTrace.onEventSystrace("ProcessPreviewFrame", true);
            stWrapper.updateTexImage();
            if (isBottom) {
                doUpdateBottomCamTimeStamp();
            } else {
                doUpdateTopCamTimeStamp();
            }
            draw();
            CameraSysTrace.onEventSystrace("ProcessPreviewFrame", false);
        }

        private void doUpdateTopCamTimeStamp() {
            if (mTopPrvSt == null) {
                return;
            }
            mLatestTopCamTimeStamp = mTopPrvSt.getTimeStamp();
            if (mPIPSwitched) {
                mBottomTransformMatrix = mTopPrvSt.getBufferTransformMatrix();
            } else {
                mTopCamTransformMatrix = mTopPrvSt.getBufferTransformMatrix();
            }
        }

        private void doUpdateBottomCamTimeStamp() {
            if (mBottomPrvSt == null) {
                return;
            }
            mLatestBottomCamTimeStamp = mBottomPrvSt.getTimeStamp();
            if (mPIPSwitched) {
                mTopCamTransformMatrix = mBottomPrvSt.getBufferTransformMatrix();
            } else {
                mBottomTransformMatrix = mBottomPrvSt.getBufferTransformMatrix();
            }
        }

        private void resetTimeStamp() {
            mBottomCamTimeStamp = 0L;
            mLatestBottomCamTimeStamp = 0L;
            mTopCamTimeStamp = 0L;
            mLatestTopCamTimeStamp = 0;
        }

        private boolean doTimestampSync() {
            // Step1: waiting two camera's buffer has arrived.
            if (mLatestBottomCamTimeStamp == 0L || mLatestTopCamTimeStamp == 0L) {
                return false;
            }
            // Step2: update according to bottom's fps, because it has high fps.
            if (mIsBottomHasHighFrameRate && (mBottomCamTimeStamp != mLatestBottomCamTimeStamp)) {
                mBottomCamTimeStamp = mLatestBottomCamTimeStamp;
                mTopCamTimeStamp = mLatestTopCamTimeStamp;
                return true;
            }
            //Step3: update according to top's fps, because it has high fps.
            if (!mIsBottomHasHighFrameRate && (mTopCamTimeStamp != mLatestTopCamTimeStamp)) {
                mBottomCamTimeStamp = mLatestBottomCamTimeStamp;
                mTopCamTimeStamp = mLatestTopCamTimeStamp;
                return true;
            }
            return false;
        }

        private void draw() {
            if (doTimestampSync() && mPreviewFrameBuffer != null) {
                CameraSysTrace.onEventSystrace("draw", true);
                drawToFbo();

                if (mRecorderRenderer != null) {
                    mRecorderRenderer.draw(
                            mPreviewFboTexId,
                            mBottomPrvSt.getTimeStamp());
                }
                mScreenRenderer.draw(
                        mPreviewTopGraphicRect.copy(),
                        mPreviewFboTexId,
                        mPreviewTopGraphicRect.getHighLightStatus());
                // draw to screen
                if (mNeedNotifyFirstFrame) {
                    mRendererCallback.onFirstFrameAvailable(
                            mBottomPrvSt.getTimeStamp());
                    mNeedNotifyFirstFrame = false;
                }
                if (mVssRendererList.size() > 0) {
                    CaptureRenderer captureRenderer = mVssRendererList.get(0);
                    if (captureRenderer != null) {
                        captureRenderer.draw(mPreviewFboTexId);
                        captureRenderer.release();
                        mVssRendererList.remove(captureRenderer);
                        mEglCore.makeCurrent(mOffScreenSurface);
                    }
                }
                // Debug for printing swap fps
                updateFrameCounter();
                CameraSysTrace.onEventSystrace("draw", false);
            }
        }

        private void drawToFbo() {
            mPreviewFrameBuffer.setupFrameBufferGraphics(mPreviewTexWidth,
                    mPreviewTexHeight);
            mPreviewBottomGraphicRenderer.draw(
                    mPIPSwitched ? mTopPrvSt.getTextureId() : mBottomPrvSt.getTextureId(),
                    mBottomTransformMatrix,
                    GLUtil.createIdentityMtx(),
                    false);
            mPreviewTopGraphicRenderer.draw(
                    mPIPSwitched ? mBottomPrvSt.getTextureId() : mTopPrvSt.getTextureId(),
                    mTopCamTransformMatrix,
                    GLUtil.createIdentityMtx(),
                    mPreviewTopGraphicRect.copy(),
                    mCurrentOrientation,
                    false,
                    mRendererCallback.bottomGraphicIsBackCamera());
            mPreviewFrameBuffer.setScreenBufferGraphics();
        }
    }

    /**
     * Handler used for doing preview and recoding.
     */
    private class PreviewRendererHandler extends PipRendererHandler {

        public PreviewRendererHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case MSG_INIT:
                initEglCore();
                mOffScreenSurface = mEglCore.createOffscreenSurface(1, 1);
                mEglCore.makeCurrent(mOffScreenSurface);
                break;
            case MSG_RELEASE:
                unInitEglCore();
                break;
            default:
                break;
            }
        }
    }

    /**
     * Handler used for taking picture.
     *
     */
    private class CaptureRendererHandler extends PipRendererHandler {
        private static final int MSG_SETUP_PICTURE_TEXTURES = MSG_COUNT;
        private static final int MSG_SETUP_CAPTURE_SURFACE = MSG_COUNT + 1;
//        private static final int MSG_CAPTURE_FRAME_AVAILABLE = MSG_COUNT + 2;

        private final HandlerThread mFrameListener = new HandlerThread("PIP-CaptureSTFListener");
        private Handler mSurfaceTextureHandler;

        private SurfaceTextureWrapper mBottomCapSt = null;
        private SurfaceTextureWrapper mTopCapSt = null;
        private BottomGraphicRenderer mBottomRenderer;
        private TopGraphicRenderer mTopRenderer;

        private WindowSurface mCapEglSurface = null;
        private int mBottomJpegRotation = 0;
        private int mTopJpegRotation = 0;

        public CaptureRendererHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            LogHelper.d(TAG, "handleMessage:" + msg.what);
            switch (msg.what) {
            case MSG_SETUP_PICTURE_TEXTURES:
                @SuppressWarnings("unchecked")
                Map<String, Size> pictureSizeMap = (HashMap<String, Size>) msg.obj;
                setUpTexturesForCapture(pictureSizeMap.get(BOTTOM), pictureSizeMap.get(TOP));
                break;
            case MSG_SETUP_CAPTURE_SURFACE:
                if (mEglCore != null) {
                    Surface captureSurface = (Surface) msg.obj;
                    mCapEglSurface = new WindowSurface(mEglCore, captureSurface);
                    mCapEglSurface.makeCurrent();
                }
                mBottomJpegRotation = 0;
                mTopJpegRotation = 0;
                break;
            case MSG_NEW_BOTTOM_FRAME_ARRIVED:
            case MSG_NEW_TOP_FRAME_ARRIVED:
                SurfaceTextureWrapper stPicWrapper = (SurfaceTextureWrapper) msg.obj;
                stPicWrapper.updateTexImage();
                tryTakePicture();
                break;
            case MSG_INIT:
                int[] formats = (int[]) msg.obj;
                mEglCore = new EglCore(
                        null,
                        EglCore.CONSTRUCTOR_FLAG_TRY_GLES3 | EglCore.CONSTRUCTOR_FLAG_RECORDABLE,
                        formats);
                break;
            case MSG_RELEASE:
                releaseRenderer();
                unInitEglCore();
                break;
            default:
                break;
            }
        }

        public int getPixelFormat() {
            return mEglCore.getOutPutPixelFormat();
        }

        /**
         * Get bottom surface texture.
         * @return bottom surface texture.
         */
        public SurfaceTextureWrapper getBottomSt() {
            return mBottomCapSt;
        }

        /**
         * Get top surface texture.
         * @return top surface texture.
         */
        public SurfaceTextureWrapper getTopSt() {
            return mTopCapSt;
        }

        /**
         * Set jpeg rotation.
         * @param isBottomCam is bottom camera.
         * @param rotation the rotation.
         */
        public void setJpegRotation(boolean isBottomCam, int rotation) {
            if (isBottomCam) {
                mBottomJpegRotation = rotation;
                return;
            }
            mTopJpegRotation = rotation;
        }

        private void setUpTexturesForCapture(Size bPictureSize, Size tPictureSize) {
            LogHelper.d(TAG, "[setUpTexturesForCapture]+");
            if (!mFrameListener.isAlive()) {
                mFrameListener.start();
                mSurfaceTextureHandler = new Handler(mFrameListener.getLooper());
            }

            if (mBottomCapSt == null) {
                // initialize top surface texture
                int[] textures = GLUtil.generateTextureIds(1);
                GLUtil.bindPreviewTexture(textures[0]);
                mBottomCapSt = new SurfaceTextureWrapper("Bottom", textures[0], this);
                mBottomCapSt.setDefaultBufferSize(
                        bPictureSize.getWidth(), bPictureSize.getHeight());
            }
            mBottomCapSt.setDefaultBufferSize(bPictureSize.getWidth(), bPictureSize.getHeight());
            if (mBottomRenderer == null) {
                mBottomRenderer = new BottomGraphicRenderer(mActivity);
                mBottomRenderer.init();
            }

            if (mTopCapSt == null) {
                int[] textures = GLUtil.generateTextureIds(1);
                GLUtil.bindPreviewTexture(textures[0]);
                mTopCapSt = new SurfaceTextureWrapper("Top", textures[0], this);
                mTopCapSt.setDefaultBufferSize(tPictureSize.getWidth(), tPictureSize.getHeight());
            }
            mTopCapSt.setDefaultBufferSize(tPictureSize.getWidth(), tPictureSize.getHeight());
            if (mTopRenderer == null) {
                mTopRenderer = new TopGraphicRenderer(mActivity);
                mTopRenderer.init();
            }

            LogHelper.d(TAG, "[setUpTexturesForCapture]-");
        }

        private void tryTakePicture() {
            if (mBottomCapSt != null && mBottomCapSt.getTimeStamp() > 0
                    && mTopCapSt != null && mTopCapSt.getTimeStamp() > 0) {
                LogHelper.d(TAG, "[tryTakePicture]+");
                mBottomRenderer.setRendererSize(
                        mBottomCapSt.getWidth(), mBottomCapSt.getHeight(), true);
                mTopRenderer.initTemplateTexture(mBackResId, mFrontResId);
                mTopRenderer.setRendererSize(mBottomCapSt.getWidth(), mBottomCapSt.getHeight());

                AnimationRect pictureTopGraphicRect = mCaptureTopGraphicRect.copy();
                pictureTopGraphicRect.changeCoordinateSystem(
                        mBottomCapSt.getWidth(),
                        mBottomCapSt.getHeight(), 360 - mCaptureOrientation);

                boolean bottomIsMainCamera = mRendererCallback.bottomGraphicIsBackCamera();
                boolean bottomNeedMirror = (!bottomIsMainCamera)
                        && PIPCustomization.SUB_CAMERA_NEED_HORIZONTAL_FLIP;
                boolean topNeedMirror = bottomIsMainCamera
                        && PIPCustomization.SUB_CAMERA_NEED_HORIZONTAL_FLIP;

                // enable blend, in order to get a transparent background
                GLES20.glViewport(0, 0, mBottomCapSt.getWidth(), mBottomCapSt.getHeight());
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                mBottomRenderer.draw(
                        mBottomCapSt.getTextureId(),
                        GLUtil.createIdentityMtx(), // OES Texture
                        getTexMatrixByRotation(mBottomJpegRotation), // texture rotate
                        bottomNeedMirror); //need flip
                mTopRenderer.draw(
                        mTopCapSt.getTextureId(),
                        GLUtil.createIdentityMtx(), // OES Texture
                        getTexMatrixByRotation(mTopJpegRotation), // texture rotate
                        pictureTopGraphicRect.copy(),
                        mCaptureOrientation > 0 ? -mCaptureOrientation : -1,
                        topNeedMirror, //need flip
                        mRendererCallback.bottomGraphicIsBackCamera());
                mCapEglSurface.swapBuffers();
                // Be careful, Surface Texture's release should always happen
                // before make nothing current.
                doReleaseCaptureSt();
                mCapEglSurface.makeNothingCurrent();
                mCapEglSurface.releaseEglSurface();
                mCapEglSurface = null;
                mNeedNotifyFirstFrame = true;
                LogHelper.d(TAG, "[tryTakePicture]-");
            }
        }

        private float[] getTexMatrixByRotation(int rotation) {
            float[] texRotateMtxByOrientation = GLUtil.createIdentityMtx();
            android.opengl.Matrix.translateM(texRotateMtxByOrientation, 0,
                    texRotateMtxByOrientation, 0, .5f, .5f, 0);
            android.opengl.Matrix.rotateM(texRotateMtxByOrientation, 0,
                    -rotation, 0, 0, 1);
            android.opengl.Matrix.translateM(texRotateMtxByOrientation, 0, -.5f, -.5f, 0);
            return texRotateMtxByOrientation;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        private void releaseRenderer() {
            if (mBottomRenderer != null) {
                mBottomRenderer.release();
                mBottomRenderer = null;
            }
            if (mTopRenderer != null) {
                mTopRenderer.release();
                mTopRenderer = null;
            }
            doReleaseCaptureSt();
            if (mSurfaceTextureHandler != null) {
                mFrameListener.quitSafely();
                mSurfaceTextureHandler = null;
            }
        }

        private void doReleaseCaptureSt() {
            if (mBottomCapSt != null) {
                mBottomCapSt.release();
                mBottomCapSt = null;
            }

            if (mTopCapSt != null) {
                mTopCapSt.release();
                mTopCapSt = null;
            }
        }
    }

    private static final int INTERVALS = 300;
    private int mDrawDrawFrameCount = 0;
    private long mDrawDrawStartTime = 0;
    private void updateFrameCounter() {
        mDrawDrawFrameCount++;
        if (mDrawDrawFrameCount % INTERVALS == 0) {
            long currentTime = System.currentTimeMillis();
            int intervals = (int) (currentTime - mDrawDrawStartTime);
            LogHelper.d(TAG, "[AP-->Wrapping][Preview] Drawing frame, fps = "
                + (mDrawDrawFrameCount * 1000.0f) / intervals + " in last " + intervals
                + " millisecond.");
            mDrawDrawStartTime = currentTime;
            mDrawDrawFrameCount = 0;
        }
    }
}