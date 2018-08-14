/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensor. Without
 * the prior written permission of MediaTek inc. and/or its licensor, any
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
 * NON-INFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
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
package com.mediatek.camera.feature.mode.pip.device;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.exif.ExifInterface;
import com.mediatek.camera.common.jpeg.JpegDecoder;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.pip.pipwrapping.IPipCaptureWrapper;
import com.mediatek.camera.portability.jpeg.encoder.JpegEncoder;
import com.mediatek.camera.portability.jpeg.encoder.JpegEncoder.JpegCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.IllegalFormatException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Pip Capture executor.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class PipCaptureExecutor {
    private static final Tag TAG = new Tag(PipCaptureExecutor.class.getSimpleName());
    private static final int NUM_FOR_SINGLE_CAPTURE = 2;
    private static final int MAX_PENDING_CAPTURE_COUNT = 2;
    private final Context mContext;
    private final JpegHeaderWrapper mJpegHeaderWrapper;
    private final IPipCaptureWrapper mPipCaptureWrapper;
    private final HandlerThread mImageReceiveThread;
    private final Handler mImageReceiveHandler;
    private final Executor mImageProcessExecutor;
    private final BlockingQueue<Runnable> mBlockingQueue;
    // A queue used to make a strong reference to CaptureInitRunnable instance
    // otherwise,CaptureInitRunnable instance may be GC by FinalizerDaemon.
    private final BlockingQueue<CaptureInitRunnable> mCaptureInitRunnableQueue;

    private int mImageOffered = 0;
    private ImageReader mImageReader;
    private Object mImageReaderLock = new Object();
    private ConditionVariable mImageReaderSync;
    private byte[] mCurrentJpegHeader;
    private boolean mReleased;

    /**
     * Pip capture executor.
     * @param context the context.
     * @param captureWrapper the capture wrapper.
     */
    public PipCaptureExecutor(Context context, IPipCaptureWrapper captureWrapper) {
        mContext = context;
        mPipCaptureWrapper = captureWrapper;

        mJpegHeaderWrapper = new JpegHeaderWrapper();
        mImageReceiveThread = new HandlerThread("Pip-Image-Receive");
        mImageReceiveThread.start();
        mImageReceiveHandler = new Handler(mImageReceiveThread.getLooper());

        mBlockingQueue = new LinkedBlockingQueue<>();
        mCaptureInitRunnableQueue = new LinkedBlockingQueue<>();
        mImageProcessExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS , mBlockingQueue);
        mImageReaderSync = new ConditionVariable();
        mImageReaderSync.open();
    }

    /**
     * Init pip capture executor.
     */
    public void init() {
        if (!mReleased) {
            return;
        }
        LogHelper.i(TAG, "init");
        mReleased = false;
        mBlockingQueue.clear();
        mCaptureInitRunnableQueue.clear();
    }

    /**
     * Set up capture.
     * @param bottomJpegSize bottom jpeg size.
     * @param topJpegSize top jpeg size.
     */
    public void setUpCapture(Size bottomJpegSize, Size topJpegSize) {
        LogHelper.d(TAG, "setUpCapture" + " released:" + mReleased);
        if (mReleased) {
            return;
        }
        CaptureInitRunnable r = new CaptureInitRunnable(bottomJpegSize, topJpegSize);
        try {
            mCaptureInitRunnableQueue.put(r);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mImageProcessExecutor.execute(r);
    }

    /**
     * Offer jpeg data buffer.
     * @param jpegData jpeg data.
     * @param isBottom is bottom jpeg.
     */
    public void offerJpegData(byte[] jpegData, boolean isBottom) {
        LogHelper.i(TAG, "[offerJpegData]+ isBottom:" + isBottom
                + ",pending size:" + mBlockingQueue.size()
                + "released:" + mReleased + ",mImageOffered:" + mImageOffered);
        if (mReleased || mCaptureInitRunnableQueue.size() == 0) {
            LogHelper.i(TAG, "offerJpegData ignore, released:" + mReleased);
            return;
        }
        mImageOffered++;
        mImageProcessExecutor.execute(new JpegProcessingRunnable(jpegData, isBottom));
        if (mImageOffered == NUM_FOR_SINGLE_CAPTURE) {
            mImageOffered = 0;
            if (!blockingWhenMaxCaptureCountReached(MAX_PENDING_CAPTURE_COUNT,
                    false/*non-blocking*/)) {
                mPipCaptureWrapper.unlockNextCapture();
            }
        }
        LogHelper.i(TAG, "[offerJpegData]-");
    }

    /**
     * Un init pip capture executor.
     */
    public void unInit() {
        LogHelper.d(TAG, "[unInit]+");
        mReleased = true;
        blockingWhenMaxCaptureCountReached(0, true/*blocking*/);
        releaseImageReader();
        mPipCaptureWrapper.unInitCapture();
        mCurrentJpegHeader = null;
        mImageOffered = 0;
        mCaptureInitRunnableQueue.clear();
        LogHelper.i(TAG, "[unInit]- CaptureInit RunnableQueue size:" +
            mCaptureInitRunnableQueue.size() + " mBlockingQueue size:" + mBlockingQueue.size());
    }

    private Surface setUpImageReader(int width, int height, int format) {
        LogHelper.d(TAG, "[setUpImageReader]+");
        mImageReaderSync.block();
        synchronized (mImageReaderLock) {
            mImageReader = ImageReader.newInstance(width, height, format, 1);
            mImageReader.setOnImageAvailableListener(mImageAvailableListener, mImageReceiveHandler);
            mImageReaderSync.close();
            LogHelper.i(TAG, "[setUpImageReader]-");
            return mImageReader.getSurface();
        }
    }

    private void releaseImageReader() {
        LogHelper.d(TAG, "[releaseImageReader]+");
        synchronized (mImageReaderLock) {
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        }
        mImageReaderSync.open();
        LogHelper.d(TAG, "[releaseImageReader]-");
    }

    private void updateJpegHeader(byte[] jpegHeader) {
        mCurrentJpegHeader = jpegHeader;
    }

    private OnImageAvailableListener mImageAvailableListener = new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            LogHelper.i(TAG, "[onImageAvailable]+ mCurrentJpegHeader:" + mCurrentJpegHeader);
            mCaptureInitRunnableQueue.remove();
            synchronized (mImageReaderLock) {
                if (mImageReader != null) {
                    Image image = reader.acquireNextImage();
                    int format = image.getFormat();
                    byte[] jpegData = null;
                    if (ImageFormat.JPEG == format) {
                        jpegData = CameraUtil.acquireJpegBytesAndClose(image);
                        if (mCurrentJpegHeader != null) {
                            try {
                                jpegData = mJpegHeaderWrapper.writeJpegHeader(
                                        jpegData, mCurrentJpegHeader);
                                jpegData = setJpegRotationToZeroInExif(jpegData);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mCurrentJpegHeader = null;
                        }
                    }
                    if (jpegData != null) {
                        mPipCaptureWrapper.onPictureTaken(jpegData);
                        jpegData = null;
                    }
                }
            }
            releaseImageReader();
            LogHelper.d(TAG, "[onImageAvailable]- mImageReader: " + mImageReader);
        }
    };

    private JpegCallback mCaptureJpegCallback = new JpegCallback() {
        @Override
        public void onJpegAvailable(byte[] jpegData) {
            LogHelper.i(TAG, "mCaptureJpegCallback [onJpegAvailable]+");
            mCaptureInitRunnableQueue.remove();
            if (mCurrentJpegHeader != null) {
                try {
                    jpegData = mJpegHeaderWrapper.writeJpegHeader(jpegData, mCurrentJpegHeader);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCurrentJpegHeader = null;
            }
            mPipCaptureWrapper.onPictureTaken(jpegData);
            LogHelper.d(TAG, "mCaptureJpegCallback [onJpegAvailable]-");
        }
    };

    /**
     * Capture init runnable.
     */
    private class CaptureInitRunnable implements Runnable {
        // private static final Tag TAG = new Tag("CaptureInitRunnable");
        private Size mBottomSz;
        private Size mTopSz;
        private JpegEncoder mJpegEncoder;
        public CaptureInitRunnable(Size bottomSz, Size topSz) {
            mBottomSz = bottomSz;
            mTopSz = topSz;
        }

        @Override
        public void run() {
            LogHelper.i(TAG, "CaptureInitRunnable [run]+");
            // Make config pipeline: Buffer ==> GPU ==> JpegEncoder ==> BitStream
            Surface jpegInputSurface;
            if (JpegEncoder.isHwEncoderSupported(mContext)) {
                Surface jpegOutputSurface = setUpImageReader(mBottomSz.getWidth(),
                        mBottomSz.getHeight(), ImageFormat.JPEG);
                mJpegEncoder = JpegEncoder.newInstance(mContext, true);

                int pixelFormat = mPipCaptureWrapper.initCapture(
                        mJpegEncoder.getSupportedInputFormats());
                jpegInputSurface = mJpegEncoder.configInputSurface(jpegOutputSurface,
                        mBottomSz.getWidth(), mBottomSz.getHeight(), pixelFormat);
            } else {
                mJpegEncoder = JpegEncoder.newInstance(mContext, false);
                int pixelFormat = mPipCaptureWrapper.initCapture(
                        mJpegEncoder.getSupportedInputFormats());
                jpegInputSurface = mJpegEncoder.configInputSurface(mCaptureJpegCallback,
                        mBottomSz.getWidth(), mBottomSz.getHeight(), pixelFormat);
            }

            mPipCaptureWrapper.setCaptureSurface(jpegInputSurface);
            mPipCaptureWrapper.setCaptureSize(mBottomSz, mTopSz);
            mPipCaptureWrapper.keepCaptureRotation();

            mJpegEncoder.startEncodeAndReleaseWhenDown();
            LogHelper.d(TAG, "CaptureInitRunnable [run]-");
        }
    }

    /**
     * Processing runnable.
     */
    private class JpegProcessingRunnable implements Runnable {
        // private static final Tag TAG = new Tag("JpegProcessingRunnable");
        private byte[] mJpegData;
        private boolean mIsBottom;
        private JpegDecoder mJpegDecoder;
        public JpegProcessingRunnable(byte[] jpegData, boolean isBottom) {
            mJpegData = jpegData;
            mIsBottom = isBottom;
        }
        @Override
        public void run() {
            LogHelper.i(TAG, "JpegProcessingRunnable [run]+ isBottom:" + mIsBottom);
            mJpegDecoder = JpegDecoder.newInstance(mIsBottom ?
                    mPipCaptureWrapper.getBottomCapSt().getSurfaceTexture() :
                    mPipCaptureWrapper.getTopCapSt().getSurfaceTexture());
            mPipCaptureWrapper.setJpegRotation(mIsBottom,
                    CameraUtil.getOrientationFromExif(mJpegData));
            if (mIsBottom) {
                try {
                    updateJpegHeader(mJpegHeaderWrapper.readJpegHeader(mJpegData));
                } catch (IllegalFormatException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mJpegDecoder.decode(mJpegData);
            mJpegDecoder.release();
            mJpegDecoder = null;
            mJpegData = null;
            LogHelper.d(TAG, "JpegProcessingRunnable [run]-");
        }
    }

    // pip GPU will rotate buffer if need, here remove original jpeg's rotation
    private byte[] setJpegRotationToZeroInExif(byte[] sourceJpeg) {
        ExifInterface exif = new ExifInterface();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            exif.readExif(sourceJpeg);
            exif.setTagValue(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.Orientation.TOP_LEFT);
            // Exif thumbnail is single camera
            // delete Exif thumbnail and use original jpeg
            exif.removeCompressedThumbnail();
            exif.writeExif(sourceJpeg, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private boolean blockingWhenMaxCaptureCountReached(int maxCaptureCount, boolean sync) {
        // one pip capture need 3 runnable
        if (mBlockingQueue.size() >= maxCaptureCount * 3) {
            if (sync) {
                waitDone(mImageProcessExecutor);
            } else {
                waitDoneAsync(mImageProcessExecutor);
            }
            return true;
        }
        return false;
    }

    private boolean waitDone(Executor executor) {
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
            executor.execute(unlockRunnable);
            try {
                waitDoneLock.wait();
            } catch (InterruptedException ex) {
                LogHelper.e(TAG, "waitDone interrupted");
                return false;
            }
        }
        return true;
    }

    private void waitDoneAsync(Executor executor) {
        final Runnable unlockRunnable = new Runnable() {
            @Override
            public void run() {
                LogHelper.d(TAG, "waitDoneAsync comes!");
                mPipCaptureWrapper.unlockNextCapture();
            }
        };
        executor.execute(unlockRunnable);
    }
}