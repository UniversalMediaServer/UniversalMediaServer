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

import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;

public class PLAYLIST extends Format {
	public static final String[] PLAYLIST_EXTENSIONS = new String[] {
		"pls",
		"m3u",
		"m3u8",
		"cue",
		"ups"
	};

	public PLAYLIST() {
		type = PLAYLIST;
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.PLAYLIST;
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
		// FIXME: untested, but ps3 reportedly supports m3u.
		// See http://www.ps3mediaserver.org/forum/viewtopic.php?f=2&t=2102
		return getMatchedExtension() != null && getMatchedExtension().startsWith("m3u");
	}

	@Override
	public boolean transcodable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAMediaInfo media, RendererConfiguration renderer) {
		// TODO: manage via renderer conf setting
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return PLAYLIST_EXTENSIONS;
	}
}
