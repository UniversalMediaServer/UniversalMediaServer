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
package net.pms.platform.windows;

/**
 * This {@code enum} represents Windows {@code CSIDL} constants.
 *
 * @author Nadahar
 */
public enum CSIDL {

	/** Shared Music ({@link KnownFolders#FOLDERID_PublicMusic}) */
	CSIDL_COMMON_MUSIC(0x0035),

	/** Shared Pictures ({@link KnownFolders#FOLDERID_PublicPictures}) */
	CSIDL_COMMON_PICTURES(0x0036),

	/** Shared Video ({@link KnownFolders#FOLDERID_PublicVideos}) */
	CSIDL_COMMON_VIDEO(0x0037),

	/** Desktop ({@link KnownFolders#FOLDERID_Desktop}) */
	CSIDL_DESKTOP(0x0000),

	/** My Music ({@link KnownFolders#FOLDERID_Music}) */
	CSIDL_MYMUSIC(0x000d),

	/** My Pictures ({@link KnownFolders#FOLDERID_Pictures}) */
	CSIDL_MYPICTURES(0x0027),

	/** My Videos ({@link KnownFolders#FOLDERID_Videos}) */
	CSIDL_MYVIDEO(0x000e);

	private final int value;

	private CSIDL(int value) {
		this.value = value;
	}

	/**
	 * @return The integer ID value.
	 */
	public int getValue() {
		return value;
	}

}
