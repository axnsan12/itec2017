package tm.itec.routemyway;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

public class SpeedInfoActivity extends AppCompatActivity {

    private static class DropdownAdapter extends ArrayAdapter<String>
    {
        public DropdownAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                view = (TextView) inflater.inflate(R.layout.time_interval_spinner_dropdown_item, parent, false);
            }

            view.setText(this.getItem(position));
            return view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Spinner dropdown = (Spinner)findViewById(R.id.dropdownSpeedInfo);
        String[] items = new String[]{"General", "Day", "Week", "Month", "Year"};
        ArrayAdapter<String> adapter = new DropdownAdapter(this, R.layout.time_interval_spinner_item, items);
        dropdown.setAdapter(adapter);
    }

}
