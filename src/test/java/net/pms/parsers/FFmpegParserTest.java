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
import net.pms.util.InputFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegParserTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegParserTest.class.getName());

	@BeforeAll
	public static void setUpClass() {
		ParserTest.setUpClass();
	}

	private MediaInfo getTestFileMediaInfo(String testFile) {
		File file = ParserTest.getTestFile(testFile);
		InputFile inputFile = new InputFile();
		inputFile.setFile(file);
		Format format = FormatFactory.getAssociatedFormat(file.getAbsolutePath());
		MediaInfo mediaInfo = new MediaInfo();
		FFmpegParser.parse(mediaInfo, inputFile, format, format.getType());
		return mediaInfo;
	}

	@Test
	public void testParser() throws Exception {
		if (!FFmpegParser.isValid()) {
			//the executable was not found
			LOGGER.info("FFmpegParser test skipped");
			return;
		} else {
			LOGGER.info("FFmpegParser will test on ffmpeg version {}", FFmpegParser.getVersion());
		}

		//video
		assertEquals(
			"Container: MP4, Size: 1325017, Overall Bitrate: 692224, Duration: 0:00:15.660, Video Tracks: 1 [Video Id: 0, Codec: h264, Format Profile: high, Stream Order: 0, Resolution: 640 x 360, Frame Rate: 23.98], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 1, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/mp4",
			getTestFileMediaInfo("video-h264-aac.mp4").toString()
		);
		assertEquals(
			"Container: MKV, Size: 11413502, Overall Bitrate: 6256640, Duration: 0:00:14.940, Video Tracks: 1 [Video Id: 0, Codec: hevc, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 25.0], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265_dolbyvision_p08.07-eac3_atmos.mkv").toString()
		);

		//should fallback to JaudiotaggerParser for audio
		assertEquals(
			JaudiotaggerParser.PARSER_NAME,
			getTestFileMediaInfo("audio-lpcm.wav").getMediaParser()
		);

		//should fallback to MetadataExtractorParser for image
		assertEquals(
			MetadataExtractorParser.PARSER_NAME,
			getTestFileMediaInfo("image-gif.gif").getMediaParser()
		);

	}

}
