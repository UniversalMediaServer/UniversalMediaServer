package net.pms.dlna;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

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
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}

		if (PMS.getConfiguration().isRunSingleInstance()) {
//			PMS.killOld();
			System.err.println("XXXXXXXXXXXXXXXXXXXXXX");
		}

		// Create the PMS instance returned by get()
		PMS.get();
	}


	@Test
	public void testSomething() throws Exception
	{
		// Get a resource handle
		// This comes from RequestV2::answer()
		DLNAResource parent = new VirtualFolder("test","test");
		DLNAResource dlna = new RealFile(
				FileUtils.toFile(CLASS.getResource("pexels-video-4809.mp4")));
		PMS.getGlobalRepo().add(dlna);
		dlna.setMedia(new DLNAMediaInfo());
		dlna.setParent(parent);
		dlna.syncResolve();
		dlna.resolveFormat();
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
		for (Player p:PlayerFactory.getPlayers(true,true)){
			System.out.println("aa "+p.id().getName());
		}
	
	}

}
