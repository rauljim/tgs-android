package com.tudelft.triblerdroid.first;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;


class Util{
	public static final int NO_CONNECTIVITY = 10000;

	static public String getConnectivity(Context context){
		int netType = getNetType(context);
		if (netType == NO_CONNECTIVITY) {
			return "OFF-LINE";
		}
		if (netType == ConnectivityManager.TYPE_MOBILE){
			return "MOBILE";
		}
		if (netType == 9){//ConnectivityManager.TYPE_ETHERNET){ Needs API13
			return "ETHERNET";
		}
		if (netType == ConnectivityManager.TYPE_WIFI){
			return "WIFI";
		}
		if (netType == ConnectivityManager.TYPE_WIMAX){
			return "WIMAX";
		}
		return "UNKNOWN";
	}
	
	static public boolean isMobileConnectivity(Context context){
		int netType = getNetType(context);
		return (netType == ConnectivityManager.TYPE_MOBILE);
	}
	
	static private int getNetType(Context context){
		ConnectivityManager mConnectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null) {
			//no connection at all
			return NO_CONNECTIVITY;
		}
		return info.getType();

	}
	
	static public void deleteAllSDContent(){
		String dir_path = Environment.getExternalStorageDirectory().getPath() + "/swift";
		File f = new File(dir_path);
		if(f.isDirectory()){
			String files[]=  f.list();
			for(int i=0;i<files.length;i++){
				new File(dir_path, files[i]).delete();

			}
		}
	}
	
	static public void mkdirSDContent(){
		try{
			String swiftFolder = "/swift";
			String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
			File mySwiftFolder = new File(extStorageDirectory + swiftFolder);
			mySwiftFolder.mkdir();	  
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}