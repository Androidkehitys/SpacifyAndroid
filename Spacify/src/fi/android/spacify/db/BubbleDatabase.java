package fi.android.spacify.db;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import fi.android.spacify.model.Bubble;
import fi.android.spacify.view.BubbleView;
import fi.android.spacify.view.BubbleView.BubbleContexts;
import fi.android.spacify.view.BubbleView.BubbleJSON;
import fi.spacify.android.util.StaticUtils;

public class BubbleDatabase extends SQLiteOpenHelper {

	private final String TAG = "SmartSpaceDatabase";
	
	private static BubbleDatabase instance;
	private Context ctx;
	
	private static final int VERSION = 1;
	private static final String DB_NAME = "smartspace.db";
	private static final String BUBBLE_TABLE = "bubble_tbl";

	
	@SuppressWarnings("javadoc")
	public static class BubbleColumns {
		public static final String ID = "_id";
		public static final String TITLE = "title";
		public static final String STYLE = "style";
		public static final String CONTENTS = "contents";
		public static final String PRIORITY = "priority";
		public static final String TITLE_IMAGE_URL = "title_image_url";
		public static final String LINKS = "links";
		public static final String TYPE = "type";
		public static final String DEBUG_ID = "debug_id";
		public static final String CONTENT_IMAGE_URL = "contents_image_url";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String X = "position_x";
		public static final String Y = "position_y";

		// Custom fields
		public static final String CONTEXT = "context";
	}
	
	private BubbleDatabase(Context context) {
		super(context, DB_NAME, null, VERSION);
		this.ctx = context;
	}
	
	public static void init(Context context) {
		if(instance != null) {
			throw new IllegalStateException("SmartSpaceDatabase is already initialized");
		} else {
			instance = new BubbleDatabase(context);
		}
	}
	
	public static BubbleDatabase getInstance() {
		if(instance == null) {
			throw new IllegalStateException("SmartSpaceDatabase is not initialized");
		} else {
			return instance;
		}
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE ").append(BUBBLE_TABLE).append(" (");
		sql.append(BubbleColumns.ID).append(" INTEGER PRIMARY KEY,");
		sql.append(BubbleColumns.TITLE).append(" TEXT,");
		sql.append(BubbleColumns.STYLE).append(" TEXT,");
		sql.append(BubbleColumns.CONTENTS).append(" TEXT,");
		sql.append(BubbleColumns.PRIORITY).append(" INTEGER,");
		sql.append(BubbleColumns.TITLE_IMAGE_URL).append(" TEXT,");
		sql.append(BubbleColumns.LINKS).append(" TEXT,");
		sql.append(BubbleColumns.TYPE).append(" TEXT,");
		sql.append(BubbleColumns.DEBUG_ID).append(" TEXT,");
		sql.append(BubbleColumns.CONTENT_IMAGE_URL).append(" TEXT,");
		sql.append(BubbleColumns.LATITUDE).append(" INTEGER,");
		sql.append(BubbleColumns.LONGITUDE).append(" INTEGER,");
		sql.append(BubbleColumns.X).append(" INTEGER,");
		sql.append(BubbleColumns.Y).append(" INTEGER,");
		sql.append(BubbleColumns.CONTEXT).append(" TEXT");
		sql.append(")");
		
		db.execSQL(sql.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}
	
	public void storeBubble(Bubble bubble) {
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(BubbleColumns.ID, bubble.getID());
		values.put(BubbleColumns.TITLE, bubble.getTitle());
		values.put(BubbleColumns.STYLE, bubble.getStyle());
		values.put(BubbleColumns.CONTENTS, bubble.getContents());
		values.put(BubbleColumns.PRIORITY, bubble.getPriority());
		values.put(BubbleColumns.TITLE_IMAGE_URL, bubble.getTitleImageUrl());
		values.put(BubbleColumns.LINKS, bubble.getLinksJSONArray().toString());
		values.put(BubbleColumns.TYPE, bubble.getType());
		values.put(BubbleColumns.DEBUG_ID, bubble.getDebugID());
		values.put(BubbleColumns.CONTENT_IMAGE_URL, bubble.getContentImageUrl());
		values.put(BubbleColumns.LATITUDE, bubble.getLattitude());
		values.put(BubbleColumns.LONGITUDE, bubble.getLongitude());
		values.put(BubbleColumns.X, bubble.x);
		values.put(BubbleColumns.Y, bubble.y);

		long change = -1;

		try {
			change = db.insertOrThrow(BUBBLE_TABLE, null, values);
		} catch(SQLException e) {
			String where = BubbleColumns.ID + " = " + bubble.getID();
			change = db.update(BUBBLE_TABLE, values, where, null);
		}

		if(change != -1) {
			Log.v(TAG, "Bubble [" + bubble.getTitle() + "] stored to database.");
		}
	}

	public void storeBubbleViews(List<BubbleView> bubbles) {
		SQLiteDatabase db = getWritableDatabase();

		db.beginTransaction();

		for(BubbleView b : bubbles) {
			ContentValues values = new ContentValues();
			values.put(BubbleColumns.ID, b.getID());
			values.put(BubbleColumns.TITLE, b.getTitle());
			values.put(BubbleColumns.STYLE, b.getStyle());
			values.put(BubbleColumns.CONTENTS, b.getContents());
			values.put(BubbleColumns.PRIORITY, b.getPriority());
			values.put(BubbleColumns.TITLE_IMAGE_URL, b.getTitleImageUrl());
			values.put(BubbleColumns.LINKS, b.getLinksJSONArray().toString());
			values.put(BubbleColumns.TYPE, b.getType());
			values.put(BubbleColumns.DEBUG_ID, b.getDebugID());
			values.put(BubbleColumns.CONTENT_IMAGE_URL, b.getContentImageUrl());
			values.put(BubbleColumns.LATITUDE, b.getLattitude());
			values.put(BubbleColumns.LONGITUDE, b.getLongitude());
			int[] position = b.getViewPosition();
			values.put(BubbleColumns.X, position[0]);
			values.put(BubbleColumns.Y, position[1]);
			values.put(BubbleColumns.CONTEXT, b.getContextJSON());

			long change = -1;

			try {
				change = db.insertOrThrow(BUBBLE_TABLE, null, values);
			} catch(SQLException e) {
				String where = BubbleColumns.ID + " = " + b.getID();
				change = db.update(BUBBLE_TABLE, values, where, null);
			}
		}

		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public Cursor getTopLevelBubblesCursor() {
		SQLiteDatabase db = getReadableDatabase();
		String selection = "";
		selection = BubbleColumns.PRIORITY + " > 0";
		
		return db.query(BUBBLE_TABLE, null, selection, null, null, null,
				BubbleColumns.TITLE);
	}

	public Cursor getBubblesWithPriority(int priority) {
		SQLiteDatabase db = getReadableDatabase();
		String selection = "";
		selection = BubbleColumns.PRIORITY + " > " + priority;

		return db.query(BUBBLE_TABLE, null, selection, null, null, null, BubbleColumns.TITLE);
	}

	public Cursor getBubblesInContext(String context) {
		SQLiteDatabase db = getReadableDatabase();
		String sql = "SELECT * FROM " + BUBBLE_TABLE + " WHERE " + BubbleColumns.CONTEXT
				+ " LIKE '%" + context + "%'";
		Cursor c = db.rawQuery(sql, null);
		return c;
	}

	public Cursor getLinkedBubblesCursor(List<Integer> links) {
		SQLiteDatabase db = getReadableDatabase();
		
		String separator = "";
		String linkString = "";
		for(Integer i : links) {
			linkString += separator + i;
			separator = ", ";
		}
		
		String selection = BubbleColumns.ID + " IN (" + linkString + ")";
		
		return db.query(BUBBLE_TABLE, null, selection, null, null, null,
				BubbleColumns.TITLE);
	}

	public void storeBubbleJson(JSONObject json) {
		SQLiteDatabase db = getWritableDatabase();

		db.beginTransaction();

		try {
			JSONArray jArray = json.getJSONArray("add");

			for(int i = 0; i < jArray.length(); i++) {
				JSONObject b = jArray.getJSONObject(i);

				ContentValues values = new ContentValues();
				int id = StaticUtils.parseIntJSON(b, BubbleJSON.id, -1);
				values.put(BubbleColumns.ID, id);
				values.put(BubbleColumns.TITLE, StaticUtils.parseStringJSON(b, BubbleJSON.title, ""));
				values.put(BubbleColumns.STYLE, StaticUtils.parseStringJSON(b, BubbleJSON.style, ""));
				values.put(BubbleColumns.CONTENTS, StaticUtils.parseStringJSON(b, BubbleJSON.contents, ""));
				if(b.has(BubbleJSON.priority)) {
					values.put(BubbleColumns.PRIORITY, StaticUtils.parseIntJSON(b, BubbleJSON.priority, -1));
				} else if(b.has(BubbleJSON.size)) {
					values.put(BubbleColumns.PRIORITY,StaticUtils.parseIntJSON(b, BubbleJSON.size, -1));
				}
				values.put(BubbleColumns.TITLE_IMAGE_URL, StaticUtils.parseStringJSON(b, BubbleJSON.titleImageUrl, ""));
				if(b.has(BubbleJSON.links)) {
					values.put(BubbleColumns.LINKS, b.getJSONArray(BubbleJSON.links).toString());
				}
				if(b.has(BubbleJSON.context)) {
					values.put(BubbleColumns.CONTEXT, b.getJSONArray(BubbleJSON.context).toString());
				} else {
					values.put(BubbleColumns.CONTEXT, "[" + BubbleContexts.CMS + "]");
				}
				values.put(BubbleColumns.TYPE, StaticUtils.parseStringJSON(b, BubbleJSON.type, ""));
				values.put(BubbleColumns.DEBUG_ID, StaticUtils.parseStringJSON(b, BubbleJSON.debugID, ""));
				values.put(BubbleColumns.CONTENT_IMAGE_URL, StaticUtils.parseStringJSON(b, BubbleJSON.contentsImageUrl, ""));
				values.put(BubbleColumns.LATITUDE, 0);
				values.put(BubbleColumns.LONGITUDE, 0);
				values.put(BubbleColumns.X, -1);
				values.put(BubbleColumns.Y, -1);

				try {
					db.insertOrThrow(BUBBLE_TABLE, null, values);
				} catch(SQLException e) {
					String where = BubbleColumns.ID + " = " + id;
					db.update(BUBBLE_TABLE, values, where, null);
				}
			}
		} catch(JSONException e) {
			e.printStackTrace();
		}

		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public Cursor getBubbleSearch(CharSequence constraint) {
		SQLiteDatabase db = getReadableDatabase();
		String sql = "SELECT * FROM " + BUBBLE_TABLE + " WHERE " + 
		BubbleColumns.TITLE + " LIKE '%"+ constraint + "%' OR " + 
		BubbleColumns.CONTENTS + " LIKE '%" + constraint+ "%' OR " + 
		BubbleColumns.CONTEXT + " LIKE '%" + constraint + "%'";
		Cursor c = db.rawQuery(sql, null);
		return c;
	}

}
