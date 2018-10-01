/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms.test.formats;

import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.LibMediaInfoParser;
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
import net.pms.network.HTTPResource;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Test the recognition of formats.
 */
public class FormatRecognitionTest {
	private static boolean mediaInfoParserIsValid;
	private final static PmsConfiguration configuration = new PmsConfiguration(false);

	@BeforeClass
	public static void setUpBeforeClass() throws ConfigurationException {
		PMS.configureJNA();
		mediaInfoParserIsValid = LibMediaInfoParser.isValid();

		// Silence all log messages from the DMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		// Initialize the RendererConfiguration
		RendererConfiguration.loadRendererConfigurations(configuration);
	}

    /**
     * Test some basic functionality of {@link RendererConfiguration#isCompatible(DLNAMediaInfo, Format)}
     */
    @Test
	public void testRendererConfigurationBasics() {
    	// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);
		RendererConfiguration conf = RendererConfiguration.getRendererConfigurationByName("Playstation 3");
		assertNotNull("Renderer named \"Playstation 3\" found.", conf);
		assertEquals("With nothing provided isCompatible() should return false", false,
				conf.isCompatible(null, null, configuration));
	}

	/**
	 * Test the compatibility of the Playstation 3 with the MP3 format.
	 */
	@Test
	public void testPlaystationAudioMp3Compatibility() {
    	// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfiguration.getRendererConfigurationByName("Playstation 3");
		assertNotNull("Renderer named \"Playstation 3\" found.", conf);

		// Construct regular two channel MP3 information
		DLNAMediaInfo info = new DLNAMediaInfo();
		info.setContainer("mp3");
		info.setMimeType(HTTPResource.AUDIO_MP3_TYPEMIME);
		DLNAMediaAudio audio = new DLNAMediaAudio();
		audio.getAudioProperties().setNumberOfChannels(2);
		List<DLNAMediaAudio> audioCodes = new ArrayList<>();
		audioCodes.add(audio);
		info.setAudioTracksList(audioCodes);
		Format format = new MP3();
		format.match("test.mp3");
		assertEquals("PS3 is compatible with MP3", true,
				conf.isCompatible(info, format, configuration));

		// Construct five channel MP3 that the PS3 does not support natively
		audio.getAudioProperties().setNumberOfChannels(5);
		assertEquals("PS3 is incompatible with five channel MP3", false,
				conf.isCompatible(info, format, configuration));
	}

	/**
	 * Test the compatibility of the Playstation 3 with the MPG format.
	 */
	@Test
	public void testPlaystationVideoMpgCompatibility() {
    	// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfiguration.getRendererConfigurationByName("Playstation 3");
		assertNotNull("Renderer named \"Playstation 3\" found.", conf);

		// Construct regular two channel MPG information
		DLNAMediaInfo info = new DLNAMediaInfo();
		info.setContainer("avi");
		DLNAMediaAudio audio = new DLNAMediaAudio();
		audio.setCodecA("ac3");
		audio.getAudioProperties().setNumberOfChannels(5);
		List<DLNAMediaAudio> audioCodes = new ArrayList<>();
		audioCodes.add(audio);
		info.setAudioTracksList(audioCodes);
		info.setCodecV("mp4");
		Format format = new MPG();
		format.match("test.avi");
		assertEquals("PS3 is compatible with MPG", true,
				conf.isCompatible(info, format, configuration));

		// Construct MPG with wmv codec that the PS3 does not support natively
		info.setCodecV("wmv");
		assertEquals("PS3 is incompatible with MPG with wmv codec", false,
				conf.isCompatible(info, format, configuration));
	}

	/**
	 * Test the compatibility of the Playstation 3 with the MPG format.
	 */
	@Test
	public void testPlaystationVideoMkvCompatibility() {
    	// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfiguration.getRendererConfigurationByName("Playstation 3");
		assertNotNull("Renderer named \"Playstation 3\" found.", conf);

		// Construct MKV information
		DLNAMediaInfo info = new DLNAMediaInfo();
		info.setContainer("mkv");
		DLNAMediaAudio audio = new DLNAMediaAudio();
		audio.setCodecA("ac3");
		audio.getAudioProperties().setNumberOfChannels(5);
		List<DLNAMediaAudio> audioCodes = new ArrayList<>();
		audioCodes.add(audio);
		info.setAudioTracksList(audioCodes);
		info.setCodecV("mp4");
		Format format = new MPG();
		format.match("test.mkv");
		assertEquals("PS3 is incompatible with MKV", false,
				conf.isCompatible(info, format, configuration));
	}

	/**
	 * Test the compatibility of the
	 * {@link Format#isCompatible(DLNAMediaInfo, RendererConfiguration)} for the
	 * Playstation 3 renderer.
	 */
	@Test
	public void testPS3Compatibility() {
    	// This test is only useful if the MediaInfo library is available
		assumeTrue(mediaInfoParserIsValid);

		RendererConfiguration conf = RendererConfiguration.getRendererConfigurationByName("Playstation 3");
		assertNotNull("Renderer named \"Playstation 3\" found.", conf);

		// DVRMS: false
		DLNAMediaInfo info = new DLNAMediaInfo();
		info.setContainer("dvr");
		Format format = new DVRMS();
		format.match("test.dvr");
		assertFalse("isCompatible() gives the outcome false for DVRMS",	conf.isCompatible(info, format, configuration));

		// ISO: false
		info = new DLNAMediaInfo();
		info.setContainer("iso");
		format = new ISO();
		format.match("test.iso");
		assertFalse("isCompatible() gives the outcome false for ISO", conf.isCompatible(info, format, configuration));

		// M4A: false
		info = new DLNAMediaInfo();
		info.setContainer("m4a");
		format = new M4A();
		format.match("test.m4a");
		assertTrue("isCompatible() gives the outcome true for M4A", conf.isCompatible(info, format, configuration));

		// MKV: false
		info = new DLNAMediaInfo();
		info.setContainer("mkv");
		format = new MKV();
		format.match("test.mkv");
		assertFalse("isCompatible() gives the outcome false for MKV", conf.isCompatible(info, format, configuration));

		// MP3: true
		info = new DLNAMediaInfo();
		info.setContainer("mp3");
		format = new MP3();
		format.match("test.mp3");
		assertTrue("isCompatible() gives the outcome true for MP3", conf.isCompatible(info, format, configuration));

		// MPG: true
		info = new DLNAMediaInfo();
		info.setContainer("avi");
		format = new MPG();
		format.match("test.mpg");
		assertTrue("isCompatible() gives the outcome true for MPG", conf.isCompatible(info, format, configuration));

		// OGG: false
		info = new DLNAMediaInfo();
		info.setContainer("ogg");
		format = new OGA();
		format.match("test.ogg");
		assertFalse("isCompatible() gives the outcome false for OGG", conf.isCompatible(info, format, configuration));

		// RAW: false
		info = new DLNAMediaInfo();
		info.setContainer("raw");
		format = new RAW();
		format.match("test.arw");
		assertFalse("isCompatible() gives the outcome false for RAW", conf.isCompatible(info, format, configuration));

		// WAV: true
		info = new DLNAMediaInfo();
		info.setContainer("wav");
		format = new WAV();
		format.match("test.wav");
		assertTrue("isCompatible() gives the outcome true for WAV", conf.isCompatible(info, format, configuration));

		// WEB: type=VIDEO
		info = new DLNAMediaInfo();
		info.setContainer("avi");
		format.setType(Format.VIDEO);
		assertTrue("isCompatible() gives the outcome true for WEB video", conf.isCompatible(info, format, configuration));
	}

	/**
	 * When DMS is in the process of starting up, something particular happens.
	 * The RootFolder is initialized and several VirtualVideoActions are added
	 * as children. VirtualVideoActions use the MPG format and at the time of
	 * initialization getDefaultRenderer() is used to determine whether or not
	 * the format can be streamed.
	 * <p>
	 * Under these conditions Format.isCompatible() must return true, or
	 * selecting the VirtualVideoAction will result in a "Corrupted data"
	 * message.
	 * <p>
	 * This test verifies the case above.
	 */
	@Test
	public void testVirtualVideoActionInitializationCompatibility() {
		boolean configurationLoaded = false;

		try {
			// Initialize DMS configuration like at initialization time, this
			// is relevant for RendererConfiguration.isCompatible().
			PMS.setConfiguration(new PmsConfiguration());
			configurationLoaded = true;
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

		// Continue the test if the configuration loaded, otherwise skip it.
		assumeTrue(configurationLoaded);

		// Continue the test if the LibMediaInfoParser can be loaded, otherwise skip it.
		assumeTrue(LibMediaInfoParser.isValid());

		// Construct media info exactly as VirtualVideoAction does
		DLNAMediaInfo info = new DLNAMediaInfo();
		info.setContainer("mpegps");
		List<DLNAMediaAudio> audioCodes = new ArrayList<>();
		info.setAudioTracksList(audioCodes);
		info.setMimeType("video/mpeg");
		info.setCodecV("mpeg2");
		info.setMediaparsed(true);
		Format format = new MPG();
		format.match("test.mpg");

		// Test without rendererConfiguration, as can happen when plugins
		// create virtual video actions under a folder.

		assertEquals("VirtualVideoAction is initialized as compatible with null configuration",
				true, format.isCompatible(info, null));
	}

}
