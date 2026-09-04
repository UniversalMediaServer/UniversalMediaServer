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

import java.io.File;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JaudiotaggerParserTest {

	@BeforeAll
	public static void setUpClass() {
		ParserTest.setUpClass();
	}

	private static MediaInfo getTestFileMediaInfo(String testFile) {
		File file = ParserTest.getTestFile(testFile);
		Format format = FormatFactory.getAssociatedFormat(file.getAbsolutePath());
		MediaInfo mediaInfo = new MediaInfo();
		JaudiotaggerParser.parse(mediaInfo, file, format);
		return mediaInfo;
	}

	@Test
	public void testParser() throws Exception {

		assertEquals(
				"Container: WAV, Size: 1073218, Overall Bitrate: 256, Duration: 0:00:34.000, Audio Tracks: 1 [Audio Id: 0, Codec: LPCM, Bitrate: 0, Channels: 2, Sample Frequency: 8000 Hz], Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Track: 1, Genre: Cinematic, Mime Type: audio/wav",
				getTestFileMediaInfo("audio-lpcm.wav").toString()
		);
		assertEquals(
				"Container: OGA, Size: 1089524, Overall Bitrate: 120, Duration: 0:01:14.000, Audio Tracks: 1 [Audio Id: 0, Codec: Vorbis, Bitrate: 0, Channels: 2, Sample Frequency: 32000 Hz], Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Track: 1, Genre: Cinematic, Mime Type: audio/ogg",
				getTestFileMediaInfo("audio-vorbis.oga").toString()
		);
		assertEquals(
				"Container: MP3, Size: 764176, Overall Bitrate: 224, Duration: 0:00:27.000, Audio Tracks: 1 [Audio Id: 0, Codec: MP3, Bitrate: 0, Channels: 2, Sample Frequency: 32000 Hz], Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Track: 1, Genre: Cinematic, Mime Type: audio/mpeg",
				getTestFileMediaInfo("audio-mp3.mp3").toString()
		);
		assertEquals(
				"Container: MP3, Size: 107290, Overall Bitrate: 206, Duration: 0:00:04.000, Audio Tracks: 1 [Audio Id: 0, Codec: MP3, Bitrate: 0, Channels: 2, Sample Frequency: 44100 Hz], Artist: Test Performer1/Performer2, Composer: Test Composer1/Composer2, Conductor: Test Conductor, Album: Test Album, Album Artist: Test AlbumPerformer, Track Name: Test Title, Year: 2023, Track: 12, Genre: Rock & Roll, Mime Type: audio/mpeg",
				getTestFileMediaInfo("audio-mp3-infos.mp3").toString()
		);
		assertEquals(
				"Container: FLAC, Size: 3208022, Overall Bitrate: 1231, Duration: 0:00:21.000, Audio Tracks: 1 [Audio Id: 0, Codec: FLAC, Bitrate: 0, Bits per Sample: 24, Channels: 2, Sample Frequency: 48000 Hz], Track Name: audio-flac24.flac, Track: 1, Mime Type: audio/flac",
				getTestFileMediaInfo("audio-flac24.flac").toString()
		);

		//"audio-real.ra" will cause a NullPointerException on getSampleRateAsNumber, so the library need  to be updated.
	}

}
