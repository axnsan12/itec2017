package tm.itec.routemyway;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import tm.itec.routemyway.model.LocationModel;

public class RouteUtils {

	public static final int NEGLIGIBLE_DISTANCE_METERS = 20;

	public static boolean locationEqual(LatLng loc1, LatLng loc2) {
		return SphericalUtil.computeDistanceBetween(loc1, loc2) < NEGLIGIBLE_DISTANCE_METERS;
	}

	public static boolean locationEqual(LocationModel loc1, LocationModel loc2) {
		return locationEqual(loc1.latLng(), loc2.latLng());
	}

	public static boolean shouldSkip(LocationModel current, LocationModel next) {
		double distance = SphericalUtil.computeDistanceBetween(current.latLng(), next.latLng());
		long elapsedSeconds = next.when - current.when;
		if (elapsedSeconds < 0) {
			throw new IllegalArgumentException("locations out of order");
		}

		boolean verySlow = distance / elapsedSeconds < 0.5; // meters per second
		boolean close = distance < 20; // meters
		return verySlow || close;
	}
}
