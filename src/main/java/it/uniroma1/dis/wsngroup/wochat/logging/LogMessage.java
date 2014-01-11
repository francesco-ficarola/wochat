package it.uniroma1.dis.wsngroup.wochat.logging;

import org.apache.log4j.Logger;

public class LogMessage {
	private static Logger logger = Logger.getLogger(LogMessage.class);
		
	public static void logMsg(String msg) {
		logger.info(msg);
	}
}
