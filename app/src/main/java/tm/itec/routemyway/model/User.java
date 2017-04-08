package tm.itec.routemyway.model;

public class User {
	public final int id;
	public final String email, name, googleId;

	public User(int id, String email, String name, String googleId) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.googleId = googleId;
	}
}
