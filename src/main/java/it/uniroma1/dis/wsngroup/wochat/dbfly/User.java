package it.uniroma1.dis.wsngroup.wochat.dbfly;

import java.util.List;

public class User {
	private String id;
	private String username;
	private Message msg;
	private String numSurvey;
	private String numRound;
	private List<String> answersSurvey;
	
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

	public String getNumSurvey() {
		return numSurvey;
	}

	public void setNumSurvey(String numSurvey) {
		this.numSurvey = numSurvey;
	}

	public String getNumRound() {
		return numRound;
	}

	public void setNumRound(String numRound) {
		this.numRound = numRound;
	}

	public List<String> getAnswersSurvey() {
		return answersSurvey;
	}

	public void setAnswersSurvey(List<String> answersSurvey) {
		this.answersSurvey = answersSurvey;
	}
}
