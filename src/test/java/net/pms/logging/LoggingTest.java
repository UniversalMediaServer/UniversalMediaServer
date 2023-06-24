/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.filter.Filter;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.util.FileUtil;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Test net.pms.logging package
 */
public class LoggingTest {

	@BeforeEach
	public void setUp() {
		// Silence all log messages from the UMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	/**
	 * Test CacheAppender and its utility class CacheLogger
	 */
	@Test
	public void testCacheLogger() {
		final String testMessage = "Test logging event";

		// Set up logging framework for testing
		assertTrue(LoggerFactory.getILoggerFactory() instanceof LoggerContext, "LogBack");
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
		Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
		TestAppender<ILoggingEvent> testAppender = new TestAppender<>();
		rootLogger.addAppender(testAppender);
		testAppender.setContext(context);
		testAppender.start();
		rootLogger.setLevel(Level.ERROR);

		// Test basic functionality
		assertFalse(CacheLogger.isActive(), "CacheLoggerInactive");
		CacheLogger.startCaching();
		rootLogger.error(testMessage);
		assertTrue(CacheLogger.isActive(), "CacheLoggerActive");
		CacheLogger.stopAndFlush();
		assertEquals(testAppender.getLastEvent().getMessage(), testMessage, "LoggedMessage");
		assertFalse(CacheLogger.isActive(), "CacheLoggerInactive");
		rootLogger.setLevel(Level.OFF);

		// Test other CacheLogger functions
		CacheLogger.startCaching();
		assertTrue(findAppender(CacheLogger.iteratorForAppenders(), testAppender), "AppenderIterator");
		CacheLogger.removeAppender(testAppender);
		assertFalse(findAppender(CacheLogger.iteratorForAppenders(), testAppender), "AppenderRemoval");
		TestAppender<ILoggingEvent> testAppender2 = new TestAppender<>();
		CacheLogger.addAppender(testAppender2);
		assertTrue(findAppender(CacheLogger.iteratorForAppenders(), testAppender2), "AppenderAdding");
		CacheLogger.stopAndFlush();
		assertTrue(findAppender(rootLogger.iteratorForAppenders(), testAppender2), "AppenderTransferred");
		assertFalse(findAppender(rootLogger.iteratorForAppenders(), testAppender), "RemovedAppenderNotTransferred");

		// Cleanup
		rootLogger.detachAppender(testAppender2);
	}

	/**
	 * Test
	 * @throws ConfigurationException
	 * @throws InterruptedException
	 *
	 */
	@Test
	public void testDebugLogPropertyDefiner() throws ConfigurationException, InterruptedException {

		// Set up PMS configuration
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration());
		DebugLogPropertyDefiner propertyDefiner = new DebugLogPropertyDefiner();

		// Test logFilePath
		propertyDefiner.setKey("logFilePath");
		File file = new File(propertyDefiner.getPropertyValue());
		assertTrue(file.isDirectory(), "logFilePathIsDirectory");
		assertFalse(file.isFile(), "logFilePathIsNotFile");

		// Test rootLevel
		propertyDefiner.setKey("rootLevel");
		assertNotNull(Level.toLevel(propertyDefiner.getPropertyValue(), null), "ValidLevel");

		// Test logFileName
		propertyDefiner.setKey("logFileName");
		assertTrue(FileUtil.isValidFileName(propertyDefiner.getPropertyValue()), "ValidLogFileName");
	}

	@Test
	public void testLoggingConfig() throws ConfigurationException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InterruptedException {

		// Set up a test (default) configuration
		PMS.get();
		UmsConfiguration configuration = new UmsConfiguration(false);
		PMS.setConfiguration(configuration);

		// Load logback configuration
		LoggingConfig.loadFile();
		// Silence logger
		LoggingConfig.setRootLevel(Level.OFF);

		// Get access to logger
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

		/* During UMS build a valid configuration should be accessible at least under "external resources"
		 * and thus testing for a valid configuration is considered OK to be able to do the other tests.
		 * "internal defaults" is returned if a valid configuration can't be found.
		 */

		// Test for a valid configuration
		File file = new File(LoggingConfig.getConfigFilePath());
		assertTrue(file.isFile(), "LoggingConfigIsFile");
		assertFalse(file.isDirectory(), "LoggingConfigIsFile");

		// Test getLogFilePaths() and LoggingConfigFileLoader.getLogFilePaths()
		Map<String, String> logFilePaths = LoggingConfig.getLogFilePaths();
		@SuppressWarnings("deprecation")
		Map<String, String> compLogFilePaths = LoggingConfigFileLoader.getLogFilePaths();
		Iterator<Appender<ILoggingEvent>> iterator = rootLogger.iteratorForAppenders();
		while (iterator.hasNext()) {
			Appender<ILoggingEvent> appender = iterator.next();
			if (appender instanceof FileAppender) {
				FileAppender<ILoggingEvent> fa = (FileAppender<ILoggingEvent>) appender;
				assertTrue(logFilePaths.containsKey(fa.getName()), "LogFilePathsContainsKey");
				assertEquals(logFilePaths.get(fa.getName()), fa.getFile(), "LogFilePathsHasPath");
				if (fa.getName().equals("default.log")) {
					assertTrue(compLogFilePaths.containsKey("debug.log"), "CompatibleLogFilePathsContainsKey");
					assertEquals(compLogFilePaths.get("debug.log"), fa.getFile(), "CompatibleLogFilePathsHasPath");
				} else {
					assertTrue(compLogFilePaths.containsKey(fa.getName()), "CompatibleLogFilePathsContainsKey");
					assertEquals(compLogFilePaths.get(fa.getName()), fa.getFile(), "CompatibleLogFilePathsHasPath");
				}
			}
		}

		// Reset LogBack configuration and create a fake one to not rely on the existing configuration file
		context.reset();

		TestFileAppender<ILoggingEvent> testDefaultAppender = new TestFileAppender<>();
		testDefaultAppender.setName("default.log");
		testDefaultAppender.setContext(context);
		PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
		layoutEncoder.setPattern("%-5level %d{HH:mm:ss.SSS} [%thread] %msg%n");
		layoutEncoder.setContext(context);
		testDefaultAppender.setEncoder(layoutEncoder);
		rootLogger.addAppender(testDefaultAppender);

		TestFileAppender<ILoggingEvent> testGenericAppender = new TestFileAppender<>();
		testGenericAppender.setName("SomeOtherFileAppender");
		testGenericAppender.setContext(context);
		layoutEncoder = new PatternLayoutEncoder();
		layoutEncoder.setPattern("%-5level %d %msg%n");
		layoutEncoder.setContext(context);
		testGenericAppender.setEncoder(layoutEncoder);
		rootLogger.addAppender(testGenericAppender);

		TestAppender<ILoggingEvent> testNonFileAppender = new TestAppender<>();
		testNonFileAppender.setName("SomeNonFileAppender");
		testNonFileAppender.setContext(context);
		rootLogger.addAppender(testNonFileAppender);

		// Test setBuffered()
		LoggingConfig.setBuffered(true);
		iterator = rootLogger.iteratorForAppenders();
		while (iterator.hasNext()) {
			Appender<ILoggingEvent> appender = iterator.next();
			if (appender instanceof OutputStreamAppender && !(appender instanceof ConsoleAppender<?>)) {
				// Appender has ImmediateFlush property
				assertFalse(((OutputStreamAppender<ILoggingEvent>) appender).isImmediateFlush(), "LogFileIsBuffered");
			}
		}
		LoggingConfig.setBuffered(false);
		iterator = rootLogger.iteratorForAppenders();
		while (iterator.hasNext()) {
			Appender<ILoggingEvent> appender = iterator.next();
			if (appender instanceof OutputStreamAppender && !(appender instanceof ConsoleAppender<?>)) {
				assertTrue(((OutputStreamAppender<ILoggingEvent>) appender).isImmediateFlush(), "LogFileIsNotBuffered");
				// Appender has ImmediateFlush property
			}
		}

		// Test getRootLevel()
		assertEquals(LoggingConfig.getRootLevel(), rootLogger.getLevel(), "GetRootLevel");

		// Test setRootLevel()
		LoggingConfig.setRootLevel(Level.ALL);
		assertEquals(LoggingConfig.getRootLevel(), Level.ALL, "SetRootLevel");
		LoggingConfig.setRootLevel(Level.INFO);
		assertEquals(LoggingConfig.getRootLevel(), Level.INFO, "SetRootLevel");
		LoggingConfig.setRootLevel(Level.OFF);

		// Test setConsoleFilter()
		configuration.setLoggingFilterConsole(Level.WARN);
		ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
		consoleAppender.setContext(context);
		PatternLayoutEncoder patternEncoder = new PatternLayoutEncoder();
		patternEncoder.setPattern("%msg%n");
		patternEncoder.setContext(context);
		patternEncoder.start();
		consoleAppender.setEncoder(patternEncoder);
		consoleAppender.start();
		rootLogger.addAppender(consoleAppender);
		LoggingConfig.setConsoleFilter();
		List<Filter<ILoggingEvent>> filterList = consoleAppender.getCopyOfAttachedFiltersList();
		assertEquals(filterList.size(), 1, "NumberOfConsoleFilters");
		assertTrue(filterList.get(0) instanceof ThresholdFilter, "ConsoleFilterIsThresholdFilter");
		ThresholdFilter thresholdFilter = (ThresholdFilter) filterList.get(0);
		Field field = thresholdFilter.getClass().getDeclaredField("level");
		field.setAccessible(true);
		assertEquals(field.get(thresholdFilter), Level.WARN, "ConsoleFilterLevel");
		configuration.setLoggingFilterConsole(Level.TRACE);
		LoggingConfig.setConsoleFilter();
		filterList = consoleAppender.getCopyOfAttachedFiltersList();
		assertEquals(filterList.size(), 1, "NumberOfConsoleFilters");
		assertTrue(filterList.get(0) instanceof ThresholdFilter, "ConsoleFilterIsThresholdFilter");
		thresholdFilter = (ThresholdFilter) filterList.get(0);
		field = thresholdFilter.getClass().getDeclaredField("level");
		field.setAccessible(true);
		assertEquals(field.get(thresholdFilter), Level.TRACE, "ConsoleFilterLevel");
		rootLogger.detachAppender(consoleAppender);

		// Test setTracesFilter()
		configuration.setLoggingFilterLogsTab(Level.WARN);
		GuiManagerAppender<ILoggingEvent> guiAppender = new GuiManagerAppender<>();
		guiAppender.setContext(context);
		patternEncoder = new PatternLayoutEncoder();
		patternEncoder.setPattern("%msg%n");
		patternEncoder.setContext(context);
		patternEncoder.start();
		guiAppender.setEncoder(patternEncoder);
		guiAppender.start();
		rootLogger.addAppender(guiAppender);
		LoggingConfig.setTracesFilter();
		filterList = guiAppender.getCopyOfAttachedFiltersList();
		assertEquals(filterList.size(), 1, "NumberOfTracesFilters");
		assertTrue(filterList.get(0) instanceof ThresholdFilter, "TracesFilterIsThresholdFilter");
		thresholdFilter = (ThresholdFilter) filterList.get(0);
		field = thresholdFilter.getClass().getDeclaredField("level");
		field.setAccessible(true);
		assertEquals(field.get(thresholdFilter), Level.WARN, "TracesFilterLevel");
		configuration.setLoggingFilterLogsTab(Level.TRACE);
		LoggingConfig.setTracesFilter();
		filterList = guiAppender.getCopyOfAttachedFiltersList();
		assertEquals(filterList.size(), 1, "NumberOfTracesFilters");
		assertTrue(filterList.get(0) instanceof ThresholdFilter, "TracesFilterIsThresholdFilter");
		thresholdFilter = (ThresholdFilter) filterList.get(0);
		field = thresholdFilter.getClass().getDeclaredField("level");
		field.setAccessible(true);
		assertEquals(field.get(thresholdFilter), Level.TRACE, "TracesFilterLevel");
		rootLogger.detachAppender(guiAppender);

		// Test isSyslogDisabled()
		if (syslogAppenderFound(rootLogger.iteratorForAppenders())) {
			assertTrue(LoggingConfig.isSyslogDisabled(), "SyslogDisabledByConfiguration");
		} else {
			assertFalse(LoggingConfig.isSyslogDisabled(), "SyslogNotDisabledByConfiguration");
		}

		// Test setSyslog() if possible
		if (!syslogAppenderFound(rootLogger.iteratorForAppenders())) {
			configuration.setLoggingSyslogHost("localhost");
			configuration.setLoggingUseSyslog(true);
			LoggingConfig.setSyslog();
			assertTrue(syslogAppenderFound(rootLogger.iteratorForAppenders()), "SyslogEnabled");
			configuration.setLoggingUseSyslog(false);
			LoggingConfig.setSyslog();
			assertFalse(syslogAppenderFound(rootLogger.iteratorForAppenders()), "SyslogDisabled");
		}

		// Test forceVerboseFileEncoder() given that LogBack configuration
		// contains at least one file appender with PatternLayoutEncoder
		LoggingConfig.forceVerboseFileEncoder();
		iterator = rootLogger.iteratorForAppenders();
		while (iterator.hasNext()) {
			Appender<ILoggingEvent> appender = iterator.next();
			if (appender instanceof OutputStreamAppender && !(appender instanceof ConsoleAppender<?>)) {
				// Appender has Encoder property
				Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
				if (encoder instanceof PatternLayoutEncoder patternLayoutEncoder) {
					// Encoder has pattern
					assertTrue(patternLayoutEncoder.getPattern().matches(".*%(d|date)\\{yyyy-MM-dd HH:mm:ss.SSS\\}.*"), "AppenderPatternHasCorrectTimestamp");
					assertTrue(patternLayoutEncoder.getPattern().matches(".*%logger.*"), "AppenderPatternHasLogger");
				}
			}
		}

		context.reset();
	}

	private static class TestAppender<E> extends AppenderBase<E> {

		private final Object lastEventLock = new Object();
		private E lastEvent = null;

		public E getLastEvent() {
			synchronized (lastEventLock) {
				return lastEvent;
			}
		}
		@Override
		protected void append(E eventObject) {
			synchronized (lastEventLock) {
				lastEvent = eventObject;
			}
		}
	}

	private static class TestFileAppender<E> extends FileAppender<E> {

		@Override
		protected void append(E eventObject) {
		}
	}

	private static boolean findAppender(Iterator<Appender<ILoggingEvent>> iterator, Appender<ILoggingEvent> appender) {
		boolean found = false;
		while (iterator.hasNext()) {
			Appender<ILoggingEvent> a = iterator.next();
			if (a == appender) {
				found = true;
			}
		}
		return found;
	}

	private static boolean syslogAppenderFound(Iterator<Appender<ILoggingEvent>> iterator) {
		while (iterator.hasNext()) {
			Appender<ILoggingEvent> appender = iterator.next();
			if (appender instanceof SyslogAppender) {
				return true;
			}
		}

		return false;
	}

}
