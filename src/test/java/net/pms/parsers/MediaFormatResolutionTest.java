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

import java.io.File;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.video.MediaVideo;
import net.pms.parsers.mediainfo.StreamKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MediaFormatResolutionTest {
	@BeforeAll
	static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	void audioOnlyContainersUseAudioMimeEvenWhenExtensionSuggestsVideo() {
		for (var entry : Map.of("mp4", "audio/mp4", "ogg", "audio/ogg", "mkv", "audio/x-matroska",
			"webm", "audio/webm", "3gp", "audio/3gpp", "3g2", "audio/3gpp2").entrySet()) {
			var media = new MediaInfo();
			media.setContainer(entry.getKey());
			media.addAudioTrack(new MediaAudio());
			media.addAudioTrack(new MediaAudio());
			Parser.postParse(media, Format.VIDEO);
			assertEquals(entry.getValue(), media.getMimeType(), entry.getKey());
			Parser.postParse(media, Format.VIDEO);
			assertEquals(entry.getValue(), media.getMimeType());
			assertEquals(entry.getKey(), media.getContainer());
		}
	}

	@Test
	void unknownVideoCodecDoesNotTurnVideoIntoAudio() {
		var media = new MediaInfo();
		media.setContainer("mkv");
		media.addVideoTrack(new MediaVideo());
		var audio = new MediaAudio();
		audio.setCodec("mp3");
		media.addAudioTrack(audio);
		Parser.postParse(media, Format.VIDEO);
		assertEquals("video/x-matroska", media.getMimeType());
	}

	@Test
	void dtsMimeRespectsTheContainer() {
		for (String codec : new String[]{"dts", "dtshd"}) {
			var media = new MediaInfo();
			var audio = new MediaAudio();
			audio.setCodec(codec);
			media.addAudioTrack(audio);
			Parser.postParse(media, Format.AUDIO);
			assertEquals(codec.equals("dts") ? "audio/vnd.dts" : "audio/vnd.dts.hd", media.getMimeType());
			media.setContainer("wav");
			Parser.postParse(media, Format.AUDIO);
			assertEquals("audio/wav", media.getMimeType());
		}
	}

	@Test
	void jpegIsAnImageButVideoJpegIsMjpeg() {
		var media = new MediaInfo();
		var video = new MediaVideo();
		var audio = new MediaAudio();
		MediaInfoParser.setFormat(StreamKind.GENERAL, media, video, audio, "JPEG", null);
		Parser.postParse(media, Format.IMAGE);
		assertEquals("image/jpeg", media.getMimeType());
		MediaInfoParser.setFormat(StreamKind.VIDEO, media, video, audio, "JPEG", null);
		assertEquals(FormatConfiguration.MJPEG, video.getCodec());
	}

	@Test
	void profileRefinementPreservesDtsHdAndMpegAudioRules() {
		var media = new MediaInfo();
		var video = new MediaVideo();
		var audio = new MediaAudio();
		audio.setCodec(FormatConfiguration.DTS);
		MediaInfoParser.setFormat(StreamKind.AUDIO, media, video, audio, "MA / Core", null);
		assertEquals(FormatConfiguration.DTSHD, audio.getCodec());
		MediaInfoParser.setFormat(StreamKind.AUDIO, media, video, audio, "DTS", null);
		assertEquals(FormatConfiguration.DTSHD, audio.getCodec());
		media.setContainer(FormatConfiguration.MPA);
		audio.setCodec(FormatConfiguration.MPA);
		MediaInfoParser.setFormat(StreamKind.AUDIO, media, video, audio, "Layer 3", null);
		assertEquals(FormatConfiguration.MP3, audio.getCodec());
		assertEquals(FormatConfiguration.MP3, media.getContainer());
	}

	@Test
	void unknownGeneralFormatCanLackFilenameAndCannotOverwriteKnownContainer() {
		var media = new MediaInfo();
		var video = new MediaVideo();
		var audio = new MediaAudio();
		MediaInfoParser.setFormat(StreamKind.GENERAL, media, video, audio, "unknown", null);
		assertNull(media.getContainer());
		MediaInfoParser.setFormat(StreamKind.GENERAL, media, video, audio, "unknown", new File("sample.WTV"));
		assertEquals("wtv", media.getContainer());
		MediaInfoParser.setFormat(StreamKind.GENERAL, media, video, audio, "unknown", new File("sample.MKV"));
		assertEquals("wtv", media.getContainer());
	}
}