package tm.itec.routemyway;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import tm.itec.routemyway.db.LocationContentProvider;
import tm.itec.routemyway.db.LocationsTable;
import tm.itec.routemyway.model.LocationModel;


public class RouteMapFragment extends SupportMapFragment implements OnMapReadyCallback {
	public static final String TAG = "RouteMapFragment";

	private static final Object QUERY_LAST_LOCATION = new Object(), QUERY_ALL_LOCATIONS = new Object();

	private GoogleMap mMap;
	private LocationQueryHandler mLocationUpdater;
	private LocationObserver mLocationObserver;
	private Marker mPositionMarker;

	private int mLocationUpdateToken = 0;
	private int mLastDisplayedToken = 0;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		getMapAsync(this);
		mLocationUpdater = new LocationQueryHandler(this);
		requestPositionUpdate();
		mLocationObserver = new LocationObserver(new Handler(Looper.getMainLooper()));

		Intent intent = new Intent(activity, LocationRecorderService.class);
		activity.startService(intent);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (getContentResolver() != null) {
			getContentResolver().unregisterContentObserver(mLocationObserver);
		}
	}

	@Nullable
	private ContentResolver getContentResolver() {
		Context context = getContext();
		if (context == null) {
			return null;
		}
		return context.getContentResolver();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mLocationObserver != null && getContentResolver() != null) {
			getContentResolver().registerContentObserver(LocationContentProvider.LAST_LOCATION_URI, false, mLocationObserver);
		}
	}


	private void requestPositionUpdate() {
		mLocationUpdater.startQuery(++mLocationUpdateToken, QUERY_LAST_LOCATION, LocationContentProvider.LAST_LOCATION_URI, null, null, null, null);
	}

	private void refreshMapDrawing() {
		mLocationUpdater.startQuery(0, QUERY_ALL_LOCATIONS, LocationContentProvider.CONTENT_URI, null, null, null, null);
	}

	private void updatePosition(double lat, double lng) {
		if (mMap == null) {
			return;
		}
		// Add a marker and move the camera
		LatLng latLng = new LatLng(lat, lng);
		if (mPositionMarker == null) {
			mPositionMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Current position"));
		}

		Log.i(TAG, "update map position - " + lat + " " + lng);
		mPositionMarker.setPosition(latLng);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mPositionMarker.getPosition(), 16));
	}


	private TileOverlay drawHeatmap(Collection<WeightedLatLng> weightedPositions) {
		if (mMap == null || weightedPositions.size() == 0) {
			return null;
		}
		HeatmapTileProvider provider = new HeatmapTileProvider.Builder().weightedData(weightedPositions).radius(50).build();
		return mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
	}

	private TileOverlay mStationaryHeatmap;

	private TileOverlay drawStationaryHeatmap(Collection<LocationModel> locations) {
		if (mStationaryHeatmap != null) {
			mStationaryHeatmap.remove();
		}

		ArrayList<WeightedLatLng> weightedPositions = new ArrayList<>();
		LocationModel prev = null;
		for (LocationModel loc : locations) {
			if (prev == null) {
				prev = loc;
				continue;
			}

			double weight = loc.when - prev.when;
			weightedPositions.add(new WeightedLatLng(new LatLng(prev.lat, prev.lng), weight));
			prev = loc;
		}

		return mStationaryHeatmap = drawHeatmap(weightedPositions);
	}


	private Polyline mPolyline;

	private Polyline drawPolyline(Collection<LocationModel> locations, int color) {
		if (mMap == null || locations.size() < 2) {
			return null;
		}

		if (mPolyline == null) {

			PolylineOptions polyline = new PolylineOptions().width(5).color(color).geodesic(true)
					.startCap(new RoundCap()).endCap(new RoundCap()).jointType(JointType.ROUND);
			mPolyline = mMap.addPolyline(polyline);

		}

		ArrayList<LatLng> latLngs = new ArrayList<>(locations.size());
		for (LocationModel loc : locations) {
			latLngs.add(loc.latLng());
		}
		mPolyline.setPoints(latLngs);
		return mPolyline;
	}

	private void drawRoutes(List<LocationModel> locations) {
		int colors[] = new int[] { Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.BLACK };
		int routeCount = 0;
		int routeStartIdx = 0;
		for (int i = 0;  i < locations.size() - 1; ++i) {
			if (!RouteUtils.sameRoute(locations.get(i), locations.get(i + 1)) || i == locations.size() - 2) {
				int routeEndIdx = i + 1;
				if (routeEndIdx - routeStartIdx > 3) {
					drawPolyline(locations.subList(routeStartIdx, routeEndIdx), colors[routeCount % colors.length]);
					routeCount += 1;
				}

				routeStartIdx = routeEndIdx;
			}
		}



		Log.i(TAG, "Drew " + routeCount + " routes");
	}


	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		refreshMapDrawing();
	}

	private static class LocationQueryHandler extends AsyncQueryHandler
	{
		private final WeakReference<RouteMapFragment> mFragment;
		public LocationQueryHandler(@NonNull RouteMapFragment fragment) {
			super(fragment.getContentResolver());
			mFragment = new WeakReference<>(fragment);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			RouteMapFragment fragment = mFragment.get();
			if (fragment == null) {
				return;
			}
			if (cookie == QUERY_LAST_LOCATION) {

				if (fragment.mLastDisplayedToken < token) {
					if (cursor.moveToNext()) {
						double lat = cursor.getDouble(cursor.getColumnIndex(LocationsTable.KEY_LAT));
						double lng = cursor.getDouble(cursor.getColumnIndex(LocationsTable.KEY_LNG));
						int id = cursor.getInt(cursor.getColumnIndex(LocationsTable.KEY_ID));
						fragment.updatePosition(lat, lng);
						fragment.refreshMapDrawing();
						Log.i(TAG, "location ID " + id);
						fragment.mLastDisplayedToken = token;
					}
				}
			}

			if (cookie == QUERY_ALL_LOCATIONS) {
				ArrayList<LocationModel> locations = new ArrayList<>();
				LocationModel prev = null;
				while (cursor.moveToNext()) {
					double lat = cursor.getDouble(cursor.getColumnIndex(LocationsTable.KEY_LAT));
					double lng = cursor.getDouble(cursor.getColumnIndex(LocationsTable.KEY_LNG));
					Date when;
					try {
						when = LocationsTable.DATETIME_FORMAT.parse(cursor.getString(cursor.getColumnIndex(LocationsTable.KEY_WHEN)));
					} catch (ParseException e) {
						e.printStackTrace();
						continue;
					}

					LocationModel next = new LocationModel(lat, lng, when);
//					if (prev == null || !RouteUtils.shouldSkip(prev, next)) {
//						locations.add(next);
//						prev = next;
//					}
					locations.add(next);
					prev = next;
				}
				Log.i(TAG, "Read " + locations.size() + " locatiosn from db");
				fragment.drawStationaryHeatmap(locations);
				fragment.drawRoutes(locations);
			}
		}
	}

	private class LocationObserver extends ContentObserver
	{
		public LocationObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			requestPositionUpdate();
		}
	}
}
