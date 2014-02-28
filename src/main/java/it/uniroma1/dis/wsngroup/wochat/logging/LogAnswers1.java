package it.uniroma1.dis.wsngroup.wochat.logging;

import org.apache.log4j.Logger;

public class LogAnswers1 {
	private static Logger logger = Logger.getLogger(LogAnswers1.class);
	
	public static void logAnswer(String msg) {
		logger.info(msg);
	}
}
