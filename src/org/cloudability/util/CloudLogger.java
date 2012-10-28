/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.util;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class CloudLogger {

	private final static Logger systemLogger;

	static {
		PropertyConfigurator.configure("config/log4j.properties");
		systemLogger = Logger.getLogger("CloudAbility");
	}

	/**
	 * Get the system logger.
	 * @return The system logger.
	 */
	public static Logger getSystemLogger() {
		return systemLogger;
	}

	/**
	 * Creates a logger for job with a given job ID.
	 * @param id The job ID.
	 * @return The job logger.
	 */
	public static Logger getJobLogger(int id) {
		String loggerName = String.format("Job%d", id);
		String appenderName = String.format("%sLog", loggerName);
		String logFile = String.format("log/jobs/%s.log", loggerName);

		String appenderFullName = String.format("log4j.appender.%s", appenderName);

		Properties properties = new Properties();
		properties.setProperty("log4j.logger." + loggerName, "DEBUG, " + appenderName);
		properties.setProperty(appenderFullName, "org.apache.log4j.FileAppender");
		properties.setProperty(appenderFullName + ".File", logFile);
		properties.setProperty(appenderFullName + ".MaxFileSize", "20MB");
		properties.setProperty(appenderFullName + ".layout", "org.apache.log4j.PatternLayout");
		properties.setProperty(appenderFullName + ".layout.ConversionPattern", "%-4r [%t] %-5p %c %x - %m%n");
		properties.setProperty(appenderFullName + ".Threshold", "DEBUG");

		PropertyConfigurator.configure(properties);

		return Logger.getLogger(loggerName);
	}

}
