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
package net.pms.test;

import ch.qos.logback.classic.LoggerContext;
import net.pms.media.MediaLang;
import net.pms.util.Iso639;
import net.pms.util.Iso639.Iso639Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Test the RendererConfiguration class
 */
public class Iso639Test {

	@BeforeEach
	public void setUp() {
		// Silence all log messages from the PMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
    }

	/**
	 * Test Iso639 class to verify the defined languages.
	 */
	@Test
	public void testCodes() {
		assertNull(Iso639.getFirstName(null), "No language found for ISO code null");

		// Reserved keyword DLNAMediaLang.UND should match "Undetermined"
		assertEquals("Undetermined", Iso639.getFirstName(MediaLang.UND), "ISO code \"" + MediaLang.UND + "\" returns \"Undetermined\"");

		assertEquals("English", Iso639.getFirstName("en"), "ISO code \"en\" returns \"English\"");
		assertEquals("English", Iso639.getFirstName("eng"), "ISO code \"eng\" returns \"English\"");
		assertEquals("English", Iso639.getFirstName("EnG"), "ISO code \"EnG\" returns \"English\"");
		assertNull(Iso639.getFirstName("Czech language"), "Language name \"Czech language\" returns null");
		assertEquals(Iso639.getFirstName("Czech language", true), "Czech");
		assertEquals(Iso639.getFirstName("The French don't like other languages", true), "French");

		// Test codeIsValid()
		assertTrue(Iso639.codeIsValid("en"), "ISO code \"en\" is valid");
		assertTrue(Iso639.codeIsValid("EN"), "ISO code \"EN\" is valid");
		assertTrue(Iso639.codeIsValid("vie"), "ISO code \"vie\" is valid");
		assertTrue(Iso639.codeIsValid("vIe"), "ISO code \"vIe\" is valid");
		assertFalse(Iso639.codeIsValid("en-uk"), "ISO code \"en-uk\" is invalid");
		assertFalse(Iso639.codeIsValid(""), "ISO code \"\" is invalid");
		assertFalse(Iso639.codeIsValid(null), "ISO code null is invalid");

		// Test isValid()
		assertTrue(Iso639.isValid("en"), "ISO code \"en\" is valid");
		assertTrue(Iso639.isValid("EN"), "ISO code \"EN\" is valid");
		assertTrue(Iso639.isValid("vie"), "ISO code \"vie\" is valid");
		assertTrue(Iso639.isValid("vIe"), "ISO code \"vIe\" is valid");
		assertFalse(Iso639.isValid("en-uk"), "ISO code \"en-uk\" is invalid");
		assertFalse(Iso639.isValid(""), "ISO code \"\" is invalid");
		assertFalse(Iso639.isValid(null), "ISO code null is invalid");
		assertTrue(Iso639.isValid("ENGLISH"));
		assertTrue(Iso639.isValid("Burmese"));
		assertTrue(Iso639.isValid("telugu"));

		// Test getISO639_2Code()
		assertEquals(Iso639.getISO639_2Code("en"), "eng", "ISO code \"en\" returns \"eng\"");
		assertEquals(Iso639.getISO639_2Code("eng"), "eng", "ISO code \"eng\" returns \"eng\"");
		assertNull(Iso639.getISO639_2Code(""), "ISO code \"\" returns null");
		assertNull(Iso639.getISO639_2Code(null), "ISO code null returns null");
		assertEquals(Iso639.getISO639_2Code("English"), "eng", "Language name \"English\" returns ISO code \"eng\"");
		assertEquals(Iso639.getISO639_2Code("english"), "eng", "Language name \"english\" returns null");
		assertEquals(Iso639.getISO639_2Code("Czech"), "cze", "Language name \"Czech\" returns ISO code \"cze\"");
		assertNull(Iso639.getISO639_2Code("Czech language"), "Language name \"Czech language\" returns null");
		assertEquals(Iso639.getISO639_2Code("Czech language", true), "cze");
		assertEquals(Iso639.getISO639_2Code("The French don't like other languages", true), "fre");
		assertEquals(Iso639.getISO639_2Code("Does anyone speak sweedish?", true), "swe");
		assertEquals(Iso639.getISO639_2Code("Norweigan"), "nor");

		// Test isCodeMatching()
		assertTrue(Iso639.isCodeMatching("Fulah", "ful"), "ISO code \"ful\" matches language \"Fulah\"");
		assertTrue(Iso639.isCodeMatching("Gaelic", "gd"), "ISO code \"gd\" matches language \"Gaelic (Scots)\"");
		assertTrue(Iso639.isCodeMatching("Gaelic", "gla"), "ISO code \"gla\" matches language \"Gaelic (Scots)\"");
		assertFalse(Iso639.isCodeMatching("Gaelic", "eng"), "ISO code \"eng\" doesn't match language \"Gaelic (Scots)\"");
		assertTrue(Iso639.isCodesMatching("gla", "gd"), "ISO code \"gla\" matches ISO code \"gd\"");
		assertTrue(Iso639.isCodesMatching("ice", "is"), "ISO code \"ice\" matches ISO code \"is\"");
		assertTrue(Iso639.isCodesMatching("isl", "ice"), "ISO code \"isl\" matches ISO code \"ice\"");
		assertFalse(Iso639.isCodesMatching("lav", "en"), "ISO code \"lav\" doesn't match ISO code \"en\"");

		// Test getISOCode()
		assertEquals(Iso639.getISOCode("eng"), "en", "ISO code \"eng\" returns ISO code \"en\"");
		assertEquals(Iso639.getISOCode("ell"), "el", "ISO code \"ell\" returns ISO code \"el\"");
		assertEquals(Iso639.getISOCode("gre"), "el", "ISO code \"gre\" returns ISO code \"el\"");
		assertEquals(Iso639.getISOCode("gay"), "gay", "ISO code \"gay\" returns ISO code \"gay\""); // No pun intended
		assertNull(Iso639.getISOCode("Czech language"), "Language name \"Czech language\" returns null");
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
