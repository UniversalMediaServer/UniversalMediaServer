package net.pms.external.audioaddict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.jupnp.util.io.IO;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.pms.external.audioaddict.mapper.Root;

public class AudioAddictTest {

	public AudioAddictTest() {
	}

	@Test
	public void testAuthenticate() throws IOException {
		AudioAddictServiceConfig aac = new AudioAddictServiceConfig();
		aac.preferEuropeanServer = true;
		RadioNetwork rn = new RadioNetwork(Platform.ROCK_RADIO, aac);

		InputStream is = getClass().getResourceAsStream("/net/pms/external/audioaddict/authenticate.txt");
		String json = IO.readLines(is);
		rn.extractAuthInfo(json);
		assertEquals("SECRET", rn.getApiKey());
		assertEquals("SECRET_LISTEN_KEY", rn.getListenKey());

		InputStream isrr = getClass().getResourceAsStream("/net/pms/external/audioaddict/rockradio.txt");
		String pls = IO.readLines(isrr);
		String url = rn.getBestUrlFromPlaylist(pls);
		assertEquals("http://prem2.rockradio.com:80/00srock", url);
	}

	@Test
	public void testBatchRequest() throws StreamReadException, DatabindException, IOException {
		InputStream is = getClass().getResourceAsStream("/net/pms/external/audioaddict/batch_request.txt");
		ObjectMapper om = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
		Root networkBatchRoot = om.readValue(is, Root.class);
		assertEquals(18, networkBatchRoot.channelFilters.size());
	}

}
