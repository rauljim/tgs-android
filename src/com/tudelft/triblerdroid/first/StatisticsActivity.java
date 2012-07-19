package com.tudelft.triblerdroid.first;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class StatisticsActivity extends Activity{
	protected Integer _seqCompInt;
	private TextView txtDownSpeed = null; 
	private TextView txtUpSpeed = null; 
	private TextView txtLeechers = null;
	private TextView txtSeeders = null;
	protected UpdateTask _updateTask;

	String hash; 
	String tracker;
	String destination;
	long seqcomp;
	int dspeed, uspeed, nleech, nseed;
	String progstr;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
		_updateTask = new UpdateTask();
		Bundle extras = getIntent().getExtras();
		hash = extras.getString("com.tudelft.triblerdroid.first.VideoPlayerActivity.hash");
		tracker = extras.getString("com.tudelft.triblerdroid.first.VideoPlayerActivity.tracker");
		destination = extras.getString("com.tudelft.triblerdroid.first.VideoPlayerActivity.destination");
		txtDownSpeed = (TextView) findViewById(R.id.down_speed);
		txtUpSpeed = (TextView) findViewById(R.id.up_speed);
		txtLeechers = (TextView) findViewById(R.id.nbr_leech);
		txtSeeders = (TextView) findViewById(R.id.nbr_seed);
		_updateTask.execute( hash, tracker, destination );
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		_updateTask.cancel(true);
		Log.w("SwiftStatsActivity", "*** SHUTDOWN SWIFT STATS ACTIVITY ***");
	}

	/**
	 * sub-class of AsyncTask. Retrieves stats from Swift via JNI and
	 * updates the statistics activity.
	 */
	private class UpdateTask extends AsyncTask<String, Integer, String> {

		protected String doInBackground(String... args) {
			String ret = "hello";
			if (args.length != 3) {
				ret = "Received wrong number of parameters during initialization!";
			}
			else {
				try {

					NativeLib nativelib =  new NativeLib();

					while(true) {
						progstr = nativelib.httpprogress(args[0]);
						String[] elems = progstr.split("/");
						seqcomp = Long.parseLong(elems[0]);

						_seqCompInt = new Integer((int)(seqcomp/1024));

						txtDownSpeed = (TextView) findViewById(R.id.down_speed);
						progstr = "";
						progstr = nativelib.stats();
						String[] items = progstr.split("/");
						dspeed = Integer.parseInt(items[0]);
						uspeed = Integer.parseInt(items[1]);
						nleech = Integer.parseInt(items[2]);
						nseed = Integer.parseInt(items[3]);
						runOnUiThread(new Runnable(){
							public void run() {
								txtDownSpeed.setText(dspeed+" kb/s");
								txtUpSpeed.setText(uspeed+" kb/s");
								//txtUpSpeed.setText(progstr);// to check all details from statsgw
								txtLeechers.setText(nleech+" ");
								txtSeeders.setText(nseed+" ");
							}
						});
						Thread.sleep( 1000 );
					}

				}
				catch (Exception e ) {
					e.printStackTrace();
					ret = "error occurred during initialization!";
				}
			}
			return ret;
		}
	}

}
