/*
 * Copyright (C) 2011-2012 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.tudelft.majorkernelpanic.streaming.video;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.tudelft.majorkernelpanic.mp4.MP4Config;
import com.tudelft.majorkernelpanic.rtp.H264Packetizer;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * This will stream H264 from the camera over RTP
 * Call setDestination() & setVideoSize() & setVideoFrameRate() & setVideoEncodingBitRate() and you're good to go
 * You can then call prepare() & start()
 */
public class H264Stream extends VideoStream {

	static private SharedPreferences settings = null;
	
	private Semaphore lock = new Semaphore(0);
	private MP4Config mp4Config;
	
	public H264Stream(int cameraId) {
		super(cameraId);
		setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		this.packetizer = new H264Packetizer();
	}
	
	static public void setPreferences(SharedPreferences prefs) {
		settings = prefs;
	}
	
	// Should not be called by the UI thread
	private MP4Config testH264() throws IllegalStateException, IOException {
		if (!qualityHasChanged && mp4Config!=null) return mp4Config;
		
		final String TESTFILE = "/sdcard/spydroid-test.mp4";
		
		Log.i(TAG,"Testing H264 support...");
		
		// Save flash state & set it to false so that led remains off while testing h264
		boolean savedFlashState = flashState;
		flashState = false;
		
		// That means the H264Stream will behave as a regular MediaRecorder object
		// it will not start the packetizer thread and can be used to save the video
		// in a file
		setMode(MODE_DEFAULT);
		
		setOutputFile(TESTFILE);
		
		// Start recording
		prepare();
		start();
		
		// We wait a little and stop recording
		this.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			public void onInfo(MediaRecorder mr, int what, int extra) {
				Log.d(TAG,"MediaRecorder callback called !");
				if (what==MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
					Log.d(TAG,"MediaRecorder: MAX_DURATION_REACHED");
				} else if (what==MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					Log.d(TAG,"MediaRecorder: MAX_FILESIZE_REACHED");
				} else if (what==MEDIA_RECORDER_INFO_UNKNOWN) {
					Log.d(TAG,"MediaRecorder: INFO_UNKNOWN");
				} else {
					Log.d(TAG,"WTF ?");
				}
				lock.release();
			}
		});
		
		try {
			if (lock.tryAcquire(6,TimeUnit.SECONDS)) {
				Log.d(TAG,"MediaRecorder callback was called :)");
				Thread.sleep(400);
			} else {
				Log.d(TAG,"MediaRecorder callback was not called :(");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			stop();
		}
		
		
		// Retrieve SPS & PPS & ProfileId with MP4Config
		mp4Config = new MP4Config(TESTFILE);

		// Delete dummy video
		File file = new File(TESTFILE);
		if (!file.delete()) Log.e(TAG,"Temp file could not be erased");
		
		// Back to streaming mode & prepare
		setMode(MODE_STREAMING);
		
		// Restore flash state
		flashState = savedFlashState;
		
		Log.i(TAG,"H264 Test succeded...");
		
		// Save test result
		if (settings != null) {
			Editor editor = settings.edit();
			editor.putString(quality.frameRate+","+quality.resX+","+quality.resY, mp4Config.getProfileLevel()+","+mp4Config.getB64SPS()+","+mp4Config.getB64PPS());
			editor.commit();
		}
		return mp4Config;
		
	}
	
	public String generateSessionDescriptor() throws IllegalStateException, IOException {
		String profile,sps,pps;

		// ARNOTODO: Apparently they run testH264() once to get the SPS and PPS
		// and store them for speedy
		if (settings != null) {
			if (!settings.contains(quality.frameRate+","+quality.resX+","+quality.resY)) {
				testH264();
				profile = mp4Config.getProfileLevel();
				pps = mp4Config.getB64PPS();
				sps = mp4Config.getB64PPS();
			} else {
				String[] s = settings.getString(quality.frameRate+","+quality.resX+","+quality.resY, "").split(",");
				profile = s[0];
				sps = s[1];
				pps = s[2];
			}
		} else {
			testH264();
			profile = mp4Config.getProfileLevel();
			pps = mp4Config.getB64PPS();
			sps = mp4Config.getB64PPS();
		}

		/*byte[] spsb = mp4Config.getSPS();
		byte[] ppsb = mp4Config.getPPS();
		StringBuilder sb = new StringBuilder();
	    for (byte b : spsb) {
	        sb.append(String.format("%02X ", b));
	    }
	    Log.v(TAG,"ARNO GET SPS " + sb.toString());		
	    sb = new StringBuilder();
	    for (byte b : ppsb) {
	        sb.append(String.format("%02X ", b));
	    }
	    Log.v(TAG,"ARNO GET PPS " + sb.toString()); */		
	    
		return "m=video "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 H264/90000\r\n" +
				   "a=fmtp:96 packetization-mode=1;profile-level-id="+profile+";sprop-parameter-sets="+sps+","+pps+";\r\n";
	}
}
