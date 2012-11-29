//Skeleton example from Alexey Reznichenko

package com.tudelft.triblerdroid.first;

import com.tudelft.triblerdroid.swift.NativeLib;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ResolveInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import java.util.List;

import me.ppsp.test.R;
import se.kth.pymdht.Pymdht;

public class VideoPlayerActivity extends Activity {
	NativeLib nativelib = null;
	protected StatsTask _statsTask;
	private VideoView mVideoView = null;
	protected ProgressDialog progressDialog;
	protected Integer _seqCompInt;

	String hash = null; 
	String tracker;
	String destination;
	boolean live=false;
	boolean inmainloop = false;

    public int PROGRESS_DIALOG = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Enable beaming of last recorded video via Android Beam, if avail
		// Must be called for each Activity in the app.
		IntroActivity.ConfigureNFCBeam(this);


		setContentView(R.layout.main);
		Util.mkdirSDContent();
		Bundle extras = getIntent().getExtras();

		hash = extras.getString("hash");
		tracker = extras.getString("tracker");
		destination = "/sdcard/swift/video.ts";
		live = extras.getBoolean("live",false);
		if (hash == null){
			return;
		}
		
		String msg = "Starting video tswift://"+tracker+"/"+hash;
		if (live)
			msg += "@-1";
		Log.w("Swift", msg);
		
		//Log.w("final hash", hash);
		startDHT(hash);
		
		// Arno, 2012-11-26: Just set tracker for this swarm (HTTPGW), Mainloop
		// already started in IntroActivity.
		NativeLib nativelib = new NativeLib();
		nativelib.SetTracker(tracker);
		
		
		// Show P2P info (stats) according to preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("pref_stats", true)){
			ShowStatistics();
		}
		// start the progress bar
		showDialog(PROGRESS_DIALOG);
		_statsTask = new StatsTask();
		_statsTask.execute( hash, tracker, destination );
		Log.w("video player", "setup DONE");
		
		startVideoPlayback(live,true);
	}
	
	protected Dialog onCreateDialog(int id) {
		if (id == PROGRESS_DIALOG){
			progressDialog = new ProgressDialog(VideoPlayerActivity.this);
			progressDialog.setCancelable(true);
			progressDialog.setMessage("Connectivity: "+Util.getConnectivity(getApplicationContext())+"\nBuffering...");
			// set the progress to be horizontal
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			// reset the bar to the default value of 0
			progressDialog.setProgress(0);

			//stop the engine if the procress scree is cancelled
			progressDialog.setOnCancelListener(new OnCancelListener() {
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
			return progressDialog;
		}
		return null;
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
			Intent intent = new Intent(getBaseContext(), Preferences.class);
			startActivity(intent);
			return true;
		case R.id.menu_about:
			setContentView(R.layout.about);
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
		//_statsTask.cancel(true);
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

	protected void startDHT(String hash){
		BufferedReader unstable = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.bootstrap_unstable)));
		BufferedReader stable = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.bootstrap_stable)));
		final Pymdht dht = new Pymdht(9999, unstable, stable, hash, false);
		Runnable runnable_dht = new Runnable(){
			@Override
			public void run() {
				dht.start();
			}
		};
		Thread dht_thread = new Thread(runnable_dht);
		dht_thread.start();
	}
	

	//starts the video playback
	private void startVideoPlayback(boolean live, boolean liveSourceContentIsRawH264) 
	{
		// Arno, 2012-10-25: Handle live stream, either MPEGTS or raw H.264 from
		// Android camera.
		String urlstr = "";
		
		if (live) 
		{
			// Do video playback via VLC as VideoView can't handle MPEG-TS
			// or raw H.264
			Intent intent = null;
			if (liveSourceContentIsRawH264)
			{
				// Arno, 2012-10-24: LIVESOURCE=ANDROID
				// Force VLC to use H.264 demuxer via URL, see
				// http://wiki.videolan.org/VLC_command-line_help
				urlstr = "http/h264://127.0.0.1:8082/"+hash;
				urlstr += ".h264";
				//urlstr += " :network-caching=50";
				//urlstr += " :http-caching=50";
	
				Uri intentUri = Uri.parse(urlstr);
		    
				// Arno, 2012-10-24: Volatile, if VLC radically changes package 
				// name,  we doomed. But normal Intent searching won't grok
				// VLC's hack with the demuxer in the scheme: http/h264:
				// so we have to do it this way.
				//
				String pkgname = getPackageNameForVLC("org.videolan.vlc.betav7neon");
				if (pkgname == "")
					return;
				
				intent = new Intent();
				ComponentName cn = new ComponentName(pkgname,pkgname+".gui.video.VideoPlayerActivity");
				intent.setComponent(cn);
			    intent.setAction(Intent.ACTION_VIEW);
			    intent.setData(intentUri);
			}
			else
			{
				// MPEG-TS live stream
				urlstr = "http://127.0.0.1:8082/"+hash;
				urlstr += "@-1";
	
				Uri intentUri = Uri.parse(urlstr);
			    
			    intent = new Intent();
			    intent.setAction(Intent.ACTION_VIEW);
			    intent.setDataAndType(intentUri,"video/mp2t");
			}
	
		    startActivity(intent);
		}
		else
		{
			// Playback via internal Android player
			
			runOnUiThread(new Runnable(){ //Raul, 120920: Why??
				public void run() {
					getWindow().setFormat(PixelFormat.TRANSLUCENT);
					mVideoView = (VideoView) findViewById(R.id.surface_view);
					// Download *and* play, using HTTPGW
					String urlstr = "http://127.0.0.1:8082/"+hash;
					mVideoView.setVideoURI(Uri.parse(urlstr));
					mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared (MediaPlayer mp) {
							dismissDialog(PROGRESS_DIALOG);
							//Cancel _statsTask if you don't want to get downloading report on catlog 
							//_statsTask.cancel(true);
						}
					});
					mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mp) {
							// TODO set as default / post tweet
	//						finish();
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
	
	
	
	/*
	 * Arno: See if VLC is installed, if so find out current name. If not 
	 * installed, open Google Play.
	 */
	private String getPackageNameForVLC(String vlcCurrentPackageName)
	{
		String vlcpkgnameprefix = "org.videolan.vlc";
	    try
	    {
	    	// From http://stackoverflow.com/questions/2780102/open-another-application-from-your-own-intent
	        Intent intent = new Intent("android.intent.action.MAIN");
	        intent.addCategory("android.intent.category.LAUNCHER");
	
	        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
	        List<ResolveInfo> resolveinfo_list = this.getPackageManager().queryIntentActivities(intent, 0);
	
	        for (ResolveInfo info:resolveinfo_list)
	        {
	        	String ilcpn = info.activityInfo.packageName.toLowerCase();
	            if (ilcpn.startsWith(vlcpkgnameprefix))
	            {
	            	return info.activityInfo.packageName;
	            }
	        }
	
	        // VLC not found, prompt user to install
	        openPlayStore(vlcCurrentPackageName);
	    }
	    catch (Exception e) 
	    {
	        openPlayStore(vlcCurrentPackageName);
	    }
	    return "";
	}
	
	
	private void openPlayStore(String packageName)
	{
	    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(intent);
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
	
					NativeLib nativelib = new NativeLib();
					boolean play = false, pause=false;
	
					String h = args[0];
					String t = args[1];
					String f = args[2];
					while(true) {
						int callid = nativelib.asyncGetHTTPProgress(h);
						String progstr = "n/a";
						while (progstr.equals("n/a"))
						{
							progstr = nativelib.asyncGetResult(callid);
							try
							{
								Thread.sleep( 100 );
							}
							catch (InterruptedException e)
							{
								System.out.println("ppsp VideoPlayerActivity: StatsTask: async sleep interrupted");
							}
						}
						String[] elems = progstr.split("/");
						long seqcomp = Long.parseLong(elems[0]);
						long asize = Long.parseLong(elems[1]);
	
						if (asize == 0 && seqcomp == 0)
							progressDialog.setMax(1024);
						else if (asize == 0 && seqcomp > 0) // LIVE
							progressDialog.setMax((int)(seqcomp/1024));
						else
							progressDialog.setMax((int)(asize/1024));
	
						_seqCompInt = new Integer((int)(seqcomp/1024));
	
						Log.w("SwiftStats", "SeqComp   " + seqcomp );
						
						if(isCancelled())
							break;
	
						runOnUiThread(new Runnable(){
							public void run() {
								progressDialog.setProgress(_seqCompInt.intValue() );
	
							}
						});
						//Raul, 20120425: removed break which caused playback interruption when
						//(asize > 0 && seqcomp == asize) (e.i, file downloaded)
						try
						{
							Thread.sleep( 1000 );
						}
						catch (InterruptedException e)
						{
							System.out.println("ppsp VideoPlayerActivity: StatsTask: main sleep interrupted");
						}
					}
					System.out.println("ppsp VideoPlayerActivity: >>>>>>>>>>>>> LEFT LOOP <<<<<<");
	
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
		intent.putExtra("hash", hash);
		intent.putExtra("tracker", tracker);
		intent.putExtra("destination", destination);
		startActivity(intent);
	
	}
}
