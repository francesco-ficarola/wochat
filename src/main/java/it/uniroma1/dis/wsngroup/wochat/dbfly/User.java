package it.uniroma1.dis.wsngroup.wochat.dbfly;

public class User {
	private String id;
	private String username;
	private Message msg;
	
	public String getId() {
		return id;
	}
	
	public User setId(String id) {
		this.id = id;
		return this;
	}
	
	public String getUsername() {
		return username;
	}
	
	public User setUsername(String username) {
		this.username = username;
		return this;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}
}
