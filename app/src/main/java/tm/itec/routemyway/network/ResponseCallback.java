package tm.itec.routemyway.network;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public abstract class ResponseCallback<T> implements Callback<T> {
	@Override
	public final void onResponse(Call<T> call, Response<T> response) {
		if (response.isSuccessful()) {
			onSuccess(response);
		}
		else {
			onFailure(response);
		}
	}

	public abstract void onSuccess(Response<T> response);
	public abstract void onFailure(Response<T> response);
}
