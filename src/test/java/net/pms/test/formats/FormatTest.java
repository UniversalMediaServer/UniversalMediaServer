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

import ch.qos.logback.classic.LoggerContext;
import net.pms.formats.*;
import net.pms.formats.audio.AIFF;
import net.pms.formats.audio.FLAC;
import net.pms.formats.audio.MP3;
import net.pms.formats.audio.WAV;
import net.pms.formats.audio.WMA;
import net.pms.formats.image.GIF;
import net.pms.formats.image.JPG;
import net.pms.formats.image.PNG;
import net.pms.formats.image.RAW;
import net.pms.formats.image.TIFF;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Test basic functionality of {@link Format}.
 */
public class FormatTest {
	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
	}

    /**
     * Test edge cases for {@link Format#match(String)}.
     */
    @Test
	public void testFormatEdgeCases() {
    	// Empty string
		assertEquals("MP3 does not match \"\"", false, new MP3().match(""));

    	// Null string
		assertEquals("MP3 does not match null", false, new MP3().match(null));

		// Mixed case
		assertEquals("TIFF matches \"tEsT.TiFf\"", true, new TIFF().match("tEsT.TiFf"));

		// Starting with identifier instead of ending
		assertEquals("TIFF does not match \"tiff.test\"", false, new TIFF().match("tiff.test"));

		// Substring
		assertEquals("TIFF does not match \"not.tiff.but.mp3\"", false, new TIFF().match("not.tiff.but.mp3"));
    }

    /**
     * Test if {@link Format#match(String)} manages to match the identifiers
     * specified in each format with getId().
     */
    @Test
	public void testFormatIdentifiers() {
		// Identifier tests based on the identifiers defined in getId() of each class
		assertEquals("DVRMS matches \"test.dvr\"", true, new DVRMS().match("test.dvr"));
		assertEquals("AIFF matches \"test.aiff\"", true, new AIFF().match("test.aiff"));
		assertEquals("FLAC matches \"test.flac\"", true, new FLAC().match("test.flac"));
		assertEquals("GIF matches \"test.gif\"", true, new GIF().match("test.gif"));
		assertEquals("ISO matches \"test.iso\"", true, new ISO().match("test.iso"));
		assertEquals("JPG matches \"test.jpg\"", true, new JPG().match("test.jpg"));
		assertEquals("WMA matches \"test.wma\"", true, new WMA().match("test.wma"));
		assertEquals("MKV matches \"test.mkv\"", true, new MKV().match("test.mkv"));
		assertEquals("MP3 matches \"test.mp3\"", true, new MP3().match("test.mp3"));
		assertEquals("MPG matches \"test.mpg\"", true, new MPG().match("test.mpg"));
		assertEquals("OGG matches \"test.ogg\"", true, new OGG().match("test.ogg"));
		assertEquals("PNG matches \"test.png\"", true, new PNG().match("test.png"));
		assertEquals("RAW matches \"test.arw\"", true, new RAW().match("test.arw"));
		assertEquals("TIF matches \"test.tiff\"", true, new TIFF().match("test.tiff"));
		assertEquals("WAV matches \"test.wav\"", true, new WAV().match("test.wav"));
		assertEquals("WEB matches \"http\"", true, new WEB().match("http://test.org/"));
	}
}
