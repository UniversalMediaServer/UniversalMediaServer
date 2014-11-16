package net.pms.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import net.pms.PMS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.protocol.http.HttpURLConnection;

public class OpenSubtitle {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSubtitle.class);
	private static final String SUB_DIR = "subs";
	private static final String UA = "OSTestUserAgent";
	private static final long TOKEN_AGE_TIME = 10 * 60 * 1000; // 10 mins
	//private static final long SUB_FILE_AGE = 14 * 24 * 60 * 60 * 1000; // two weeks

	/**
	 * Size of the chunks that will be hashed in bytes (64 KB)
	 */
	private static final int HASH_CHUNK_SIZE = 64 * 1024;

	private static final String OPENSUBS_URL = "http://api.opensubtitles.org/xml-rpc";
	private static String token = null;
	private static long tokenAge;

	public static String computeHash(File file) throws IOException {
		long size = file.length();
		FileInputStream fis = new FileInputStream(file);
		return computeHash(fis, size);
	}

	public static String computeHash(InputStream stream, long length) throws IOException {

		int chunkSizeForFile = (int) Math.min(HASH_CHUNK_SIZE, length);

		// Buffer that will contain the head and the tail chunk, chunks will overlap if length is smaller than two chunks
		byte[] chunkBytes = new byte[(int) Math.min(2 * HASH_CHUNK_SIZE, length)];
		long head;
		long tail;
		try (DataInputStream in = new DataInputStream(stream)) {
			// First chunk
			in.readFully(chunkBytes, 0, chunkSizeForFile);

			long position = chunkSizeForFile;
			long tailChunkPosition = length - chunkSizeForFile;

			// Seek to position of the tail chunk, or not at all if length is smaller than two chunks 
			while (position < tailChunkPosition && (position += in.skip(tailChunkPosition - position)) >= 0);

			// Second chunk, or the rest of the data if length is smaller than two chunks 
			in.readFully(chunkBytes, chunkSizeForFile, chunkBytes.length - chunkSizeForFile);

			head = computeHashForChunk(ByteBuffer.wrap(chunkBytes, 0, chunkSizeForFile));
			tail = computeHashForChunk(ByteBuffer.wrap(chunkBytes, chunkBytes.length - chunkSizeForFile, chunkSizeForFile));
		}
		return String.format("%016x", length + head + tail);
	}

	private static long computeHashForChunk(ByteBuffer buffer) {

		LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
		long hash = 0;

		while (longBuffer.hasRemaining()) {
			hash += longBuffer.get();
		}

		return hash;
	}

	public static String postPage(URLConnection connection, String query) throws IOException {
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setRequestProperty("Content-Length", "" + query.length());
		((HttpURLConnection)connection).setRequestMethod("POST");
		//LOGGER.debug("opensub query "+query);
		// open up the output stream of the connection
		if (!StringUtils.isEmpty(query)) {
			try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
				output.writeBytes(query);
				output.flush();
			}
		}

		StringBuilder page;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			page = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null) {
				page.append(str.trim());
				page.append("\n");
			}
		}
		//LOGGER.debug("opensubs result page "+page.toString());
		return page.toString();
	}

	private static boolean tokenIsYoung() {
		long now = System.currentTimeMillis();
		return ((now - tokenAge) < TOKEN_AGE_TIME);
	}

	private static void login() throws IOException {
		if ((token != null) && tokenIsYoung()) {
			return;
		}
		URL url = new URL(OPENSUBS_URL);
		String req = "<methodCall>\n<methodName>LogIn</methodName>\n<params>\n<param>\n<value><string/></value>\n</param>\n" +
			"<param>\n" +
			"<value><string/></value>\n</param>\n<param>\n<value><string/></value>\n" +
			"</param>\n<param>\n<value><string>" + UA + "</string></value>\n</param>\n" +
			"</params>\n" +
			"</methodCall>\n";
		Pattern re = Pattern.compile("token.*?<string>([^<]+)</string>", Pattern.DOTALL);
		Matcher m = re.matcher(postPage(url.openConnection(), req));
		if (m.find()) {
			token = m.group(1);
			tokenAge = System.currentTimeMillis();
		}
	}

	public static String fetchImdbId(File f) throws IOException {
		return fetchImdbId(getHash(f));
	}

	public static String fetchImdbId(String hash) throws IOException {
		LOGGER.debug("fetch imdbid for hash " + hash);
		login();
		if (token == null) {
			return "";
		}
		URL url = new URL(OPENSUBS_URL);
		String req = "<methodCall>\n<methodName>CheckMovieHash2</methodName>\n" +
			"<params>\n<param>\n<value><string>" + token + "</string></value>\n</param>\n" +
			"<param>\n<value>\n<array>\n<data>\n<value><string>" + hash + "</string></value>\n" +
			"</data>\n</array>\n</value>\n</param>" +
			"</params>\n</methodCall>\n";
		Pattern re = Pattern.compile("MovieImdbID.*?<string>([^<]+)</string>", Pattern.DOTALL);
		Matcher m = re.matcher(postPage(url.openConnection(), req));
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	public static String getHash(File f) throws IOException {
		LOGGER.debug("get hash of " + f);
		String hash = ImdbUtil.extractOSHash(f);
		if (!StringUtils.isEmpty(hash)) {
			return hash;
		}
		return computeHash(f);
	}

	public static Map<String, Object> findSubs(File f) throws IOException {
		Map<String, Object> res = findSubs(getHash(f), f.length());
		if (res.isEmpty()) { // no good on hash! try imdb
			String imdb = ImdbUtil.extractImdb(f);
			if (StringUtils.isEmpty(imdb)) {
				imdb = fetchImdbId(f);
			}
			res = findSubs(imdb);
		}
		if (res.isEmpty()) { // final try, use the name
			res = querySubs(f.getName());
		}
		return res;

	}

	public static Map<String, Object> findSubs(String hash, long size) throws IOException {
		return findSubs(hash, size, null, null);
	}

	public static Map<String, Object> findSubs(String imdb) throws IOException {
		return findSubs(null, 0, imdb, null);
	}

	public static Map<String, Object> querySubs(String query) throws IOException {
		return findSubs(null, 0, null, query);
	}

	public static Map<String, Object> findSubs(String hash, long size, String imdb, String query) throws IOException {
		login();
		TreeMap<String, Object> res = new TreeMap<>();
		if (token == null) {
			return res;
		}
		String lang = iso639(PMS.getConfiguration().getSubtitlesLanguages());
		URL url = new URL(OPENSUBS_URL);
		String hashStr = "";
		String imdbStr = "";
		String qStr = "";
		if (!StringUtils.isEmpty(hash)) {
			hashStr = "<member><name>moviehash</name><value><string>" + hash + "</string></value></member>\n" +
				"<member><name>moviebytesize</name><value><double>" + size + "</double></value></member>\n";
		} else if (!StringUtils.isEmpty(imdb)) {
			imdbStr = "<member><name>imdbid</name><value><string>" + imdb + "</string></value></member>\n";
		} else if (!StringUtils.isEmpty(query)) {
			qStr = "<member><name>query</name><value><string>" + query + "</string></value></member>\n";
		} else {
			return res;
		}
		String req = "<methodCall>\n<methodName>SearchSubtitles</methodName>\n" +
			"<params>\n<param>\n<value><string>" + token + "</string></value>\n</param>\n" +
			"<param>\n<value>\n<array>\n<data>\n<value><struct><member><name>sublanguageid" +
			"</name><value><string>" + lang + "</string></value></member>" +
			hashStr + imdbStr + qStr + "\n" +
			"</struct></value></data>\n</array>\n</value>\n</param>" +
			"</params>\n</methodCall>\n";
		Pattern re = Pattern.compile("SubFileName</name>.*?<string>([^<]+)</string>.*?SubLanguageID</name>.*?<string>([^<]+)</string>.*?SubDownloadLink</name>.*?<string>([^<]+)</string>", Pattern.DOTALL);
		String page = postPage(url.openConnection(), req);
		Matcher m = re.matcher(page);
		while (m.find()) {
			LOGGER.debug("found subtitle " + m.group(2) + " name " + m.group(1) + " zip " + m.group(3));
			res.put(m.group(2) + ":" + m.group(1), m.group(3));
			if (res.size() > PMS.getConfiguration().liveSubtitlesLimit()) {
				// limit the number of hits somewhat
				break;
			}
		}
		return res;
	}

	private static String iso639(String s) {
		String[] tmp = s.split(",");
		StringBuilder res = new StringBuilder();
		String sep = "";
		for (String tmp1 : tmp) {
			res.append(sep).append(Iso639.getISO639_2Code(tmp1));
			sep = ",";
		}
		if (StringUtils.isNotEmpty(res)) {
			return res.toString();
		}
		return s;
	}

	public static String subFile(String name) {
		String dir = PMS.getConfiguration().getDataFile(SUB_DIR); 
		File path = new File(dir);
		if (!path.exists()) {
			path.mkdirs();
		}
		return path.getAbsolutePath() + File.separator + name + ".srt";
	}

	public static String fetchSubs(String url) throws FileNotFoundException, IOException {
		return fetchSubs(url, subFile(String.valueOf(System.currentTimeMillis())));
	}

	public static String fetchSubs(String url, String outName) throws FileNotFoundException, IOException {
		login();
		if (token == null) {
			return "";
		}
		if (StringUtils.isEmpty(outName)) {
			outName = subFile(String.valueOf(System.currentTimeMillis()));
		}
		File f = new File(outName);
		URL u = new URL(url);
		URLConnection connection = u.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		InputStream in = connection.getInputStream();
		OutputStream out;
		try (GZIPInputStream gzipInputStream = new GZIPInputStream(in)) {
			out = new FileOutputStream(f);
			byte[] buf = new byte[4096];
			int len;
			while ((len = gzipInputStream.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		}
		out.close();
		if (!PMS.getConfiguration().isLiveSubtitlesKeep()) {
			PMS.get().addTempFile(f);
		}
		return f.getAbsolutePath();
	}

	public static String getLang(String str) {
		String[] tmp = str.split(":", 2);
		if (tmp.length > 1) {
			return tmp[0];
		}
		return "";
	}

	public static String getName(String str) {
		String[] tmp = str.split(":", 2);
		if (tmp.length > 1) {
			return tmp[1];
		}
		return str;
	}

	public static void convert() {
		if (PMS.getConfiguration().isLiveSubtitlesKeep()) {
			return;
		}
		File path = new File(PMS.getConfiguration().getDataFile(SUB_DIR));
		if (!path.exists()) {
			// no path nothing to do
			return;
		}
		File[] files = path.listFiles();
		for (File file : files) {
			PMS.get().addTempFile(file);
		}
	}
}
