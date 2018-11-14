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
package net.pms.encoders;

import net.pms.dlna.DLNAResource;
import net.pms.util.PlayerUtil;

public class VLCWebVideo extends VLCVideo {
	public static final PlayerId ID = StandardPlayerId.VLC_WEB_VIDEO;

	/** The {@link Configuration} key for the VLC Web executable type. */
	public static final String KEY_VLC_WEB_EXECUTABLE_TYPE = "vlc_web_executable_type";
	public static final String NAME = "VLC Web Video";

	// Not to be instantiated by anything but PlayerFactory
	VLCWebVideo() {
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_PLAYER;
	}

	@Override
	public PlayerId id() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_VLC_WEB_EXECUTABLE_TYPE;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isWebVideo(resource);
	}
}
