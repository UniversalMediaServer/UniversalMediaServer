package net.pms.dlna;

import org.junit.BeforeClass;
import org.junit.Test;

import static net.pms.configuration.RendererConfiguration.loadRendererConfigurations;
import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;

import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.Player;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;


public class DLNAMediaInfoTest
{

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
		
		// Get a resource handle
		// This comes from RequestV2::answer()
		RendererConfiguration mediaRenderer = RendererConfiguration.getDefaultConf();
		
		File f = new File("/mnt/konstant/todo/A.Scanner.Darkly.2006.1080p.BluRay.x264.YIFY.mp4");
		DLNAResource dlna = new RealFile(f);
		dlna.setMedia(new DLNAMediaInfo());
		dlna.setMediaAudio(new DLNAMediaAudio());
		PMS.getGlobalRepo().add(dlna);
		OutputParams params = new OutputParams(PMS.getConfiguration());
		params.mediaRenderer = new DeviceConfiguration();
		params.aid = dlna.getMediaAudio();

		Player player = new FFMpegVideo();
		ProcessWrapper externalProcess = player.launchTranscode(dlna,
				dlna.getMedia(), params);
	
	}

}
