package tm.itec.routemyway.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tm.itec.routemyway.model.LocationModel;

public class SaveLocationsRequest {
	public final String token;
	public final List<LocationModel> locations;

	public SaveLocationsRequest(String token, Collection<LocationModel> locations) {
		this.token = token;
		this.locations = new ArrayList<>(locations);
	}
}
