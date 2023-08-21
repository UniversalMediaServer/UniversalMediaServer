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
package net.pms.parsers;

import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.parsers.mediainfo.StreamKind;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MediaInfoParserTest {
	@BeforeAll
	public static void SetUPClass() {
		PMS.configureJNA();
	}

	@Test
	public void testGetReferenceFrameCount() throws Exception {
		assertEquals(MediaInfoParser.getReferenceFrameCount("-5 6"), (byte) -5);
		assertEquals(MediaInfoParser.getReferenceFrameCount("7"), (byte) 7);
		assertEquals(MediaInfoParser.getReferenceFrameCount("2 frame2"), (byte) 2);
		assertEquals(MediaInfoParser.getReferenceFrameCount("-16 frame3"), (byte) -16);
		assertEquals(MediaInfoParser.getReferenceFrameCount(""), (byte) -1);
		assertEquals(MediaInfoParser.getReferenceFrameCount("strange1"), (byte) -1);
		assertEquals(MediaInfoParser.getReferenceFrameCount("6ref"), (byte) -1);
	}

	@Test
	public void testGetAvcLevel() throws Exception {
		assertEquals(MediaInfoParser.getAvcLevel("Main@L2.0"), "2.0");
		assertEquals(MediaInfoParser.getAvcLevel("High@L3.0"), "3.0");
		assertEquals(MediaInfoParser.getAvcLevel("high@l4.0"), "4.0");
		assertEquals(MediaInfoParser.getAvcLevel("hIgH@L4.1"), "4.1");
		assertNull(MediaInfoParser.getAvcLevel("5.1"));
		assertNull(MediaInfoParser.getAvcLevel("level5"));
	}

	@Test
	public void testGetAvcProfile() throws Exception {
		assertEquals(MediaInfoParser.getAvcProfile("Main@L2.0"), "main");
		assertEquals(MediaInfoParser.getAvcProfile("High@L3.0"), "high");
		assertEquals(MediaInfoParser.getAvcProfile("high@l4.0"), "high");
		assertEquals(MediaInfoParser.getAvcProfile("hIgH@L4.1"), "high");
		assertEquals(MediaInfoParser.getAvcProfile("LOW@L4.1"), "low");
	}

	@Test
	public void testGetAvcProfileInvalidInput() throws Exception {
		assertNull(MediaInfoParser.getAvcProfile("@L2.0"));
		assertNull(MediaInfoParser.getAvcProfile("@l2.0"));
	}

	@Test
	public void testGetBitrate() throws Exception {
		assertEquals(MediaInfoParser.getBitrate("256"), 256);
		assertEquals(MediaInfoParser.getBitrate("128/192"), 128);
	}

	@Test
	public void testGetBitrateInvalidInput() throws Exception {
		assertEquals(MediaInfoParser.getBitrate(""), 0);
		assertEquals(MediaInfoParser.getBitrate("asfd"), 0);
	}

	@Test
	public void testGetSpecificID() throws Exception {
		assertEquals(MediaInfoParser.getSpecificID("256"), 256);
		assertEquals(MediaInfoParser.getSpecificID("189 (0xBD)-32 (0x80)"), 32);
		assertEquals(MediaInfoParser.getSpecificID("189 (0xBD)"), 189);
		assertEquals(MediaInfoParser.getSpecificID("189 (0xBD)-"), 189);
	}

	@Test
	public void testGetSampleFrequency() throws Exception {
		assertEquals(MediaInfoParser.getSampleFrequency("44100"), "44100");
		assertEquals(MediaInfoParser.getSampleFrequency("24000khz"), "24000");
		assertEquals(MediaInfoParser.getSampleFrequency("48000 / 44100"), "48000");
	}

	@Test
	public void testGetFPSValue() throws Exception {
		assertEquals(MediaInfoParser.getFPSValue("30"), "30");
		assertEquals(MediaInfoParser.getFPSValue("30fps"), "30");
	}

	@Test
	public void testGetFrameRateModeValue() throws Exception {
		assertEquals(MediaInfoParser.getFrameRateModeValue("VBR"), "VBR");
		assertEquals(MediaInfoParser.getFrameRateModeValue("CBR/VBR"), "CBR");
	}

	@Test
	public void testGetLang() throws Exception {
		assertEquals(MediaInfoParser.getLang("enUS"), "enUS");
		assertEquals(MediaInfoParser.getLang("ptBR (Brazil)"), "ptBR");
		assertEquals(MediaInfoParser.getLang("enUS/GB"), "enUS");
	}

	@Test
	public void testSetFormat() throws Exception {
		MediaInfo media = new MediaInfo();
		MediaAudio audio = new MediaAudio();
		MediaInfoParser.setFormat(StreamKind.GENERAL, media, audio, "XVID", null);
		assertEquals(FormatConfiguration.DIVX, media.getContainer());
		MediaInfoParser.setFormat(StreamKind.VIDEO, media, audio, "XVID", null);
		assertEquals(FormatConfiguration.DIVX, media.getCodecV());
		media.setContainer("");
		MediaInfoParser.setFormat(StreamKind.GENERAL, media, audio, "mp42 (mp42/isom)", null);
		assertEquals(FormatConfiguration.MP4, media.getContainer());
		media.setCodecV("");
		MediaInfoParser.setFormat(StreamKind.VIDEO, media, audio, "DIVX", null);
		assertEquals(FormatConfiguration.DIVX, media.getCodecV());
		// TODO this can continue with other container, video and audio formats
	}
}
