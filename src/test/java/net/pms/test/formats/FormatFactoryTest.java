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

package net.pms.test.formats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

/**
 * Test basic functionality of {@link Format}.
 */
public class FormatFactoryTest {
	/**
	 * Set up testing conditions before running the tests.
	 */
	@Before
	public final void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
	}

	/**
	 * Test edge cases for {@link FormatFactory#getAssociatedExtension(String)}.
	 */
	@Test
	public final void testFormatFactoryEdgeCases() {
		// Null string
		Format result = FormatFactory.getAssociatedExtension(null);
		assertNull("Null string matches no format", result);

		// Empty string
		result = FormatFactory.getAssociatedExtension("");
		assertNull("Empty string matches no extension", result);

		// Unsupported protocol and extension
		result = FormatFactory.getAssociatedExtension(
			"bogus://example.com/test.bogus"
		);
		assertNull(
		    "Unsupported protocol and extension: \"bogus://example.com/test.bogus\" matches no format",
		    result
		);
				
		// XXX an unsupported (here misspelt) protocol should result in a failed match rather
		// than fall through to an extension match
		/*
			result = FormatFactory.getAssociatedExtension(
				"htp://example.com/test.mp3"
			);
			assertNull(
				"Unsupported protocol: \"htp://example.com/test.mp3\" matches no format",
				result
			);
		*/

		// Unsupported extension
		result = FormatFactory.getAssociatedExtension(
			"test.bogus"
		);
		assertNull(
			"Unsupported extension: \"test.bogus\" matches no format",
			result
		);

		// Confirm the protocol (e.g. WEB) is checked before the extension
		testSingleFormat("http://example.com/test.mp3", "WEB");
		testSingleFormat("http://example.com/test.asf?format=.wmv", "WEB");
	}

	/**
	 * Test whether {@link FormatFactory#getAssociatedExtension(String)} manages
	 * to retrieve the correct format.
	 */
	@Test
	public final void testFormatRetrieval() {
		testSingleFormat("test.dvr", "DVRMS");
		testSingleFormat("test.flac", "FLAC");
		testSingleFormat("test.gif", "GIF");
		testSingleFormat("test.iso", "ISO");
		testSingleFormat("test.jpg", "JPG");
		testSingleFormat("test.wma", "M4A");
		testSingleFormat("test.mkv", "MKV");
		testSingleFormat("test.mp3", "MP3");
		testSingleFormat("test.mpg", "MPG");
		testSingleFormat("test.ogg", "OGG");
		testSingleFormat("test.png", "PNG");
		testSingleFormat("test.arw", "RAW");
		testSingleFormat("test.tiff", "TIF");
		testSingleFormat("test.wav", "WAV");
		testSingleFormat("http://example.com/", "WEB");
	}

	/**
	 * Verify if a filename is recognized as a given format. Use
	 * <code>null</code> as formatName when no match is expected.
	 * 
	 * @param filename
	 *            The filename to verify.
	 * @param formatName
	 *            The name of the expected format.
	 */
	private void testSingleFormat(final String filename, final String formatName) {
		Format result = FormatFactory.getAssociatedExtension(filename);

		if (result != null) {
			assertEquals("\"" + filename + "\" is expected to match",
					formatName, result.toString());
		} else {
			assertNull("\"" + filename + "\" is expected to match nothing", formatName);
		}
	}
}
