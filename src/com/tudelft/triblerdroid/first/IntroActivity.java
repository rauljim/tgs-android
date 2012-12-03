package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.tudelft.triblerdroid.swift.NativeLib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ppsp.test.R;
import se.kth.pymdht.Id;
import se.kth.pymdht.Id.IdError;

public class IntroActivity extends FragmentActivity implements LiveIPDialogFragment.LiveIPDialogListener 
{
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 100;
    private static final int SELECT_VIDEO_FILE_REQUEST_CODE = 200;
    private static final int BACK_FROM_PLAYER_CODE = 300;
    private static final int BACK_FROM_UPLOAD_CODE = 400;

    public static final String PREFS_NAME = "settings.dat";

//    CheckBox cb_showIntro;
    String hash = null;
    boolean user_set_default_now = false;
    public int INVALID_ID_DIALOG = 0;
    public int SET_DEFAULT_DIALOG = 1;
    public int MOBILE_WARNING_DIALOG = 2;

	// Arno, 2012-11-27: Swift mainloop run here.
	protected SwiftMainThread _swiftMainThread = null;
    protected boolean _inmainloop = false;
    
    // Arno, 2012-11-28: NFC + Beam
    protected static final String     _beamdefaultfilename = "/sdcard/swift/capture.mp4";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Util.sendKillToDHT(); //just in case
		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean showIntro = settings.getBoolean("showIntro", true);

		// Arno, 2012-11-26: Init single swift thread 
		
        // create dir for swift
        String swiftFolder = "/swift";
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        File mySwiftFolder = new File(extStorageDirectory + swiftFolder);
        mySwiftFolder.mkdir();

		// Start the swift engine
		_swiftMainThread = new SwiftMainThread();
		_swiftMainThread.start();

		// Enable beaming of last recorded video via Android Beam, if avail
		// Must be called for each Activity in the app.
		IntroActivity.ConfigureNFCBeam(this);

		/*
		 * Handle Intent 
		 */
		
		hash = getHash();
		
		// Check whether this app is the default for http://ppsp.me links
		//Raul, 120920: Disable this for now (it's a bit annoying)
//		Intent ppspme_intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://ppsp.me"));
//		PackageManager pm = getBaseContext().getPackageManager();
//		final ResolveInfo mInfo = pm.resolveActivity(ppspme_intent, 0);
//		if (!pm.getApplicationLabel(mInfo.activityInfo.applicationInfo).equals("ppsp_player")){
////			Toast.makeText(getBaseContext(), "ppsp_player is not default app for ppsp.me links", Toast.LENGTH_LONG).show();
//			// Show dialog to set myself as default
//			if (hash == null || !hash.equals("null")){
//				// avoids infinite loop (null comes from setting default dialog)
//				showDialog(SET_DEFAULT_DIALOG);
//			}
//			if (user_set_default_now){
//				return;
//			}
//		}

		if (hash == null || hash.equals("null")){
			// no link: show welcome
			setContentView(R.layout.welcome);
			Button b_twitter = (Button) findViewById(R.id.b_twitter);
			b_twitter.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse("https://twitter.com/ppsp_test"));
					startActivity(i);
				}
			});
			return;
		}
		Id id = null;
		try{
			id = new Id(hash);
		}
		catch(IdError e){
			Log.w("hash", "invalid");
			showDialog(INVALID_ID_DIALOG);
			return;
		}
		boolean showWarning = false;
		if (Util.isMobileConnectivity(getBaseContext())){
			// we are connected via mobile connectivity. Show warning, if preference checked.
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			if (prefs.getBoolean("pref_mobile_warning", true)){
				showWarning = true;
				showDialog(MOBILE_WARNING_DIALOG);
//
//				setContentView(R.layout.warning);
//				final CheckBox cb_mobile_warning = (CheckBox) findViewById(R.id.cb_mobile_warning);
//				cb_mobile_warning.setChecked(true);
//				Button b_continue = (Button) findViewById(R.id.b_continue);
//				b_continue.setOnClickListener(new OnClickListener() {
//					public void onClick(View v) {
//						if (!cb_mobile_warning.isChecked()){
//							//SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//							SharedPreferences.Editor editor = prefs.edit();
//							editor.putBoolean("pref_mobile_warning", false);
//							editor.commit(); //Raul: don't forget to commit edits!!
//							Log.w("intro", "Don't show Intro next time");
//						}
//						Intent intent = getPlayerIntent(hash,"",false);
//						startActivityForResult(intent, 0);
//
//					}  	
//				});
			}
		}
		if (!showWarning){
			Log.w("intro", "don't show warning: go to P2P directly");
			Intent intent = getPlayerIntent(hash,"",false);
			startActivityForResult(intent, BACK_FROM_PLAYER_CODE);
		}
	}
	
	
	
    /** Open phone's gallery when user clicks the button 'Select a video' */
    public void selectVideo(View view) {
    	Util.sendKillToDHT(); //just in case
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*"); // Only show videos
        startActivityForResult(intent, SELECT_VIDEO_FILE_REQUEST_CODE);
//        setTextFields();
    }
    
    /** Start phone's camera when user clicks the button 'Record a video' */
    public void startCamera(View view) {
    	Util.sendKillToDHT(); //just in case
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
//        setTextFields();
    }


	
    /** Arno: Start live broadcast when user clicks 'Start Live' */
    public void startLive(View view) {
		Intent intent = new Intent(this, SourceActivity.class);
		startActivity(intent);
	}

    /** Arno: Watch live broadcast when user clicks 'Watch Live' */
    public void watchLive(View view) 
    {
		DialogFragment dialog = new LiveIPDialogFragment();
		dialog.show(getSupportFragmentManager(), "LiveIPDialogFragment");
	}
	
	protected Dialog onCreateDialog(int id) {
		if (id == INVALID_ID_DIALOG){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Invalid PPSP link")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					IntroActivity.this.finish();
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		if (id == SET_DEFAULT_DIALOG){
			final String finalHash = hash;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("PPSP.me links cannot be played by browsers. We recommend setting ppsp_player as default app for PPSP.me links.")
			.setCancelable(false)
			.setPositiveButton("Set default now", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					user_set_default_now = true;
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse("http://ppsp.me/"+finalHash));
					Log.d("intro", "relaunch >> http://ppsp.me/"+finalHash);
					startActivity(i);
					IntroActivity.this.finish();
				}
			})
			.setNegativeButton("Later", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			});
			AlertDialog alert = builder.create();
			return alert;	
		}
		if (id == MOBILE_WARNING_DIALOG){
			final String finalHash = hash;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Using mobile connectivity.\nWe recommend using wi-fi to download PPSP videos.\nDo you want to play anyway?")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Intent intent = getPlayerIntent(hash,"",false);
					startActivityForResult(intent, 0);
					IntroActivity.this.finish();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			});
			AlertDialog alert = builder.create();
			return alert;	
		}
		return null;
	}
	
	private Intent getPlayerIntent(String hash, String tracker, boolean live){
		Intent intent = null;
		//found hash: play video
		intent = new Intent(getBaseContext(), VideoPlayerActivity.class);
		intent.putExtra("hash", hash);
		// Point to swift<->DHT interface
		if (tracker == "")
			tracker = "127.0.0.1:9999";//"192.16.127.98:20050"; //TODO
		intent.putExtra("tracker", tracker);
		intent.putExtra("live", live);
		return intent;
	}
	
	private String getHash(){
		Log.d("hhhh","getHash");
		String hash = null;
		Uri data = getIntent().getData(); 
		Uri datas = getIntent().getData(); 
		if (datas != null) { 
			Log.d("hhhh","datas");
			System.out.println("URI: " + datas);
			String scheme = data.getScheme(); 
			String host = data.getHost(); 
			//			  int port = data.getPort();
			if (scheme.equals("ppsp")){
				hash = host;
			}
			if (host.equals("ppsp.me")){
				hash = data.getLastPathSegment();
			}
			Log.w("videoplayer", "ppsp link: " + hash);
			return hash;
		}

		Bundle extras = getIntent().getExtras();
		if (extras != null){
			String text = extras.getString("android.intent.extra.TEXT");
			if (text != null){
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
					hash = null;
					Log.w("video twicca", "no ppsp link found");
				}
				return hash;
			}
			hash = extras.getString("hash");
			return hash;
		}
		return hash;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		boolean readyToTwit = false;
		Uri videoUri = null;
		
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
				if (resultCode == RESULT_OK && data.getDataString() != null) {
					videoUri = data.getData();
					Toast.makeText(this, "Video saved to:\n" + videoUri, Toast.LENGTH_LONG)
					.show();
				} else if (resultCode == RESULT_OK && data.getDataString() == null) {
//					if (DEBUG_MODE) {
//						Log.i(Log_Tag, "Problem in saving the video");
//					}
					Toast.makeText(this, "Problem in saving the video", Toast.LENGTH_LONG).show();
				} else if (resultCode == RESULT_CANCELED) {
					// User cancelled the video capture
				} else {
					// Video capture failed, advise user
				}
				break;
			case SELECT_VIDEO_FILE_REQUEST_CODE:
				if (resultCode == RESULT_OK) {
//					showTextFields(0);
//					setTextFields(data.getDataString(), data.getData().getLastPathSegment());
					videoUri = data.getData();
//					setVideoURI(vUri);
//					String vPath = getRealPathFromURI(vUri);
//					setVideoThumbnail(vPath);
					Toast.makeText(this, "User selected "+videoUri, Toast.LENGTH_LONG).show();
					Log.i("upload", "User selected "+videoUri);

				} else if (resultCode == RESULT_CANCELED) {
					// User cancelled the video selection
//					if (DEBUG_MODE) {
//						Log.i(Log_Tag, "User cancelled the video selection");
//					}
					Toast.makeText(this, "User cancelled the video selection", Toast.LENGTH_LONG)
					.show();
				} else {
					// Some other error, advise user
//					if (DEBUG_MODE) {
//						Log.i(Log_Tag, "Problem in selecting the video");
//					}
					Toast.makeText(this, "Problem in selecting the video", Toast.LENGTH_LONG)
					.show();
				}
				break;
			case BACK_FROM_PLAYER_CODE:
				Util.sendKillToDHT();
				Log.d("intro", "watching DONE");
				finish(); //User exited player. We're done.
				break;
			case BACK_FROM_UPLOAD_CODE:
				Util.sendKillToDHT();
				Log.d("intro", "upload DONE");
				
		}
		Log.d("intro", "after switch, code: " + requestCode );

		if (videoUri != null){
			Toast.makeText(this, "uri not null", Toast.LENGTH_LONG)
			.show();
			
 			Toast.makeText(this, "URI: "+videoUri, Toast.LENGTH_LONG)
			.show();
 			String filename = getRealPathFromURI(videoUri);
			Toast.makeText(this, "filename: "+filename, Toast.LENGTH_LONG)
			.show();
			Log.i("intro", "filename: "+filename);

			// Arno, 2012-11-28: Copy video to default beam location
			Copy4BeamTask beamtask = new Copy4BeamTask();
			beamtask.execute( filename );

			Intent intent = new Intent(getBaseContext(), UploadActivity.class);
			intent.putExtra("destination", filename);
			startActivityForResult(intent, BACK_FROM_UPLOAD_CODE);
		}
	}
	
	//Snipet from http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
	private String getRealPathFromURI(Uri contentUri) {
		//NOTE: CursorLoader requires API11.
		//TODO: Find out an alternative that works on API10 
		String[] proj = { MediaStore.Images.Media.DATA };
		CursorLoader loader = new CursorLoader(getBaseContext(), contentUri, proj, null, null, null);
		Cursor cursor = loader.loadInBackground();
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	

	/*
	 * Arno: Thread to execute swift Mainloop
	 */
	private class SwiftMainThread extends Thread{
		public void run(){
			try{
				NativeLib nativelib =  new NativeLib();
				String ret = nativelib.Init( "0.0.0.0:6778", "127.0.0.1:8082" );
				Log.w("introSwift", "Startup returned " + ret + "END");
				// Arno: Never returns, calls libevent2 mainloop
				if (!_inmainloop){
					_inmainloop = true;
					Log.w("introSwift","Entering libevent2 mainloop");
					nativelib.Mainloop();
					Log.w("introSwift","LEFT MAINLOOP!");
				}
			}
			catch (Exception e ){
				e.printStackTrace();
			}
		}
	}

	/** User clicks OK in LiveIP dialog */
	@Override
	public void onDialogPositiveClick(DialogFragment dialog) 
	{
		hash = "e5a12c7ad2d8fab33c699d1e198d66f79fa610c3";
		LiveIPDialogFragment d = (LiveIPDialogFragment)dialog;
		String tracker = d.getTracker();
		
		Intent intent = getPlayerIntent(hash, tracker, true);
		startActivity(intent);
	}

	/** User clicks Cancel in LiveIP dialog */
	@Override
	public void onDialogNegativeClick(DialogFragment dialog) 
	{
	}
	

	/*
	 * NFC + Beam
	 */

    /*
     * Should be called by all activities of this app.
     */
    
    public static void ConfigureNFCBeam(Activity act)
    {
		// Arno: Only configure when full Beam support. NFC is already in API 9.
		if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 16)
		{
	        // Check for available NFC Adapter
			NfcAdapter nfcadapter = NfcAdapter.getDefaultAdapter(act);
	        if (nfcadapter == null) 
	        {
	        	Log.w("Swift","Error when trying to invoke Beam API: getDefaultAdapter");
	            return;
	        }
	        
            String text = ("Beam me up, Android!\n\n" +
                    "Beam Time: " + System.currentTimeMillis());
            NdefMessage msg = new NdefMessage(
                    new NdefRecord[] { createMimeRecord(
                            "application/com.example.android.beam", text.getBytes())
              //,NdefRecord.createApplicationRecord("com.example.android.beam")
            });
	        
	        // Use reflection to detect if API 16 method is avail.
	        try
	        {
	        	Method method = NfcAdapter.class.getMethod("setNdefPushMessage", NdefMessage.class, Activity.class, Activity[].class);
	        	// http://stackoverflow.com/questions/5454249/java-reflection-getmethodstring-method-object-class-not-working
	        	method.invoke(nfcadapter, msg, act, new Activity[]{});
	        }
	        catch(Exception e)
	        {
	        	Log.w("Swift","Error when trying to invoke Beam API: setNdefPushMessage",e);
	        }

	        // Ideally this should be done dynamically when the Record video
	        // feature is used. However, then we have to call setBeamPushUris
	        // for all Activities that make up the application. Hence, we
	        // make this class the base class for all our activities and 
	        // announce a static file: URL to which the latest video is copied
	        // after Record is done.
	        
	        String urlstr = "file://"+IntroActivity.getDefaultBeamFilename();
	        Uri offeruri = Uri.parse(urlstr);
	        
	        // Use reflection to detect if API 16 method is avail.
	        try
	        {
	        	Method method = NfcAdapter.class.getMethod("setBeamPushUris", Uri[].class, Activity.class);
	        	// Offer for transfer when bumping
	        	method.invoke(nfcadapter, new Uri[] {offeruri}, act);
	        }
	        catch(Exception e)
	        {
	        	Log.w("Swift","Error when trying to invoke Beam API: setBeamPushUris",e);
	        }
	        
	        Log.w("Swift","Beam API: registered " + urlstr );
		}
		else
			Log.w("Swift","Beam API: Android version too old " + android.os.Build.VERSION.SDK );

	}
    
    
    /**
     * Creates a custom MIME type encapsulated in an NDEF record
     */
    public static NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }

    
    public static String getDefaultBeamFilename()
    {
    	return IntroActivity._beamdefaultfilename;
    }
    
    
	/**
	 * sub-class of AsyncTask. Copies file to default beam location.
	 */
	private class Copy4BeamTask extends AsyncTask<String, Integer, String> {
	
		protected String doInBackground(String... args) {
	
			String ret = "hello";
			if (args.length != 1) {
				ret = "Received wrong number of parameters during initialization!";
			}
			else 
			{
				String sourcefilename = args[0];
				String destfilename = IntroActivity.getDefaultBeamFilename();
				
				Log.w("SwiftBeam", "Copy "+sourcefilename+" to "+destfilename );
				try 
				{
					InputStream in = new FileInputStream(sourcefilename);
				    OutputStream out = new FileOutputStream(destfilename);
				    byte[] buf = new byte[1024];
				    int len;
				    while ((len = in.read(buf)) > 0) {
				        out.write(buf, 0, len);
				    }
				    in.close();
				    out.close();
				    
				    Log.w("SwiftBeam", "Copy succesful" );
				}
				catch (Exception e ) 
				{
					e.printStackTrace();
					ret = e.toString();
				}
			}
			return ret;
		}
	}
}
