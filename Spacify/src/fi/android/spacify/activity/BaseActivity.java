package fi.android.spacify.activity;

import android.os.Bundle;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import fi.android.service.EventService;

/**
 * Base activity for all activities in Spacify application. Extends
 * FragmentActivity from Android support package v4.
 * 
 * @author Tommy
 * 
 */
public class BaseActivity extends FragmentActivity implements Callback {

	private final String TAG = "BaseActivity";

	protected boolean onTop = false;
	protected boolean started = false;

	protected EventService es;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		started = true;
		es = EventService.getInstance();
		es.addCallback(this);
	}

	@Override
	protected void onResume() {
		onTop = true;
		super.onResume();
	}

	@Override
	protected void onPause() {
		onTop = false;
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		started = false;
		super.onDestroy();
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.v(TAG, "Got message:" + msg.what);
		return false;
	}

}