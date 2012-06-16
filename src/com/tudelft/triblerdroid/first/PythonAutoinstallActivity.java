/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
//import com.tudelft.triblerdroid.first.PythonInstallIntegration;

/**
 * @author Alexey Reznichenko (alexey.reznichenko@gmail.com)
 */

public class PythonAutoinstallActivity extends PythonInstallIntegration {
	
	private String MkdirSwift(){
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
		  return extStorageDirectory;
	}

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
//	  Raul, 2012-06-15: super.onCreate will call
//	  prepareInstallButton (if python not installed)
//	  or
//	  prepareUninstallButton (if Python is installed)
	  setContentView(R.layout.pythonautoinstall);
  }

  private boolean arnoTest(){
	  boolean python_is_installed = false;
	  String extStorageDirectory = MkdirSwift();
	  // ARNO TEST
	  File pythonBin = new File("/data/data/"+getClass().getPackage().getName()+"/files/python/bin/python");
	  if (pythonBin.exists() && pythonBin.canExecute()){
		  setInstalled(true); //Raul: where is setInstalled this defined????
		  python_is_installed = true;
		  Log.w("autoinstall", "arno test OK");
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
		  Log.w("autoinstall", "Copy Python interpreter to sdcard");
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
				  Log.e("autoinstall", e.getMessage());
		      }    
			  
		  }
		  Log.w("autoinstall", "arno test FAIL");
		  setInstalled(false); //Raul: where is setInstalled this defined????
		  python_is_installed = false;
	  }
	  return python_is_installed;
  }
  
  private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}
  @Override
  protected void prepareInstallButton() {
	  // This is called if not installed
	  // see http://android-scripting.googlecode.com/hg-history/22a732e968f7f2cd8375fcad23dc2b6d537b5f58/android/PythonForAndroid/src/com/googlecode/pythonforandroid/Main.java
	  Log.w("autoinstall", "done FAIL >> installing");
	  boolean pythonIsInstalled = arnoTest();
	  if (pythonIsInstalled){
		  Log.w("autoinstall", "arno test OK");
	  }
	  else{
		  Log.w("autoinstall", "arno test FALSE");
	  }
	  install();//super.prepareInstallButton();
//	  Log.w("autoinstall", "done OK (optimistic)");
//	  Intent intent=new Intent();
//	  setResult(RESULT_OK, intent);
//	  finish();
  }
  
  @Override
  protected void prepareUninstallButton() {
	  // This is called if the interpreted is already installed or
	  // it has been just installed.
	  // see http://android-scripting.googlecode.com/hg-history/22a732e968f7f2cd8375fcad23dc2b6d537b5f58/android/PythonForAndroid/src/com/googlecode/pythonforandroid/Main.java
	  Log.w("autoinstall", "done OK");
	  Intent intent=new Intent();
	  setResult(RESULT_OK, intent);
	  //finish();
  }
}
