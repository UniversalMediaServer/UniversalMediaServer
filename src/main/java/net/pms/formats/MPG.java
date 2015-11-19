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

public class MPG extends Format {
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.MPG;
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
	public String[] getSupportedExtensions() {
		return new String[] {
			"avi",
			"div",
			"divx",
			"m2p",
			"m2t",
			"m2ts",
			"m4v",
			"mod",
			"mp4",
			"mpe",
			"mpeg",
			"mpg",
			"mts",
			"tivo",
			"tmf",
			"tp",
			"ts",
			"ty",
			"vdr",
			"vob",
			"wm",
			"wmv",
			"wtv"
		};
	}
}
