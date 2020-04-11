package net.pms.dlna;

import ch.qos.logback.classic.Level;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.logging.LoggingConfig;
import net.pms.service.Services;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLNAMediaInfoTest {
	private static final Class<?> CLASS = DLNAMediaInfoTest.class;

	private static final String[] test_files = {
		"video-h264-aac.mp4",
		"video-mpeg4-aac.mkv",
		"video-h265.mp4",
		"video-ogg.ogv",
		"video-h264-aac.m4v",
		"video-mp4-aac.3g2",
		"video-mp4-adpcm.avi",
		"video-mp4-aac.mov",
		"video-wmv-wma.wmv",
		"video-vp8.webm",
		"video-sor-aac.flv",
	};
	private static final int[] test_content = new int[test_files.length];

	@BeforeClass
	public static void setUpBeforeClass() {
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
			DLNAResource dlna;
			dlna = new RealFile(FileUtils.toFile(CLASS.getResource(test_files[i])));
			dlna.setMedia(new DLNAMediaInfo());
			dlna.setParent(parent);
			dlna.getParent().setDefaultRenderer(RendererConfiguration.getDefaultConf());
			dlna.resolveFormat();
			dlna.syncResolve();
			PMS.getGlobalRepo().add(dlna);
			test_content[i] = dlna.getIntId();
		}
	}

	@Test
	public void testContainerProperties() throws Exception {
		DLNAResource mp4Video = PMS.getGlobalRepo().get(test_content[0]);
		DLNAMediaInfo mp4VideoMediaInfo = mp4Video.getMedia();
		assertThat(mp4VideoMediaInfo.toString()).isEqualTo(
			"Container: MP4, Size: 1325017, Overall Bitrate: 676979, Video Tracks: 1, Video Codec: h264, Duration: 0:00:15.658, Video Resolution: 640 x 360, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 23.976, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.601, Reference Frame Count: 4, AVC Level: 3, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 125547, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/mp4"
		);

		DLNAResource mkvVideo = PMS.getGlobalRepo().get(test_content[1]);
		DLNAMediaInfo mkvVideoMediaInfo = mkvVideo.getMedia();
		assertThat(mkvVideoMediaInfo.toString()).isEqualTo(
			"Container: MKV, Size: 2097841, Overall Bitrate: 1575843, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:10.650, Video Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate Mode: VFR (VFR), Frame Rate Mode Raw: VFR, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 0, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
		);

		DLNAResource mkvHevc10bitVideo = PMS.getGlobalRepo().get(test_content[2]);
		DLNAMediaInfo mkvHevc10bitVideoMediaInfo = mkvHevc10bitVideo.getMedia();
		assertThat(mkvHevc10bitVideoMediaInfo.toString()).isEqualTo(
			"Container: MP4, Size: 706007, Overall Bitrate: 404647, Video Tracks: 1, Video Codec: h265, Duration: 0:00:13.958, Video Resolution: 1920 x 800, Display Aspect Ratio: 2.39:1, Frame Rate: 24.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: hevc:fps=24@GPAC0.5.1-DEV-rev4807, Mime Type: video/mp4"
		);

		DLNAResource oggVideo = PMS.getGlobalRepo().get(test_content[3]);
		DLNAMediaInfo oggVideoMediaInfo = oggVideo.getMedia();
		assertThat(oggVideoMediaInfo.toString()).isEqualTo(
			"Container: OGG, Size: 4372706, Overall Bitrate: 672672, Video Tracks: 1, Video Codec: theora, Duration: 0:00:52.004, Video Resolution: 640 x 360, Display Aspect Ratio: 16:9, Frame Rate: 24.000, File Title from Metadata: Sintel Trailer, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 128000, Channels: 2, Sample Frequency: 44100 Hz, Artist: Durian Open Movie Team], Mime Type: video/ogg"
		);

		DLNAResource m4vVideo = PMS.getGlobalRepo().get(test_content[4]);
		DLNAMediaInfo m4vVideoMediaInfo = m4vVideo.getMedia();
		assertThat(m4vVideoMediaInfo.toString()).isEqualTo(
			"Container: MP4, Size: 3538130, Overall Bitrate: 542149, Video Tracks: 1, Video Codec: h264, Duration: 0:00:52.209, Video Resolution: 720 x 480, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.56 (1563/1000), Scan Type: Progressive, Frame Rate: 24.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Reference Frame Count: 4, AVC Level: 3, AVC Profile: high, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 128290, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/mp4"
		);

		DLNAResource a3g2Video = PMS.getGlobalRepo().get(test_content[5]);
		DLNAMediaInfo a3g2VideoMediaInfo = a3g2Video.getMedia();
		assertThat(a3g2VideoMediaInfo.toString()).isEqualTo(
			"Container: 3G2, Size: 1792091, Overall Bitrate: 275410, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.056, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Frame Rate: 18.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 59721, Channels: 2, Sample Frequency: 22050 Hz], Mime Type: video/3gpp2"
		);

		DLNAResource aviVideo = PMS.getGlobalRepo().get(test_content[6]);
		DLNAMediaInfo aviVideoMediaInfo = aviVideo.getMedia();
		assertThat(aviVideoMediaInfo.toString()).isEqualTo(
			"Container: AVI, Size: 3893340, Overall Bitrate: 598711, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.023, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Frame Rate: 24.000, Video Track Title from Metadata: Sintel Trailer, Audio Tracks: 1 [Audio Codec: ADPCM, Bitrate: 128000, Bits per Sample: 4, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/avi"
		);

		DLNAResource movVideo = PMS.getGlobalRepo().get(test_content[7]);
		DLNAMediaInfo movVideoMediaInfo = movVideo.getMedia();
		assertThat(movVideoMediaInfo.toString()).isEqualTo(
			"Container: MOV, Size: 2658492, Overall Bitrate: 408339, Video Tracks: 1, Video Codec: mp4, Duration: 0:00:52.084, Video Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Frame Rate: 24.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Video Track Title from Metadata: Sintel Trailer, Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Codec: AAC-LC, Bitrate: 125805, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/quicktime"
		);

		DLNAResource wmvVideo = PMS.getGlobalRepo().get(test_content[8]);
		DLNAMediaInfo wmvVideoMediaInfo = wmvVideo.getMedia();
		assertThat(wmvVideoMediaInfo.toString()).isEqualTo(
			"Container: WMV, Size: 3002945, Overall Bitrate: 460107, Video Tracks: 1, Video Codec: wmv, Duration: 0:00:52.213, Video Resolution: 360 x 240, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.56 (1563/1000), Frame Rate: 24.000, Audio Tracks: 1 [Audio Codec: WMA, Bitrate: 128000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/x-ms-wmv"
		);

		DLNAResource webmVideo = PMS.getGlobalRepo().get(test_content[9]);
		DLNAMediaInfo webmVideoMediaInfo = webmVideo.getMedia();
		assertThat(webmVideoMediaInfo.toString()).isEqualTo(
			"Container: WEBM, Size: 3028588, Overall Bitrate: 2012351, Video Tracks: 1, Video Codec: vp8, Duration: 0:00:12.040, Video Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Frame Rate: 25.000, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Mime Type: video/webm"
		);

		DLNAResource flvVideo = PMS.getGlobalRepo().get(test_content[10]);
		DLNAMediaInfo flvVideoMediaInfo = flvVideo.getMedia();
		assertThat(flvVideoMediaInfo.toString()).isEqualTo(
			"Container: FLV, Size: 2097492, Overall Bitrate: 1529899, Video Tracks: 1, Video Codec: sor, Duration: 0:00:10.968, Video Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 375000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-flv"
		);
	}

	@Test
	public void testMediaInfoOutputParse() throws Exception {
		for (int id : test_content) {
			DLNAResource dlna = PMS.getGlobalRepo().get(id);
			System.out.format("mediainfo: %s\n", dlna.getMedia().toString());
			assertThat(dlna.getMedia().getExifOrientation().getValue()).isEqualTo(1);
			System.out.format("MediaInfo parsing OK \n");
		}

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
	}
}
