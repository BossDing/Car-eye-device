/*  car eye 车辆管理平台 
 * car-eye管理平台   www.car-eye.cn
 * car-eye开源网址:  https://github.com/Car-eye-team
 * Copyright
 */
package com.sh.camera.codec;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.push.hw.EncoderDebugger;
import org.push.hw.NV21Convertor;
import org.push.push.AudioDecoder;
//import org.push.push.AudioDecoder;

import android.content.Intent;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sh.camera.audio.AudioStream;
import com.sh.camera.service.MainService;
import com.sh.camera.util.AppLog;
import com.sh.camera.util.CameraFileUtil;
import com.sh.camera.util.Constants;

/**    
 *     
 * 项目名称：SH_CAMERA    
 * 类名称：MediaCodecManager    
 * 类描述：    
 * 创建人：Administrator    
 * 创建时间：2016年10月18日 上午11:56:29    
 * 修改人：Administrator    
 * 修改时间：2016年10月18日 上午11:56:29    
 * 修改备注：    
 * @version 1.0  
 *     
 */
public class MediaCodecManager {

	private static final String TAG = "MediaCodecManager";
	private static MediaCodecManager instance;
	public static NV21Convertor mConvertor;
	//解码
	private int previewFormat;
	public static MediaCodec[] mMediaCodec;
	public static boolean TakePicture = false;	
	private static boolean sw_codec	= false;
	private VideoConsumer[] mVC={null, null, null, null};	
	private static boolean RECODER_PUSHING	= true;
	public static int CAMERA_OPER_MODE = 1;
	private EncoderDebugger debugger;
	private AudioStream audioStream;
	private int m_type = 0;

	private static AudioDecoder audioDecoder;



	public static MediaCodecManager getInstance() {
		if (instance == null) {
			//mMediaCodec = new MediaCodec[Constants.MAX_NUM_OF_CAMERAS];			
  			audioDecoder = new AudioDecoder(MainService.c);
			instance = new MediaCodecManager();
		}
		return instance;
	}
	/**
	 * 释放解码器资源
	 * @param index
	 */

	public static  void PrepareTakePicture()
	{
		TakePicture = true;
	}

	public void StartUpload(int index, Camera camera, long handle, int type)

 {
	 m_type = type;
	 if(type == 0) {
 	 debugger = EncoderDebugger.debug(MainService.getInstance(), Constants.UPLOAD_VIDEO_WIDTH, Constants.UPLOAD_VIDEO_HEIGHT);
	 previewFormat = sw_codec ? ImageFormat.YV12 : debugger.getNV21Convertor().getPlanar() ? ImageFormat.YV12 : ImageFormat.NV21;    	 
	 mVC[index] = new HWConsumer(MainService.getInstance(), MainService.mPusher,handle);
	  try {
			 mVC[index].onVideoStart(Constants.UPLOAD_VIDEO_WIDTH, Constants.UPLOAD_VIDEO_HEIGHT);
		 } catch (IOException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }
	 if(Constants.AudioRecord && audioStream == null)
	 {	
		  audioStream = new AudioStream(MainService.mPusher, null, handle);
          audioStream.startRecord();
          if(type == 1)
		  {
		  	//开始语音对讲
			  try {
				  audioDecoder.startPlay();
			  } catch (IOException e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  }

		  }
	 }	 	
 }
 public void StopUpload(int index, int talk)
 {
 	if(talk==0) {
		if (mVC[index] != null) {
			mVC[index].onVideoStop();
			mVC[index] = null;
		}
	}else
	{
		audioDecoder.stopdecoder();
	}
	 if (audioStream != null) {
		 audioStream.stop();
		 audioStream = null;
	 }
 }
 public void DecodeAAC(byte[] data)
 {
	 audioDecoder.decode(data,0,data.length);
 }
	public static boolean TakePicture(int cameraid,int type){
		try {
			//检查SD卡是否存在			
			CAMERA_OPER_MODE = type;
			if(CAMERA_OPER_MODE == 1){
				//if(!SdCardUtil.checkSdCardUtil()){
				if(MainService.getDiskManager().getDiskCnt()<=0){
					AppLog.d(TAG, "SD卡不存在");
					return false;
				}
			}else
			{
				File f = new File(Constants.SNAP_FILE_PATH);
				if(!f.exists()){
					f.mkdirs();
				}
			}
		
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
		Camera camera = MainService.camera[cameraid];
		if(MainService.rules.length>0&&camera!=null){
			//MainService.picid = MainService.rules[index];
			MainService.picid = cameraid;				
			Log.d("CMD", " cameraid"+cameraid);			
			MainService.getInstance().setCallback(cameraid, camera);
			TakePicture  = true;
			return true;
		}		
		return false;
	}

public void onPreviewFrameUpload(byte[] data,int index,Camera camera){	
		
	 if (data == null ) {
		 camera.addCallbackBuffer(data);
         return;
     }	
     Camera.Size previewSize = camera.getParameters().getPreviewSize();
     if (data.length != Constants.UPLOAD_VIDEO_HEIGHT * Constants.UPLOAD_VIDEO_WIDTH * 3 / 2) {
    	 camera.addCallbackBuffer(data);
    	 return;
     }   
     MainService.getInstance().SetPreviewValid(index);
     if(TakePicture && MainService.picid == index )
     {
		 TakePicture = false;
		 CameraFileUtil.saveJpeg_snap(
				 index,
				 data,
				 Constants.UPLOAD_VIDEO_WIDTH,
				 Constants.UPLOAD_VIDEO_HEIGHT);
	 }


     if(mVC[index]!= null)
     {
    	 mVC[index].onVideo(data, previewFormat);
     }else
     {   	
    	 camera.setPreviewCallback(null);    
     }
     camera.addCallbackBuffer(data);      
 }

}
