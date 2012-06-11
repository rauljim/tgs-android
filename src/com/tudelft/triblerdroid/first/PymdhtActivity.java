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
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.ScriptLauncher;
import com.googlecode.android_scripting.interpreter.Interpreter;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;
import com.googlecode.android_scripting.interpreter.InterpreterUtils;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManager;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * A service that allows scripts and the RPC server to run in the background.
 * 
 * @author Alexey Reznichenko (alexey.reznichenko@gmail.com)
 * @author Manuel Naranjo (manuel@aircable.net)
 */
public class PymdhtActivity extends Activity {

	private InterpreterConfiguration mInterpreterConfiguration;
	private RpcReceiverManager mFacadeManager;
    private AndroidProxy mProxy;
	private final CountDownLatch mLatch = new CountDownLatch(1);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInterpreterConfiguration = ((BaseApplication) getApplication())
				.getInterpreterConfiguration();
	}

	// @Override
	//public void onDestroy() {
	//     Toast.makeText(this, "Arno says: service done", Toast.LENGTH_SHORT).show(); 
	//}

	
	@Override
	public void onStart() {
		super.onStart();
		try
		{
			startP2PEngine();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		catch(ExceptionInInitializerError e)
		{
			// Arno: When the moons are not properly aligned, 
			// Executing /data/data/com.googlecode.pythonforandroid/files/python/bin/python with arguments [/data/data/com.tudelf
			// throws an 02-16 13:54:41.877 W/dalvikvm(27642): Exception Ljava/lang/UnsatisfiedLinkError;
			// thrown while initializing Lcom/googlecode/android_scripting/Exec;
			//
			// I catch that Error here so swift part stays alive until moons
			// are aligned again.
			e.printStackTrace();
		}
	}
	
	RpcReceiverManager getRpcReceiverManager() throws InterruptedException {
		mLatch.await();
		if (mFacadeManager==null) { // Facade manage may not be available on startup.
			mFacadeManager = mProxy.getRpcReceiverManagerFactory()
					.getRpcReceiverManagers().get(0);
		}
		return mFacadeManager;
	}
	
	private void startP2PEngine(){
		String fileName = Script.getFileName(this);
		
		Log.w("Arno: Looking for interpreter for script " + fileName );
		
		Interpreter interpreter = null;
		for (int i=0; i<10; i++) {
			// Arno, 2012-03-06: Sometimes the interpreter detection stuff
			// doesn't appear to be ready when this is called. Calling it
			// multiple times seems to help?
			
			interpreter = mInterpreterConfiguration
				.getInterpreterForScript(fileName);
		}
		
		if (interpreter == null || !interpreter.isInstalled()) {
			return;
		}
		// Copies script to internal memory.
		fileName = InterpreterUtils.getInterpreterRoot(this).getAbsolutePath()
				+ "/" + fileName;
		File script = new File(fileName);
		// TODO(raaar): Check size here!
		if (!script.exists()) {
			script = FileUtils.copyFromStream(fileName, getResources()
					.openRawResource(Script.ID));
		}
//		mProxy = new AndroidProxy(this, null, true);
		mProxy.startLocal();
		mLatch.countDown();
//		2012-03-20, Raul: this line crashes
//		03-20 13:25:23.815: E/sl4a.StreamGobbler:108(3875): java.io.FileNotFoundException: /mnt/sdcard/sl4a/script.py.log: open failed: ENOENT (No such file or directory)

		ScriptLauncher.launchScript(script, mInterpreterConfiguration,
				mProxy, new Runnable() {
			@Override
			public void run() {
				mProxy.shutdown();
			}
		});
		
	}
	
	private void stopP2PEngine(){
		
	}

}
