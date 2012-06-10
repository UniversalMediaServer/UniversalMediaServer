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
import net.pms.encoders.FFMpegAudio;
import net.pms.encoders.MPlayerAudio;
import net.pms.encoders.Player;

public class WAV extends Format {
	public WAV() {
		type = AUDIO;
	}
	
	@Override
	public boolean transcodable() {
		return true;
	}

	@Override
	public ArrayList<Class<? extends Player>> getProfiles() {
		ArrayList<Class<? extends Player>> a = new ArrayList<Class<? extends Player>>();
		PMS r = PMS.get();
		for (String engine : PMS.getConfiguration().getEnginesAsList(r.getRegistry())) {
			if (engine.equals(MPlayerAudio.ID)) {
				a.add(MPlayerAudio.class);
			} else if (engine.equals(FFMpegAudio.ID)) {
				a.add(FFMpegAudio.class);
			}
		}
		return a;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getId() {
		return new String[] { "wav" };
	}

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
		return true;
	}
}
