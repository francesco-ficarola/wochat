package it.uniroma1.dis.wsngroup.wochat.logging;

import org.apache.log4j.Logger;

public class LogAnswer {
	private static Logger logger = Logger.getLogger(LogAnswer.class);
	
	public static void logAnswer(String msg) {
		logger.info(msg);
	}
}
