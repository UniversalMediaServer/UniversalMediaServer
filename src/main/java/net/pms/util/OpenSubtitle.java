/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSubtitle {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSubtitle.class);
	private static final String SUB_DIR = "subs";
	private static final String UA = "Universal Media Server v1";
	private static final long TOKEN_AGE_TIME = 10 * 60 * 1000; // 10 mins
	//private static final long SUB_FILE_AGE = 14 * 24 * 60 * 60 * 1000; // two weeks

	/**
	 * Size of the chunks that will be hashed in bytes (64 KB)
	 */
	private static final int HASH_CHUNK_SIZE = 64 * 1024;

	private static final String OPENSUBS_URL = "http://api.opensubtitles.org/xml-rpc";
	private static final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();
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
		((HttpURLConnection) connection).setRequestMethod("POST");
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

	/*
	 * This MUST be called with a lock on tokenLock
	 */
	private static boolean tokenIsYoung() {
		long now = System.currentTimeMillis();
		return ((now - tokenAge) < TOKEN_AGE_TIME);
	}

	private static boolean login() throws IOException {
		tokenLock.writeLock().lock();
		try {
			if (token != null && tokenIsYoung()) {
				return true;
			}
			URL url = new URL(OPENSUBS_URL);
			CredMgr.Cred cred = PMS.getCred("opensubtitles");
			String pwd = "";
			String usr = "";
			if(cred != null) {
				// if we got credentials use them
				if (!StringUtils.isEmpty(cred.password)) {
					pwd = DigestUtils.md5Hex(cred.password);
				}
				usr = cred.username;
			}
			String req = "<methodCall>\n<methodName>LogIn</methodName>\n<params>\n"+
					"<param>\n<value><string>"+usr+"</string></value>\n</param>\n" +
					"<param>\n" +
					"<value><string>"+pwd+"</string></value>\n</param>\n<param>\n<value><string/></value>\n" +
					"</param>\n<param>\n<value><string>" + UA + "</string></value>\n</param>\n" +
					"</params>\n" +
					"</methodCall>\n";
			Pattern re = Pattern.compile("token.*?<string>([^<]+)</string>", Pattern.DOTALL);
			Matcher m = re.matcher(postPage(url.openConnection(), req));
			if (m.find()) {
				token = m.group(1);
				tokenAge = System.currentTimeMillis();
			}
			return token != null;
		} finally {
			tokenLock.writeLock().unlock();
		}
	}

	public static String fetchImdbId(File f) throws IOException {
		return fetchImdbId(getHash(f));
	}

	public static String fetchImdbId(String hash) throws IOException {
		LOGGER.debug("fetch imdbid for hash " + hash);
		Pattern re = Pattern.compile("MovieImdbID.*?<string>([^<]+)</string>", Pattern.DOTALL);
		String info = checkMovieHash(hash);
		LOGGER.debug("info is " + info);
		Matcher m = re.matcher(info);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	private static String checkMovieHash(String hash) throws IOException {
		if (!login()) {
			return "";
		}
		URL url = new URL(OPENSUBS_URL);
		tokenLock.readLock().lock();
		String req = null;
		try {
		req = "<methodCall>\n<methodName>CheckMovieHash</methodName>\n" +
			"<params>\n<param>\n<value><string>" + token + "</string></value>\n</param>\n" +
			"<param>\n<value>\n<array>\n<data>\n<value><string>" + hash + "</string></value>\n" +
			"</data>\n</array>\n</value>\n</param>" +
			"</params>\n</methodCall>\n";
		} finally {
			tokenLock.readLock().unlock();
		}
		LOGGER.debug("req " + req);
		return postPage(url.openConnection(), req);
	}

	public static String getMovieInfo(File f) throws IOException {
		String info = checkMovieHash(getHash(f));
		if (StringUtils.isEmpty(info)) {
			return "";
		}
		@SuppressWarnings("unused")
		Pattern re = Pattern.compile("MovieImdbID.*?<string>([^<]+)</string>", Pattern.DOTALL);
		LOGGER.debug("info is " + info);
		return info;
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
		return findSubs(f, null);
	}

	public static Map<String, Object> findSubs(File f, RendererConfiguration r) throws IOException {
		Map<String, Object> res = findSubs(getHash(f), f.length(), null, null, r);
		if (res.isEmpty()) { // no good on hash! try imdb
			String imdb = ImdbUtil.extractImdb(f);
			if (StringUtils.isEmpty(imdb)) {
				imdb = fetchImdbId(f);
			}
			res = findSubs(null, 0, imdb, null, r);
		}
		if (res.isEmpty()) { // final try, use the name
			res = querySubs(f.getName(), r);
		}
		return res;
	}

	public static Map<String, Object> findSubs(String hash, long size) throws IOException {
		return findSubs(hash, size, null, null, null);
	}

	public static Map<String, Object> findSubs(String imdb) throws IOException {
		return findSubs(null, 0, imdb, null, null);
	}

	public static Map<String, Object> querySubs(String query) throws IOException {
		return querySubs(query, null);
	}

	public static Map<String, Object> querySubs(String query, RendererConfiguration r) throws IOException {
		return findSubs(null, 0, null, query, r);
	}

	public static Map<String, Object> findSubs(String hash, long size, String imdb, String query, RendererConfiguration r) throws IOException {
		TreeMap<String, Object> res = new TreeMap<>();
		if (!login()) {
			return res;
		}
		String lang = UMSUtils.getLangList(r, true);
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
		String req = null;
		tokenLock.readLock().lock();
		try {
			req = "<methodCall>\n<methodName>SearchSubtitles</methodName>\n" +
				"<params>\n<param>\n<value><string>" + token + "</string></value>\n</param>\n" +
				"<param>\n<value>\n<array>\n<data>\n<value><struct><member><name>sublanguageid" +
				"</name><value><string>" + lang + "</string></value></member>" +
				hashStr + imdbStr + qStr + "\n" +
				"</struct></value></data>\n</array>\n</value>\n</param>" +
				"</params>\n</methodCall>\n";
		} finally {
			tokenLock.readLock().unlock();
		}
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

	/**
	 * Feeds the correct parameters to getInfo below.
	 *
	 * @see #getInfo(java.lang.String, long, java.lang.String, java.lang.String)
	 *
	 * @param f the file to lookup
	 * @param formattedName the name to use in the name search
	 *
	 * @return
	 * @throws IOException
	 */
	public static String[] getInfo(File f, String formattedName) throws IOException {
		return getInfo(f, formattedName, null);
	}

	public static String[] getInfo(File f, String formattedName, RendererConfiguration r) throws IOException {
		String[] res = getInfo(getHash(f), f.length(), null, null, r);
		if (res == null || res.length == 0) { // no good on hash! try imdb
			String imdb = ImdbUtil.extractImdb(f);
			if (StringUtil.hasValue(imdb)) {
				res = getInfo(null, 0, imdb, null, r);
			}
		}
		if (res == null || res.length == 0) { // final try, use the name
			if (StringUtils.isNotEmpty(formattedName)) {
				res = getInfo(null, 0, null, formattedName, r);
			} else {
				res = getInfo(null, 0, null, f.getName(), r);
			}
		}
		return res;
	}

	/**
	 * Attempt to return information from IMDb about the file based on information
	 * from the filename; either the hash, the IMDb ID or the filename itself.
	 *
	 * @param hash  the video hash
	 * @param size  the bytesize to be used with the hash
	 * @param imdb  the IMDb ID
	 * @param query the string to search IMDb for
	 *
	 * @return a string array including the IMDb ID, episode title, season number,
	 *         episode number relative to the season, and the show name, or null
	 *         if we couldn't find it on IMDb.
	 *
	 * @throws IOException
	 */
	private static String[] getInfo(String hash, long size, String imdb, String query, RendererConfiguration r) throws IOException {
		if (!login()) {
			return null;
		}
		String lang = UMSUtils.getLangList(r, true);
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
			return null;
		}
		String req = null;
		tokenLock.readLock().lock();
		try {
			req = "<methodCall>\n<methodName>SearchSubtitles</methodName>\n" +
				"<params>\n<param>\n<value><string>" + token + "</string></value>\n</param>\n" +
				"<param>\n<value>\n<array>\n<data>\n<value><struct><member><name>sublanguageid" +
				"</name><value><string>" + lang + "</string></value></member>" +
				hashStr + imdbStr + qStr + "\n" +
				"</struct></value></data>\n</array>\n</value>\n</param>" +
				"</params>\n</methodCall>\n";
		} finally {
			tokenLock.readLock().unlock();
		}
		Pattern re = Pattern.compile(
				".*IDMovieImdb</name>.*?<string>([^<]+)</string>.*?" + "" +
				"MovieName</name>.*?<string>([^<]+)</string>.*?" +
				"SeriesSeason</name>.*?<string>([^<]+)</string>.*?" +
				"SeriesEpisode</name>.*?<string>([^<]+)</string>.*?" +
				"MovieYear</name>.*?<string>([^<]+)</string>.*?",
				Pattern.DOTALL
		);
		String page = postPage(url.openConnection(), req);
		Matcher m = re.matcher(page);
		if (m.find()) {
			LOGGER.debug("match " + m.group(1) + "," + m.group(2) + "," + m.group(3) + "," + m.group(4) + "," + m.group(5));
			Pattern re1 = Pattern.compile("&#34;([^&]+)&#34;(.*)");
			String name = m.group(2);
			Matcher m1 = re1.matcher(name);
			String episodeName = "";
			if (m1.find()) {
				episodeName = m1.group(2).trim();
				name = m1.group(1).trim();
			}

			/**
 			 * Sometimes if OpenSubtitles doesn't have an episode title they call it
 			 * something like "Episode #1.4", so discard that.
 			 */
 			episodeName = StringEscapeUtils.unescapeHtml4(episodeName);
 			if (episodeName.startsWith("Episode #")) {
 				episodeName = "";
 			}

			return new String[]{
				ImdbUtil.ensureTT(m.group(1).trim()),
				episodeName,
				StringEscapeUtils.unescapeHtml4(name),
				m.group(3).trim(), // Season number
				m.group(4).trim(), // Episode number
				m.group(5).trim()  // Year
			};
		}
		return null;
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
		if (!login()) {
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
			int tmo = PMS.getConfiguration().getLiveSubtitlesTimeout();
			if (tmo <= 0) {
				PMS.get().addTempFile(f);
			}
			else {
				PMS.get().addTempFile(f, tmo);
			}
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
