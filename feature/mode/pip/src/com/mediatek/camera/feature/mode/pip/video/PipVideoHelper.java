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

package com.mediatek.camera.feature.mode.pip.video;

import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil.Tag;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.portability.CamcorderProfileEx;
import com.mediatek.camera.portability.MediaRecorderEx;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Video helper is utility class provide some common function.
 */
public class PipVideoHelper {
    private static final Tag TAG = new Tag(PipVideoHelper.class.getSimpleName());
    private static final int INVALID_DURATION = -1;
    private static final int FILE_ERROR = -2;
    public static final int MEDIA_INFO_BITRATE_ADJUSTED = 898;
    public static final int MEDIA_INFO_CAMERA_RELEASE = 1999;
    public static final int MEDIA_INFO_RECORDING_SIZE = 895;
    public static final int MEDIA_INFO_FPS_ADJUSTED = 897;
    public static final int MEDIA_INFO_START_TIMER = 1998;
    public static final int MEDIA_INFO_WRITE_SLOW = 899;
    public static final int MEDIA_ENCODER_ERROR = -1103;
    private final ICameraContext mCameraContext;

    /**
     * Construct a video helper.
     * @param cameraContext the camera context.
     */
    public PipVideoHelper(ICameraContext cameraContext) {
        mCameraContext = cameraContext;
    }

    /**
     * Filter video quality.
     * @param supportedQualities supported video qualities.
     * @param videoQuality the video quality.
     * @return the filtered video quality.
     */
    public String filterVideoQuality(@Nonnull List<String> supportedQualities,
                                       @Nonnull String videoQuality) {
        if (videoQuality == null ||
                supportedQualities == null || supportedQualities.size() <= 0) {
            return null;
        }
        if (supportedQualities.contains(videoQuality)) {
            return videoQuality;
        }
        return supportedQualities.get(0);
    }

    /**
     * Filter supported video quality by max and min resolution.
     *
     * @param originalSupportedQualities original supported qualities.
     * @param cameraId the camera id.
     * @param maxResolution max resolution.
     * @param minResolution min resolution.
     * @return the result supported qualities.
     */
    public HashMap<String, CamcorderProfile> filterSupportedVideoProfiles(
                        @Nonnull List<String> originalSupportedQualities,
                        @Nonnull String cameraId,
                        @Nonnull Size maxResolution,
                        @Nonnull Size minResolution) {
        HashMap<String, CamcorderProfile> resultSupportedVideoProfileMap = new HashMap<>();
        if (minResolution == null || maxResolution == null ||
                originalSupportedQualities == null || originalSupportedQualities.size() <= 0) {
            return resultSupportedVideoProfileMap;
        }
        for (String quality: originalSupportedQualities) {
            CamcorderProfile profile = CamcorderProfileEx.getProfile(Integer.parseInt(cameraId),
                    Integer.parseInt(quality));
            if (profile != null &&
                    profile.videoFrameWidth <= maxResolution.getWidth() &&
                    profile.videoFrameHeight <= maxResolution.getHeight() &&
                    profile.videoFrameWidth >= minResolution.getWidth() &&
                    profile.videoFrameHeight >= minResolution.getHeight()) {
                resultSupportedVideoProfileMap.put(profile.videoFrameWidth + "x" +
                        profile.videoFrameHeight, profile);
            }
        }
        return resultSupportedVideoProfileMap;
    }

    /**
     * Get video temp file path.
     * @return the video temp file path.
     */
    public String getVideoTempFilePath() {
        return mCameraContext.getStorageService().getFileDirectory()
                + '/' + "videorecorder" + ".3gp" + ".tmp";
    }

    /**
     * Get video size.
     * @param videoPath video path.
     * @return the video size.
     */
    public long getVideoSize(String videoPath) {
        return new File(videoPath).length();
    }

    /**
     * Get video duration.
     * @param fileName the file name.
     * @return the video duration.
     */
    public long getVideoDuration(String fileName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(fileName);
            return Long.valueOf(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (IllegalArgumentException e) {
            return INVALID_DURATION;
        } catch (RuntimeException e) {
            return FILE_ERROR;
        } finally {
            retriever.release();
        }
    }

    /**
     * Create video file title.
     * @param dateTaken date taken.
     * @return the video file title.
     */
    public String createVideoFileTitle(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat format = new SimpleDateFormat("'VID'_yyyyMMdd_HHmmss");
        return format.format(date);
    }

    /**
     * Create video file name.
     * @param title video file title.
     * @param fileFormat video file format.
     * @return the video file name.
     */
    public String createVideoFileName(String  title, int fileFormat) {
        String fileName = title + convertOutputFormatToFileExt(fileFormat);
        LogHelper.d(TAG, "[createFileName] fileName = " + fileName);
        return fileName;
    }

    /**
     * Convert output format to mime type.
     * @param outputFileFormat output format.
     * @return the mime type.
     */
    public String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    /**
     * set media recorder parameter.
     * @param mediaRecorder the media recorder.
     */
    public void setMediaRecorderParameterEx(MediaRecorder mediaRecorder) {
        if (mediaRecorder != null && MediaRecorderEx.isCanUseParametersExtra()) {
            MediaRecorderEx.setParametersExtra(mediaRecorder,
                    "media-recorder-info=" + MEDIA_INFO_BITRATE_ADJUSTED);
            MediaRecorderEx.setParametersExtra(mediaRecorder,
                    "media-recorder-info=" + MEDIA_INFO_CAMERA_RELEASE);
            MediaRecorderEx.setParametersExtra(mediaRecorder,
                    "media-recorder-info=" + MEDIA_INFO_FPS_ADJUSTED);
            MediaRecorderEx.setParametersExtra(mediaRecorder,
                    "media-recorder-info=" + MEDIA_INFO_START_TIMER);
            MediaRecorderEx.setParametersExtra(mediaRecorder,
                    "media-recorder-info=" + MEDIA_INFO_WRITE_SLOW);
        }
    }

    /**
     * use audio focus to stop audio play back when start recording.
     * @param app the IApp object.
     */
    public void pauseAudioPlayBack(IApp app) {
        LogHelper.i(TAG, "[pauseAudioPlayback]");
        AudioManager am = (AudioManager) app.getActivity().getSystemService(
                app.getActivity().AUDIO_SERVICE);
        if (am != null) {
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    /**
     * release audio focus after stop recording.
     * @param app the IApp object.
     */
    public void releaseAudioFocus(IApp app) {
        LogHelper.i(TAG, "[releaseAudioFocus]");
        AudioManager am = (AudioManager) app.getActivity().getSystemService(
                app.getActivity().AUDIO_SERVICE);
        if (am != null) {
            am.abandonAudioFocus(null);
        }
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            LogHelper.d(TAG, ".mp4");
            return ".mp4";
        }
        LogHelper.d(TAG, ".3gp");
        return ".3gp";
    }
}