package it.uniroma1.dis.wsngroup.wochat.dbfly;

public class Message {
	private User receiver;
	private String body;
	
	public User getReceiver() {
		return receiver;
	}
	
	public void setReceiver(User receiver) {
		this.receiver = receiver;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
}
