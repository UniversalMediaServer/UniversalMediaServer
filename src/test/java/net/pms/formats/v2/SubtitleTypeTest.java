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
package net.pms.formats.v2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static net.pms.formats.v2.SubtitleType.*;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

public class SubtitleTypeTest {
	@Test
	public void testValueOfFileExtension_matchingExtensions() throws Exception {
		assertThat(valueOfFileExtension("srt")).isEqualTo(SUBRIP);
		assertThat(valueOfFileExtension("txt")).isEqualTo(TEXT);
		assertThat(valueOfFileExtension("sub")).isEqualTo(MICRODVD);
		assertThat(valueOfFileExtension("smi")).isEqualTo(SAMI);
		assertThat(valueOfFileExtension("ssa")).isEqualTo(ASS);
		assertThat(valueOfFileExtension("ass")).isEqualTo(ASS);
		assertThat(valueOfFileExtension("idx")).isEqualTo(VOBSUB);
		assertThat(valueOfFileExtension("vtt")).isEqualTo(WEBVTT);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_matchingCodecs() throws Exception {
		assertThat(valueOfLibMediaInfoCodec("s_utf8")).isEqualTo(SUBRIP);
		assertThat(valueOfLibMediaInfoCodec("S_TEXT/UTF8")).isEqualTo(SUBRIP);
		assertThat(valueOfLibMediaInfoCodec("Subrip")).isEqualTo(SUBRIP);
		assertThat(valueOfLibMediaInfoCodec("s_ssa")).isEqualTo(ASS);
		assertThat(valueOfLibMediaInfoCodec("s_ass")).isEqualTo(ASS);
		assertThat(valueOfLibMediaInfoCodec("S_TEXT/SSA")).isEqualTo(ASS);
		assertThat(valueOfLibMediaInfoCodec("S_TEXT/ASS")).isEqualTo(ASS);
		assertThat(valueOfLibMediaInfoCodec("SSA")).isEqualTo(ASS);
		assertThat(valueOfLibMediaInfoCodec("ASS")).isEqualTo(ASS);
		assertThat(valueOfLibMediaInfoCodec("subp")).isEqualTo(VOBSUB);
		assertThat(valueOfLibMediaInfoCodec("S_VOBSUB")).isEqualTo(VOBSUB);
		assertThat(valueOfLibMediaInfoCodec("mp4s")).isEqualTo(VOBSUB);
		assertThat(valueOfLibMediaInfoCodec("E0")).isEqualTo(VOBSUB);
		assertThat(valueOfLibMediaInfoCodec("s_usf")).isEqualTo(USF);
		assertThat(valueOfLibMediaInfoCodec("S_TEXT/USF")).isEqualTo(USF);
		assertThat(valueOfLibMediaInfoCodec("S_IMAGE/BMP")).isEqualTo(BMP);
		assertThat(valueOfLibMediaInfoCodec("DXSB")).isEqualTo(DIVX);
		assertThat(valueOfLibMediaInfoCodec("tx3g")).isEqualTo(TX3G);
		assertThat(valueOfLibMediaInfoCodec("pgs")).isEqualTo(PGS);
		assertThat(valueOfLibMediaInfoCodec("S_HDMV/PGS")).isEqualTo(PGS);
		assertThat(valueOfLibMediaInfoCodec("144")).isEqualTo(PGS);
		assertThat(valueOfLibMediaInfoCodec("WebVTT")).isEqualTo(WEBVTT);
	}

	@Test
	public void testGetDescription() throws Exception {
		assertThat(UNKNOWN.getDescription()).isEqualTo("Generic");
		assertThat(UNSUPPORTED.getDescription()).isEqualTo("Unsupported");
		assertThat(SUBRIP.getDescription()).isEqualTo("SubRip");
		assertThat(TEXT.getDescription()).isEqualTo("Text file");
		assertThat(MICRODVD.getDescription()).isEqualTo("MicroDVD");
		assertThat(SAMI.getDescription()).isEqualTo("SAMI");
		assertThat(ASS.getDescription()).isEqualTo("(Advanced) SubStation Alpha");
		assertThat(VOBSUB.getDescription()).isEqualTo("VobSub");
		assertThat(USF.getDescription()).isEqualTo("Universal Subtitle Format");
		assertThat(BMP.getDescription()).isEqualTo("BMP");
		assertThat(DIVX.getDescription()).isEqualTo("DIVX subtitles");
		assertThat(TX3G.getDescription()).isEqualTo("Timed text (TX3G)");
		assertThat(PGS.getDescription()).isEqualTo("Blu-ray subtitles");
	}

	@Test
	public void testGetExtension() throws Exception {
		assertThat(UNKNOWN.getExtension()).isEqualTo("");
		assertThat(SUBRIP.getExtension()).isEqualTo("srt");
		assertThat(TEXT.getExtension()).isEqualTo("txt");
		assertThat(MICRODVD.getExtension()).isEqualTo("sub");
		assertThat(SAMI.getExtension()).isEqualTo("smi");
		assertThat(ASS.getExtension()).isEqualTo("ass");
		assertThat(VOBSUB.getExtension()).isEqualTo("idx");
		assertThat(UNSUPPORTED.getExtension()).isEqualTo("");
		assertThat(WEBVTT.getExtension()).isEqualTo("vtt");
	}

	@Test
	public void testValueOfFileExtension_nullOrBlankExtension() throws Exception {
		assertThat(valueOfFileExtension(null)).isEqualTo(UNKNOWN);
		assertThat(valueOfFileExtension("")).isEqualTo(UNKNOWN);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_nullOrBlankCodec() throws Exception {
		assertThat(valueOfLibMediaInfoCodec(null)).isEqualTo(UNKNOWN);
		assertThat(valueOfLibMediaInfoCodec("")).isEqualTo(UNKNOWN);
	}

	@Test
	public void testValueOfFileExtension_unknownExtension() throws Exception {
		assertThat(valueOfFileExtension("xyz")).isEqualTo(UNKNOWN);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_unknownCodec() throws Exception {
		assertThat(valueOfLibMediaInfoCodec("xyz")).isEqualTo(UNKNOWN);
	}

	@Test
	public void testValueOfFileExtension_extensionCaseInsensitivity() throws Exception {
		assertThat(valueOfFileExtension("ssA")).isEqualTo(ASS);
		assertThat(valueOfFileExtension("SSA")).isEqualTo(ASS);
		assertThat(valueOfFileExtension("sSa")).isEqualTo(ASS);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_CodecInsensitivity() throws Exception {
		assertThat(valueOfLibMediaInfoCodec("s_TeXT/UtF8")).isEqualTo(SUBRIP);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_CodecWithExtraSpaces() throws Exception {
		assertThat(valueOfLibMediaInfoCodec("s_utf8 ")).isEqualTo(SUBRIP);
		assertThat(valueOfLibMediaInfoCodec("   s_utf8")).isEqualTo(SUBRIP);
		assertThat(valueOfLibMediaInfoCodec("	s_utf8 ")).isEqualTo(SUBRIP);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_SubstringShouldNotMatch() throws Exception {
		assertThat(valueOfLibMediaInfoCodec("S_TEXT/SSA2")).isEqualTo(UNKNOWN);
		assertThat(valueOfLibMediaInfoCodec("ps_utf8")).isEqualTo(UNKNOWN);
	}

	@Test
	public void getSupportedFileExtensions() {
		Set<String> expectedExtensionsSet = new HashSet<>(Arrays.asList("srt", "txt", "sub", "smi", "ssa", "ass", "idx", "vtt"));
		assertThat(SubtitleType.getSupportedFileExtensions()).isEqualTo(expectedExtensionsSet);
	}

	@Test
	public void testGetStableIndex() {
		assertThat(UNKNOWN.getStableIndex()).isEqualTo(0);
		assertThat(SUBRIP.getStableIndex()).isEqualTo(1);
		assertThat(TEXT.getStableIndex()).isEqualTo(2);
		assertThat(MICRODVD.getStableIndex()).isEqualTo(3);
		assertThat(SAMI.getStableIndex()).isEqualTo(4);
		assertThat(ASS.getStableIndex()).isEqualTo(5);
		assertThat(VOBSUB.getStableIndex()).isEqualTo(6);
		assertThat(UNSUPPORTED.getStableIndex()).isEqualTo(7);
		assertThat(USF.getStableIndex()).isEqualTo(8);
		assertThat(BMP.getStableIndex()).isEqualTo(9);
		assertThat(DIVX.getStableIndex()).isEqualTo(10);
		assertThat(TX3G.getStableIndex()).isEqualTo(11);
		assertThat(PGS.getStableIndex()).isEqualTo(12);
		assertThat(WEBVTT.getStableIndex()).isEqualTo(13);
	}

	@Test
	public void testGetStableIndex_uniqueness() {
		Set<Integer> stableIndexes = new HashSet<>();
		for (SubtitleType subtitleType : values()) {
			assertThat(stableIndexes.contains(subtitleType.getStableIndex())).isFalse();
			stableIndexes.add(subtitleType.getStableIndex());
		}
	}

	@Test
	public void testValueOfStableIndex() {
		assertThat(valueOfStableIndex(0)).isEqualTo(UNKNOWN);
		assertThat(valueOfStableIndex(1)).isEqualTo(SUBRIP);
		assertThat(valueOfStableIndex(2)).isEqualTo(TEXT);
		assertThat(valueOfStableIndex(3)).isEqualTo(MICRODVD);
		assertThat(valueOfStableIndex(4)).isEqualTo(SAMI);
		assertThat(valueOfStableIndex(5)).isEqualTo(ASS);
		assertThat(valueOfStableIndex(6)).isEqualTo(VOBSUB);
		assertThat(valueOfStableIndex(7)).isEqualTo(UNSUPPORTED);
		assertThat(valueOfStableIndex(8)).isEqualTo(USF);
		assertThat(valueOfStableIndex(9)).isEqualTo(BMP);
		assertThat(valueOfStableIndex(10)).isEqualTo(DIVX);
		assertThat(valueOfStableIndex(11)).isEqualTo(TX3G);
		assertThat(valueOfStableIndex(12)).isEqualTo(PGS);
		assertThat(valueOfStableIndex(13)).isEqualTo(WEBVTT);
	}

	@Test
	public void testValueOfStableIndex_unknownIndex() {
		assertThat(valueOfStableIndex(-1)).isEqualTo(UNKNOWN);
		assertThat(valueOfStableIndex(456)).isEqualTo(UNKNOWN);
	}
}