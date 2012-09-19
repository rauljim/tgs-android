package com.tudelft.triblerdroid.first;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import me.ppsp.test.R;

import org.apache.http.Header;

public class Preferences extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		Preference button = (Preference)findPreference("pref_clean_now");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
            	String dir_path = Environment.getExternalStorageDirectory().getPath() + "/swift";
            	File f = new File(dir_path);
                if(f.isDirectory()){
                	String files[]=  f.list();
                  	for(int i=0;i<files.length;i++){
                  		new File(dir_path, files[i]).delete();

                  	}
                }

            	Toast.makeText(getApplicationContext(), "All videos DELETED", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
		button = (Preference)findPreference("pref_tweet");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) { 
            	String tweetUrl = "https://twitter.com/intent/tweet?text=" +
            			"Video description ppsp.me/2b2fe5f1462e5b7ac4d70fa081e0169160b2d3a6";
            	Uri uri = Uri.parse(tweetUrl);
            	startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
		button = (Preference)findPreference("pref_share");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) { 
            	 Intent share = new Intent(Intent.ACTION_SEND);
            	 share.putExtra(Intent.EXTRA_TEXT, 
            			 "Video description ppsp.me/2b2fe5f1462e5b7ac4d70fa081e0169160b2d3a6");
            	 startActivity(Intent.createChooser(share, "Share this via"));
                return true;
            }
        });
		
	}
}
