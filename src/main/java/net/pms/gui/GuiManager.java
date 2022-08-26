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
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.newgui.LooksFrame;

public class GuiManager {
	private static final List<String> LOG_BUFFER = new ArrayList<>();
	private static final List<IGui> GUI_INSTANCES = new ArrayList<>();

	private static EConnectionState connectionState = EConnectionState.UNKNOWN;
	private static long readCount = 0;
	private static int currentBitrate = 0;
	private static int peakBitrate = 0;
	private static boolean reloadable = false;
	private static boolean serverReady = false;

	public static void addGui(IGui gui) {
		if (gui != null) {
			synchronized (GUI_INSTANCES) {
				GUI_INSTANCES.add(gui);
				if (gui instanceof LooksFrame) {
					// fill the log
					dumpCurrentLog(gui);
				}
				gui.setConnectionState(connectionState);
				gui.setCurrentBitrate(currentBitrate);
				gui.setPeakBitrate(peakBitrate);
				gui.setReloadable(reloadable);
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

	public static void setConnectionState(EConnectionState value) {
		synchronized (GUI_INSTANCES) {
			if (!value.equals(connectionState)) {
				connectionState = value;
				if (!GUI_INSTANCES.isEmpty()) {
					for (IGui guiInstance : GUI_INSTANCES) {
						guiInstance.setConnectionState(connectionState);
					}
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

	public static void setReloadable(boolean value) {
		synchronized (GUI_INSTANCES) {
			if (reloadable != value) {
				reloadable = value;
				if (!GUI_INSTANCES.isEmpty()) {
					for (IGui guiInstance : GUI_INSTANCES) {
						guiInstance.setReloadable(reloadable);
					}
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
			if (!serverReady) {
				serverReady = true;
				startMemoryThread();
				if (!GUI_INSTANCES.isEmpty()) {
					for (IGui guiInstance : GUI_INSTANCES) {
						guiInstance.serverReady();
					}
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

	public static void updateBuffer() {
		long buf = 0;
		List<RendererConfiguration> foundRenderers = PMS.get().getFoundRenderers();
		synchronized (foundRenderers) {
			for (RendererConfiguration r : foundRenderers) {
				buf += r.getBuffer();
			}
		}
		if (buf == 0 && currentBitrate != 0) {
			currentBitrate = 0;
			updateCurrentBitrate();
		}
	}

	public static void setReadValue(long v) {
		if (v > readCount) {
			int sizeinMb = (int) ((v - readCount) / 125) / 1024;
			if (currentBitrate != sizeinMb) {
				currentBitrate = sizeinMb;
				updateCurrentBitrate();
			}
			if (sizeinMb > peakBitrate) {
				peakBitrate = sizeinMb;
				updatePeakBitrate();
			}
		}
		readCount = v;
	}

	private static void updateCurrentBitrate() {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setCurrentBitrate(currentBitrate);
				}
			}
		}
	}

	private static void updatePeakBitrate() {
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setPeakBitrate(peakBitrate);
				}
			}
		}
	}

	private static int maxMemory;
	private static int usedMemory;
	private static int bufferMemory;

	private static void updateMemoryUsage() {
		maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1048576);
		usedMemory = (int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576);
		long buf = 0;
		List<RendererConfiguration> foundRenderers = PMS.get().getFoundRenderers();
		synchronized (foundRenderers) {
			for (RendererConfiguration r : foundRenderers) {
				buf += (r.getBuffer());
			}
		}
		if (buf == 0 && currentBitrate != 0) {
			currentBitrate = 0;
			updateCurrentBitrate();
		}
		bufferMemory = (int) buf;
		synchronized (GUI_INSTANCES) {
			if (!GUI_INSTANCES.isEmpty()) {
				for (IGui guiInstance : GUI_INSTANCES) {
					guiInstance.setMemoryUsage(maxMemory, usedMemory, bufferMemory);
				}
			}
		}
	}

	private static void startMemoryThread() {
		if (!UPDATE_MEMORY_USAGE_THREAD.isAlive()) {
			UPDATE_MEMORY_USAGE_THREAD.start();
		}
	}

	private static final Thread UPDATE_MEMORY_USAGE_THREAD = new Thread(() -> {
		while (true) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				return;
			}
			updateMemoryUsage();
		}
	}, "GuiManager Memory Usage Updater");

}
