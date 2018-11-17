package net.pms.dlna;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.logging.LoggingConfig;
import net.pms.service.Services;


public class DLNAMediaInfoTest
{
	private static final Class<?> CLASS = DLNAMediaInfoTest.class;
	
	private static final String[] test_files =
	{
			"pexels-video-4809.mp4", "pexels-video-4809.mkv"
	};
	private static final int[] test_content =
			new int[test_files.length];

	@BeforeClass
	public static void setUpBeforeClass()
	{
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
		
		Logger logger = LoggerFactory.getLogger(CLASS);

		Services.create();
		 try {
			 PMS.getConfiguration().initCred();
		} catch (Exception ex) {
			logger.warn("Failed to write credentials configuration", ex);
		}


		if (PMS.getConfiguration().isRunSingleInstance()) {
			PMS.killOld();
		}

		// Create a new PMS instance
		PMS.getNew();
		
		// Create handles to the test content
		// This comes from RequestV2::answer()

		DLNAResource parent = new VirtualFolder("test","test");
		parent.setDefaultRenderer(RendererConfiguration.getDefaultConf());
		PMS.getGlobalRepo().add(parent);
		
		for(int i=0; i<test_files.length; ++i) {
			DLNAResource dlna;
			dlna = new RealFile(FileUtils.toFile(CLASS.getResource(test_files[i])));
			dlna.setMedia(new DLNAMediaInfo());
			dlna.setParent(parent);
			dlna.resolveFormat();
			dlna.syncResolve();
			logger.trace("mediainfo: %s\n", dlna.getMedia().toString() );
			PMS.getGlobalRepo().add(dlna);
			test_content[i] = dlna.getIntId();
		}
	}


	@Test
	public void testContainerProperties() throws Exception
	{
		DLNAResource dlna = PMS.getGlobalRepo().get(test_content[0]);

		assertThat( dlna.getMedia().getSize() ).isEqualTo(9441436L);
		assertThat( dlna.getMedia().getContainer() ).isEqualTo("mp4");
		assertThat( dlna.getMedia().getMimeType() ).isEqualTo("video/mp4");
		assertThat( dlna.getFormat().getType() ).isEqualTo(4);

		dlna = PMS.getGlobalRepo().get(test_content[1]);
		System.out.format( "mediainfo: %s\n", dlna.getMedia().toString() );

		assertThat( dlna.getMedia().getSize() ).isEqualTo(9439150L);
		assertThat( dlna.getMedia().getContainer() ).isEqualTo("matroska");
		assertThat( dlna.getMedia().getMimeType() ).isEqualTo("video/x-matroska");
		assertThat( dlna.getFormat().getType() ).isEqualTo(4);
	}


	@Test
	public void testFFmpegOutputParse() throws Exception
	{
		for(int id : test_content) {
			DLNAResource dlna = PMS.getGlobalRepo().get(id);

			assertThat( dlna.getMedia().getVideoTrackCount() ).isEqualTo(1);
			assertThat( dlna.getMedia().getCodecV() ).isEqualTo("h264");
			assertThat( dlna.getMedia().getBitrate() ).isCloseTo(5016576,withPercentage(5));
			assertThat( Float.parseFloat(dlna.getMedia().getFrameRate()) ).isEqualTo(29.97f);
			assertThat( dlna.getMedia().getDuration() ).isCloseTo(15.42,withPercentage(1));
			assertThat( dlna.getMedia().getResolution() ).isEqualToIgnoringWhitespace("1920x1080");
			assertThat( dlna.getMedia().getFrameNumbers() ).isCloseTo(462,withPercentage(5));
			assertThat( dlna.getMedia().getExifOrientation().getValue() ).isEqualTo(1);
		}

		//System.out.format( "name: %s\n", dlna.getName() );
		//System.out.format( "display name: %s\n", dlna.getDisplayName() );
		//System.out.format( "aspect ratio: %s\n", dlna.getMedia().getAspectRatioMencoderMpegopts(false) );
		//System.out.format( "aspect ratio: %s\n", dlna.getMedia().getAspectRatioMencoderMpegopts(true) );
		//System.out.format( "aspect ratio: %d/%d\n", dlna.getMedia().getAspectRatioVideoTrack().getNumerator(), dlna.getMedia().getAspectRatioVideoTrack().getDenominator() );
		//System.out.format( "frame rate mode: %s\n", dlna.getMedia().getFrameRateMode() );
			/*
		for(RendererConfiguration mediaRenderer : RendererConfiguration.getEnabledRenderersConfigurations()) {
			if( mediaRenderer.getConfName() != null && mediaRenderer.getConfName().equals("VLC for desktop") ) {
				dlna.resolvePlayer(mediaRenderer);
			}
		}
		Player player = PlayerFactory.getPlayer(dlna);

		for (Player p:PlayerFactory.getAllPlayers()){
			System.out.println(p.id().getName());
			System.out.println(p.isActive());
			System.out.println(p.isAvailable());
			System.out.println(p.isEnabled());
			System.out.println(p.getClass().getName());
		}
		*/
	
	}

}
