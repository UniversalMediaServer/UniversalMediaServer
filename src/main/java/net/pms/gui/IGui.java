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

import net.pms.renderers.Renderer;

public interface IGui {
	public void appendLog(String msg);
	public void setCurrentBitrate(int sizeinMb);
	public void setPeakBitrate(int sizeinMb);
	public void setMemoryUsage(int maxMemory, int usedMemory, int dbCacheMemory, int bufferMemory);
	public void setConnectionState(EConnectionState connectionState);
	public void addRenderer(Renderer renderer);
	public void setReloadable(boolean reload);
	public void addEngines();
	public void setStatusLine(String line);
	public void setSecondaryStatusLine(String line);
	public void serverReady();
	public void updateServerStatus();
	public void setMediaScanStatus(boolean running);
	public void enableWebUiButton();
	public void showErrorMessage(String message, String title);
	public void setConfigurationChanged(String key);
}
