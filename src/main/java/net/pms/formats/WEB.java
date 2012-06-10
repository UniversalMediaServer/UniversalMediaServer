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
package net.pms.formats;

import java.util.ArrayList;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.encoders.MEncoderWebVideo;
import net.pms.encoders.MPlayerWebAudio;
import net.pms.encoders.MPlayerWebVideoDump;
import net.pms.encoders.Player;
import net.pms.encoders.VideoLanAudioStreaming;
import net.pms.encoders.VideoLanVideoStreaming;

public class WEB extends Format {
	/**
	 * @deprecated Use {@link #isCompatible(DLNAMediaInfo, RendererConfiguration)} instead.
	 * <p>
	 * Returns whether or not a format can be handled by the PS3 natively.
	 * This means the format can be streamed to PS3 instead of having to be
	 * transcoded.
	 * 
	 * @return True if the format can be handled by PS3, false otherwise.
	 */
	@Deprecated
	@Override
	public boolean ps3compatible() {
		return type == IMAGE;
	}

	@Override
	public ArrayList<Class<? extends Player>> getProfiles() {
		ArrayList<Class<? extends Player>> a = new ArrayList<Class<? extends Player>>();
		if (type == AUDIO) {
			PMS r = PMS.get();
			for (String engine : PMS.getConfiguration().getEnginesAsList(r.getRegistry())) {
				if (engine.equals(MPlayerWebAudio.ID)) {
					a.add(MPlayerWebAudio.class);
				} else if (engine.equals(VideoLanAudioStreaming.ID)) {
					a.add(VideoLanAudioStreaming.class);
				}
			}
		} else {
			PMS r = PMS.get();
			for (String engine : PMS.getConfiguration().getEnginesAsList(r.getRegistry())) {
				if (engine.equals(MEncoderWebVideo.ID)) {
					a.add(MEncoderWebVideo.class);
				} else if (engine.equals(VideoLanVideoStreaming.ID)) {
					a.add(VideoLanVideoStreaming.class);
				} else if (engine.equals(MPlayerWebVideoDump.ID)) {
					a.add(MPlayerWebVideoDump.class);
				}
			}
		}
		return a;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getId() {
		return new String[] { "http", "mms", "rtsp", "rtp", "udp", "screen" };
	}

	@Override
	public boolean transcodable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAMediaInfo media, RendererConfiguration renderer) {
		// Emulating ps3compatible()
		return type == IMAGE;
	}
}
