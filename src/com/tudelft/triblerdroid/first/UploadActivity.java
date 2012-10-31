//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ppsp.test.R;

public class UploadActivity extends Activity {
    private static final int SELECT_VIDEO_FILE_REQUEST_CODE = 200;

    public static final String PREFS_NAME = "settings.dat";

	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 100;
//    CheckBox cb_showIntro;
    boolean user_set_default_now = false;
    public int INVALID_ID_DIALOG = 0;
    public int SET_DEFAULT_DIALOG = 1;
    public int MOBILE_WARNING_DIALOG = 2;

    String hash = null;
    String destination;
	String tracker;
	boolean inmainloop = false;

	private SwiftMainThread _swiftMainThread;

	private NativeLib nativelib;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  		
		setContentView(R.layout.video_upload);

		//generate roothash
		
		//FIXME
		hash = "0000000000000000000000000000000000000000";
		tracker = "192.16.127.98:20050";
		destination = "/sdcard/swift/video.ts";

		nativelib =  new NativeLib();		
		String ret = nativelib.start(hash, tracker, destination);
		hash = nativelib.roothash(0);
		Toast.makeText(this, "HASH1: "+hash, Toast.LENGTH_LONG)
		.show();
		_swiftMainThread = new SwiftMainThread();
		_swiftMainThread.start();
		Toast.makeText(this, "HASH2: "+hash, Toast.LENGTH_LONG)
		.show();
		
//		Intent i = new Intent(Intent.ACTION_VIEW);
//		i.setData(Uri.parse("https://twitter.com/intent/tweet?&text=I+just+uploaded+a+video.+Check+it+out!+&url=http://ppsp.me/"+rootHash));
//		startActivity(i);
		//int progr = nativelib.mainloop();

		
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

	
	protected Dialog onCreateDialog(int id) {
		if (id == INVALID_ID_DIALOG){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Invalid PPSP link")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					UploadActivity.this.finish();
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
					UploadActivity.this.finish();
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
					Intent intent = getPlayerIntent(hash);
					startActivityForResult(intent, 0);
					UploadActivity.this.finish();
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
	
	private Intent getPlayerIntent(String hash){
		Intent intent = null;
		//found hash: play video
		intent = new Intent(getBaseContext(), VideoPlayerActivity.class);
		intent.putExtra("hash", hash);
		return intent;
	}
	
	private String getHash(){
		Log.d("hhhh","getHash");
		String hash = null;
		String tracker = "192.16.127.98:20050"; //TODO
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
				tracker = "192.16.127.98:20050"; //TODO
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
//					showTextFields(0);
//					setTextFields(data.getDataString(), data.getData().getLastPathSegment());
//					Uri vUri = data.getData();
//					setVideoURI(vUri);
//					String vPath = getRealPathFromURI(vUri);
//					setVideoThumbnail(vPath);
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
		}
		Toast.makeText(this, "after switch", Toast.LENGTH_LONG)
		.show();

		if (videoUri != null){
			//generate roothash
			Toast.makeText(this, "uri not null", Toast.LENGTH_LONG)
			.show();

			//FIXME
			hash = "0000000000000000000000000000000000000000";
			tracker = "192.16.127.98:20050";
			destination = "/sdcard/swift/video.ts";

			nativelib =  new NativeLib();		
			String ret = nativelib.start(hash, tracker, destination);
			String rootHash = hash;//nativelib.roothash(0);
			_swiftMainThread = new SwiftMainThread();
			_swiftMainThread.start();
			Toast.makeText(this, "HASH: "+hash, Toast.LENGTH_LONG)
			.show();

			
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse("https://twitter.com/intent/tweet?&text=I+just+uploaded+a+video.+Check+it+out!+&url=http://ppsp.me/"+rootHash));
			startActivity(i);
//			int progr = nativelib.mainloop();

		}
		// Done, exit application
		//TODO: seed on background until one full copy is out
//        finish();
	}
	
	private class SwiftMainThread extends Thread{

		public void run(){
			try{
				if (!inmainloop){
					inmainloop = true;
					Log.w("Swift","Entering libevent2 mainloop");
					int progr = nativelib.mainloop();
					Log.w("Swift","LEFT MAINLOOP!");
				}
			}
			catch (Exception e ){
				e.printStackTrace();
			}
		}
	}

}
