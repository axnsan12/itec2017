package tm.itec.routemyway.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.renderscript.RSInvalidStateException;
import android.support.annotation.NonNull;

public class LocationContentProvider extends ContentProvider {

	private LocationDbHelper dbHelper;

	private static final int ALL_LOCATIONS = 1;
	private static final int LOCATION_BY_ID = 2;
	private static final int LOCATIONS_BETWEEN_TIMESTAMPS = 3;
	private static final int LAST_LOCATION = 4;

	// authority is the symbolic name of your provider
	// To avoid conflicts with other providers, you should use
	// Internet domain ownership (in reverse) as the basis of your provider authority.
	private static final String AUTHORITY = "tm.itec.routemyway.locations";

	// create content URIs from the authority by appending path to database table
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/all");

	public static final Uri LAST_LOCATION_URI = Uri.parse("content://" + AUTHORITY + "/last");

	// a content URI pattern matches content URIs using wildcard characters:
	// *: Matches a string of any valid characters of any length.
	// #: Matches a string of numeric characters of any length.
	private static final UriMatcher uriMatcher;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "all", ALL_LOCATIONS);
		uriMatcher.addURI(AUTHORITY, "byid/#", LOCATION_BY_ID);
		uriMatcher.addURI(AUTHORITY, "between/#/#", LOCATIONS_BETWEEN_TIMESTAMPS);
		uriMatcher.addURI(AUTHORITY, "last", LAST_LOCATION);
	}

	@NonNull
	private Context getContextNonNull() {
		Context context = getContext();
		if (context == null) {
			throw new RSInvalidStateException("content provider is not bound to a context");
		}
		return context;
	}

	// system calls onCreate() when it starts up the provider.
	@Override
	public boolean onCreate() {
		// get access to the database helper
		dbHelper = new LocationDbHelper(getContext());
		return false;
	}

	//Return the MIME type corresponding to a content URI
	@Override
	public String getType(@NonNull Uri uri) {

		switch (uriMatcher.match(uri)) {
			case ALL_LOCATIONS:
			case LOCATIONS_BETWEEN_TIMESTAMPS:
				return "vnd.android.cursor.dir/vnd.com.tm.itec.routemyway.location";
			case LOCATION_BY_ID:
			case LAST_LOCATION:
				return "vnd.android.cursor.item/vnd.com.tm.itec.routemyway.location";
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	// The insert() method adds a new row to the appropriate table, using the values
	// in the ContentValues argument. If a column name is not in the ContentValues argument,
	// you may want to provide a default value for it either in your provider code or in
	// your database schema.
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (uriMatcher.match(uri)) {
			case ALL_LOCATIONS:
				//do nothing
				break;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		long id = db.insert(LocationsTable.TABLE_NAME, null, values);
		getContextNonNull().getContentResolver().notifyChange(uri, null);
		getContextNonNull().getContentResolver().notifyChange(LAST_LOCATION_URI, null);
		return Uri.parse(CONTENT_URI + "/" + id);
	}

	// The query() method must return a Cursor object, or if it fails,
	// throw an Exception. If you are using an SQLite database as your data storage,
	// you can simply return the Cursor returned by one of the query() methods of the
	// SQLiteDatabase class. If the query does not match any rows, you should return a
	// Cursor instance whose getCount() method returns 0. You should return null only
	// if an internal error occurred during the query process.
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
	                    String[] selectionArgs, String sortOrder) {

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(LocationsTable.TABLE_NAME);

		switch (uriMatcher.match(uri)) {
			case ALL_LOCATIONS:
				//do nothing
				break;
			case LOCATION_BY_ID:
				String id = uri.getPathSegments().get(1);
				queryBuilder.appendWhere(LocationsTable.KEY_ID + "=" + id);
				break;
			case LAST_LOCATION:
				return queryBuilder.query(db, projection, selection, selectionArgs, null, null,
						LocationsTable.KEY_WHEN + " DESC", "1");
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		return queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);

	}

	// The delete() method deletes rows based on the seletion or if an id is
	// provided then it deleted a single row. The methods returns the numbers
	// of records delete from the database. If you choose not to delete the data
	// physically then just update a flag here.
	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("cannot delete from location database");
	}

	// The update method() is same as delete() which updates multiple rows
	// based on the selection or a single row if the row id is provided. The
	// update method returns the number of updated rows.
	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection,
	                  String[] selectionArgs) {
		throw new UnsupportedOperationException("cannot update location database");
	}

}
