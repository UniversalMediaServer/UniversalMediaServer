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

import static net.pms.util.Constants.*;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.util.FileUtil;
import net.pms.util.OpenSubtitle;
import net.pms.util.OpenSubtitle.SubtitleItem;
import net.pms.util.StringUtil.LetterCase;

/**
 * This class represents subtitles from OpenSubtitles.
 *
 * @author Nadahar
 */
public class DLNAMediaOpenSubtitle extends DLNAMediaOnDemandSubtitle {

	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaSubtitle.class);

	/** The maximum number of download attempts */
	public static final int DOWNLOAD_ATTEMPTS = 3;

	/** Whether or not the downloaded subtitles file should be kept */
	protected boolean keepLiveSubtitle;

	/** The downloaded subtitles file */
	protected Path subtitleFile;

	/** The {@link SubtitleItem} describing these subtitles */
	protected SubtitleItem subtitleItem;

	/** Keeps tracks of download attempts */
	protected int downloadAttempts = 0;

	/** Synchronization object preventing concurrent download attempts */
	protected final Object downloadLock = new Object();

	/**
	 * Creates a new instance using the specified {@link SubtitleItem}.
	 *
	 * @param subtitleItem the {@link SubtitleItem} to use.
	 */
	public DLNAMediaOpenSubtitle(SubtitleItem subtitleItem) {
		if (subtitleItem == null) {
			throw new IllegalArgumentException("subtitleItem cannot be null");
		}
		this.subtitleItem = subtitleItem;
		keepLiveSubtitle = PMS.getConfiguration().isLiveSubtitlesKeep();
		if (subtitleItem.getSubtitleType() != null) {
			setType(subtitleItem.getSubtitleType());
		}
		setLang(subtitleItem.getLanguageCode());
	}

	/**
	 * Resolves the subtitles file name and path, and attempts to download the
	 * file if necessary. The subtitles file path is stored in
	 * {@link #subtitleFile}.
	 *
	 * @return {@code true} if the subtitle file is available for use, otherwise
	 *         {@code false}.
	 */
	protected boolean resolveSubtitleFile() {
		if (subtitleItem == null || subtitleItem.getSubDownloadLink() == null) {
			// Should be impossible
			LOGGER.error("OpenSubtitles: Asked to download subtitle without URL - aborting");
			return false;
		}
		synchronized (downloadLock) {
			if (subtitleFile == null) {
				String fileName;
				if (isBlank(subtitleItem.getSubFileName())) {
					// Generate temp name
					fileName = "tempsubtitle" + System.nanoTime();
					keepLiveSubtitle = false;
				} else {
					fileName = FileUtil.getFileNameWithoutExtension(subtitleItem.getSubFileName());
					if (isNotBlank(subtitleItem.getIDSubtitleFile())) {
						fileName += "_" + subtitleItem.getIDSubtitleFile();
					} else {
						fileName += "_" + subtitleItem.getIDSubtitle();
					}
					String extension = FileUtil.getExtension(subtitleItem.getSubFileName(), LetterCase.LOWER, Locale.ROOT);
					if (extension != null) {
						if (subtitleItem.getLanguageCode() != null) {
							fileName += "." + subtitleItem.getLanguageCode() + "." + FileUtil.getExtension(subtitleItem.getSubFileName());
						} else {
							fileName += "." + FileUtil.getExtension(subtitleItem.getSubFileName());
						}
					}
				}
				try {
					subtitleFile = OpenSubtitle.resolveSubtitlesPath(fileName);
				} catch (IOException e) {
					downloadAttempts = Integer.MAX_VALUE;
					LOGGER.error(
						"An error occurred when trying to resolve subtitles path for \"{}\": {}",
						fileName,
						e.getMessage()
					);
					LOGGER.trace("", e);
					return false;
				}
			}
			boolean doDownload = true;
			if (Files.exists(subtitleFile)) {
				try {
					if (Files.size(subtitleFile) == 0) {
						Files.delete(subtitleFile);
						doDownload = true;
					} else {
						doDownload = false;
					}
				} catch (IOException e) {
					downloadAttempts = Integer.MAX_VALUE;
					LOGGER.error(
						"An error occurred when trying to access subtitles file \"{}\": {}",
						subtitleFile,
						e.getMessage()
					);
					LOGGER.trace("", e);
					return false;
				}
			}
			if (doDownload && downloadAttempts < DOWNLOAD_ATTEMPTS) {
				downloadAttempts++;
				try {
					LOGGER.debug(
						"Downloading live subtitles \"{}\" from \"{}\" to file \"{}\"",
						subtitleItem.getSubFileName(),
						subtitleItem.getSubDownloadLink(),
						subtitleFile
					);
					if (OpenSubtitle.fetchSubs(subtitleItem.getSubDownloadLink().toURL(), subtitleFile) == null) {
						LOGGER.error(
							"Failed to login to OpenSubtitles when trying to download \"{}\"",
							subtitleItem.getSubFileName()
						);
						return false;
					}
				} catch (IOException e) {
					LOGGER.error("Failed to download subtitles \"{}\": {}", subtitleItem.getSubFileName(), e.getMessage());
					LOGGER.trace("", e);
					return false;
				}
			} else if (doDownload) {
				LOGGER.error(
					"Didn't try to download subtitles \"{}\" from OpenSubtitles " +
					"since it already unsuccessfully has been attempted {} times",
					subtitleItem.getSubFileName(),
					DOWNLOAD_ATTEMPTS
				);
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean fetch() {
		synchronized (downloadLock) {
			return resolveSubtitleFile();
		}
	}

	@Override
	public File getExternalFile() {
		synchronized (downloadLock) {
			return subtitleFile == null ? null : subtitleFile.toFile();
		}
	}

	@Override
	public String getSubCharacterSet() {
		return subtitleItem.getSubEncoding() != null ? subtitleItem.getSubEncoding() : super.getSubCharacterSet();
	}

	@Override
	public boolean isSubsUtf8() {
		return isExternalFileUtf8();
	}

	@Override
	public boolean isExternalFileUtf8() {
		return equalsIgnoreCase(subtitleItem.getSubEncoding(), CHARSET_UTF_8);
	}

	@Override
	public boolean isExternalFileUtf16() {
		return
			equalsIgnoreCase(subtitleItem.getSubEncoding(), CHARSET_UTF_16BE) ||
			equalsIgnoreCase(subtitleItem.getSubEncoding(), CHARSET_UTF_16LE);
	}

	@Override
	public boolean isExternalFileUtf32() {
		return
			equalsIgnoreCase(subtitleItem.getSubEncoding(), CHARSET_UTF_32BE) ||
			equalsIgnoreCase(subtitleItem.getSubEncoding(), CHARSET_UTF_32LE);
	}

	/**
	 * @return {@code true} if these OpenSubtitles subtitles has been downloaded
	 *         to disk, {@code false} otherwise.
	 */
	public boolean isDownloaded() {
		synchronized (downloadLock) {
			return subtitleFile != null && Files.exists(subtitleFile);
		}
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}

	@Override
	public String getName() {
		synchronized (downloadLock) {
			if (subtitleItem != null) {
				return subtitleItem.getSubFileName();
			}
			if (subtitleFile != null) {
				return subtitleFile.toString();
			}
			return super.getName();
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(getClass().getSimpleName());
		result.append(" [Type: ").append(getType());
		if (isNotBlank(getSubtitlesTrackTitleFromMetadata())) {
			result.append(", Subtitles Track Title From Metadata: ");
			result.append(getSubtitlesTrackTitleFromMetadata());
		}
		result.append(", Language: ").append(getLang());

		synchronized (downloadLock) {
			if (subtitleFile != null) {
				result.append(", Downloaded File: \"").append(subtitleFile).append("\" (");
				if (keepLiveSubtitle) {
					result.append("keep");
				} else {
					result.append("delete after use");
				}
				result.append(")");
			} else {
				result.append(", Filename: \"").append(subtitleItem.getSubFileName()).append("\" (not downloaded)");
			}
		}
		if (isNotBlank(subtitleItem.getSubEncoding())) {
			result.append(", Character Set: ").append(subtitleItem.getSubEncoding());
		}

		if (getConvertedFile() != null) {
			result.append(", Converted File: ");
			result.append(getConvertedFile().toString());
		}
		result.append("]");

		return result.toString();
	}

	/**
	 * Deletes the live subtitles file if it exists and isn't configured to be
	 * kept.
	 */
	public void deleteLiveSubtitlesFile() {
		synchronized (downloadLock) {
			if (subtitleFile != null && !keepLiveSubtitle) {
				if (Files.exists(subtitleFile)) {
					try {
						LOGGER.debug("Deleting temporary live subtitles file \"{}\"", subtitleFile);
						Files.delete(subtitleFile);
					} catch (IOException e) {
						LOGGER.error(
							"Could not delete temporary live subtitles file \"{}\", trying to delete it at program exit: {}",
							subtitleFile,
							e.getMessage()
						);
						LOGGER.trace("", e);
						subtitleFile.toFile().deleteOnExit();
					}
				}
				subtitleFile = null;
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		deleteLiveSubtitlesFile();
		super.finalize();
	}
}
