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
 *   MediaTek Inc. (C) 2016. All rights reserved.
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

package com.mediatek.camera.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.IAppUiListener.ISurfaceStatusListener;
import com.mediatek.camera.common.IAppUiListener.OnPreviewAreaChangedListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.common.widget.PreviewFrameLayout;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * A manager for {@link FixedAspectSurfaceView}.
 */
class PreviewManager implements View.OnTouchListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PreviewManager.class.getSimpleName());
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private double mPreviewAspectRatio = 0.0d;
    private FixedAspectSurfaceView mSurfaceView;
    private IApp mApp;
    private RectF mPreviewArea = new RectF();

    private Thread mProducerThread;
    private GLRendererImpl mRenderer;
    private final Object mRenderSyncLock = new Object();

    private final CopyOnWriteArrayList<OnPreviewAreaChangedListener>
                                    mPreviewAreaChangedListeners;

    private ViewGroup mPreviewRoot;
    private ViewGroup mPreviewContainer;
    private ViewGroup mLastPreviewContainer;
    private View.OnTouchListener mOnTouchListener;
    private PreviewFrameLayout mPreviewFrameLayout;
    private boolean mIsSurfaceCreated = false;
    private SurfaceChangeCallback mSurfaceChangeCallback;


    private BlockingQueue<View> mFrameLayoutQueue =
                        new LinkedBlockingQueue<View>();


    /**
     * constructor of PreviewManager.
     * @param app The {@link IApp} implementer.
     */
    public PreviewManager(IApp app) {
        mApp = app;
        mPreviewRoot  =  (ViewGroup) mApp.getActivity().findViewById(R.id.preview_frame_root);
        initSurfaceViewLayout();
        mPreviewAreaChangedListeners = new CopyOnWriteArrayList<>();
        mPreviewFrameLayout =
                (PreviewFrameLayout) mApp.getActivity().findViewById(R.id.preview_layout_container);
        if (isThirdPartyIntent(mApp.getActivity())) {
            View cover = mApp.getActivity().findViewById(R.id.preview_cover);
            cover.setVisibility(View.VISIBLE);
        }
    }

    /**
     * update preview size.
     * This function should be called on the UI thread and listeners will be
     * notified on the UI thread.
     * @param width preview width, must > 0.
     * @param height preview height, must > 0.
     * @param listener Set preview status listener. The new listener will replace the old one.
     */
    public void updatePreviewSize(int width, int height, ISurfaceStatusListener listener) {
        LogHelper.i(TAG, "updatePreviewSize: new size (" + width + " , " + height + " )"
                + " current size (" + mPreviewWidth + " , " + mPreviewHeight + " )" + "," +
                "mIsSurfaceCreated = " + mIsSurfaceCreated +
                "listener = " + listener);
        if (mPreviewWidth == width && mPreviewHeight == height) {
            //If preview size is same, just call back surface available.
            ISurfaceStatusListener l = mSurfaceChangeCallback.getBindStatusListener();
            if (listener != null && listener != l) {
                mSurfaceView.getHolder().removeCallback(mSurfaceChangeCallback);
                mSurfaceChangeCallback = new SurfaceChangeCallback(listener);
                mSurfaceView.getHolder().addCallback(mSurfaceChangeCallback);

            }
            if (mIsSurfaceCreated) {
                if (listener != null) {
                    listener.surfaceAvailable(mSurfaceView.getHolder(),
                                                    mPreviewWidth, mPreviewHeight);
                }
            }
            return;
        }
        if (mPreviewAspectRatio != 0) {
            mLastPreviewContainer = mPreviewContainer;
            mSurfaceView = null;
        } else {
            double ratio = (double) Math.max(width, height)
                    / Math.min(width, height);
            if (!mSurfaceView.isFullScreenPreview(ratio)) {
                mLastPreviewContainer = mPreviewContainer;
                mSurfaceView = null;
            }
        }

        mPreviewWidth = width;
        mPreviewHeight = height;
        mPreviewAspectRatio = (double) Math.max(width, height)
                / Math.min(width, height);
        // if aspect ration changed, attach a new surface view
        if (mSurfaceView == null) {
            attachSurfaceView(listener);
        } else {
            mSurfaceView.getHolder().removeCallback(mSurfaceChangeCallback);
            mSurfaceChangeCallback = new SurfaceChangeCallback(listener);
            mSurfaceView.getHolder().addCallback(mSurfaceChangeCallback);
        }
        mSurfaceView.getHolder().setFixedSize(mPreviewWidth, mPreviewHeight);
        mSurfaceView.setAspectRatio(mPreviewAspectRatio);
    }

    /**
     * Gets called from module when preview is started.
     */
    public void onPreviewStarted() {
    }

    /**
     * Clear the Status previous listener that set when update preview size.
     * @param listener Listener set to previewManager previously.
     */
    public void clearPreviewStatusListener(ISurfaceStatusListener listener) {
        if (mSurfaceChangeCallback != null) {
            ISurfaceStatusListener l = mSurfaceChangeCallback.getBindStatusListener();
            if (l != null && l == listener) {
                mSurfaceView.getHolder().removeCallback(mSurfaceChangeCallback);
                mSurfaceChangeCallback = new SurfaceChangeCallback(null);
                mSurfaceView.getHolder().addCallback(mSurfaceChangeCallback);
            }
        }
    }

    /**
     * Adds a listener that will get notified when the preview area changed. This
     * can be useful for UI elements or focus view to adjust themselves according
     * to the preview area change.
     * <p/>
     * Note that a listener will only be added once. A newly added listener will receive
     * a notification of current preview area immediately after being added.
     * <p/>
     * This function should be called on the UI thread and listeners will be notified
     * on the UI thread.
     *
     * @param listener the listener that will get notified of preview area change
     */
    public void registerPreviewAreaChangedListener(
            IAppUiListener.OnPreviewAreaChangedListener listener) {
        if (listener != null && !mPreviewAreaChangedListeners.contains(listener)) {
            mPreviewAreaChangedListeners.add(listener);
            if (mPreviewArea.width() != 0 || mPreviewArea.height() != 0) {
                listener.onPreviewAreaChanged(mPreviewArea,
                        new Size(mPreviewWidth, mPreviewHeight));
            }
        }
    }

    /**
     * Removes a listener that gets notified when the preview area changed.
     *
     * @param listener the listener that gets notified of preview area change
     */
    public void unregisterPreviewAreaChangedListener(
            OnPreviewAreaChangedListener listener) {
        if (listener != null && mPreviewAreaChangedListeners.contains(listener)) {
            mPreviewAreaChangedListeners.remove(listener);
        }
    }

    public void setEnabled(final boolean enabled) {
        if (mSurfaceView != null) {
            mSurfaceView.setEnabled(enabled);
        }
    }

    public View getView() {
        return mSurfaceView;
    }

    public PreviewFrameLayout getPreviewFrameLayout() {
        return mPreviewFrameLayout;
    }

    public void onPause() {
        if(!mApp.getActivity().isFinishing()) {
            if (mLastPreviewContainer != null) {
                //Remove the old last preview, otherwise the surface number maybe overflow.
                mLastPreviewContainer.setVisibility(View.GONE);
                mPreviewRoot.removeView(mLastPreviewContainer);
            }
            mLastPreviewContainer = mPreviewContainer;
            mSurfaceView = null;
            attachSurfaceView(mSurfaceChangeCallback.getBindStatusListener());
            mSurfaceView.getHolder().setFixedSize(mPreviewWidth, mPreviewHeight);
            mSurfaceView.setAspectRatio(mPreviewAspectRatio);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mOnTouchListener != null) {
            //add RCS 30秒情况下,点击屏幕取消拍照暂停 liuxi 20180726 start
            if (mApp.providerStartPreView()==30000){
                mApp.removeStartPreviewHandler();
            }
            //add RCS 30秒情况下,点击屏幕取消拍照暂停 liuxi 20180726 end
            return mOnTouchListener.onTouch(v, event);
        } else {
            return false;
        }
    }

    protected  void removeTopSurface() {
        int queueSize = mFrameLayoutQueue.size();
        LogHelper.d(TAG, "removeTopSurface size = " + queueSize);
        if (isThirdPartyIntent(mApp.getActivity())) {
            View cover = mApp.getActivity().findViewById(R.id.preview_cover);
            cover.setVisibility(View.GONE);
        }
        for (int i = 0; i < queueSize; i++) {
            View view = mFrameLayoutQueue.poll();
            if (view != null) {
                view.setVisibility(View.GONE);
                mPreviewRoot.removeView(view);
            }
        }
        mLastPreviewContainer = null;
    }

    protected void setOnTouchListener(View.OnTouchListener listener) {
        mOnTouchListener = listener;
    }

    private void initSurfaceViewLayout() {
        if (mSurfaceView == null) {
            mPreviewContainer = (ViewGroup) mPreviewRoot
                    .findViewById(R.id.preview_container);
            mSurfaceView = (FixedAspectSurfaceView) mPreviewRoot
                    .findViewById(R.id.preview_surface);
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            mSurfaceChangeCallback = new SurfaceChangeCallback(null);
            surfaceHolder.addCallback(mSurfaceChangeCallback);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mSurfaceView.addOnLayoutChangeListener(mOnLayoutChangeCallback);
            mSurfaceView.setOnTouchListener(this);
        }
    }

    private void attachSurfaceView(ISurfaceStatusListener listener) {
        ViewGroup container = (ViewGroup) mApp.getActivity().getLayoutInflater().inflate(
                R.layout.preview_layout, null);
        FixedAspectSurfaceView surfaceView =
                (FixedAspectSurfaceView) container.findViewById(R.id.preview_surface);
        surfaceView.setVisibility(View.GONE);

        if (mLastPreviewContainer != null) {
           SurfaceView surface =
                   (SurfaceView) mLastPreviewContainer.findViewById(R.id.preview_surface);
            surface.removeOnLayoutChangeListener(mOnLayoutChangeCallback);
            surface.getHolder().removeCallback(mSurfaceChangeCallback);
            if (mSurfaceChangeCallback != null) {
                mSurfaceChangeCallback.surfaceDestroyed(surface.getHolder());
            }
            mLastPreviewContainer.bringToFront();
            if (!mFrameLayoutQueue.contains(mLastPreviewContainer)) {
                mFrameLayoutQueue.add(mLastPreviewContainer);
            }
        }

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        mSurfaceChangeCallback = new SurfaceChangeCallback(listener);
        surfaceHolder.addCallback(mSurfaceChangeCallback);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.addOnLayoutChangeListener(mOnLayoutChangeCallback);
        surfaceView.setOnTouchListener(this);
        mPreviewRoot.addView(container, 0);
        mPreviewContainer = container;
        mSurfaceView = surfaceView;
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    private View.OnLayoutChangeListener mOnLayoutChangeCallback
                                          = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                   int oldTop, int oldRight, int oldBottom) {
            mPreviewArea.set(left, top, right, bottom);
            // This method can be called during layout pass. We post a Runnable so
            // that the callbacks won't happen during the layout pass.
            mSurfaceView.post(new Runnable() {
                @Override
                public void run() {
                    notifyPreviewAreaChanged();
                    LogHelper.d(TAG, "onLayoutChange mPreviewArea = " + mPreviewArea);
                    mPreviewFrameLayout.setPreviewSize((int) mPreviewArea.width(),
                            (int) mPreviewArea.height());
                }
            });
        }
    };


    /**
     * Surface view surface change call back receiver, it bind a status change listener.
     * When surface status change, use the listener to notify the change.
     */
    private class SurfaceChangeCallback implements SurfaceHolder.Callback {
        private ISurfaceStatusListener mListener;

        public SurfaceChangeCallback(ISurfaceStatusListener listener) {
            mListener = listener;
        }

        public ISurfaceStatusListener getBindStatusListener() {
            return mListener;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mRenderer = new GLRendererImpl(mApp.getActivity());
            mProducerThread = new GLProducerThread(holder.getSurface(),
                    mRenderer, mRenderSyncLock);
            mProducerThread.start();
            synchronized (mRenderSyncLock) {
                try {
                    //Need wait for ever, or else other producer can not connect this BQ
                    mRenderSyncLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mIsSurfaceCreated = true;
            if (mListener != null && width == mPreviewWidth
                    && height == mPreviewHeight) {
                mListener.surfaceChanged(holder, width, height);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mIsSurfaceCreated = false;
            if (mListener != null) {
                mListener.surfaceDestroyed(holder);
            }
        }
    }

    private void notifyPreviewAreaChanged() {
        for (OnPreviewAreaChangedListener listener :
                mPreviewAreaChangedListeners) {
            listener.onPreviewAreaChanged(mPreviewArea, new Size(mPreviewWidth, mPreviewHeight));
        }
    }

    private boolean isThirdPartyIntent(Activity activity) {
        Intent intent = activity.getIntent();
        String action = intent.getAction();
        boolean value = MediaStore.ACTION_IMAGE_CAPTURE.equals(action) ||
                MediaStore.ACTION_VIDEO_CAPTURE.equals(action);
        return value;
    }
}
