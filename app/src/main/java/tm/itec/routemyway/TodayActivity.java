package tm.itec.routemyway;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import static tm.itec.routemyway.LoginActivity.KEY_GOOGLE_EMAIL;
import static tm.itec.routemyway.LoginActivity.KEY_GOOGLE_NAME;
import static tm.itec.routemyway.LoginActivity.KEY_GOOGLE_PICTURE_URL;

public class TodayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View speedInfoButton = findViewById(R.id.btnSpeedInfo);
        View.OnClickListener openMap = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TodayActivity.this, SpeedInfoActivity.class);
                startActivity(intent);
            }
        };
        speedInfoButton.setOnClickListener(openMap);
        findViewById(R.id.mapContainer).setOnClickListener(openMap);

        View positionInfoButton = findViewById(R.id.btnPosInfo);
        positionInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TodayActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        View positionProfileButton = findViewById(R.id.btnProfileInfo);
        positionProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TodayActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        ImageView profilePicture = (ImageView) findViewById(R.id.profilePicture);
        TextView userName = (TextView) findViewById(R.id.userName);
        TextView emailAddress = (TextView) findViewById(R.id.emailAddress);

        SharedPreferences sharedPrefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String googleEmail = sharedPrefs.getString(KEY_GOOGLE_EMAIL, null);
        String googleName = sharedPrefs.getString(KEY_GOOGLE_NAME, null);
        String profilePictureUrl = sharedPrefs.getString(KEY_GOOGLE_PICTURE_URL, null);
        if (profilePictureUrl != null) {
            Picasso.with(this).load(profilePictureUrl).fit().into(profilePicture);
        }
        userName.setText(googleName);
        emailAddress.setText(googleEmail);
    }
}

//        PieChart chart = (PieChart) findViewById(R.id.chart);
//        PieData data = new PieData();
//
//        List<PieEntry> pieEntries = new ArrayList<>();
//        pieEntries.add(new PieEntry(0.3f, "treij"));
//        pieEntries.add(new PieEntry(0.7f, "șaptej"));
//        PieDataSet pieDataSet = new PieDataSet(pieEntries, "ce label vrei?!");
//        pieDataSet.setColors(new int[] { R.color.red, R.color.green }, this);
//        data.addDataSet(pieDataSet);
//        chart.setData(data);

