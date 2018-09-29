/*  car eye 车辆管理平台 
 * car-eye管理平台   www.car-eye.cn
 * car-eye开源网址:  https://github.com/Car-eye-team
 * Copyright
 */

package com.sh.camera.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.sh.camera.socket.CommCenterUsers;
import com.sh.camera.socket.coder.CommEncoder;
import com.sh.camera.socket.model.VideoInfo;
import com.sh.camera.socket.utils.ParseUtil;

import android.annotation.SuppressLint;
import android.util.Log;

/**    
 *     
 * 项目名称：DSS_CAMERA    
 * 类名称：CameraFileUtil    
 * 类描述：摄像头文件工具类    
 * 创建人：Administrator    
 * 创建时间：2016年10月12日 下午8:18:13    
 * 修改人：Administrator    
 * 修改时间：2016年10月12日 下午8:18:13    
 * 修改备注：    
 * @version 1.0  
 *     
 */
@SuppressLint("SimpleDateFormat")
public class CommCameraFileUtil {

	/**
	 * 筛选视频文件
	 * @param stime 开始时间
	 * @param etime 结束时间
	 * @param cameraid 摄像头ID
	 */
	public synchronized static void screenVideoFile(final String starttime,final String edntime,final int cameraid){
		new Thread(new Runnable() {
			@Override
			public void run() {

				String FileName;
				FileName = CameraFileUtil.getRootFilePath() + Constants.CAMERA_PATH;
				File f = new File(FileName);
				ArrayList<HashMap<String, String>> filedata = null;
				HashMap<Integer, ArrayList<HashMap<String, String>>> datamap = new HashMap<Integer, ArrayList<HashMap<String, String>>>();

				if(f.exists()){
					try {
						File[] fs = f.listFiles();
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String stime = DateUtil.numberTodate(starttime);
						String etime = DateUtil.numberTodate(edntime);
						if(fs!=null&&fs.length>0){
							filedata = new ArrayList<HashMap<String, String>>();
							int j = 0;
							int k = 0;
							for (int i = 0; i < fs.length; i++) {
								String name = fs[i].getName();
								if(name.endsWith("mp4")){
									int cid = Integer.parseInt(name.split("-")[0]);
									if(cid == cameraid){
										File file = fs[i];

										//文件开始录制时间
										String eftime = sdf.format(new Date(file.lastModified()));
										//文件录制结束时间
										String utctime = name.split("-")[1].replace(".mp4", "");
										String sftime = DateUtil.utcTimestampToBjTime(utctime);

										//总时长
										int sumrec = DateUtil.secBetween(eftime, sftime);

										//判断文件结束时间与开始时间 结束时间比较
										int esec1 = DateUtil.secBetween(eftime, stime);
										int esec2 = DateUtil.secBetween(eftime, etime);

										int splaysec = 0;  //开始播放秒
										int eplaysec = 0;  //结束播放秒
										boolean flag = false;
										if(esec1 > 0 && esec2 < 0){

											int ssec = DateUtil.secBetween(sftime, stime);
											if(ssec > 0){
												splaysec = 0;
												eplaysec = sumrec;
											}else{
												splaysec = DateUtil.secBetween(stime, sftime);
												eplaysec = sumrec;
											}
											flag = true;
										}else if(esec1 > 0 && esec2 > 0){
											int esec = DateUtil.secBetween(sftime, etime);
											if(esec > 0 ){
												flag = false;
											}else{
												int ssec = DateUtil.secBetween(sftime, stime);
												if(ssec > 0){
													splaysec = 0;
													eplaysec = sumrec-DateUtil.secBetween(eftime, etime);
												}else{
													splaysec = DateUtil.secBetween(stime, sftime);
													eplaysec = DateUtil.secBetween(eftime, etime);
												}
												flag = true;
											}
										}

										if(flag){
											j++;
											HashMap<String, String> map = new HashMap<String, String>();
											map.put("cameraid", String.valueOf(cameraid));
											map.put("name", name);
											map.put("time", sftime);
											map.put("sord", String.valueOf(file.lastModified()));
											map.put("path", file.getAbsolutePath());
											map.put("type", name.substring(name.length()-3, name.length()));
											map.put("size", (file.length()/1024)+" KB");
											map.put("click", "0");
											map.put("splaysec", String.valueOf(splaysec));
											map.put("eplaysec", String.valueOf(eplaysec));
											filedata.add(map);

											if(j == 10){
												j = 0;
												datamap.put(k, filedata);
												filedata = new ArrayList<HashMap<String, String>>();
												k++;
											}
										}
									}
								}

								//最后一条记录
								if((i+1) == fs.length){
									if(filedata.size() > 0){
										j = 0;
										datamap.put(k, filedata);
										filedata = new ArrayList<HashMap<String, String>>();
										k++;
									}
								}
							}

							int datamapsize = datamap.size();
							for (int l = 0; l < datamapsize; l++) {
								ArrayList<HashMap<String, String>> data = datamap.get(l);
								if(data != null){
									int size = data.size();
									int len = 2;
									for (HashMap<String, String> lenmap : data) {
										//文件名称
										String name = lenmap.get("name");
										String filename = name;
										byte [] filenamebyte = Tools.stringToByte(filename);
										int filenamelen = filenamebyte.length;
										len += filenamelen;
										len += 10;
									}

									byte[] body = new byte[len];	
									int dstPos = 0;
									//通道ID
									System.arraycopy(Tools.sortToByte(Tools.int2Bytes(cameraid, 1)), 0, body, dstPos, 1);
									dstPos+=1;
									//文件数据
									System.arraycopy(Tools.sortToByte(Tools.int2Bytes(size, 1)), 0, body, dstPos, 1);
									dstPos+=1;

									for (HashMap<String, String> map : data) {
										String name = map.get("name");
										int id = Integer.parseInt(name.split("-")[0]);
										String filename = name;
										byte [] filenamebyte = Tools.stringToByte(filename);
										int filenamelen = filenamebyte.length;
										int splaysec = Integer.parseInt(map.get("splaysec"));
										int eplaysec = Integer.parseInt(map.get("eplaysec"));

										//文件名
										System.arraycopy(filenamebyte, 0, body, dstPos, filenamelen);
										dstPos+=filenamelen;
										System.arraycopy(Tools.sortToByte(Tools.int2Bytes(splaysec, 4)), 0, body, dstPos, 4);
										dstPos+=4;
										System.arraycopy(Tools.sortToByte(Tools.int2Bytes(eplaysec, 4)), 0, body, dstPos, 4);
										dstPos+=4;
										//总包数
										System.arraycopy(Tools.sortToByte(Tools.int2Bytes(datamap.size(), 1)), 0, body, dstPos, 1);
										dstPos+=1;
										//分包数
										System.arraycopy(Tools.sortToByte(Tools.int2Bytes((l+1), 1)), 0, body, dstPos, 1);
										dstPos+=1;
									}

									Thread.sleep(200);
									//组装文件列表发送至服务器
									/*Intent intent = new Intent("com.dss.camera.ACTION_VIDEO_PLAYBACK_LIST");
									intent.putExtra("EXTRA_DATA", Tools.parseByte2HexStr(body));
									context.sendBroadcast(intent);*/
									//发送至服务器
									byte[] cameradata = CommEncoder.getVideoPlayBackList(body);
									CommCenterUsers.witeMsg(cameradata,1);

									//CameraUtil.startVideoFileStream(context, data);
								}
							}

						}

					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
			}}).start();
	}
	
	/**
	 * 筛选视频文件
	 * @param stime 开始时间
	 * @param etime 结束时间
	 * @param cameraid 摄像头ID
	 */
	public synchronized static void screenVideoFile1078(final String starttime,final String edntime,final int cameraid,final int seq){
		new Thread(new Runnable() {
			@Override
			public void run() {

				String FileName;
				FileName = CameraFileUtil.getRootFilePath() + Constants.CAMERA_PATH;
				File f = new File(FileName);
				ArrayList<HashMap<String, String>> filedata = null;
				HashMap<Integer, ArrayList<HashMap<String, String>>> datamap = new HashMap<Integer, ArrayList<HashMap<String, String>>>();

				if(f.exists()){
					try {
						File[] fs = f.listFiles();
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String stime = DateUtil.numberTodate(starttime);
						String etime = DateUtil.numberTodate(edntime);
						Log.d("vedio", "start file list"+stime+"endtime"+etime);

						if(fs!=null&&fs.length>0){
							filedata = new ArrayList<HashMap<String, String>>();
							int k = 0;
							List<VideoInfo> list = new ArrayList<VideoInfo>();
							for (int i = 0; i < fs.length; i++) {
								String name = fs[i].getName();
								if(name.endsWith("mp4")){
									int cid = Integer.parseInt(name.split("-")[0]);
									if(cid == cameraid){
										
										File file = fs[i];

										//文件开始录制时间
										String eftime = sdf.format(new Date(file.lastModified()));
										//文件录制结束时间
										String utctime = name.split("-")[1].replace(".mp4", "");
										String sftime = DateUtil.utcTimestampToBjTime(utctime);
										
										//总时长
										int sumrec = DateUtil.secBetween(eftime, sftime);
										//判断文件结束时间与开始时间 结束时间比较
										int esec1 = DateUtil.secBetween(eftime, stime);
										int esec2 = DateUtil.secBetween(eftime, etime);

										int splaysec = 0;  //开始播放秒
										int eplaysec = 0;  //结束播放秒
										boolean flag = false;
										if(esec1 > 0 && esec2 < 0){

											int ssec = DateUtil.secBetween(sftime, stime);
											if(ssec > 0){
												splaysec = 0;
												eplaysec = sumrec;
											}else{
												splaysec = DateUtil.secBetween(stime, sftime);
												eplaysec = sumrec;
											}
											flag = true;
										}else if(esec1 > 0 && esec2 > 0){
											int esec = DateUtil.secBetween(sftime, etime);
											if(esec > 0 ){
												flag = false;
											}else{
												int ssec = DateUtil.secBetween(sftime, stime);
												if(ssec > 0){
													splaysec = 0;
													eplaysec = sumrec-DateUtil.secBetween(eftime, etime);
												}else{
													splaysec = DateUtil.secBetween(stime, sftime);
													eplaysec = DateUtil.secBetween(eftime, etime);
												}
												flag = true;
											}
										}

										if(flag){
											VideoInfo videoItems = new VideoInfo();
											//逻辑通道号
											videoItems.setLogicChannel(cameraid);
											//开始时间
											videoItems.setStartTime(sftime);
											//结束时间
											videoItems.setEndTime(eftime);
											//音视频资源类型 0：音视频 1：音频 2：视频 3视频或音视频
											videoItems.setMediaType(2);
											//码流类型 0：所有码流 1：主码流 2 子码流
											videoItems.setStreamType(1);
											//存储类型 0：所有存储器 1：主存储器 2：灾备服务器
											videoItems.setMemoryType(1);
											//文件大小
											int filesize = (int) (file.length()/1024);
											videoItems.setSize(filesize);
											list.add(videoItems);
										}
									}
								}

								//最后一条记录
								if((i+1) == fs.length){
									if(filedata.size() > 0){
										datamap.put(k, filedata);
										filedata = new ArrayList<HashMap<String, String>>();
										k++;
									}
								}
							}
							
							//发送文件数据
							byte[] cameradata = CommEncoder.getAudioVideoResourceList(seq, list);
							CommCenterUsers.witeMsg(cameradata,1);
							Log.d("vedio", "cameradata"+Tools.parseByte2HexStr(cameradata));
						}

					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
			}}).start();
	}




	public static File SearchFile( String starttime, String edntime, int cameraid){

		String FileName;
		FileName = CameraFileUtil.getRootFilePath() + Constants.CAMERA_PATH;
		File f = new File(FileName);
		ArrayList<HashMap<String, String>> filedata = null;
		HashMap<Integer, ArrayList<HashMap<String, String>>> datamap = new HashMap<Integer, ArrayList<HashMap<String, String>>>();

		if(f.exists()){
			try {
				File[] fs = f.listFiles();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String stime = DateUtil.numberTodate(starttime);
				String etime = DateUtil.numberTodate(edntime);
				Log.d("vedio", "start file list"+stime+"endtime"+etime);

				if(fs!=null&&fs.length>0){
					filedata = new ArrayList<HashMap<String, String>>();
					int k = 0;
					for (int i = 0; i < fs.length; i++) {
						String name = fs[i].getName();
						if(name.endsWith("mp4")){
							int cid = Integer.parseInt(name.split("-")[0]);
							if(cid == cameraid){
								File file = fs[i];
								//文件开始录制时间
								String eftime = sdf.format(new Date(file.lastModified()));
								//文件录制结束时间
								String utctime = name.split("-")[1].replace(".mp4", "");
								String sftime = DateUtil.utcTimestampToBjTime(utctime);
								//总时长
								int sumrec = DateUtil.secBetween(sftime, sftime);
								//判断文件结束时间与开始时间 结束时间比较
								int esec1 = DateUtil.secBetween(sftime, stime);
								int esec2 = DateUtil.secBetween(eftime, etime);
								boolean flag = false;
								if(esec1 >= 0 && esec2 <= 0){
									flag = true;
								}
								if(flag){
									return file;
								}
							}
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		return null;
	}
}