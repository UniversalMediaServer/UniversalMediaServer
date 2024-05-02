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
package net.pms.media.subtitle;

import com.ibm.icu.text.CharsetMatch;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import net.pms.formats.v2.SubtitleType;
import net.pms.media.MediaLang;
import net.pms.util.Constants;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the subtitle information for media.
 */
public class MediaSubtitle extends MediaLang implements Cloneable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaSubtitle.class);

	private SubtitleType type = SubtitleType.UNKNOWN;
	private Integer streamOrder;
	private Long optionalId;
	private boolean defaultFlag;
	private boolean forcedFlag;
	private String title;

	/** The external {@link File}, always in its "absolute" version */
	private File externalFile;
	private String subsCharacterSet;
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

	/**
	 * @return the container stream index.
	 */
	public Integer getStreamOrder() {
		return streamOrder;
	}

	/**
	 * @param streamIndex the container stream index to set
	 */
	public void setStreamOrder(Integer streamIndex) {
		this.streamOrder = streamIndex;
	}

	/**
	 * Returns the optional id for this subtitles stream.
	 *
	 * @return The optional id.
	 */
	public Long getOptionalId() {
		return optionalId;
	}

	/**
	 * Sets an optional id for this subtitles stream.
	 *
	 * @param uid the optional id to set.
	 */
	public void setOptionalId(Long optionalId) {
		this.optionalId = optionalId;
	}

	/**
	 * Returns whether or not the subtitles are flagged Default.
	 *
	 * @return True if the subtitles are flagged Default, false otherwise.
	 */
	public boolean isDefault() {
		return this.defaultFlag;
	}

	/**
	 * Sets the parsed subtitles Default flag.
	 *
	 * @param value the subtitles Default flag.
	 */
	public void setDefault(boolean value) {
		this.defaultFlag = value;
	}

	/**
	 * Returns whether or not the subtitles are flagged Forced.
	 *
	 * @return True if the subtitles are flagged Forced, false otherwise.
	 */
	public boolean isForced() {
		return this.forcedFlag;
	}

	/**
	 * Sets the parsed subtitles Forced flag.
	 *
	 * @param value the subtitles Forced flag.
	 */
	public void setForced(boolean value) {
		this.forcedFlag = value;
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
	 * @return The subtitles title if parsed.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the parsed subtitles title.
	 *
	 * @param value the subtitles title.
	 */
	public void setTitle(String value) {
		this.title = value;
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
	 * @throws FileNotFoundException If {@code externalFile} can't be read or is
	 *             {@code null}.
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
	 * Sets the external subtitles {@link File} without any checks or automatic
	 * detection. Primarily to be used when retrieving an instance from the
	 * database.
	 *
	 * @param externalFile the external {@link File} to set.
	 */
	public void setExternalFileOnly(File externalFile) {
		this.externalFile = externalFile;
	}

	/**
	 * Detects and sets the subtitles' character set. If the language isn't
	 * set/known and it is detected, the language is also set.
	 */
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
					if (lang == null || MediaLang.UND.equals(lang)) {
						String tmpLanguage = match.getLanguage();
						if (StringUtils.isNotBlank(tmpLanguage)) {
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
	 * Sets the subtitles character set.
	 *
	 * @param charSet the subtitles character set.
	 */
	public void setSubCharacterSet(String charSet) {
		subsCharacterSet = charSet;
	}

	/**
	 * @return The subtitles character set.
	 */
	public String getSubCharacterSet() {
		return subsCharacterSet;
	}

	/**
	 * @return {@code true} if the subtitles are UTF-8 encoded, {@code false}
	 *         otherwise.
	 */
	public boolean isSubsUtf8() {
		return StringUtils.equalsIgnoreCase(subsCharacterSet, Constants.CHARSET_UTF_8);
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
	 * Sets the converted {@link File}.
	 *
	 * @param convertedFile the converted {@link File}.
	 */
	public void setConvertedFile(File convertedFile) {
		this.convertedFile = convertedFile;
	}

	/**
	 * @return The converted {@link File} or {@code null}.
	 */
	public File getConvertedFile() {
		return convertedFile;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (isEmbedded()) {
			result.append("Id: ").append(getId()).append(", Embedded");
			if (forcedFlag) {
				result.append(", Default");
			}
			if (forcedFlag) {
				result.append(", Forced");
			}
		} else {
			result.append("External");
		}
		if (StringUtils.isNotBlank(title)) {
			result.append(", title: ");
			result.append(title);
		}
		result.append(", lang: ");
		result.append(getLang());
		result.append(", type: ").append(type);
		if (getStreamOrder() != null) {
			result.append(", Stream Order: ").append(getStreamOrder());
		}
		if (isExternal()) {
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

}
