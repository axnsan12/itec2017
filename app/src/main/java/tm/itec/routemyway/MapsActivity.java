package tm.itec.routemyway;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import tm.itec.routemyway.db.LocationContentProvider;
import tm.itec.routemyway.db.LocationsTable;
import tm.itec.routemyway.model.LocationModel;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
	public static final String TAG = "MapsActivity";

	private static final int RC_LOCATION_PERMISSION = 2;
	private static final int RC_LOCATION_SETTINGS = 3;

	private static final Object QUERY_LAST_LOCATION = new Object(), QUERY_ALL_LOCATIONS = new Object();

	private GoogleMap mMap;
	private GoogleApiClient mGoogleApiClient;
	private LocationQueryHandler mLocationUpdater;
	private LocationObserver mLocationObserver;
	private Marker mPositionMarker;

	private int mLocationUpdateToken = 0;
	private int mLastDisplayedToken = 0;

	private void checkLocationPermission() {
		int fine = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
		int coarse = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
		boolean havePermission = fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED;
		if (!havePermission) {
			ActivityCompat.requestPermissions(this,
					new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
					RC_LOCATION_PERMISSION);
		}
		else {
			checkLocationSettings();
		}
	}

	private void checkLocationSettings() {
		final ResultCallback<LocationSettingsResult> settingsResultCallback = new ResultCallback<LocationSettingsResult>() {
			@Override
			public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
				Status locationSettingsStatus = locationSettingsResult.getStatus();
				switch (locationSettingsStatus.getStatusCode()) {
					case LocationSettingsStatusCodes.SUCCESS:
						onLocationReady();
						break;

					case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
						// Location settings are not satisfied, but this can be fixed
						// by showing the user a dialog.
						try {
							// Show the dialog by calling startResolutionForResult(),
							// and check the result in onActivityResult().
							locationSettingsStatus.startResolutionForResult(MapsActivity.this, RC_LOCATION_SETTINGS);
						} catch (IntentSender.SendIntentException e) {
							onLocationUnavailable();
						}
					break;
					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
						break;
				}
			}
		};

		final GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
			@Override
			public void onConnected(@Nullable Bundle bundle) {
				LocationRequest locationRequest = LocationRecorderService.buildLocationRequest();
				LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
						.addLocationRequest(locationRequest);

				PendingResult<LocationSettingsResult> result =
						LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

				result.setResultCallback(settingsResultCallback);
			}

			@Override
			public void onConnectionSuspended(int i) {

			}
		};

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(connectionCallbacks)
				.addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
						onLocationUnavailable();
					}
				})
				.addApi(LocationServices.API)
				.build();

		mGoogleApiClient.connect();
	}

	private void onLocationReady() {
		Intent intent = new Intent(this, LocationRecorderService.class);
		startService(intent);
	}

	private void onLocationUnavailable() {
		Intent intent = new Intent(this, LocationRecorderService.class);
		stopService(intent);
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		checkLocationPermission();
		mLocationUpdater = new LocationQueryHandler(this);
		requestPositionUpdate();

		mLocationObserver = new LocationObserver(new Handler(Looper.getMainLooper()));
	}

	@Override
	protected void onPause() {
		super.onPause();
		getContentResolver().unregisterContentObserver(mLocationObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mLocationObserver != null) {
			getContentResolver().registerContentObserver(LocationContentProvider.LAST_LOCATION_URI, false, mLocationObserver);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case RC_LOCATION_PERMISSION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					checkLocationSettings();
				} else {
					onLocationUnavailable();
				}
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RC_LOCATION_SETTINGS) {
			if (resultCode == Activity.RESULT_OK) {
				onLocationReady();
			}
			else {
				onLocationUnavailable();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void requestPositionUpdate() {
		mLocationUpdater.startQuery(++mLocationUpdateToken, QUERY_LAST_LOCATION, LocationContentProvider.LAST_LOCATION_URI, null, null, null, null);
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

	private void drawHeatmap(Collection<WeightedLatLng> weightedPositions) {
		if (mMap == null || weightedPositions.size() == 0) {
			return;
		}
		HeatmapTileProvider provider = new HeatmapTileProvider.Builder().weightedData(weightedPositions).radius(50).build();
		mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
	}

	private void drawStationaryHeatmap(Collection<LocationModel> locations) {
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

		drawHeatmap(weightedPositions);
	}

	private void drawPolyline(Collection<LocationModel> locations) {
		if (mMap == null || locations.size() < 2) {
			return;
		}

		PolylineOptions polyline = new PolylineOptions().width(5).color(Color.RED);
		ArrayList<LatLng> latLngs = new ArrayList<>(locations.size());
		for (LocationModel loc : locations) {
			latLngs.add(loc.latLng());
		}
		polyline.addAll(latLngs);
		polyline.jointType(JointType.ROUND);
		mMap.addPolyline(polyline);
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		updatePosition(-34, 151);
		mLocationUpdater.startQuery(0, QUERY_ALL_LOCATIONS, LocationContentProvider.CONTENT_URI, null, null, null, null);
	}

	private static class LocationQueryHandler extends AsyncQueryHandler
	{
		private final WeakReference<MapsActivity> mActivity;
		public LocationQueryHandler(@NonNull  MapsActivity activity) {
			super(activity.getContentResolver());
			mActivity = new WeakReference<>(activity);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			MapsActivity activity = mActivity.get();
			if (activity == null) {
				return;
			}
			if (cookie == QUERY_LAST_LOCATION) {

				if (activity.mLastDisplayedToken < token) {
					if (cursor.moveToNext()) {
						double lat = cursor.getDouble(cursor.getColumnIndex(LocationsTable.KEY_LAT));
						double lng = cursor.getDouble(cursor.getColumnIndex(LocationsTable.KEY_LNG));
						int id = cursor.getInt(cursor.getColumnIndex(LocationsTable.KEY_ID));
						activity.updatePosition(lat, lng);
						Log.i(TAG, "location ID " + id);
						activity.mLastDisplayedToken = token;
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
					if (prev == null || !RouteUtils.shouldSkip(prev, next)) {
						locations.add(next);
						prev = next;
					}
				}
				Log.i(TAG, "Read " + locations.size() + " locatiosn from db");
				activity.drawStationaryHeatmap(locations);
				activity.drawPolyline(locations);
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
