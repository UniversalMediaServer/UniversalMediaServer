package net.pms.fileprovider;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import net.pms.PMS;
import net.pms.fileprovider.filesystem.FilesystemFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileProviderFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileProviderFactory.class);

	/**
	 * The name of the file containing the package and class name
	 * for which UMS will instantiate the file provider plugin. This file has to 
	 * be contained at the root of the jar
	 */
	private static final String DESCRIPTOR_FILE_NAME = "FileProviderPlugin";
	private static final String JAR_FILE_EXTENSION = ".jar";

	/**
	 * The class loader will be lazy initialized the first time it's being used
	 */
	private static ClassLoader classLoader;

	private static List<FileProvider> fileProviders = new ArrayList<>();
	private static FileProvider activeFileProvider;
	
	/**
	 * Private constructor to avoid instantiation.
	 */
	private FileProviderFactory() { }
	
	/**
	 * Gets the file providers.
	 *
	 * @return the file providers
	 */
	public static List<FileProvider> getFileProviders() {
		return fileProviders;
	}
	
	public static FileProvider getActiveFileProvider() {
		if(activeFileProvider == null) {
			String fileProviderClassName = PMS.getConfiguration().getActiveFileProviderClassName();
			FileProvider fileProvider = getFileProviderbyClassName(fileProviderClassName);
			
			if(fileProvider == null) {
				// Fall back to the default class provider if the configured one isn't being found; it must exist!
				fileProvider = getFileProviderbyClassName(FilesystemFileProvider.class.getName());
			}
			
			if(!fileProvider.isActivated()) {
				fileProvider.activate();
			}
			
			activeFileProvider = fileProvider;
		}
		
		return activeFileProvider;
	}
	
	/**
	 * Sets the active file provider.
	 *
	 * @param fileProvider the active file provider
	 */
	public static void setActiveFileProvider(FileProvider fileProvider) {
		if(activeFileProvider != null) {
			// Deactivate the currently active file provider
			activeFileProvider.deactivate();
		}
		
		if(!fileProvider.isActivated()) {
			// Activate the newly active file provider if it hasn't been previously done
			fileProvider.activate();
		}
		
		activeFileProvider = fileProvider;
		
		// Persist the change
		PMS.getConfiguration().setActiveFileProviderClassName(fileProvider.getClass().getName());
	}
	
	/**
	 * Registers a file provider.
	 *
	 * @param fileProvider the file provider to register
	 */
	public static void registerFileProvider(FileProvider fileProvider) {
		if(!fileProviders.contains(fileProvider)) {
			fileProviders.add(fileProvider);
			LOGGER.info(String.format("File provider '%s' has been registered", fileProvider.getName()));
		} else {
			LOGGER.debug(String.format("File provider '%s' has been previously registered", fileProvider.getName()));			
		}
	}

	/**
	 * This method scans the plugins directory for ".jar" files and processes
	 * each file that is found.<br>First, a resource named "FileProviderPlugin" is extracted
	 * from the jar file. Its content determines the name of the main plugin
	 * class. This main plugin class is then loaded and an instance is being created
	 * and registered for later use.
	 */
	public static void lookup() {
		File pluginDirectory = new File(PMS.getConfiguration().getPluginDirectory());

		if (!pluginDirectory.exists()) {
			LOGGER.warn("File provider plugin directory doesn't exist: " + pluginDirectory);
			return;
		}

		if (!pluginDirectory.isDirectory()) {
			LOGGER.warn("File provider plugin directory is not a directory: " + pluginDirectory);
			return;
		}

		LOGGER.info("Searching for file provider plugins  in " + pluginDirectory.getAbsolutePath());
		
		// Filter all .jar files from the plugin directory
		File[] jarFiles = pluginDirectory.listFiles(
			new FileFilter() {
				public boolean accept(File file) {
					return file.isFile() && file.getName().toLowerCase().endsWith(JAR_FILE_EXTENSION);
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
			URI jarUri = jarFiles[i].toURI();
			try {
				jarURLList.add(jarUri.toURL());
			} catch (MalformedURLException e) {
				LOGGER.error(String.format("Can't convert file path '%s' to URL", jarUri), e);
			}
		}

		URL[] jarURLs = new URL[jarURLList.size()];
		jarURLList.toArray(jarURLs);

		if(classLoader == null) {
			// specify the parent classloader being PMS to include the required plugin interface definitions
			// for the classloader. If this isn't being set, a ClassNotFoundException might be raised
			// because the interface implemented by the plugin can't be resolved.
			classLoader = new URLClassLoader(jarURLs, PMS.class.getClassLoader());
		}
		Enumeration<URL> resources;

		try {
			resources = classLoader.getResources(DESCRIPTOR_FILE_NAME);
		} catch (IOException e) {
			LOGGER.error("Can't load file provider plugin resources", e);
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
				Object instance;
				try {
					instance = classLoader.loadClass(pluginMainClassName).newInstance();
				} catch (Throwable t) {
					// this can happen if a plugin created for a custom build is being dropped inside
					// the plugins directory of pms. The plugin might implement an interface only
					// available in the custom build, but not in pms.
					LOGGER.warn(String.format("The file provider plugin '%s' couldn't be loaded", pluginMainClassName), t);
					continue;
				}
				if (instance instanceof FileProvider) {
					registerFileProvider((FileProvider) instance);
				}
			} catch (Exception e) {
				LOGGER.error("Error loading file provider plugin", e);
			} catch (NoClassDefFoundError e) {
				LOGGER.error("Error loading file provider plugin", e);
			}
		}
	}
	
	private static FileProvider getFileProviderbyClassName(String className) {
		FileProvider resultfileProvider = null;
		for(FileProvider fileProvider : getFileProviders()) {
			if(fileProvider.getClass().getName().equals(className)) {
				resultfileProvider = fileProvider;
				break;
			}
		}
		return resultfileProvider;
	}
}
