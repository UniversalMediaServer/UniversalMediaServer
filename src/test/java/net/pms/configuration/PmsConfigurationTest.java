/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import java.util.Locale;
import net.pms.util.FileUtil;
import net.pms.util.Languages;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class PmsConfigurationTest {

	private PmsConfiguration configuration;
	@Before
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);

		// Create default configuration instance
		configuration = new PmsConfiguration(false);
	}

	/**
	 * Test Logging Configuration defaults
	 */
	@Test
	public void testLoggingConfigurationDefaults() {
		// Test defaults and valid values where applicable
		assertFalse("LogSearchCaseSensitiveDefault", configuration.getGUILogSearchCaseSensitive());
		assertFalse("LogSearchMultiLineDefault", configuration.getGUILogSearchMultiLine());
		assertFalse("LogSearchRegEx", configuration.getGUILogSearchRegEx());
		assertTrue("LogFileNameValid", FileUtil.isValidFileName(configuration.getDefaultLogFileName()));
		assertEquals("LogFileNameDefault", configuration.getDefaultLogFileName(), "debug.log");
		File file = new File(configuration.getDefaultLogFileFolder());
		assertTrue("DefaultLogFileFolder", file.isDirectory());
		file = new File(configuration.getDefaultLogFilePath());
		assertTrue("DefaultLogFilePath", configuration.getDefaultLogFilePath().endsWith("debug.log"));
		assertFalse("LoggingBufferedDefault", configuration.getLoggingBuffered());
		assertEquals("LoggingFilterConsoleDefault", configuration.getLoggingFilterConsole(), Level.INFO);
		assertEquals("LoggingFilterLogsTabDefault", configuration.getLoggingFilterLogsTab(), Level.INFO);
		assertEquals("LoggingLogsTabLinebufferDefault", configuration.getLoggingLogsTabLinebuffer(), 1000);
		assertTrue("LoggingLogsTabLinebufferLegal",
			configuration.getLoggingLogsTabLinebuffer() >= PmsConfiguration.LOGGING_LOGS_TAB_LINEBUFFER_MIN &&
			configuration.getLoggingLogsTabLinebuffer() <= PmsConfiguration.LOGGING_LOGS_TAB_LINEBUFFER_MAX);
		assertEquals("LoggingSyslogFacilityDefault", configuration.getLoggingSyslogFacility(), "USER");
		assertEquals("LoggingSyslogHostDefault", configuration.getLoggingSyslogHost(), "");
		assertEquals("LoggingSyslogPortDefault", configuration.getLoggingSyslogPort(), 514);
		assertFalse("LoggingUseSyslogDefault", configuration.getLoggingUseSyslog());
		assertEquals("getLanguageLocaleDefault", configuration.getLanguageLocale(), Languages.toLocale(Locale.getDefault()));
		assertEquals("getLanguageTagDefault", configuration.getLanguageTag(), Languages.toLanguageTag(Locale.getDefault()) );
		configuration.getConfiguration().setProperty("language", "");
		assertEquals("getLanguageLocaleDefault", configuration.getLanguageLocale(), Languages.toLocale(Locale.getDefault()));
		assertEquals("getLanguageTagDefault", configuration.getLanguageTag(), Languages.toLanguageTag(Locale.getDefault()) );
		configuration.getConfiguration().setProperty("language", "en-GB");
		assertEquals("getLanguageLocaleBritishEnglish", configuration.getLanguageLocale(), Locale.forLanguageTag("en-GB"));
		assertEquals("getLanguageTagBritishEnglish", configuration.getLanguageTag(), "en-GB");
		configuration.getConfiguration().setProperty("language", "en");
		assertEquals("getLanguageLocaleEnglish", configuration.getLanguageLocale(), Locale.forLanguageTag("en-US"));
		assertEquals("getLanguageTagEnglish", configuration.getLanguageTag(), "en-US");
		configuration.getConfiguration().setProperty("language", "zh");
		assertEquals("getLanguageLocaleChinese", configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"));
		assertEquals("getLanguageTagChinese", configuration.getLanguageTag(), "zh-Hant");
		configuration.setLanguage(Locale.UK);
		assertEquals("setLanguageUK", configuration.getLanguageLocale(), Locale.forLanguageTag("en-GB"));
		configuration.setLanguage(Locale.SIMPLIFIED_CHINESE);
		assertEquals("setLanguageSimplifiedChinese", configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hans"));
		configuration.setLanguage(Locale.TRADITIONAL_CHINESE);
		assertEquals("setLanguageTraditionalChinese", configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"));
		Locale locale = null;
		configuration.setLanguage(locale);
		assertEquals("setLanguageNull", configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"));
		String code = null;
		configuration.setLanguage(code);
		assertEquals("setLanguageNull", configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"));
		configuration.setLanguage("");
		assertEquals("setLanguageEmpty", configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"));
		configuration.setLanguage("en");
		assertEquals("setLanguageEnglish", configuration.getLanguageLocale(), Locale.forLanguageTag("en-US"));
	}

	@Test
	public void testDefaults() {
		assertNull("getLanguageRawStringDefault", configuration.getLanguageRawString());
		configuration.setLanguage((Locale) null);
		assertEquals("setLanguage(null)SetsBlankString", configuration.getLanguageRawString(), "");
	}
}
