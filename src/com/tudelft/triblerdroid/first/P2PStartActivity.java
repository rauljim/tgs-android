//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.facade.ActivityResultFacade;
import com.googlecode.android_scripting.interpreter.InterpreterUtils;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;

public class P2PStartActivity extends Activity {

	private SwiftService scriptService = null;
	
  @Override
  protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.p2p);
	  //TOO copyResources ONLY if app upgrade (use SharedPreferences)
	  copyResourcesToLocal();
  }
	
  private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}

  protected void startP2PEngine() {
	  
	/* Arno, 2012-03-05: Moved from onCreate, such that we only launch the
	 * service when Python is installed.
	 */
	Log.w("QMediaPython","prepareUninstallButton");
    if (1==1){//Constants.ACTION_LAUNCH_SCRIPT_FOR_RESULT.equals(getIntent().getAction())) {
    	
      // Arno: layout moved up
      //setTheme(android.R.style.Theme_Dialog);
      //setContentView(R.layout.dialog);
      ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          scriptService = ((SwiftService.LocalBinder) service).getService();
          try {
            RpcReceiverManager manager = scriptService.getRpcReceiverManager();
            ActivityResultFacade resultFacade = manager.getReceiver(ActivityResultFacade.class);
            resultFacade.setActivity(P2PStartActivity.this);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          // Ignore.
        }
      };
//      Raul, 2012-03-28: This creates problems when restarting P2P
//      bindService(new Intent(this, ScriptService.class), connection, Context.BIND_AUTO_CREATE);
      startService(new Intent(this, SwiftService.class));
    } else {
    	
    	
      ScriptApplication application = (ScriptApplication) getApplication();
      if (application.readyToStart()) {
        startService(new Intent(this, SwiftService.class));
      }
      // Arno, 2012-02-15: Hack to keep this activity alive.
      // finish();
    }
//    Raul, 2012-03-26: Autoinstall done, show video list (no need for button) 
    Intent intent;
	Bundle extras = getIntent().getExtras();
	if (extras != null){
		intent = new Intent(getBaseContext(), VideoPlayerActivity.class);
		intent.putExtras(extras);
	}
	else{
		intent = new Intent(getBaseContext(), VideoListActivity.class);
	}
	startActivityForResult(intent, 0);
  }

  
	public void stopP2PEngine()
	{
		stopService(new Intent(getBaseContext(), SwiftService.class));
//		unbindService(scriptService);
		
		// Arno, 2012-03-23: Don't work if called by TimerTask :-(
		// Toast.makeText(getBaseContext(), "P2P Engine DOWN", Toast.LENGTH_LONG).show();
		
		String msg = "KILL_DHT";
		InetAddress IPAddress = null;
		try {
			IPAddress = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		DatagramPacket sendPacket = 
				new DatagramPacket(msg.getBytes(), msg.length(), IPAddress, 9999); 
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		try {
			clientSocket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		clientSocket.close(); 
	}
	
	private void copyResourcesToLocal() {
		String name, sFileName;
		InputStream content;
		R.raw a = new R.raw();
		java.lang.reflect.Field[] t = R.raw.class.getFields();
		Resources resources = getResources();
		for (int i = 0; i < t.length; i++) {
			try {
				name = resources.getText(t[i].getInt(a)).toString();
				sFileName = name.substring(name.lastIndexOf('/') + 1, name
						.length());
				content = getResources().openRawResource(t[i].getInt(a));

				// Copies script to internal memory only if changes were made
				sFileName = InterpreterUtils.getInterpreterRoot(this)
						.getAbsolutePath()
						+ "/" + sFileName;
				if (needsToBeUpdated(sFileName, content)) {
					Log.d("Swift", "Copying from stream " + sFileName);
					content.reset();
					FileUtils.copyFromStream(sFileName, content);
				}
				FileUtils.chmod(new File(sFileName), 0755);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private boolean needsToBeUpdated(String filename, InputStream content) {
		File script = new File(filename);
		FileInputStream fin;
		Log.d("Swift", "Checking if " + filename + " exists");

		if (!script.exists()) {
			Log.d("Swift", "not found");
			return true;
		}

		Log.d("Swift", "Comparing file with content");
		try {
			fin = new FileInputStream(filename);
			int c;
			while ((c = fin.read()) != -1) {
				if (c != content.read()) {
					Log.d("Swift", "Something changed replacing");
					return true;
				}
			}
		} catch (Exception e) {
			Log.d("Swift", "Something failed during comparing");
//			Log.e("Swift", e);
			return true;
		}
		Log.d("Swift", "No need to update " + filename);
		return false;
	}

  	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Done, exit application
		stopP2PEngine();
        finish();
	}
  	
  	@Override
  	public void onStart()
  	{
  		super.onStart();
  		Log.w("Swift","P2PStartActivity.onStart" );
  		startP2PEngine();
  	}
}
