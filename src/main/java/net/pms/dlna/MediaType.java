/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.dlna;


/**
 * Defines the media types.
 *
 * @author Nadahar
 */
public enum MediaType {

	/**
	 * <code>UNKNOWN</code> for when the media type is unknown.
	 */
	UNKNOWN(0),

	/**
	 * <code>AUDIO</code> for when the media type is audio.
	 */
	AUDIO(1),

	/**
	 * <code>IMAGE</code> for when the media type is image.
	 */
	IMAGE(2),

	/**
	 * <code>VIDEO</code> for when the media type is video.
	 */
	VIDEO(3);

	private int value;
	private MediaType(int value) {
		this.value = value;
	}

	/**
	 * @param value {@link MediaType} integer value.
	 * @return The {@link MediaType} corresponding to the integer value or
	 *         {@code null} if invalid.
	 */
	public static MediaType typeOf(int value)
	{
		for (MediaType mediaType : MediaType.values())
		{
			if (mediaType.value == value)
			{
				return mediaType;
			}
		}
		return null;
	}

	/**
	 * @return The integer value for this {@link MediaType}.
	 */
	public int getValue() {
		return value;
	}

	@Override
	public String toString()
	{
		return "Media type " + this.name() + " ("+ value + ")";
	}
}
