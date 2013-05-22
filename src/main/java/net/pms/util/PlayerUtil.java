package net.pms.util;

import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;

public class PlayerUtil {
	/**
	 * Returns whether or not the supplied DLNA resource matches the supplied format and format identifier.
	 *
	 * @param resource the DLNA resource
	 * @param matchType the {@link Format} type to match against
	 * @param matchIdentifier the {@link Format.Identifier} to match against
	 * @return true if the format and identifier match, false otherwise
	 */
	public static boolean isType(DLNAResource resource, int matchType, Format.Identifier matchIdentifier) {
		return isType(resource, matchType, matchIdentifier, true);
	}

	/**
	 * Returns a specified value if the supplied DLNA resource matches the supplied format and format identifier.
	 *
	 * @param resource the DLNA resource
	 * @param matchType the {@link Format} type to match against
	 * @param matchIdentifier the {@link Format.Identifier} to match against
	 * @param ifMatch the value to return
	 * @return ifMatch if the format and identifier match, !ifMatch otherwise
	 */
	public static boolean isType(DLNAResource resource, int matchType, Format.Identifier matchIdentifier, boolean ifMatch) {
		boolean match = false;

		if (resource != null) {
			Format format = resource.getFormat();

			// the int returned by format.getType() is a bitmap, so match by checking the bit is set
			// XXX the old isCompatible implementations (incorrectly) used to match
			// with getType() == matchType
			if ((format != null) && ((format.getType() & matchType) == matchType)) {
				Format.Identifier identifier = format.getIdentifier();
				match = identifier.equals(matchIdentifier) ? ifMatch : !ifMatch;
			}
		}

		return match;
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

	/**
	 * Returns whether or not the supplied DLNA resource is a web audio file.
	 *
	 * @param resource the DLNA resource
	 * @return true if the DLNA resource is a web audio file, false otherwise.
	 */
	public static boolean isWebAudio(DLNAResource resource) {
		return isType(resource, Format.AUDIO, Format.Identifier.WEB);
	}
}
