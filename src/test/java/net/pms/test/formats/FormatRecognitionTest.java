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
package net.pms.test.formats;

import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.formats.DVRMS;
import net.pms.formats.Format;
import net.pms.formats.ISO;
import net.pms.formats.MKV;
import net.pms.formats.MPG;
import net.pms.formats.audio.M4A;
import net.pms.formats.audio.MP3;
import net.pms.formats.audio.OGA;
import net.pms.formats.audio.WAV;
import net.pms.formats.image.RAW;
import net.pms.formats.v2.SubtitleType;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.HTTPResource;
import net.pms.parsers.MediaInfoParser;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Test the recognition of formats using the MediaInfo and renderer configuration.
 */
public class FormatRecognitionTest {
	private static boolean mediaInfoParserIsValid;
	private static UmsConfiguration configuration;

	@BeforeAll
	public static void setUpBeforeClass() throws ConfigurationException, InterruptedException {
		PMS.configureJNA();
		PMS.forceHeadless();
		try {
			PMS.setConfiguration(new UmsConfiguration(false));
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
		RendererConfigurations.loadRendererConfigurations();
		mediaInfoParserIsValid = MediaInfoParser.isValid();

		// Silence all log messages from the UMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		// Initialize the RendererConfiguration
		configuration = new UmsConfiguration(false);
	}

    /**
     * Test some basic functionality of {@link RendererConfiguration#isCompatible(MediaInfo, Format)}
     */
    @Test
    public void testRendererConfigurationBasics() {
    	// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);
		RendererConfiguration conf = RendererConfigurations.getRendererConfigurationByName("Playstation 3");
		assertNotNull(conf, "Renderer named \"Playstation 3\" not found.");
		assertFalse(conf.isCompatible(null, null, configuration),
			"With nothing provided isCompatible() should return false");
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

		// Construct regular two channel MP3 information
		DLNAResource dlna = new RealFile(new File("test.mkv"));
		MediaInfo info = new MediaInfo();
		info.setContainer("mp3");
		info.setMimeType(HTTPResource.AUDIO_MP3_TYPEMIME);
		MediaAudio audio = new MediaAudio();
		audio.getAudioProperties().setNumberOfChannels(2);
		List<MediaAudio> audioCodes = new ArrayList<>();
		audioCodes.add(audio);
		info.setAudioTracks(audioCodes);
		dlna.setMedia(info);
		Format format = new MP3();
		format.match("test.mp3");
		assertTrue(conf.isCompatible(dlna, format, configuration),
			"PS3 is compatible with MP3");

		// Construct five channel MP3 that the PS3 does not support natively
		audio.getAudioProperties().setNumberOfChannels(5);
		assertFalse(conf.isCompatible(dlna, format, configuration),
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

		DLNAResource dlna = new RealFile(new File("test.mkv"));
		// Construct regular two channel MPG information
		MediaInfo info = new MediaInfo();
		info.setContainer("avi");
		MediaAudio audio = new MediaAudio();
		audio.setCodecA("ac3");
		audio.getAudioProperties().setNumberOfChannels(5);
		List<MediaAudio> audioCodes = new ArrayList<>();
		audioCodes.add(audio);
		info.setAudioTracks(audioCodes);
		info.setCodecV("mp4");
		Format format = new MPG();
		format.match("test.avi");
		dlna.setMedia(info);
		assertTrue(conf.isCompatible(dlna, format, configuration),
			"PS3 is compatible with MPG");

		// Construct MPG with wmv codec that the PS3 does not support natively
		info.setCodecV("wmv");
		assertFalse(conf.isCompatible(dlna, format, configuration),
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

		DLNAResource dlna = new RealFile(new File("test.mkv"));
		// Construct MKV information
		MediaInfo info = new MediaInfo();
		info.setContainer("mkv");
		MediaAudio audio = new MediaAudio();
		audio.setCodecA("ac3");
		audio.getAudioProperties().setNumberOfChannels(5);
		List<MediaAudio> audioCodes = new ArrayList<>();
		audioCodes.add(audio);
		info.setAudioTracks(audioCodes);
		info.setCodecV("mp4");
		Format format = new MPG();
		format.match("test.mkv");
		dlna.setMedia(info);
		assertFalse(conf.isCompatible(dlna, format, configuration), "PS3 is incompatible with MKV");
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

		DLNAResource dlna = new RealFile(new File("test.mkv"));
		// DVRMS: false
		MediaInfo info = new MediaInfo();
		info.setContainer("dvr");
		Format format = new DVRMS();
		dlna.setMedia(info);
		assertTrue(format.match("test.dvr"), "Format \"test.dvr\" not matches DVRMS");
		assertFalse(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"true\" for DVRMS");

		// ISO: false
		info.setContainer("iso");
		format = new ISO();
		assertTrue(format.match("test.iso"), "Format not matches ISO");
		assertFalse(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"true\" for ISO");

		// M4A: true
		info.setContainer("m4a");
		format = new M4A();
		assertTrue(format.match("test.m4a"), "Format \"test.m4a\" not matches M4A");
		assertTrue(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"false\" for M4A");

		// MKV: false
		info.setContainer("mkv");
		format = new MKV();
		assertTrue(format.match("test.mkv"), "Format \"test.mkv\" not matches MKV");
		assertFalse(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"true\" for MKV");

		// MP3: true
		info.setContainer("mp3");
		format = new MP3();
		assertTrue(format.match("test.mp3"), "Format \"test.mkv\" does not match MP3");
		assertTrue(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"false\" for MP3");

		// MPG: true);
		info.setContainer("avi");
		format = new MPG();
		assertTrue(format.match("test.mpg"), "Format \"test.mpg\" does not match MPG");
		assertTrue(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"false\" for MPG");

		// OGG: false
		info.setContainer("ogg");
		format = new OGA();
		assertFalse(format.match("test.ogg"), "Format \"test.ogg\" does not match OGA");
		assertFalse(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"true\" for OGG");

		// RAW: false
		info.setContainer("raw");
		format = new RAW();
		assertTrue(format.match("test.raw"), "Format \"test.raw\" does not match RAW");
		assertFalse(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"true\"for RAW");

		// WAV: true
		info.setContainer("wav");
		format = new WAV();
		assertTrue(format.match("test.wav"), "Format \"test.raw\" does not match WAV");
		assertTrue(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"false\" for WAV");

		// WEB: type=VIDEO
		info.setContainer("avi");
		format.setType(Format.VIDEO);
		assertTrue(conf.isCompatible(dlna, format, configuration),
			"isCompatible() gives the wrong outcome \"false\" for WEB video");
	}

	/**
	 * When UMS is in the process of starting up, something particular happens.
	 * The RootFolder is initialized and several VirtualVideoActions are added
	 * as children. VirtualVideoActions use the MPG format and, at the time of
	 * initialization getDefaultRenderer(), is used to determine whether or not
	 * the format can be streamed.
	 * <p>
	 * Under these conditions Format.isCompatible() must return true, or
	 * selecting the VirtualVideoAction will result in a "Corrupted data"
	 * message.
	 * <p>
	 * This test verifies the case above.
	 * @throws InterruptedException
	 */
	@Test
	public void testVirtualVideoActionInitializationCompatibility() throws InterruptedException {
		// Continue the test if the configuration loaded, otherwise skip it.
		assumeTrue(RendererConfigurations.getDefaultConf() != null);

		// Continue the test if the LibMediaInfoParser can be loaded, otherwise skip it.
		assumeTrue(MediaInfoParser.isValid());

		DLNAResource dlna = new RealFile(new File("test.mkv"));
		// Construct media info exactly as VirtualVideoAction does
		MediaInfo info = new MediaInfo();
		info.setContainer("mpegps");
		List<MediaAudio> audioCodes = new ArrayList<>();
		info.setAudioTracks(audioCodes);
		info.setMimeType("video/mpeg");
		info.setCodecV("mpeg2");
		info.setMediaparsed(true);
		Format format = new MPG();
		format.match("test.mpg");
		dlna.setMedia(info);

		// Test without rendererConfiguration, as can happen when plugins
		// create virtual video actions under a folder.

		assertTrue(format.isCompatible(dlna, null),
			"VirtualVideoAction is initialized as compatible with null configuration");
	}

	/**
	 * Test the compatibility of the subtitles in
	 * {@link Format#isCompatible(MediaInfo, RendererConfiguration)} for
	 * given subtitles formats
	 * @throws FileNotFoundException 
	 */
	@Test
	public void testSubtitlesRecognition() throws FileNotFoundException {
    	// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);
		RendererConfiguration renderer = RendererConfigurations.getRendererConfigurationByName("Panasonic TX-L32V10E");
		assertNotNull(renderer, "Renderer named \"Panasonic TX-L32V10E\" not found.");
		
		DLNAResource dlna = new RealFile(new File("test.avi"));
		MediaInfo info = new MediaInfo();
		MediaAudio audio = new MediaAudio();
		MediaSubtitle subs = new MediaSubtitle();
		audio.setCodecA(FormatConfiguration.AC3);
		info.setContainer(FormatConfiguration.AVI);
		info.setCodecV(FormatConfiguration.MP4);
		info.getAudioTracksList().add(audio);
		dlna.setMedia(info);

		// SUBRIP external: true
		subs.setExternalFileOnly(new File("test.srt"));
		subs.setType(SubtitleType.SUBRIP);
		dlna.setMediaSubtitle(subs);
		assertTrue(renderer.isCompatible(dlna, null), "isCompatible() gives the wrong outcome \"false\" for external SUBRIP format");

		//ASS external: false
		subs.setExternalFileOnly(new File("test.ass"));
		subs.setType(SubtitleType.ASS);
		dlna.setMediaSubtitle(subs);
		assertFalse(renderer.isCompatible(dlna, null), "isCompatible() gives the wrong outcome \"true\" for external ASS format");
		
		//DIVX internal: true
		subs.setExternalFileOnly(null);
		subs.setType(SubtitleType.DIVX);
		dlna.setMediaSubtitle(subs);
		assertTrue(renderer.isCompatible(dlna, null), "isCompatible() gives the wrong outcome \"false\" for embedded DIVX format");

		//PGS internal: false
		subs.setExternalFileOnly(null);
		subs.setType(SubtitleType.PGS);
		dlna.setMediaSubtitle(subs);
		assertFalse(renderer.isCompatible(dlna, null), "isCompatible() gives the wrong outcome \"true\" for embedded PGS format");
	}
}
