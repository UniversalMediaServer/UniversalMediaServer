/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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

package net.pms.configuration;

import java.io.File;
import net.pms.PMS;
import net.pms.io.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WindowsRegistryProgramPaths implements ProgramPaths {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsRegistryProgramPaths.class);
	private final ProgramPaths defaults;

	WindowsRegistryProgramPaths(ProgramPaths defaults) {
		this.defaults = defaults;
	}

	@Override
	public String getFfmpegPath() {
		return defaults.getFfmpegPath();
	}

	@Override
	public String getFlacPath() {
		return defaults.getFlacPath();
	}

	@Override
	public String getMencoderPath() {
		return defaults.getMencoderPath();
	}

	@Override
	public String getMplayerPath() {
		return defaults.getMplayerPath();
	}

	@Override
	public String getTsmuxerPath() {
		return defaults.getTsmuxerPath();
	}

	@Override
	public String getTsmuxerNewPath() {
		return defaults.getTsmuxerNewPath();
	}

	@Override
	public String getVlcPath() {
		SystemUtils registry = PMS.get().getRegistry();
		if (registry.getVlcPath() != null) {
			String vlc = registry.getVlcPath();
			String version = registry.getVlcVersion();
			if (new File(vlc).exists() && version != null) {
				return vlc;
			}
		}

		return defaults.getVlcPath();
	}

	@Override
	public String getDCRaw() {
		return defaults.getDCRaw();
	}

	@Override
	public String getInterFramePath() {
		return defaults.getInterFramePath();
	}
}
