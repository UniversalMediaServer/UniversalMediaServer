/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2010  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.PropertiesUtil;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Simple loader for logback configuration files.
 * 
 * @author thomas@innot.de
 */
public class LoggingConfig {
	private static String filepath = null;
	private static HashMap<String, String> logFilePaths = new HashMap<>(); // key: appender name, value: log file path
	private static LoggerContext loggerContext = null;
	private static SyslogAppender syslog;
	private static enum ActionType {START, STOP, NONE};

	/**
	 * Gets the full path of a successfully loaded Logback configuration file.
	 * 
	 * If the configuration file could not be loaded the string
	 * <code>internal defaults</code> is returned.
	 * 
	 * @return pathname or <code>null</code>
	 */
	public static String getConfigFilePath() {
		if (filepath != null) {
			return filepath;
		} else {
			return "internal defaults";
		}
	}

	private static File getFile(String[] fileList) {
		for (String fileName : fileList) {
			fileName = fileName.trim();
			if (fileName.length() > 0) {
				if (fileName.matches("\\[PROFILE_DIR\\].*")) {
					String s = PMS.getConfiguration().getProfileDirectory().replace("\\","/");
					fileName = fileName.replaceAll("\\[PROFILE_DIR\\]", s);
				}
				File file = new File(fileName.trim());
				if (file.exists() && file.canRead()) {
					return file;
				}
			}
		}
		return null;
	}

	
	/**
	 * Loads the (optional) Logback configuration file.
	 * 
	 * It loads the file defined in the <code>project.logback</code> property from the current
	 * directory and (re-)initializes Logback with this file. If running
	 * headless (<code>System.Property("console")</code> set), then the
	 * alternative config file defined in <code>project.logback.headless</code> is tried first.
	 * 
	 * If no config file can be found in the CWD, then nothing is loaded and
	 * Logback will use the logback.xml file on the classpath as a default. If
	 * this doesn't exist then a basic console appender is used as fallback.
	 * 
	 * <strong>Note:</strong> Any error messages generated while parsing the
	 * config file are dumped only to <code>stdout</code>.
	 */
	public static boolean loadFile() {
		// Note: Do not use any logging method in this method!
		// Any status output needs to go to the console.

		boolean headless = !(System.getProperty("console") == null);
		boolean loaded = false;
		File file = null;

		ILoggerFactory ilf = LoggerFactory.getILoggerFactory();
		if (!(ilf instanceof LoggerContext)) {
			// Not using LogBack.
			// Can't configure the logger, so just exit
			return loaded;
		}
		loggerContext = (LoggerContext) ilf;
		
		if (headless) {
			file = getFile(PropertiesUtil.getProjectProperties().get("project.logback.headless").split(","));
		}

		if (file == null) {
			file = getFile(PropertiesUtil.getProjectProperties().get("project.logback").split(","));			
		}

		if (file == null) {
			// Unpredictable: Any logback.xml found in the Classpath is loaded, if that fails defaulting to BasicConfigurator
			// See http://logback.qos.ch/xref/ch/qos/logback/classic/BasicConfigurator.html
			return loaded;
		}

		// Now get logback to actually use the config file

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(loggerContext);
			// the context was probably already configured by
			// default configuration rules
			loggerContext.reset();
			configurator.doConfigure(file);

			// Save the filepath after loading the file
			filepath = file.getAbsolutePath();
			loaded = true;
		} catch (JoranException je) {}

		for (Logger LOGGER : loggerContext.getLoggerList()) {
			Iterator<Appender<ILoggingEvent>> it = LOGGER.iteratorForAppenders();

			while (it.hasNext()) {
				Appender<ILoggingEvent> ap = it.next();

				if (ap instanceof FileAppender) {
					FileAppender<ILoggingEvent> fa = (FileAppender<ILoggingEvent>) ap;
					logFilePaths.put(fa.getName(), fa.getFile());
				}
			}
		}
				
		StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
		return loaded;
	}

	/**
	* Adds/modifies/removes a syslog appender based on PmsConfiguration and disables/enables file appenders 
	* for easier access to syslog logging for users without in-depth knowledge of LogBack.
	* Stops file appenders if syslog is started and vice versa.<P>
	* 
	* Must be called after {@link #loadFile()} and after UMS configuration is loaded.
	*/
	public static void setSyslog() {
		org.slf4j.Logger logger = LoggerFactory.getLogger(PMS.class);
		boolean found = false;
		ActionType action = ActionType.NONE;
		PmsConfiguration configuration = PMS.getConfiguration();

		if (loggerContext == null) {
			logger.error("Unknown loggerContext, aborting syslog configuration. Make sure that loadFile() has been called first");
			return;
		}

		if (configuration.getLoggingUseSyslog()) {
			// Check for valid parameters
			if (configuration.getLoggingSyslogHost().isEmpty()) {
				logger.error("Empty syslog hostname, syslog configuration aborted");
				return;
			}
			try {
				InetAddress.getByName(configuration.getLoggingSyslogHost());
			} catch (UnknownHostException e) {
				logger.error(String.format("Unknown syslog hostname '%s', syslog configuration aborted",configuration.getLoggingSyslogHost()));
				return;			
			}
			if (configuration.getLoggingSyslogPort() < 1 && configuration.getLoggingSyslogPort() > 65535) {
				logger.error(String.format("Invalid syslog port %d, using default", configuration.getLoggingSyslogPort()));
				configuration.setLoggingSyslogPortDefault();
			}
			if (!configuration.getLoggingSyslogFacility().toLowerCase().matches(
				"auth|authpriv|daemon|cron|ftp|lpr|kern|mail|news|syslog|user|uucp|local0|local1|local2|local3|local4|local5|local6|local7")) {
				logger.error(String.format("Invalid syslog facility %s, using default", configuration.getLoggingSyslogFacility()));
				configuration.setLoggingSyslogFacilityDefault();			
			}
		}

		Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		if (rootLogger == null) {
			logger.error("Couldn't find root logger, aborting");
			return;
		}

		if (configuration.getLoggingUseSyslog() && syslog == null) {
			// UMS haven't already configured one, but there can be another syslog appender there from custom LogBack configuration
			//Check that syslog isn't already configured
			Iterator<Appender<ILoggingEvent>> it = rootLogger.iteratorForAppenders();
			while (it.hasNext()) {
				Appender<ILoggingEvent> appender = it.next();
				if (appender instanceof SyslogAppender) {
					found = true;
					break;
				}
			}
			//Only create a new appender if there's no syslog appender there already
			if (found) {
				logger.warn("A syslog appender is already configured, aborting syslog configuration");
				return;
			}
			SyslogAppender syslog = new SyslogAppender();
			syslog.setContext(loggerContext);
			syslog.setSuffixPattern("UMS [%thread] %msg");
			syslog.setName("UMS syslog");
			rootLogger.addAppender(syslog);
			action = ActionType.START;
		} else if (!configuration.getLoggingUseSyslog() && syslog != null && syslog.isStarted()) {
			action = ActionType.STOP;
		}
		if (syslog != null && (action == ActionType.START || action == ActionType.NONE)) {
			syslog.setSyslogHost(configuration.getLoggingSyslogHost());
			syslog.setPort(configuration.getLoggingSyslogPort());
			syslog.setFacility(configuration.getLoggingSyslogFacility().toUpperCase());			
		}
		if (action == ActionType.START || action == ActionType.STOP) {
			
			Iterator<Appender<ILoggingEvent>> it = rootLogger.iteratorForAppenders();
			
			while (it.hasNext()) {
				Appender<ILoggingEvent> appender = it.next();
				
				if (appender instanceof FileAppender) {
					FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) appender;
					if (action == ActionType.START && fileAppender.isStarted()) {
						fileAppender.stop();
					} else if (action == ActionType.STOP && !fileAppender.isStarted()) {
						fileAppender.start();
					}
				} else if (appender == syslog) {
					if (action == ActionType.START) {
						syslog.start();
					} else {
						syslog.stop();
					}
				}
			}
		}
	}
	
	/**
	* Adds/modifies/removes a syslog appender based on PmsConfiguration and disables/enables file appenders 
	* for easier access to syslog logging for users without in-depth knowledge of LogBack.
	* Stops file appenders if syslog is started and vice versa.<P>
	* 
	* Must be called after {@link #loadFile()} and after UMS configuration is loaded.
	*/
	public static void setBuffered(boolean buffered) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(PMS.class);
		
		if (loggerContext == null) {
			logger.error("Unknown loggerContext, aborting buffered logging. Make sure that loadFile() has been called first");
			return;
		}

		for (Logger LOGGER : loggerContext.getLoggerList()) {
			Iterator<Appender<ILoggingEvent>> it = LOGGER.iteratorForAppenders();
			
			while (it.hasNext()) {
				Appender<ILoggingEvent> appender = it.next();
				
				if (appender instanceof OutputStreamAppender && !(appender instanceof ConsoleAppender<?>)) {
					// Appender has Encoder property
					Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
					if (encoder instanceof LayoutWrappingEncoder) {
						// Encoder has ImmideateFlush property
						((LayoutWrappingEncoder<ILoggingEvent>) encoder).setImmediateFlush(!buffered);
					}
				}
			}
		}
	}
	
	public static HashMap<String, String> getLogFilePaths() {
		return logFilePaths;
	}
}
