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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import net.pms.PMS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes care of registering plugins. Plugin jars are loaded,
 * instantiated and stored for later retrieval.
 */
public class ExternalFactory {
	/**
	 * For logging messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFactory.class);

	/**
	 * List of external listener class instances.
	 */
	private static List<ExternalListener> externalListeners = new ArrayList<ExternalListener>();

	/**
	 * List of external listener classes.
	 */
	private static List<Class<?>> externalListenerClasses = new ArrayList<Class<?>>();

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

	/**
	 * This method scans the plugins directory for ".jar" files and processes
	 * each file that is found. First, a resource named "plugin" is extracted
	 * from the jar file. Its contents determine the name of the main plugin
	 * class. This main plugin class is then loaded and an instance is created
	 * and registered for later use.
	 */
	public static void lookup() {
		File pluginDirectory = new File(PMS.getConfiguration().getPluginDirectory());
		LOGGER.info("Searching for plugins in " + pluginDirectory.getAbsolutePath());

		if (!pluginDirectory.exists()) {
			LOGGER.warn("Plugin directory doesn't exist: " + pluginDirectory);
			return;
		}

		if (!pluginDirectory.isDirectory()) {
			LOGGER.warn("Plugin directory is not a directory: " + pluginDirectory);
			return;
		}

		// Filter all .jar files from the plugin directory
		File[] jarFiles = pluginDirectory.listFiles(
			new FileFilter() {
				public boolean accept(File file) {
					return file.isFile() && file.getName().toLowerCase().endsWith(".jar");
				}
			}
		);

		int nJars = jarFiles.length;

		if (nJars == 0) {
			LOGGER.info("No plugins found");
			return;
		}

		// To load a .jar file the filename needs to converted to a file URL
		List<URL> jarURLList = new ArrayList<URL>();

		for (int i = 0; i < nJars; ++i) {
			try {
				jarURLList.add(jarFiles[i].toURI().toURL());
			} catch (MalformedURLException e) {
				LOGGER.error("Can't convert file path " + jarFiles[i] + " to URL", e);
			}
		}

		URL[] jarURLs = new URL[jarURLList.size()];
		jarURLList.toArray(jarURLs);

		// Create a classloader to take care of loading the plugin classes from
		// their URL.
		URLClassLoader classLoader = new URLClassLoader(jarURLs);
		Enumeration<URL> resources;

		try {
			// Each plugin .jar file has to contain a resource named "plugin"
			// which should contain the name of the main plugin class.
			resources = classLoader.getResources("plugin");
		} catch (IOException e) {
			LOGGER.error("Can't load plugin resources", e);
			return;
		}

		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();

			try {
				// Determine the plugin main class name from the contents of
				// the plugin file.
				InputStreamReader in = new InputStreamReader(url.openStream());
				char[] name = new char[512];
				in.read(name);
				in.close();
				String pluginMainClassName = new String(name).trim();

				LOGGER.info("Found plugin: " + pluginMainClassName);

				// Try to load the class based on the main class name
				Class<?> clazz = classLoader.loadClass(pluginMainClassName);
				registerListenerClass(clazz);
			} catch (Exception e) {
				LOGGER.error("Error loading plugin", e);
			} catch (NoClassDefFoundError e) {
				LOGGER.error("Error loading plugin", e);
			}
		}

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
			if (!AdditionalFolderAtRoot.class.isAssignableFrom(clazz) &&
				!AdditionalFoldersAtRoot.class.isAssignableFrom(clazz)) {

				try {
					// Create a new instance of the plugin class and store it
					ExternalListener instance = (ExternalListener) clazz.newInstance();
					registerListener(instance);
				} catch (InstantiationException e) {
					LOGGER.error("Error instantiating plugin", e);
				} catch (IllegalAccessException e) {
					LOGGER.error("Error instantiating plugin", e);
				}
			}
		}
	}

	/**
	 * This method instantiates the external listeners whose class has not yet
	 * been instantiated by {@link #instantiateEarlyListeners()}.
	 */
	public static void instantiateLateListeners() {
		for (Class<?> clazz: externalListenerClasses) {
			// Only AdditionalFolderAtRoot and AdditionalFoldersAtRoot
			// classes have been skipped by lookup().
			if (AdditionalFolderAtRoot.class.isAssignableFrom(clazz) ||
				AdditionalFoldersAtRoot.class.isAssignableFrom(clazz)) {

				try {
					// Create a new instance of the plugin class and store it
					ExternalListener instance = (ExternalListener) clazz.newInstance();
					registerListener(instance);
				} catch (InstantiationException e) {
					LOGGER.error("Error instantiating plugin", e);
				} catch (IllegalAccessException e) {
					LOGGER.error("Error instantiating plugin", e);
				}
			}
		}
	}
}
