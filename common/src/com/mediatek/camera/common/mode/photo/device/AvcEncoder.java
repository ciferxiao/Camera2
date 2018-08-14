package com.mediatek.camera.common.mode.photo.device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import java.nio.ByteBuffer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
/*
 *xiao add for RCS UVC socket data change 
 */
public class AvcEncoder {
	private static String TAG = "N710_AvcEncoder";
	int m_width = 0;
	int m_height = 0;
	int m_framerate = 0;
	MediaCodec mediaCodec;
	byte[] m_info = null;  
    //转成后的数据  
    private byte[] yuv420 = null;  
    //pts时间基数  
    long presentationTimeUs = 0;  
    
    
	
	public AvcEncoder(int width, int height, int framerate, int bitrate) {   
	    Log.e(TAG, "AvcEncoder start");	      
	    m_width  = width;  
	    m_height = height;  
	    m_framerate = framerate;  
	    MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);  
	    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);      
	    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);  
	    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);  
	    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);  
	    try {  
	        mediaCodec = MediaCodec.createEncoderByType("video/avc");  
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block  
	        e.printStackTrace();  
	    }  
	    mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);  
	    mediaCodec.start();  
	    
	} 
	
	
    private void NV21ToNV12(byte[] nv21,byte[] nv12,int width,int height){  
        if(nv21 == null || nv12 == null) 
        	return;  
        
        int framesize = width*height;  
        Log.e(TAG, "offerEncoder NV21ToNV12 framesize=" + framesize + " nv21.length=" + nv21.length + " nv12.length=" + nv12.length );
        
        int i = 0,j = 0;  
        System.arraycopy(nv21, 0, nv12, 0, framesize);  
        for(i = 0; i < framesize; i++){  
            nv12[i] = nv21[i];  
        }  
        for (j = 0; j < framesize/2; j+=2)  
        {  
          nv12[framesize + j-1] = nv21[j+framesize];  
        }  
        for (j = 0; j < framesize/2; j+=2)  
        {  
          nv12[framesize + j] = nv21[j+framesize-1];  
        }  
    } 
    
        
    
    private long computePresentationTime(long frameIndex) {  
     	return 132 + frameIndex * 1000000 / m_framerate;  
 	}  
 	
 	
 	public int offerEncoder(byte[] input, byte[] output) {
        int pos = 0;
        
        /*  
        //这里根据你设置的采集格式调用。我这里是nv21  
        //swapYV12toI420(input, yuv420, m_width, m_height);  
        NV21ToNV12(input, rotateYuv420, m_width, m_height);  
        //把视频逆时针旋转90度。（正常视觉效果）  
        
        */ 
        byte[] yuv420 = new byte[m_width*m_height*3/2];
        NV21ToNV12(input, yuv420, m_width, m_height);
        
        //xiao test
        //YV12toNV12(input, yuv420, m_width, m_height);
        
        try {  
            @SuppressWarnings("deprecation")  
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();  
            @SuppressWarnings("deprecation")  
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();  
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);  
            Log.e(TAG, "offerEncoder inputBufferIndex=" + inputBufferIndex);
            if (inputBufferIndex >= 0) {  
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];  
                inputBuffer.clear();  
                inputBuffer.put(yuv420);  
  
                //计算pts，这个值是一定要设置的  
                long pts = computePresentationTime(presentationTimeUs);  
  
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, pts, 0);  
                presentationTimeUs += 1;  
            }  
  
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();  
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);  
  			Log.e(TAG, "offerEncoder aaa outputBufferIndex=" + outputBufferIndex);
            while (outputBufferIndex >= 0) {  
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];  
                byte[] outData = new byte[bufferInfo.size];  
                outputBuffer.get(outData);  
  
  				Log.e(TAG, "offerEncoder m_info=" + m_info);
                if (m_info != null) {  
                    System.arraycopy(outData, 0, output, pos, outData.length);  
                    pos += outData.length;  
  					Log.e(TAG, "offerEncoder outData.length=" + outData.length + " pos=" + pos);
                } else {  
                    //保存pps sps 只有开始时 第一个帧里有， 保存起来后面用  
                    int i = 0;
                    for (i = 0; i < outData.length; i++)
                    	Log.e(TAG, "offerEncoder pps or sps outData[" + i + "]=" + Integer.toHexString(outData[i]));
                    	
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);  
                    
                    if (spsPpsBuffer.getInt() == 0x00000001) {  
                    	Log.e(TAG, "offerEncoder spsPpsBuffer.getInt()=0x00000001");
                        m_info = new byte[outData.length];  
                        System.arraycopy(outData, 0, m_info, 0, outData.length);  
                    } else {  
                        return -1;  
                    }  
                }  
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);  
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);  
                Log.e(TAG, "offerEncoder bbb outputBufferIndex=" + outputBufferIndex);
            }  
  
            if (output[4] == 0x65) {  
            	Log.e(TAG, "offerEncoder output[4] = 0x65");
                //key frame   编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上  
                System.arraycopy(output, 0, yuv420, 0, pos);  
                System.arraycopy(m_info, 0, output, 0, m_info.length);  
                System.arraycopy(yuv420, 0, output, m_info.length, pos);  
                pos += m_info.length;  
            }  
  
        } catch (Throwable t) {  
            t.printStackTrace();  
        }  
  
  		Log.e(TAG, "offerEncoder finish, pos=" + pos);
        return pos;  
    }  
    
    private byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight){
       byte [] yuv = new byte[imageWidth*imageHeight*3/2];
       int i = 0;
       int count = 0;

       for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
          yuv[count] = data[i];
          count++;
       }

       i = imageWidth * imageHeight * 3 / 2 - 1;
       for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
             * imageHeight; i -= 2) {
          yuv[count++] = data[i - 1];
          yuv[count++] = data[i];
       }
       return yuv;
    }
    
    //旋转180度（顺时逆时结果是一样的）
    private void YUV420spRotate180(byte[] src, byte[] des, int width, int height) {

        int n = 0;
        int uh = height >> 1;
        int wh = width * height;
        //copy y
        for (int j = height - 1; j >= 0; j--) {
            for (int i = width - 1; i >= 0; i--) {
                des[n++] = src[width * j + i];
            }
        }


        for (int j = uh - 1; j >= 0; j--) {
            for (int i = width - 1; i > 0; i -= 2) {
                des[n] = src[wh + width * j + i - 1];
                des[n + 1] = src[wh + width * j + i];
                n += 2;
            }
        }
    }
    
}
