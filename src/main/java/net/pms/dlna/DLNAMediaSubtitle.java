/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 * Copyright (C) 2012  I. Sokolov
 *
 * This program is free software; you can redistribute it and/or
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

import com.ibm.icu.text.CharsetMatch;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import net.pms.formats.v2.SubtitleType;
import static net.pms.formats.v2.SubtitleType.UNKNOWN;
import static net.pms.util.Constants.CHARSET_UTF_8;
import net.pms.util.FileUtil;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the subtitle information for media.
 */
public class DLNAMediaSubtitle extends DLNAMediaLang implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaSubtitle.class);
	private SubtitleType type = UNKNOWN;

	private String subtitlesTrackTitleFromMetadata;

	/** The external {@link File}, always in its "absolute" version */
	private File externalFile;
	private String subsCharacterSet;

	private boolean isStreamable = false;
	private File convertedFile;

	/**
	 * Returns whether or not the subtitles are embedded.
	 *
	 * @return {@code true} if the subtitles are embedded, {@code false}
	 *         otherwise.
	 * @since 1.51.0
	 */
	public boolean isEmbedded() {
		return (externalFile == null);
	}

	/**
	 * Returns whether or not the subtitles are external.
	 *
	 * @return True if the subtitles are external file, false otherwise.
	 * @since 1.70.0
	 */
	public boolean isExternal() {
		return !isEmbedded();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("id: ");
		result.append(getId());
		result.append(", type: ");
		result.append(type);

		if (isNotBlank(subtitlesTrackTitleFromMetadata)) {
			result.append(", subtitles track title from metadata: ");
			result.append(subtitlesTrackTitleFromMetadata);
		}

		result.append(", lang: ");
		result.append(getLang());

		if (externalFile != null) {
			result.append(", externalFile: ");
			result.append(externalFile);
			result.append(", external file character set: ");
			result.append(subsCharacterSet);
		}

		if (convertedFile != null) {
			result.append(", convertedFile: ");
			result.append(convertedFile.toString());
		}

		return result.toString();
	}

	/**
	 * @deprecated charset is autodetected for text subtitles after setExternalFile()
	 */
	@Deprecated
	public void checkUnicode() {
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * @return the type
	 */
	public SubtitleType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(SubtitleType type) {
		if (type == null) {
			throw new IllegalArgumentException("Can't set null SubtitleType.");
		}
		this.type = type;
	}

	/**
	 * @deprecated use getSubtitlesTrackTitleFromMetadata()
	 */
	@Deprecated
	public String getFlavor() {
		return getSubtitlesTrackTitleFromMetadata();
	}

	/**
	 * @deprecated use setSubtitlesTrackTitleFromMetadata()
	 */
	@Deprecated
	public void setFlavor(String value) {
		setSubtitlesTrackTitleFromMetadata(value);
	}

	public String getSubtitlesTrackTitleFromMetadata() {
		return subtitlesTrackTitleFromMetadata;
	}

	public void setSubtitlesTrackTitleFromMetadata(String value) {
		this.subtitlesTrackTitleFromMetadata = value;
	}

	/**
	 * @deprecated use {@link #FileUtil.convertFileFromUtf16ToUtf8()} for UTF-16 -> UTF-8 conversion.
	 */
	@Deprecated
	public File getPlayableExternalFile() {
		return getExternalFile();
	}

	/**
	 * @return The absolute external {@link File} or {@code null}.
	 */
	public File getExternalFile() {
		return externalFile;
	}

	/**
	 * @return The file name of the subtitles file if applicable or an empty
	 *         {@link String}.
	 */
	public String getName() {
		return externalFile == null ? "" : externalFile.getName();
	}

	/**
	 * Sets the external subtitles {@link File} and detect and sets its
	 * character set. If the language isn't set/known and it is detected, the
	 * language is also set.
	 *
	 * @param externalFile the external {@link File} to set.
	 */
	public void setExternalFile(File externalFile) throws FileNotFoundException {
		if (externalFile == null) {
			throw new FileNotFoundException("Can't read file: no file supplied");
		}
		this.externalFile = externalFile.getAbsoluteFile();
		if (!FileUtil.getFilePermissions(this.externalFile).isReadable()) {
			throw new FileNotFoundException("Insufficient permission to read " + externalFile.getAbsolutePath());
		}

		setFileSubsCharacterSet();
	}

	/**
	 * Detects and sets the subtitles' character set. If the language isn't
	 * set/known and it is detected, the language is also set.
	 */
	@SuppressWarnings("deprecation")
	private void setFileSubsCharacterSet() {
		if (externalFile != null && !type.isPicture()) {
			try {
				CharsetMatch match = FileUtil.getFileCharsetMatch(externalFile);
				if (match != null) {
					subsCharacterSet = match.getName().toUpperCase(Locale.ROOT);
					// Returned Charset can have additional info like ISO-8859-8-I but
					// FFmpeg video filter knows only ISO-8859-8 so extract the additional "-I".
					if (subsCharacterSet.split("-").length > 3) {
						subsCharacterSet = subsCharacterSet.substring(0, subsCharacterSet.lastIndexOf("-"));
					}

					// Set the detected language if is isn't already set
					if (lang == null || DLNAMediaLang.UND.equals(lang)) {
						String tmpLanguage = match.getLanguage();
						if (isNotBlank(tmpLanguage)) {
							lang = tmpLanguage;
						}
					}

					LOGGER.debug("Set detected charset \"{}\" and language \"{}\" for {}", subsCharacterSet, lang, externalFile);
				} else {
					subsCharacterSet = null;
					LOGGER.debug("No charset detected for {}", externalFile);
				}

			} catch (IOException ex) {
				LOGGER.warn("Exception during external file charset detection: {}", ex.getMessage());
				LOGGER.trace("", ex);
			}
		} else {
			subsCharacterSet = null;
		}
	}

	/**
	 * @deprecated use {@link #setSubCharacterSet(String)}
	 */
	@Deprecated
	public void setExternalFileCharacterSet(String charSet) {
		setSubCharacterSet(charSet);
	}

	public void setSubCharacterSet(String charSet) {
		subsCharacterSet = charSet;
	}

	/**
	 * @deprecated use {@link #getSubCharacterSet()}
	 */
	@Deprecated
	public String getExternalFileCharacterSet() {
		return getSubCharacterSet();
	}

	public String getSubCharacterSet() {
		return subsCharacterSet;
	}

	/**
	 * @return {@code true} if the subtitles are UTF-8 encoded, {@code false}
	 *         otherwise.
	 */
	public boolean isSubsUtf8() {
		return equalsIgnoreCase(subsCharacterSet, CHARSET_UTF_8);
	}

	/**
	 * @return {@code true} if the external subtitles file is UTF-8 encoded,
	 *         {@code false} otherwise.
	 */
	public boolean isExternalFileUtf8() {
		return FileUtil.isCharsetUTF8(subsCharacterSet);
	}

	/**
	 * @return {@code true} if the external subtitles file is UTF-16 encoded,
	 *         {@code false} otherwise.
	 */
	public boolean isExternalFileUtf16() {
		return FileUtil.isCharsetUTF16(subsCharacterSet);
	}

	/**
	 * @return {@code true} if the external subtitles file is UTF-32 encoded,
	 *         {@code false} otherwise.
	 */
	public boolean isExternalFileUtf32() {
		return FileUtil.isCharsetUTF32(subsCharacterSet);
	}

	/**
	 * @return {@code true} if the external subtitles file is UTF-8, UTF-16 or
	 *         UTF-32 encoded, {@code false} otherwise.
	 */
	public boolean isExternalFileUtf() {
		return (isExternalFileUtf8() || isExternalFileUtf16() || isExternalFileUtf32());
	}

	/**
	 * @return {@code true} if this is streamable, {@code false} otherwise.
	 */
	public boolean isStreamable() {
		return isExternal() && isStreamable;
	}

	public void setSubsStreamable(boolean isStreamable) {
		this.isStreamable = isStreamable;
	}

	public void setConvertedFile (File convertedFile) {
		this.convertedFile = convertedFile;
	}

	public File getConvertedFile() {
		return convertedFile;
	}
}
