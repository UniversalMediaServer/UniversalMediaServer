package net.pms.network;

import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * <pre>
 * Unescaped regex for testMusicFile:
 *
 * \(\s+F\.TYPE\s+=\s+1\s+and\s+A\.SONGNAME\s+regexp\s'\.\*Auto\.\*'\s*\)
 *</pre>
 */
public class SearchRequestHandlerTest {

	@Test
	public void testMusicFile() {
		SearchRequestHandler sr = new SearchRequestHandler();
		String s = "( upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Auto\" )";
		String result = sr.convertToSql(s, sr.getRequestType(s)).toString();
		System.out.println(result);
		assertTrue(result.matches("select\\s+FILENAME,\\s+MODIFIED\\s+from\\s+FILES\\s+as\\s+F\\s+left\\s+outer\\s+join\\s+AUDIOTRACKS\\s+as\\s+A\\s+on\\s+F\\.ID\\s+=\\s+A\\.FILEID\\s+where\\s+\\(\\s+F\\.TYPE\\s+=\\s+1\\s+and\\s+A\\.SONGNAME\\s+regexp\\s+'\\.\\*Auto\\.\\*'\\s*\\)"));
	}

	@Test
	public void testAlbumFile() {
		SearchRequestHandler sr = new SearchRequestHandler();
		String s = "( upnp:class derivedfrom \"object.container.album\" and dc:title contains \"Auto\")";
		String result = sr.convertToSql(s, sr.getRequestType(s)).toString();
		System.out.println(result);
		assertTrue(result.matches("select\\s+FILENAME,\\s+MODIFIED\\s+from\\s+FILES\\s+as\\s+F\\s+left\\s+outer\\s+join\\s+AUDIOTRACKS\\s+as\\s+A\\s+on\\s+F\\.ID\\s+=\\s+A\\.FILEID\\s+where\\s+\\(\\s+F\\.TYPE\\s+=\\s+1\\s+and\\s+A\\.ALBUM\\s+regexp\\s+'\\.\\*Auto\\.\\*'\\s*\\)"));
	}
}
