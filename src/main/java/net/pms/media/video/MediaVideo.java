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
package net.pms.media.video;

/**
 * This class keeps track of the video properties of media.
 */
public class MediaVideo {

	/**
	 * Used by FFmpeg for 3D video format naming
	 */
	public enum Mode3D {
		ML,
		MR,
		SBSL,
		SBSR,
		SBS2L,
		SBS2R,
		ABL,
		ABR,
		AB2L,
		AB2R,
		ARCG,
		ARCH,
		ARCC,
		ARCD,
		AGMG,
		AGMH,
		AGMC,
		AGMD,
		AYBG,
		AYBH,
		AYBC,
		AYBD
	}

}
