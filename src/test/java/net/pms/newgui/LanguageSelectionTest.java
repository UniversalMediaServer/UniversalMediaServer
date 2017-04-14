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
package net.pms.newgui;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Locale;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;


public class LanguageSelectionTest {

	@Before
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	@Test(expected=IllegalArgumentException.class)
	public void LanguageSelectionConstructorThrowTest() {
		new LanguageSelection(null, null, false);
	}

	@Test
	public void LanguageSelectionClassTest() {
		LanguageSelection languageSelection;
		languageSelection = new LanguageSelection(null, Locale.US, false);
		assertFalse("isAbortedIsFalseByDefault", languageSelection.isAborted());

	}

}
