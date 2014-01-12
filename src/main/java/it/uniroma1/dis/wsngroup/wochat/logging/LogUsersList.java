package it.uniroma1.dis.wsngroup.wochat.logging;

import org.apache.log4j.Logger;

public class LogUsersList {
	private static Logger logger = Logger.getLogger(LogUsersList.class);
	
	public static void logUsersList(String msg) {
		logger.info(msg);
	}
}
