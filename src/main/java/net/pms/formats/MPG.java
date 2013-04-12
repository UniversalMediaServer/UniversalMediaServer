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
import net.pms.configuration.PmsConfiguration;
import net.pms.encoders.*;

public class MPG extends Format {
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.MPG;
	}

	@Override
	public ArrayList<Class<? extends Player>> getProfiles() {
		PMS r = PMS.get();
		PMS r1 = PMS.get();
		PMS r2 = PMS.get();
		if (configuration.getEnginesAsList(r.getRegistry()) == null || configuration.getEnginesAsList(r1.getRegistry()).isEmpty() || configuration.getEnginesAsList(r2.getRegistry()).contains("none"))
		{
			return null;
		}
		ArrayList<Class<? extends Player>> a = new ArrayList<>();
		PMS r3 = PMS.get();
		for (String engine : configuration.getEnginesAsList(r3.getRegistry())) {
			if (engine.equals(VLCVideo.ID)) {
				a.add(VLCVideo.class);
			} else if (engine.equals(MEncoderVideo.ID)) {
				a.add(MEncoderVideo.class);
			} else if (engine.equals(AviSynthMEncoder.ID) && PMS.get().getRegistry().isAvis()) {
				a.add(AviSynthMEncoder.class);
			} else if (engine.equals(FFMpegVideo.ID)) {
				a.add(FFMpegVideo.class);
			} else if (engine.equals(AviSynthFFmpeg.ID) && PMS.get().getRegistry().isAvis()) {
				a.add(AviSynthFFmpeg.class);
			} else if (engine.equals(TsMuxeRVideo.ID)/* && PMS.get().isWindows()*/) {
				a.add(TsMuxeRVideo.class);
			}
		}
		return a;
	}

	@Override
	public boolean transcodable() {
		return true;
	}

	public MPG() {
		type = VIDEO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getId() {
		return new String[] { "mpg", "mpeg", "mpe", "mod", "tivo", "ty", "tmf",
				"ts", "tp", "m2t", "m2ts", "m2p", "mts", "mp4", "m4v", "avi",
				"wmv", "wm", "vob", "divx", "div", "vdr" , "wtv"};
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
