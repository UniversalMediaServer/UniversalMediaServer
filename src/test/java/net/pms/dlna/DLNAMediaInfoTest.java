package net.pms.dlna;

import ch.qos.logback.classic.Level;
import static org.assertj.core.api.Assertions.assertThat;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.logging.LoggingConfig;
import net.pms.service.Services;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLNAMediaInfoTest {
	private static final Class<?> CLASS = DLNAMediaInfoTest.class;

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
		"video-av1.mp4"
	};
	private static final int[] dlnaResourceIds = new int[test_files.length];

	@Test
	public void testContainerProperties() throws Exception {
		PMS.configureJNA();
		PMS.forceHeadless();
		try {
			PMS.setConfiguration(new PmsConfiguration(false));
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
		assert PMS.getConfiguration() != null;
		PMS.getConfiguration().setSharedFolders(null);
		PMS.getConfiguration().setScanSharedFoldersOnStartup(false);
		PMS.getConfiguration().setUseCache(false);

		LoggingConfig.setRootLevel(Level.TRACE);
		// force unbuffered if in trace mode
		LoggingConfig.setBuffered(false);

		Logger LOGGER = LoggerFactory.getLogger(CLASS);

		Services.create();
		try {
			PMS.getConfiguration().initCred();
		} catch (Exception ex) {
			LOGGER.warn("Failed to write credentials configuration", ex);
		}

		if (PMS.getConfiguration().isRunSingleInstance()) {
			PMS.killOld();
		}

		// Create a new PMS instance
		PMS.getNewInstance();

		// Create handles to the test content
		// This comes from RequestV2::answer()
		DLNAResource parent = new VirtualFolder("test", "test");
		parent.setDefaultRenderer(RendererConfiguration.getDefaultConf());
		PMS.getGlobalRepo().add(parent);

		for (int i = 0; i < test_files.length; ++i) {
			DLNAResource dlna = new RealFile(FileUtils.toFile(CLASS.getResource(test_files[i])));
			dlna.setParent(parent);
			dlna.resolveFormat();
			dlna.syncResolve();

			DLNAMediaInfo mediaInfo = dlna.getMedia();

			switch(i) {
				case 0:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MP4, Size: 1325017, Overall Bitrate: 676979, Video Tracks: 1, Video Codec: h264, Duration: 0:00:15.658, Video Resolution: 640 x 360, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.601, Reference Frame Count: 4, AVC Level: 3, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 125547, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/mp4"
					);
					break;
				case 1:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MKV, Size: 2097841, Overall Bitrate: 1575843, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:10.650, Video Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate Mode: VFR (VFR), Frame Rate Mode Raw: VFR, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 0, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
					);
					break;
				case 2:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MKV, Size: 5291494, Overall Bitrate: 2619552, Video Tracks: 1, Video Codec: h265, Duration: 0:00:16.160, Video Resolution: 1920 x 960, Display Aspect Ratio: 2.00:1, Frame Rate: 25.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.709, Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Track Title From Metadata: Stereo, Audio Codec: AAC-LC, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
					);
					break;
				case 3:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: OGG, Size: 1734919, Overall Bitrate: 454643, Video Tracks: 1, Video Codec: theora, Duration: 0:00:30.528, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 30.000, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/ogg"
					);
					break;
				case 4:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MP4, Size: 3538130, Overall Bitrate: 542149, Video Tracks: 1, Video Codec: h264, Duration: 0:00:52.209, Video Resolution: 720 x 480, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.56 (1563/1000), Scan Type: Progressive, Frame Rate: 24.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Reference Frame Count: 4, AVC Level: 3, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 128290, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/mp4"
					);
					break;
				case 5:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: 3G2, Size: 1792091, Overall Bitrate: 275410, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.056, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Frame Rate: 18.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 59721, Channels: 2, Sample Frequency: 22050 Hz], Mime Type: video/3gpp2"
					);
					break;
				case 6:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: AVI, Size: 3893340, Overall Bitrate: 598711, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.023, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Frame Rate: 24.000, Video Track Title from Metadata: Sintel Trailer, Audio Tracks: 1 [Audio Codec: ADPCM, Bitrate: 128000, Bits per Sample: 4, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/avi"
					);
					break;
				case 7:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MOV, Size: 2658492, Overall Bitrate: 408339, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.084, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Frame Rate: 24.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: Sintel Trailer, Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Codec: AAC-LC, Bitrate: 125805, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/quicktime"
					);
					break;
				case 8:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: WMV, Size: 3002945, Overall Bitrate: 460107, Video Tracks: 1, Video Codec: wmv, Duration: 0:00:52.213, Video Resolution: 360 x 240, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.56 (1563/1000), Frame Rate: 24.000, Audio Tracks: 1 [Audio Codec: WMA, Bitrate: 128000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/x-ms-wmv"
					);
					break;
				case 9:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: WEBM, Size: 901185, Overall Bitrate: 236044, Video Tracks: 1, Video Codec: vp8, Duration: 0:00:30.543, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 30.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/webm"
					);
					break;
				case 10:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: FLV, Size: 2097492, Overall Bitrate: 1529899, Video Tracks: 1, Video Codec: sor, Duration: 0:00:10.968, Video Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 375000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-flv"
					);
					break;
				case 11:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: AVI, Size: 742478, Overall Bitrate: 194029, Video Tracks: 1, Video Codec: h264, Duration: 0:00:30.613, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 30.000, Frame Rate Mode: VFR (VFR), Frame Rate Mode Raw: VFR, Reference Frame Count: 4, AVC Level: 2.1, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 139632, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi"
					);
					break;
				case 12:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: WAV, Size: 1073218, Overall Bitrate: 256069, Bitrate: 256069, Duration: 0:00:33.529, Audio Tracks: 1 [Audio Codec: LPCM, Bitrate: 256000, Channels: 2, Sample Frequency: 8000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/wav"
					);
					break;
				case 13:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: OGA, Size: 1089524, Overall Bitrate: 117233, Bitrate: 117233, Duration: 0:01:14.349, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 120000, Channels: 2, Sample Frequency: 32000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/ogg"
					);
					break;
				case 14:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MP3, Size: 764176, Overall Bitrate: 224000, Bitrate: 224000, Duration: 0:00:27.252, Audio Tracks: 1 [Audio Codec: MP3, Bitrate: 224000, Channels: 2, Sample Frequency: 32000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/mpeg"
					);
					break;
				case 15:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MP4, Size: 245747, Overall Bitrate: 130716, Video Tracks: 1, Video Codec: av1, Duration: 0:00:15.040, Video Resolution: 960 x 540, Display Aspect Ratio: 16:9, Frame Rate: 25.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: vid, Audio Tracks: 1 [Id: 0, Language Code: snd, Audio Track Title From Metadata: snd, Audio Codec: AAC-LC, Bitrate: 8887, Channel: 1, Sample Frequency: 32000 Hz], Mime Type: video/mp4"
					);
					break;
				case 16:
					assertThat(mediaInfo.toString()).isEqualTo(
						"Container: MP4, Size: 690235, Overall Bitrate: 952377, Video Tracks: 1, Video Codec: av1, Duration: 0:00:05.798, Video Resolution: 480 x 270, Display Aspect Ratio: 16:9, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: ivf@GPAC0.7.2-DEV-rev654-gb6f7409ce-github_master, Mime Type: video/mp4"
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
//			assertThat(dlna.getMedia().getExifOrientation().getValue()).isEqualTo(1);
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
