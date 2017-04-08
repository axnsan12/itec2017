package tm.itec.routemyway;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Collections;
import java.util.Date;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import tm.itec.routemyway.db.LocationContentProvider;
import tm.itec.routemyway.db.LocationsTable;
import tm.itec.routemyway.model.LocationModel;
import tm.itec.routemyway.network.BackendService;
import tm.itec.routemyway.network.ResponseCallback;
import tm.itec.routemyway.network.SaveLocationsRequest;


public class LocationRecorderService extends Service {
	public static final String TAG = "LocationRecorderSerice";

	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;

	private Location mLastLocation;
	private Date mLocationUpdateTime;
	private BackendService mBackendService;

	public static LocationRequest buildLocationRequest() {
		LocationRequest request = new LocationRequest();
		request.setInterval(3000);
		request.setFastestInterval(1000);
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		return request;
	}

	GoogleApiClient.ConnectionCallbacks mConnectionHandler = new GoogleApiClient.ConnectionCallbacks() {
		@Override
		public void onConnected(@Nullable Bundle bundle) {
			try {
				mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
				if (mLastLocation != null) {
					Log.i(TAG, "Location update (initial): " + mLastLocation);
				}

				LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
						mLocationRequest, mLocationCollector);
			}
			catch (SecurityException e) {
				Log.e(TAG, "Location recording failed - no permission");
			}
		}

		@Override
		public void onConnectionSuspended(int i) {

		}
	};

	private LocationListener mLocationCollector = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			mLastLocation = location;
			mLocationUpdateTime = new Date();
			Log.i(TAG, "Location update: " + location);

			ContentValues dbValues = new ContentValues();
			dbValues.put(LocationsTable.KEY_LAT, location.getLatitude());
			dbValues.put(LocationsTable.KEY_LNG, location.getLongitude());
			dbValues.put(LocationsTable.KEY_WHEN, LocationsTable.DATETIME_FORMAT.format(mLocationUpdateTime));
			getContentResolver().insert(LocationContentProvider.CONTENT_URI, dbValues);

			String token = "e05368d61dc171fde4d9339b2b979e454defd3413cc43ccf0993d879c69b98ec4e5e0fba75d8c63f";
			LocationModel model = new LocationModel(location.getLatitude(), location.getLongitude(), mLocationUpdateTime);
			SaveLocationsRequest saveLocationsRequest = new SaveLocationsRequest(token, Collections.singleton(model));
			Call<ResponseBody> response = mBackendService.saveLocations(1, saveLocationsRequest);
			response.enqueue(new ResponseCallback<ResponseBody>() {
				@Override
				public void onSuccess(Response<ResponseBody> response) {
					Log.i(TAG, "Location save success");
				}

				@Override
				public void onFailure(Response<ResponseBody> response) {
					Log.i(TAG, "Location save fail");
				}

				@Override
				public void onFailure(Call<ResponseBody> call, Throwable t) {
					Log.i(TAG, "Location save error " + t.toString());
				}
			});
		}

	};

	@Override
	public void onCreate() {
		// Create an instance of GoogleAPIClient.
		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(mConnectionHandler)
					.addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
						@Override
						public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

						}
					})
					.addApi(LocationServices.API)
					.build();
		}

		mLocationRequest = buildLocationRequest();
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("http://192.168.1.131:5000/")
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		mBackendService = retrofit.create(BackendService.class);
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		mGoogleApiClient.connect();
		return START_STICKY;
	}

	public void onDestroy() {
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationCollector);
		mGoogleApiClient.disconnect();
		super.onDestroy();
	}
}
