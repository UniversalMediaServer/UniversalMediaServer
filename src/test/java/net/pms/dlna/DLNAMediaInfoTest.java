package net.pms.dlna;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static net.pms.configuration.RendererConfiguration.loadRendererConfigurations;
import static org.assertj.core.api.Assertions.*;

import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.ProgramExecutableType;
import net.pms.configuration.RendererConfiguration;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.encoders.Player;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;


public class DLNAMediaInfoTest
{
	private final Class<?> CLASS = DLNAMediaInfoTest.class;

	@BeforeClass
	public static void SetUPClass()
	{
		PMS.configureJNA();
	}


	@Test
	public void testSomething() throws Exception
	{
		// Set default to non existent renderer
		// This comes from net.pms.configuration.RendererConfigurationTest::testBogusDefault()
		PmsConfiguration pmsConf = new PmsConfiguration(false);
		pmsConf.setRendererDefault("Bogus Renderer");
		//pmsConf.setRendererForceDefault(true);
		PMS.setConfiguration(pmsConf);
		PMS.forceHeadless();
		PMS.get();
		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);
		
		Player player = new FFmpegWebVideo();
		player.setCurrentExecutableType(ProgramExecutableType.BUNDLED);

		// Get a resource handle
		// This comes from RequestV2::answer()
		DLNAResource dlna = new RealFile(
				FileUtils.toFile(CLASS.getResource("pexels-video-4809.mp4")));
		dlna.setMedia(new DLNAMediaInfo());
		dlna.setMediaAudio(new DLNAMediaAudio());
		PMS.getGlobalRepo().add(dlna);
		
		for(RendererConfiguration mediaRenderer : RendererConfiguration.getEnabledRenderersConfigurations()) {
			if( mediaRenderer.getConfName() != null && mediaRenderer.getConfName().equals("VLC for desktop") ) {
				OutputParams params = new OutputParams(PMS.getConfiguration());
				params.mediaRenderer = new DeviceConfiguration(mediaRenderer);
				params.aid = dlna.getMediaAudio();
				ProcessWrapper externalProcess = player.launchTranscode(dlna,
						dlna.getMedia(), params);
				//dlna.getMedia().parseFFmpegInfo(externalProcess.getResults(), "-");
				System.out.println(dlna.getMedia().getDurationString());
				System.out.println(dlna.getMedia().getContainer());
			}
		}
		
		
	
	}

}
