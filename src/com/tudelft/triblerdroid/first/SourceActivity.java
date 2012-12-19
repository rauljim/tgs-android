/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
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
 * 
 * Swift streaming additions (c) Copyright 2012 Technische Universiteit Delft.
 */

package com.tudelft.triblerdroid.first;

import java.net.InetAddress;

//ARNOAPI12
//import com.tudelft.majorkernelpanic.streaming.audio.AACStream;
import com.tudelft.majorkernelpanic.streaming.video.H264Stream;
import com.tudelft.majorkernelpanic.streaming.video.VideoQuality;
import com.tudelft.majorkernelpanic.spydroid.*;
import com.tudelft.triblerdroid.swift.NativeLib;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Camera.CameraInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
//import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import org.tribler.mobile.R;



public class SourceActivity extends Activity implements OnSharedPreferenceChangeListener {
        
        static final public String TAG = "SpydroidActivity"; 
        
        private ImageView logo, led;
        private PowerManager.WakeLock wl;
        private RtspServer rtspServer = null;
        private SurfaceHolder holder;
        private SurfaceView camera;
        private TextView console, status;
        private VideoQuality defaultVideoQuality = new VideoQuality();
        private Display display;
        private Context context;
        
        // Arno
        private Session _session;
        private Button  _b1;
        private Button  _b2;
        private int 	_oldNALULength;
        static public String  _swarmid;
        
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		// Enable beaming of last recorded video via Android Beam, if avail
		// Must be called for each Activity in the app.
		IntroActivity.ConfigureNFCBeam(this);
        
        setContentView(R.layout.source);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        logo = (ImageView)findViewById(R.id.logo);
        //console = (TextView) findViewById(R.id.console);
        status = (TextView) findViewById(R.id.status);
        display = getWindowManager().getDefaultDisplay();
        context = this.getApplicationContext();
        led = (ImageView)findViewById(R.id.led);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        H264Stream.setPreferences(settings);
        //ARNOAPI12 AACStream.setAACSupported(android.os.Build.VERSION.SDK_INT>=14);
        /*
         * Arno: The H.264 Sequence Picture Set (SPS) and Picture Parameter Set 
         * (PPS) constants used in com.tudelft.majorkernelpanic.rtp.MPEG2TSWriter
         * and/or Swift's jni/httpgw.cpp are hardcoded to 640x480@15 fps 500 
         * kbps. If you want to change the below params, you should change 
         * these constants (and mTimeStampIncr).
         * See com.tudelft.majorkernelpanic.streaming.H264Stream.testH264() on
         * how to let Android create the new constants (to be prefixed with
         * 00 00 00 01) for you. 
         */
        defaultVideoQuality.resX = settings.getInt("video_resX", 640);
        defaultVideoQuality.resY = settings.getInt("video_resY", 480);
        defaultVideoQuality.frameRate = Integer.parseInt(settings.getString("video_framerate", "15"));
        defaultVideoQuality.bitRate = Integer.parseInt(settings.getString("video_bitrate", "500"))*1000; // 500 kb/s
        
        settings.registerOnSharedPreferenceChangeListener(this);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");
    
        Session.setSurfaceHolder(holder);
        Session.setDefaultVideoQuality(defaultVideoQuality);
        // Arno: No audio
        Session.setDefaultAudioEncoder(settings.getBoolean("stream_audio", false)?Integer.parseInt(settings.getString("audio_encoder", "1")):0);
        Session.setDefaultVideoEncoder(settings.getBoolean("stream_video", true)?Integer.parseInt(settings.getString("video_encoder", "1")):0);
        
        //if (settings.getBoolean("enable_rtsp", true)) rtspServer = new RtspServer(8086, handler);

        
        
        /*
         * ARNO: Swift live streaming changes
         */
        _oldNALULength = -1;
        
		// Arno, 2012-11-26: Hardcoded swarm id of live swarm
        // FUSETODO: let user pick name for broadcast, or let IP keep it unique
        _swarmid = "e5a12c7ad2d8fab33c699d1e198d66f79fa610c3";
        		
        NativeLib nativelib = new NativeLib();
		String ret = nativelib.LiveCreate( _swarmid );
		
        // Create new Session
        try
        {
        	_session = new Session(InetAddress.getByName("127.0.0.1"),handler);
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
			e.printStackTrace();
		}
     			
        
        // Setup the UI
        _b1 = (Button) findViewById(R.id.button1);
        _b2 = (Button) findViewById(R.id.button2);

        _b1.setOnClickListener(new OnClickListener() {

        	public void onClick(View v) {
        		
        		boolean flash = false;
        		int camera = CameraInfo.CAMERA_FACING_BACK;
        		
				VideoQuality quality = VideoQuality.defaultVideoQualiy;
				try
				{
					_session.addVideoTrack(Session.VIDEO_H264, camera, quality, flash);
					_session.start(0);
				}
				catch(Exception e)
				{
					System.out.println(e.toString());
					e.printStackTrace();
				}
            }
          });

        _b2.setOnClickListener(new OnClickListener() {

        	public void onClick(View v) {
        		
        		NativeLib nativelib = new NativeLib();
        		nativelib.asyncClose(_swarmid,true,true);
        		//finish();
        		System.exit(0);
            }
          });

        
        }

        
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	if (key.equals("video_resX")) {
        		defaultVideoQuality.resX = sharedPreferences.getInt("video_resX", 640);
        		Session.setDefaultVideoQuality(defaultVideoQuality);
        	}
        	else if (key.equals("video_resY"))  {
        		defaultVideoQuality.resY = sharedPreferences.getInt("video_resY", 480);
        		Session.setDefaultVideoQuality(defaultVideoQuality);
        	}
        	else if (key.equals("video_framerate")) {
        		defaultVideoQuality.frameRate = Integer.parseInt(sharedPreferences.getString("video_framerate", "15"));
        		Session.setDefaultVideoQuality(defaultVideoQuality);
        	}
        	else if (key.equals("video_bitrate")) {
        		defaultVideoQuality.bitRate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "500"))*1000;
        		Session.setDefaultVideoQuality(defaultVideoQuality);
        	}
        	else if (key.equals("stream_audio") || key.equals("audio_encoder")) { 
        		Session.setDefaultAudioEncoder(sharedPreferences.getBoolean("stream_audio", true)?Integer.parseInt(sharedPreferences.getString("audio_encoder", "1")):0);
        	}
        	else if (key.equals("stream_video") || key.equals("video_encoder")) {
        		Session.setDefaultVideoEncoder(sharedPreferences.getBoolean("stream_video", true)?Integer.parseInt(sharedPreferences.getString("video_encoder", "1")):0);
        	}
        	else if (key.equals("enable_rtsp")) {
        		if (sharedPreferences.getBoolean("enable_rtsp", true)) {
        			if (rtspServer == null) rtspServer = new RtspServer(8086, handler);
        		} else {
        			if (rtspServer != null) rtspServer = null;
        		}
        	}	
        }
        
        public void onStart() {
        	super.onStart();
        	// Lock screen
        	wl.acquire();
        	
        	/*
        	Intent notificationIntent = new Intent(this, SourceActivity.class);
        	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        	NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        	Notification notification = builder.setContentIntent(pendingIntent)
        	        .setWhen(System.currentTimeMillis())
        	        .setTicker(getText(R.string.notification_title))
        	        .setSmallIcon(R.drawable.icon)
        	        .setContentTitle(getText(R.string.notification_title))
        	        .setContentText(getText(R.string.notification_content)).build();
        	notification.flags |= Notification.FLAG_ONGOING_EVENT;
        	((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);
        	*/
        }
        	
        public void onStop() {
        	super.onStop();
        	wl.release();
        }
        
        public void onResume() {
        	super.onResume();
        	// Determines if user is connected to a wireless network & displays ip 
        	displayIpAddress();
        	//startServers();
        	registerReceiver(wifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        	//handler.postDelayed(logoAnimation, 7000);
        }
        
        public void onPause() {
        	super.onPause();
        	if (rtspServer != null) rtspServer.stop();
        	unregisterReceiver(wifiStateReceiver);
        	//handler.removeCallbacks(logoAnimation);
        }
        
        public void onDestroy() {
        	super.onDestroy();
        	// Remove notification
        	((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
        	if (rtspServer != null) rtspServer.stop();
        }
        
        public void onBackPressed() {
        	Intent setIntent = new Intent(Intent.ACTION_MAIN);
        	setIntent.addCategory(Intent.CATEGORY_HOME);
        	setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(setIntent);
        }

        /*
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.layout.sourcemenu, menu);
            return true;
        }
        
        public boolean onOptionsItemSelected(MenuItem item) {
        	Intent intent;
        	
            switch (item.getItemId()) {
            case R.id.options:
                // Starts QualityListActivity where user can change the streaming quality
                intent = new Intent(this.getBaseContext(),OptionsActivity.class);
                startActivityForResult(intent, 0);
                return true;
            case R.id.quit:
            	finish();	
                return true;
            default:
                return super.onOptionsItemSelected(item);
            }
        }
        
        private void startServers() {
        	if (rtspServer != null) {
        		try {
        			rtspServer.start();
        		} catch (IOException e) {
        			log("RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
        		}
        	}
        }*/
        
        // BroadcastReceiver that detects wifi state changements
        private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
            	String action = intent.getAction();
            	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            		displayIpAddress();
            	}
            } 
        };
        
        private boolean streaming = false;
        
        // The Handler that gets information back from the RtspServer
        private final Handler handler = new Handler() {
        	
        	public void handleMessage(Message msg) { 
        		switch (msg.what) {
        		case RtspServer.MESSAGE_LOG:
        			log((String)msg.obj);
        			break;
        		case RtspServer.MESSAGE_ERROR:
        			log((String)msg.obj);
        			break;
        		case Session.MESSAGE_START:
        			if (!streaming) handler.postDelayed(ledAnimation, 100);
        			streaming = true;
        			status.setText(R.string.streaming);
        			break;
        		case Session.MESSAGE_STOP:
        			streaming = false;
        			handler.removeCallbacks(ledAnimation);
        			displayIpAddress();
        			break;
        		case Session.MESSAGE_ERROR:
        			log((String)msg.obj);
        			break;
        		}
        	}
        	
        };
        
        private String getIPAddress()
        {
    		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    		WifiInfo info = wifiManager.getConnectionInfo();
        	if (info!=null && info.getNetworkId()>-1) {
    	    	int i = info.getIpAddress();
    	    	return String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
        	}
        	else
        		return "0.0.0.0";
        }
        
        private void displayIpAddress() {
        	String ip = getIPAddress();
        	if (ip != "") {
    	    	status.setText("Source ");
    	    	status.append(ip);
    	    	led.setImageResource(R.drawable.led_green);
        	} else {
        		led.setImageResource(R.drawable.led_red);
        		status.setText(R.string.warning);
        	}
        }
        
        public void log(String s) {
        	/*String t = console.getText().toString();
        	if (t.split("\n").length>8) {
        		console.setText(t.substring(t.indexOf("\n")+1, t.length()));
        	}
        	console.append(Html.fromHtml(s+"<br />"));*/
        	Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
        }

    	private Runnable logoAnimation = new Runnable() {
    		public void run() {
    			runLogoAnimation();
    			handler.postDelayed(this,7000);
    		}
    	};
        
    	private boolean ledState = true; 
    	
    	private void toggleLed() {

    		status.setText( "Stream " + getIPAddress() + " * " + com.tudelft.majorkernelpanic.rtp.H264Packetizer.arnoLastNALULength );

    		// Arno: show stall in encoder
    		if (_oldNALULength == com.tudelft.majorkernelpanic.rtp.H264Packetizer.arnoLastNALULength)
    		{
    			led.setImageResource(R.drawable.led_red);
    			return;
    		}
    		_oldNALULength = com.tudelft.majorkernelpanic.rtp.H264Packetizer.arnoLastNALULength;
    		if (ledState) {
    			ledState = false;
    			led.setImageResource(R.drawable.led_green);
    		} else {
    			ledState = true;
    			led.setImageResource(getResources().getColor(android.R.color.transparent));
    		}
    	}
    	
    	private Runnable ledAnimation = new Runnable() {
    		public void run() {
    			toggleLed();
    			handler.postDelayed(this,900);
    		}
    	};
    	
    	private void runLogoAnimation() { 
    		int width = display.getWidth(), height = display.getHeight();
    		int side = (int) (Math.random()*4);
    		int position = (int) (side<2?(width-256)*Math.random():(height-256)*Math.random());
    		
    		RotateAnimation rotateAnimation = new RotateAnimation(0, side==0?180:(side==1?0:(side==2?90:270)),Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
    		TranslateAnimation translateAnimation = new TranslateAnimation(
    				Animation.ABSOLUTE, side<2?position:(side==2?-200:width), 
    				Animation.ABSOLUTE, side<2?position:(side==2?-100:width-80), 
    				Animation.ABSOLUTE, side>=2?position:(side==0?-200:height), 
    				Animation.ABSOLUTE, side>=2?position:(side==0?-110:height-80));
    		
    		rotateAnimation.setDuration(0);
    		rotateAnimation.setFillAfter(true);
    		translateAnimation.setStartOffset(1500);
    		translateAnimation.setDuration(1500);
    		translateAnimation.setRepeatCount(1);
    		translateAnimation.setRepeatMode(Animation.REVERSE);
    		translateAnimation.setFillAfter(true);
    		
    		AnimationSet animationSet = new AnimationSet(true);
    		
    		animationSet.setAnimationListener(new AnimationListener() {
    			public void onAnimationEnd(Animation animation) {
    				logo.setVisibility(View.INVISIBLE);
    			}
    			public void onAnimationRepeat(Animation animation) {}
    			public void onAnimationStart(Animation animation) {}
    		});
    		
    		animationSet.addAnimation(rotateAnimation);
    		animationSet.addAnimation(translateAnimation);
    		
    		logo.startAnimation(animationSet);
    		logo.setVisibility(View.VISIBLE);
    		
    	}
}
    
    