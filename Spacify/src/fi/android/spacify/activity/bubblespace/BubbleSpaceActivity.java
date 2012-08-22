package fi.android.spacify.activity.bubblespace;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MotionEvent;
import fi.android.spacify.R;
import fi.android.spacify.activity.BaseActivity;
import fi.android.spacify.gesture.GestureInterface;
import fi.android.spacify.model.Bubble;
import fi.android.spacify.view.BubbleSurface;
import fi.android.spacify.view.BubbleSurface.BubbleEvents;

/**
 * Activity to show bubbles.
 * 
 * @author Tommy
 *
 */
public class BubbleSpaceActivity extends BaseActivity {
	
	private Vibrator vibrator;
	private final int VIBRATION_TIME = 200;

	private BubbleSurface bSurface;
	private PopupControlFragment controlPopup;
	private final List<Fragment> visibleFragments = new ArrayList<Fragment>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		setContentView(R.layout.bubble_space);
		
		bSurface = (BubbleSurface) findViewById(R.id.bubblespace_surface);
		bSurface.setGesture(BubbleEvents.LONG_CLICK, longClick);
		
		controlPopup = new PopupControlFragment();
	}

	private final GestureInterface<Bubble> longClick = new GestureInterface<Bubble>() {
		
		@Override
		public void onGestureDetected(Bubble b, MotionEvent ev) {
			if(b != null) {
				vibrator.vibrate(VIBRATION_TIME);

				FragmentManager fm = getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.replace(R.id.bubblespace_control_popup, controlPopup);
				ft.commit();
				
				visibleFragments.add(controlPopup);
			}
		}
	};
	
	@Override
	public void onBackPressed() {
		if(visibleFragments.size() > 0) {
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			for(Fragment f : visibleFragments) {
				ft.remove(f);
			}
			visibleFragments.clear();
			ft.commit();
		} else {
			super.onBackPressed();
		}
	};
	
}
