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
package net.pms.formats.v2;

import java.util.HashSet;
import java.util.Set;
import static net.pms.formats.v2.SubtitleType.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SubtitleTypeTest {

	@Test
	public void testValueOfFileExtension_matchingExtensions() throws Exception {
		assertEquals(valueOfFileExtension("srt"), SUBRIP);
		assertEquals(valueOfFileExtension("txt"), TEXT);
		assertEquals(valueOfFileExtension("sub"), MICRODVD);
		assertEquals(valueOfFileExtension("smi"), SAMI);
		assertEquals(valueOfFileExtension("ssa"), ASS);
		assertEquals(valueOfFileExtension("ass"), ASS);
		assertEquals(valueOfFileExtension("idx"), VOBSUB);
		assertEquals(valueOfFileExtension("vtt"), WEBVTT);
		assertEquals(valueOfFileExtension("sup"), PGS);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_matchingCodecs() throws Exception {
		assertEquals(valueOfMediaInfoValue("s_utf8"), SUBRIP);
		assertEquals(valueOfMediaInfoValue("S_TEXT/UTF8"), SUBRIP);
		assertEquals(valueOfMediaInfoValue("Subrip"), SUBRIP);
		assertEquals(valueOfMediaInfoValue("s_ssa"), ASS);
		assertEquals(valueOfMediaInfoValue("s_ass"), ASS);
		assertEquals(valueOfMediaInfoValue("S_TEXT/SSA"), ASS);
		assertEquals(valueOfMediaInfoValue("S_TEXT/ASS"), ASS);
		assertEquals(valueOfMediaInfoValue("SSA"), ASS);
		assertEquals(valueOfMediaInfoValue("ASS"), ASS);
		assertEquals(valueOfMediaInfoValue("subp"), VOBSUB);
		assertEquals(valueOfMediaInfoValue("S_VOBSUB"), VOBSUB);
		assertEquals(valueOfMediaInfoValue("mp4s"), VOBSUB);
		assertEquals(valueOfMediaInfoValue("E0"), VOBSUB);
		assertEquals(valueOfMediaInfoValue("s_usf"), USF);
		assertEquals(valueOfMediaInfoValue("S_TEXT/USF"), USF);
		assertEquals(valueOfMediaInfoValue("S_IMAGE/BMP"), BMP);
		assertEquals(valueOfMediaInfoValue("DXSB"), DIVX);
		assertEquals(valueOfMediaInfoValue("tx3g"), TX3G);
		assertEquals(valueOfMediaInfoValue("pgs"), PGS);
		assertEquals(valueOfMediaInfoValue("S_HDMV/PGS"), PGS);
		assertEquals(valueOfMediaInfoValue("144"), PGS);
		assertEquals(valueOfMediaInfoValue("WebVTT"), WEBVTT);
		assertEquals(valueOfMediaInfoValue("S_TEXT/WEBVTT"), WEBVTT);
		assertEquals(valueOfMediaInfoValue("S_HDMV/TEXTST"), TEXTST);
		assertEquals(valueOfMediaInfoValue("S_DVBSUB"), DVBSUB);
		assertEquals(valueOfMediaInfoValue("EIA-608"), EIA608);
//		assertEquals(valueOfMediaInfoValue("EIA-708"), EIA708);

	}

	@Test
	public void testGetDescription() throws Exception {
		assertEquals(UNKNOWN.getDescription(), "Generic");
		assertEquals(UNSUPPORTED.getDescription(), "Unsupported");
		assertEquals(SUBRIP.getDescription(), "SubRip");
		assertEquals(TEXT.getDescription(), "Text");
		assertEquals(MICRODVD.getDescription(), "MicroDVD");
		assertEquals(SAMI.getDescription(), "Synchronized Accessible Media Interchange");
		assertEquals(ASS.getDescription(), "(Advanced) Sub Station Alpha");
		assertEquals(VOBSUB.getDescription(), "VobSub");
		assertEquals(USF.getDescription(), "Universal Subtitle Format");
		assertEquals(BMP.getDescription(), "Bitmap");
		assertEquals(DIVX.getDescription(), "DivX subtitles");
		assertEquals(TX3G.getDescription(), "3GPP Timed Text");
		assertEquals(PGS.getDescription(), "Presentation Graphic Stream");
		assertEquals(WEBVTT.getDescription(), "Web Video Text Tracks");
		assertEquals(TEXTST.getDescription(), "HDMV Text SubTitles");
		assertEquals(DVBSUB.getDescription(), "DVB Subtitles");
		assertEquals(EIA608.getDescription(), "EIA-608 subtitles");
//		assertEquals(EIA708.getDescription(), "EIA-708 subtitles");
	}

	@Test
	public void testGetExtension() throws Exception {
		assertEquals(UNKNOWN.getExtension(), "");
		assertEquals(SUBRIP.getExtension(), "srt");
		assertEquals(TEXT.getExtension(), "txt");
		assertEquals(MICRODVD.getExtension(), "sub");
		assertEquals(SAMI.getExtension(), "smi");
		assertEquals(ASS.getExtension(), "ass");
		assertEquals(VOBSUB.getExtension(), "idx");
		assertEquals(UNSUPPORTED.getExtension(), "");
		assertEquals(WEBVTT.getExtension(), "vtt");
		assertEquals(PGS.getExtension(), "sup");
	}

	@Test
	public void testValueOfFileExtension_nullOrBlankExtension() throws Exception {
		assertEquals(valueOfFileExtension(null), UNKNOWN);
		assertEquals(valueOfFileExtension(""), UNKNOWN);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_nullOrBlankCodec() throws Exception {
		assertEquals(valueOfMediaInfoValue(null), UNKNOWN);
		assertEquals(valueOfMediaInfoValue(""), UNKNOWN);
	}

	@Test
	public void testValueOfFileExtension_unknownExtension() throws Exception {
		assertEquals(valueOfFileExtension("xyz"), UNKNOWN);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_unknownCodec() throws Exception {
		assertEquals(valueOfMediaInfoValue("xyz"), UNKNOWN);
	}

	@Test
	public void testValueOfFileExtension_extensionCaseInsensitivity() throws Exception {
		assertEquals(valueOfFileExtension("ssA"), ASS);
		assertEquals(valueOfFileExtension("SSA"), ASS);
		assertEquals(valueOfFileExtension("sSa"), ASS);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_CodecInsensitivity() throws Exception {
		assertEquals(valueOfMediaInfoValue("s_TeXT/UtF8"), SUBRIP);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_CodecWithExtraSpaces() throws Exception {
		assertEquals(valueOfMediaInfoValue("s_utf8 "), SUBRIP);
		assertEquals(valueOfMediaInfoValue("   s_utf8"), SUBRIP);
		assertEquals(valueOfMediaInfoValue("	s_utf8 "), SUBRIP);
	}

	@Test
	public void testValueOfLibMediaInfoCodec_SubstringShouldNotMatch() throws Exception {
		assertEquals(valueOfMediaInfoValue("S_TEXT/SSA2"), UNKNOWN);
		assertEquals(valueOfMediaInfoValue("ps_utf8"), UNKNOWN);
	}

	@Test
	public void getSupportedFileExtensions() {
		Set<String> expectedExtensionsSet = Set.of("srt", "txt", "sub", "smi", "ssa", "ass", "idx", "vtt", "sup");
		assertEquals(SubtitleType.getSupportedFileExtensions(), expectedExtensionsSet);
	}

	@Test
	public void testGetStableIndex() {
		assertEquals(UNKNOWN.getStableIndex(), 0);
		assertEquals(SUBRIP.getStableIndex(), 1);
		assertEquals(TEXT.getStableIndex(), 2);
		assertEquals(MICRODVD.getStableIndex(), 3);
		assertEquals(SAMI.getStableIndex(), 4);
		assertEquals(ASS.getStableIndex(), 5);
		assertEquals(VOBSUB.getStableIndex(), 6);
		assertEquals(UNSUPPORTED.getStableIndex(), 7);
		assertEquals(USF.getStableIndex(), 8);
		assertEquals(BMP.getStableIndex(), 9);
		assertEquals(DIVX.getStableIndex(), 10);
		assertEquals(TX3G.getStableIndex(), 11);
		assertEquals(PGS.getStableIndex(), 12);
		assertEquals(WEBVTT.getStableIndex(), 13);
		assertEquals(TEXTST.getStableIndex(), 14);
		assertEquals(DVBSUB.getStableIndex(), 15);
		assertEquals(EIA608.getStableIndex(), 16);
//		assertEquals(EIA708.getStableIndex(), 17);
	}

	@Test
	public void testGetStableIndex_uniqueness() {
		Set<Integer> stableIndexes = new HashSet<>();
		for (SubtitleType subtitleType : values()) {
			assertFalse(stableIndexes.contains(subtitleType.getStableIndex()));
			stableIndexes.add(subtitleType.getStableIndex());
		}
	}

	@Test
	public void testValueOfStableIndex() {
		assertEquals(valueOfStableIndex(0), UNKNOWN);
		assertEquals(valueOfStableIndex(1), SUBRIP);
		assertEquals(valueOfStableIndex(2), TEXT);
		assertEquals(valueOfStableIndex(3), MICRODVD);
		assertEquals(valueOfStableIndex(4), SAMI);
		assertEquals(valueOfStableIndex(5), ASS);
		assertEquals(valueOfStableIndex(6), VOBSUB);
		assertEquals(valueOfStableIndex(7), UNSUPPORTED);
		assertEquals(valueOfStableIndex(8), USF);
		assertEquals(valueOfStableIndex(9), BMP);
		assertEquals(valueOfStableIndex(10), DIVX);
		assertEquals(valueOfStableIndex(11), TX3G);
		assertEquals(valueOfStableIndex(12), PGS);
		assertEquals(valueOfStableIndex(13), WEBVTT);
		assertEquals(valueOfStableIndex(14), TEXTST);
		assertEquals(valueOfStableIndex(15), DVBSUB);
		assertEquals(valueOfStableIndex(16), EIA608);
//		assertEquals(valueOfStableIndex(17), EIA708);
	}

	@Test
	public void testValueOfStableIndex_unknownIndex() {
		assertEquals(valueOfStableIndex(-1), UNKNOWN);
		assertEquals(valueOfStableIndex(456), UNKNOWN);
	}
}
