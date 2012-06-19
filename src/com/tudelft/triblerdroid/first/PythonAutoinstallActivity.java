//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PythonAutoinstallActivity extends PythonInstallIntegration {


	
  @Override
  protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  
	  
	  setContentView(R.layout.pythonautoinstall);

//	  Raul, 2012-03-09: moved here because pymdht creates files in this directory
	  // create dir for swift
	  String swiftFolder = "/swift";
	  String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	  try
	  {
		  File mySwiftFolder = new File(extStorageDirectory + swiftFolder);
		  mySwiftFolder.mkdir();
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
	  }

	  // ARNO TEST
	  File pythonBin = new File("/data/data/"+getClass().getPackage().getName()+"/files/python/bin/python");
	  if (pythonBin.exists() && pythonBin.canExecute()){
		  setInstalled(true);
	  }
	  else{
		  String pythonPath = extStorageDirectory + "/python-for-android-files/";
		  try
		  {
			  File myPythonFolder =  new File(pythonPath);
			  myPythonFolder.mkdir();
			  
		  }
		  catch(Exception e)
		  {
			  e.printStackTrace();
		  }
		  Log.w("Swift", "Copy Python interpreter to sdcard");
		  String[] filenames = {"python_extras_r14.zip", "python_r16.zip"};
		  AssetManager assetManager = getAssets();
		  InputStream in = null;
		  OutputStream out = null;
		  for(String filename : filenames) {
			  try {
				  in = assetManager.open(filename);
				  out = new FileOutputStream(pythonPath + filename);
				  copyFile(in, out);
				  in.close();
				  out.close();
			  }catch(Exception e) {
				  Log.e("Swift", e.getMessage());
		      }    
			  
		  }

		  setInstalled(false);
	  }
  }
	
  private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}
  @Override
  protected void prepareUninstallButton() {
	  
	/* Arno, 2012-03-05: Moved from onCreate, such that we only launch the
	 * service when Python is installed.
	 */
	Log.w("QMediaPython","prepareUninstallButton");
//    Raul, 2012-03-26: Autoinstall done, show video list (no need for button) 
    Intent intent = new Intent(getBaseContext(), P2PStartActivity.class);
	Bundle extras = getIntent().getExtras();
	if (extras != null){
		intent.putExtras(extras);
	}
	startActivityForResult(intent, 0);
  }

	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Done, exit application
        finish();
	}
}
