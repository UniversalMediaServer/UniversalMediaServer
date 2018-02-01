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

import ch.qos.logback.classic.LoggerContext;
import net.pms.dlna.DLNAMediaLang;
import net.pms.util.Iso639;
import net.pms.util.Iso639.Iso639Entry;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;


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
		assertNull("No language found for ISO code null", Iso639.getFirstName(null));

		// Reserved keyword DLNAMediaLang.UND should match "Undetermined"
		assertEquals("ISO code \"" + DLNAMediaLang.UND + "\" returns \"Undetermined\"",
				"Undetermined", Iso639.getFirstName(DLNAMediaLang.UND));

		assertEquals("ISO code \"en\" returns \"English\"", "English", Iso639.getFirstName("en"));
		assertEquals("ISO code \"eng\" returns \"English\"", "English", Iso639.getFirstName("eng"));
		assertEquals("ISO code \"EnG\" returns \"English\"", "English", Iso639.getFirstName("EnG"));
		assertNull("Language name \"Czech language\" returns null", Iso639.getFirstName("Czech language"));
		assertEquals(Iso639.getFirstName("Czech language", true), "Czech");
		assertEquals(Iso639.getFirstName("The French don't like other languages", true), "French");

		// Test codeIsValid()
		assertTrue("ISO code \"en\" is valid", Iso639.codeIsValid("en"));
		assertTrue("ISO code \"EN\" is valid", Iso639.codeIsValid("EN"));
		assertTrue("ISO code \"vie\" is valid", Iso639.codeIsValid("vie"));
		assertTrue("ISO code \"vIe\" is valid", Iso639.codeIsValid("vIe"));
		assertFalse("ISO code \"en-uk\" is invalid", Iso639.codeIsValid("en-uk"));
		assertFalse("ISO code \"\" is invalid", Iso639.codeIsValid(""));
		assertFalse("ISO code null is invalid", Iso639.codeIsValid(null));

		// Test isValid()
		assertTrue("ISO code \"en\" is valid", Iso639.isValid("en"));
		assertTrue("ISO code \"EN\" is valid", Iso639.isValid("EN"));
		assertTrue("ISO code \"vie\" is valid", Iso639.isValid("vie"));
		assertTrue("ISO code \"vIe\" is valid", Iso639.isValid("vIe"));
		assertFalse("ISO code \"en-uk\" is invalid", Iso639.isValid("en-uk"));
		assertFalse("ISO code \"\" is invalid", Iso639.isValid(""));
		assertFalse("ISO code null is invalid", Iso639.isValid(null));
		assertTrue(Iso639.isValid("ENGLISH"));
		assertTrue(Iso639.isValid("Burmese"));
		assertTrue(Iso639.isValid("telugu"));

		// Test getISO639_2Code()
		assertEquals("ISO code \"en\" returns \"eng\"", Iso639.getISO639_2Code("en"), "eng");
		assertEquals("ISO code \"eng\" returns \"eng\"", Iso639.getISO639_2Code("eng"), "eng");
		assertNull("ISO code \"\" returns null", Iso639.getISO639_2Code(""));
		assertNull("ISO code null returns null", Iso639.getISO639_2Code(null));
		assertEquals("Language name \"English\" returns ISO code \"eng\"", Iso639.getISO639_2Code("English"), "eng");
		assertEquals("Language name \"english\" returns null", Iso639.getISO639_2Code("english"), "eng");
		assertEquals("Language name \"Czech\" returns ISO code \"cze\"", Iso639.getISO639_2Code("Czech"), "cze");
		assertNull("Language name \"Czech language\" returns null", Iso639.getISO639_2Code("Czech language"));
		assertEquals(Iso639.getISO639_2Code("Czech language", true), "cze");
		assertEquals(Iso639.getISO639_2Code("The French don't like other languages", true), "fre");
		assertEquals(Iso639.getISO639_2Code("Does anyone speak sweedish?", true), "swe");
		assertEquals(Iso639.getISO639_2Code("Norweigan"), "nor");

		// Test isCodeMatching()
		assertTrue("ISO code \"ful\" matches language \"Fulah\"", Iso639.isCodeMatching("Fulah", "ful"));
		assertTrue("ISO code \"gd\" matches language \"Gaelic (Scots)\"", Iso639.isCodeMatching("Gaelic", "gd"));
		assertTrue("ISO code \"gla\" matches language \"Gaelic (Scots)\"", Iso639.isCodeMatching("Gaelic", "gla"));
		assertFalse("ISO code \"eng\" doesn't match language \"Gaelic (Scots)\"", Iso639.isCodeMatching("Gaelic", "eng"));
		assertTrue("ISO code \"gla\" matches ISO code \"gd\"", Iso639.isCodesMatching("gla", "gd"));
		assertTrue("ISO code \"ice\" matches ISO code \"is\"", Iso639.isCodesMatching("ice", "is"));
		assertTrue("ISO code \"isl\" matches ISO code \"ice\"", Iso639.isCodesMatching("isl", "ice"));
		assertFalse("ISO code \"lav\" doesn't match ISO code \"en\"", Iso639.isCodesMatching("lav", "en"));

		// Test getISOCode()
		assertEquals("ISO code \"eng\" returns ISO code \"en\"", Iso639.getISOCode("eng"), "en");
		assertEquals("ISO code \"ell\" returns ISO code \"el\"", Iso639.getISOCode("ell"), "el");
		assertEquals("ISO code \"gre\" returns ISO code \"el\"", Iso639.getISOCode("gre"), "el");
		assertEquals("ISO code \"gay\" returns ISO code \"gay\"", Iso639.getISOCode("gay"), "gay"); // No pun intended
		assertNull("Language name \"Czech language\" returns null", Iso639.getISOCode("Czech language"));
		assertEquals(Iso639.getISOCode("Czech language", true), "cs");
		assertEquals(Iso639.getISOCode("The French don't like other languages", true), "fr");
		assertEquals(Iso639.getISOCode("Where do they speak Choctaw?", true), "cho");
		assertEquals(Iso639.getISOCode("Where do they speak madureese?", true), "mad");
		assertEquals(Iso639.getISOCode("Where do they speak philipine?", true), "phi");
		assertEquals(Iso639.getISOCode("Where do they speak portugese?", true), "pt");
		assertEquals(Iso639.getISOCode("Where do they speak sinhaleese?", true), "si");
		assertEquals(Iso639.getISOCode("Does anyone speak sweedish?", true), "sv");

		// Test multiple language names
		Iso639Entry entry1 = Iso639.get("Imperial Aramaic");
		Iso639Entry entry2 = Iso639.get("Official Aramaic");
		Iso639Entry entry3 = Iso639.get("arc");
		assertEquals("Imperial Aramaic (700-300 BCE)", entry1.getFirstName());
		assertEquals(entry1, entry2);
		assertEquals(entry1, entry3);
		assertEquals("Official Aramaic (700-300 BCE)", entry3.getNames()[1]);
		assertEquals("Official Aramaic (700-300 BCE)", entry3.getNames()[1]);
		assertEquals(3, Iso639.get("SEPEDI").getNames().length);
	}
}
