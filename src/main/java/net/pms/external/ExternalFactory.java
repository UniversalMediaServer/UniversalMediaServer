/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.ArrayList;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JLabel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.external.URLResolver.URLResult;
import net.pms.util.FilePermissions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class takes care of registering plugins. Plugin jars are loaded,
 * instantiated and stored for later retrieval.
 */
public class ExternalFactory {
	/**
	 * For logging messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFactory.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * List of external listener class instances.
	 */
	private static List<ExternalListener> externalListeners = new ArrayList<>();

	/**
	 * List of external listener classes.
	 */
	private static List<Class<?>> externalListenerClasses = new ArrayList<>();

	/**
	 * List of external listener classes (not yet started).
	 */
	private static List<Class<?>> downloadedListenerClasses = new ArrayList<>();

	/**
	 * List of urlresolvers.
	 */
	private static List<URLResolver> urlResolvers = new ArrayList<>();

	private static boolean allDone = false;

	/**
	 * Returns the list of external listener class instances.
	 *
	 * @return The instances.
	 */
	public static List<ExternalListener> getExternalListeners() {
		return externalListeners;
	}

	/**
	 * Stores the instance of an external listener in a list for later
	 * retrieval. The same instance will only be stored once.
	 *
	 * @param listener The instance to store.
	 */
	public static void registerListener(ExternalListener listener) {
		if (!externalListeners.contains(listener)) {
			externalListeners.add(listener);
			if (listener instanceof URLResolver) {
				addURLResolver((URLResolver) listener);
			}
		}
	}

	/**
	 * Stores the class of an external listener in a list for later retrieval.
	 * The same class will only be stored once.
	 *
	 * @param clazz The class to store.
	 */
	private static void registerListenerClass(Class<?> clazz) {
		if (!externalListenerClasses.contains(clazz)) {
			externalListenerClasses.add(clazz);
		}
	}

	private static String getMainClass(URL jar) {
		URL[] jarURLs1 = {jar};
		try (URLClassLoader classLoader = new URLClassLoader(jarURLs1)) {
			Enumeration<URL> resources;
			// Each plugin .jar file has to contain a resource named "plugin"
			// which should contain the name of the main plugin class.
			resources = classLoader.getResources("plugin");

			if (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				char[] name;

				// Determine the plugin main class name from the contents of
				// the plugin file.
				try (InputStreamReader in = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
					name = new char[512];
					if (in.read(name) > 0) {
						return new String(name).trim();
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Can't load plugin resources: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	private static boolean isLib(URL jar) {
		return (getMainClass(jar) == null);
	}

	public static void loadJARs(URL[] jarURLs, boolean download) {
		// find lib jars first
		ArrayList<URL> libs = new ArrayList<>();

		for (URL jarURL : jarURLs) {
			if (isLib(jarURL)) {
				libs.add(jarURL);
			}
		}

		URL[] jarURLs1 = new URL[libs.size() + 1];
		libs.toArray(jarURLs1);
		int pos = libs.size();

		for (URL jarURL : jarURLs) {
			jarURLs1[pos] = jarURL;
			loadJAR(jarURLs1, download, jarURL);
		}
	}

	/**
	 * This method loads the jar files found in the plugin dir
	 * or if installed from the web.
	 */
	public static void loadJAR(final URL[] jarURL, boolean download, URL newURL) {
		/* Create a classloader to take care of loading the plugin classes from
		 * their URL.
		 *
		 * A note on the suppressed warning: The classloader need to remain open as long
		 * as the loaded classes are in use - in our case forever.
		 * @see http://stackoverflow.com/questions/13944868/leaving-classloader-open-after-first-use
		 */
		@SuppressWarnings("resource")
		URLClassLoader classLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {

			@Override
			public URLClassLoader run() {
				return new URLClassLoader(jarURL);
			}

		});
		Enumeration<URL> resources;

		try {
			// Each plugin .jar file has to contain a resource named "plugin"
			// which should contain the name of the main plugin class.
			resources = classLoader.getResources("plugin");
		} catch (IOException e) {
			LOGGER.error("Can't load plugin resources: {}", e.getMessage());
			LOGGER.trace("", e);
			try {
				classLoader.close();
			} catch (IOException e2) {
				// Just swallow
			}
			return;
		}

		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();

			try {
				// Determine the plugin main class name from the contents of
				// the plugin file.
				char[] name;
				try (InputStreamReader in = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
					name = new char[512];
					if (in.read(name) < 1) {
						continue;
					}
				}
				String pluginMainClassName = new String(name).trim();

				LOGGER.info("Found plugin: " + pluginMainClassName);

				if (download) {
					// Only purge code when downloading!
					purgeCode(pluginMainClassName, newURL);
				}

				// Try to load the class based on the main class name
				Class<?> clazz = classLoader.loadClass(pluginMainClassName);
				registerListenerClass(clazz);

				if (download) {
					downloadedListenerClasses.add(clazz);
				}
			} catch (Exception | NoClassDefFoundError e) {
				LOGGER.error("Error loading plugin", e);
			}
		}
	}

	@SuppressFBWarnings("DM_GC")
	private static void purgeCode(String mainClass, URL newUrl) {
		Class<?> clazz1 = null;

		for (Class<?> clazz : externalListenerClasses) {
			if (mainClass.equals(clazz.getCanonicalName())) {
				clazz1 = clazz;
				break;
			}
		}

		if (clazz1 == null) {
			return;
		}

		externalListenerClasses.remove(clazz1);
		ExternalListener remove = null;
		for (ExternalListener list : externalListeners ) {
			if (list.getClass().equals(clazz1)) {
				remove = list;
				break;
			}
		}

		RendererConfiguration.resetAllRenderers();

		if (remove != null) {
			externalListeners.remove(remove);
			remove.shutdown();
		}

		for (int i = 0; i < 3; i++) {
			System.gc();
		}

		URLClassLoader cl = (URLClassLoader) clazz1.getClassLoader();
		URL[] urls = cl.getURLs();
		for (URL url : urls) {
			String mainClass1 = getMainClass(url);

			if (mainClass1 == null || !mainClass.equals(mainClass1)) {
				continue;
			}

			File f = url2file(url);
			File f1 = url2file(newUrl);

			if (f1 == null || f ==null) {
				continue;
			}

			if (!f1.getName().equals(f.getName())) {
				addToPurgeFile(f);
			}
		}
	}

	private static File url2file(URL url) {
		File f;

		try {
			f = new File(url.toURI());
		} catch(URISyntaxException e) {
			f = new File(url.getPath());
		}

		return f;
	}

	private static void addToPurgeFile(File f) {
		try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("purge", true), StandardCharsets.UTF_8)) {
			out.write(f.getAbsolutePath() + "\r\n");
			out.flush();
		} catch (IOException e) {
			LOGGER.debug("Purge file error: {}" + e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private static void purgeFiles() {
		File purge = new File("purge");
		String action = configuration.getPluginPurgeAction();

		if (action.equalsIgnoreCase("none")) {
			if (!purge.delete()) {
				LOGGER.error("Could not delete purgefile: \"{}\"", purge.getAbsolutePath());
			}
			return;
		}

		try (FileInputStream fis = new FileInputStream(purge); BufferedReader in = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
			String line;

			while ((line = in.readLine()) != null) {
				File f = new File(line);

				if (action.equalsIgnoreCase("delete")) {
					if (!f.delete()) {
						LOGGER.error("Could not delete file: \"{}\"", f.getAbsolutePath());
					}
				} else if(action.equalsIgnoreCase("backup")) {
					FileUtils.moveFileToDirectory(f, new File("backup"), true);
					if (!f.delete()) {
						LOGGER.error("Could not delete file: \"{}\"", f.getAbsolutePath());
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error while deleting plugins: {}", e.getMessage());
			LOGGER.trace("", e);
		}
		if (!purge.delete()) {
			LOGGER.error("Could not delete purgefile: \"{}\"", purge.getAbsolutePath());
		}
	}

	/**
	 * This method scans the plugins directory for ".jar" files and processes
	 * each file that is found. First, a resource named "plugin" is extracted
	 * from the jar file. Its contents determine the name of the main plugin
	 * class. This main plugin class is then loaded and an instance is created
	 * and registered for later use.
	 */
	@SuppressWarnings("null")
	public static void lookup() {
		// Start by purging files
		purgeFiles();
		File pluginsFolder = new File(configuration.getPluginDirectory());
		LOGGER.info("Searching for plugins in " + pluginsFolder.getAbsolutePath());

		try {
			FilePermissions permissions = new FilePermissions(pluginsFolder);
			if (!permissions.isFolder()) {
				LOGGER.warn("Plugins folder is not a folder: " + pluginsFolder.getAbsolutePath());
				return;
			}
			if (!permissions.isBrowsable()) {
				LOGGER.warn("Plugins folder is not readable: " + pluginsFolder.getAbsolutePath());
				return;
			}
		} catch (FileNotFoundException e) {
			LOGGER.warn("Can't find plugins folder: {}", e.getMessage());
			return;
		}

		// Find all .jar files in the plugin directory
		File[] jarFiles = pluginsFolder.listFiles(
			new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isFile() && file.getName().toLowerCase().endsWith(".jar");
				}
			}
		);

		int nJars = (jarFiles == null) ? 0 : jarFiles.length;

		if (nJars == 0) {
			LOGGER.info("No plugins found");
			return;
		}

		// To load a .jar file the filename needs to converted to a file URL
		List<URL> jarURLList = new ArrayList<>();

		for (int i = 0; i < nJars; ++i) {
			try {
				jarURLList.add(jarFiles[i].toURI().toURL());
			} catch (MalformedURLException e) {
				LOGGER.error("Can't convert file path " + jarFiles[i] + " to URL", e);
			}
		}

		URL[] jarURLs = new URL[jarURLList.size()];
		jarURLList.toArray(jarURLs);

		// Load the jars
		loadJARs(jarURLs, false);

		// Instantiate the early external listeners immediately.
		instantiateEarlyListeners();
	}

	/**
	 * This method instantiates the external listeners that need to be
	 * instantiated immediately so they can influence the PMS initialization
	 * process.
	 * <p>
	 * Not all external listeners are instantiated immediately to avoid
	 * premature initialization where other parts of PMS have not been
	 * initialized yet. Those listeners are instantiated at a later time by
	 * {@link #instantiateLateListeners()}.
	 */
	private static void instantiateEarlyListeners() {
		for (Class<?> clazz: externalListenerClasses) {
			// Skip the classes that should not be instantiated at this
			// time but rather at a later time.
			try {
				// Create a new instance of the plugin class and store it
				ExternalListener instance = (ExternalListener) clazz.newInstance();
				registerListener(instance);
			} catch (InstantiationException | IllegalAccessException e) {
				LOGGER.error("Error instantiating plugin", e);
			}
		}
	}

	/**
	 * This method instantiates the external listeners whose class has not yet
	 * been instantiated by {@link #instantiateEarlyListeners()}.
	 */
	public static void instantiateLateListeners() {
		allDone = true;
	}

	private static void postInstall(Class<?> clazz) {
		Method postInstall;
		try {
			postInstall = clazz.getDeclaredMethod("postInstall", (Class<?>[]) null);

			if (Modifier.isStatic(postInstall.getModifiers())) {
				postInstall.invoke((Object[]) null, (Object[]) null);
			}
		}

		// Ignore all errors
		catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
		}
	}

	private static void doUpdate(JLabel update, String text) {
		if (update == null) {
			return;
		}

		update.setText(text);
	}

	public static void instantiateDownloaded(JLabel update) {
		// These are found in the downloadedListenerClasses list
		for (Class<?> clazz: downloadedListenerClasses) {
			ExternalListener instance;

			try {
				doUpdate(update, Messages.getString("NetworkTab.48") + " " + clazz.getSimpleName());
				postInstall(clazz);
				LOGGER.debug("do inst of " + clazz.getSimpleName());
				instance = (ExternalListener) clazz.newInstance();
				doUpdate(update,instance.name() + " " + Messages.getString("NetworkTab.49"));
				registerListener(instance);
			} catch (InstantiationException | IllegalAccessException e) {
				LOGGER.error("Error instantiating plugin", e);
			}
		}

		downloadedListenerClasses.clear();
	}

	public static boolean localPluginsInstalled() {
		return allDone;
	}

	private static boolean quoted(String s) {
		return s.startsWith("\"") && s.endsWith("\"");
	}

	private static String quote(String s) {
		if (quoted(s)) {
			return s;
		}
		return "\"" + s + "\"";
	}

	public static URLResult resolveURL(String url) {
		String quotedUrl = quote(url);
		for (URLResolver resolver : urlResolvers) {
			URLResult res = resolver.urlResolve(url);
			if (res != null) {
				if (StringUtils.isEmpty(res.url) || quotedUrl.equals(quote(res.url))) {
					res.url = null;
				}
				if (res.precoder != null && res.precoder.isEmpty()) {
					res.precoder = null;
				}
				if (res.args != null && res.args.isEmpty()) {
					res.args = null;
				}
				if (res.url != null || res.precoder != null || res.args != null) {
					LOGGER.debug(((ExternalListener)resolver).name() + " resolver:" +
						(res.url == null ? "" : " url=" + res.url) +
						(res.precoder == null ? "" : " precoder=" + res.precoder) +
						(res.args == null ? "" : " args=" + res.args));
					return res;
				}
			}
		}
		return null;
	}

	public static void addURLResolver(URLResolver res) {
		if (urlResolvers.contains(res)) {
			return;
		}
		if (urlResolvers.isEmpty()) {
			urlResolvers.add(res);
			return;
		}

		String[] tmp = PMS.getConfiguration().getURLResolveOrder();
		if (tmp.length == 0) {
			// no order at all, just add it
			urlResolvers.add(res);
			return;
		}
		int id = -1;
		for (int i = 0; i < tmp.length; i++) {
			if (tmp[i].equalsIgnoreCase(res.name())) {
				id = i;
				break;
			}
		}

		if (id == -1) {
			// no order here, just add it
			urlResolvers.add(res);
			return;
		}
		if (id > urlResolvers.size()) {
			// add it last
			urlResolvers.add(res);
			return;
		}
		urlResolvers.add(id, res);
	}
}
