//Skeleton example from Alexey Reznichenko

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import me.ppsp.test.R;
import se.kth.pymdht.Pymdht;

public class VideoPlayerActivity extends Activity {
	//Anand - begin - added constants to pass parameters to next activity
	private static final String _HASH = "com.tudelft.triblerdroid.first.VideoPlayerActivity.hash";
	private static final String _TRACKER = "com.tudelft.triblerdroid.first.VideoPlayerActivity.tracker";
	private static final String _DESTINATION = "com.tudelft.triblerdroid.first.VideoPlayerActivity.destination";
	//end
	NativeLib nativelib = null;
    protected SwiftMainThread _swiftMainThread;
    protected StatsTask _statsTask;
	private VideoView mVideoView = null;
    protected ProgressDialog _dialog;
    protected Integer _seqCompInt;

    String hash = null; 
	String tracker;
	String destination;
	boolean inmainloop = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	
	  super.onCreate(savedInstanceState);
 
      setContentView(me.ppsp.test.R.layout.main);
      try
      {
    	  SwiftInitalize();
      }
      catch(Exception e)
      {
    	  e.printStackTrace();
      }
      
	  Bundle extras = getIntent().getExtras();

	  hash = extras.getString("hash");//"280244b5e0f22b167f96c08605ee879b0274ce22"
	  tracker = "192.16.127.98:20050"; //TODO
	  destination = "/sdcard/swift/video.ts";
	  if (hash != null){
		  Log.w("final hash", hash);
		  SwiftStartDownload();
	  }		 
	  Log.w("video player", "setup DONE");
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
      MenuInflater menuInflater = getMenuInflater();
      menuInflater.inflate(R.layout.menu, menu);
      return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {

      switch (item.getItemId())
      {
      case R.id.menu_stats:
    	  ShowStatistics();
    	  return true;
      case R.id.menu_settings:
      	// Single menu item is selected do something
      	// Ex: launching new activity/screen or show alert message
  		Intent intent = new Intent(getBaseContext(), Preferences.class);
  		startActivity(intent);
  		return true;

      default:
          return super.onOptionsItemSelected(item);
      }
  }    
  
  //stops the Async task when we press back button on video player
  @Override
  public void onStop()
  {
	  super.onStop();
	  _statsTask.cancel(true);
  }
  @Override
  public void onDestroy()
  {
		super.onDestroy();
		Log.w("SwiftStats", "*** SHUTDOWN SWIFT ***");
		// Raul, 2012-04-25: Halts swift completely on destroy
		_statsTask.cancel(true);
		Log.w("SwiftStats", "*** SHUTDOWN SWIFT ***");
		// Halts swift completely
		//nativelib.stop(); Raul: this raises an exception.
		//I think it's because there is not time to execute it onDestroy
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
		BufferedReader unstable = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(me.ppsp.test.R.raw.bootstrap_unstable)));
		BufferedReader stable = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(me.ppsp.test.R.raw.bootstrap_stable)));
		final Pymdht dht = new Pymdht(9999, unstable, stable);
		Runnable runnable_dht = new Runnable(){
		    @Override
		    public void run() {
		        dht.start();
		    }
		};

		Thread dht_thread = new Thread(runnable_dht);
		dht_thread.start();
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
	  
	  ShowStatistics();
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
		    		mVideoView = (VideoView) findViewById(me.ppsp.test.R.id.surface_view);
	
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
							
							//Cancel _statsTask if you don't want to get downloading report on catlog 
							//_statsTask.cancel(true);
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
	  		try {//TODO: catch InterruptedException (onDestroy)
	
	  			NativeLib nativelib =  new NativeLib();
	  			mVideoView = (VideoView) findViewById(me.ppsp.test.R.id.surface_view);
	  			boolean play = false, pause=false;
	  			
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
	  				if(isCancelled())
	  					break;
	  				
	  	    		runOnUiThread(new Runnable(){
	  	    			public void run() {
	          				_dialog.setProgress(_seqCompInt.intValue() );
	
	  	    			}
	  	    		});
	  				//Raul, 20120425: removed break which caused playback interruption when
	  	    		//(asize > 0 && seqcomp == asize) (e.i, file downloaded)
	  	    		try{
	  	    			Thread.sleep( 1000 );
	  	    		}
	  	    		catch (InterruptedException e){
	  	    			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>Sleep interrupted<<<<<<<<<<<<<<<<<<<<<<<");
	  	    		}
	  			}
	  			
	  		}
	  		catch (Exception e ) {
	  			//System.out.println("Stacktrace "+e.toString());
	  			e.printStackTrace();
	  			ret = "error occurred during initialization!";
	  		}
	  	}
	      return ret;
	  }
	}
	
	public void ShowStatistics(){
		Intent intent = new Intent(getBaseContext(), StatisticsActivity.class);
		intent.putExtra(_HASH, hash);
		intent.putExtra(_TRACKER, tracker);
		intent.putExtra(_DESTINATION, destination);
		startActivity(intent);
		
	}
}
