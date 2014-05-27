package it.uniroma1.dis.wsngroup.wochat.dbfly;

public class Answer {
	private String answerString;
	private String confidence;
	
	public Answer(String answerString, String confidence) {
		this.answerString = answerString;
		this.confidence = confidence;
	}

	public String getAnswerString() {
		return answerString;
	}

	public void setAnswerString(String answerString) {
		this.answerString = answerString;
	}

	public String getConfidence() {
		return confidence;
	}

	public void setConfidence(String confidence) {
		this.confidence = confidence;
	}
}
