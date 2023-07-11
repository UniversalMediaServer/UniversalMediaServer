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
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Test basic functionality of {@link Format}.
 */
public class FormatTest {
	@BeforeEach
	public void setUp() {
		// Silence all log messages from the PMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
	}

    /**
     * Test edge cases for {@link Format#match(String)}.
     */
    @Test
	public void testFormatEdgeCases() {
    	// Empty string
		assertFalse(new MP3().match(""), "MP3 does not match \"\"");

    	// Null string
		assertFalse(new MP3().match(null), "MP3 does not match null");

		// Mixed case
		assertTrue(new TIFF().match("tEsT.TiFf"), "TIFF matches \"tEsT.TiFf\"");

		// Starting with identifier instead of ending
		assertFalse(new TIFF().match("tiff.test"), "TIFF does not match \"tiff.test\"");

		// Substring
		assertFalse(new TIFF().match("not.tiff.but.mp3"), "TIFF does not match \"not.tiff.but.mp3\"");
    }

    /**
     * Test if {@link Format#match(String)} manages to match the identifiers
     * specified in each format with getId().
     */
    @Test
	public void testFormatIdentifiers() {
		// Identifier tests based on the identifiers defined in getId() of each class
		assertTrue(new DVRMS().match("test.dvr"), "DVRMS matches \"test.dvr\"");
		assertTrue(new AIFF().match("test.aiff"), "AIFF matches \"test.aiff\"");
		assertTrue(new FLAC().match("test.flac"), "FLAC matches \"test.flac\"");
		assertTrue(new GIF().match("test.gif"), "GIF matches \"test.gif\"");
		assertTrue(new ISO().match("test.iso"), "ISO matches \"test.iso\"");
		assertTrue(new JPG().match("test.jpg"), "JPG matches \"test.jpg\"");
		assertTrue(new WMA().match("test.wma"), "WMA matches \"test.wma\"");
		assertTrue(new MKV().match("test.mkv"), "MKV matches \"test.mkv\"");
		assertTrue(new MP3().match("test.mp3"), "MP3 matches \"test.mp3\"");
		assertTrue(new MPG().match("test.mpg"), "MPG matches \"test.mpg\"");
		assertTrue(new OGG().match("test.ogg"), "OGG matches \"test.ogg\"");
		assertTrue(new PNG().match("test.png"), "PNG matches \"test.png\"");
		assertTrue(new RAW().match("test.arw"), "RAW matches \"test.arw\"");
		assertTrue(new TIFF().match("test.tiff"), "TIF matches \"test.tiff\"");
		assertTrue(new WAV().match("test.wav"), "WAV matches \"test.wav\"");
		assertTrue(new WEB().match("http://test.org/"), "WEB matches \"http\"");
	}
}
