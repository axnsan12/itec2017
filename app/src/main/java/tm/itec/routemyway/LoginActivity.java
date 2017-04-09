package tm.itec.routemyway;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import tm.itec.routemyway.model.Session;
import tm.itec.routemyway.network.BackendService;
import tm.itec.routemyway.network.GoogleAuth;
import tm.itec.routemyway.network.ResponseCallback;

public class LoginActivity extends AppCompatActivity {
	private static final String TAG = "LoginActivity";

	public static final String KEY_GOOGLE_ID_TOKEN = "googleIdToken";
	public static final String KEY_BACKEND_TOKEN = "backendToken";
	public static final String KEY_USER_ID = "userId";

	private GoogleApiClient mGoogleApiClient;
	private SharedPreferences mSharedPrefs;
	private SignInButton mGoogleSignInButton;

	private boolean mLoggedIn, mCheckedLocation, mMinimumDelayPassed;

	private static final int RC_SIGN_IN_GOOGLE = 1;
	private static final int RC_LOCATION_PERMISSION = 2;
	private static final int RC_LOCATION_SETTINGS = 3;

	private BackendService mService;

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
						locationSettingsStatus.startResolutionForResult(LoginActivity.this, RC_LOCATION_SETTINGS);
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

	private void checkLocationSettings() {
		mGoogleApiClient.connect();
		onLocationReady();
	}

	private void onLocationReady() {
		mCheckedLocation = true;
		Intent intent = new Intent(this, LocationRecorderService.class);
		startService(intent);
		nextActivity();
	}

	private void onLocationUnavailable() {
		Intent intent = new Intent(this, LocationRecorderService.class);
		stopService(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
//		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//		setSupportActionBar(toolbar);

		// Configure sign-in to request the user's ID, email address, and basic
		// profile. ID and basic profile are included in DEFAULT_SIGN_IN.
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.server_client_id))
				.requestEmail()
				.build();

		// Build a GoogleApiClient with access to the Google Sign-In API and the
		// options specified by gso.
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(connectionCallbacks)
				.enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
						Log.e(TAG, "Google auth - CONNECTION FAILED: " + connectionResult.getErrorMessage());
						onLocationUnavailable();
					}
				})
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.addApi(LocationServices.API)
				.build();

		mGoogleSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
		mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mGoogleSignInButton.setEnabled(false);
				googleSignIn();
			}
		});

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("http://192.168.43.197:5000/")
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		mService = retrofit.create(BackendService.class);

		mSharedPrefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		String googleIdToken = mSharedPrefs.getString(KEY_GOOGLE_ID_TOKEN, null);
		if (googleIdToken != null) {
			mGoogleSignInButton.setEnabled(false);
			String backendToken = mSharedPrefs.getString(KEY_BACKEND_TOKEN, null);
			if (backendToken != null) {
				mLoggedIn = true;
				nextActivity();
			}
			else {
				authBackend(googleIdToken);
			}
		}

		checkLocationPermission();

		Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mMinimumDelayPassed = true;
				nextActivity();
			}
		}, 1500);
	}

	private void googleSignIn() {
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
		startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
		if (requestCode == RC_SIGN_IN_GOOGLE) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
			handleSignInResult(result);
		}

		if (requestCode == RC_LOCATION_SETTINGS) {
			if (resultCode == Activity.RESULT_OK) {
				onLocationReady();
			}
			else {
				onLocationUnavailable();
			}
		}
	}

	private void nextActivity() {
		if (mLoggedIn && mCheckedLocation && mMinimumDelayPassed) {
			Intent intent = new Intent(LoginActivity.this, TodayActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void authBackend(String idToken) {
		Call<Session> response = mService.googleAuth(new GoogleAuth.Request(idToken));
		response.enqueue(new ResponseCallback<Session>() {
			@Override
			public void onSuccess(Response<Session> response) {
				Log.i(TAG, "Backend authentication success: " + response.body().token);

				SharedPreferences.Editor editor = mSharedPrefs.edit();
				editor.putString(KEY_BACKEND_TOKEN, response.body().token);
				editor.putInt(KEY_USER_ID, response.body().user.id);
				editor.apply();

				mLoggedIn = true;
				nextActivity();
			}

			@Override
			public void onFailure(Response<Session> response) {
				Log.i(TAG, "Backend authentication rejected: " + response.errorBody());
				mGoogleSignInButton.setEnabled(true);
				mLoggedIn = true;
				nextActivity();
			}

			@Override
			public void onFailure(Call<Session> call, Throwable t) {
				Log.i(TAG, "Backend authentication request failed: " + t.toString());
				mGoogleSignInButton.setEnabled(true);
				mLoggedIn = true;
				nextActivity();
			}
		});
	}

	private void handleSignInResult(GoogleSignInResult result) {
		Log.d(TAG, "handleSignInResult:" + result.isSuccess());
		if (result.isSuccess()) {
			// Signed in successfully, show authenticated UI.
			GoogleSignInAccount acct = result.getSignInAccount();
			if (acct != null) {
				Log.i(TAG, "Google log in success: " + acct.getDisplayName() + " " + acct.getEmail() + " " + acct.getIdToken() + " " + acct.getId());

				SharedPreferences.Editor editor = mSharedPrefs.edit();
				editor.putString(KEY_GOOGLE_ID_TOKEN, acct.getIdToken());
				editor.apply();

				authBackend(acct.getIdToken());
			}
			else {
				Log.i(TAG, "Google sign in - no account");
				mGoogleSignInButton.setEnabled(true);
			}

		} else {
			// Signed out, show unauthenticated UI.
			Log.i(TAG, "Google sign in fail " + result.toString());
			mGoogleSignInButton.setEnabled(true);
		}
	}

}
