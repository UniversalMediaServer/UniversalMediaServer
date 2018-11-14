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

import java.io.IOException;
import javax.swing.JComponent;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.util.PlayerUtil;

public class TsMuxeRAudio extends TsMuxeRVideo {
	public static final PlayerId ID = StandardPlayerId.TSMUXER_AUDIO;

	/** The {@link Configuration} key for the tsMuxeR Audio executable type. */
	public static final String KEY_TSMUXER_AUDIO_EXECUTABLE_TYPE = "tsmuxer_audio_executable_type";
	public static final String NAME = "tsMuxeR Audio";

	// Not to be instantiated by anything but PlayerFactory
	TsMuxeRAudio() {
	}

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public PlayerId id() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_TSMUXER_AUDIO_EXECUTABLE_TYPE;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		params.timeend = media.getDurationInSeconds();
		params.waitbeforestart = 2500;
		return super.launchTranscode(dlna, media, params);
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public int purpose() {
		return AUDIO_SIMPLEFILE_PLAYER;
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isVideo(resource, Format.Identifier.AUDIO_AS_VIDEO);
	}
}
