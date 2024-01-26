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
package net.pms.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.network.webguiserver.WebGuiServer;
import net.pms.renderers.Renderer;
import net.pms.swing.gui.JavaGui;

public class GuiManager {

	private static final List<String> LOG_BUFFER = Collections.synchronizedList(new ArrayList<>());
	private static final int LOG_BUFFER_SIZE = 5000;
	private static final int BYTES_TO_MBYTES = 1024 * 1024;

	private static IGui swingFrame;
	private static IGui webGui;

	private static EConnectionState connectionState = EConnectionState.UNKNOWN;
	private static long readCount = 0;
	private static int currentBitrate = 0;
	private static int peakBitrate = 0;
	private static int maxMemory;
	private static int usedMemory;
	private static int dbCacheMemory;
	private static int bufferMemory;
	private static boolean reloadable = false;
	private static boolean serverReady = false;
	private static boolean needLogFile = false;
	private static boolean mediaScanRunning = false;

	/**
	 * This class is not meant to be instantiated.
	 */
	private GuiManager() {
	}

	public static void addGui(IGui gui) {
		if (gui != null) {
			if (gui instanceof JavaGui) {
				// fill the log
				dumpCurrentLog(gui);
				swingFrame = gui;
			} else if (gui instanceof WebGuiServer) {
				webGui = gui;
			} else {
				return;
			}
			startMemoryThread();
			gui.setConnectionState(connectionState);
			gui.setCurrentBitrate(currentBitrate);
			gui.setPeakBitrate(peakBitrate);
			gui.setReloadable(reloadable);
			gui.setMemoryUsage(maxMemory, usedMemory, dbCacheMemory, bufferMemory);
			gui.setMediaScanStatus(mediaScanRunning);
			if (serverReady) {
				gui.serverReady();
			}
			List<Renderer> foundRenderers = PMS.get().getFoundRenderers();
			synchronized (foundRenderers) {
				for (Renderer renderer : foundRenderers) {
					gui.addRenderer(renderer);
				}
			}
		}
	}

	public static void removeGui(IGui gui) {
		if (gui instanceof WebGuiServer) {
			webGui = null;
		}
	}

	private static boolean hasGui() {
		return (webGui != null || swingFrame != null);
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
			if (LOG_BUFFER.size() > LOG_BUFFER_SIZE) {
				needLogFile = true;
				LOG_BUFFER.remove(0);
			}
		}
		if (swingFrame != null) {
			swingFrame.appendLog(msg);
		}
		if (webGui != null) {
			webGui.appendLog(msg);
		}
	}

	public static String[] getLogLines() {
		synchronized (LOG_BUFFER) {
			return LOG_BUFFER.toArray(String[]::new);
		}
	}

	public static boolean hasMoreLogLines() {
		return needLogFile;
	}

	public static void setConnectionState(EConnectionState value) {
		if (!value.equals(connectionState)) {
			connectionState = value;
			if (swingFrame != null) {
				swingFrame.setConnectionState(connectionState);
			}
			if (webGui != null) {
				webGui.setConnectionState(connectionState);
			}
		}
	}

	public static void addRenderer(Renderer renderer) {
		if (swingFrame != null) {
			swingFrame.addRenderer(renderer);
		}
		if (webGui != null) {
			webGui.addRenderer(renderer);
		}
	}

	public static void setReloadable(boolean value) {
		reloadable = value;
		if (swingFrame != null) {
			swingFrame.setReloadable(reloadable);
		}
		if (webGui != null) {
			webGui.setReloadable(reloadable);
		}
	}

	public static void addEngines() {
		if (swingFrame != null) {
			swingFrame.addEngines();
		}
		if (webGui != null) {
			webGui.addEngines();
		}
	}

	public static void setStatusLine(String line) {
		if (swingFrame != null) {
			swingFrame.setStatusLine(line);
		}
		if (webGui != null) {
			webGui.setStatusLine(line);
		}
	}

	public static void showSwingFrame() {
		if (swingFrame instanceof JavaGui frame) {
			frame.setVisible(true);
		}
	}

	public static void setSecondaryStatusLine(String line) {
		if (swingFrame != null) {
			swingFrame.setSecondaryStatusLine(line);
		}
		if (webGui != null) {
			webGui.setSecondaryStatusLine(line);
		}
	}

	public static void serverReady() {
		if (!serverReady) {
			serverReady = true;
			if (swingFrame != null) {
				swingFrame.serverReady();
			}
			if (webGui != null) {
				webGui.serverReady();
			}
		}
	}

	public static void updateServerStatus() {
		if (swingFrame != null) {
			swingFrame.updateServerStatus();
		}
		if (webGui != null) {
			webGui.updateServerStatus();
		}
	}

	public static void setMediaScanStatus(boolean running) {
		if (running != mediaScanRunning) {
			mediaScanRunning = running;
			if (swingFrame != null) {
				swingFrame.setMediaScanStatus(mediaScanRunning);
			}
			if (webGui != null) {
				webGui.setMediaScanStatus(mediaScanRunning);
			}
		}
	}

	public static void enableWebUiButton() {
		if (swingFrame != null) {
			swingFrame.enableWebUiButton();
		}
		if (webGui != null) {
			webGui.enableWebUiButton();
		}
	}

	/**
	 * Show error message
	 * @param message the message to display
	 * @param title the title string for the dialog
	 */
	public static void showErrorMessage(String message, String title) {
		if (swingFrame != null) {
			swingFrame.showErrorMessage(message, title);
		}
		if (webGui != null) {
			webGui.showErrorMessage(message, title);
		}
	}

	/**
	 * Let know guis that configuration has changed
	 * @param key the configuration key
	 */
	public static void setConfigurationChanged(String key) {
		if (swingFrame != null) {
			swingFrame.setConfigurationChanged(key);
		}
		if (webGui != null) {
			webGui.setConfigurationChanged(key);
		}
	}

	public static void updateBuffer() {
		long buf = 0;
		List<Renderer> foundRenderers = PMS.get().getFoundRenderers();
		synchronized (foundRenderers) {
			for (Renderer r : foundRenderers) {
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
		if (swingFrame != null) {
			swingFrame.setCurrentBitrate(currentBitrate);
		}
		if (webGui != null) {
			webGui.setCurrentBitrate(currentBitrate);
		}
	}

	private static void updatePeakBitrate() {
		if (swingFrame != null) {
			swingFrame.setPeakBitrate(peakBitrate);
		}
		if (webGui != null) {
			webGui.setPeakBitrate(peakBitrate);
		}
	}

	private static int getBytesToMegabytes(long value) {
		return (int) (value / BYTES_TO_MBYTES);
	}

	private static void updateMemoryUsage() {
		if (hasGui()) {
			maxMemory = getBytesToMegabytes(Runtime.getRuntime().maxMemory());
			usedMemory = getBytesToMegabytes(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
			dbCacheMemory = MediaDatabase.getCacheSize();
			long buf = 0;
			List<Renderer> foundRenderers = PMS.get().getFoundRenderers();
			synchronized (foundRenderers) {
				for (Renderer r : foundRenderers) {
					buf += (r.getBuffer());
				}
			}
			if (buf == 0 && currentBitrate != 0) {
				currentBitrate = 0;
				updateCurrentBitrate();
			}
			bufferMemory = (int) buf;
			if (swingFrame != null) {
				swingFrame.setMemoryUsage(maxMemory, usedMemory, dbCacheMemory, bufferMemory);
			}
			if (webGui != null) {
				webGui.setMemoryUsage(maxMemory, usedMemory, dbCacheMemory, bufferMemory);
			}
		}
	}

	private static void startMemoryThread() {
		if (!UPDATE_MEMORY_USAGE_THREAD.isAlive()) {
			UPDATE_MEMORY_USAGE_THREAD.start();
		}
	}

	private static final Runnable UPDATE_MEMORY_USAGE_RUNNABLE = () -> {
		while (true) {
			updateMemoryUsage();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	};

	private static final Thread UPDATE_MEMORY_USAGE_THREAD = new Thread(UPDATE_MEMORY_USAGE_RUNNABLE, "GuiManager Memory Usage Updater");

}
