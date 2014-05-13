package it.uniroma1.dis.wsngroup.wochat.logging;

import org.apache.log4j.Logger;

public class LogAnswers {
	private static Logger logger = Logger.getLogger(LogAnswers.class);
	
	public static void logAnswer(String msg) {
		logger.info(msg);
	}
}
