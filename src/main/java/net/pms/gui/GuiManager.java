/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.gui;

import java.util.ArrayList;
import java.util.List;
import net.pms.configuration.RendererConfiguration;
import net.pms.newgui.LooksFrame;

public class GuiManager {
	private static final List<String> LOG_BUFFER = new ArrayList<>();
	private static final List<IGui> GUI_INSTANCES = new ArrayList<>();

	public static void addGui(IGui gui) {
		synchronized (GUI_INSTANCES) {
			GUI_INSTANCES.add(gui);
			if (gui instanceof LooksFrame) {
				// drain the buffer
				dumpCurrentLog(gui);
			}
		}
	}

	// fill gui with current log
	private static void dumpCurrentLog(IGui gui) {
		synchronized (LOG_BUFFER) {
			if (!LOG_BUFFER.isEmpty()) {
				for (String buffered : LOG_BUFFER) { // drain the buffer
					gui.appendLog(buffered);
				}
			}
		}
	}

	public static void appendLog(String msg) {
		synchronized (LOG_BUFFER) {
			LOG_BUFFER.add(msg);
		}
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.appendLog(msg);
				}
			}
		}
	}

	public static String[] getLogLines() {
		synchronized (LOG_BUFFER) {
			return (String[]) LOG_BUFFER.toArray();
		}
	}

	public static void updateBuffer() {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.updateBuffer();
				}
			}
		}
	}

	public static void setReadValue(long v, String msg) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setReadValue(v, msg);
				}
			}
		}
	}

	public static void setConnectionState(EConnectionState connectionState) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setConnectionState(connectionState);
				}
			}
		}
	}

	public static void addRenderer(RendererConfiguration renderer) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.addRenderer(renderer);
				}
			}
		}
	}

	public static void updateRenderer(RendererConfiguration renderer) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.updateRenderer(renderer);
				}
			}
		}
	}

	public static void setReloadable(boolean reload) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setReloadable(reload);
				}
			}
		}
	}

	public static void addEngines() {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.addEngines();
				}
			}
		}
	}

	public static void setStatusLine(String line) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setStatusLine(line);
				}
			}
		}
	}

	public static void setSecondaryStatusLine(String line) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setSecondaryStatusLine(line);
				}
			}
		}
	}

	public static void serverReady() {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.serverReady();
				}
			}
		}
	}

	public static void updateServerStatus() {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.updateServerStatus();
				}
			}
		}
	}

	public static void setScanLibraryStatus(boolean enabled, boolean running) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setScanLibraryStatus(enabled, running);
				}
			}
		}
	}

	public static void enableWebUiButton() {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.enableWebUiButton();
				}
			}
		}
	}

	/**
	 * Show error message
	 * @param message the message to display
	 * @param title the title string for the dialog
	 */
	public static void showErrorMessage(String message, String title) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.showErrorMessage(message, title);
				}
			}
		}
	}

	/**
	 * Let know guis that configuration has changed
	 * @param key the configuration key
	 */
	public static void setConfigurationChanged(String key) {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setConfigurationChanged(key);
				}
			}
		}
	}
}
