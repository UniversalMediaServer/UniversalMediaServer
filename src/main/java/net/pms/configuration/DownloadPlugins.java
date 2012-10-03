package net.pms.configuration;

import com.sun.jna.Platform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.swing.JLabel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.external.ExternalFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadPlugins {
	private final static String PLUGIN_LIST_URL = "https://raw.github.com/SharkHunter/Channel/master/ext.txt";
	private final static String PLUGIN_TEST_FILE = "plugin_inst.tst";

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadPlugins.class);

	private static final int TYPE_JAR = 0;
	private static final int TYPE_LIST = 1;
	private static final int TYPE_PLATFORM_LIST = 2;

	private String name;
	private String rating;
	private String desc;
	private String url;
	private String author;
	private int type;
	private ArrayList<URL> jars;
	private JLabel updateLabel;

	public static ArrayList<DownloadPlugins> downloadList() {
		ArrayList<DownloadPlugins> res = new ArrayList<DownloadPlugins>();
		try {
			URL u = new URL(PLUGIN_LIST_URL);
			URLConnection connection = u.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			parse_list(res, in, false);
			File test = new File(PMS.getConfiguration().getPluginDirectory() + File.separator + PLUGIN_TEST_FILE);
			if (test.exists()) {
				in = new BufferedReader(new InputStreamReader(new FileInputStream(test)));
				parse_list(res, in, true);
			}
		} catch (Exception e) {
		}
		return res;
	}

	private static void parse_list(ArrayList<DownloadPlugins> res, BufferedReader in, boolean test) throws IOException {
		String str;
		DownloadPlugins plugin = new DownloadPlugins();
		while ((str = in.readLine()) != null) {
			str = str.trim();
			if (StringUtils.isEmpty(str)) {
				if (plugin.isOk()) {
					res.add(plugin);
				}
				plugin = new DownloadPlugins();
			}
			String[] keyval = str.split("=", 2);
			if (keyval.length < 2) {
				continue;
			}
			if (keyval[0].equalsIgnoreCase("name")) {
				plugin.name = keyval[1];
			}
			if (keyval[0].equalsIgnoreCase("rating")) {
				// Rating is temporarily switched off
				// plugin.rating = keyval[1];
			}
			if (keyval[0].equalsIgnoreCase("desc")) {
				plugin.desc = keyval[1];
			}
			if (keyval[0].equalsIgnoreCase("url")) {
				plugin.url = keyval[1];
			}
			if (keyval[0].equalsIgnoreCase("author")) {
				plugin.author = keyval[1];
			}
			if (keyval[0].equalsIgnoreCase("type")) {
				if (keyval[1].equalsIgnoreCase("jar")) {
					plugin.type = DownloadPlugins.TYPE_JAR;
				}
				if (keyval[1].equalsIgnoreCase("list")) {
					plugin.type = DownloadPlugins.TYPE_LIST;
				}
				if (keyval[1].equalsIgnoreCase("platform_list")) {
					plugin.type = DownloadPlugins.TYPE_PLATFORM_LIST;
				}
			}
		}
		if (plugin.isOk()) { // Add the last one
			if (test) {
				plugin.setRating("TEST");
			}
			res.add(plugin);
		}
		in.close();
	}

	public DownloadPlugins() {
		type = DownloadPlugins.TYPE_JAR;
		rating = "--";
		jars = null;
	}

	public String getName() {
		return name;
	}

	public String getRating() {
		return rating;
	}

	public String getAuthor() {
		return author;
	}

	public String getDescription() {
		return desc;
	}

	public void setRating(String str) {
		rating = str;
	}

	public boolean isOk() {
		// We must have a name and an url
		return (!StringUtils.isEmpty(name)) && (!StringUtils.isEmpty(url));
	}

	private String splitString(String string) {
		StringBuilder buf = new StringBuilder();
		String tempString = string;

		if (string != null) {
			while (tempString.length() > 60) {
				String block = tempString.substring(0, 60);
				int index = block.lastIndexOf(' ');
				if (index < 0) {
					index = tempString.indexOf(' ');
				}
				if (index >= 0) {
					buf.append(tempString.substring(0, index)).append("<BR>");
				}
				tempString = tempString.substring(index + 1);
			}
		} else {
			tempString = " ";
		}
		buf.append(tempString);
		return buf.toString();
	}

	private String header(String hdr) {
		return "<br><b>" + hdr + ":  </b>";
	}

	public String htmlString() {
		String res = "<html>";
		res += "<b>Name:  </b>" + getName();
		if (!StringUtils.isEmpty(getRating())) {
			res += header("Rating") + getRating();
		}
		if (!StringUtils.isEmpty(getAuthor())) {
			res += header("Author") + getAuthor();
		}
		if (!StringUtils.isEmpty(getDescription())) {
			res += header("Description") + splitString(getDescription());
		}
		return res;
	}

	private String extractFileName(String str, String name) {
		if (!StringUtils.isEmpty(name)) {
			return name;
		}
		int pos = str.lastIndexOf("/");
		if (pos == -1) {
			return name;
		}
		return str.substring(pos + 1);
	}

	private void ensureCreated(String p) {
		File f = new File(p);
		f.mkdirs();
	}

	private boolean downloadFile(String url, String dir, String name) throws Exception {
		URL u = new URL(url);
		ensureCreated(dir);
		String fName = extractFileName(url, name);
		if (updateLabel != null) {
			updateLabel.setText(Messages.getString("NetworkTab.47") + ": " + fName);
		}
		File f = new File(dir + File.separator + fName);
		URLConnection connection = u.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		InputStream in = connection.getInputStream();
		FileOutputStream out = new FileOutputStream(f);
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) != -1) {
			out.write(buf, 0, len);
		}
		out.flush();
		out.close();
		in.close();
		// If we got down here add the jar to the list (if it is a jar)
		if (f.getAbsolutePath().endsWith(".jar")) {
			jars.add(f.toURI().toURL());
		}
		return true;
	}

	private boolean downloadList(String url) throws Exception {
		URL u = new URL(url);
		URLConnection connection = u.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String str;
		boolean res = true;
		while ((str = in.readLine()) != null) {
			str = str.trim();
			if (StringUtils.isEmpty(str)) {
				continue;
			}
			String[] tmp = str.split(",", 3);
			String dir = PMS.getConfiguration().getPluginDirectory();
			String name = "";
			if (tmp.length > 1) {
				String rootDir = new File("").getAbsolutePath();
				if (tmp[1].equalsIgnoreCase("root")) {
					dir = rootDir;
				} else {
					dir = rootDir + File.separator + tmp[1];
				}
				if (tmp.length > 2) {
					name = tmp[2];
				}
			}
			res &= downloadFile(tmp[0], dir, name);
		}
		return res;
	}

	private boolean download() throws Exception {
		if (type == DownloadPlugins.TYPE_JAR) {
			return downloadFile(url, PMS.getConfiguration().getPluginDirectory(), "");
		}
		if (type == DownloadPlugins.TYPE_LIST) {
			return downloadList(url);
		}
		if (type == DownloadPlugins.TYPE_PLATFORM_LIST) {
			String ext = "";
			if (Platform.isWindows()) {
				ext = ".win";
			} else if (Platform.isLinux()) {
				ext = ".lin";
			} else if (Platform.isMac()) {
				ext = ".osx";
			}
			return downloadList(url + ext);
		}
		return false;
	}

	public boolean install(JLabel update) throws Exception {
		LOGGER.debug("Installing plugin " + name + " type " + type);
		updateLabel = update;

		// Init the jar file list
		jars = new ArrayList<URL>();

		// Download the list
		if (!download()) { // Download failed, bail out
			LOGGER.debug("Download failed");
			return false;
		}

		// Load the jars (if any)
		if (jars.isEmpty()) {
			return true;
		}
		URL[] jarURLs = new URL[jars.size()];
		jars.toArray(jarURLs);
		if (updateLabel != null) {
			updateLabel.setText("Loading JARs");
		}
		LOGGER.debug("Loading jars");
		ExternalFactory.loadJARs(jarURLs, true);

		// Create the instances of the plugins
		ExternalFactory.instantiateDownloaded(update);
		updateLabel = null;
		return true;
	}
}
