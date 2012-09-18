package com.tudelft.triblerdroid.first;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.widget.Button;

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
            	Log.d("prefs", "CLEAN UP");
                return true;
            }
        });
	}
}
