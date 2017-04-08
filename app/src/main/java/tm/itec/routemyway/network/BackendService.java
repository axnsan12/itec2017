package tm.itec.routemyway.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import tm.itec.routemyway.model.Session;

public interface BackendService {
	@POST("/auth/google")
	Call<Session> googleAuth(@Body GoogleAuth.Request authRequest);

	@POST("/users/{userid}/locations")
	Call<ResponseBody> saveLocations(@Path("userid") int userId, @Body SaveLocationsRequest locationsRequest);
}
