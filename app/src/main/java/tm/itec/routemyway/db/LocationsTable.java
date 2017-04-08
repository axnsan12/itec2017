package tm.itec.routemyway.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationsTable {
	public static final String TABLE_NAME = "locations";
	public static final String KEY_ID = "id";
	public static final String KEY_LAT = "lat";
	public static final String KEY_LNG = "lng";
	public static final String KEY_WHEN = "recorded_at";


	public static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

	public static String formatDateTime(Date date) {
		return DATETIME_FORMAT.format(date);
	}
}
