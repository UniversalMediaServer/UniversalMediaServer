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
package net.pms.formats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.formats.audio.*;
import net.pms.formats.image.*;
import net.pms.formats.subtitle.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class matches and instantiates formats.
 */
public final class FormatFactory {
	/** The {@link Logger} used for all logging for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatFactory.class);

	/**
	 * Initial list of known formats.
	 */
	static final Format[] FORMATS = new Format[] {
		new AACP(),
		new AC3(),
		new ADPCM(),
		new ADTS(),
		new AIFF(),
		new ASS(),
		new ATRAC(),
		new AU(),
		new BMP(),
		new DFF(),
		new DSF(),
		new DTS(),
		new DVRMS(),
		new EAC3(),
		new FLAC(),
		new GIF(),
		new ICNS(),
		new ICO(),
		new IDX(),
		new IFF(),
		new ISO(),
		new ISOVOB(),
		new JPG(),
		new M4A(),
		new MicroDVD(),
		new MKA(),
		new MKV(),
		new MLP(),
		new MonkeysAudio(),
		new MP3(),
		new MPC(),
		new MPG(),
		new MPGAudio(),
		new OGA(),
		new OGG(),
		new PCX(),
		new PICT(),
		new PLAYLIST(),
		new PNG(),
		new PNM(),
		new PSD(),
		new RA(),
		new RAW(),
		new RGBE(),
		new SAMI(),
		new SGI(),
		new SHN(),
		new SubRip(),
		new SUP(),
		new TGA(),
		new THREEGA(),
		new THREEG2A(),
		new TIFF(),
		new TrueHD(),
		new TTA(),
		new TXT(),
		new WAV(),
		new WavPack(),
		new WBMP(),
		new WEB(),
		new WEBP(),
		new WebVTT(),
		new WMA(),
	};

	private static final ReentrantReadWriteLock FORMATS_LOCK = new ReentrantReadWriteLock();
	/**
	 * The list of registered formats.
	 */
	private static final List<Format> REGISTERED_FORMATS = new ArrayList<>(Arrays.asList(FORMATS));

	/**
	 * This class should not be instantiated.
	 */
	private FormatFactory() {}

	/**
	 * Match a given filename to all known formats and return a fresh instance
	 * of that format. Matching is done by the file extension (e.g. ".gif") or
	 * protocol (e.g. "http://") of the filename. Will return <code>null</code>
	 * if no match can be made.
	 *
	 * @param filename The filename to match.
	 * @return The format.
	 * @see Format#match(String)
	 * @since 1.90.0
	 */
	public static Format getAssociatedFormat(final String filename) {
		FORMATS_LOCK.readLock().lock();
		try {
			for (Format format : REGISTERED_FORMATS) {
				if (format.match(filename)) {
					LOGGER.trace("Matched format {} to \"{}\"", format, filename);

					// Return a fresh instance
					return format.duplicate();
				}
			}
		} finally {
			FORMATS_LOCK.readLock().unlock();
		}

		LOGGER.trace("Could not match any format to \"{}\"", filename);
		return null;
	}

	public static Format getFormat(Class<? extends Format> clazz) {
		if (clazz == null) {
			return null;
		}
		FORMATS_LOCK.readLock().lock();
		try {
			for (Format format : REGISTERED_FORMATS) {
				if (format.getClass().equals(clazz)) {
					return format.duplicate();
				}
			}
		} finally {
			FORMATS_LOCK.readLock().unlock();
		}
		return null;
	}

	/**
	 * Returns the list of supported formats.
	 *
	 * @return The list of supported formats.
	 * @since 1.90.0
	 */
	public static List<Format> getSupportedFormats() {
		FORMATS_LOCK.readLock().lock();
		try {
			return new ArrayList<>(REGISTERED_FORMATS);
		} finally {
			FORMATS_LOCK.readLock().unlock();
		}
	}

	/**
	 * Adds a {@link Format} to the registered formats.
	 *
	 * @param format the {@link Format} to register.
	 */
	public static boolean addFormat(Format format) {
		if (format == null) {
			throw new NullPointerException("format cannot be null");
		}
		FORMATS_LOCK.writeLock().lock();
		try {
			return REGISTERED_FORMATS.add(format);
		} finally {
			FORMATS_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Removes a {@link Format} from the registered formats.
	 *
	 * @param format the {@link Format} to remove.
	 */
	public static boolean removeFormat(Format format) {
		if (format == null) {
			throw new NullPointerException("format cannot be null");
		}
		FORMATS_LOCK.writeLock().lock();
		try {
			return REGISTERED_FORMATS.remove(format);
		} finally {
			FORMATS_LOCK.writeLock().unlock();
		}
	}
}
