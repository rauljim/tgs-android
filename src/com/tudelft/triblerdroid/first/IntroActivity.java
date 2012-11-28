//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.DialogFragment;
import android.content.CursorLoader;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;

import me.ppsp.test.R;
import com.tudelft.triblerdroid.swift.NativeLib;
import com.tudelft.triblerdroid.first.SourceActivity;


import se.kth.pymdht.Id;
import se.kth.pymdht.Id.IdError;

public class IntroActivity extends FragmentActivity implements LiveIPDialogFragment.LiveIPDialogListener 
{
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 100;
    private static final int SELECT_VIDEO_FILE_REQUEST_CODE = 200;
    private static final int BACK_FROM_PLAYER_CODE = 300;

    public static final String PREFS_NAME = "settings.dat";

//    CheckBox cb_showIntro;
    String hash = null;
    boolean user_set_default_now = false;
    public int INVALID_ID_DIALOG = 0;
    public int SET_DEFAULT_DIALOG = 1;
    public int MOBILE_WARNING_DIALOG = 2;

	// Arno, 2012-11-27: Swift mainloop run here.
	private SwiftMainThread _swiftMainThread = null;
    private boolean _inmainloop = false;
    
    // Arno, 2012-11-28: NFC + Beam
    protected NfcAdapter _nfcadapter = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  
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


		// Arno: Only configure when full Beam support. NFC is already in API 9.
		if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 16)
		{
	        // Check for available NFC Adapter
	        _nfcadapter = NfcAdapter.getDefaultAdapter(this);
	        if (_nfcadapter == null) 
	        {
	        	Log.w("Swift","Error when trying to invoke Beam API: getDefaultAdapter");
	            return;
	        }
	        
            String text = "PPSPBeam: Sent latest video";
            NdefMessage msg = new NdefMessage(
	                    new NdefRecord[] { createMimeRecord(
	                            "application/com.tudelft.android.beam", text.getBytes())  });
	        
	        // Use reflection to detect if API 16 method is avail.
	        try
	        {
	        	Method method = NfcAdapter.class.getMethod("setNdefPushMessage", NdefMessage.class, Activity.class, Activity[].class);
	        	// http://stackoverflow.com/questions/5454249/java-reflection-getmethodstring-method-object-class-not-working
	        	method.invoke(_nfcadapter, msg, this, new Activity[]{});
	        }
	        catch(Exception e)
	        {
	        	Log.w("Swift","Error when trying to invoke Beam API: setNdefPushMessage",e);
	        }
	        
	        Log.w("Swift","Configured app for Beam API");
		}
		else
			Log.w("Swift","Beam API: Android version too old " + android.os.Build.VERSION.SDK );
		

		
		
		
		
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*"); // Only show videos
        startActivityForResult(intent, SELECT_VIDEO_FILE_REQUEST_CODE);
//        setTextFields();
    }
    
    /** Start phone's camera when user clicks the button 'Record a video' */
    public void startCamera(View view) {
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
		if (tracker == "")
			tracker = "192.16.127.98:20050"; //TODO
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
				Log.d("intro", "DONE");
				finish(); //User exited player. We're done.
				break;
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

			
			// Arno, 2012-11-28: Register video for bump transfer
			if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 16 && _nfcadapter != null )
			{
		        String s = "file://"+filename; // /system/media/video/AndroidInSpace.240p.mp4";
		        Uri offeruri = Uri.parse(s);
		        
		        // Use reflection to detect if API 16 method is avail.
		        try
		        {
		        	Method method = NfcAdapter.class.getMethod("setBeamPushUris", Uri[].class, Activity.class);
		        	// Offer for transfer when bumping
		        	method.invoke(_nfcadapter, new Uri[] {offeruri}, this);
		        }
		        catch(Exception e)
		        {
		        	Log.w("Swift","Error when trying to invoke Beam API: setBeamPushUris",e);
		        }
		        
		        Log.w("Swift","Beam API: registered " + filename );
			}	
			else
				Log.w("Swift","Error registering with Beam API: " + filename + " adapter " + _nfcadapter + "API " + android.os.Build.VERSION.SDK);

			Intent intent = new Intent(getBaseContext(), UploadActivity.class);
			intent.putExtra("destination", filename);
			startActivity(intent);
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
				Log.w("Swift", "Startup returned " + ret + "END");
				// Arno: Never returns, calls libevent2 mainloop
				if (!_inmainloop){
					_inmainloop = true;
					Log.w("Swift","Entering libevent2 mainloop");
					nativelib.Mainloop();
					Log.w("Swift","LEFT MAINLOOP!");
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

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        //textView = (TextView) findViewById(R.id.text);
    	
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        //textView.setText(new String(msg.getRecords()[0].getPayload()));
    }

    /**
     * Creates a custom MIME type encapsulated in an NDEF record
     */
    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }

}
