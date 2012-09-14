package com.tudelft.triblerdroid.first;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.apps.analytics.AnalyticsReceiver;

public class Receiver extends AnalyticsReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Bundle extras = intent.getExtras();
		String referrerString = extras.getString("referrer");
			Log.w("TEST", "Referrer is: " + referrerString);
	
			Intent introIntent = new Intent(context, com.tudelft.triblerdroid.first.IntroActivity.class);
			introIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			introIntent.putExtra("hash", referrerString);
			context.startActivity(introIntent);
	}
}
