package net.pms.parsers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.video.MediaVideo;
import net.pms.parsers.mediainfo.StreamKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DivxCodecRecognitionTest {
	@BeforeAll
	static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	private MediaInfo ffmpeg(String codec) {
		var media = new MediaInfo();
		FFmpegParser.parseFFmpegInfo(media, List.of(
			"Input #0, matroska,webm, from 'sample.mkv':",
			"  Duration: 00:00:30.86, start: 0.000000, bitrate: 2106 kb/s",
			"  Stream #0:0: Video: " + codec + ", yuv420p, 640x480, 23.98 fps"), "sample.mkv");
		return media;
	}

	@Test
	void ffmpegRecognizesDivxAndXvidRegardlessOfProfileAndCase() {
		for (String codec : List.of(
			"mpeg4 (Simple Profile) (XVID / 0x44495658)",
			"mpeg4 (Advanced Simple Profile) (XVID / 0x44495658)",
			"mpeg4 (xvid / 0x44495658)",
			"mpeg4 (Simple Profile) (DIVX / 0x58564944)",
			"mpeg4 (Advanced Simple Profile) (DX50 / 0x30355844)",
			"msmpeg4v3 (DIV3 / 0x33564944)", "XVID", "DIVX")) {
			var media = ffmpeg(codec);
			assertEquals(FormatConfiguration.DIVX, media.getDefaultVideoTrack().getCodec(), codec);
			assertEquals(FormatConfiguration.MKV, media.getContainer());
		}
	}

	@Test
	void mediaInfoRecognizesWrappedFourccFromMatroska() {
		for (String identifier : List.of("V_MS/VFW/FOURCC / XVID", "V_MS/VFW/FOURCC / DIVX",
			"DX50", "DVX1", "DIV3", "xvid")) {
			var media = new MediaInfo();
			media.setContainer(FormatConfiguration.MKV);
			var video = new MediaVideo();
			var audio = new MediaAudio();
			MediaInfoParser.setFormat(StreamKind.VIDEO, media, video, audio, "MPEG-4 Visual", null);
			MediaInfoParser.setFormat(StreamKind.VIDEO, media, video, audio, identifier, null);
			assertEquals(FormatConfiguration.DIVX, video.getCodec(), identifier);
			assertEquals(FormatConfiguration.MKV, media.getContainer());
		}
	}

	@Test
	void mediaInfoUsesCodecHintOrEncoderWhenFourccIsNotAvailable() {
		assertEquals(FormatConfiguration.DIVX,
			Parser.normalizeMpeg4VideoCodec(FormatConfiguration.MP4, "V_MPEG4/ISO/ASP", "XviD", ""));
		for (String library : List.of("XviD0036", "XviD 1.3.7", "DivX 5.2.1", "DivX503b1393p")) {
			assertEquals(FormatConfiguration.DIVX,
				Parser.normalizeMpeg4VideoCodec(FormatConfiguration.MP4, "", "", library), library);
		}
	}

	@Test
	void doesNotReclassifyOtherCodecsOrTheMp4Container() {
		assertEquals(FormatConfiguration.MP4, ffmpeg("mpeg4 (Simple Profile)").getDefaultVideoTrack().getCodec());
		assertEquals(FormatConfiguration.H264, ffmpeg("h264 (High)").getDefaultVideoTrack().getCodec());
		assertEquals(FormatConfiguration.H264, Parser.normalizeMpeg4VideoCodec(FormatConfiguration.H264, "DivX Plus"));
		assertFalse(Parser.isDivxIdentifier("notxvid"));
		var media = new MediaInfo();
		MediaInfoParser.setFormat(StreamKind.GENERAL, media, new MediaVideo(), new MediaAudio(), "mp42 (mp42/isom)", null);
		assertEquals(FormatConfiguration.MP4, media.getContainer());
	}
}
