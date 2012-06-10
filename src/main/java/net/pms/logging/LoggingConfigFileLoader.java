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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import net.pms.util.PropertiesUtil;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Simple loader for logback configuration files.
 * 
 * @author thomas@innot.de
 */
public class LoggingConfigFileLoader {
	private static String filepath = null;
	private static HashMap<String, String> logFilePaths = new HashMap<String, String>(); // key=appender name, value, log file path

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
	public static void load() {
		// Note: Do not use any logging method in this method!
		// Any logging would cause PMS.get() to be called from the
		// FrameAppender, which in turn would start the PMS instance, which
		// would cause further log messages. This will lead to either lost
		// log messages or Deadlocks!
		// Any status output needs to go to the console.

		boolean headless = !(System.getProperty("console") == null);

		File file = null;

		if (headless) {
			file = new File(PropertiesUtil.getProjectProperties().get("project.logback.headless"));
		}

		if (file == null || !file.exists()) {
			file = new File(PropertiesUtil.getProjectProperties().get("project.logback"));
		}

		if (!file.exists()) {
			// No problem, the internal logback.xml is used.
			return;
		}

		// Now get logback to actually use the config file

		ILoggerFactory ilf = LoggerFactory.getILoggerFactory();
		if (!(ilf instanceof LoggerContext)) {
			// Not using LogBack.
			// Can't configure the logger, so just exit
			return;
		}

		LoggerContext lc = (LoggerContext) ilf;

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(lc);
			// the context was probably already configured by
			// default configuration rules
			lc.reset();
			configurator.doConfigure(file);

			// Save the filepath after loading the file
			filepath = file.getAbsolutePath();

		} catch (JoranException je) {
			// StatusPrinter will handle this
			je.printStackTrace();
		}

		for (Logger LOGGER : lc.getLoggerList()) {
			Iterator<Appender<ILoggingEvent>> it = LOGGER.iteratorForAppenders();
			while (it.hasNext()) {
				Appender<ILoggingEvent> ap = it.next();
				if (ap instanceof FileAppender) {
					FileAppender<ILoggingEvent> fa = (FileAppender<ILoggingEvent>) ap;
					logFilePaths.put(fa.getName(), fa.getFile());
				}
			}
		}

		StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
	}

	public static HashMap<String, String> getLogFilePaths() {
		return logFilePaths;
	}
}
