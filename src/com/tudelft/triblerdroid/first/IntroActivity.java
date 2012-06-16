//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class IntroActivity extends Activity {
	Button b_continue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  
	  
		setContentView(R.layout.intro);
		b_continue = (Button) findViewById(R.id.b_continue);
		b_continue.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setContentView(R.layout.pythonautoinstall);
				Intent intent = new Intent(getBaseContext(), PythonAutoinstallActivity.class);
				startActivityForResult(intent, 0);
			}  	
		});
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Done, exit application
        finish();
	}
}
