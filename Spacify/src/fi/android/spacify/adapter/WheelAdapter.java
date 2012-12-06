package fi.android.spacify.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import fi.android.spacify.R;
import fi.android.spacify.activity.BubbleActivity;
import fi.android.spacify.view.BubbleView;

public abstract class WheelAdapter extends ArrayAdapter<BubbleView> {

	private int size;
	protected BubbleActivity bubbleAct;

	public WheelAdapter(BubbleActivity context) {
		super(context, 0);
		bubbleAct = context;

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null) {
			convertView = getEmptyView(parent);
		}
		ViewHolder holder = (ViewHolder) convertView.getTag();
		holder.position = position;
		BubbleView bv = getItem(position);
		if(bv != null) {
			holder.text.setText(bv.getTitle());
		}

		return convertView;
	}

	public View getEmptyView(ViewGroup parent) {
		View v = LayoutInflater.from(getContext()).inflate(R.layout.wheel_bubble, parent, false);
		v.setTag(new ViewHolder(v));
		return v;
	}

	public class ViewHolder {
		public int position = -1;
		public TextView text;
		public ImageView background;
		public View touchArea;

		public ViewHolder(View v) {
			text = (TextView) v.findViewById(R.id.wheel_text);
			background = (ImageView) v.findViewById(R.id.wheel_background);
			touchArea = v.findViewById(R.id.wheel_touch_area);
			setViewSize(text);
			setViewSize(background);
		}
	}

	private void setViewSize(View v) {
		LayoutParams params = (LayoutParams) v.getLayoutParams();
		params.width = size;
		params.height = size;
		v.setLayoutParams(params);
	}

	public void setBubbleSize(int size) {
		this.size = size;
	}

	public int getBubbleSize() {
		return size;
	}

	public abstract void onSingleClick(View from, BubbleView bv);

}