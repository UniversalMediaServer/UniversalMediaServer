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
package net.pms.configuration;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.util.Locale;
import net.pms.TestHelper;
import net.pms.util.FileUtil;
import net.pms.util.Languages;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UmsConfigurationTest {

	private UmsConfiguration configuration;
	@BeforeEach
	public void setUp() throws ConfigurationException, InterruptedException {
		TestHelper.SetLoggingOff();

		// Create default configuration instance
		configuration = new UmsConfiguration(false);
	}

	/**
	 * Test Logging Configuration defaults
	 */
	@Test
	public void testLoggingConfigurationDefaults() {
		// Test defaults and valid values where applicable
		assertFalse(configuration.getGUILogSearchCaseSensitive(), "LogSearchCaseSensitiveDefault");
		assertFalse(configuration.getGUILogSearchMultiLine(), "LogSearchMultiLineDefault");
		assertFalse(configuration.getGUILogSearchRegEx(), "LogSearchRegEx");
		assertTrue(FileUtil.isValidFileName(configuration.getDefaultLogFileName()), "LogFileNameValid");
		assertEquals(configuration.getDefaultLogFileName(), "debug.log", "LogFileNameDefault");
		File file = new File(configuration.getDefaultLogFileFolder());
		assertTrue(file.isDirectory(), "DefaultLogFileFolder");
		file = new File(configuration.getDefaultLogFilePath());
		assertTrue(configuration.getDefaultLogFilePath().endsWith("debug.log"), "DefaultLogFilePath");
		assertFalse(configuration.getLoggingBuffered(), "LoggingBufferedDefault");
		assertEquals(configuration.getLoggingFilterConsole(), Level.INFO, "LoggingFilterConsoleDefault");
		assertEquals(configuration.getLoggingFilterLogsTab(), Level.INFO, "LoggingFilterLogsTabDefault");
		assertEquals(configuration.getLoggingLogsTabLinebuffer(), 1000, "LoggingLogsTabLinebufferDefault");
		assertTrue(configuration.getLoggingLogsTabLinebuffer() >= UmsConfiguration.getLoggingLogsTabLinebufferMin() &&
				configuration.getLoggingLogsTabLinebuffer() <= UmsConfiguration.getLoggingLogsTabLinebufferMax(),
				"LoggingLogsTabLinebufferLegal");
		assertEquals(configuration.getLoggingSyslogFacility(), "USER", "LoggingSyslogFacilityDefault");
		assertEquals(configuration.getLoggingSyslogHost(), "", "LoggingSyslogHostDefault");
		assertEquals(configuration.getLoggingSyslogPort(), 514, "LoggingSyslogPortDefault");
		assertFalse(configuration.getLoggingUseSyslog(), "LoggingUseSyslogDefault");
		assertEquals(configuration.getLanguageLocale(), Languages.toLocale(Locale.getDefault()), "getLanguageLocaleDefault");
		assertEquals(configuration.getLanguageTag(), Languages.toLanguageTag(Locale.getDefault()), "getLanguageTagDefault");
		configuration.getConfiguration().setProperty("language", "");
		assertEquals(configuration.getLanguageLocale(), Languages.toLocale(Locale.getDefault()), "getLanguageLocaleDefault");
		assertEquals(configuration.getLanguageTag(), Languages.toLanguageTag(Locale.getDefault()), "getLanguageTagDefault");
		configuration.getConfiguration().setProperty("language", "en-GB");
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("en-GB"), "getLanguageLocaleBritishEnglish");
		assertEquals(configuration.getLanguageTag(), "en-GB", "getLanguageTagBritishEnglish");
		configuration.getConfiguration().setProperty("language", "en");
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("en-US"), "getLanguageLocaleEnglish");
		assertEquals(configuration.getLanguageTag(), "en-US", "getLanguageTagEnglish");
		configuration.getConfiguration().setProperty("language", "zh");
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"), "getLanguageLocaleChinese");
		assertEquals(configuration.getLanguageTag(), "zh-Hant", "getLanguageTagChinese");
		configuration.setLanguage(Locale.UK);
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("en-GB"), "setLanguageUK");
		configuration.setLanguage(Locale.SIMPLIFIED_CHINESE);
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hans"), "setLanguageSimplifiedChinese");
		configuration.setLanguage(Locale.TRADITIONAL_CHINESE);
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"), "setLanguageTraditionalChinese");
		Locale locale = null;
		configuration.setLanguage(locale);
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"), "setLanguageNull");
		String code = null;
		configuration.setLanguage(code);
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"), "setLanguageNull");
		configuration.setLanguage("");
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("zh-Hant"), "setLanguageEmpty");
		configuration.setLanguage("en");
		assertEquals(configuration.getLanguageLocale(), Locale.forLanguageTag("en-US"), "setLanguageEnglish");
	}

	@Test
	public void testDefaults() {
		assertNull(configuration.getLanguageRawString(), "getLanguageRawStringDefault");
		configuration.setLanguage((Locale) null);
		assertEquals(configuration.getLanguageRawString(), "", "setLanguage(null)SetsBlankString");
	}
}
