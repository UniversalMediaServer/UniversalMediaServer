package net.pms.configuration;

import java.io.File;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A subclass that keeps an associated so that file-related operations (load, save, getFile …)
 * are available directly on the configuration object for compatibility with the old API.
 */
public class ManagedPropertiesConfiguration extends PropertiesConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(ManagedPropertiesConfiguration.class.getName());

	private final FileHandler fileHandler = new FileHandler(this);

	private final EventListener<ConfigurationEvent> autoSaveListener = event -> {
		if (!event.isBeforeUpdate()) {
			try {
				fileHandler.save();
			} catch (ConfigurationException e) {
				LOGGER.error("Failed to auto-save configuration file: " + e.getMessage(), e);
			}
		}
	};


	public File getFile() {
		return fileHandler.getFile();
	}

	public void setFile(File file) {
		fileHandler.setFile(file);
	}

	public void setPath(String path) {
		fileHandler.setPath(path);
	}

	public void load(File file) throws ConfigurationException {
		fileHandler.load(file);
	}

	public void save() throws ConfigurationException {
		fileHandler.save();
	}

	public void refresh() throws ConfigurationException {
		fileHandler.load();
	}

	public void setAutoSave(boolean autoSave) {
		if (autoSave) {
			addEventListener(ConfigurationEvent.ANY, autoSaveListener);
		} else {
			removeEventListener(ConfigurationEvent.ANY, autoSaveListener);
		}
	}
}
