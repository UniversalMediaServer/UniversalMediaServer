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
package net.pms.formats;

import java.io.File;
import java.io.FileNotFoundException;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.audio.M4A;
import net.pms.formats.audio.MP3;
import net.pms.formats.audio.OGA;
import net.pms.formats.audio.WAV;
import net.pms.formats.image.RAW;
import net.pms.formats.v2.SubtitleType;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.parsers.MediaInfoParser;
import net.pms.parsers.Parser;
import net.pms.renderers.Renderer;
import net.pms.store.item.RealFile;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test the recognition of formats using the MediaInfo and renderer
 * configuration.
 */
public class FormatRecognitionTest {

	private static boolean mediaInfoParserIsValid;

	@BeforeAll
	public static void setUpBeforeClass() throws ConfigurationException, InterruptedException {
		PMS.configureJNA();
		PMS.forceHeadless();

		// Initialize the RendererConfiguration
		UmsConfiguration configuration = new UmsConfiguration(false);
		PMS.setConfiguration(configuration);
		RendererConfigurations.loadRendererConfigurations();
		mediaInfoParserIsValid = MediaInfoParser.isValid();

		TestHelper.SetLoggingOff();
	}

	/**
	 * Test some basic functionality of
	 * {@link RendererConfiguration#isCompatible(MediaInfo, Format)}
	 */
	@Test
	public void testRendererConfigurationBasics() {
		// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);
		RendererConfiguration conf = RendererConfigurations.getRendererConfigurationByName("Playstation 3");
		assertNotNull(conf, "Renderer named \"Playstation 3\" not found.");
		assertFalse(conf.isCompatible(null, null),
				"With nothing provided isCompatible() should return false");
	}

	private Renderer getRenderer(RendererConfiguration conf) {
		Renderer renderer = null;
		try {
			renderer = new Renderer(conf);
		} catch (ConfigurationException | InterruptedException ex) {
		}
		return renderer;
	}

	/**
	 * Test the compatibility of the Playstation 3 with the MP3 format.
	 */
	@Test
	public void testPlaystationAudioMp3Compatibility() {
		// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfigurations.getRendererConfigurationByName("Playstation 3");
		assertNotNull(conf, "Renderer named \"Playstation 3\" not found.");
		Renderer renderer = getRenderer(conf);
		assertNotNull(conf, "Renderer named \"Playstation 3\" fail.");

		// Construct regular two channel MP3 information
		RealFile resource = new RealFile(renderer, new File("test.mkv"));
		MediaInfo info = new MediaInfo();
		info.setContainer(FormatConfiguration.MP3);
		info.setMimeType(HTTPResource.AUDIO_MP3_TYPEMIME);
		MediaAudio audio = new MediaAudio();
		audio.setNumberOfChannels(2);
		info.addAudioTrack(audio);
		resource.setMediaInfo(info);
		Format format = new MP3();
		format.match("test.mp3");
		assertTrue(conf.isCompatible(resource, format),
				"PS3 is compatible with MP3");

		// Construct five channel MP3 that the PS3 does not support natively
		audio.setNumberOfChannels(5);
		assertFalse(conf.isCompatible(resource, format),
				"PS3 is incompatible with five channel MP3");
	}

	/**
	 * Test the compatibility of the Playstation 3 with the MPG format.
	 */
	@Test
	public void testPlaystationVideoMpgCompatibility() {
		// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfigurations.getRendererConfigurationByName("Playstation 3");
		assertNotNull(conf, "Renderer named \"Playstation 3\" not found.");
		Renderer renderer = getRenderer(conf);
		assertNotNull(conf, "Renderer named \"Playstation 3\" fail.");

		RealFile resource = new RealFile(renderer, new File("test.mkv"));
		// Construct regular two channel MPG information
		MediaInfo info = new MediaInfo();
		info.setContainer(FormatConfiguration.AVI);
		MediaVideo video = new MediaVideo();
		video.setCodec(FormatConfiguration.MP4);
		info.addVideoTrack(video);
		MediaAudio audio = new MediaAudio();
		audio.setCodec(FormatConfiguration.AC3);
		audio.setNumberOfChannels(5);
		info.addAudioTrack(audio);
		Format format = new MPG();
		format.match("test.avi");
		resource.setMediaInfo(info);
		assertTrue(conf.isCompatible(resource, format),
				"PS3 is compatible with MPG");

		// Construct MPG with wmv codec that the PS3 does not support natively
		video.setCodec("wmv");
		assertFalse(conf.isCompatible(resource, format),
				"PS3 is incompatible with MPG with wmv codec");
	}

	/**
	 * Test the compatibility of the Playstation 3 with the MPG format.
	 */
	@Test
	public void testPlaystationVideoMkvCompatibility() {
		// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfigurations.getRendererConfigurationByName("Playstation 3");
		assertNotNull(conf, "Renderer named \"Playstation 3\" not found.");
		Renderer renderer = getRenderer(conf);
		assertNotNull(conf, "Renderer named \"Playstation 3\" fail.");

		RealFile resource = new RealFile(renderer, new File("test.mkv"));
		// Construct MKV information
		MediaInfo info = new MediaInfo();
		MediaVideo video = new MediaVideo();
		MediaAudio audio = new MediaAudio();
		info.setContainer(FormatConfiguration.MKV);
		video.setCodec(FormatConfiguration.MP4);
		info.addVideoTrack(video);
		audio.setCodec(FormatConfiguration.AC3);
		audio.setNumberOfChannels(5);
		info.addAudioTrack(audio);
		Format format = new MPG();
		format.match("test.mkv");
		resource.setMediaInfo(info);
		assertFalse(conf.isCompatible(resource, format), "PS3 is incompatible with MKV");
	}

	/**
	 * Test the compatibility of the
	 * {@link Format#isCompatible(MediaInfo, RendererConfiguration)} for the
	 * Sony Playstation 3 renderer.
	 */
	@Test
	public void testPS3Compatibility() {
		// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfigurations.getRendererConfigurationByName("Playstation 3");
		assertNotNull(conf, "Renderer named \"Playstation 3\" not found.");
		Renderer renderer = getRenderer(conf);
		assertNotNull(conf, "Renderer named \"Playstation 3\" fail.");

		RealFile resource = new RealFile(renderer, new File("test.mkv"));
		// DVRMS: false
		MediaInfo info = new MediaInfo();
		info.setContainer("dvr");
		Format format = new DVRMS();
		resource.setMediaInfo(info);
		assertTrue(format.match("test.dvr"), "Format \"test.dvr\" not matches DVRMS");
		assertFalse(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"true\" for DVRMS");

		// ISO: false
		info.setContainer(FormatConfiguration.ISO);
		format = new ISO();
		assertTrue(format.match("test.iso"), "Format not matches ISO");
		assertFalse(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"true\" for ISO");

		// M4A: true
		info.setContainer(FormatConfiguration.M4A);
		format = new M4A();
		assertTrue(format.match("test.m4a"), "Format \"test.m4a\" not matches M4A");
		assertTrue(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"false\" for M4A");

		// MKV: false
		info.setContainer(FormatConfiguration.MKV);
		format = new MKV();
		assertTrue(format.match("test.mkv"), "Format \"test.mkv\" not matches MKV");
		assertFalse(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"true\" for MKV");

		// MP3: true
		info.setContainer(FormatConfiguration.MP3);
		format = new MP3();
		assertTrue(format.match("test.mp3"), "Format \"test.mkv\" does not match MP3");
		assertTrue(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"false\" for MP3");

		// MPG: true);
		info.setContainer(FormatConfiguration.AVI);
		format = new MPG();
		assertTrue(format.match("test.mpg"), "Format \"test.mpg\" does not match MPG");
		assertTrue(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"false\" for MPG");

		// OGG: false
		info.setContainer(FormatConfiguration.OGG);
		format = new OGA();
		assertFalse(format.match("test.ogg"), "Format \"test.ogg\" does not match OGA");
		assertFalse(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"true\" for OGG");

		// RAW: false
		info.setContainer(FormatConfiguration.RAW);
		format = new RAW();
		assertTrue(format.match("test.raw"), "Format \"test.raw\" does not match RAW");
		assertFalse(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"true\"for RAW");

		// WAV: true
		info.setContainer(FormatConfiguration.WAV);
		format = new WAV();
		assertTrue(format.match("test.wav"), "Format \"test.raw\" does not match WAV");
		assertTrue(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"false\" for WAV");

		// WEB: type=VIDEO
		info.setContainer(FormatConfiguration.AVI);
		format.setType(Format.VIDEO);
		assertTrue(conf.isCompatible(resource, format),
				"isCompatible() gives the wrong outcome \"false\" for WEB video");
	}

	/**
	 * When UMS is in the process of starting up, something particular happens.
	 * The MediaStore is initialized and several VirtualVideoActions are added
	 * as children. VirtualVideoActions use the MPG format and, at the time of
	 * initialization getDefaultRenderer(), is used to determine whether or not
	 * the format can be streamed.
	 * <p>
	 * Under these conditions Format.isCompatible() must return true, or
	 * selecting the VirtualVideoAction will result in a "Corrupted data"
	 * message.
	 * <p>
	 * This test verifies the case above.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testVirtualVideoActionInitializationCompatibility() throws InterruptedException {
		// Continue the test if the configuration loaded, otherwise skip it.
		assumeTrue(RendererConfigurations.getDefaultConf() != null);

		// Continue the test if the LibMediaInfoParser can be loaded, otherwise skip it.
		assumeTrue(MediaInfoParser.isValid());

		RealFile resource = new RealFile(RendererConfigurations.getDefaultRenderer(), new File("test.mkv"));
		// Construct media info exactly as VirtualVideoAction does
		MediaInfo info = new MediaInfo();
		info.setContainer("mpegps");
		MediaVideo video = new MediaVideo();
		video.setCodec(FormatConfiguration.MPEG2);
		info.addVideoTrack(video);
		info.setMimeType("video/mpeg");
		info.setMediaParser(Parser.MANUAL_PARSER);
		Format format = new MPG();
		format.match("test.mpg");
		resource.setMediaInfo(info);

		// Test without rendererConfiguration, as can happen when plugins
		// create virtual video actions under a folder.
		assertTrue(format.isCompatible(resource, null),
				"VirtualVideoAction is initialized as compatible with null configuration");
	}

	/**
	 * Test the compatibility of the subtitles in
	 * {@link Format#isCompatible(MediaInfo, RendererConfiguration)} for given
	 * subtitles formats
	 *
	 * @throws FileNotFoundException
	 */
	@Test
	public void testSubtitlesRecognition() throws FileNotFoundException {
		// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);
		RendererConfiguration conf = RendererConfigurations.getRendererConfigurationByName("Panasonic TX-L32V10E");
		assertNotNull(conf, "Renderer named \"Panasonic TX-L32V10E\" not found.");
		Renderer renderer = getRenderer(conf);
		assertNotNull(conf, "Renderer named \"Playstation 3\" fail.");

		RealFile resource = new RealFile(renderer, new File("test.avi"));
		MediaInfo info = new MediaInfo();
		MediaVideo video = new MediaVideo();
		MediaAudio audio = new MediaAudio();
		MediaSubtitle subs = new MediaSubtitle();
		info.setContainer(FormatConfiguration.AVI);
		video.setCodec(FormatConfiguration.MP4);
		audio.setCodec(FormatConfiguration.AC3);
		info.addVideoTrack(video);
		info.addAudioTrack(audio);
		resource.setMediaInfo(info);

		// SUBRIP external: true
		subs.setExternalFileOnly(new File("test.srt"));
		subs.setType(SubtitleType.SUBRIP);
		resource.setMediaSubtitle(subs);
		assertTrue(conf.isCompatible(resource, null), "isCompatible() gives the wrong outcome \"false\" for external SUBRIP format");

		//ASS external: false
		subs.setExternalFileOnly(new File("test.ass"));
		subs.setType(SubtitleType.ASS);
		resource.setMediaSubtitle(subs);
		assertFalse(conf.isCompatible(resource, null), "isCompatible() gives the wrong outcome \"true\" for external ASS format");

		//DIVX internal: true
		subs.setExternalFileOnly(null);
		subs.setType(SubtitleType.DIVX);
		resource.setMediaSubtitle(subs);
		assertTrue(conf.isCompatible(resource, null), "isCompatible() gives the wrong outcome \"false\" for embedded DIVX format");

		//PGS internal: false
		subs.setExternalFileOnly(null);
		subs.setType(SubtitleType.PGS);
		resource.setMediaSubtitle(subs);
		assertFalse(conf.isCompatible(resource, null), "isCompatible() gives the wrong outcome \"true\" for embedded PGS format");
	}
}
