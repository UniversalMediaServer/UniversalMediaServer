package net.pms.util;

import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;

public class PlayerUtil {
	/**
	 * This class is not meant to be instantiated.
	 */
	private PlayerUtil() { }

	// Returns whether or not the supplied DLNA resource matches the supplied format and format identifier
	private static boolean isType(DLNAResource resource, int matchType, Format.Identifier matchIdentifier) {
		boolean match = false;

		if (resource != null) {
			Format format = resource.getFormat();

			// the int returned by format.getType() is a bitmap, so match by checking the bit is set
			// XXX the old isCompatible implementations (incorrectly) used to match
			// with getType() == matchType
			if ((format != null) && ((format.getType() & matchType) == matchType)) {
				if (matchIdentifier == null) { // match any identifier
					match = true;
				} else { // match the specified format identifier
					Format.Identifier identifier = format.getIdentifier();
					match = identifier.equals(matchIdentifier);
				}
			}
		}

		return match;
	}

	/**
	 * Returns whether or not the supplied DLNA resource is an image file.
	 *
	 * @param resource the DLNA resource
	 * @return true if the DLNA resource is an image file, false otherwise.
	 */
	public static boolean isImage(DLNAResource resource) {
		return isType(resource, Format.IMAGE, null);
	}

	/**
	 * Returns whether or not the supplied DLNA resource is an image file.
	 *
	 * @param resource the DLNA resource
	 * @param identifier the format identifier to match against
	 * @return true if the DLNA resource is an image file with the specified format identifier, false otherwise
	 */
	public static boolean isImage(DLNAResource resource, Format.Identifier identifier) {
		return isType(resource, Format.IMAGE, identifier);
	}

	/**
	 * Returns whether or not the supplied DLNA resource is an audio file.
	 *
	 * @param resource the DLNA resource
	 * @return true if the DLNA resource is an audio file, false otherwise.
	 */
	public static boolean isAudio(DLNAResource resource) {
		return isType(resource, Format.AUDIO, null);
	}

	/**
	 * Returns whether or not the supplied DLNA resource is an audio file.
	 *
	 * @param resource the DLNA resource
	 * @param identifier the format identifier to match against
	 * @return true if the DLNA resource is an audio file with the specified format identifier, false otherwise
	 */
	public static boolean isAudio(DLNAResource resource, Format.Identifier identifier) {
		return isType(resource, Format.AUDIO, identifier);
	}

	/**
	 * Returns whether or not the supplied DLNA resource is a video file.
	 *
	 * @param resource the DLNA resource
	 * @return true if the DLNA resource is a video file, false otherwise.
	 */
	public static boolean isVideo(DLNAResource resource) {
		return isType(resource, Format.VIDEO, null);
	}

	/**
	 * Returns whether or not the supplied DLNA resource is a video file.
	 *
	 * @param resource the DLNA resource
	 * @param identifier the format identifier to match against
	 * @return true if the DLNA resource is a video file with the specified format identifier, false otherwise.
	 */
	public static boolean isVideo(DLNAResource resource, Format.Identifier identifier) {
		return isType(resource, Format.VIDEO, identifier);
	}

	/**
	 * Returns whether or not the supplied DLNA resource is a web audio file.
	 *
	 * @param resource the DLNA resource
	 * @return true if the DLNA resource is a web audio file, false otherwise.
	 */
	public static boolean isWebAudio(DLNAResource resource) {
		return isType(resource, Format.AUDIO, Format.Identifier.WEB);
	}

	/**
	 * Returns whether or not the supplied DLNA resource is a web video file.
	 *
	 * @param resource the DLNA resource
	 * @return true if the DLNA resource is a web video file, false otherwise.
	 */
	public static boolean isWebVideo(DLNAResource resource) {
		return isType(resource, Format.VIDEO, Format.Identifier.WEB);
	}
}
