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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp;

/**
 * This state variable indicates the storage medium of the resource specified by
 * AVTransportURI.
 */
public enum StorageMediumValue {

	/**
	 * Unknown medium
	 */
	UNKNOWN("MINI-DV"),
	/**
	 * Digital Video Tape medium
	 */
	DV("DV"),
	/**
	 * Mini Digital Video Tape medium
	 */
	MINI_DV("MINI-DV"),
	/**
	 * VHS Tape medium
	 */
	VHS("VHS"),
	/**
	 * W-VHS Tape medium
	 */
	W_VHS("W-VHS"),
	/**
	 * Super VHS Tape medium
	 */
	S_VHS("S-VHS"),
	/**
	 * Digital VHS Tape medium
	 */
	D_VHS("D-VHS"),
	/**
	 * Compact VHS medium
	 */
	VHSC("VHSC"),
	/**
	 * 8 mm Video Tape medium
	 */
	VIDEO8("VIDEO8"),
	/**
	 * High resolution 8 mm Video Tape medium
	 */
	HI8("HI8"),
	/**
	 * Compact Disc-Read Only Memory medium
	 */
	CD_ROM("CD-ROM"),
	/**
	 * Compact Disc-Digital Audio medium
	 */
	CD_DA("CD-DA"),
	/**
	 * Compact Disc-Recordable medium
	 */
	CD_R("CD-R"),
	/**
	 * Compact Disc-Rewritable medium
	 */
	CD_RW("CD-RW"),
	/**
	 * Video Compact Disc medium
	 */
	VIDEO_CD("VIDEO-CD"),
	/**
	 * Super Audio Compact Disc medium
	 */
	SACD("SACD"),
	/**
	 * Mini Disc Audio medium
	 */
	MD_AUDIO("M-AUDIO"),
	/**
	 * Mini Disc Picture medium
	 */
	MD_PICTURE("MD-PICTURE"),
	/**
	 * DVD Read Only medium
	 */
	DVD_ROM("DVD-ROM"),
	/**
	 * DVD Video medium
	 */
	DVD_VIDEO("DVD-VIDEO"),
	/**
	 * DVD Recordable medium
	 *
	 * @since AVTransport v2
	 */
	DVD_PLUS_R("DVD+R"),
	/**
	 * DVD Recordable medium
	 */
	DVD_MINUS_R("DVD-R"),
	/**
	 * DVD Rewritable medium
	 */
	DVD_PLUS_RW("DVD+RW"),
	/**
	 * DVD Rewritable medium
	 */
	DVD_MINUS_RW("DVD-RW"),
	/**
	 * DVD RAM medium
	 */
	DVD_RAM("DVD-RAM"),
	/**
	 * DVD Audio medium
	 */
	DVD_AUDIO("DVD-AUDIO"),
	/**
	 * Digital Audio Tape medium
	 */
	DAT("DAT"),
	/**
	 * Laser Disk medium
	 */
	LD("LD"),
	/**
	 * Hard Disk Drive medium
	 */
	HDD("HDD"),
	/**
	 * Micro MV Tape medium
	 */
	MICRO_MV("MICRO_MV"),
	/**
	 * Network Interface medium
	 */
	NETWORK("NETWORK"),
	/**
	 * No medium present
	 */
	NONE("NONE"),
	/**
	 * Medium type discovery is not implemented
	 */
	NOT_IMPLEMENTED("NOT_IMPLEMENTED"),
	/**
	 * SD (Secure Digital) Memory Card medium
	 *
	 * @since AVTransport v2
	 */
	SD("SD"),
	/**
	 * PC Card medium
	 *
	 * @since AVTransport v2
	 */
	PC_CARD("PC-CARD"),
	/**
	 * MultimediaCard medium
	 *
	 * @since AVTransport v2
	 */
	MMC("MMC"),
	/**
	 * Compact Flash medium
	 *
	 * @since AVTransport v2
	 */
	CF("CF"),
	/**
	 * Blu-ray Disc medium
	 *
	 * @since AVTransport v2
	 */
	BD("BD"),
	/**
	 * Memory Stick medium
	 *
	 * @since AVTransport v2
	 */
	MS("MS"),
	/**
	 * HD DVD medium
	 *
	 * @since AVTransport v2
	 */
	HD_DVD("HD_DVD");

	private final String value;

	StorageMediumValue(String value) {
		this.value = value;
	}

	public static StorageMediumValue valueOrNullOf(String s) {
		for (StorageMediumValue value : values()) {
			if (value.toString().equals(s)) {
				return value;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return value;
	}

}
