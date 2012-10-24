//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ppsp.test.R;

import se.kth.pymdht.Id;
import se.kth.pymdht.Id.IdError;

public class IntroActivity extends Activity {
    private static final int SELECT_VIDEO_FILE_REQUEST_CODE = 200;

    public static final String PREFS_NAME = "settings.dat";

	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 100;
//    CheckBox cb_showIntro;
    String hash = null;
    boolean user_set_default_now = false;
    public int INVALID_ID_DIALOG = 0;
    public int SET_DEFAULT_DIALOG = 1;
    public int MOBILE_WARNING_DIALOG = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  
		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean showIntro = settings.getBoolean("showIntro", true);
		
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
//						Intent intent = getPlayerIntent(hash);
//						startActivityForResult(intent, 0);
//
//					}  	
//				});
			}
		}
		if (!showWarning){
			Log.w("intro", "don't show warning: go to P2P directly");
			Intent intent = getPlayerIntent(hash);
			startActivityForResult(intent, 0);
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
					Intent intent = getPlayerIntent(hash);
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
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
				if (resultCode == RESULT_OK && data.getDataString() != null) {
					Toast.makeText(this, "Video saved to:\n" + data.getData(), Toast.LENGTH_LONG)
					.show();
					readyToTwit = true;
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
					Uri vUri = data.getData();
//					setVideoURI(vUri);
//					String vPath = getRealPathFromURI(vUri);
//					setVideoThumbnail(vPath);
					Toast.makeText(this, "User selected "+vUri, Toast.LENGTH_LONG).show();
					readyToTwit = true;
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
		if (readyToTwit){
			//TODO generate roothash
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse("https://twitter.com/intent/tweet?&text=YourTextHere+&url=http://ppsp.me/1234567890123456789012345678901234567890"));
			startActivity(i);
		}
		// Done, exit application
		//TODO: seed on background until one full copy is out
        finish();
	}
}
