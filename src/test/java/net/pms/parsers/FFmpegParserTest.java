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
		/**
		 * note: FFmpeg does not seem to output enough information to get the following:
		 * - Format Level
		 * - Display Aspect Ratio
		 * - Scan Type
		 * - Frame Rate Mode
		 */
		assertEquals(
			"Container: MP4, Size: 1325017, Overall Bitrate: 692224, Duration: 0:00:15.660, Video Tracks: 1 [Video Id: 0, Codec: h264, Format Profile: high, Stream Order: 0, Resolution: 640 x 360, Frame Rate: 23.98], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 1, Bitrate: 125000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/mp4",
			getTestFileMediaInfo("video-h264-aac.mp4").toString()
		);
		assertEquals(
			"Container: MKV, Size: 2097841, Overall Bitrate: 1612800, Duration: 0:00:10.650, Video Tracks: 1 [Video Id: 0, Codec: mp4, Format Profile: simple profile, Stream Order: 0, Resolution: 1280 x 720, Frame Rate: 25.0], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 0, Bitrate: 0, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-mpeg4-aac.mkv").toString()
		);
		assertEquals(
			"Container: MKV, Size: 5291494, Overall Bitrate: 2681856, Duration: 0:00:16.160, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main, Stream Order: 0, Resolution: 1920 x 960, Frame Rate: 25.0], Audio Tracks: 1 [Audio Id: 0, Title: Stereo, Language Code: eng, Codec: AAC-LC, Stream Order: 1, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265-aac.mkv").toString()
		);
		assertEquals(
			"Container: OGG, Size: 1734919, Overall Bitrate: 464896, Duration: 0:00:30.530, Video Tracks: 1 [Video Id: 0, Codec: theora, Stream Order: 0, Resolution: 480 x 270, Frame Rate: 30.0], Audio Tracks: 1 [Audio Id: 0, Codec: Vorbis, Stream Order: 1, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/ogg",
			getTestFileMediaInfo("video-theora-vorbis.ogg").toString()
		);
		assertEquals(
			"Container: M4V, Size: 3538130, Overall Bitrate: 555008, Duration: 0:00:52.210, Video Tracks: 1 [Video Id: 0, Codec: h264, Format Profile: high, Stream Order: 0, Resolution: 720 x 480, Frame Rate: 24.0], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 1, Bitrate: 128000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/x-m4v",
			getTestFileMediaInfo("video-h264-aac.m4v").toString()
		);
		assertEquals(
			"Container: 3G2, Size: 1792091, Overall Bitrate: 281600, Duration: 0:00:52.060, Video Tracks: 1 [Video Id: 0, Codec: mp4, Format Profile: simple profile, Stream Order: 0, Resolution: 360 x 240, Frame Rate: 18.0], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 1, Bitrate: 59000, Channels: 2, Sample Frequency: 22050 Hz], Mime Type: video/3gpp2",
			getTestFileMediaInfo("video-mp4-aac.3g2").toString()
		);
		assertEquals(
			"Container: AVI, Size: 3893340, Overall Bitrate: 618496, Duration: 0:00:51.540, Video Tracks: 1 [Video Id: 0, Codec: mp4, Format Profile: mp42 / 0x3234504d, Stream Order: 0, Resolution: 360 x 240, Frame Rate: 24.0], Audio Tracks: 1 [Audio Id: 0, Codec: ADPCM, Stream Order: 0, Bitrate: 352000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/avi",
			getTestFileMediaInfo("video-mp4-adpcm.avi").toString()
		);
		assertEquals(
			"Container: MOV, Size: 2658492, Overall Bitrate: 417792, Duration: 0:00:52.080, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: mp4, Format Profile: simple profile, Stream Order: 0, Resolution: 360 x 240, Frame Rate: 24.0], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: AAC-LC, Stream Order: 1, Bitrate: 125000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/quicktime",
			getTestFileMediaInfo("video-mp4-aac.mov").toString()
		);
		assertEquals(
			"Container: WMV, Size: 3002945, Overall Bitrate: 470016, Duration: 0:00:52.260, Video Tracks: 1 [Video Id: 0, Codec: wmv, Format Profile: wmv2 / 0x32564d57, Stream Order: 0, Resolution: 360 x 240, Frame Rate: 24.0], Audio Tracks: 1 [Audio Id: 0, Codec: WMA, Stream Order: 0, Bitrate: 128000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/x-ms-wmv",
			getTestFileMediaInfo("video-wmv-wma.wmv").toString()
		);
		assertEquals(
			"Container: WEBM, Size: 901185, Overall Bitrate: 241664, Duration: 0:00:30.540, Video Tracks: 1 [Video Id: 0, Codec: vp8, Stream Order: 0, Resolution: 480 x 270, Frame Rate: 30.0], Audio Tracks: 1 [Audio Id: 0, Codec: Vorbis, Stream Order: 0, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/webm",
			getTestFileMediaInfo("video-vp8-vorbis.webm").toString()
		);
		assertEquals(
			"Container: FLV, Size: 2097492, Overall Bitrate: 1561600, Duration: 0:00:11.000, Video Tracks: 1 [Video Id: 0, Codec: sor, Format Profile: flv, Stream Order: 0, Resolution: 1280 x 720, Frame Rate: 25.0], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 0, Bitrate: 384000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-flv",
			getTestFileMediaInfo("video-sor-aac.flv").toString()
		);
		assertEquals(
			"Container: AVI, Size: 742478, Overall Bitrate: 198656, Duration: 0:00:30.610, Video Tracks: 1 [Video Id: 0, Codec: h264, Format Profile: high, Stream Order: 0, Resolution: 480 x 270, Frame Rate: 30.0], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 0, Bitrate: 139000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi",
			getTestFileMediaInfo("video-h264-aac.avi").toString()
		);
		assertEquals(
			"Container: MP4, Size: 245747, Overall Bitrate: 133120, Duration: 0:00:15.040, Video Tracks: 1 [Video Id: 0, Codec: av1, Format Profile: libaom-av1, Stream Order: 1, Resolution: 960 x 540, Frame Rate: 25.0], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 0, Bitrate: 8000, Channel: 1, Sample Frequency: 32000 Hz], Mime Type: video/mp4",
			getTestFileMediaInfo("video-av1-aac.mp4").toString()
		);
		assertEquals(
			"Container: MP4, Size: 690235, Overall Bitrate: 974848, Duration: 0:00:05.800, Video Tracks: 1 [Video Id: 0, Codec: av1, Format Profile: libaom-av1, Stream Order: 0, Resolution: 480 x 270, Frame Rate: 23.98], Mime Type: video/mp4",
			getTestFileMediaInfo("video-av1.mp4").toString()
		);
		assertEquals(
			"Container: MKV, Size: 6291087, Overall Bitrate: 23007232, Duration: 0:00:02.240, Video Tracks: 1 [Video Id: 0, Codec: vc1, Format Profile: advanced, Stream Order: 0, Resolution: 1920 x 1080, Frame Rate: 25.0], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-vc1.mkv").toString()
		);
		assertEquals(
			"Container: MKV, Size: 8710128, Overall Bitrate: 7953408, Duration: 0:00:08.970, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: high, Stream Order: 0, Resolution: 1920 x 1080, Frame Rate: 23.98], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: DTS-HD, Stream Order: 1, Bitrate: 0, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h264-dtshd.mkv").toString()
		);
		assertEquals(
			"Container: MKV, Size: 9513954, Overall Bitrate: 8687616, Duration: 0:00:08.970, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: high, Stream Order: 0, Resolution: 1920 x 1080, Frame Rate: 23.98], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: DTS-HD, Stream Order: 1, Bitrate: 0, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h264-dtshd_x.mkv").toString()
		);
		assertEquals(
			"Container: MP4, Size: 1099408, Overall Bitrate: 192512, Duration: 0:00:46.630, Video Tracks: 1 [Video Id: 0, Codec: h264, Format Profile: main, Stream Order: 0, Resolution: 800 x 600, Frame Rate: 8.0], Audio Tracks: 1 [Audio Id: 0, Codec: HE-AAC, Stream Order: 1, Bitrate: 159000, Channels: 6, Sample Frequency: 44100 Hz], Mime Type: video/mp4",
			getTestFileMediaInfo("video-h264-heaac.mp4").toString()
		);
		assertEquals(
			"Container: MKV, Size: 6270615, Overall Bitrate: 8539136, Duration: 0:00:06.020, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: main, Stream Order: 0, Resolution: 1280 x 544, Frame Rate: 23.98], Audio Tracks: 1 [Audio Id: 0, Language Code: fre, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 1536000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h264-eac3.mkv").toString()
		);
		assertEquals(
			"Container: AVI, Size: 1282694, Overall Bitrate: 813056, Duration: 0:00:12.920, Video Tracks: 1 [Video Id: 0, Codec: divx, Format Profile: advanced simple profile, Stream Order: 0, Resolution: 720 x 400, Frame Rate: 25.0], Audio Tracks: 1 [Audio Id: 0, Title: video-mpeg4-aac, Codec: MP3, Stream Order: 0, Bitrate: 128000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi",
			getTestFileMediaInfo("video-xvid-mp3.avi").toString()
		);
		assertEquals(
			"Container: MKV, Size: 8925360, Overall Bitrate: 12152832, Duration: 0:00:06.020, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 1920 x 1080, Frame Rate: 59.94, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision)], Audio Tracks: 1 [Audio Id: 0, Codec: Enhanced AC-3, Stream Order: 0, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265_dolbyvision_p05.05-eac3_atmos.mkv").toString()
		);
		assertEquals(
			"Container: MKV, Size: 7799945, Overall Bitrate: 10620928, Duration: 0:00:06.020, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 1920 x 1080, Frame Rate: 59.94, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2086 (dolbyvision), HDR Format Compatibility: HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Codec: Enhanced AC-3, Stream Order: 0, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265_dolbyvision_p08.05-eac3_atmos.mkv").toString()
		);
		assertEquals(
			"Container: MKV, Size: 2270734, Overall Bitrate: 6228992, Duration: 0:00:02.990, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 59.94, Bit Depth: 10, HDR Format: HDR10 (hdr10), HDR Format Compatibility: HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 0, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265_hdr10-aac.mkv").toString()
		);
		assertEquals(
			"Container: MKV, Size: 8652028, Overall Bitrate: 62889984, Duration: 0:00:01.130, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 23.98, Bit Depth: 10, HDR Format: HDR10 (hdr10), HDR Format Compatibility: HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Title: DTS:X, Language Code: eng, Codec: DTS-HD, Stream Order: 1, Bitrate: 0, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265_hdr10+-dtshd_x_imax.mkv").toString()
		);
		assertEquals(
			"Container: MPEGTS, Size: 32636236, Overall Bitrate: 31890432, Duration: 0:00:08.380, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 23.98, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision), HDR Format Compatibility: SDR], Mime Type: video/vnd.dlna.mpeg-tts",
			getTestFileMediaInfo("video-h265_dolbyvision_p04.06.ts").toString()
		);
		assertEquals(
			"Container: MPEGTS, Size: 29792360, Overall Bitrate: 40920064, Duration: 0:00:05.960, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 23.98, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision)], Mime Type: video/vnd.dlna.mpeg-tts",
			getTestFileMediaInfo("video-h265_dolbyvision_p05.06.ts").toString()
		);
		assertEquals(
			"Container: MPEGTS, Size: 12851116, Overall Bitrate: 20574208, Duration: 0:00:05.120, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 60.0, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision)], Mime Type: video/vnd.dlna.mpeg-tts",
			getTestFileMediaInfo("video-h265_dolbyvision_p05.09.ts").toString()
		);
		assertEquals(
			"Container: MPEGTS, Size: 2448136, Overall Bitrate: 4006912, Duration: 0:00:05.000, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 23.98, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2086 (dolbyvision), HDR Format Compatibility: Blu-ray / HDR10 (hdr10)], Mime Type: video/vnd.dlna.mpeg-tts",
			getTestFileMediaInfo("video-h265_dolbyvision_p07.06.ts").toString()
		);
		assertEquals(
			"Container: MPEGTS, Size: 16063660, Overall Bitrate: 27198464, Duration: 0:00:04.840, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 23.98, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision), HDR Format Compatibility: HLG (hlg)], Mime Type: video/vnd.dlna.mpeg-tts",
			getTestFileMediaInfo("video-h265_dolbyvision_p08.12.ts").toString()
		);
		assertEquals(
			"Container: MKV, Size: 19121021, Overall Bitrate: 15014912, Duration: 0:00:10.430, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 1608, Frame Rate: 23.98, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2086 (dolbyvision), HDR Format Compatibility: HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Title: DDP 7.1, Language Code: eng, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 1536000, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265_dolbyvision_p08.06-eac3_dolby_surround_ex.mkv").toString()
		);
		assertEquals(
			"Container: MKV, Size: 11413502, Overall Bitrate: 6256640, Duration: 0:00:14.940, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 25.0, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2086 (dolbyvision), HDR Format Compatibility: HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 768000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
			getTestFileMediaInfo("video-h265_dolbyvision_p08.07-eac3_atmos.mkv").toString()
		);
		assertEquals(
			"Container: MP4, Size: 23449234, Overall Bitrate: 2670592, Duration: 0:01:11.910, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: main, Stream Order: 1, Resolution: 1280 x 720, Frame Rate: 29.97], Audio Tracks: 1 [Audio Id: 0, Codec: ac4, Stream Order: 0, Bitrate: 128000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/mp4",
			getTestFileMediaInfo("video-h264-6ch-ac4.mp4").toString()
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
