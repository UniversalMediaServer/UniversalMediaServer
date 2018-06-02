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
package net.pms.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Locale;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

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
		assertTrue("enIsValid", Languages.isValid(Locale.forLanguageTag("en")));
		assertTrue("en-USIsValid", Languages.isValid(Locale.forLanguageTag("en-US")));
		assertTrue("en-GBIsValid", Languages.isValid(Locale.forLanguageTag("en-GB")));
		assertTrue("zh-HansIsValid", Languages.isValid(Locale.forLanguageTag("zh-Hans")));
		assertTrue("cmn-HantIsValid", Languages.isValid(Locale.forLanguageTag("cmn-Hant")));
		assertTrue("zh-CHIsValid", Languages.isValid(Locale.forLanguageTag("zh-CH")));
		assertTrue("zh-TWIsValid", Languages.isValid(Locale.forLanguageTag("zh-TH")));
		assertTrue("pt-PTIsValid", Languages.isValid(Locale.forLanguageTag("pt-PT")));
		assertTrue("no-NOIsValid", Languages.isValid(Locale.forLanguageTag("no-NO")));
		assertFalse("EmptyIsInvalid", Languages.isValid(Locale.forLanguageTag("")));
		Locale locale = null;
		assertFalse("NullIsInvalid", Languages.isValid(locale));
	}

	@Test
	public void testIsCompatible() {
		assertTrue("enIsValid", Languages.isCompatible("en"));
		assertTrue("en-USIsValid", Languages.isCompatible("en-US"));
		assertTrue("no-NOIsValid", Languages.isCompatible("no-NO"));
		assertTrue("sv-FIIsValid", Languages.isCompatible("sv-FI"));
		assertTrue("en-GBIsValid", Languages.isCompatible("en-GB"));
		assertTrue("cmn-HantIsValid", Languages.isCompatible("cmn-Hant"));
		assertTrue("cmn-SGIsValid", Languages.isCompatible("cmn-SG"));
		assertTrue("csIsValid", Languages.isCompatible("cs"));
		assertFalse("czIsInvalid", Languages.isCompatible("cz"));
		assertFalse("fooIsInvalid", Languages.isCompatible("foo"));
		assertFalse("EmptyIsInvalid", Languages.isCompatible(""));
		assertFalse("NullIsInvalid", Languages.isCompatible(null));
	}

	@Test
	public void testToLanguageCode() {
		// Test the string version
		assertEquals("EnIsen-US", Languages.toLanguageTag("En"), "en-US");
		assertEquals("EN-USIsen-US", Languages.toLanguageTag("EN-US"), "en-US");
		assertEquals("En-gBIsen-GB", Languages.toLanguageTag("En-gB"), "en-GB");
		assertEquals("zh-hansIszh-Hans", Languages.toLanguageTag("zh-hans"), "zh-Hans");
		assertEquals("cmn-HantIszh-Hant", Languages.toLanguageTag("cmn-HantIs"), "zh-Hant");
		assertNull("EmptyIsNull", Languages.toLanguageTag(""));
		String code = null;
		assertNull("NullIsNull", Languages.toLanguageTag(code));

		// Test the locale version
		assertEquals("enIsen-US", Languages.toLanguageTag(Locale.forLanguageTag("en")), "en-US");
		assertEquals("en-USIsen-US", Languages.toLanguageTag(Locale.forLanguageTag("en-US")), "en-US");
		assertEquals("en-GBIsen-GB", Languages.toLanguageTag(Locale.forLanguageTag("en-GB")), "en-GB");
		assertEquals("zh-HansIszh-Hans", Languages.toLanguageTag(Locale.forLanguageTag("zh-Hans")), "zh-Hans");
		assertEquals("cmn-HantIszh-Hant", Languages.toLanguageTag(Locale.forLanguageTag("cmn-Hant")), "zh-Hant");
		assertEquals("zh-CNIszh-Hans", Languages.toLanguageTag(Locale.forLanguageTag("zh-CN")), "zh-Hans");
		assertEquals("zh-SGIszh-Hans", Languages.toLanguageTag(Locale.forLanguageTag("zh-SG")), "zh-Hans");
		assertEquals("zh-TWIszh-Hant", Languages.toLanguageTag(Locale.forLanguageTag("zh-TW")), "zh-Hant");
		assertEquals("pt-PTIspt", Languages.toLanguageTag(Locale.forLanguageTag("pt-PT")), "pt");
		assertEquals("no-NOIsno", Languages.toLanguageTag(Locale.forLanguageTag("no-NO")), "no");
		assertNull("EmptyIsNull", Languages.toLanguageTag(Locale.forLanguageTag("")));
		Locale locale = null;
		assertNull("NullIsNull", Languages.toLanguageTag(locale));
	}

	@Test
	public void testToLocale() {
		assertEquals("enIsen-US", Languages.toLocale(Locale.forLanguageTag("en")), Locale.forLanguageTag("en-US"));
		assertEquals("en-USIsen-US", Languages.toLocale(Locale.forLanguageTag("en-US")), Locale.forLanguageTag("en-US"));
		assertEquals("en-GBIsen-GB", Languages.toLocale(Locale.forLanguageTag("en-GB")), Locale.forLanguageTag("en-GB"));
		assertEquals("zh-HansIszh-Hans", Languages.toLocale(Locale.forLanguageTag("zh-Hans")), Locale.forLanguageTag("zh-Hans"));
		assertEquals("cmn-HantIszh-Hant", Languages.toLocale(Locale.forLanguageTag("cmn-Hant")), Locale.forLanguageTag("zh-Hant"));
		assertEquals("zh-CNIszh-Hans", Languages.toLocale(Locale.forLanguageTag("zh-CN")), Locale.forLanguageTag("zh-Hans"));
		assertEquals("zh-SGIszh-Hans", Languages.toLocale(Locale.forLanguageTag("zh-SG")), Locale.forLanguageTag("zh-Hans"));
		assertEquals("zh-TWIszh-Hant", Languages.toLocale(Locale.forLanguageTag("zh-TW")), Locale.forLanguageTag("zh-Hant"));
		assertEquals("pt-PTIspt", Languages.toLocale(Locale.forLanguageTag("pt-PT")), Locale.forLanguageTag("pt"));
		assertEquals("no-NOIsno", Languages.toLocale(Locale.forLanguageTag("no-NO")), Locale.forLanguageTag("no"));
		assertNull("EmptyIsNull", Languages.toLocale(Locale.forLanguageTag("")));
		Locale locale = null;
		assertNull("NullIsNull", Languages.toLocale(locale));
	}
}

