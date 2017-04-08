package tm.itec.routemyway.network;



public class GoogleAuth {

	public static class Request {
		public final String idToken;

		public Request(String idToken) {
			this.idToken = idToken;
		}
	}
}
