/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexey Reznichenko (alexey.reznichenko@gmail.com)
 */
public class VideoPlayerActivity extends Activity {


	NativeLib nativelib = null;
//	protected TextView _text;
    protected SwiftMainThread _swiftMainThread;
    protected StatsTask _statsTask;
	private VideoView mVideoView = null;
    protected ProgressDialog _dialog;
    protected Integer _seqCompInt;

    private boolean videoPlaying = false;
    
    String hash; 
	String tracker;
	String destination;
	boolean inmainloop = false;
	
	
  @Override
  protected void onCreate(Bundle savedInstanceState) {
	
	  super.onCreate(savedInstanceState);
	  
	  if (pythonIsInstalled()){
		  Log.w("player","onCreate true");

		  startSwiftAndPlay();
	  }
	  else{
		  Log.w("player","onCreate false");
		  Intent intent = new Intent(getBaseContext(), PythonAutoinstallActivity.class);
		  startActivity(intent);
	  }	  
  }
  
  @Override
  protected void onRestart() {
	  super.onRestart();
	  // Go back to video list
	  if (pythonIsInstalled()){
		  Log.w("player","onRestart play");
		  startSwiftAndPlay();
	  }
	  else{
		  Log.w("player","onRestart false");  
		  finish();
	  }
  }

  public void onPause()
  {
	  super.onPause();
	  if (pythonIsInstalled()){
		  Log.w("player","onPause kill");
		  finish();
	  }
	  else{
		  Log.w("player","onPause false");
	  }
  }
	
  public void onResume()
  {
	  super.onResume();
	  if (pythonIsInstalled()){
		  Log.w("player","onResume play");
		  startSwiftAndPlay();
	  }
	  else{
		  Log.w("player","onPause false");  
	  }
  }
	
  public void onDestroy()
  {
	  super.onDestroy();
	  Log.w("player","onDestroy");
	  _statsTask.cancel(true);
  }

  
	private boolean pythonIsInstalled(){
		File pythonBin = new File("/data/data/"+getClass().getPackage().getName()+"/files/python/bin/python");
		return (pythonBin.exists() && pythonBin.canExecute());
	}
	
	private void startSwiftAndPlay(){
		if (videoPlaying){
			return;
		}
		videoPlaying = true;
		
		setContentView(R.layout.main);	      
		try
		{
			SwiftInitalize();
		}	
		catch(Exception e)
		{
			e.printStackTrace();
		}
		Bundle extras = getIntent().getExtras();
		String text = extras.getString("android.intent.extra.TEXT");
		if (text == null){
			//from menu
		
			hash = extras.getString("hash");//"280244b5e0f22b167f96c08605ee879b0274ce22"
			tracker = extras.getString("tracker"); // See VodoEitActivity to change this
		}
		else{
			//from twicca			
			Log.w("video twicca", text);
			Pattern p = Pattern.compile("ppsp://.{40}");
			Matcher m = p.matcher(text);
			if (m.find()) {
			    String s = m.group();
			    hash = s.substring(7);
			    Log.w("video twicca", hash);
			}
			else{
				hash = "";
			    Log.w("video twicca", "no ppsp link found");
			}
			tracker = "192.16.127.98:20050"; //TODO
					
		}
		destination = "/sdcard/swift/video.ts";
		if (hash != ""){
			SwiftStartDownload();
		}
	}

  
  /*
   *  Arno: From Riccardo's original SwiftBeta
   */
  
  protected void SwiftInitalize()
  {
	  // create dir for swift
	  String swiftFolder = "/swift";
	  String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	  File mySwiftFolder = new File(extStorageDirectory + swiftFolder);
	  mySwiftFolder.mkdir();	  
  }
  
	//starts the download thread
	protected void SwiftStartDownload() {
		// Start the background process
		_swiftMainThread = new SwiftMainThread();
		_swiftMainThread.start();    	
		// start the progress bar
		SwiftCreateProgress();
		_statsTask = new StatsTask();
		_statsTask.execute( hash, tracker, destination );
	}
	
	// creates the progress dialog
	protected void SwiftCreateProgress() {
		_dialog = new ProgressDialog(VideoPlayerActivity.this);
	  _dialog.setCancelable(true);
	  _dialog.setMessage("Buffering...");
	  // set the progress to be horizontal
	  _dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	  // reset the bar to the default value of 0
	  _dialog.setProgress(0);
	  
	  //stop the engine if the procress scree is cancelled
	  _dialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
//				_text.setText("TODO HTTPGW engine stopped!");
				// Arno, 2012-01-30: TODO tell HTTPGW to stop serving data
				//nativelib.stop();
				// Raul, 2012-03-27: don't stay here with a black screen. 
				// Go back to video list
				finish();
			}
		});
	
	  // display the progressbar
	  _dialog.show();
	  
	}
	
	
	//starts the video playback
	private void SwiftStartPlayer() {
		//_dialog.dismiss();
		if (destination == null || destination.length() == 0) {
			Toast.makeText(VideoPlayerActivity.this, "File URL/path is empty",
					Toast.LENGTH_LONG).show();
		}
		else {
			runOnUiThread(new Runnable(){
				public void run() {
					getWindow().setFormat(PixelFormat.TRANSLUCENT);
//					_text.setText("Play " + destination);
		    		mVideoView = (VideoView) findViewById(R.id.surface_view);
	
		    		// Arno, 2012-01-30: Download *and* play, using HTTPGW
		    		//String filename = "/sdcard/swift/" + destination;
		    		//mVideoView.setVideoPath(destination);
		    		String urlstr = "http://127.0.0.1:8082/"+hash;
		    		//String urlstr = "file:"+destination;
		    		mVideoView.setVideoURI(Uri.parse(urlstr));
		    		
		    		mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared (MediaPlayer mp) {
//							_text.setText("Player75 prepared!");
							_dialog.dismiss();
						}
					});
		    		
		    		
		    		MediaController mediaController = new MediaController(VideoPlayerActivity.this);
		    		mediaController.setAnchorView(mVideoView);
		            mVideoView.setMediaController(mediaController);
					mVideoView.start();
					mVideoView.requestFocus();
		    		//mediaController.show(0); // keep visible
				}
				
			});
			
		}
	}
	
    private class SwiftMainThread extends Thread
    {
        public void run() 
        {
    		try 
    		{
    			NativeLib nativelib =  new NativeLib();
    			String ret = nativelib.start(hash, tracker, destination);
    			
				SwiftStartPlayer();
				
				// Arno: Never returns, calls libevent2 mainloop
				if (!inmainloop) 
				{
					inmainloop = true;
					Log.w("Swift","Entering libevent2 mainloop");
					
					int progr = nativelib.mainloop();
					
					Log.w("Swift","LEFT MAINLOOP!");
    			}
    		}
        	catch (Exception e ) 
        	{
        			e.printStackTrace();
        	}
        }
    }
	
    
	/**
	* sub-class of AsyncTask. Retrieves stats from Swift via JNI and
	* updates the progress dialog.
	*/
	private class StatsTask extends AsyncTask<String, Integer, String> {
		
	  protected String doInBackground(String... args) {
	  	
	  	String ret = "hello";
	  	if (args.length != 3) {
	  		ret = "Received wrong number of parameters during initialization!";
	  	}
	  	else {
	  		try {
	  			NativeLib nativelib =  new NativeLib();
	  			mVideoView = (VideoView) findViewById(R.id.surface_view);
	  			while(true) {
	  				String progstr = nativelib.httpprogress(args[0]);
	  				String[] elems = progstr.split("/");
	  				long seqcomp = Long.parseLong(elems[0]);
	  				long asize = Long.parseLong(elems[1]);
	
	  				if (asize == 0)
	  					_dialog.setMax(1024);
	  				else
	  					_dialog.setMax((int)(asize/1024));
	  				
	  				_seqCompInt = new Integer((int)(seqcomp/1024));
	  				
	  				Log.w("SwiftStats", "SeqComp   " + seqcomp );
	  				
	  	    		runOnUiThread(new Runnable(){
	  	    			public void run() {
	          				_dialog.setProgress(_seqCompInt.intValue() );
	
	  	    			}
	  	    		});
	  				//Raul, 20120425: removed break which caused playback interruption when
	  	    		//(asize > 0 && seqcomp == asize) (e.i, file downloaded)
	  				Thread.sleep( 1000 );
	  			}
	  		}
	  		catch (InterruptedException e ) {
	  			Log.d("player","InterruptedException (onDestroy): stop Swift");
	  			nativelib.stop();
	  		}
	  	}
	      return ret;
	  }
	}

	

}
