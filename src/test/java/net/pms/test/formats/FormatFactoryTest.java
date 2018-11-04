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
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

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
		Format result = FormatFactory.getAssociatedFormat(null);
		assertNull("Null string matches no format", result);

		// Empty string
		result = FormatFactory.getAssociatedFormat("");
		assertNull("Empty string matches no extension", result);

		// Unsupported extension
		result = FormatFactory.getAssociatedFormat(
			"test.bogus"
		);
		assertNull(
			"Unsupported extension: \"test.bogus\" matches no format",
			result
		);

		// Confirm the protocol (e.g. WEB) is checked before the extension
		testSingleFormat("http://example.com/test.mp3", "WEB", Format.UNKNOWN);
		testSingleFormat("http://example.com/test.asf?format=.wmv", "WEB", Format.UNKNOWN);

		// confirm that the WEB format is assigned for arbitrary protocols
		testSingleFormat("svn+ssh://example.com/example.test", "WEB", Format.UNKNOWN);
		testSingleFormat("bogus://example.com/test.test", "WEB", Format.UNKNOWN);
		testSingleFormat("fake://example.com/test.test", "WEB", Format.UNKNOWN);
		testSingleFormat("pms://example", "WEB", Format.UNKNOWN);
	}

	/**
	 * Test whether {@link FormatFactory#getAssociatedExtension(String)} manages
	 * to retrieve the correct format.
	 */
	@Test
	public final void testFormatRetrieval() {
		testSingleFormat("test.ac3", "AC3", Format.AUDIO);
		testSingleFormat("test.act", "ADPCM", Format.AUDIO);
		testSingleFormat("test.aac", "ADTS", Format.AUDIO);
		testSingleFormat("test.aif", "AIFF", Format.AUDIO);
		testSingleFormat("test.aiff", "AIFF", Format.AUDIO);
		testSingleFormat("test.aifc", "AIFF", Format.AUDIO);
		testSingleFormat("test.ass", "ASS", Format.SUBTITLE);
		testSingleFormat("test.ssa", "ASS", Format.SUBTITLE);
		testSingleFormat("test.aa3", "ATRAC", Format.AUDIO);
		testSingleFormat("test.at3", "ATRAC", Format.AUDIO);
		testSingleFormat("test.at9", "ATRAC", Format.AUDIO);
		testSingleFormat("test.atrac", "ATRAC", Format.AUDIO);
		testSingleFormat("test.msa", "ATRAC", Format.AUDIO);
		testSingleFormat("test.oma", "ATRAC", Format.AUDIO);
		testSingleFormat("test.omg", "ATRAC", Format.AUDIO);
		testSingleFormat("test.au", "AU", Format.AUDIO);
		testSingleFormat("test.snd", "AU", Format.AUDIO);
		testSingleFormat("test.dff", "DFF", Format.AUDIO);
		testSingleFormat("test.dsf", "DSF", Format.AUDIO);
		testSingleFormat("test.dvr", "DVRMS", Format.VIDEO);
		testSingleFormat("test.dts", "DTS", Format.AUDIO);
		testSingleFormat("test.eac3", "EAC3", Format.AUDIO);
		testSingleFormat("test.fla", "FLAC", Format.AUDIO);
		testSingleFormat("test.flac", "FLAC", Format.AUDIO);
		testSingleFormat("test.gif", "GIF", Format.IMAGE);
		testSingleFormat("test.idx", "IDX", Format.SUBTITLE);
		testSingleFormat("test.img", "ISO", Format.ISO);
		testSingleFormat("test.iso", "ISO", Format.ISO);
		testSingleFormat("test.jpe", "JPG", Format.IMAGE);
		testSingleFormat("test.jpeg", "JPG", Format.IMAGE);
		testSingleFormat("test.jpg", "JPG", Format.IMAGE);
		testSingleFormat("test.mpo", "JPG", Format.IMAGE);
		testSingleFormat("test.m4a", "M4A", Format.AUDIO);
		testSingleFormat("test.3g2", "MKV", Format.VIDEO);
		testSingleFormat("test.3gp", "MKV", Format.VIDEO);
		testSingleFormat("test.3gp2", "MKV", Format.VIDEO);
		testSingleFormat("test.asf", "MKV", Format.VIDEO);
		testSingleFormat("test.asx", "MKV", Format.VIDEO);
		testSingleFormat("test.dv", "MKV", Format.VIDEO);
		testSingleFormat("test.evo", "MKV", Format.VIDEO);
		testSingleFormat("test.flv", "MKV", Format.VIDEO);
		testSingleFormat("test.hdmov", "MKV", Format.VIDEO);
		testSingleFormat("test.hdm", "MKV", Format.VIDEO);
		testSingleFormat("test.m2v", "MKV", Format.VIDEO);
		testSingleFormat("test.mk3d", "MKV", Format.VIDEO);
		testSingleFormat("test.mkv", "MKV", Format.VIDEO);
		testSingleFormat("test.mov", "MKV", Format.VIDEO);
		testSingleFormat("test.ogm", "OGG", Format.VIDEO);
		testSingleFormat("test.ogv", "OGG", Format.VIDEO);
		testSingleFormat("test.rmv", "MKV", Format.VIDEO);
		testSingleFormat("test.rmvb", "MKV", Format.VIDEO);
		testSingleFormat("test.rm", "MKV", Format.VIDEO);
		testSingleFormat("test.webm", "MKV", Format.VIDEO);
		testSingleFormat("test.265", "MKV", Format.VIDEO);
		testSingleFormat("test.h265", "MKV", Format.VIDEO);
		testSingleFormat("test.mlp", "MLP", Format.AUDIO);
		testSingleFormat("test.mp3", "MP3", Format.AUDIO);
		testSingleFormat("test.mpc", "MPC", Format.AUDIO);
		testSingleFormat("test.mp+", "MPC", Format.AUDIO);
		testSingleFormat("test.mpp", "MPC", Format.AUDIO);
		testSingleFormat("test.avi", "MPG", Format.VIDEO);
		testSingleFormat("test.div", "MPG", Format.VIDEO);
		testSingleFormat("test.divx", "MPG", Format.VIDEO);
		testSingleFormat("test.m2p", "MPG", Format.VIDEO);
		testSingleFormat("test.m2t", "MPG", Format.VIDEO);
		testSingleFormat("test.m2ts", "MPG", Format.VIDEO);
		testSingleFormat("test.m4v", "MPG", Format.VIDEO);
		testSingleFormat("test.mj2", "MPG", Format.VIDEO);
		testSingleFormat("test.mjp2", "MPG", Format.VIDEO);
		testSingleFormat("test.mod", "MPG", Format.VIDEO);
		testSingleFormat("test.mp4", "MPG", Format.VIDEO);
		testSingleFormat("test.mpe", "MPG", Format.VIDEO);
		testSingleFormat("test.mpeg", "MPG", Format.VIDEO);
		testSingleFormat("test.mpg", "MPG", Format.VIDEO);
		testSingleFormat("test.mts", "MPG", Format.VIDEO);
		testSingleFormat("test.s4ud", "MPG", Format.VIDEO);
		testSingleFormat("test.tivo", "MPG", Format.VIDEO);
		testSingleFormat("test.tmf", "MPG", Format.VIDEO);
		testSingleFormat("test.tp", "MPG", Format.VIDEO);
		testSingleFormat("test.ts", "MPG", Format.VIDEO);
		testSingleFormat("test.ty", "MPG", Format.VIDEO);
		testSingleFormat("test.vdr", "MPG", Format.VIDEO);
		testSingleFormat("test.vob", "MPG", Format.VIDEO);
		testSingleFormat("test.vro", "MPG", Format.VIDEO);
		testSingleFormat("test.wm", "MPG", Format.VIDEO);
		testSingleFormat("test.wmv", "MPG", Format.VIDEO);
		testSingleFormat("test.wtv", "MPG", Format.VIDEO);
		testSingleFormat("test.mpa", "MPGAudio", Format.AUDIO);
		testSingleFormat("test.mp2", "MPGAudio", Format.AUDIO);
		testSingleFormat("test.sub", "MicroDVD", Format.SUBTITLE);
		testSingleFormat("test.ape", "MonkeysAudio", Format.AUDIO);
		testSingleFormat("test.oga", "OGA", Format.AUDIO);
		testSingleFormat("test.ogg", "OGG", Format.VIDEO);
		testSingleFormat("test.spx", "OGA", Format.AUDIO);
		testSingleFormat("test.opus", "OGA", Format.AUDIO);
		testSingleFormat("test.pls", "PLAYLIST", Format.PLAYLIST);
		testSingleFormat("test.m3u", "PLAYLIST", Format.PLAYLIST);
		testSingleFormat("test.m3u8", "PLAYLIST", Format.PLAYLIST);
		testSingleFormat("test.cue", "PLAYLIST", Format.PLAYLIST);
		testSingleFormat("test.ups", "PLAYLIST", Format.PLAYLIST);
		testSingleFormat("test.ra", "RA", Format.AUDIO);
		testSingleFormat("test.png", "PNG", Format.IMAGE);
		testSingleFormat("test.3fr", "RAW", Format.IMAGE);
		testSingleFormat("test.ari", "RAW", Format.IMAGE);
		testSingleFormat("test.arw", "RAW", Format.IMAGE);
		testSingleFormat("test.bay", "RAW", Format.IMAGE);
		testSingleFormat("test.cap", "RAW", Format.IMAGE);
		testSingleFormat("test.cr2", "RAW", Format.IMAGE);
		testSingleFormat("test.crw", "RAW", Format.IMAGE);
		testSingleFormat("test.dcr", "RAW", Format.IMAGE);
		testSingleFormat("test.dcs", "RAW", Format.IMAGE);
		testSingleFormat("test.dng", "RAW", Format.IMAGE);
		testSingleFormat("test.drf", "RAW", Format.IMAGE);
		testSingleFormat("test.eip", "RAW", Format.IMAGE);
		testSingleFormat("test.erf", "RAW", Format.IMAGE);
		testSingleFormat("test.fff", "RAW", Format.IMAGE);
		testSingleFormat("test.iiq", "RAW", Format.IMAGE);
		testSingleFormat("test.k25", "RAW", Format.IMAGE);
		testSingleFormat("test.kdc", "RAW", Format.IMAGE);
		testSingleFormat("test.mdc", "RAW", Format.IMAGE);
		testSingleFormat("test.mef", "RAW", Format.IMAGE);
		testSingleFormat("test.mos", "RAW", Format.IMAGE);
		testSingleFormat("test.mrw", "RAW", Format.IMAGE);
		testSingleFormat("test.nef", "RAW", Format.IMAGE);
		testSingleFormat("test.nrw", "RAW", Format.IMAGE);
		testSingleFormat("test.obm", "RAW", Format.IMAGE);
		testSingleFormat("test.orf", "RAW", Format.IMAGE);
		testSingleFormat("test.pef", "RAW", Format.IMAGE);
		testSingleFormat("test.ptx", "RAW", Format.IMAGE);
		testSingleFormat("test.pxn", "RAW", Format.IMAGE);
		testSingleFormat("test.r3d", "RAW", Format.IMAGE);
		testSingleFormat("test.raf", "RAW", Format.IMAGE);
		testSingleFormat("test.raw", "RAW", Format.IMAGE);
		testSingleFormat("test.rwl", "RAW", Format.IMAGE);
		testSingleFormat("test.rw2", "RAW", Format.IMAGE);
		testSingleFormat("test.rwz", "RAW", Format.IMAGE);
		testSingleFormat("test.sr2", "RAW", Format.IMAGE);
		testSingleFormat("test.srf", "RAW", Format.IMAGE);
		testSingleFormat("test.srw", "RAW", Format.IMAGE);
		testSingleFormat("test.x3f", "RAW", Format.IMAGE);
		testSingleFormat("test.smi", "SAMI", Format.SUBTITLE);
		testSingleFormat("test.shn", "SHN", Format.AUDIO);
		testSingleFormat("test.sup", "SUP", Format.SUBTITLE);
		testSingleFormat("test.srt", "SubRip", Format.SUBTITLE);
		testSingleFormat("test.3g2a", "THREEG2A", Format.AUDIO);
		testSingleFormat("test.3ga", "THREEGA", Format.AUDIO);
		testSingleFormat("test.amr", "THREEGA", Format.AUDIO);
		testSingleFormat("test.3gpa", "THREEGA", Format.AUDIO);
		testSingleFormat("test.tta", "TTA", Format.AUDIO);
		testSingleFormat("test.txt", "TXT", Format.SUBTITLE);
		testSingleFormat("test.thd", "TrueHD", Format.AUDIO);
		testSingleFormat("test.tif", "TIFF", Format.IMAGE);
		testSingleFormat("test.tiff", "TIFF", Format.IMAGE);
		testSingleFormat("test.wav", "WAV", Format.AUDIO);
		testSingleFormat("test.wave", "WAV", Format.AUDIO);
		testSingleFormat("test.wv", "WavPack", Format.AUDIO);
		testSingleFormat("test.wvp", "WavPack", Format.AUDIO);
		testSingleFormat("test.vtt", "WebVTT", Format.SUBTITLE);
		testSingleFormat("test.wma", "WMA", Format.AUDIO);
		testSingleFormat("http://example.com/", "WEB", Format.UNKNOWN);
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
	private void testSingleFormat(final String filename, final String formatName, final int type) {
		Format result = FormatFactory.getAssociatedFormat(filename);

		if (result != null) {
			assertEquals("\"" + filename + "\" is expected to match",
					formatName, result.toString());
			assertEquals("\"" + filename + "\" is expected to be of type " + type, type, result.getType());
		} else {
			assertNull("\"" + filename + "\" is expected to match nothing", formatName);
		}
	}
}
