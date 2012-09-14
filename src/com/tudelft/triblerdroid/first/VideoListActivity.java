package com.tudelft.triblerdroid.first;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Timer;

public class VideoListActivity extends ListActivity {
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	  super.onCreate(savedInstanceState);
    	  
    	  setListAdapter(new ArrayAdapter<String>(this, me.ppsp.test.R.layout.list_item, VIDEOS));

    	  ListView lv = getListView();
    	  lv.setTextFilterEnabled(true);

    	  lv.setOnItemClickListener(new OnItemClickListener() {
    	    public void onItemClick(AdapterView<?> parent, View view,
    	        int position, long id) {
    	    	// Play video
    	    	Intent intent = new Intent(getBaseContext(), VideoPlayerActivity.class);
    	    	intent.putExtra("hash", HASHES[position]);
    	    	// Arno, 2012-03-22: Default tracker is central tracker, swift now
    	    	// has a default local peer which is the DHT.
    	    	// intent.putExtra("tracker", "192.16.127.98:20050"); // KTH's tracker
    	    	intent.putExtra("tracker", "tracker3.p2p-next.org:20050"); // Delft's tracker
    	    	//intent.putExtra("tracker", "127.0.0.1:9999"); // DHT
    	    	//intent.putExtra("destination", destination);
    	    	startActivityForResult(intent, 0);
    	    }
    	  });
    } 
    
    // If you change/add here, change HASHES[] order as well!
    static final String[] VIDEOS = new String[] {
		"(480p) TED: Ken Robinson says schools kill creativity", 
		"(480p-low) TED: Ken Robinson says schools kill creativity", 
		"(480p) TED: Jill Bolte Taylor's stroke of insight", 
		"(480p-low) TED: Jill Bolte Taylor's stroke of insight", 
		"(480p) TED: Pranav Mistry: The thrilling potential of SixthSense technology", 
		"(480p-low) TED: Pranav Mistry: The thrilling potential of SixthSense technology", 
		"(480p) TED: David Gallo shows underwater astonishments", 
		"(480p-low) TED: David Gallo shows underwater astonishments", 
		"(480p) TED: Pattie Maes and Pranav Mistry demo SixthSense", 
		"(480p-low) TED: Pattie Maes and Pranav Mistry demo SixthSense", 
		"(480p) TED: Simon Sinek: How great leaders inspire action", 
		"(480p-low) TED: Simon Sinek: How great leaders inspire action", 
		"(480p) TED: Arthur Benjamin does 'Mathemagic'", 
		"(480p-low) TED: Arthur Benjamin does 'Mathemagic'", 
		"(480p) TED: Hans Rosling shows the best stats you've ever seen", 
		"(480p-low) TED: Hans Rosling shows the best stats you've ever seen", 
		"(480p) TED: Rob Reid: The $8 billion iPod", 
		"(480p-low) TED: Rob Reid: The $8 billion iPod", 
		"(480p) TED: Brene Brown: Listening to shame", 
		"(480p-low) TED: Brene Brown: Listening to shame", 
    	"(480p) TED: Susan Cain: The power of introverts", 
    	"(480p-low) TED: Susan Cain: The power of introverts", 
		"(480p) TED: Vijay Kumar: Robots that fly ... and cooperate", 
		"(480p-low) TED: Vijay Kumar: Robots that fly ... and cooperate", 
    	"(480p) VODO: I Think Were Alone Now", 
    	"(480p-low) VODO: I Think Were Alone Now", 
		"(480p) VODO: L5 Part 1", 
		"(480p-low) VODO: L5 Part 1", 
    	"(480p) VODO: Pioneer One S01E06", 
    	"(480p-low) VODO: Pioneer One S01E06", 
		"(480p) VODO: An Honest Man", 
		"(480p-low) VODO: An Honest Man", 
    };
    // If you change/add here, change VIDEOS[] as well!
    static final String[] HASHES = new String[] {
		"2b2fe5f1462e5b7ac4d70fa081e0169160b2d3a6", // SirKenRobinson_2006-480p.ts
		"114c618ec72e691e5b4730a17f58d215b0418ad4", // SirKenRobinson_2006-480p-512kbps.ts
		"a004e583a05de39f87ceb7a6eb5608c89415e2f0", // JillBolteTaylor_2008-480p.ts
		"21b51297ec27750826c7f55691fbf85e21725d41", // JillBolteTaylor_2008-480p-512kbps.ts
		"23f99be0f5198efceb4da15fd196106b70216e1e", // PranavMistry_2009I-480p.ts
		"769841858131723c947ce718773f8dfde9be1648", // PranavMistry_2009I-480p-512kbps.ts
		"022e1c308d991c653c5e3549fee247cfabc7cf55", // DavidGallo_2007-480p.ts
		"b8393a6c463b16dcb02494425c56fc13d76320ca", // DavidGallo_2007-480p-512kbps.ts
		"5e615dfdf66953f63be284ae0763f80cb70f0892", // PattieMaes_2009-480p.ts
		"f00f05d4a893ed9e665d2bb3d573f212bd17fb2d", // PattieMaes_2009-480p-512kbps.ts
		"e5478e34e01551a2925fc12f4d28a523b2911af5", // SimonSinek_2009X-480p.ts
		"98c3aa6a435c5d3ffa95d31df6e7033a77b2a1a4", // SimonSinek_2009X-480p-512kbps.ts
		"ec677ff98abe4a0b2b5c122065c080f14ad4a272", // ArthurBenjamin_2005-480p.ts
		"79f0e2705b00a01d213037a68e4344637b90e482", // ArthurBenjamin_2005-480p-512kbps.ts
		"71ccb9341537a9a5738650e6842c97fc88306582", // HansRosling_2006.ts
		"af0d9d0eb8ce56963d9bfac0a2a640bb45f45a4d", // HansRosling_2006-480p-512kbps.ts
		"ad2fa2dd346f67583ab327a14d739bdccd44cdb3", // RobReid_2012-480p.ts
		"69312db9bfa36b75676c611ae29b76b9b8010c68", // RobReid_2012-480p-512kbps.ts
		"2dcb65253916e44a791ac7a1a0ee56f51f30086f", // BreneBrown_2012-480p.ts
		"152a89b9def5eb89a57d91e9ca368cda2eb9d3fb", // BreneBrown_2012-480p-512kbps.ts
		"5692014fadcdb33792f0cfa7cc87287bfb8deb91", // SusanCain_2012-480p.ts
		"acc7688b1405047b475ac140f3b84fb80cc1e970", // SusanCain_2012-480p-512kbps.ts
		"dbfcbf3e5ca676d1e4ea8a88375ea95ce2f2184f", // VijayKumar_2012-480p.ts
		"f69bd815b606696139b22c772917b667ae5383d2", // VijayKumar_2012-480p-512kbps.ts
		"db5dabb90a3cbd61a90866a4cc208ae959440ec9", // I.Think.Were.Alone.Now.2008.720p.x264-VODO.ts
		"e99a2a3299087f9eeaae489afa663f8c30f783e0", // I.Think.Were.Alone.Now.2008.720p.x264-VODO-512kbps.ts
		"071d43828a3291defa073008b601aacfb09fd281", // L5.Part.1.2012.Xvid-VODO.ts
		"49713d7fd02c5426b94656d63358eca64a0dca88", // L5.Part.1.2012.Xvid-VODO-512kbps.ts
		"cbc48a70222e37230bf3f2b3bd84eaef5ae16b41", // Pioneer.One.S01E06.Xvid-VODO.ts
		"086a2b6cab9d58ab417f4c8976f78644399372ba", // Pioneer.One.S01E06.Xvid-VODO-512kbps.ts
		"3ce3f4a5bb785d5e8eb7bf3f2615e37095eb5170", // An.Honest.Man.Xvid-VODO.ts
		"15f2fb2f7880c8c806d67ed1c070d925aaaa7f7b", // An-Honest-Man-Xvid-VODO-480p-512kbps.ts	
	};

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Done, exit application
        //finish(); //anand - testing the statistics activity
	}
}

