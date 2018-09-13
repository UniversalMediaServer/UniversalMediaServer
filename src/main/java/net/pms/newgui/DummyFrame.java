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
package net.pms.newgui;

import java.util.ArrayList;
import net.pms.configuration.RendererConfiguration;
import net.pms.newgui.StatusTab.ConnectionState;
import org.apache.commons.lang3.StringUtils;

public class DummyFrame implements IFrame {

	private ArrayList<String> log;

	public DummyFrame() {
		log = new ArrayList<>();
	}

	@Override
	public void append(String msg) {
		log.add(msg);
	}

	@Override
	public void updateBuffer() {
	}

	@Override
	public void setReadValue(long v, String msg) {
	}

	@Override
	public void setConnectionState(ConnectionState connectionState) {
	}

	@Override
	public void setReloadable(boolean reload) {
	}

	@Override
	public void addEngines() {
	}

	@Override
	public void setStatusLine(String line) {
	}

	@Override
	public void addRenderer(RendererConfiguration renderer) {
	}

	@Override
	public void updateRenderer(RendererConfiguration renderer) {
	}

	@Override
	public void serverReady() {
	}

	@Override
	public void setScanLibraryEnabled(boolean flag) {
	}

	public String getLog() {
		return StringUtils.join(log, "\n");
	}
}
