package com.tudelft.triblerdroid.first;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.tudelft.triblerdroid.swift.NativeLib;
import me.ppsp.test.R;

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
	ExecutorService exec;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Enable beaming of last recorded video via Android Beam, if avail
		// Must be called for each Activity in the app.
		IntroActivity.ConfigureNFCBeam(this);
		
		setContentView(R.layout.stats);
		_updateTask = new UpdateTask();
		Bundle extras = getIntent().getExtras();
		hash = extras.getString("hash");
		tracker = extras.getString("tracker");
		destination = extras.getString("destination");
		txtDownSpeed = (TextView) findViewById(R.id.down_speed);
		txtUpSpeed = (TextView) findViewById(R.id.up_speed);
		txtLeechers = (TextView) findViewById(R.id.nbr_leech);
		txtSeeders = (TextView) findViewById(R.id.nbr_seed);
		System.out.println("creating thread pool");
		exec = Executors.newCachedThreadPool();
		System.out.println("starting thread");
		exec.execute(_updateTask);
		System.out.println("create done");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		exec.shutdown();
		Log.w("SwiftStatsActivity", "*** SHUTDOWN SWIFT STATS ACTIVITY ***");
		_updateTask.stop();
	}
	  
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.stats_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.menu_hide_stats)
		{
			finish();
			return true;
		}
		else
			return super.onOptionsItemSelected(item);
	}    

	
	
	private class UpdateTask implements Runnable {
		private boolean running = true;

		public void run() {
			try {

				NativeLib nativelib = new NativeLib();
				while (running) {
					int callid = nativelib.asyncGetHTTPProgress(hash);
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
							System.out.println("ppsp StatisticsActivity: UpdateTask: async sleep interrupted");
						}
					}
					String[] elems = progstr.split("/");
					seqcomp = Long.parseLong(elems[0]);

					_seqCompInt = new Integer((int) (seqcomp / 1024));

					txtDownSpeed = (TextView) findViewById(R.id.down_speed);

					callid = nativelib.asyncGetStats(hash);
					progstr = "n/a";
					while (progstr.equals("n/a"))
					{
						progstr = nativelib.asyncGetResult(callid);
						try
						{
							Thread.sleep( 100 );
						}
						catch (InterruptedException e)
						{
							System.out.println("ppsp StatisticsActivity: UpdateTask: async sleep interrupted");
						}
					}
					String[] items = progstr.split("/");
					Log.i("SwiftStats", progstr);
					dspeed = Integer.parseInt(items[0]);
					uspeed = Integer.parseInt(items[1]);
					nleech = Integer.parseInt(items[2]);
					nseed = Integer.parseInt(items[3]);
					runOnUiThread(new Runnable() {
						public void run() {
							txtDownSpeed.setText(dspeed + " kb/s");
							txtUpSpeed.setText(uspeed + " kb/s");
							txtLeechers.setText(nleech + " ");
							txtSeeders.setText(nseed + " ");
						}
					});
					Thread.sleep(1000);
				}

			} catch (Exception e) {
				Log.w("Swift stats Activity", "Exception");
				e.printStackTrace();
			}
		}
	

		public void stop(){
			running = false;
		}
	}
}
