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
package net.pms.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Locale;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class LanguagesTest {

	@BeforeEach
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	@Test
	public void testIsValid() {
		// Test the string version
		assertFalse(Languages.isValid("en"), "enIsInvalid");
		assertTrue(Languages.isValid("en-US"), "en-USIsValid");
		assertTrue(Languages.isValid("en-GB"), "en-GBIsValid");
		assertTrue(Languages.isValid("zh-Hans"), "zh-HansIsValid");
		assertFalse(Languages.isValid("cmn-Hant"), "cmn-HantIsInvalid");
		assertFalse(Languages.isValid(""), "EmptyIsInvalid");
		String code = null;
		assertFalse(Languages.isValid(code), "NullIsInvalid");

		// Test the locale version
		assertTrue(Languages.isValid(Locale.forLanguageTag("en")), "enIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("en-US")), "en-USIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("en-GB")), "en-GBIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("zh-Hans")), "zh-HansIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("cmn-Hant")), "cmn-HantIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("zh-CH")), "zh-CHIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("zh-TH")), "zh-TWIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("pt-PT")), "pt-PTIsValid");
		assertTrue(Languages.isValid(Locale.forLanguageTag("no-NO")), "no-NOIsValid");
		assertFalse(Languages.isValid(Locale.forLanguageTag("")), "EmptyIsInvalid");
		Locale locale = null;
		assertFalse(Languages.isValid(locale), "NullIsInvalid");
	}

	@Test
	public void testIsCompatible() {
		assertTrue(Languages.isCompatible("en"), "enIsValid");
		assertTrue(Languages.isCompatible("en-US"), "en-USIsValid");
		assertTrue(Languages.isCompatible("no-NO"), "no-NOIsValid");
		assertTrue(Languages.isCompatible("sv-FI"), "sv-FIIsValid");
		assertTrue(Languages.isCompatible("en-GB"), "en-GBIsValid");
		assertTrue(Languages.isCompatible("cmn-Hant"), "cmn-HantIsValid");
		assertTrue(Languages.isCompatible("cmn-SG"), "cmn-SGIsValid");
		assertTrue(Languages.isCompatible("cs"), "csIsValid");
		assertFalse(Languages.isCompatible("cz"), "czIsInvalid");
		assertFalse(Languages.isCompatible("foo"), "fooIsInvalid");
		assertFalse(Languages.isCompatible(""), "EmptyIsInvalid");
		assertFalse(Languages.isCompatible(null), "NullIsInvalid");
	}

	@Test
	public void testToLanguageCode() {
		// Test the string version
		assertEquals(Languages.toLanguageTag("En"), "en-US", "EnIsen-US");
		assertEquals(Languages.toLanguageTag("EN-US"), "en-US", "EN-USIsen-US");
		assertEquals(Languages.toLanguageTag("En-gB"), "en-GB", "En-gBIsen-GB");
		assertEquals(Languages.toLanguageTag("zh-hans"), "zh-Hans", "zh-hansIszh-Hans");
		assertEquals(Languages.toLanguageTag("cmn-HantIs"), "zh-Hant", "cmn-HantIszh-Hant");
		assertNull(Languages.toLanguageTag(""), "EmptyIsNull");
		String code = null;
		assertNull(Languages.toLanguageTag(code), "NullIsNull");

		// Test the locale version
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("en")), "en-US", "enIsen-US");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("en-US")), "en-US", "en-USIsen-US");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("en-GB")), "en-GB", "en-GBIsen-GB");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("zh-Hans")), "zh-Hans", "zh-HansIszh-Hans");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("cmn-Hant")), "zh-Hant", "cmn-HantIszh-Hant");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("zh-CN")), "zh-Hans", "zh-CNIszh-Hans");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("zh-SG")), "zh-Hans", "zh-SGIszh-Hans");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("zh-TW")), "zh-Hant", "zh-TWIszh-Hant");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("pt-PT")), "pt", "pt-PTIspt");
		assertEquals(Languages.toLanguageTag(Locale.forLanguageTag("no-NO")), "no", "no-NOIsno");
		assertNull(Languages.toLanguageTag(Locale.forLanguageTag("")), "EmptyIsNull");
		Locale locale = null;
		assertNull(Languages.toLanguageTag(locale), "NullIsNull");
	}

	@Test
	public void testToLocale() {
		assertEquals(Languages.toLocale(Locale.forLanguageTag("en")), Locale.forLanguageTag("en-US"), "enIsen-US");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("en-US")), Locale.forLanguageTag("en-US"), "en-USIsen-US");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("en-GB")), Locale.forLanguageTag("en-GB"), "en-GBIsen-GB");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("zh-Hans")), Locale.forLanguageTag("zh-Hans"), "zh-HansIszh-Hans");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("cmn-Hant")), Locale.forLanguageTag("zh-Hant"), "cmn-HantIszh-Hant");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("zh-CN")), Locale.forLanguageTag("zh-Hans"), "zh-CNIszh-Hans");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("zh-SG")), Locale.forLanguageTag("zh-Hans"), "zh-SGIszh-Hans");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("zh-TW")), Locale.forLanguageTag("zh-Hant"), "zh-TWIszh-Hant");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("pt-PT")), Locale.forLanguageTag("pt"), "pt-PTIspt");
		assertEquals(Languages.toLocale(Locale.forLanguageTag("no-NO")), Locale.forLanguageTag("no"), "no-NOIsno");
		assertNull(Languages.toLocale(Locale.forLanguageTag("")), "EmptyIsNull");
		Locale locale = null;
		assertNull(Languages.toLocale(locale), "NullIsNull");
	}
}

