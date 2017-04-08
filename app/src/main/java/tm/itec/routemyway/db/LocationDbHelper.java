package tm.itec.routemyway.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocationDbHelper extends SQLiteOpenHelper {
	public static final String DATABASE_NAME = "locations.db";
	public static final int DATABASE_VERSION = 1;

	public LocationDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + LocationsTable.TABLE_NAME + " (" +
				LocationsTable.KEY_ID + " INTEGER NOT NULL, " +
				LocationsTable.KEY_LAT + " FLOAT, " +
				LocationsTable.KEY_LNG + " FLOAT, " +
				LocationsTable.KEY_WHEN + " DATETIME, " +
				"PRIMARY KEY (id))");
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

	}
}
