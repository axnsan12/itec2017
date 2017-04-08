package tm.itec.routemyway.model;


import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class LocationModel {
	public final double lat, lng;
	public final long when;

	public LocationModel(double lat, double lng, Date when) {
		this.lat = lat;
		this.lng = lng;
		this.when = when.getTime() / 1000;
	}

	public LatLng latLng() {
		return new LatLng(lat, lng);
	}
}
