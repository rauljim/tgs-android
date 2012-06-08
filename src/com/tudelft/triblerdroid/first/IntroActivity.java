package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class IntroActivity extends Activity {

	String swiftFolder = "/swift";
	String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	Button b_continue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  
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
}
