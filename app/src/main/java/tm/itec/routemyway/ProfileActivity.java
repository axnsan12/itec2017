package tm.itec.routemyway;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import static tm.itec.routemyway.LoginActivity.KEY_GOOGLE_EMAIL;
import static tm.itec.routemyway.LoginActivity.KEY_GOOGLE_NAME;
import static tm.itec.routemyway.LoginActivity.KEY_GOOGLE_PICTURE_URL;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageView profilePicture = (ImageView) findViewById(R.id.profilePicture);
        TextView userName = (TextView) findViewById(R.id.userName);
        TextView emailAddress = (TextView) findViewById(R.id.emailAddress);

        final SharedPreferences sharedPrefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String googleEmail = sharedPrefs.getString(KEY_GOOGLE_EMAIL, null);
        String googleName = sharedPrefs.getString(KEY_GOOGLE_NAME, null);
        String profilePictureUrl = sharedPrefs.getString(KEY_GOOGLE_PICTURE_URL, null);
        if (profilePictureUrl != null) {
            Picasso.with(this).load(profilePictureUrl).fit().into(profilePicture);
        }
        userName.setText(googleName);
        emailAddress.setText(googleEmail);

        View signOutButton = findViewById(R.id.btnSignOut);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPrefs.edit().clear().commit();
                finish();
            }
        });
    }

}
