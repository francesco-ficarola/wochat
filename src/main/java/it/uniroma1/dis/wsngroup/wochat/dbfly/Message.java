package it.uniroma1.dis.wsngroup.wochat.dbfly;

public class Message {
	private String seqHex;
	private User receiver;
	private String body;
	
	public String getSeqHex() {
		return seqHex;
	}

	public void setSeqHex(String seqHex) {
		this.seqHex = seqHex;
	}

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
