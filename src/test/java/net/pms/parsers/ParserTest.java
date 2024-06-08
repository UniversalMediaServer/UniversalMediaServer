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
import java.util.logging.Level;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.encoders.EngineFactory;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.service.Services;
import net.pms.util.InputFile;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ParserTest {
	private static final Class<?> CLASS = ParserTest.class;

	@BeforeAll
	public static void setUpClass() {
		PMS.configureJNA();
		TestHelper.SetLoggingOff();
		//silent org.jaudiotagger
		org.jaudiotagger.audio.wav.WavTagReader.logger.setLevel(Level.OFF);
		org.jaudiotagger.audio.flac.FlacInfoReader.logger.setLevel(Level.OFF);
		org.jaudiotagger.tag.id3.ID3v23Tag.logger.setLevel(Level.OFF);
		try {
			PMS.setConfiguration(new UmsConfiguration(false));
			PMS.getConfiguration().setExternalNetwork(false);
			Services.destroy();
			Services.create();
			EngineFactory.initialize();
		} catch (InterruptedException | ConfigurationException ex) {
			throw new AssertionError(ex);
		}
	}

	public static File getTestFile(String testFile) {
		return FileUtils.toFile(CLASS.getResource(testFile));
	}

	private static MediaInfo getTestFileMediaInfo(String testFile) {
		File file = getTestFile(testFile);
		Format format = FormatFactory.getAssociatedFormat(file.getAbsolutePath());
		InputFile inputFile = new InputFile();
		inputFile.setFile(file);
		MediaInfo mediaInfo = new MediaInfo();
		Parser.parse(mediaInfo, inputFile, format, format.getType());
		return mediaInfo;
	}

	@Test
	public void testParserChoice() throws Exception {
		//should use MediaInfo parser
		assertEquals(
			MediaInfoParser.PARSER_NAME,
			getTestFileMediaInfo("video-h264-aac.mp4").getMediaParser()
		);
		//should fallback to MetadataExtractor parser
		assertEquals(
			MetadataExtractorParser.PARSER_NAME,
			getTestFileMediaInfo("image-bmp.bmp").getMediaParser()
		);
		//should fallback to MediaInfo parser
		assertEquals(
			MediaInfoParser.PARSER_NAME,
			getTestFileMediaInfo("audio-realmedia.ra").getMediaParser()
		);

		//disable MediaInfoParser
		MediaInfoParser.block();
		//should fallback to FFmpegParser when MediaInfoParser is not found
		if (FFmpegParser.isValid()) {
			assertEquals(
				FFmpegParser.PARSER_NAME,
				getTestFileMediaInfo("video-h264-aac.mp4").getMediaParser()
			);
		}
		//should fallback to JaudiotaggerParser when MediaInfoParser is not found
		assertEquals(
			JaudiotaggerParser.PARSER_NAME,
			getTestFileMediaInfo("audio-mp3-infos.mp3").getMediaParser()
		);
		MediaInfoParser.unblock();

	}

}
