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
package net.pms.util;

import static org.junit.Assert.*;
import java.util.Locale;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class LanguagesTest {

	@Before
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	@Test
	public void testIsValid() {
		// Test the string version
		assertFalse("enIsInvalid", Languages.isValid("en"));
		assertTrue("en-USIsValid", Languages.isValid("en-US"));
		assertTrue("en-GBIsValid", Languages.isValid("en-GB"));
		assertTrue("zh-HansIsValid", Languages.isValid("zh-Hans"));
		assertFalse("cmn-HantIsInvalid", Languages.isValid("cmn-Hant"));
		assertFalse("EmptyIsInvalid", Languages.isValid(""));
		String code = null;
		assertFalse("NullIsInvalid", Languages.isValid(code));

		// Test the locale version
		assertTrue("enIsValid", Languages.isValid(Languages.localeFromTag("en")));
		assertTrue("en-USIsValid", Languages.isValid(Languages.localeFromTag("en-US")));
		assertTrue("en-GBIsValid", Languages.isValid(Languages.localeFromTag("en-GB")));
		assertTrue("zh-HansIsValid", Languages.isValid(Languages.localeFromTag("zh-Hans")));
		assertTrue("cmn-HantIsValid", Languages.isValid(Languages.localeFromTag("cmn-Hant")));
		assertTrue("zh-CHIsValid", Languages.isValid(Languages.localeFromTag("zh-CH")));
		assertTrue("zh-TWIsValid", Languages.isValid(Languages.localeFromTag("zh-TH")));
		assertTrue("pt-PTIsValid", Languages.isValid(Languages.localeFromTag("pt-PT")));
		assertTrue("no-NOIsValid", Languages.isValid(Languages.localeFromTag("no-NO")));
		assertFalse("EmptyIsInvalid", Languages.isValid(Languages.localeFromTag("")));
		Locale locale = null;
		assertFalse("NullIsInvalid", Languages.isValid(locale));
	}

	@Test
	public void testToLanguageCode() {
		// Test the string version
		assertNull("EnIsNull", Languages.toLanguageCode("En"));
		assertEquals("EN-USIsen-US", Languages.toLanguageCode("EN-US"), "en-US");
		assertEquals("En-gBIsen-GB", Languages.toLanguageCode("En-gB"), "en-GB");
		assertEquals("zh-hansIszh-Hans", Languages.toLanguageCode("zh-hans"), "zh-Hans");
		assertNull("cmn-HantIsNull", Languages.toLanguageCode("cmn-HantIs"));
		assertNull("EmptyIsNull", Languages.toLanguageCode(""));
		String code = null;
		assertNull("NullIsNull", Languages.toLanguageCode(code));

		// Test the locale version
		assertEquals("enIsen-US", Languages.toLanguageCode(Languages.localeFromTag("en")), "en-US");
		assertEquals("en-USIsen-US", Languages.toLanguageCode(Languages.localeFromTag("en-US")), "en-US");
		assertEquals("en-GBIsen-GB", Languages.toLanguageCode(Languages.localeFromTag("en-GB")), "en-GB");
		assertEquals("zh-HansIszh-Hans", Languages.toLanguageCode(Languages.localeFromTag("zh-Hans")), "zh-Hans");
		assertEquals("cmn-HantIszh-Hant", Languages.toLanguageCode(Languages.localeFromTag("cmn-Hant")), "zh-Hant");
		assertEquals("zh-CNIszh-Hans", Languages.toLanguageCode(Languages.localeFromTag("zh-CN")), "zh-Hans");
		assertEquals("zh-SGIszh-Hans", Languages.toLanguageCode(Languages.localeFromTag("zh-SG")), "zh-Hans");
		assertEquals("zh-TWIszh-Hant", Languages.toLanguageCode(Languages.localeFromTag("zh-TW")), "zh-Hant");
		assertEquals("pt-PTIspt", Languages.toLanguageCode(Languages.localeFromTag("pt-PT")), "pt");
		assertEquals("no-NOIsno", Languages.toLanguageCode(Languages.localeFromTag("no-NO")), "no");
		assertNull("EmptyIsNull", Languages.toLanguageCode(Languages.localeFromTag("")));
		Locale locale = null;
		assertNull("NullIsNull", Languages.toLanguageCode(locale));
	}

	@Test
	public void testToLocale() {
		assertEquals("enIsen-US", Languages.toLocale(Languages.localeFromTag("en")), Languages.localeFromTag("en-US"));
		assertEquals("en-USIsen-US", Languages.toLocale(Languages.localeFromTag("en-US")), Languages.localeFromTag("en-US"));
		assertEquals("en-GBIsen-GB", Languages.toLocale(Languages.localeFromTag("en-GB")), Languages.localeFromTag("en-GB"));
		assertEquals("zh-HansIszh-Hans", Languages.toLocale(Languages.localeFromTag("zh-Hans")), Languages.localeFromTag("zh-Hans"));
		assertEquals("cmn-HantIszh-Hant", Languages.toLocale(Languages.localeFromTag("cmn-Hant")), Languages.localeFromTag("zh-Hant"));
		assertEquals("zh-CNIszh-Hans", Languages.toLocale(Languages.localeFromTag("zh-CN")), Languages.localeFromTag("zh-Hans"));
		assertEquals("zh-SGIszh-Hans", Languages.toLocale(Languages.localeFromTag("zh-SG")), Languages.localeFromTag("zh-Hans"));
		assertEquals("zh-TWIszh-Hant", Languages.toLocale(Languages.localeFromTag("zh-TW")), Languages.localeFromTag("zh-Hant"));
		assertEquals("pt-PTIspt", Languages.toLocale(Languages.localeFromTag("pt-PT")), Languages.localeFromTag("pt"));
		assertEquals("no-NOIsno", Languages.toLocale(Languages.localeFromTag("no-NO")), Languages.localeFromTag("no"));
		assertNull("EmptyIsNull", Languages.toLocale(Languages.localeFromTag("")));
		Locale locale = null;
		assertNull("NullIsNull", Languages.toLocale(locale));
	}
}

