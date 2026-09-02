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
import java.util.Locale;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.util.InputFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards the hand-off between the parsers and the renderer configurations: a parsed audio file must
 * describe itself in the vocabulary the "Supported" lines are written in, otherwise no renderer ever
 * matches an audio file and every one of them silently falls back to a default MIME type.
 */
public class AudioSupportedMatchingTest {

	@BeforeAll
	public static void setUpClass() {
		ParserTest.setUpClass();
	}

	@BeforeEach
	public void setUp() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		PMS.setLocale(Locale.ENGLISH);
		PMS.setConfiguration(new UmsConfiguration(false));
		RendererConfigurations.loadRendererConfigurations();
	}

	private static MediaInfo parse(String testFile) {
		File file = ParserTest.getTestFile(testFile);
		Format format = FormatFactory.getAssociatedFormat(file.getAbsolutePath());
		InputFile inputFile = new InputFile();
		inputFile.setFile(file);
		MediaInfo mediaInfo = new MediaInfo();
		Parser.parse(mediaInfo, inputFile, format, format.getType());
		return mediaInfo;
	}

	private static void assertParsedAs(String testFile, String container, String codec) {
		MediaInfo mediaInfo = parse(testFile);
		MediaAudio audio = mediaInfo.getDefaultAudioTrack();
		assertEquals(container, mediaInfo.getContainer(), testFile + " container");
		assertEquals(codec, audio == null ? null : audio.getCodec(), testFile + " codec");
	}

	/**
	 * The parsers must emit {@link FormatConfiguration} tokens, not the audio header prose
	 * JAudiotagger reports ("FLAC 24 bits", "MPEG-1 Layer 3", "WAV PCM 16 bits").
	 */
	@Test
	public void testParsedAudioUsesFormatConfigurationTokens() {
		assertParsedAs("audio-flac24.flac", FormatConfiguration.FLAC, FormatConfiguration.FLAC);
		assertParsedAs("audio-lpcm.wav", FormatConfiguration.WAV, FormatConfiguration.LPCM);
		assertParsedAs("audio-mp3.mp3", FormatConfiguration.MP3, FormatConfiguration.MP3);
		assertParsedAs("audio-vorbis.oga", FormatConfiguration.OGA, FormatConfiguration.VORBIS);
	}

	/**
	 * End-to-end: a parsed file must resolve to the MIME type its renderer profile declares, so a
	 * profile can actually steer what is streamed natively and what is transcoded.
	 */
	@Test
	public void testRendererSupportedLinesMatchParsedAudio() {
		RendererConfiguration renderer = RendererConfigurations.getRendererConfigurationByName("NextCP/2 Web Player");
		assertEquals("nextcp2webplayer.conf", renderer.getFile().getName(), "test renderer");

		assertMatchedMimeType(renderer, "audio-flac24.flac", "audio/flac");
		assertMatchedMimeType(renderer, "audio-lpcm.wav", "audio/wav");
		assertMatchedMimeType(renderer, "audio-mp3.mp3", "audio/mpeg");
		assertMatchedMimeType(renderer, "audio-vorbis.oga", "audio/ogg");
	}

	/**
	 * A re-parse starts from the MediaInfo read back from the database, container included. The parser
	 * must overwrite it, otherwise a stored container can never be corrected.
	 */
	@Test
	public void testReparseOverwritesStoredContainer() {
		File file = ParserTest.getTestFile("audio-flac24.flac");
		Format format = FormatFactory.getAssociatedFormat(file.getAbsolutePath());
		MediaInfo mediaInfo = new MediaInfo();
		mediaInfo.setContainer("flac 24 bits");
		InputFile inputFile = new InputFile();
		inputFile.setFile(file);
		Parser.parse(mediaInfo, inputFile, format, format.getType());
		assertEquals(FormatConfiguration.FLAC, mediaInfo.getContainer(), "stale container");
	}

	private static void assertMatchedMimeType(RendererConfiguration renderer, String testFile, String expected) {
		MediaInfo mediaInfo = parse(testFile);
		MediaAudio audio = mediaInfo.getDefaultAudioTrack();
		assertEquals(
			expected,
			renderer.getFormatConfiguration().getMatchedMIMEtype(
				mediaInfo.getContainer(),
				null,
				audio == null ? null : audio.getCodec()),
			testFile + " matched MIME type"
		);
	}
}
