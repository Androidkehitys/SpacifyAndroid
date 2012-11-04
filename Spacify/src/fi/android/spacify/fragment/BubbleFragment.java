package fi.android.spacify.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout.LayoutParams;
import fi.android.spacify.R;
import fi.android.spacify.activity.BubbleActivity;
import fi.android.spacify.animation.ReverseInterpolator;
import fi.android.spacify.service.ContentManagementService;
import fi.android.spacify.view.BubbleView;
import fi.android.spacify.view.ConnectionLayout;
import fi.spacify.android.util.Events;
import fi.spacify.android.util.StaticUtils;

public class BubbleFragment extends BaseFragment implements OnTouchListener {

	private final String TAG = "BubbleFragment";

	private final int UPDATE_DELAY = 300;

	private final ContentManagementService cms = ContentManagementService.getInstance();
	private Map<Integer, BubbleView> list = new HashMap<Integer, BubbleView>();
	private ConnectionLayout frame;
	private int height, width;
	private BubbleView singleTouched;
	private Handler handler;
	private boolean animationInProgress = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		updateConnections();
		handler = new Handler();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		frame = (ConnectionLayout) inflater.inflate(R.layout.bubble_frame, container, false);
		frame.setOnTouchListener(this);

		DisplayMetrics metrics = new DisplayMetrics();
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		display.getMetrics(metrics);

		height = metrics.heightPixels;
		width = metrics.widthPixels;

		updateBubbles();
		updateConnections();

		handler.postDelayed(updateConnections, UPDATE_DELAY);

		return frame;
	}

	private Runnable updateConnections = new Runnable() {

		@Override
		public void run() {
			updateConnections();

			handler.postDelayed(updateConnections, UPDATE_DELAY);
		}
	};

	private void updateBubbles() {
		Random r = new Random();
		for(BubbleView b : list.values()) {
			b.setOnTouchListener(this);
			if(b.x <= 0 && b.y <= 0) {
				int radius = b.getRadius();
				int xn = width / radius;
				int yn = height / radius;
				int x = radius * r.nextInt(xn);
				int y = radius * r.nextInt(yn - 1);

				b.move(x, y);
			}
			
			frame.addView(b);
		}

		frame.invalidate();
	}

	@Override
	public boolean handleMessage(Message msg) {

		switch (Events.values()[msg.what]) {
			case ALL_BUBBLES_FETCHED:
				return true;
			default:
				break;
		}

		return super.handleMessage(msg);
	}

	private BubbleView firstTouched = null;
	private double initialD = -1;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		updateConnections();
		float x = event.getRawX();
		float y = event.getRawY();

		BubbleView bv = null;
		if(v instanceof BubbleView) {
			bv = (BubbleView) v;
			if(event.getPointerCount() == 1) {
				firstTouched = bv;
			}
		}

		LayoutParams params = ((LayoutParams) v.getLayoutParams());
		float dx = 0;
		float dy = 0;
		int action = (event.getAction() & MotionEvent.ACTION_MASK);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				if(bv != null) {
					bv.onTouchDown();
					dx = params.leftMargin - x + (params.width / 2);
					dy = params.topMargin - y + (params.height / 2);
					bv.offsetX = (int) dx;
					bv.offsetY = (int) dy;

					if(bv.getID() != firstTouched.getID()) {
						firstTouched = null;
					}
				} else {
					onEmptyClick();
				}

				break;
			case MotionEvent.ACTION_MOVE:
				if(bv != null) {
					bv.move((int) x, (int) y);
					testHit(bv);
				}

				if(firstTouched != null && event.getPointerCount() == 2) {
					float x1 = event.getX(event.getPointerId(0));
					float y1 = event.getY(event.getPointerId(0));
					float x2 = event.getX(event.getPointerId(1));
					float y2 = event.getY(event.getPointerId(1));

					double d = distance(x1, y1, x2, y2);
					if(initialD < 0) {
						initialD = d;
					} else {
						firstTouched.zoom(d / initialD);
					}
				}

				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				if(firstTouched != null) {
					firstTouched.endZoom();
					if(event.getPointerCount() == 1) {
						if(firstTouched.moved <= BubbleView.MOVEMENT_TOUCH_TRESHOLD) {
							onSingleTouch(bv);
						}
						firstTouched = null;
					}
				}
				initialD = -1;

				if(bv != null) {
					bv.onTouchUp();
					((BubbleActivity) getActivity()).dropDown(bv);
				}

				break;
		}

		return true;
	}

	public void onSingleTouch(BubbleView bv) {
		if(!animationInProgress) {
			addSubBubbles(bv);
		}
	}

	private void addSubBubbles(BubbleView bv) {
		Random r = new Random();
		if(hasChildsVisible(bv)) {
			removeBubbles(bv.getLinks(), bv);
			return;
		}

		for(final BubbleView nBubble : cms.getBubbles(bv.getLinks())) {
			if(!list.containsKey(nBubble.getID())) {
				Log.d(TAG, "New Bubble [" + nBubble.getTitle() + "]");
				animateBubbleAdd(nBubble, bv);
			}
		}
		updateConnections();
	}

	private void removeBubbles(List<Integer> links, BubbleView from) {
		for(int id : links) {
			if(from != null) {
				animateBubbleRemove(list.get(id), from);
			} else {
				removeBubble(list.get(id));
			}
		}
	}

	public void removeBubble(BubbleView bubbleView) {
		if(bubbleView != null) {
			frame.removeView(bubbleView);
			list.remove(bubbleView.getID());

			cms.saveBubble(bubbleView);

			updateConnections();
		}
	}

	private void animateBubbleRemove(final BubbleView bv, BubbleView to) {
		list.remove(bv.getID());
		updateConnections();
		Animation anim = new ScaleAnimation(0, 1, 0, 1, Animation.ABSOLUTE,
				(to.x - bv.x + (bv.diameter / 2)), Animation.ABSOLUTE,
				(to.y - bv.y + (bv.diameter / 2)));
		anim.setDuration(StaticUtils.ANIMATION_DURATION);
		anim.setInterpolator(new ReverseInterpolator());

		anim.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				removeBubble(bv);
				animationInProgress = false;
			}
		});

		bv.startAnimation(anim);
		animationInProgress = true;
	}

	private void animateBubbleAdd(final BubbleView bv, BubbleView from) {
		if(!list.containsKey(bv.getID())) {
			frame.addView(bv);

			int fromX = (from.x);
			int fromY = (from.y);
			int toX = (bv.x);
			int toY = (bv.y);

			Animation anim = new ScaleAnimation(0, 1, 0, 1, 
					Animation.ABSOLUTE,	(fromX - toX + (bv.diameter / 2)), 
					Animation.ABSOLUTE, (fromY - toY + (bv.diameter / 2)));
			anim.setDuration(StaticUtils.ANIMATION_DURATION);
			anim.setInterpolator(new LinearInterpolator());

			anim.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					list.put(bv.getID(), bv);
					bv.setOnTouchListener(BubbleFragment.this);
					updateConnections();
					animationInProgress = false;
				}
			});

			bv.startAnimation(anim);
			animationInProgress = true;
		} else {
			Log.w(TAG, "Could not add bubble [" + bv.getTitle() + "], already in view");
		}
	}

	protected void addBubble(BubbleView bv) {
		frame.addView(bv);
		bv.setOnTouchListener(BubbleFragment.this);
		list.put(bv.getID(), bv);
		updateConnections();
	}

	private void testHit(final BubbleView bv) {
		for(BubbleView b : list.values()) {
			if(isHit(bv, b) && b.getID() != bv.getID()) {
				autoMove(bv, b);
			}
		}
	}

	public static boolean isHit(View b1, View b2) {
		double radius1 = (b1.getWidth() / 2);
		double radius2 = (b2.getWidth() / 2);
		double a = (radius1 + radius2);
		double dx = (b1.getLeft() + radius1) - (b2.getLeft() + radius2);
		double dy = (b1.getTop() + radius1) - (b2.getTop() + radius2);
		return a * a > ((dx * dx) + (dy * dy));
	}

	private void autoMove(final BubbleView bv, BubbleView pushed) {
		int[] movingPos = bv.getViewPosition();
		int[] pushedPos = pushed.getViewPosition();
		double distance = distance(movingPos[0], movingPos[1], pushedPos[0], pushedPos[1]);

		int movingRadius = bv.getRadius();
		int pushedRadius = pushed.getRadius();

		double dx = movingPos[0] - pushedPos[0];
		double dy = movingPos[1] - pushedPos[1];
		float radius = pushedRadius > movingRadius ? pushedRadius : movingRadius;
		double move = Math.abs(distance - radius);

		double factor = 1;
		if(dx > 0 && dy > 0) {
			factor = dx < dy ? Math.abs(dx / dy) : 1 - Math.abs(dy / dx);
		} else if(dx > 0 && dy < 0) {
			factor = dx < Math.abs(dy) ? Math.abs(dx / dy) : 1 - Math.abs(dy / dx);
		} else if(dx < 0 && dy > 0) {
			factor = Math.abs(dx) < dy ? Math.abs(dx / dy) : 1 - Math.abs(dy / dx);
		} else {
			factor = dx > dy ? Math.abs(dx / dy) : 1 - Math.abs(dy / dx);
		}

		int x = 0;
		if(dx > 0) {
			x = (int) (pushedPos[0] - (move * factor));
		} else {
			x = (int) (pushedPos[0] + (move * factor));
		}

		int y = 0;
		double rFactor = 1 - factor;
		if(dy > 0) {
			y = (int) (pushedPos[1] - (move * rFactor));
		} else {
			y = (int) (pushedPos[1] + (move * rFactor));
		}

		// Don't let it go over
		if(x + pushedRadius > BubbleActivity.width) {
			x = (BubbleActivity.width - pushedRadius);
		} else if(x - pushedRadius < 0) {
			x = pushedRadius;
		}

		if(y + pushedRadius > BubbleActivity.height) {
			y = (BubbleActivity.height - pushedRadius);
		} else if(y - pushedRadius < 0) {
			y = pushedRadius;
		}

		pushed.move(x, y);
	}

	private double distance(LayoutParams b1, LayoutParams b2) {
		return distance(b1.leftMargin, b1.topMargin, b2.leftMargin, b2.topMargin);
	}

	public static double distance(float x, float y, float bx, float by) {
		double dx2 = Math.pow(x - bx, 2);
		double dy2 = Math.pow(y - by, 2);

		return Math.sqrt(dx2 + dy2);
	}

	public void setBubbleCursor(Cursor c, Context context) {
		c.moveToFirst();
		while(!c.isAfterLast()) {
			BubbleView bv = new BubbleView(context, c);
			list.put(bv.getID(), bv);
			c.moveToNext();
		}
		c.close();
		updateConnections();
	}
	
	public boolean hasChildsVisible(BubbleView bv) {
		for(int id : bv.getLinks()) {
			if(id != bv.getID()) {
				BubbleView child = list.get(id);
				if(child == null) {
					return false;
				}
			}
		}
		return true;
	}

	private void updateConnections() {
		int[][] connections = new int[list.size() * list.size()][4];
		int i = 0;
		Iterator<BubbleView> iterator = list.values().iterator();
		while(iterator.hasNext()) {
			BubbleView b1 = iterator.next();
			for(int id : b1.getLinks()) {
				BubbleView b2 = list.get(id);
				if(b2 != null && b1 != null && connections.length > i) {
					int[] b1Position = b1.getViewPosition();
					int[] b2Position = b2.getViewPosition();

					connections[i][0] = b1Position[0];
					connections[i][1] = b1Position[1];
					connections[i][2] = b2Position[0];
					connections[i][3] = b2Position[1];
					i++;
				}
			}
		}
		if(frame != null) {
			frame.setConnections(connections);
		}
	}

	private void onEmptyClick() {
		((BubbleActivity) getActivity()).onEmptyClick();
	}

	public void saveBubbles() {
		cms.saveBubbles(new ArrayList<BubbleView>(list.values()));
	}

}
