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
		// assertEquals(
		// 	"Container: WEBM, Size: 901185, Overall Bitrate: 236044, Duration: 0:00:30.543, Video Tracks: 1 [Video Id: 0, Codec: vp8, Stream Order: 0, Duration: 0:00:30.033, Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 30.0, Frame Rate Mode: CFR (CFR)], Audio Tracks: 1 [Audio Id: 0, Codec: Vorbis, Stream Order: 1, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz, Video Delay: -3], Mime Type: video/webm",
		// 	getTestFileMediaInfo("video-vp8-vorbis.webm").toString()
		// );
		// assertEquals(
		// 	"Container: FLV, Size: 2097492, Overall Bitrate: 1529899, Duration: 0:00:10.968, Video Tracks: 1 [Video Id: 0, Codec: sor, Duration: 0:00:10.960, Resolution: 1280 x 720, Display Aspect Ratio: 16:9], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Bitrate: 375000, Channels: 2, Sample Frequency: 48000 Hz, Video Delay: 3], Mime Type: video/x-flv",
		// 	getTestFileMediaInfo("video-sor-aac.flv").toString()
		// );
		// assertEquals(
		// 	"Container: AVI, Size: 742478, Overall Bitrate: 194029, Duration: 0:00:30.613, Video Tracks: 1 [Video Id: 0, Codec: h264, Format Profile: high, Format Level: 2.1, Stream Order: 0, Duration: 0:00:30.033, Resolution: 480 x 270, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 30.0, Frame Rate Mode: VFR (VFR), Reference Frame Count: 4], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 1, Bitrate: 139632, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi",
		// 	getTestFileMediaInfo("video-h264-aac.avi").toString()
		// );
		// assertEquals(
		// 	"Container: MP4, Size: 245747, Overall Bitrate: 130716, Duration: 0:00:15.040, Video Tracks: 1 [Video Id: 0, Title: vid, Codec: av1, Format Profile: main, Format Level: 3.0, Stream Order: 1, Duration: 0:00:15.000, Resolution: 960 x 540, Display Aspect Ratio: 16:9, Frame Rate: 25.0, Frame Rate Mode: CFR (CFR)], Audio Tracks: 1 [Audio Id: 0, Title: snd, Language Code: snd, Codec: AAC-LC, Stream Order: 0, Bitrate: 8887, Channel: 1, Sample Frequency: 32000 Hz], Mime Type: video/mp4",
		// 	getTestFileMediaInfo("video-av1-aac.mp4").toString()
		// );
		// assertEquals(
		// 	"Container: MP4, Size: 690235, Overall Bitrate: 952377, Duration: 0:00:05.798, Video Tracks: 1 [Video Id: 0, Title: ivf@GPAC0.7.2-DEV-rev654-gb6f7409ce-github_master, Codec: av1, Format Profile: main, Format Level: 2.0, Stream Order: 0, Duration: 0:00:05.798, Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR)], Mime Type: video/mp4",
		// 	getTestFileMediaInfo("video-av1.mp4").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 6291087, Overall Bitrate: 22468168, Duration: 0:00:02.240, Video Tracks: 1 [Video Id: 0, Codec: vc1, Format Profile: advanced, Format Level: 3, Stream Order: 0, Duration: 0:00:02.240, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Scan Type: Interlaced, Scan Order: Top Field First, Frame Rate: 25.0, Frame Rate Mode: CFR (CFR)], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-vc1.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 8710128, Overall Bitrate: 7767364, Duration: 0:00:08.971, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: high, Format Level: 4.1, Stream Order: 0, Duration: 0:00:08.967, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Reference Frame Count: 4], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: DTS-HD, Stream Order: 1, Bitrate: 0, Bits per Sample: 24, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h264-dtshd.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 9513954, Overall Bitrate: 8484186, Duration: 0:00:08.971, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: high, Format Level: 4.1, Stream Order: 0, Duration: 0:00:08.967, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Reference Frame Count: 4], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: DTS-HD, Stream Order: 1, Bitrate: 0, Bits per Sample: 24, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h264-dtshd_x.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MP4, Size: 1099408, Overall Bitrate: 188638, Duration: 0:00:46.625, Video Tracks: 1 [Video Id: 0, Codec: h264, Format Profile: main, Format Level: 3.1, Stream Order: 0, Duration: 0:00:46.625, Resolution: 800 x 600, Display Aspect Ratio: 4:3, Scan Type: Progressive, Frame Rate: 8.0, Frame Rate Mode: CFR (CFR), Reference Frame Count: 4], Audio Tracks: 1 [Audio Id: 0, Codec: HE-AAC, Stream Order: 1, Bitrate: 159992, Channels: 6, Sample Frequency: 44100 Hz], Mime Type: video/mp4",
		// 	getTestFileMediaInfo("video-h264-heaac.mp4").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 6270615, Overall Bitrate: 8339970, Duration: 0:00:06.015, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: main, Format Level: 5.1, Stream Order: 0, Duration: 0:00:06.006, Resolution: 1280 x 544, Display Aspect Ratio: 2.35:1, Scan Type: Progressive, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Reference Frame Count: 1], Audio Tracks: 1 [Audio Id: 0, Language Code: fre, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 1536000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h264-eac3.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: AVI, Size: 1282694, Overall Bitrate: 793255, Duration: 0:00:12.936, Video Tracks: 1 [Video Id: 0, Codec: divx, Format Profile: advanced simple, Format Level: 5, Stream Order: 0, Duration: 0:00:12.920, Resolution: 720 x 400, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 25.0], Audio Tracks: 1 [Audio Id: 0, Title: video-mpeg4-aac, Codec: MP3, Stream Order: 1, Bitrate: 128000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi",
		// 	getTestFileMediaInfo("video-xvid-mp3.avi").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 8925360, Overall Bitrate: 11868830, Duration: 0:00:06.016, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: main, Stream Order: 0, Duration: 0:00:06.006, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Frame Rate: 59.94, Frame Rate Mode: CFR (CFR), Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision)], Audio Tracks: 1 [Audio Id: 0, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p05.05-eac3_atmos.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 7799945, Overall Bitrate: 10372267, Duration: 0:00:06.016, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: main, Stream Order: 0, Duration: 0:00:06.006, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Frame Rate: 59.94, Frame Rate Mode: CFR (CFR), Matrix Coefficients: BT.2020 non-constant, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2086 (dolbyvision), HDR Format Compatibility: HDR10 / HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p08.05-eac3_atmos.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 2270734, Overall Bitrate: 6083681, Duration: 0:00:02.986, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: main, Stream Order: 0, Duration: 0:00:02.853, Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 58.535, Frame Rate Mode: VFR (VFR), Matrix Coefficients: BT.2020 non-constant, Bit Depth: 10, HDR Format: SMPTE ST 2086, HDR Format Compatibility: HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Codec: AAC-LC, Stream Order: 1, Bitrate: 117969, Channels: 2, Sample Frequency: 48000 Hz, Video Delay: -67], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h265_hdr10-aac.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 8652028, Overall Bitrate: 61416348, File Title from Metadata: A Beautiful Planet (2016), Duration: 0:00:01.127, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: high, Stream Order: 0, Duration: 0:00:01.001, Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Matrix Coefficients: BT.2020 non-constant, Bit Depth: 10, HDR Format: SMPTE ST 2094 App 4, HDR Format Compatibility: HDR10+ Profile A (hdr10+)], Audio Tracks: 1 [Audio Id: 0, Title: DTS:X, Language Code: eng, Codec: DTS-HD, Stream Order: 1, Bitrate: 8543871, Bits per Sample: 24, Channels: 8, Sample Frequency: 48000 Hz, Video Delay: 134], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h265_hdr10+-dtshd_x_imax.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MPEGTS, Size: 32636236, Overall Bitrate: 31110533, Duration: 0:00:08.392, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Format Level: 5, Format Tier: main, Duration: 0:00:08.425, Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision), HDR Format Compatibility: SDR], Mime Type: video/vnd.dlna.mpeg-tts",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p04.06.ts").toString()
		// );
		// assertEquals(
		// 	"Container: MPEGTS, Size: 29792360, Overall Bitrate: 40020476, Duration: 0:00:05.955, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Format Level: 6.1, Format Tier: main, Duration: 0:00:06.006, Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision)], Mime Type: video/vnd.dlna.mpeg-tts",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p05.06.ts").toString()
		// );
		// assertEquals(
		// 	"Container: MPEGTS, Size: 12851116, Overall Bitrate: 20212872, Duration: 0:00:05.086, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: main, Duration: 0:00:05.116, Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 60.0, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision)], Mime Type: video/vnd.dlna.mpeg-tts",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p05.09.ts").toString()
		// );
		// assertEquals(
		// 	"Container: MPEGTS, Size: 2448136, Overall Bitrate: 3926871, Duration: 0:00:04.987, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: high, Duration: 0:00:05.005, Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Matrix Coefficients: BT.2020 non-constant, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2086 (dolbyvision), HDR Format Compatibility: Blu-ray / HDR10 (hdr10)], Mime Type: video/vnd.dlna.mpeg-tts",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p07.06.ts").toString()
		// );
		// assertEquals(
		// 	"Container: MPEGTS, Size: 16063660, Overall Bitrate: 28310726, Duration: 0:00:04.539, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: high, Duration: 0:00:04.504, Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Matrix Coefficients: BT.2020 non-constant, Bit Depth: 10, HDR Format: Dolby Vision (dolbyvision), HDR Format Compatibility: HLG (hlg)], Mime Type: video/vnd.dlna.mpeg-tts",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p08.12.ts").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 19121021, Overall Bitrate: 14663360, Duration: 0:00:10.432, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Format Level: 5.1, Format Tier: main, Stream Order: 0, Duration: 0:00:10.427, Resolution: 3840 x 1608, Display Aspect Ratio: 2.39:1, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Matrix Coefficients: BT.2020 non-constant, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2094 App 4 (dolbyvision), HDR Format Compatibility: HDR10 / HDR10+ Profile B (hdr10)], Audio Tracks: 1 [Audio Id: 0, Title: DDP 7.1, Language Code: eng, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 1536000, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p08.06-eac3_dolby_surround_ex.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MKV, Size: 11413502, Overall Bitrate: 6256640, Duration: 0:00:14.940, Video Tracks: 1 [Video Id: 0, Codec: h265, Format Profile: main 10, Stream Order: 0, Resolution: 3840 x 2160, Frame Rate: 25.0, Bit Depth: 10, HDR Format: Dolby Vision / SMPTE ST 2086 (dolbyvision), HDR Format Compatibility: HDR10 (hdr10)], Audio Tracks: 1 [Audio Id: 0, Language Code: eng, Codec: Enhanced AC-3, Stream Order: 1, Bitrate: 768000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
		// 	getTestFileMediaInfo("video-h265_dolbyvision_p08.07-eac3_atmos.mkv").toString()
		// );
		// assertEquals(
		// 	"Container: MP4, Size: 23449234, Overall Bitrate: 2608913, Duration: 0:01:11.905, Video Tracks: 1 [Video Id: 0, Language Code: eng, Codec: h264, Format Profile: main, Format Level: 4, Stream Order: 1, Duration: 0:01:11.905, Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 29.97, Frame Rate Mode: CFR (CFR), Reference Frame Count: 2], Audio Tracks: 1 [Audio Id: 0, Codec: ac4, Stream Order: 0, Bitrate: 128000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/mp4",
		// 	getTestFileMediaInfo("video-h264-6ch-ac4.mp4").toString()
		// );

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
