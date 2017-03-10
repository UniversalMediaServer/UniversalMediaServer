/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2013  I. Sokolov
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
package net.pms.dlna;

import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

public class LibMediaInfoParserTest {
	@Test
	public void testGetReferenceFrameCount() throws Exception {
		assertThat(LibMediaInfoParser.getReferenceFrameCount("-5 6")).isEqualTo((byte) -5);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("7")).isEqualTo((byte) 7);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("2 frame2")).isEqualTo((byte) 2);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("-16 frame3")).isEqualTo((byte) -16);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("")).isEqualTo((byte) -1);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("strange1")).isEqualTo((byte) -1);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("6ref")).isEqualTo((byte) -1);
	}

	@Test
	public void testGetBitrate() throws Exception {
		assertThat(LibMediaInfoParser.getBitrate("256")).isEqualTo(256);
		assertThat(LibMediaInfoParser.getBitrate("128/192")).isEqualTo(128);
	}

	@Test
	public void testGetBitrateInvalidInput() throws Exception {
		assertThat(LibMediaInfoParser.getBitrate("")).isEqualTo(0);
		assertThat(LibMediaInfoParser.getBitrate("asfd")).isEqualTo(0);
	}

	@Test
	public void testGetSpecificID() throws Exception {
		assertThat(LibMediaInfoParser.getSpecificID("256")).isEqualTo(256);
		assertThat(LibMediaInfoParser.getSpecificID("189 (0xBD)-32 (0x80)")).isEqualTo(32);
		assertThat(LibMediaInfoParser.getSpecificID("189 (0xBD)")).isEqualTo(189);
		assertThat(LibMediaInfoParser.getSpecificID("189 (0xBD)-")).isEqualTo(189);
	}

	@Test
	public void testGetSampleFrequency() throws Exception {
		assertThat(LibMediaInfoParser.getSampleFrequency("44100")).isEqualTo("44100");
		assertThat(LibMediaInfoParser.getSampleFrequency("24000khz")).isEqualTo("24000");
		assertThat(LibMediaInfoParser.getSampleFrequency("48000 / 44100")).isEqualTo("48000");
	}

	@Test
	public void testGetFPSValue() throws Exception {
		assertThat(LibMediaInfoParser.getFPSValue("30")).isEqualTo("30");
		assertThat(LibMediaInfoParser.getFPSValue("30fps")).isEqualTo("30");
	}

	@Test
	public void testGetFrameRateModeValue() throws Exception {
		assertThat(LibMediaInfoParser.getFrameRateModeValue("VBR")).isEqualTo("VBR");
		assertThat(LibMediaInfoParser.getFrameRateModeValue("CBR/VBR")).isEqualTo("CBR");
	}

	@Test
	public void testGetLang() throws Exception {
		assertThat(LibMediaInfoParser.getLang("enUS")).isEqualTo("enUS");
		assertThat(LibMediaInfoParser.getLang("ptBR (Brazil)")).isEqualTo("ptBR");
		assertThat(LibMediaInfoParser.getLang("enUS/GB")).isEqualTo("enUS");
	}
}
