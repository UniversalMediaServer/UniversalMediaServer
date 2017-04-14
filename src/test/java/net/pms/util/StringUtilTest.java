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
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;


public class StringUtilTest {

	@Before
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	@Test
	public void fillStringTest() {
		char[] chars = { 'a', 'æ' };
		assertEquals("fillStringCharArray", StringUtil.fillString(chars, 4), "aæaæaæaæ");
		assertEquals("fillStringCharSpace", StringUtil.fillString(' ', 10), "          ");
		assertEquals("fillStringCodePoint", StringUtil.fillString(1333 , 3), "\u0535\u0535\u0535");
		assertEquals("fillStringUnicodeString", StringUtil.fillString("\u0648\u0AA7\u184A", 2), "\u0648\u0AA7\u184A\u0648\u0AA7\u184A");
		assertEquals("fillStringEmptyString", StringUtil.fillString("", 100), "");
		assertEquals("FillStringZero", StringUtil.fillString("foo",	0), "");
	}

	@Test(expected=IllegalArgumentException.class)
	public void stripHTMLTest() {
		assertEquals("stripHTMLBasicBody", StringUtil.stripHTML("<html><sometag></sometag><someothertag/><body>Sometext</body></html>"), "Sometext");
		assertEquals("stripHTMLBodyWithTags", StringUtil.stripHTML("<html><sometag></sometag><someothertag/><body>Sometext <strong>someSTRONGtext</strong></body></html>"), "Sometext someSTRONGtext");
		assertEquals("stripHTMLWithoutBody", StringUtil.stripHTML("<html><header></header>Somecontent</html>"), "");
	}
}
