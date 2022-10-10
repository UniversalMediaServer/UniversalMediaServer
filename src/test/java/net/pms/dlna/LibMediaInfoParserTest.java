/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.dlna;

import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.MediaInfo.StreamType;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LibMediaInfoParserTest {
	@BeforeAll
	public static void SetUPClass() {
		PMS.configureJNA();
	}

	@Test
	public void testGetReferenceFrameCount() throws Exception {
		assertEquals(LibMediaInfoParser.getReferenceFrameCount("-5 6"), (byte) -5);
		assertEquals(LibMediaInfoParser.getReferenceFrameCount("7"), (byte) 7);
		assertEquals(LibMediaInfoParser.getReferenceFrameCount("2 frame2"), (byte) 2);
		assertEquals(LibMediaInfoParser.getReferenceFrameCount("-16 frame3"), (byte) -16);
		assertEquals(LibMediaInfoParser.getReferenceFrameCount(""), (byte) -1);
		assertEquals(LibMediaInfoParser.getReferenceFrameCount("strange1"), (byte) -1);
		assertEquals(LibMediaInfoParser.getReferenceFrameCount("6ref"), (byte) -1);
	}

	@Test
	public void testGetAvcLevel() throws Exception {
		assertEquals(LibMediaInfoParser.getAvcLevel("Main@L2.0"), "2.0");
		assertEquals(LibMediaInfoParser.getAvcLevel("High@L3.0"), "3.0");
		assertEquals(LibMediaInfoParser.getAvcLevel("high@l4.0"), "4.0");
		assertEquals(LibMediaInfoParser.getAvcLevel("hIgH@L4.1"), "4.1");
		assertNull(LibMediaInfoParser.getAvcLevel("5.1"));
		assertNull(LibMediaInfoParser.getAvcLevel("level5"));
	}

	@Test
	public void testGetAvcProfile() throws Exception {
		assertEquals(LibMediaInfoParser.getAvcProfile("Main@L2.0"), "main");
		assertEquals(LibMediaInfoParser.getAvcProfile("High@L3.0"), "high");
		assertEquals(LibMediaInfoParser.getAvcProfile("high@l4.0"), "high");
		assertEquals(LibMediaInfoParser.getAvcProfile("hIgH@L4.1"), "high");
		assertEquals(LibMediaInfoParser.getAvcProfile("LOW@L4.1"), "low");
	}

	@Test
	public void testGetAvcProfileInvalidInput() throws Exception {
		assertNull(LibMediaInfoParser.getAvcProfile("@L2.0"));
		assertNull(LibMediaInfoParser.getAvcProfile("@l2.0"));
	}

	@Test
	public void testGetBitrate() throws Exception {
		assertEquals(LibMediaInfoParser.getBitrate("256"), 256);
		assertEquals(LibMediaInfoParser.getBitrate("128/192"), 128);
	}

	@Test
	public void testGetBitrateInvalidInput() throws Exception {
		assertEquals(LibMediaInfoParser.getBitrate(""), 0);
		assertEquals(LibMediaInfoParser.getBitrate("asfd"), 0);
	}

	@Test
	public void testGetSpecificID() throws Exception {
		assertEquals(LibMediaInfoParser.getSpecificID("256"), 256);
		assertEquals(LibMediaInfoParser.getSpecificID("189 (0xBD)-32 (0x80)"), 32);
		assertEquals(LibMediaInfoParser.getSpecificID("189 (0xBD)"), 189);
		assertEquals(LibMediaInfoParser.getSpecificID("189 (0xBD)-"), 189);
	}

	@Test
	public void testGetSampleFrequency() throws Exception {
		assertEquals(LibMediaInfoParser.getSampleFrequency("44100"), "44100");
		assertEquals(LibMediaInfoParser.getSampleFrequency("24000khz"), "24000");
		assertEquals(LibMediaInfoParser.getSampleFrequency("48000 / 44100"), "48000");
	}

	@Test
	public void testGetFPSValue() throws Exception {
		assertEquals(LibMediaInfoParser.getFPSValue("30"), "30");
		assertEquals(LibMediaInfoParser.getFPSValue("30fps"), "30");
	}

	@Test
	public void testGetFrameRateModeValue() throws Exception {
		assertEquals(LibMediaInfoParser.getFrameRateModeValue("VBR"), "VBR");
		assertEquals(LibMediaInfoParser.getFrameRateModeValue("CBR/VBR"), "CBR");
	}

	@Test
	public void testGetLang() throws Exception {
		assertEquals(LibMediaInfoParser.getLang("enUS"), "enUS");
		assertEquals(LibMediaInfoParser.getLang("ptBR (Brazil)"), "ptBR");
		assertEquals(LibMediaInfoParser.getLang("enUS/GB"), "enUS");
	}

	@Test
	public void testSetFormat() throws Exception {
		DLNAMediaInfo media = new DLNAMediaInfo();
		DLNAMediaAudio audio = new DLNAMediaAudio();
		LibMediaInfoParser.setFormat(StreamType.General, media, audio, "XVID", null);
		assertEquals(FormatConfiguration.DIVX, media.getContainer());
		LibMediaInfoParser.setFormat(StreamType.Video, media, audio, "XVID", null);
		assertEquals(FormatConfiguration.DIVX, media.getCodecV());
		media.setContainer("");
		LibMediaInfoParser.setFormat(StreamType.General, media, audio, "mp42 (mp42/isom)", null);
		assertEquals(FormatConfiguration.MP4, media.getContainer());
		media.setCodecV("");
		LibMediaInfoParser.setFormat(StreamType.Video, media, audio, "DIVX", null);
		assertEquals(FormatConfiguration.DIVX, media.getCodecV());
		// TODO this can continue with other container, video and audio formats
	}
}
