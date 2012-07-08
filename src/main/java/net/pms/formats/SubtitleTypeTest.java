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

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class SubtitleTypeTest {
	@Test
	public void testGetSubtitleTypeByFileExtension_matchingExtensions() throws Exception {
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("srt")).isEqualTo(SubtitleType.SUBRIP);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("txt")).isEqualTo(SubtitleType.TEXT);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("sub")).isEqualTo(SubtitleType.MICRODVD);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("smi")).isEqualTo(SubtitleType.SAMI);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("ssa")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("ass")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("idx")).isEqualTo(SubtitleType.VOBSUB);
	}

	@Test
	public void testGetDescription() throws Exception {
		assertThat(SubtitleType.UNKNOWN.getDescription()).isEqualTo("Unknown");
		assertThat(SubtitleType.EMBEDDED.getDescription()).isEqualTo("Embedded");
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
		assertThat(SubtitleType.EMBEDDED.getExtension()).isEqualTo("");
		assertThat(SubtitleType.UNSUPPORTED.getExtension()).isEqualTo("");
	}

	@Test
	public void testGetSubtitleTypeByFileExtension_nullOrBlankExtension() throws Exception {
		assertThat(SubtitleType.getSubtitleTypeByFileExtension(null)).isEqualTo(SubtitleType.UNKNOWN);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("")).isEqualTo(SubtitleType.UNKNOWN);
	}

	@Test
	public void testGetSubtitleTypeByFileExtension_unknownExtension() throws Exception {
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("xyz")).isEqualTo(SubtitleType.UNKNOWN);
	}

	@Test
	public void testGetSubtitleTypeByFileExtension_extensionCaseInsensitivity() throws Exception {
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("ssA")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("SSA")).isEqualTo(SubtitleType.ASS);
		assertThat(SubtitleType.getSubtitleTypeByFileExtension("sSa")).isEqualTo(SubtitleType.ASS);
	}

	@Test
	public void getSupportedFileExtensions() {
		Set<String> expectedExtensionsSet = new HashSet<String>(Arrays.asList("srt", "txt", "sub", "smi", "ssa", "ass", "idx"));
		assertThat(SubtitleType.getSupportedFileExtensions()).isEqualTo(expectedExtensionsSet);
	}
}