package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
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

import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.interpreter.InterpreterUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoPlayerActivity extends Activity {

    NativeLib nativelib = null;
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
		  Log.w("player","onCreate: Python OK");
		  startSwiftAndPlay();
	  }
	  else{
		  Log.w("player","onCreate: Python FAIL (installing python package)");
		  Intent intent = new Intent(getBaseContext(), PythonAutoinstallActivity.class);
		  startActivity(intent);
	  }	  
  }
  
  @Override
  protected void onRestart() {
	  super.onRestart();
	  if (pythonIsInstalled()){
		  Log.w("player","onRestart play");
		  startSwiftAndPlay();
	  }
	  else{
		  // Go back to video list or twicca
		  Log.w("player","onRestart: Python FAIL (exit player)");  
		  finish();
	  }
  }

  public void onPause()
  {
	  super.onPause();
	  if (pythonIsInstalled()){
		  Log.w("player","onPause: Python OK (exit player)");
		  finish();
	  }
	  else{
		  Log.w("player","onPause: Python FAIL (wait for installer)");
	  }
  }
	
  public void onResume()
  {
	  super.onResume();
	  if (pythonIsInstalled()){
		  Log.w("player","onResume: Python OK (back from installer, play video)");
		  startSwiftAndPlay();
	  }
	  else{
		  Log.w("player","onPause: Python FAIL (wait, installer still not done)");  
	  }
  }
	
  public void onDestroy()
  {
	  super.onDestroy();
	  Log.w("player","onDestroy: finish() OR OS killed Activity");
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
		setContentView(R.layout.p2p);
		copyResourcesToLocal(); //copy Python code from res/raw to SD card
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
			//no text from twicca, parameters come from menu
			hash = extras.getString("hash");//"280244b5e0f22b167f96c08605ee879b0274ce22"
			tracker = extras.getString("tracker");
		}
		else{
			//parameters come from twicca			
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
	  //exit player if the progress screen is cancelled
	  _dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
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
		    		mVideoView = (VideoView) findViewById(R.id.surface_view);
		    		// Play via HTTPGW
		    		String urlstr = "http://127.0.0.1:8082/"+hash;
		    		mVideoView.setVideoURI(Uri.parse(urlstr));
		    		//TODO(low priority): finish() when video is over
		    		mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared (MediaPlayer mp) {
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
	
	
//	Code from P2PStartActivity below this line
	
	private void copyResourcesToLocal() {
		String name, sFileName;
		InputStream content;
		R.raw a = new R.raw();
		java.lang.reflect.Field[] t = R.raw.class.getFields();
		Resources resources = getResources();
		for (int i = 0; i < t.length; i++) {
			try {
				name = resources.getText(t[i].getInt(a)).toString();
				sFileName = name.substring(name.lastIndexOf('/') + 1, name
						.length());
				content = getResources().openRawResource(t[i].getInt(a));

				// Copies script to internal memory only if changes were made
				sFileName = InterpreterUtils.getInterpreterRoot(this)
						.getAbsolutePath()
						+ "/" + sFileName;
				if (needsToBeUpdated(sFileName, content)) {
					Log.d("Swift", "Copying from stream " + sFileName);
					content.reset();
					FileUtils.copyFromStream(sFileName, content);
				}
				FileUtils.chmod(new File(sFileName), 0755);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private boolean needsToBeUpdated(String filename, InputStream content) {
		File script = new File(filename);
		FileInputStream fin;
		Log.d("Swift", "Checking if " + filename + " exists");

		if (!script.exists()) {
			Log.d("Swift", "not found");
			return true;
		}

		Log.d("Swift", "Comparing file with content");
		try {
			fin = new FileInputStream(filename);
			int c;
			while ((c = fin.read()) != -1) {
				if (c != content.read()) {
					Log.d("Swift", "Something changed replacing");
					return true;
				}
			}
		} catch (Exception e) {
			Log.d("Swift", "Something failed during comparing");
//			Log.e("Swift", e);
			return true;
		}
		Log.d("Swift", "No need to update " + filename);
		return false;
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}

	
	
}


