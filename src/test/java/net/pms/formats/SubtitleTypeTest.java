/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  I. Sokolov
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
package net.pms.formats;

import net.pms.formats.v2.SubtitleType;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class SubtitleTypeTest {
	@Test
	public void testValueOfFileExtension_matchingExtensions() throws Exception {
		assertThat(SubtitleType.valueOfFileExtension("srt")).isEqualTo(SubtitleType.SUBRIP);
		assertThat(SubtitleType.valueOfFileExtension("txt")).isEqualTo(SubtitleType.TEXT);
		assertThat(SubtitleType.valueOfFileExtension("sub")).isEqualTo(SubtitleType.MICRODVD);
		assertThat(SubtitleType.valueOfFileExtension("smi")).isEqualTo(SubtitleType.SAMI);
		assertThat(SubtitleType.valueOfFileExtension("ssa")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfFileExtension("ass")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfFileExtension("idx")).isEqualTo(SubtitleType.VOBSUB);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_matchingCodecs() throws Exception {
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("s_utf8")).isEqualTo(SubtitleType.SUBRIP);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_TEXT/UTF8")).isEqualTo(SubtitleType.SUBRIP);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("Subrip")).isEqualTo(SubtitleType.SUBRIP);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("s_ssa")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("s_ass")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_TEXT/SSA")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_TEXT/ASS")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("SSA")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("ASS")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("subp")).isEqualTo(SubtitleType.VOBSUB);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_VOBSUB")).isEqualTo(SubtitleType.VOBSUB);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("mp4s")).isEqualTo(SubtitleType.VOBSUB);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("E0")).isEqualTo(SubtitleType.VOBSUB);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("s_usf")).isEqualTo(SubtitleType.USF);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_TEXT/USF")).isEqualTo(SubtitleType.USF);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_IMAGE/BMP")).isEqualTo(SubtitleType.BMP);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("DXSB")).isEqualTo(SubtitleType.DIVX);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("tx3g")).isEqualTo(SubtitleType.TX3G);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("pgs")).isEqualTo(SubtitleType.PGS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_HDMV/PGS")).isEqualTo(SubtitleType.PGS);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("144")).isEqualTo(SubtitleType.PGS);
	}

	@Test
	public void testGetDescription() throws Exception {
		assertThat(SubtitleType.UNKNOWN.getDescription()).isEqualTo("Generic");
		assertThat(SubtitleType.UNSUPPORTED.getDescription()).isEqualTo("Unsupported");
		assertThat(SubtitleType.SUBRIP.getDescription()).isEqualTo("SubRip");
		assertThat(SubtitleType.TEXT.getDescription()).isEqualTo("Text file");
		assertThat(SubtitleType.MICRODVD.getDescription()).isEqualTo("MicroDVD");
		assertThat(SubtitleType.SAMI.getDescription()).isEqualTo("SAMI");
		assertThat(SubtitleType.ASS.getDescription()).isEqualTo("(Advanced) SubStation Alpha");
		assertThat(SubtitleType.VOBSUB.getDescription()).isEqualTo("VobSub");
		assertThat(SubtitleType.USF.getDescription()).isEqualTo("Universal Subtitle Format");
		assertThat(SubtitleType.BMP.getDescription()).isEqualTo("BMP");
		assertThat(SubtitleType.DIVX.getDescription()).isEqualTo("DIVX subtitles");
		assertThat(SubtitleType.TX3G.getDescription()).isEqualTo("Timed text (TX3G)");
		assertThat(SubtitleType.PGS.getDescription()).isEqualTo("Blu-ray subtitles");
	}

	@Test
	public void testGetExtension() throws Exception {
		assertThat(SubtitleType.UNKNOWN.getExtension()).isEqualTo("");
		assertThat(SubtitleType.SUBRIP.getExtension()).isEqualTo("srt");
		assertThat(SubtitleType.TEXT.getExtension()).isEqualTo("txt");
		assertThat(SubtitleType.MICRODVD.getExtension()).isEqualTo("sub");
		assertThat(SubtitleType.SAMI.getExtension()).isEqualTo("smi");
		assertThat(SubtitleType.ASS.getExtension()).isEqualTo("ass");
		assertThat(SubtitleType.VOBSUB.getExtension()).isEqualTo("idx");
		assertThat(SubtitleType.UNSUPPORTED.getExtension()).isEqualTo("");
	}

	@Test
	public void testValueOfFileExtension_nullOrBlankExtension() throws Exception {
		assertThat(SubtitleType.valueOfFileExtension(null)).isEqualTo(SubtitleType.UNKNOWN);
		assertThat(SubtitleType.valueOfFileExtension("")).isEqualTo(SubtitleType.UNKNOWN);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_nullOrBlankCodec() throws Exception {
		assertThat(SubtitleType.valueOfLibMediaInfoCodec(null)).isEqualTo(SubtitleType.UNKNOWN);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("")).isEqualTo(SubtitleType.UNKNOWN);
	}

	@Test
	public void testValueOfFileExtension_unknownExtension() throws Exception {
		assertThat(SubtitleType.valueOfFileExtension("xyz")).isEqualTo(SubtitleType.UNKNOWN);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_unknownCodec() throws Exception {
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("xyz")).isEqualTo(SubtitleType.UNKNOWN);
	}

	@Test
	public void testValueOfFileExtension_extensionCaseInsensitivity() throws Exception {
		assertThat(SubtitleType.valueOfFileExtension("ssA")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfFileExtension("SSA")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.valueOfFileExtension("sSa")).isEqualTo(SubtitleType.ASS);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_CodecInsensitivity() throws Exception {
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("s_TeXT/UtF8")).isEqualTo(SubtitleType.SUBRIP);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_CodecWithExtraSpaces() throws Exception {
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("s_utf8 ")).isEqualTo(SubtitleType.SUBRIP);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("   s_utf8")).isEqualTo(SubtitleType.SUBRIP);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("	s_utf8 ")).isEqualTo(SubtitleType.SUBRIP);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_SubstringShouldNotMatch() throws Exception {
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("S_TEXT/SSA2")).isEqualTo(SubtitleType.UNKNOWN);
		assertThat(SubtitleType.valueOfLibMediaInfoCodec("ps_utf8")).isEqualTo(SubtitleType.UNKNOWN);
	}

	@Test
	public void getSupportedFileExtensions() {
		Set<String> expectedExtensionsSet = new HashSet<String>(Arrays.asList("srt", "txt", "sub", "smi", "ssa", "ass", "idx"));
		assertThat(SubtitleType.getSupportedFileExtensions()).isEqualTo(expectedExtensionsSet);
	}

	@Test
	public void testGetStableIndex() {
		assertThat(SubtitleType.UNKNOWN.getStableIndex()).isEqualTo(0);
		assertThat(SubtitleType.SUBRIP.getStableIndex()).isEqualTo(1);
		assertThat(SubtitleType.TEXT.getStableIndex()).isEqualTo(2);
		assertThat(SubtitleType.MICRODVD.getStableIndex()).isEqualTo(3);
		assertThat(SubtitleType.SAMI.getStableIndex()).isEqualTo(4);
		assertThat(SubtitleType.ASS.getStableIndex()).isEqualTo(5);
		assertThat(SubtitleType.VOBSUB.getStableIndex()).isEqualTo(6);
		assertThat(SubtitleType.UNSUPPORTED.getStableIndex()).isEqualTo(7);
		assertThat(SubtitleType.USF.getStableIndex()).isEqualTo(8);
		assertThat(SubtitleType.BMP.getStableIndex()).isEqualTo(9);
		assertThat(SubtitleType.DIVX.getStableIndex()).isEqualTo(10);
		assertThat(SubtitleType.TX3G.getStableIndex()).isEqualTo(11);
		assertThat(SubtitleType.PGS.getStableIndex()).isEqualTo(12);
	}

	@Test
	public void testGetStableIndex_uniqueness() {
		Set<Integer> stableIndexes = new HashSet<Integer>();
		for (SubtitleType subtitleType : SubtitleType.values()) {
			assertThat(stableIndexes.contains(subtitleType.getStableIndex())).isFalse();
			stableIndexes.add(subtitleType.getStableIndex());
		}
	}
}