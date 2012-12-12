//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;


import com.tudelft.triblerdroid.swift.NativeLib;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import me.ppsp.test.R;
import se.kth.pymdht.Pymdht;


public class UploadActivity extends Activity {
    private static final int SELECT_VIDEO_FILE_REQUEST_CODE = 200;

    public static final String PREFS_NAME = "settings.dat";

	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 100;
//    CheckBox cb_showIntro;
    boolean user_set_default_now = false;
    public int INVALID_ID_DIALOG = 0;
    public int SET_DEFAULT_DIALOG = 1;
    public int MOBILE_WARNING_DIALOG = 2;

    String destination;
	String tracker;

	private SeedTask _seedTask = null;
	protected String statstr;

	public int numConnections;

	public String newhash;

	public NativeLib nativelib;

	public boolean gotBackPressed = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		// Enable beaming of last recorded video via Android Beam, if avail
		// Must be called for each Activity in the app.
		IntroActivity.ConfigureNFCBeam(this);
		
		setContentView(R.layout.video_upload);
	
		Bundle extras = getIntent().getExtras();
//		hash  = extras.getString("hash");
		destination  = extras.getString("destination");
		if (destination == null){
			Log.d("upload.destination", "null");

		}
		Log.d("upload.destination", destination);
		// Point to swift<->DHT interface
		tracker = "127.0.0.1:9999";//"192.16.127.98:20050";

		// Arno: Announce to DHT and post to Twitter done in SeedTask
		_seedTask = new SeedTask();
		_seedTask.execute( tracker, destination );
	}

	protected void startDHT(String hash){
		BufferedReader unstable = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.bootstrap_unstable)));
		BufferedReader stable = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.bootstrap_stable)));
		final Pymdht dht = new Pymdht(9999, unstable, stable, hash, true);
		Runnable runnable_dht = new Runnable(){
			@Override
			public void run() {
				dht.start();
			}
		};
		Thread dht_thread = new Thread(runnable_dht);
		dht_thread.start();
	}


	/**
	 * sub-class of AsyncTask. Starts seed of file, exits when hash checked.
	 */
	private class SeedTask extends AsyncTask<String, Integer, String> {
	
		private int upSpeed;
		private String upSpeedStr;
		private String numConnectionsStr;
		private int numSeeders;

		protected String doInBackground(String... args) {
	
			String ret = "hello";
			if (args.length != 2) {
				ret = "Received wrong number of parameters during initialization!";
			}
			else {
				String t = args[0];
				String f = args[1];
				Log.w("SwiftSeed", "Args " + " " + t + " " + f );
				try {//TODO: catch InterruptedException (onDestroy)
	
					nativelib = new NativeLib();
					
					// Create checkpoint without using Mainloop thread, will
					// speed up asyncOpen.
					newhash = nativelib.hashCheckOffline(f);
					if (newhash.length() != 40)
					{
						Log.e("SwiftSeed", newhash );
						return newhash;
					}
					
					// Announce to DHT
					startDHT(newhash); //make sure this is called after hashCheckOffline()

					// Announce to Twitter
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse("https://twitter.com/intent/tweet?&text=I+just+uploaded+a+video.+Check+it+out!+&url=http://ppsp.me/"+newhash+"+@ppsp_me"));
					startActivity(i);
					
					
					
					// Actually open and seed file
					int openCallid = nativelib.asyncOpen(newhash,t,f);
					String resstr = "n/a";
					String [] splittedStat;
					runOnUiThread(new Runnable(){
						public void run() {
							TextView progressTV = (TextView) findViewById(R.id.upload_progress);
							progressTV.setText("Waiting for boosters..." );	
						}
					});
					int statsCallid;
					while (!gotBackPressed)//resstr.equals("n/a"))
					{
						Log.w("SwiftSeed", "Poll " + openCallid );
						resstr = nativelib.asyncGetResult(openCallid);
						Log.w("SwiftSeed", "Progress   " + resstr );
						Log.w("SwiftSeed", "Stats   " + statstr);
						statstr = "n/a";
						statsCallid = nativelib.asyncGetStats(newhash);
						try{
							Thread.sleep(500);
						}
						catch (InterruptedException e){
							System.out.println("ppsp VideoPlayerActivity: SeedTask: async sleep interrupted");
						}
						statstr = nativelib.asyncGetResult(statsCallid);
						Log.w("SwiftSeed", "statstr "+statstr);
						splittedStat = statstr.split("/");
						Log.w("SwiftSeed", "split length "+splittedStat.length);
						if (splittedStat.length != 6){
							continue;
						}
						Log.w("SwiftSeed", "statstr OK "+statstr);
						upSpeedStr = splittedStat[1];
						numConnections = Integer.parseInt(splittedStat[2]) - 1; //remove connection to DHT tracker
						numSeeders = Integer.parseInt(splittedStat[3]);
						runOnUiThread(new Runnable(){
							public void run() {
								String statusStr = null;
								if (numSeeders > 0){
									statusStr = "Upload DONE\nPress BACK to stop uploading.";
								}
								if (statusStr == null && numConnections == 0){
									statusStr = "Waiting for peers...\nPress BACK to cancel upload.";
								}
								if (statusStr == null){
									statusStr = "Connected to "+numConnections+" peers\nUpload speed: "+upSpeedStr+" KB/s\nPress BACK to cancel upload";
								}
								TextView progressTV = (TextView) findViewById(R.id.upload_progress);
								progressTV.setText(statusStr);	
							}
						});

					}
//					Log.w("SwiftSeed", "Result   " + resstr );
					
					//TODO: seed on background until one full copy is out
//			        finish();

					
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
	@Override
	public void onBackPressed() {
		gotBackPressed = true;
		nativelib.asyncClose(newhash, false, false); 
		super.onBackPressed();
	}

}
