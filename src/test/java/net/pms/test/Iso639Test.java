/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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

package net.pms.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.pms.dlna.DLNAMediaLang;
import net.pms.util.Iso639;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;


/**
 * Test the RendererConfiguration class
 */
public class Iso639Test {

	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset(); 
    }

	/**
	 * Test Iso639 class to verify the defined languages. 
	 */
	@Test
	public void testCodes() {
		assertNull("No language found for ISO code null", Iso639.getLanguage(null));

		// Reserved keyword "loc" should not match anything
		assertNull("No language found for ISO code \"loc\"", Iso639.getLanguage("loc"));

		// Reserved keyword DLNAMediaLang.UND should match "Undetermined"
		assertEquals("ISO code \"" + DLNAMediaLang.UND + "\" returns \"Undetermined\"",
				"Undetermined", Iso639.getLanguage(DLNAMediaLang.UND));

		assertEquals("ISO code \"en\" returns \"English\"", "English", Iso639.getLanguage("en"));
		assertEquals("ISO code \"eng\" returns \"English\"", "English", Iso639.getLanguage("eng"));
		assertEquals("ISO code \"EnG\" returns \"English\"", "English", Iso639.getLanguage("EnG"));
	}
}
