/*
 * Universal Media Server, for streaming any medias to DLNA
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

import static org.junit.Assert.*;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import net.pms.util.FileUtil;

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
		assertTrue("LogFileNameValid", FileUtil.isValidFileName(configuration.getLogFileName()));
		assertEquals("LogFileNameDefault", configuration.getLogFileName(), "debug.log");
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
	}
}
