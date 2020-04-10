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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLNAMediaInfoTest {
	private static final Class<?> CLASS = DLNAMediaInfoTest.class;

	private static final String[] test_files = {
		"pexels-video-4809.mp4",
		"pexels-video-4809.mkv",
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
			LOGGER.trace("mediainfo: %s\n", dlna.getMedia().toString());
			PMS.getGlobalRepo().add(dlna);
			test_content[i] = dlna.getIntId();
		}
	}

	@Test
	public void testContainerProperties() throws Exception {
		DLNAResource mp4Video = PMS.getGlobalRepo().get(test_content[0]);
		DLNAMediaInfo mp4VideoMediaInfo = mp4Video.getMedia();
		assertThat(mp4VideoMediaInfo.toString()).isEqualTo(
			"Container: MP4, Size: 9441436, Video Bitrate: 4899870, Video Tracks: 1, Video Codec: h264, Duration: 0:00:15.415, Video Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 29.970, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.709, Reference Frame Count: 4, AVC Level: 4, AVC Profile: high, Mime Type: video/mp4"
		);

		// assertThat(mp4Video.getMedia().getAudioTrackCount()).isEqualTo(1);
		// assertThat(mp4Video.getMedia().getImageCount()).isEqualTo(0);
		// assertThat(mp4Video.getMedia().getSubTrackCount()).isEqualTo(0);

		// assertThat(mp4Video.getMedia().getSize()).isEqualTo(9441436L);
		// assertThat(mp4Video.getFormat().getType()).isEqualTo(4);

		DLNAResource mkvVideo = PMS.getGlobalRepo().get(test_content[1]);
		DLNAMediaInfo mkvVideoMediaInfo = mkvVideo.getMedia();
		assertThat(mkvVideoMediaInfo.toString()).isEqualTo(
			"Container: MKV, Size: 9439150, Video Bitrate: 4898683, Video Tracks: 1, Video Codec: h264, Duration: 0:00:15.415, Video Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Scan Type: Progressive, Frame Rate: 29.970, Frame Rate Mode: CFR (CFR), Frame Rate Mode Raw: CFR, Matrix Coefficients: BT.709, Reference Frame Count: 4, AVC Level: 4, AVC Profile: high, Mime Type: video/x-matroska"
		);

		// assertThat(mkvVideo.getMedia().getAudioTrackCount()).isEqualTo(1);
		// assertThat(mkvVideo.getMedia().getImageCount()).isEqualTo(0);
		// assertThat(mkvVideo.getMedia().getSubTrackCount()).isEqualTo(0);

		// assertThat(mkvVideo.getMedia().getSize()).isEqualTo(9439150L);
		// assertThat(mkvVideo.getFormat().getType()).isEqualTo(4);
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
