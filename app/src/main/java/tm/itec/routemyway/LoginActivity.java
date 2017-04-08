package tm.itec.routemyway;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

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

	private GoogleApiClient mGoogleApiClient;

	private static final int RC_SIGN_IN_GOOGLE = 1;
	private BackendService mService;

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
				.enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
						Log.e(TAG, "Google auth - CONNECTION FAILED: " + connectionResult.getErrorMessage());
					}
				})
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.build();

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				googleSignIn();
			}
		});

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("http://192.168.1.131:5000/")
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		mService = retrofit.create(BackendService.class);
	}

	private void googleSignIn() {
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
		startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
		if (requestCode == RC_SIGN_IN_GOOGLE) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
			handleSignInResult(result);
		}
	}

	private void handleSignInResult(GoogleSignInResult result) {
		Log.d(TAG, "handleSignInResult:" + result.isSuccess());
		if (result.isSuccess()) {
			// Signed in successfully, show authenticated UI.
			GoogleSignInAccount acct = result.getSignInAccount();
			if (acct != null) {
				Log.i(TAG, "Google log in success: " + acct.getDisplayName() + " " + acct.getEmail() + " " + acct.getIdToken() + " " + acct.getId());
				Call<Session> response = mService.googleAuth(new GoogleAuth.Request(acct.getIdToken()));
				response.enqueue(new ResponseCallback<Session>() {
					@Override
					public void onSuccess(Response<Session> response) {
						Log.i(TAG, "Backend authentication success: " + response.body().token);
//						Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
//						startActivity(intent);
						Intent intent = new Intent(LoginActivity.this, TodayActivity.class);
						startActivity(intent);
					}

					@Override
					public void onFailure(Response<Session> response) {
						Log.i(TAG, "Backend authentication rejected: " + response.errorBody());
					}

					@Override
					public void onFailure(Call<Session> call, Throwable t) {
						Log.i(TAG, "Backend authentication request failed: " + t.toString());
					}
				});
			}
			else {
				Log.i(TAG, "Google sign in - no account");
			}

		} else {
			// Signed out, show unauthenticated UI.
			Log.i(TAG, "Google sign in fail " + result.toString());
		}
	}

}
