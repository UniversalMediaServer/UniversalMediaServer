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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.Iterators;
import net.pms.util.PropertiesUtil;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Simple loader for logback configuration files.
 *
 * @author thomas@innot.de, expanded by Nadahar
 */
public class LoggingConfig {
	private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LoggingConfig.class);
	private static Object filepathLock = new Object();
	private static String filepath = null;
	private static Object logFilePathsLock = new Object();
	private static HashMap<String, String> logFilePaths = new HashMap<>(); // key: appender name, value: log file path
	private static LoggerContext loggerContext = null;
	private static Logger rootLogger;
	private static SyslogAppender syslog;
	private static boolean syslogDisabled = false;
	private static enum ActionType {START, STOP, NONE};
	private static Level consoleLevel = null;
	private static Level tracesLevel = null;
	private static LinkedList<Appender<ILoggingEvent>> syslogDetachedAppenders = new LinkedList<>();

	/**
	 * Gets the full path of a successfully loaded Logback configuration file.
	 *
	 * If the configuration file could not be loaded the string
	 * <code>internal defaults</code> is returned.
	 *
	 * @return pathname or <code>null</code>
	 */
	public static String getConfigFilePath() {
		synchronized (filepathLock) {
			if (filepath != null) {
				return filepath;
			} else {
				return "internal defaults";
			}
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

	private static boolean setContextAndRoot() {
		ILoggerFactory ilf = LoggerFactory.getILoggerFactory();
		if (!(ilf instanceof LoggerContext)) {
			// Not using LogBack.
			// Can't configure the logger, so just exit
			LOGGER.debug("Not using LogBack, aborting LogBack configuration");
			return false;
		}
		loggerContext = (LoggerContext) ilf;
		rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		if (rootLogger == null) {
			// Shouldn't be possible
			LOGGER.error("Couldn't find root logger, aborting LogBack configuration");
			return false;
		}
		return true;
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
	public static synchronized void loadFile() {
		File file = null;

		if (!setContextAndRoot()) {
			return;
		}

		if (PMS.isHeadless()) {
			file = getFile(PropertiesUtil.getProjectProperties().get("project.logback.headless").split(","));
		}

		if (file == null) {
			file = getFile(PropertiesUtil.getProjectProperties().get("project.logback").split(","));
		}

		if (file == null) {
			// Unpredictable: Any logback.xml found in the Classpath is loaded, if that fails defaulting to BasicConfigurator
			// See http://logback.qos.ch/xref/ch/qos/logback/classic/BasicConfigurator.html
			LOGGER.warn("Could not load LogBack configuration file from " + (PMS.isHeadless() ?
					PropertiesUtil.getProjectProperties().get("project.logback.headless") + ", " : "") +
					PropertiesUtil.getProjectProperties().get("project.logback"));
			LOGGER.warn("Falling back to somewhat unpredictable defaults, probably only logging to console.");
			return;
		}

		// Now get logback to actually use the config file

		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		try {
			// the context was probably already configured by
			// default configuration rules
			loggerContext.reset();
			loggerContext.getStatusManager().clear();
			// Do not log between loggerContext.reset() and CacheLogger.initContext()
			configurator.doConfigure(file);
			if (CacheLogger.isActive()) {
				CacheLogger.initContext();
			}
			// Save the file path after loading the file
			synchronized (filepathLock) {
				filepath = file.getAbsolutePath();
				LOGGER.debug("LogBack started with configuration file: {}", filepath);
			}
		} catch (JoranException je) {
			try {
				System.err.println("LogBack configuration failed: " + je.getLocalizedMessage());
				System.err.println("Trying to create \"emergency\" configuration");
				// Try to create "emergency" appenders for some logging if configuration fails
				if (PMS.isHeadless()) {
					ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
					PatternLayoutEncoder pe = new PatternLayoutEncoder();
					pe.setPattern("%-5level %d{HH:mm:ss.SSS} [%thread] %logger %msg%n");
					pe.setContext(loggerContext);
					pe.start();
					ca.setEncoder(pe);
					ca.setContext(loggerContext);
					ca.setName("Emergency Console");
					ca.start();
					loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(ca);
				} else {
					FrameAppender<ILoggingEvent> fa = new FrameAppender<>();
					PatternLayoutEncoder pe = new PatternLayoutEncoder();
					pe.setPattern("%-5level %d{HH:mm:ss.SSS} [%thread] %logger %msg%n");
					pe.setContext(loggerContext);
					pe.start();
					fa.setEncoder(pe);
					fa.setContext(loggerContext);
					fa.setName("Emergency Frame");
					fa.start();
					loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(fa);
				}
				System.err.println("LogBack \"emergency\" configuration applied.");
			} catch (Exception e) {
				System.err.println("LogBack \"emergency\" configuration failed with: " + e);
			}
			if (CacheLogger.isActive()) {
				CacheLogger.initContext();
			}
			LOGGER.error("Logback configuration failed with: {}", je.getLocalizedMessage());
			StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
			return;
		}

		// Build the iterator
		Iterators<Appender<ILoggingEvent>> iterators = new Iterators<>();
		// Add CacheLogger appenders if CacheLogger is active
		if (CacheLogger.isActive()) {
			iterators.addIterator(CacheLogger.iteratorForAppenders());
		}
		// syslogDetachedAppenders can't be populated at this stage, so no reason to iterate it.
		// Iterate loggerContext even if CacheLogger is active as there could still be
		// non-root appenders there.
		for (Logger LOGGER : loggerContext.getLoggerList()) {
			iterators.addIterator(LOGGER.iteratorForAppenders());
		}
		// Iterate

		Iterator<Appender<ILoggingEvent>> it = iterators.combinedIterator();
		synchronized (logFilePathsLock) {
			while (it.hasNext()) {
				Appender<ILoggingEvent> appender = it.next();

				if (appender instanceof FileAppender) {
					FileAppender<ILoggingEvent> fa = (FileAppender<ILoggingEvent>) appender;
					logFilePaths.put(fa.getName(), fa.getFile());
				} else if (appender instanceof SyslogAppender) {
					syslogDisabled = true;
				}
			}
		}

		// Set filters for console and traces
		setConfigurableFilters(true, true);

		StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
		return;
	}

	private static synchronized void setConfigurableFilters(boolean setConsole, boolean setTraces) {
		PmsConfiguration configuration = PMS.getConfiguration();
		if (loggerContext == null) {
			LOGGER.error("Unknown loggerContext, aborting buffered logging. Make sure that loadFile() has been called first.");
			return;
		}

		if (setConsole) {
			Level level = configuration.getLoggingFilterConsole();
			if (consoleLevel == null || consoleLevel != level) {
				consoleLevel = level;
			} else {
				setConsole = false;
			}
		}
		if (setTraces) {
			Level level = configuration.getLoggingFilterLogsTab();
			if (tracesLevel == null || tracesLevel != level) {
				tracesLevel = level;
			} else {
				setTraces = false;
			}
		}

		if (setConsole || setTraces) {

			// Since Console- and FrameAppender will exist at root level and won't be detached by syslog,
			// there's no reason to build an iterator as this should suffice.
			Iterator<Appender<ILoggingEvent>> it = CacheLogger.isActive() ? CacheLogger.iteratorForAppenders() : rootLogger.iteratorForAppenders();
			while (it.hasNext()) {
				Appender<ILoggingEvent> appender = it.next();

				if (setConsole && appender instanceof ConsoleAppender) {
					ConsoleAppender<ILoggingEvent> ca = (ConsoleAppender<ILoggingEvent>) appender;
					boolean createNew = true;
					if (!ca.getCopyOfAttachedFiltersList().isEmpty()) {
						for (Filter<ILoggingEvent> filter : ca.getCopyOfAttachedFiltersList()) {
							if (filter instanceof ThresholdFilter) {
								createNew = false;
								((ThresholdFilter) filter).setLevel(consoleLevel.levelStr);
								((ThresholdFilter) filter).start();
							}
						}
					}
					if (createNew) {
						ThresholdFilter consoleFilter = new ThresholdFilter();
						ca.addFilter(consoleFilter);
						consoleFilter.setLevel(consoleLevel.levelStr);
						consoleFilter.setContext(loggerContext);
						consoleFilter.start();
					}
				}
				if (setTraces && appender instanceof FrameAppender) {
					FrameAppender<ILoggingEvent> fa = (FrameAppender<ILoggingEvent>) appender;
					boolean createNew = true;
					if (!fa.getCopyOfAttachedFiltersList().isEmpty()) {
						for (Filter<ILoggingEvent> filter : fa.getCopyOfAttachedFiltersList()) {
							if (filter instanceof ThresholdFilter) {
								createNew = false;
								((ThresholdFilter) filter).setLevel(tracesLevel.levelStr);
								((ThresholdFilter) filter).start();
							}
						}
					}
					if (createNew) {
						ThresholdFilter tracesFilter = new ThresholdFilter();
						fa.addFilter(tracesFilter);
						tracesFilter.setLevel(tracesLevel.levelStr);
						tracesFilter.setContext(loggerContext);
						tracesFilter.start();
					}
				}
			}
		}
	}

	/**
	 * Finds and configures the console appender with a filter with
	 * the logging level from configuration provided that it's not already
	 * configured in logback.xml or logback.headless.xml.
	 */
	public static void setConsoleFilter() {
		setConfigurableFilters(true, false);
	}

	/**
	 * Finds and configures the frame appender with a filter with
	 * the logging level from configuration provided that it's not already
	 * configured in logback.xml or logback.headless.xml.
	 */
	public static void setTracesFilter() {
		setConfigurableFilters(false, true);
	}

	/**
	 * Shows whether or not UMS' automated syslog function is disabled because
	 * one or more syslog appenders are manually configured in LogBack
	 * configuration.
	 * @return the status.
	 */
	public static synchronized boolean isSyslogDisabled() {
		return syslogDisabled;
	}

	/**
	* Adds/modifies/removes a syslog appender based on PmsConfiguration and
	* disables/enables file appenders for easier access to syslog logging for
	* users without in-depth knowledge of LogBack. Stops file appenders if
	* syslog is started and vice versa.<P>
	*
	* Must be called after {@link #loadFile()} and after UMS configuration is
	* loaded.
	*/
	public static synchronized void setSyslog() {
		ActionType action = ActionType.NONE;
		PmsConfiguration configuration = PMS.getConfiguration();

		if (loggerContext == null) {
			LOGGER.error("Unknown loggerContext, aborting syslog configuration. Make sure that loadFile() has been called first.");
			return;
		} else if (syslogDisabled) {
			//Only create a new syslog appender if there's no syslog appender configured already
			LOGGER.warn("A syslog appender is already configured, aborting syslog configuration");
			return;
		}

		if (configuration.getLoggingUseSyslog()) {
			// Check for valid parameters
			if (configuration.getLoggingSyslogHost().isEmpty()) {
				LOGGER.error("Empty syslog hostname, syslog configuration aborted");
				return;
			}
			try {
				InetAddress.getByName(configuration.getLoggingSyslogHost());
			} catch (UnknownHostException e) {
				LOGGER.error("Unknown syslog hostname {}, syslog configuration aborted",configuration.getLoggingSyslogHost());
				return;
			}
			if (configuration.getLoggingSyslogPort() < 1 && configuration.getLoggingSyslogPort() > 65535) {
				LOGGER.error("Invalid syslog port {}, using default", configuration.getLoggingSyslogPort());
				configuration.setLoggingSyslogPortDefault();
			}
			if (!configuration.getLoggingSyslogFacility().toLowerCase().matches(
				"auth|authpriv|daemon|cron|ftp|lpr|kern|mail|news|syslog|user|uucp|local0|local1|local2|local3|local4|local5|local6|local7")) {
				LOGGER.error("Invalid syslog facility \"{}\", using default", configuration.getLoggingSyslogFacility());
				configuration.setLoggingSyslogFacilityDefault();
			}
		}

		if (configuration.getLoggingUseSyslog() && syslog == null) {
			syslog = new SyslogAppender();
			syslog.setContext(loggerContext);
			syslog.setSuffixPattern("UMS [%thread] %msg");
			syslog.setName("UMS syslog");
			syslog.setCharset(StandardCharsets.UTF_8);
			action = ActionType.START;
		} else if (!configuration.getLoggingUseSyslog() && syslog != null) {
			action = ActionType.STOP;
		}
		if (syslog != null && (action == ActionType.START || action == ActionType.NONE)) {
			syslog.setSyslogHost(configuration.getLoggingSyslogHost());
			syslog.setPort(configuration.getLoggingSyslogPort());
			syslog.setFacility(configuration.getLoggingSyslogFacility().toUpperCase());
			syslog.start();
		}
		if (action == ActionType.START || action == ActionType.STOP) {

			Iterator<Appender<ILoggingEvent>> it = CacheLogger.isActive() ? CacheLogger.iteratorForAppenders() : rootLogger.iteratorForAppenders();
			while (it.hasNext()) {
				Appender<ILoggingEvent> appender = it.next();

				if (action == ActionType.START && appender instanceof FileAppender) {
					if (CacheLogger.isActive()) {
						CacheLogger.removeAppender(appender);
					} else {
						rootLogger.detachAppender(appender);
					}
					syslogDetachedAppenders.add(appender);
					// If syslog is disabled later and this appender reactivated, append to the file instead of truncate
					((FileAppender<ILoggingEvent>) appender).setAppend(true);
				} else if (action == ActionType.STOP && appender == syslog) {
					if (CacheLogger.isActive()) {
						CacheLogger.removeAppender(syslog);
					} else {
						rootLogger.detachAppender(syslog);
					}
					syslog.stop();
					syslog = null;
				}
			}

			if (action == ActionType.START) {
				if (CacheLogger.isActive()) {
					CacheLogger.addAppender(syslog);
				} else {
					rootLogger.addAppender(syslog);
				}
				LOGGER.info("Syslog logging started, file logging disabled");
			} else {
				it = syslogDetachedAppenders.iterator();
				while (it.hasNext()) {
					Appender<ILoggingEvent> appender = it.next();
					if (CacheLogger.isActive()) {
						CacheLogger.addAppender(appender);
					} else {
						rootLogger.addAppender(appender);
					}
				}
				syslogDetachedAppenders.clear();
				LOGGER.info("Syslog logging stopped, file logging enabled");
			}
		}
	}

	/**
	* Turns ImmediateFlush off or on (!buffered) for all encoders descending from
	* LayoutWrappingEncoder for appenders descending from OutputStreamAppender
	* except ConsoleAppender. The purpose is to turn on or off buffering/flushing
	* for all file logging appenders.
	*
	* Must be called after {@link #loadFile()}.
	*
	* @param buffered whether or not file logging should be buffered or flush immediately
	*/
	public static synchronized void setBuffered(boolean buffered) {
		if (loggerContext == null) {
			LOGGER.error("Unknown loggerContext, aborting buffered logging. Make sure that loadFile() has been called first.");
			return;
		}

		// Build iterator
		Iterators<Appender<ILoggingEvent>> iterators = new Iterators<>();
		// Add CacheLogger or rootLogger appenders depending on whether CacheLogger is active.
		if (CacheLogger.isActive()) {
			iterators.addIterator(CacheLogger.iteratorForAppenders());
		} else {
			iterators.addIterator(rootLogger.iteratorForAppenders());
		}
		// If syslog is active there probably are detached appenders there as well
		if (!syslogDetachedAppenders.isEmpty()) {
			iterators.addList(syslogDetachedAppenders);
		}

		// Iterate
		Iterator<Appender<ILoggingEvent>> it = iterators.combinedIterator();
		while (it.hasNext()) {
			Appender<ILoggingEvent> appender = it.next();

			if (appender instanceof OutputStreamAppender && !(appender instanceof ConsoleAppender<?>)) {
				// Appender has Encoder property
				Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
				if (encoder instanceof LayoutWrappingEncoder) {
					// Encoder has ImmediateFlush property
					((LayoutWrappingEncoder<ILoggingEvent>) encoder).setImmediateFlush(!buffered);
				}
			}
		}
		LOGGER.info("Buffered logging turned {}", buffered ? "ON" : "OFF");
	}

	/**
	* Sets the root logger level.
	*
	* Must be called after {@link #loadFile()}.
	*
	* @param level the new root logger level.
	*/
	public static synchronized void setRootLevel(Level level) {
		if (loggerContext == null && !setContextAndRoot()) {
			LOGGER.error("Unknown loggerContext, aborting setRootLevel");
			return;
		}
		rootLogger.setLevel(level);
	}

	/**
	* Gets the root logger level.
	*
	* Must be called after {@link #loadFile()}.
	*/
	public static synchronized Level getRootLevel() {
		if (loggerContext == null && !setContextAndRoot()) {
			LOGGER.error("Unknown loggerContext, aborting getRootLevel.");
			return Level.OFF;
		}
		return rootLogger.getLevel();
	}

	public static synchronized void forceVerboseFileEncoder() {
		final String timeStampFormat = "yyyy-MM-dd HH:mm:ss.SSS";
		if (loggerContext == null) {
			LOGGER.error("Unknown loggerContext, aborting buffered logging. Make sure that loadFile() has been called first.");
			return;
		}

		// Build iterator
		Iterators<Appender<ILoggingEvent>> iterators = new Iterators<>();
		// Add CacheLogger or rootLogger appenders depending on whether CacheLogger is active.
		if (CacheLogger.isActive()) {
			iterators.addIterator(CacheLogger.iteratorForAppenders());
		} else {
			iterators.addIterator(rootLogger.iteratorForAppenders());
		}
		// If syslog is active there probably are detached appenders there as well
		if (!syslogDetachedAppenders.isEmpty()) {
			iterators.addList(syslogDetachedAppenders);
		}

		// Iterate
		Iterator<Appender<ILoggingEvent>> it = iterators.combinedIterator();
		while (it.hasNext()) {
			Appender<ILoggingEvent> appender = it.next();

			if (appender instanceof OutputStreamAppender && !(appender instanceof ConsoleAppender<?>)) {
				// Appender has Encoder property
				Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
				if (encoder instanceof PatternLayoutEncoder) {
					// Encoder has pattern
					PatternLayoutEncoder patternEncoder = (PatternLayoutEncoder) encoder;
					String logPattern = patternEncoder.getPattern();

					// Set timestamp format
					Pattern pattern = Pattern.compile("%((date|d)(\\{([^\\}]*)\\})?)(?=\\s)");
					Matcher matcher = pattern.matcher(logPattern);
					if (matcher.find()) {
						boolean replace = true;
						if (matcher.group(4) != null && matcher.group(4).equals(timeStampFormat)) {
							replace = false;
						}
						if (replace) {
							logPattern = logPattern.replaceFirst(pattern.pattern(), "%d{" + timeStampFormat + "}");
						}
					} else {
						if (logPattern.startsWith("%-5level")) {
							logPattern = logPattern.substring(0,8) + " %d{" + timeStampFormat + "}" + logPattern.substring(8);
						} else {
							logPattern = "d%{" + timeStampFormat + "} " + logPattern;
						}
					}

					// Make sure %logger is included
					pattern = Pattern.compile("((%logger|%lo|%c)(\\{\\d+\\})?)(?=\\s)");
					matcher = pattern.matcher(logPattern);
					if (matcher.find()) {
						boolean replace = true;
						if (matcher.group().equals("%logger")) {
							replace = false;
						}
						if (replace) {
							logPattern = logPattern.replaceFirst(pattern.pattern(), "%logger");
						}
					} else {
						if (logPattern.contains("%msg")) {
							logPattern = logPattern.substring(0,logPattern.indexOf("%msg")) + "%logger " + logPattern.substring(logPattern.indexOf("%msg"));
						} else {
							logPattern = "%logger " + logPattern;
						}
					}

					// Activate changes
					patternEncoder.setPattern(logPattern);
					patternEncoder.start();
				}
			}
		}
		LOGGER.info("Verbose file logging pattern enforced");
	}

	public static HashMap<String, String> getLogFilePaths() {
		synchronized (logFilePathsLock) {
			return logFilePaths;
		}
	}
}
