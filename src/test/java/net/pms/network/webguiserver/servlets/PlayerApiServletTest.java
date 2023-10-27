package net.pms.network.webguiserver.servlets;

import java.lang.reflect.Constructor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.renderers.Renderer;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerApiServletTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerApiServletTest.class.getName());

	@Mock
	DLNAResource videoFolder;

	@Mock
	Renderer renderer;

	@Mock
	RootFolder rootFolder;

	@Mock
	PMS pms;

	@Mock
	UmsConfiguration config;

	@BeforeEach
	public void SetUp() {
		videoFolder = Mockito.mock(DLNAResource.class);
		renderer = Mockito.mock(Renderer.class);
		pms = Mockito.mock(PMS.class);
		config = Mockito.mock(UmsConfiguration.class);
		Mockito.when(pms.getConfiguration(renderer)).thenReturn(config);
		try {
			Field privateField = PMS.class.getDeclaredField("umsConfiguration");
			privateField.setAccessible(true);
			privateField.set(pms, config);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetMediaLibraryFolderChildsForVideoFolderChildrenEmpty() throws Exception {
		Constructor<PlayerApiServlet> constructor = PlayerApiServlet.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		PlayerApiServlet playerApiServletInstance = constructor.newInstance();
		Method method = PlayerApiServlet.class.getDeclaredMethod("getMediaLibraryFolderChilds", DLNAResource.class,
				Renderer.class, String.class);
		method.setAccessible(true);
		try {
			Mockito.when(videoFolder.getId()).thenReturn("100");
			JsonArray jLibraryVideos = (JsonArray) method.invoke(playerApiServletInstance, videoFolder, renderer,
					"100");
			assertEquals(null, jLibraryVideos);
		} catch (Exception ex) {
			LOGGER.error("Exception occured : ", ex.getMessage());
		}
	}

	@Test
	public void testGetMediaLibraryFolderChildsForVideoFolderChildrenNotEmpty() throws Exception {
		Constructor<PlayerApiServlet> playerApiServletConstructor = PlayerApiServlet.class.getDeclaredConstructor();
		playerApiServletConstructor.setAccessible(true);
		PlayerApiServlet playerApiServletInstance = playerApiServletConstructor.newInstance();
		DLNAResource abstractClass = Mockito.mock(DLNAResource.class, Mockito.CALLS_REAL_METHODS);
		Field stringValueField = DLNAResource.class.getDeclaredField("id");
		stringValueField.setAccessible(true);
		stringValueField.set(abstractClass, "100");
		List<DLNAResource> libraryVideos = new ArrayList<>();
		libraryVideos.add(abstractClass);
		try {
			Method method = PlayerApiServlet.class.getDeclaredMethod("getMediaLibraryFolderChilds", DLNAResource.class,
					Renderer.class, String.class);
			method.setAccessible(true);
			Mockito.when(abstractClass.getDLNAResources(abstractClass.getId(), true, 0, 0, renderer, "100"))
					.thenReturn(libraryVideos);
			JsonArray jLibraryVideos = (JsonArray) method.invoke(playerApiServletInstance, abstractClass, renderer,
					"100");
			assertEquals(null, jLibraryVideos);
		} catch (Exception ex) {
			LOGGER.error("Exception occured : " + ex);
		}
	}

}
