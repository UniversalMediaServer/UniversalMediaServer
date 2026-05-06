/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.configuration;

import java.io.File;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

/**
 * A {@link PropertiesConfiguration} subclass that keeps an associated
 * {@link FileHandler} so that file-related operations (load, save, getFile …)
 * are available directly on the configuration object – matching the CC 1.x API
 * that the rest of the code was written against.
 */
public class ManagedPropertiesConfiguration extends PropertiesConfiguration {

	private final FileHandler fileHandler = new FileHandler(this);

	/** Auto-save listener – added/removed by {@link #setAutoSave(boolean)}. */
	private final EventListener<ConfigurationEvent> autoSaveListener = event -> {
		if (!event.isBeforeUpdate()) {
			try {
				fileHandler.save();
			} catch (ConfigurationException e) {
				// best-effort; caller is responsible for explicit saves
			}
		}
	};

	// -----------------------------------------------------------------------
	// File I/O – delegates to FileHandler
	// -----------------------------------------------------------------------

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

	/**
	 * Reload the configuration from the file that was previously set via
	 * {@link #setPath(String)} or {@link #setFile(File)}.
	 */
	public void refresh() throws ConfigurationException {
		fileHandler.load();
	}

	// -----------------------------------------------------------------------
	// Auto-save support
	// -----------------------------------------------------------------------

	public void setAutoSave(boolean autoSave) {
		if (autoSave) {
			addEventListener(ConfigurationEvent.ANY, autoSaveListener);
		} else {
			removeEventListener(ConfigurationEvent.ANY, autoSaveListener);
		}
	}
}
