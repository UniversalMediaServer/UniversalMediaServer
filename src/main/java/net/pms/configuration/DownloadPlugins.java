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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JLabel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.external.ExternalFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadPlugins {
	private final static String PLUGIN_LIST_URL = "http://www.universalmediaserver.com/plugins/list.php";
	private final static String PLUGIN_TEST_FILE = "plugin_inst.tst";

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadPlugins.class);

	private static final int TYPE_JAR = 0;
	private static final int TYPE_LIST = 1;
	private static final int TYPE_PLATFORM_LIST = 2;

	private String id;
	private String name;
	private String rating;
	private String desc;
	private String url;
	private String author;
	private String version;
	private int type;
	private ArrayList<URL> jars;
	private JLabel updateLabel;
	private String[] props;
	private boolean test;

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
					LOGGER.info("An invalid plugin was ignored");
				}
				plugin = new DownloadPlugins(test);
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
			LOGGER.info("An invalid plugin was ignored");
		}
		in.close();
	}

	public DownloadPlugins() {
		this(false);
	}

	public DownloadPlugins(boolean test) {
		type = DownloadPlugins.TYPE_JAR;
		rating = "--";
		jars = null;
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

	private void unzip(File f,String dir) {
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
				BufferedOutputStream dest = new BufferedOutputStream(fos, 4096);
				while ((count = zis.read(data, 0, 4096)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
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

		LOGGER.debug("fetch file "+url);
		if (updateLabel != null) {
			updateLabel.setText(Messages.getString("NetworkTab.47") + ": " + fName);
		}

		File f = new File(dir + File.separator + fName);
		if(f.exists()) {
			// no need to download it twice
			return true;
		}
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
		
		if(fName.endsWith(".zip")) {
			for(int i=0;i<props.length;i++) {
				if(props[i].equalsIgnoreCase("unzip")) {
					unzip(f,dir);
					continue;
				}
			}
		}			

		// If we got down here add the jar to the list (if it is a jar)
		if (f.getAbsolutePath().endsWith(".jar")) {
			jars.add(f.toURI().toURL());
		}
		return true;
	}
	
	private boolean command(String cmd,String[] args) {
		if(cmd.equalsIgnoreCase("move")) {
			// arg1 is src and arg2 is dst
			try {
				FileUtils.moveFile(new File(args[1]), new File(args[2]));
			} catch (IOException e) {
				// Ignore errors, just log it
				LOGGER.debug("couldn't move file " + args[1] + " to " + args[2]);
			}
			return true;
		}
		if(cmd.equalsIgnoreCase("touch")) {
			// arg1 is file to touch
			try {
				FileUtils.touch(new File(args[1]));
			} catch (IOException e) {
				// Ignore errors, just log it
				LOGGER.debug("couldn't touch file " + args[1]);
			}
			return true;
		}
		return false;
	}

	private boolean downloadList(String url) throws Exception {
		if ("".equals(url)) {
			url = PLUGIN_LIST_URL + "?id=" + id;
		}
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
			String filename = "";
			
			if(command(tmp[0], tmp)) {
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
		return res;
	}

	private boolean download() throws Exception {
		if (type == DownloadPlugins.TYPE_LIST) {
			if(isTest()) {
				// we can't use the id based stuff
				// when testing
				return downloadList(url);
			}
			return downloadList("");
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
		 if (isTest()) {
			 return downloadFile(url, PMS.getConfiguration().getPluginDirectory(), "");
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
