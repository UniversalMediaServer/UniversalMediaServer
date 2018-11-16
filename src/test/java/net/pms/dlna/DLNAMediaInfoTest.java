package net.pms.dlna;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.Assert.assertThat;

import ch.qos.logback.classic.Level;

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
	private final Class<?> CLASS = DLNAMediaInfoTest.class;

	@BeforeClass
	public static void SetUPClass()
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
		Services.create();
		 try {
			 PMS.getConfiguration().initCred();
		} catch (Exception ex) {
			ex.printStackTrace();
		}


		if (PMS.getConfiguration().isRunSingleInstance()) {
//			PMS.killOld();
			System.err.println("XXXXXXXXXXXXXXXXXXXXXX");
		}

		// Create the PMS instance returned by get()
		PMS.get();
	}


	@Test
	public void testFFmpegOutputParse() throws Exception
	{
		// Get a resource handle
		// This comes from RequestV2::answer()
		DLNAResource parent = new VirtualFolder("test","test");
		DLNAResource dlna = new RealFile(
				FileUtils.toFile(CLASS.getResource("pexels-video-4809.mp4")));
		
		dlna.setMedia(new DLNAMediaInfo());
		dlna.setParent(parent);
		dlna.syncResolve();
		dlna.resolveFormat();

		assertThat( dlna.getMedia().getSize() ).isEqualTo(9441436);
		assertThat( dlna.getMedia().getContainer() ).isEqualToIgnoringCase("mp4");
		assertThat( dlna.getMedia().getMimeType() ).isEqualToIgnoringCase("video/mp4");
		assertThat( dlna.getFormat().getType() ).isEqualTo(4);
		
		assertThat( dlna.getMedia().getVideoTrackCount() ).isEqualTo(1);
		assertThat( dlna.getMedia().getCodecV() ).isEqualToIgnoringCase("h264");
		assertThat( dlna.getMedia().getBitrate() ).isEqualTo(5016576);
		assertThat( dlna.getMedia().getFrameRate() ).isEqualTo("29.97");
		assertThat( dlna.getMedia().getDuration() ).isEqualTo(15.42);
		assertThat( dlna.getMedia().getResolution() ).isEqualToIgnoringCase("1920x1080");
		assertThat( dlna.getMedia().getFrameNumbers() ).isEqualTo(462);
		assertThat( dlna.getMedia().getExifOrientation().getValue() ).isEqualTo(1);

		//System.out.format( "name: %s\n", dlna.getName() );
		//System.out.format( "display name: %s\n", dlna.getDisplayName() );
		//System.out.format( "aspect ratio: %s\n", dlna.getMedia().getAspectRatioMencoderMpegopts(false) );
		//System.out.format( "aspect ratio: %s\n", dlna.getMedia().getAspectRatioMencoderMpegopts(true) );
		//System.out.format( "aspect ratio: %d/%d\n", dlna.getMedia().getAspectRatioVideoTrack().getNumerator(), dlna.getMedia().getAspectRatioVideoTrack().getDenominator() );
		//System.out.format( "frame rate mode: %s\n", dlna.getMedia().getFrameRateMode() );
		for(RendererConfiguration mediaRenderer : RendererConfiguration.getEnabledRenderersConfigurations()) {
			if( mediaRenderer.getConfName() != null && mediaRenderer.getConfName().equals("VLC for desktop") ) {
				dlna.resolvePlayer(mediaRenderer);
			}
		}
		Player player = PlayerFactory.getPlayer(dlna);

		/*
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
