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

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Alexey Reznichenko (alexey.reznichenko@gmail.com)
 */
public class IntroActivity extends Activity {

	String swiftFolder = "/swift";
	String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	
	Button b_continue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  
	  
		setupEnvironment();
		setContentView(R.layout.intro);
		b_continue = (Button) findViewById(R.id.b_continue);
		b_continue.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//setContentView(R.layout.pythonautoinstall);
				Intent intent = new Intent(getBaseContext(), VideoListActivity.class);
				startActivity(intent);
			}  	
		});

	}
	
	protected void setupEnvironment(){
		createSwiftDirectory();
		checkPythonBinaries();
	}
	
	private void createSwiftDirectory(){
		// create dir for swift
		try
		{
			File mySwiftFolder = new File(extStorageDirectory + swiftFolder);
			mySwiftFolder.mkdir();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean checkPythonBinaries(){
		File pythonBin = new File("/data/data/"+getClass().getPackage().getName()+"/files/python/bin/python");
		boolean isInstalled = (pythonBin.exists() && pythonBin.canExecute());
		if (!isInstalled){
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
		  }
		return isInstalled;
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}

}