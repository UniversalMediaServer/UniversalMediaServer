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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.video.MediaVideo;
import net.pms.parsers.mediainfo.MediaInfoHelper;
import net.pms.parsers.mediainfo.StreamKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MediaParsingValidationTest {
	@BeforeAll
	static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	void parserNameAndContainerDoNotProveContentWasFound() {
		var media = new MediaInfo();
		media.setContainer("wtv");
		media.setMediaParser("MediaInfo");
		assertTrue(media.isMediaParsed());
		assertFalse(Parser.hasMediaContent(media, Format.VIDEO));
		assertFalse(Parser.hasMediaContent(media, Format.AUDIO));
		assertFalse(Parser.hasMediaContent(media, Format.IMAGE));
	}

	@Test
	void multipleAudioTracksAndCoverAreAudioEvenInVideoContainer() {
		var media = new MediaInfo();
		media.setContainer("mkv");
		media.addAudioTrack(new MediaAudio());
		media.addAudioTrack(new MediaAudio());
		media.setImageCount(1);
		assertTrue(media.isAudio());
		assertFalse(media.isVideo());
		assertFalse(media.isImage());
		assertTrue(Parser.hasMediaContent(media, Format.VIDEO));
		media.addVideoTrack(new MediaVideo());
		assertTrue(media.isVideo());
	}

	@Test
	void failedFallbackPreservesPrimaryMetadataAndParser() {
		var primary = new MediaInfo();
		primary.setContainer("wtv");
		primary.setTitle("BBC News");
		primary.setDuration(64.64);
		primary.setMediaParser("MediaInfo");
		var fallback = new MediaInfo();
		fallback.setMediaParser("FFmpeg");
		Parser.applyFallback(primary, fallback, Format.VIDEO);
		assertEquals("MediaInfo", primary.getMediaParser());
		assertEquals("BBC News", primary.getTitle());
		assertEquals(64.64, primary.getDurationInSeconds());
	}

	@Test
	void successfulFallbackAddsStreamsWithoutReplacingPrimaryDetails() {
		var primary = new MediaInfo();
		primary.setContainer("wtv");
		primary.setTitle("BBC News");
		primary.setDuration(64.64);
		var fallback = new MediaInfo();
		fallback.setContainer("mpegts");
		fallback.setDuration(65.0);
		fallback.setMediaParser("FFmpeg");
		fallback.addVideoTrack(new MediaVideo());
		fallback.addAudioTrack(new MediaAudio());
		Parser.applyFallback(primary, fallback, Format.VIDEO);
		assertTrue(primary.isVideo());
		assertTrue(primary.hasAudio());
		assertEquals("wtv", primary.getContainer());
		assertEquals("BBC News", primary.getTitle());
		assertEquals(64.64, primary.getDurationInSeconds());
		assertEquals("FFmpeg", primary.getMediaParser());
	}

	@Test
	void fallbackDoesNotReplaceExistingTracks() {
		var primary = new MediaInfo();
		var video = new MediaVideo();
		video.setHDRFormat("Dolby Vision");
		primary.addVideoTrack(video);
		var fallback = new MediaInfo();
		fallback.addVideoTrack(new MediaVideo());
		Parser.applyFallback(primary, fallback, Format.VIDEO);
		assertSame(video, primary.getDefaultVideoTrack());
		assertEquals("Dolby Vision", primary.getDefaultVideoTrack().getHDRFormat());
	}

	@Test
	void helperCreationFailureReleasesParsingFlag() {
		var media = new MediaInfo();
		MediaInfoParser.parse(media, new File("sample.mkv"), Format.VIDEO, () -> {
			throw new IllegalStateException("Test failure");
		});
		assertFalse(media.isParsing());
		assertFalse(media.isMediaParsed());
	}

	@Test
	void nativeParsingHandlesMissingDimensionsAndSubtitleLikeVideoTitle() {
		var helper = new MediaInfoHelper(false) {
			@Override
			public Long getLong(StreamKind kind, int stream, String field) {
				if (kind == StreamKind.VIDEO && ("Width".equals(field) || "Height".equals(field))) {
					return null;
				}
				return super.getLong(kind, stream, field);
			}
			@Override
			public String get(StreamKind kind, int stream, String field) {
				return kind == StreamKind.VIDEO && "Title".equals(field) ? "Subtitle demonstration" : super.get(kind, stream, field);
			}
		};
		assumeTrue(helper.isValid());
		var media = new MediaInfo();
		MediaInfoParser.parse(media, ParserTest.getTestFile("video-h264-aac.mp4"), Format.VIDEO, () -> helper);
		assertFalse(helper.isValid(), "Native handle must be deleted");
		assertFalse(media.isParsing());
		assertTrue(media.isMediaParsed());
		assertNotNull(media.getDefaultVideoTrack());
		assertEquals(0, media.getWidth());
		assertEquals(0, media.getHeight());
	}

	@Test
	void failureWhileReadingNativeFieldsClosesHandle() {
		var helper = new MediaInfoHelper(false) {
			@Override
			public String get(StreamKind kind, int stream, String field) {
				throw new IllegalStateException("Test read failure");
			}
		};
		assumeTrue(helper.isValid());
		var media = new MediaInfo();
		MediaInfoParser.parse(media, ParserTest.getTestFile("video-h264-aac.mp4"), Format.VIDEO, () -> helper);
		assertFalse(helper.isValid());
		assertFalse(media.isParsing());
		assertFalse(media.isMediaParsed());
	}

	@Test
	void divxSubtitlesAreIdentifiedByCodec() {
		assertTrue(MediaInfoParser.isVideoSubtitle("DXSA"));
		assertTrue(MediaInfoParser.isVideoSubtitle("dxsb"));
		assertFalse(MediaInfoParser.isVideoSubtitle("avc1"));
		assertFalse(MediaInfoParser.isVideoSubtitle(null));
	}
}