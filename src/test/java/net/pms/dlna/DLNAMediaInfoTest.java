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
package net.pms.dlna;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.sharedcontent.SharedContentArray;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.parsers.MediaInfoParser;
import net.pms.service.Services;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLNAMediaInfoTest {
	private static final Class<?> CLASS = DLNAMediaInfoTest.class;

	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaInfoTest.class.getName());

	// Look for more formats at https://samples.ffmpeg.org/
	private static final String[] test_files = {
		"video-h264-aac.mp4",
		"video-mpeg4-aac.mkv",
		"video-h265-aac.mkv",
		"video-theora-vorbis.ogg",
		"video-h264-aac.m4v",
		"video-mp4-aac.3g2",
		"video-mp4-adpcm.avi",
		"video-mp4-aac.mov",
		"video-wmv-wma.wmv",
		"video-vp8-vorbis.webm",
		"video-sor-aac.flv",
		"video-h264-aac.avi",
		"audio-lpcm.wav",
		"audio-vorbis.oga",
		"audio-mp3.mp3",
		"video-av1-aac.mp4",
		"video-av1.mp4",
		"video-vc1.mkv",
		"video-h264-heaac.mp4",
		"video-h264-eac3.mkv",
		"audio-flac24.flac",
		"video-xvid-mp3.avi",
		"video-h265_dolbyvision_p05.05-eac3_atmos.mkv",
		"video-h265_dolbyvision_p08.05-eac3_atmos.mkv",
		"video-h265_hdr10-aac.mkv",
		"video-h265_hdr10+-dtshd_ma.mkv",
	};

	/**
	 * Set up testing conditions before running the tests.
	 *
	 * @throws ConfigurationException
	 */
	@BeforeAll
	public static final void setUp() throws ConfigurationException, InterruptedException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
		PMS.configureJNA();
		PMS.forceHeadless();
		try {
			PMS.setConfiguration(new UmsConfiguration(false));
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
		assert PMS.getConfiguration() != null;
		PMS.getConfiguration().setAutomaticMaximumBitrate(false); // do not test the network speed.
		SharedContentConfiguration.updateSharedContent(new SharedContentArray(), false);
		PMS.getConfiguration().setScanSharedFoldersOnStartup(false);
		PMS.getConfiguration().setUseCache(false);

		Services.destroy();

		try {
			PMS.getConfiguration().initCred();
		} catch (Exception ex) {
			LOGGER.warn("Failed to write credentials configuration", ex);
		}

		// Create a new PMS instance
		PMS.getNewInstance();
	}

	@Test
	public void testContainerProperties() throws Exception {
		// Check if the MediaInfo library is properly installed and initialized
		// especially on Linux which needs users to be involved.
		assertTrue(MediaInfoParser.isValid() ,
			"\r\nYou do not appear to have MediaInfo installed on your machine, please install it before running this test\r\n");

		// Create handles to the test content
		// This comes from RequestV2::answer()
		DLNAResource parent = new VirtualFolder("test", "test");
		parent.setDefaultRenderer(RendererConfigurations.getDefaultRenderer());
		PMS.getGlobalRepo().add(parent);

		for (int i = 0; i < test_files.length; ++i) {
			DLNAResource dlna = new RealFile(FileUtils.toFile(CLASS.getResource(test_files[i])));
			dlna.setParent(parent);
			dlna.resolveFormat();
			dlna.syncResolve();

			DLNAMediaInfo mediaInfo = dlna.getMedia();

			switch(i) {
				case 0:
					assertEquals(
						"Container: MP4, Size: 1325017, Overall Bitrate: 676979, Video Tracks: 1, Video Codec: h264, Duration: 0:00:15.658, Video Resolution: 640 x 360, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.601, Reference Frame Count: 4, AVC Level: 3, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 125547, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/mp4",
						mediaInfo.toString()
					);
					break;
				case 1:
					assertEquals(
						"Container: MKV, Size: 2097841, Overall Bitrate: 1575843, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:10.650, Video Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate Mode: VFR (VFR), Frame Rate Mode Raw: VFR, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 0, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				case 2:
					assertEquals(
						"Container: MKV, Size: 5291494, Overall Bitrate: 2619551, Video Tracks: 1, Video Codec: h265, Duration: 0:00:16.160, Video Resolution: 1920 x 960, Display Aspect Ratio: 2.00:1, Frame Rate: 25.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.709, Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Track Title From Metadata: Stereo, Audio Codec: AAC-LC, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				case 3:
					assertEquals(
						"Container: OGG, Size: 1734919, Overall Bitrate: 454643, Video Tracks: 1, Video Codec: theora, Duration: 0:00:30.528, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 30.000, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/ogg",
						mediaInfo.toString()
					);
					break;
				case 4:
					assertEquals(
						"Container: MP4, Size: 3538130, Overall Bitrate: 542149, Video Tracks: 1, Video Codec: h264, Duration: 0:00:52.209, Video Resolution: 720 x 480, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.563, Scan Type: Progressive, Frame Rate: 24.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Reference Frame Count: 4, AVC Level: 3, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 128290, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/mp4",
						mediaInfo.toString()
					);
					break;
				case 5:
					assertEquals(
						"Container: 3G2, Size: 1792091, Overall Bitrate: 275410, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.056, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Frame Rate: 18.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 59721, Channels: 2, Sample Frequency: 22050 Hz], Mime Type: video/3gpp2",
						mediaInfo.toString()
					);
					break;
				case 6:
					assertEquals(
						"Container: AVI, Size: 3893340, Overall Bitrate: 598711, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.023, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Frame Rate: 24.000, Video Track Title from Metadata: Sintel Trailer, Audio Tracks: 1 [Audio Codec: ADPCM, Bitrate: 128000, Bits per Sample: 4, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/avi",
						mediaInfo.toString()
					);
					break;
				case 7:
					assertEquals(
						"Container: MOV, Size: 2658492, Overall Bitrate: 408339, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.084, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Frame Rate: 24.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: Sintel Trailer, Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Codec: AAC-LC, Bitrate: 125805, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/quicktime",
						mediaInfo.toString()
					);
					break;
				case 8:
					assertEquals(
						"Container: WMV, Size: 3002945, Overall Bitrate: 460107, Video Tracks: 1, Video Codec: wmv, Duration: 0:00:52.213, Video Resolution: 360 x 240, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.563, Frame Rate: 24.000, Audio Tracks: 1 [Audio Codec: WMA, Bitrate: 128000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/x-ms-wmv",
						mediaInfo.toString()
					);
					break;
				case 9:
					assertEquals(
						"Container: WEBM, Size: 901185, Overall Bitrate: 236044, Video Tracks: 1, Video Codec: vp8, Duration: 0:00:30.543, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 30.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/webm",
						mediaInfo.toString()
					);
					break;
				case 10:
					assertEquals(
						"Container: FLV, Size: 2097492, Overall Bitrate: 1529899, Video Tracks: 1, Video Codec: sor, Duration: 0:00:10.968, Video Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 375000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-flv",
						mediaInfo.toString()
					);
					break;
				case 11:
					assertEquals(
						"Container: AVI, Size: 742478, Overall Bitrate: 194029, Video Tracks: 1, Video Codec: h264, Duration: 0:00:30.613, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 30.000, Frame Rate Mode: VFR (VFR), Frame Rate Mode Raw: VFR, Reference Frame Count: 4, AVC Level: 2.1, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 139632, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi",
						mediaInfo.toString()
					);
					break;
				case 12:
					assertEquals(
						"Container: WAV, Size: 1073218, Overall Bitrate: 256062, Bitrate: 256062, Duration: 0:00:33.530, Audio Tracks: 1 [Audio Codec: LPCM, Bitrate: 256000, Channels: 2, Sample Frequency: 8000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/wav",
						mediaInfo.toString()
					);
					break;
				case 13:
					assertEquals(
						"Container: OGA, Size: 1089524, Overall Bitrate: 117233, Bitrate: 117233, Duration: 0:01:14.349, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 120000, Channels: 2, Sample Frequency: 32000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/ogg",
						mediaInfo.toString()
					);
					break;
				case 14:
					assertEquals(
						"Container: MP3, Size: 764176, Overall Bitrate: 224000, Bitrate: 224000, Duration: 0:00:27.252, Audio Tracks: 1 [Audio Codec: MP3, Bitrate: 224000, Channels: 2, Sample Frequency: 32000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/mpeg",
						mediaInfo.toString()
					);
					break;
				case 15:
					assertEquals(
						"Container: MP4, Size: 245747, Overall Bitrate: 130716, Video Tracks: 1, Video Codec: av1, Duration: 0:00:15.040, Video Resolution: 960 x 540, Display Aspect Ratio: 16:9, Frame Rate: 25.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: vid, Audio Tracks: 1 [Id: 0, Language Code: snd, Audio Track Title From Metadata: snd, Audio Codec: AAC-LC, Bitrate: 8887, Channel: 1, Sample Frequency: 32000 Hz], Mime Type: video/mp4",
						mediaInfo.toString()
					);
					break;
				case 16:
					assertEquals(
						"Container: MP4, Size: 690235, Overall Bitrate: 952377, Video Tracks: 1, Video Codec: av1, Duration: 0:00:05.798, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: ivf@GPAC0.7.2-DEV-rev654-gb6f7409ce-github_master, Mime Type: video/mp4",
						mediaInfo.toString()
					);
					break;
				case 17:
					assertEquals(
						"Container: MKV, Size: 6291087, Overall Bitrate: 22468168, Video Tracks: 1, Video Codec: vc1, Duration: 0:00:02.240, Video Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Scan Type: Interlaced, Scan Order: Top Field First, Frame Rate: 25.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				case 18:
					assertEquals(
						"Container: MP4, Size: 1099408, Overall Bitrate: 188638, Video Tracks: 1, Video Codec: h264, Duration: 0:00:46.625, Video Resolution: 800 x 600, Display Aspect Ratio: 4:3, Scan Type: Progressive, Frame Rate: 8.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Reference Frame Count: 4, AVC Level: 3.1, AVC Profile: main, Audio Tracks: 1 [Audio Codec: HE-AAC, Bitrate: 159992, Channels: 6, Sample Frequency: 44100 Hz], Mime Type: video/mp4",
						mediaInfo.toString()
					);
					break;
				case 19:
					assertEquals(
						"Container: MKV, Size: 6356992, Overall Bitrate: 7121, Video Tracks: 1, Video Codec: h264, Duration: 1:59:01.690, Video Resolution: 1280 x 544, Display Aspect Ratio: 2.35:1, Scan Type: Progressive, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Reference Frame Count: 1, AVC Level: 5.1, AVC Profile: main, Audio Tracks: 1 [Audio Codec: Enhanced AC-3, Bitrate: 1536000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				case 20:
					assertEquals(
						"Container: FLAC, Size: 3208022, Overall Bitrate: 1231959, Bitrate: 1231959, Duration: 0:00:20.832, Audio Tracks: 1 [Audio Codec: FLAC, Bitrate: 1231916, Bits per Sample: 24, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: audio/x-flac",
						mediaInfo.toString()
					);
					break;
				case 21:
					assertEquals(
						"Container: AVI, Size: 1282694, Overall Bitrate: 793255, Video Tracks: 1, Video Codec: divx, Duration: 0:00:12.936, Video Resolution: 720 x 400, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 25.000, Audio Tracks: 1 [Audio Track Title From Metadata: video-mpeg4-aac, Audio Codec: MP3, Bitrate: 128000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi",
						mediaInfo.toString()
					);
					break;
				case 22:
					assertEquals(
						"Container: MKV, Size: 8925360, Overall Bitrate: 11868830, Video Tracks: 1, Video Codec: h265, Duration: 0:00:06.016, Video Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Frame Rate: 59.940, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Bit Depth: 10, Video HDR Format: Dolby Vision, Audio Tracks: 1 [Audio Codec: Enhanced AC-3, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				case 23:
					assertEquals(
						"Container: MKV, Size: 7799945, Overall Bitrate: 10372267, Video Tracks: 1, Video Codec: h265, Duration: 0:00:06.016, Video Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Frame Rate: 59.940, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.2020 non-constant, Video Bit Depth: 10, Video HDR Format: Dolby Vision / SMPTE ST 2086, Audio Tracks: 1 [Audio Codec: Enhanced AC-3, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				case 24:
					assertEquals(
						"Container: MKV, Size: 8180475, Overall Bitrate: 21916879, Video Tracks: 1, Video Codec: h265, Duration: 0:00:02.986, Video Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 59.940, Original Frame Rate: 59.940, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.2020 non-constant, Video Bit Depth: 10, Video HDR Format: HDR10, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 117969, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				case 25:
					assertEquals(
						"Container: MKV, Size: 8652028, Overall Bitrate: 61416348, Video Tracks: 1, Video Codec: h265, Duration: 0:00:01.127, Video Resolution: 3840 x 2160, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.2020 non-constant, Video Bit Depth: 10, Video HDR Format: HDR10+ Profile A / HDR10, File Title from Metadata: A Beautiful Planet (2016), Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Track Title From Metadata: DTS:X, Audio Codec: DTS HD, Bitrate: 8543871, Bits per Sample: 24, Channels: 8, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska",
						mediaInfo.toString()
					);
					break;
				default:
					break;
			}
		}
	}

//	@Test
//	public void testMediaInfoOutputParse() throws Exception {
//		for (int id : dlnaResourceIds) {
//			DLNAResource dlna = PMS.getGlobalRepo().get(id);
//			System.out.format("mediainfo: %s\n", dlna.getMedia().toString());
//			assertThat(dlna.getMedia().getExifOrientation().getValue(),1);
//			System.out.format("MediaInfo parsing OK \n");
//		}

		// System.out.format( "name: %s\n", dlna.getName() );
		// System.out.format( "display name: %s\n", dlna.getDisplayName() );
		// System.out.format( "aspect ratio: %s\n", dlna.getMedia().getAspectRatioMencoderMpegopts(false) );
		// System.out.format( "aspect ratio: %s\n", dlna.getMedia().getAspectRatioMencoderMpegopts(true) );
		// System.out.format( "aspect ratio: %d/%d\n", dlna.getMedia().getAspectRatioVideoTrack().getNumerator(), dlna.getMedia().getAspectRatioVideoTrack().getDenominator() );
		// System.out.format( "frame rate mode: %s\n", dlna.getMedia().getFrameRateMode() );
		// for(RendererConfiguration mediaRenderer : RendererConfiguration.getEnabledRenderersConfigurations()) {
		// 	if( mediaRenderer.getConfName() != null && mediaRenderer.getConfName().equals("VLC for desktop") ) {
		// 		dlna.resolvePlayer(mediaRenderer);
		// 	}
		// }
		// Player player = PlayerFactory.getPlayer(dlna);

		// for (Player p:PlayerFactory.getAllPlayers()){
		// 	System.out.println(p.id().getName());
		// 	System.out.println(p.isActive());
		// 	System.out.println(p.isAvailable());
		// 	System.out.println(p.isEnabled());
		// 	System.out.println(p.getClass().getName());
		// }
//	}

}
