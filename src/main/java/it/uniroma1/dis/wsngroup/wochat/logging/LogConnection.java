package it.uniroma1.dis.wsngroup.wochat.logging;

import org.apache.log4j.Logger;

public class LogConnection {
private static Logger logger = Logger.getLogger(LogConnection.class);
	
	public static void logConnection(String msg) {
		logger.info(msg);
	}
}
