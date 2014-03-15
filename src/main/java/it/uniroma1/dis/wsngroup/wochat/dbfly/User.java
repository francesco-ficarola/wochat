package it.uniroma1.dis.wsngroup.wochat.dbfly;

import java.util.List;

public class User {
	private String id;
	private String username;
	private Message msg;
	private List<String> answersSurvey1;
	private List<String> answersSurvey2;
	
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

	public List<String> getAnswersSurvey1() {
		return answersSurvey1;
	}

	public void setAnswersSurvey1(List<String> answersSurvey1) {
		this.answersSurvey1 = answersSurvey1;
	}

	public List<String> getAnswersSurvey2() {
		return answersSurvey2;
	}

	public void setAnswersSurvey2(List<String> answersSurvey2) {
		this.answersSurvey2 = answersSurvey2;
	}
}
