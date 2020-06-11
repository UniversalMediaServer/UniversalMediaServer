/*
 * Universal Media Server, for streaming any media to DLNA
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

package net.pms.configuration;

import com.sun.jna.Platform;
import java.io.BufferedOutputStream;
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
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JLabel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.external.ExternalFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadPlugins {
	private final static String PLUGIN_LIST_URL = "https://www.universalmediaserver.com/plugins/list.php";
	private final static String PLUGIN_TEST_FILE = "plugin_inst.tst";

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadPlugins.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	private String id;
	private String name;
	private String rating;
	private String desc;
	private String url;
	private String author;
	private String version;
	private ArrayList<URL> jars;
	private JLabel updateLabel;
	private String[] props;
	private boolean test;
	private boolean old;

	public static ArrayList<DownloadPlugins> downloadList() {
		ArrayList<DownloadPlugins> res = new ArrayList<>();

		if (!configuration.getExternalNetwork()) {
			// Do not try to get the plugin list if there is no
			// external network.
			return res;
		}

		try {
			URL u = new URL(PLUGIN_LIST_URL);
			URLConnection connection = u.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			parse_list(res, in, false);
			File test = new File(configuration.getPluginDirectory() + File.separator + PLUGIN_TEST_FILE);
			if (test.exists()) {
				in = new BufferedReader(new InputStreamReader(new FileInputStream(test)));
				parse_list(res, in, true);
			}
		} catch (IOException e) {
		}
		return res;
	}

	private static int getInt(String val) {
		try {
			return Integer.parseInt(val);
		} catch (Exception e) {
			return 0;
		}
	}

	private static void parse_list(ArrayList<DownloadPlugins> res, BufferedReader in, boolean test) throws IOException {
		String str;
		DownloadPlugins plugin = new DownloadPlugins(test);
		while ((str = in.readLine()) != null) {
			str = str.trim();
			if (StringUtils.isEmpty(str)) {
				if (plugin.isOk() || plugin.isTest()) {
					if (test) {
						plugin.setRating("TEST");
					}
					res.add(plugin);
				} else {
					LOGGER.trace("An invalid plugin was ignored (1)");
				}
				plugin = new DownloadPlugins(test);
				continue;
			}
			String[] keyval = str.split("=", 2);
			if (keyval.length < 2) {
				continue;
			}
			if (keyval[0].equalsIgnoreCase("id")) {
				plugin.id = keyval[1];
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
			if (keyval[0].equalsIgnoreCase("version")) {
				plugin.version = keyval[1];
			}
			if (keyval[0].equalsIgnoreCase("type")) {
				// Deprecated
			}
			if (keyval[0].equalsIgnoreCase("require")) {
				plugin.old = false;
				String[] minVer = keyval[1].split("\\.");
				String[] myVer = PMS.getVersion().split("\\.");
				int max = Math.max(myVer.length, minVer.length);
				for (int i = 0; i < max; i++) {
					int my,min;
					// If the versions are of different length
					// say that the part of "wrong" length is 0
					my = getInt(((i > myVer.length) ? "0" : myVer[i]));
					min = getInt(((i > minVer.length) ? "0" : minVer[i]));
					if (min == my) {
						// This is equal take the next part of the
						// version string
						continue;
					}
					if (min > my) {
						// Old otherwise it is new
						plugin.old = true;
					}
					// anyway stop here
					break;
				}
			}
			if (keyval[0].equalsIgnoreCase("prop")) {
				plugin.props = keyval[1].split(",");
			}
		}
		if (plugin.isOk() || plugin.isTest()) { // Add the last one
			if (test) {
				plugin.setRating("TEST");
			}
			res.add(plugin);
		} else {
			LOGGER.trace("An invalid plugin was ignored (2)");
		}
		in.close();
	}

	public DownloadPlugins() {
		this(false);
	}

	public DownloadPlugins(boolean test) {
		rating = "--";
		jars = null;
		old = false;
		this.test = test;
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

	public String getVersion() {
		return version;
	}

	public void setRating(String str) {
		rating = str;
	}

	public boolean isOk() {
		// We must have a name and ID
		return (!StringUtils.isEmpty(name)) && (!StringUtils.isEmpty(id));
	}

	public boolean isTest() {
		return test;
	}

	public boolean isOld() {
		return old;
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
		int pos = str.lastIndexOf('/');
		if (pos == -1) {
			return name;
		}
		return str.substring(pos + 1);
	}

	private void ensureCreated(String p) {
		File f = new File(p);
		f.mkdirs();
	}

	private void unzip(File f, String dir) {
		// Zip file with loads of goodies
		// Unzip it
		ZipInputStream zis;
		try {
			zis = new ZipInputStream(new FileInputStream(f));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File dst = new File(dir + File.separator + entry.getName());
				if (entry.isDirectory()) {
					dst.mkdirs();
					continue;
				}
				int count;
				byte data[] = new byte[4096];
				FileOutputStream fos = new FileOutputStream(dst);
				try (BufferedOutputStream dest = new BufferedOutputStream(fos, 4096)) {
					while ((count = zis.read(data, 0, 4096)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
				}
				if (dst.getAbsolutePath().endsWith(".jar")) {
					jars.add(dst.toURI().toURL());
				}
			}
			zis.close();
		} catch (Exception e) {
			LOGGER.info("unzip error " + e);
		}
		f.delete();
	}

	private boolean downloadFile(String url, String dir, String name) throws Exception {
		URL u = new URL(url);
		ensureCreated(dir);
		String fName = extractFileName(url, name);

		LOGGER.debug("fetch file " + url);
		if (updateLabel != null) {
			updateLabel.setText(Messages.getString("NetworkTab.47") + ": " + fName);
		}

		File f = new File(dir + File.separator + fName);
		if (f.exists()) {
			// no need to download it twice
			return true;
		}
		URLConnection connection = u.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(f)) {
			byte[] buf = new byte[4096];
			int len;

			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
			}

			out.flush();
		} catch (Exception e) {
			LOGGER.warn("Plugin download error: " + e);
		}

		if (fName.endsWith(".zip")) {
			for (String prop : props) {
				if (prop.equalsIgnoreCase("unzip")) {
					unzip(f, dir);
				}
			}
		}

		// If we got down here add the jar to the list (if it is a jar)
		if (f.getAbsolutePath().endsWith(".jar")) {
			jars.add(f.toURI().toURL());
		}
		return true;
	}

	private void doExec(String args) throws IOException, InterruptedException, ConfigurationException {
		int pos = args.indexOf(',');
		if (pos == -1) { // weird stuff
			return;
		}

		// Everything after the "," is what we're supposed to run
		// First make note of jars we got
		File[] oldJar = new File(configuration.getPluginDirectory()).listFiles();

		// Before we start external installers better save the config
		configuration.save();
		ProcessBuilder pb = new ProcessBuilder(args.substring(pos + 1));
		pb.redirectErrorStream(true);
		Map<String, String> env = pb.environment();
		env.put("PROFILE_PATH", configuration.getProfilePath());
		env.put("UMS_VERSION", PMS.getVersion());

		LOGGER.debug("running '" + args + "'");
		Process pid = pb.start();
		// Consume and log any output
		Scanner output = new Scanner(pid.getInputStream());
		while (output.hasNextLine()) {
			LOGGER.debug("[" + args + "] " + output.nextLine());
		}
		configuration.reload();
		pid.waitFor();

		File[] newJar = new File(configuration.getPluginDirectory()).listFiles();
		for (File f : newJar) {
			if (!f.getAbsolutePath().endsWith(".jar")) {
				// skip non jar files
				continue;
			}
			for (File oldJar1 : oldJar) {
				if (f.getAbsolutePath().equals(oldJar1.getAbsolutePath())) {
					// old jar file break out, and set f to null to skip adding it
					f = null;
					break;
				}
			}
			// if f is null this is an jar that is old
			if (f != null) {
				jars.add(f.toURI().toURL());
			}
		}
	}

	private boolean command(String cmd, String args) throws ConfigurationException {
		if (cmd.equalsIgnoreCase("move")) {
			// arg1 is src and arg2 is dst
			String[] tmp = args.split(",");
			try {
				FileUtils.moveFile(new File(tmp[1]), new File(tmp[2]));
			} catch (IOException e) {
				// Ignore errors, just log it
				LOGGER.debug("couldn't move file " + tmp[1] + " to " + tmp[2]);
			}
			return true;
		}

		if (cmd.equalsIgnoreCase("touch")) {
			// arg1 is file to touch
			String[] tmp = args.split(",");
			try {
				FileUtils.touch(new File(tmp[1]));
			} catch (IOException e) {
				// Ignore errors, just log it
				LOGGER.debug("couldn't touch file " + tmp[1]);
			}

			return true;
		}

		if (cmd.equalsIgnoreCase("conf")) {
			String[] tmp = args.split(",", 2);
			tmp = tmp[1].split("=");
			configuration.setCustomProperty(tmp[1], tmp[2]);
			configuration.save();
			return true;
		}

		if (cmd.equalsIgnoreCase("exec")) {
			try {
				doExec(args);
			} catch (ConfigurationException | IOException | InterruptedException e) {
			}

			return true;
		}

		return false;
	}

	private boolean downloadList(String url) throws Exception {
		if ("".equals(url)) {
			url = PLUGIN_LIST_URL + "?id=" + id;
		}
		String platform = Platform.isWindows() ? "windows" : Platform.isLinux() ? "linux" : Platform.isMac() ? "osx" : "";
		URL u = new URL(url);
		URLConnection connection = u.openConnection();
		boolean res, skip = false;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String str;
			res = true;

			while ((str = in.readLine()) != null) {
				str = str.trim();
				if (StringUtils.isEmpty(str)) {
					continue;
				}
				if (str.toLowerCase().matches("windows|linux|osx")) {
					skip = !str.toLowerCase().equals(platform);
					continue;
				}
				if (skip) {
					// This line is specific to another platform, ignore it
					continue;
				}

				String[] tmp = str.split(",", 3);
				String dir = configuration.getPluginDirectory();
				String filename = "";

				if (command(tmp[0], str)) {
					// a command take the next line
					continue;
				}

				if (tmp.length > 1) {
					String rootDir = new File("").getAbsolutePath();
					if (tmp[1].equalsIgnoreCase("root")) {
						dir = rootDir;
					} else {
						dir = rootDir + File.separator + tmp[1];
					}
					if (tmp.length > 2) {
						filename = tmp[2];
					}
				}

				res &= downloadFile(tmp[0], dir, filename);
			}
		}

		return res;
	}

	private boolean download() throws Exception {
		if (isTest()) {
			// we can't use the id based stuff
			// when testing
			return downloadList(url);
		}
		return downloadList("");
	}

	public boolean install(JLabel update) throws Exception {
		LOGGER.debug("Installing plugin " + name);
		updateLabel = update;

		// Init the jar file list
		jars = new ArrayList<>();

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
