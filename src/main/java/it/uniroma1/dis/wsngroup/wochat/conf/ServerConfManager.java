package it.uniroma1.dis.wsngroup.wochat.conf;

import it.uniroma1.dis.wsngroup.wochat.utils.Constants;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ServerConfManager {
	private Logger logger = Logger.getLogger(getClass()); 
	private static ServerConfManager instance = new ServerConfManager();
	private Properties props;
	
	private ServerConfManager() {
		init();
	}
	
	private void init() {
		logger.debug("Loading the properties file...");
		try{
			
			FileInputStream fis = new FileInputStream(Constants.PATH_CONF_FILE);
			props = new Properties();
			props.load(fis);
			
		} catch(Throwable th) {
			logger.error(th.getMessage(), th);
		}
	}
	
	public static  ServerConfManager getInstance() {
		return instance;
	}
	
	public String getProperty(String propertyName) {
		return props.getProperty(propertyName);
	}
}