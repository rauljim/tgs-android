//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  		
		setContentView(R.layout.video_upload);
	
		Bundle extras = getIntent().getExtras();
		hash  = extras.getString("hash");
		destination  = extras.getString("filename");

		//generate roothash
		Log.d("upload", "init hash: "+hash);
		//FIXME
//		String zeroHash = "0000000000000000000000000000000000000000";
		tracker = "192.16.127.98:20050";

		_swiftMainThread = new SwiftMainThread();
		_swiftMainThread.start();

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse("https://twitter.com/intent/tweet?&text=I+just+uploaded+a+video.+Check+it+out!+&url=http://ppsp.me/"+hash));
		startActivity(i);
		//int progr = nativelib.mainloop();

		
	
		//TODO: seed on background until one full copy is out
//        finish();
	}
	

	private class SwiftMainThread extends Thread{

		public void run(){
			try{
				NativeLib nativelib =  new NativeLib();
				String ret = nativelib.start(hash, tracker, destination);
				//startVideoPlayback(); //Raul, 120920: moved to onCreate
				// Arno: Never returns, calls libevent2 mainloop
				if (!inmainloop){
					inmainloop = true;
					Log.w("upload-Swift","Entering libevent2 mainloop");
					int progr = nativelib.mainloop();
					Log.w("upload-Swift","LEFT MAINLOOP!");
				}
			}
			catch (Exception e ){
				e.printStackTrace();
			}
		}
	}

}
