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
		assertEquals("TIFF matches \"tEsT.TiFf\"", true, new TIF().match("tEsT.TiFf"));

		// Starting with identifier instead of ending
		assertEquals("TIFF does not match \"tiff.test\"", false, new TIF().match("tiff.test"));

		// Substring
		assertEquals("TIFF does not match \"not.tiff.but.mp3\"", false, new TIF().match("not.tiff.but.mp3"));
    }

    /**
     * Test if {@link Format#match(String)} manages to match the identifiers
     * specified in each format with getId().
     */
    @Test
	public void testFormatIdentifiers() {
		// Identifier tests based on the identifiers defined in getId() of each class
		assertEquals("DVRMS matches \"test.dvr\"", true, new DVRMS().match("test.dvr"));
		assertEquals("AC3 matches \"test.ac3\"", true, new AC3().match("test.ac3"));
		assertEquals("ADPCM matches \"test.act\"", true, new ADPCM().match("test.act"));
		assertEquals("ADTS matches \"test.aac\"", true, new ADTS().match("test.aac"));
		assertEquals("AIFF matches \"test.aiff\"", true, new AIFF().match("test.aiff"));
		assertEquals("AIFF matches \"test.aif\"", true, new AIFF().match("test.aif"));
		assertEquals("AIFF matches \"test.aifc\"", true, new AIFF().match("test.aifc"));
		assertEquals("ASS matches \"test.ass\"", true, new ASS().match("test.ass"));
		assertEquals("ASS matches \"test.ssa\"", true, new ASS().match("test.ssa"));
		assertEquals("ATRAC matches \"test.aa3\"", true, new ATRAC().match("test.aa3"));
		assertEquals("ATRAC matches \"test.at3\"", true, new ATRAC().match("test.at3"));
		assertEquals("ATRAC matches \"test.at9\"", true, new ATRAC().match("test.at9"));
		assertEquals("ATRAC matches \"test.atrac\"", true, new ATRAC().match("test.atrac"));
		assertEquals("ATRAC matches \"test.msa\"", true, new ATRAC().match("test.msa"));
		assertEquals("ATRAC matches \"test.oma\"", true, new ATRAC().match("test.oma"));
		assertEquals("ATRAC matches \"test.omg\"", true, new ATRAC().match("test.omg"));
		assertEquals("AU matches \"test.au\"", true, new AU().match("test.au"));
		assertEquals("AU matches \"test.snd\"", true, new AU().match("test.snd"));
		assertEquals("BMP matches \"test.bmp\"", true, new BMP().match("test.bmp"));
		assertEquals("CAF matches \"test.caf\"", true, new CAF().match("test.caf"));
		assertEquals("DSDAudio matches \"test.dff\"", true, new DSDAudio().match("test.dff"));
		assertEquals("DSDAudio matches \"test.dsf\"", true, new DSDAudio().match("test.dsf"));
		assertEquals("DTS matches \"test.dts\"", true, new DTS().match("test.dts"));
		assertEquals("EAC3 matches \"test.eac3\"", true, new EAC3().match("test.eac3"));
		assertEquals("PLAYLIST matches \"test.cue\"", true, new PLAYLIST().match("test.cue"));
		assertEquals("FLAC matches \"test.fla\"", true, new FLAC().match("test.fla"));
		assertEquals("FLAC matches \"test.flac\"", true, new FLAC().match("test.flac"));
		assertEquals("FlashAudio matches \"test.f4a\"", true, new FlashAudio().match("test.f4a"));
		assertEquals("FlashAudio matches \"test.f4b\"", true, new FlashAudio().match("test.f4b"));
		assertEquals("FLV matches \"test.f4p\"", true, new FLV().match("test.f4p"));
		assertEquals("FLV matches \"test.f4v\"", true, new FLV().match("test.f4v"));
		assertEquals("FLV matches \"test.flv\"", true, new FLV().match("test.flv"));
		assertEquals("GIF matches \"test.gif\"", true, new GIF().match("test.gif"));
		assertEquals("IDX matches \"test.idx\"", true, new IDX().match("test.idx"));
		assertEquals("ISO matches \"test.img\"", true, new ISO().match("test.img"));
		assertEquals("ISO matches \"test.iso\"", true, new ISO().match("test.iso"));
		assertEquals("JPG matches \"test.jpe\"", true, new JPG().match("test.jpe"));
		assertEquals("JPG matches \"test.jpg\"", true, new JPG().match("test.jpg"));
		assertEquals("JPG matches \"test.jpeg\"", true, new JPG().match("test.jpeg"));
		assertEquals("JPG matches \"test.mpo\"", true, new JPG().match("test.mpo"));
		assertEquals("PLAYLIST matches \"test.m3u\"", true, new PLAYLIST().match("test.m3u"));
		assertEquals("PLAYLIST matches \"test.m3u8\"", true, new PLAYLIST().match("test.m3u8"));
		assertEquals("MKA matches \"test.mka\"", true, new MKA().match("test.mka"));
		assertEquals("MKV matches \"test.mkv\"", true, new MKV().match("test.mkv"));
		assertEquals("MicroDVD matches \"test.sub\"", true, new MicroDVD().match("test.sub"));
		assertEquals("MonkeysAudio matches \"test.ape\"", true, new MonkeysAudio().match("test.ape"));
		assertEquals("MP3 matches \"test.mp3\"", true, new MP3().match("test.mp3"));
		assertEquals("MPG matches \"test.mpg\"", true, new MPG().match("test.mpg"));
		assertEquals("MPGAudio matches \"test.mp2\"", true, new MPGAudio().match("test.mp2"));
		assertEquals("MPGAudio matches \"test.mpa\"", true, new MPGAudio().match("test.mpa"));
//		assertEquals("OGA matches \"test.oga\"", true, new OGA().match("test.oga"));
		assertEquals("OGG matches \"test.ogg\"", true, new OGG().match("test.ogg"));
		assertEquals("OGG matches \"test.opus\"", true, new OGG().match("test.opus"));
		assertEquals("OGG matches \"test.spx\"", true, new OGG().match("test.spx"));
		assertEquals("PLAYLIST matches \"test.pls\"", true, new PLAYLIST().match("test.pls"));
		assertEquals("PNG matches \"test.png\"", true, new PNG().match("test.png"));
		assertEquals("QCELP matches \"test.qcp\"", true, new QCELP().match("test.qcp"));
		assertEquals("RA matches \"test.ra\"", true, new RA().match("test.ra"));
		assertEquals("RAW matches \"test.3fr\"", true, new RAW().match("test.3fr"));
		assertEquals("RAW matches \"test.ari\"", true, new RAW().match("test.ari"));
		assertEquals("RAW matches \"test.arw\"", true, new RAW().match("test.arw"));
		assertEquals("RAW matches \"test.bay\"", true, new RAW().match("test.bay"));
		assertEquals("RAW matches \"test.cap\"", true, new RAW().match("test.cap"));
		assertEquals("RAW matches \"test.cr2\"", true, new RAW().match("test.cr2"));
		assertEquals("RAW matches \"test.crw\"", true, new RAW().match("test.crw"));
		assertEquals("RAW matches \"test.dcr\"", true, new RAW().match("test.dcr"));
		assertEquals("RAW matches \"test.dcs\"", true, new RAW().match("test.dcs"));
		assertEquals("RAW matches \"test.dng\"", true, new RAW().match("test.dng"));
		assertEquals("RAW matches \"test.drf\"", true, new RAW().match("test.drf"));
		assertEquals("RAW matches \"test.eip\"", true, new RAW().match("test.eip"));
		assertEquals("RAW matches \"test.erf\"", true, new RAW().match("test.erf"));
		assertEquals("RAW matches \"test.fff\"", true, new RAW().match("test.fff"));
		assertEquals("RAW matches \"test.iiq\"", true, new RAW().match("test.iiq"));
		assertEquals("RAW matches \"test.k25\"", true, new RAW().match("test.k25"));
		assertEquals("RAW matches \"test.kdc\"", true, new RAW().match("test.kdc"));
		assertEquals("RAW matches \"test.mdc\"", true, new RAW().match("test.mdc"));
		assertEquals("RAW matches \"test.mef\"", true, new RAW().match("test.mef"));
		assertEquals("RAW matches \"test.mos\"", true, new RAW().match("test.mos"));
		assertEquals("RAW matches \"test.nef\"", true, new RAW().match("test.nef"));
		assertEquals("RAW matches \"test.nrw\"", true, new RAW().match("test.nrw"));
		assertEquals("RAW matches \"test.obm\"", true, new RAW().match("test.obm"));
		assertEquals("RAW matches \"test.orf\"", true, new RAW().match("test.orf"));
		assertEquals("RAW matches \"test.pef\"", true, new RAW().match("test.pef"));
		assertEquals("RAW matches \"test.ptx\"", true, new RAW().match("test.ptx"));
		assertEquals("RAW matches \"test.pxn\"", true, new RAW().match("test.pxn"));
		assertEquals("RAW matches \"test.r3d\"", true, new RAW().match("test.r3d"));
		assertEquals("RAW matches \"test.raf\"", true, new RAW().match("test.raf"));
		assertEquals("RAW matches \"test.raw\"", true, new RAW().match("test.raw"));
		assertEquals("RAW matches \"test.rwl\"", true, new RAW().match("test.rwl"));
		assertEquals("RAW matches \"test.rw2\"", true, new RAW().match("test.rw2"));
		assertEquals("RAW matches \"test.rwz\"", true, new RAW().match("test.rwz"));
		assertEquals("RAW matches \"test.sr2\"", true, new RAW().match("test.sr2"));
		assertEquals("RAW matches \"test.srf\"", true, new RAW().match("test.srf"));
		assertEquals("RAW matches \"test.srw\"", true, new RAW().match("test.srw"));
		assertEquals("RAW matches \"test.x3f\"", true, new RAW().match("test.x3f"));
		assertEquals("SAMI matches \"test.smi\"", true, new SAMI().match("test.smi"));
		assertEquals("SHN matches \"test.shn\"", true, new SHN().match("test.shn"));
		assertEquals("SubRip matches \"test.srt\"", true, new SubRip().match("test.srt"));
		assertEquals("SUP matches \"test.sup\"", true, new SUP().match("test.sup"));
		assertEquals("THREEG2A matches \"test.3g2a\"", true, new THREEG2A().match("test.3g2a"));
		assertEquals("THREEGA matches \"test.3ga\"", true, new THREEGA().match("test.3ga"));
		assertEquals("THREEGA matches \"test.3gpa\"", true, new THREEGA().match("test.3gpa"));
		assertEquals("THREEGA matches \"test.amr\"", true, new THREEGA().match("test.amr"));
		assertEquals("TXT matches \"test.txt\"", true, new TXT().match("test.txt"));
		assertEquals("TIF matches \"test.tiff\"", true, new TIF().match("test.tiff"));
		assertEquals("TIF matches \"test.tif\"", true, new TIF().match("test.tif"));
		assertEquals("TrueHD matches \"test.thd\"", true, new TrueHD().match("test.thd"));
		assertEquals("TTA matches \"test.tta\"", true, new TTA().match("test.tta"));
		assertEquals("PLAYLIST matches \"test.ups\"", true, new PLAYLIST().match("test.ups"));
		assertEquals("VQF matches \"test.vqf\"", true, new VQF().match("test.vqf"));
		assertEquals("WAV matches \"test.wav\"", true, new WAV().match("test.wav"));
		assertEquals("WAV matches \"test.wave\"", true, new WAV().match("test.wave"));
		assertEquals("WavPack matches \"test.wv\"", true, new WavPack().match("test.wv"));
		assertEquals("WavPack matches \"test.wvp\"", true, new WavPack().match("test.wvp"));
		assertEquals("WEB matches \"http\"", true, new WEB().match("http://test.org/"));
		assertEquals("WebVTT matches \"test.vtt\"", true, new WebVTT().match("test.vtt"));
		assertEquals("WMA matches \"test.wma\"", true, new WMA().match("test.wma"));
	}
}
