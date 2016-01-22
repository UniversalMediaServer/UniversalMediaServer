/*
 * Universal Media Server, for streaming any medias to DLNA
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
 * Defines the media types used by UMS
 * <ul>
 * <li>Unknown</li>
 * <li>Audio</li>
 * <li>Image</li>
 * <li>Video</li>
 * </ul>
 * The {@link MediaType} class is final and cannot be sub-classed.</p>
 */
public class MediaType {

	public static final int UNKNOWN_INT = 0;
	public static final int AUDIO_INT = 1;
	public static final int IMAGE_INT = 2;
	public static final int VIDEO_INT = 3;

	public static final Integer UNKNOWN_INTEGER = UNKNOWN_INT;
	public static final Integer AUDIO_INTEGER = AUDIO_INT;
	public static final Integer IMAGE_INTEGER = IMAGE_INT;
	public static final Integer VIDEO_INTEGER = VIDEO_INT;

	/**
	 * <code>UNKNOWN</code> for when the media type is unknown.
	 */
	public static final MediaType UNKNOWN = new MediaType(UNKNOWN_INT, "Unknown");

	/**
	 * <code>AUDIO</code> for when the media type is audio.
	 */
	public static final MediaType AUDIO = new MediaType(AUDIO_INT, "Audio");

	/**
	 * <code>IMAGE</code> for when the media type is image.
	 */
	public static final MediaType IMAGE = new MediaType(IMAGE_INT, "Image");

	/**
	 * <code>VIDEO</code> for when the media type is video.
	 */
	public static final MediaType VIDEO = new MediaType(VIDEO_INT, "Video");

	public final int MediaTypeInt;
	public final String MediaTypeStr;

	/**
	 * Instantiate a {@link MediaType} object.
	 */
	private MediaType(int MediaTypeInt, String MediaTypeStr) {
		this.MediaTypeInt = MediaTypeInt;
		this.MediaTypeStr = MediaTypeStr;
	}

	/**
	 * Returns the string representation of this {@link MediaType}.
	 */
	@Override
	public String toString() {
		return MediaTypeStr;
	}

	/**
	 * Returns the integer representation of this {@link MediaType}.
	 */
	public int toInt() {
		return MediaTypeInt;
	}

	/**
	 * Converts a {@link MediaType} to an {@link Integer} object.
	 *
	 * @return This {@link MediaType}'s {@link Integer} mapping.
	 */
	public Integer toInteger() {
		switch (MediaTypeInt) {
			case UNKNOWN_INT:
				return UNKNOWN_INTEGER;
			case AUDIO_INT:
				return AUDIO_INTEGER;
			case IMAGE_INT:
				return IMAGE_INTEGER;
			case VIDEO_INT:
				return VIDEO_INTEGER;
			default:
				throw new IllegalStateException("MediaType " + MediaTypeStr + ", " + MediaTypeInt + " is unknown.");
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a {@link MediaType}. If
	 * the conversion fails, this method returns {@link #UNKNOWN}.
	 */
	public static MediaType toMediaType(String sArg) {
		return toMediaType(sArg, MediaType.UNKNOWN);
	}

	/**
	 * Converts the integer passed as argument to a {@link MediaType}. If the
	 * conversion fails, this method returns {@link #UNKNOWN}.
	 */
	public static MediaType toMediaType(int val) {
		return toMediaType(val, MediaType.UNKNOWN);
	}

	/**
	 * Converts the integer passed as argument to a {@link MediaType}. If the
	 * conversion fails, this method returns the specified default.
	 */
	public static MediaType toMediaType(int val, MediaType defaultMediaType) {
		switch (val) {
			case UNKNOWN_INT:
				return UNKNOWN;
			case AUDIO_INT:
				return AUDIO;
			case IMAGE_INT:
				return IMAGE;
			case VIDEO_INT:
				return VIDEO;
			default:
				return defaultMediaType;
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a {@link MediaType}. If
	 * the conversion fails, this method returns the specified default.
	 */
	public static MediaType toMediaType(String sArg, MediaType defaultMediaType) {
		if (sArg == null) {
			return defaultMediaType;
		}

		sArg = sArg.toLowerCase();
		switch (sArg.toLowerCase()) {
			case "unknown":
				return MediaType.UNKNOWN;
			case "audio":
				return MediaType.AUDIO;
			case "image":
				return MediaType.IMAGE;
			case "video":
				return MediaType.VIDEO;
			default:
				return defaultMediaType;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + MediaTypeInt;
		result = prime * result + ((MediaTypeStr == null) ? 0 : MediaTypeStr.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MediaType)) {
			return false;
		}
		MediaType other = (MediaType) obj;
		if (MediaTypeInt != other.MediaTypeInt) {
			return false;
		}
		if (MediaTypeStr == null) {
			if (other.MediaTypeStr != null) {
				return false;
			}
		} else if (!MediaTypeStr.equals(other.MediaTypeStr)) {
			return false;
		}
		return true;
	}
}
