package fi.android.spacify.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.ArrayAdapter;
import fi.android.spacify.db.BubbleDatabase.BubbleColumns;
import fi.android.spacify.fragment.BubbleFragment;
import fi.android.spacify.fragment.ControlAdapter;

@SuppressWarnings("javadoc")
public class BubbleView extends BaseBubbleView {

	private final String TAG = "Bubble";

	public static class BubbleJSON {
		public static final String debugID = "debugId";
		public static final String id = "id";
		public static final String type = "type";
		public static final String priority = "priority";
		public static final String size = "size";
		public static final String style = "style";
		public static final String title = "title";
		public static final String contents = "contents";
		public static final String links = "links";
		public static final String titleImageUrl = "titleImageUrl";
		public static final String contentsImageUrl = "contentsImageUrl";
		public static final String context = "context";
	}

	public static class BubbleContexts {
		public static final String CMS = "cms";
		public static final String ME = "me";
		public static final String EVENTS = "events";
		public static final String PEOPLE = "people";
		public static final String GROUP = "group";
		public static final String INFORMATION = "information";
		public static final String PLACES = "places";
	}

	public static class BubbleMovement {
		public static final int INERT = 0;
		public static final int MOVING = 1;
		public static final int AUTOMATIC = 2;
	}

	public boolean linkStatusChanged = false;
	public boolean lockedToPlase = false;

	private int priority, id;
	private String debugID = "", type = "", style = "", title = "", contents = "", titleImageUrl = "",
			contentImageUrl = "";
	private List<Integer> links = new ArrayList<Integer>();
	private long latitude = 0, longitude = 0;
	private Set<String> contexts = new HashSet<String>();

	private void init() {
		zoom(1);
	}

	public BubbleView(Context context, int id) {
		super(context);
		this.id = id;
		init();
	}


	public BubbleView(Context context, Cursor c) {
		super(context);
		super.setLayoutParams(new LayoutParams(100, 100));
		
		updateContent(c);
		init();
	}

	public void updateContent(Cursor c) {
		id = c.getInt(c.getColumnIndex(BubbleColumns.ID));
		title = c.getString(c.getColumnIndex(BubbleColumns.TITLE));
		setText(title);
		style = c.getString(c.getColumnIndex(BubbleColumns.STYLE));
		contents = c.getString(c.getColumnIndex(BubbleColumns.CONTENTS));
		priority = c.getInt(c.getColumnIndex(BubbleColumns.PRIORITY));
		titleImageUrl = c.getString(c.getColumnIndex(BubbleColumns.TITLE_IMAGE_URL));
		try {
			String linksJSON = c.getString(c.getColumnIndex(BubbleColumns.LINKS));
			if(linksJSON != null) {
				parseJsonLinks(new JSONArray(linksJSON));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			JSONArray array = new JSONArray(c.getString(c.getColumnIndex(BubbleColumns.CONTEXT)));
			setContexts(array);
		} catch(JSONException e) {
			e.printStackTrace();
		}

		type = c.getString(c.getColumnIndex(BubbleColumns.TYPE));
		debugID = c.getString(c.getColumnIndex(BubbleColumns.DEBUG_ID));
		contentImageUrl = c.getString(c.getColumnIndex(BubbleColumns.CONTENT_IMAGE_URL));
		latitude = c.getLong(c.getColumnIndex(BubbleColumns.LATITUDE));
		longitude = c.getLong(c.getColumnIndex(BubbleColumns.LONGITUDE));

		x = c.getInt(c.getColumnIndex(BubbleColumns.X));
		y = c.getInt(c.getColumnIndex(BubbleColumns.Y));

		if(x != -1 && y != -1) {
			move(x, y);
			moved = 0;
		}

		postInvalidate();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BubbleView) {
			BubbleView b = (BubbleView) o;
			if (b.id == id) {
				return true;
			} else {
				return false;
			}
		}
		return super.equals(o);
	}

	/**
	 * Get id of this Bubble.
	 * 
	 * @return ID as integer
	 */
	public int getID() {
		return id;
	}

	/**
	 * Get content of the bubble.
	 * 
	 * @return String content of this Bubble.
	 */
	public String getContents() {
		return contents;
	}

	public void setContents(String content) {
		this.contents = content;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getDebugID() {
		return debugID;
	}

	public void setDebugID(String debugID) {
		this.debugID = debugID;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getTitleImageUrl() {
		return titleImageUrl;
	}

	public void setTitleImageUrl(String titleImageUrl) {
		this.titleImageUrl = titleImageUrl;
	}

	public String getContentImageUrl() {
		return contentImageUrl;
	}

	public void setContentImageUrl(String contentImageUrl) {
		this.contentImageUrl = contentImageUrl;
	}

	public List<Integer> getLinks() {
		return links;
	}

	public void setLinks(List<Integer> links) {
		this.links = links;
	}

	public void addLink(int link) {
		if (!links.contains(link)) {
			this.links.add(link);
		}
	}

	public void removeLink(Integer link) {
		this.links.remove(link);
	}

	public JSONArray getLinksJSONArray() {
		JSONArray jArray = new JSONArray();
		for (int link : links) {
			jArray.put(link);
		}
		return jArray;
	}

	public void parseJsonLinks(JSONArray jArray) {
		for (int i = 0; i < jArray.length(); i++) {
			try {
				links.add(jArray.getInt(i));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public long getLattitude() {
		return latitude;
	}

	public void setLattitude(long lattitude) {
		this.latitude = lattitude;
	}

	public long getLongitude() {
		return longitude;
	}

	public void setLongitude(long longitude) {
		this.longitude = longitude;
	}

	public void setContext(String context) {
		contexts.add(context);
	}

	public void removeContext(String context) {
		contexts.remove(context);
	}

	public String getContextJSON() {
		JSONArray json = new JSONArray();
		for(String s : contexts) {
			json.put(s);
		}

		return json.toString();
	}

	public void translateContexts(String contextJson) {
		try {
			JSONArray json = new JSONArray(contextJson);
			for(int i = 0; i < json.length(); i++) {
				contexts.add(json.getString(i));
			}

		} catch(JSONException e) {
			Log.w(TAG, "Could not translate Context information from: " + contextJson, e);
		}
	}

	public void setContexts(JSONArray array) {
		for(int i = 0; i < array.length(); i++) {
			try {
				contexts.add(array.getString(i));
			} catch(JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public ArrayAdapter<Integer> getControlAdapter(BubbleFragment bf) {
		ArrayAdapter<Integer> adapter = new ControlAdapter(getContext(), this, bf);
		adapter.add(ControlAdapter.COMMANDS.TOGGLE_LINKS);

		return adapter;
	}

}
