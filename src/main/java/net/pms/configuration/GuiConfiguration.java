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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles storage of gui properties.
 */
public class GuiConfiguration implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(GuiConfiguration.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private String graphicsDevice;
	private Rectangle screenBounds;
	private Insets screenInsets;
	private Rectangle windowBounds;
	private int windowState;
	private transient boolean dirty;
	private transient Path path;

	public String getGraphicsDevice() {
		return graphicsDevice;
	}

	public void setGraphicsDevice(String graphicsDevice) {
		if (this.graphicsDevice == null ? graphicsDevice != null : !this.graphicsDevice.equals(graphicsDevice)) {
			this.graphicsDevice = graphicsDevice;
			dirty = true;
		}
	}

	public Rectangle getScreenBounds() {
		return screenBounds;
	}

	public void setScreenBounds(Rectangle screenBounds) {
		if (this.screenBounds == null ? screenBounds != null : !this.screenBounds.equals(screenBounds)) {
			this.screenBounds = screenBounds.getBounds();
			dirty = true;
		}
	}

	public Insets getScreenInsets() {
		return screenInsets;
	}

	public void setScreenInsets(Insets screenInsets) {
		if (this.screenInsets == null ? screenInsets != null : !this.screenInsets.equals(screenInsets)) {
			this.screenInsets = (Insets) screenInsets.clone();
			dirty = true;
		}
	}

	public Rectangle getWindowBounds() {
		return windowBounds;
	}

	public void setWindowBounds(Rectangle windowBounds) {
		if (this.windowBounds == null ? windowBounds != null : !this.windowBounds.equals(windowBounds)) {
			this.windowBounds = windowBounds.getBounds();
			dirty = true;
		}
	}

	public int getWindowState() {
		return windowState;
	}

	public void setWindowState(int windowState) {
		if (this.windowState != windowState) {
			this.windowState = windowState;
			dirty = true;
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	public void save() {
		if (dirty && path != null) {
			dirty = false;
			try {
				LOGGER.debug("Writing window configuration file: " + path);
				Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
			} catch (IOException e) {
				LOGGER.debug("An error occurred while writing the window configuration file: {}", e);
			}
		}
	}

	/**
	 * Finds the {@link GraphicsConfiguration} that matches the previously
	 * stored information, if any.
	 * <p>
	 * <b>Note:</b> This class is <i>not</i> thread-safe, and all other methods
	 * is called either by the constructor or the event dispatcher thread. Great
	 * care should be taken when calling this method to make sure this
	 * {@link WindowPropertiesConfiguration} isn't currently in use by a
	 * {@link WindowProperties} instance at the time.
	 *
	 * @return The matching {@link GraphicsConfiguration} or {@code null} if no
	 * match was found.
	 */
	public GraphicsConfiguration getGraphicsConfiguration() {
		if (StringUtils.isBlank(graphicsDevice) || screenBounds == null) {
			LOGGER.debug("No stored graphics device, using the default");
			return null;
		}
		for (GraphicsDevice graphicsDeviceItem : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			if (graphicsDevice.equals(graphicsDeviceItem.getIDstring())) {
				for (GraphicsConfiguration graphicsConfiguration : graphicsDeviceItem.getConfigurations()) {
					if (screenBounds.equals(graphicsConfiguration.getBounds())) {
						return graphicsConfiguration;
					}
				}
			}
		}
		LOGGER.debug("No matching graphics configuration found, using the default");
		return null;
	}

	/**
	 * Creates a new instance bound to the specified {@link Path}.
	 *
	 * @param path the {@link Path} used to read and write window
	 *            properties.
	 */
	public static GuiConfiguration getConfiguration(UmsConfiguration umsConfiguration) {
		GuiConfiguration conf = null;
		Path path = Paths.get(umsConfiguration.getProfileDirectory()).resolve("GUI.conf");
		try {
			if (Files.exists(path)) {
				LOGGER.info("Getting gui configuration file : " + path);
				String json = Files.readString(path, StandardCharsets.UTF_8);
				conf = GSON.fromJson(json, GuiConfiguration.class);
			}
		} catch (IOException | JsonSyntaxException ex) {
			LOGGER.info("Error in gui configuration file : " + ex.getMessage());
			LOGGER.debug(null, ex);
		}
		if (conf == null) {
			conf = new GuiConfiguration();
		}
		conf.path = path;
		conf.save();
		return conf;
	}

}
