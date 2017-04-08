package tm.itec.routemyway;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class TodayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View speedInfoButton = findViewById(R.id.btnSpeedInfo);
        speedInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TodayActivity.this, SpeedInfoActivity.class);
                startActivity(intent);
            }
        });

        View positionInfoButton = findViewById(R.id.btnPosInfo);
        positionInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TodayActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });


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
    }

}
