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
package net.pms.external.musicbrainz.coverart;

import net.pms.PMS;
import net.pms.util.CoverSupplier;
import org.jaudiotagger.tag.Tag;

/**
 * This class is the superclass of all cover utility implementations.
 * Cover utilities are responsible for getting media covers based
 * on information given by the caller.
 *
 * @author Nadahar
 */

public abstract class CoverUtil {

	private static final Object INSTANCE_LOCK = new Object();
	private static CoverUtil instance = null;

	/**
	 * Do not instantiate this class, use {@link #get()}
	 */
	protected CoverUtil() {
	}

	/**
	 * Factory method that gets an instance of correct type according to
	 * configuration, or <code>null</code> if no cover utility is configured.
	 *
	 * @return The {@link CoverUtil} instance.
	 */

	public static CoverUtil get() {
		CoverSupplier supplier = PMS.getConfiguration().getAudioThumbnailMethod();
		synchronized (INSTANCE_LOCK) {
			if (supplier.toInt() == CoverSupplier.COVER_ART_ARCHIVE_INT) {
				if (!(instance instanceof CoverArtArchiveUtil)) {
					instance = new CoverArtArchiveUtil();
				}
			} else {
				instance = null;
			}
			return instance;
		}
	}

	/**
	 * Gets a thumbnail from the configured cover utility based on a {@link Tag}
	 * @param tag the {@link tag} to use while searching for a cover
	 * @return The thumbnail or <code>null</code> if none was found
	 */
	public final byte[] getThumbnail(Tag tag) {
		boolean externalNetwork = PMS.getConfiguration().getExternalNetwork();
		return doGetThumbnail(tag, externalNetwork);
	}

	protected abstract byte[] doGetThumbnail(Tag tag, boolean externalNetwork);

}
