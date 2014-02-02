package com.gearfrog.network;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class NetworkActivity extends Activity {
	   private static final int MENU_EASY = 1;

	    private static final int MENU_HARD = 2;

	    private static final int MENU_MEDIUM = 3;

	    private static final int MENU_PAUSE = 4;

	    private static final int MENU_RESUME = 5;

	    private static final int MENU_START = 6;

	    private static final int MENU_STOP = 7;

	    /** A handle to the View in which the game is running. */
	    private NetworkView mNetworkView;

	    /**
	     * Invoked during init to give the Activity a chance to set up its Menu.
	     * 
	     * @param menu the Menu to which entries may be added
	     * @return true
	     */
	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        super.onCreateOptionsMenu(menu);

	        menu.add(0, MENU_START, 0, R.string.menu_start);
	        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
	        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
	        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);

	        return true;
	    }

	    /**
	     * Invoked when the user selects an item from the Menu.
	     * 
	     * @param item the Menu entry which was selected
	     * @return true if the Menu item was legit (and we consumed it), false
	     *         otherwise
	     */
	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	            case MENU_START:
	            	mNetworkView.doStart();
	                return true;
	            case MENU_STOP:
	            	mNetworkView.setState(NetworkView.STATE_LOSE,
	                        getText(R.string.message_stopped));
	                return true;
	            case MENU_PAUSE:
	            	mNetworkView.pause();
	                return true;
	            case MENU_RESUME:
	            	mNetworkView.unpause();
	                return true;

	        }

	        return false;
	    }

	    /**
	     * Invoked when the Activity is created.
	     * 
	     * @param savedInstanceState a Bundle containing state saved from a previous
	     *        execution, or null if this is a new execution
	     */
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        // turn off the window's title bar
	        requestWindowFeature(Window.FEATURE_NO_TITLE);

	        // tell system to use the layout defined in our XML file
	        setContentView(R.layout.network_layout);

	        // get handles to the NetworkView from XML, and its NetworkThread
	        mNetworkView = (NetworkView) findViewById(R.id.network);

	        // give the NetworkView a handle to the TextView used for messages
	        mNetworkView.setTextView((TextView) findViewById(R.id.text));
	        mNetworkView.setMoneyText((TextView) findViewById(R.id.money));
	        mNetworkView.setScore1Text((TextView) findViewById(R.id.score1));
	        mNetworkView.setScore2Text((TextView) findViewById(R.id.score2));

	        MediaPlayer mp = MediaPlayer.create(getBaseContext(), R.raw.popcorn);
	        mp.setLooping(true);
	        mp.start();
	        mNetworkView.setMusic(mp);
	        
	        // Hook up button presses to the appropriate event handler.
	        ToggleImageButton links = (ToggleImageButton)findViewById(R.id.links);
	        links.setOnClickListener(mNetworkView.mLinkListener);
	        mNetworkView.setLinkButton(links);
	        LevelSelectButton level = (LevelSelectButton)findViewById(R.id.linkType);
	        mNetworkView.setLevelButton(level);
	        //((Button) findViewById(R.id.nodes)).setOnClickListener(mNodeListener);
	        


	        
            // we were just launched: set up a new game
        	mNetworkView.setState(NetworkView.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
	    }

	    /**
	     * Invoked when the Activity loses user focus.
	     */
	    @Override
	    protected void onPause() {
	        super.onPause();
	        mNetworkView.pause(); // pause game when Activity pauses
	    }

	    /**
	     * A call-back for when the user presses the node button.
	     */
	    OnClickListener mNodeListener = new OnClickListener() {
	        public void onClick(View v) {
	        	mNetworkView.setState(NetworkView.STATE_BUY_NODES);
	        }
	    };
}