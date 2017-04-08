package tm.itec.routemyway.model;


public class Session {
	public final User user;
	public final String token;

	public Session(User user, String token) {
		this.user = user;
		this.token = token;
	}
}
