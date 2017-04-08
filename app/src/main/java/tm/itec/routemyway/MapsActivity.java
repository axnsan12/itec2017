package tm.itec.routemyway;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MapsActivity extends FragmentActivity {
	public static final String TAG = "MapsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
	}
}
