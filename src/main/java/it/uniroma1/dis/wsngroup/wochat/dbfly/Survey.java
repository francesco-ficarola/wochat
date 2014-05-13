package it.uniroma1.dis.wsngroup.wochat.dbfly;

import java.util.List;

public class Survey {
	private String numSurvey;
	private String numRound;
	private List<String> questionsList;

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

	public List<String> getQuestionsList() {
		return questionsList;
	}

	public void setQuestionsList(List<String> questionsList) {
		this.questionsList = questionsList;
	}
}
