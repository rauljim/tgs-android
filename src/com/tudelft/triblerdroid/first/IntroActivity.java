//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class IntroActivity extends Activity {
    public static final String PREFS_NAME = "settings.dat";
    Button b_continue;
    CheckBox cb_showIntro;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  
		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean showIntro = settings.getBoolean("showIntro", true);

		if (showIntro) {
			setContentView(R.layout.intro);
			cb_showIntro = (CheckBox) findViewById(R.id.cb_show_intro);
			cb_showIntro.setChecked(true);
			b_continue = (Button) findViewById(R.id.b_continue);
			b_continue.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (!cb_showIntro.isChecked()){
						//SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean("showIntro", false);
						editor.commit(); //Raul: don't forget to commit edits!!
						Log.w("intro", "Don't show Intro next time");
					}
//					setContentView(R.layout.pythonautoinstall);
					Intent intent = new Intent(getBaseContext(), P2PStartActivity.class);
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
			});
		}
		else
		{
			Log.w("intro", "don't show intro: go to P2P directly");
			Intent intent = new Intent(getBaseContext(), P2PStartActivity.class);
			Bundle extras = getIntent().getExtras();
			if (extras != null){
				intent.putExtras(extras);
			}
			startActivityForResult(intent, 0);
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Done, exit application
        finish();
	}
}
