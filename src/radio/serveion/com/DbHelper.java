package radio.serveion.com;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {

//	private Context context;
	private static final String DB_NAME = "mbradio_fm.db";
	private static final int DB_VERSION = 1;
	private static final String TABLE_NAME = "radios";
	private static final String CREATE_QUERY = "CREATE TABLE " + TABLE_NAME
			+ " (id INTEGER PRIMARY KEY AUTOINCREMENT," + "name TEXT NOT NULL,"
			+ "mail TEXT NOT NULL," + "about TEXT NOT NULL,"
			+ "logo TEXT NOT NULL," + "wurl TEXT," + "phone TEXT,"
			+ "rating VARCHAR(4)," + "rfbook TEXT," + "genre TEXT,"
			+ "lang TEXT" + ");";
	private static final String TABLE_NAME1 = "urls";
	private static final String CREATE_QUERY1 = "CREATE TABLE " + TABLE_NAME1
			+ " (id INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "rid INTEGER NOT NULL," + "br TEXT NOT NULL,"
			+ "url TEXT NOT NULL);";
	private SQLiteDatabase db;
	private long lastRadioID = -1;

	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
//		this.context = context;
		this.db = getReadableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_QUERY);
		db.execSQL(CREATE_QUERY1);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DbHelper.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME1);
		onCreate(db);
	}

	/**
	 * Clear all radio entries in table.
	 */
	public void ClearRadios() {
		db.delete(TABLE_NAME, "1", null);
		db.delete(TABLE_NAME1, "1", null);
	}

	/**
	 * Gets the last radio id inserted.
	 * @return String with last row id inserted
	 */
	public String getLastId() {
		return "" + lastRadioID;
	}

	/**
	 * Gets the stored radios list.
	 * 
	 * @return A Cursor with only radio names.
	 */
	public Cursor GetRadiosList() {
		String[] columns = { "id", "name", "logo", "genre", "lang" };
		return getReadableDatabase().query(TABLE_NAME, columns, null, null,
				null, null, "name");
	}

	public Cursor GetRadioStreams(String id) {
		String[] columns = { "br", "url" };
		String[] selectionArgs = { id };
		String selection = "rid=?";
		return getReadableDatabase().query(TABLE_NAME1, columns, selection,
				selectionArgs, null, null, "br");
	}

	/**
	 * Gets the data of radio 'name' from DB.
	 * 
	 * @param name
	 *            The radio name.
	 * @return Returns a Cursor that points to data values, use it to get needed
	 *         data.
	 */
	public Cursor GetRadioData(String name) {
		String[] selectionArgs = { name };
		String selection = "name=?";
		return getReadableDatabase().query(TABLE_NAME, null, selection,
				selectionArgs, null, null, null);
	}

	/**
	 * Inserts the radio data into database
	 * 
	 * @param data
	 *            Array with values to store in database, following the next
	 *            format: {"name","stream","mail","about","logo","wurl","phone",
	 *            "rating","rfbook"}
	 * @return Returns true if data successfully inserted, false if not.
	 */
	public boolean InsertRadioData(String[] data) {
		String[] columns = { "name", "mail", "about", "logo", "wurl", "phone",
				"rating", "rfbook", "genre", "lang" };
		// Log.e("DbHelper", data[0]);
		if (columns.length == data.length) {
			ContentValues values = new ContentValues();
			for (int i = 0; i < columns.length; i++) {
				values.put(columns[i], data[i]);
			}
			lastRadioID = getWritableDatabase().insert(TABLE_NAME, null, values);
			if (lastRadioID == -1)
				return false;
			else
				return true;
		} else {
			if (columns.length > data.length)
				Log.e("SQLite insert error", "data in not sufficient...");
			else if (columns.length < data.length)
				Log.e("SQLite insert error", "data bigger than necessary...");
			return false;
		}
	}
	
	/**
	 * Inserts a radio stream url of radio ID
	 * @param data String array in format {radio ID, bitrate, radio url}
	 * @return true if successfully inserted, false else.
	 */
	public boolean InsertRadioStream(String[] data){
		String[] columns = {"rid","br","url"};
		if (columns.length == data.length) {
			ContentValues values = new ContentValues();
			for (int i = 0; i < columns.length; i++) {
				values.put(columns[i], data[i]);
			}
			long lid = getWritableDatabase().insert(TABLE_NAME1, null, values);
			if (lid == -1)
				return false;
			else
				return true;
		} else {
			if (columns.length > data.length)
				Log.e("SQLite insert error", "data in not sufficient...");
			else if (columns.length < data.length)
				Log.e("SQLite insert error", "data bigger than necessary...");
			return false;
		}
	}

}
