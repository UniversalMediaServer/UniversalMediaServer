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

	private File externalFile;
	private String subsCharacterSet;

	private String liveSubURL;
	private String liveSubFile;
	private boolean isStreamable = false;
	private File convertedFile;

	/**
	 * Returns whether or not the subtitles are embedded.
	 *
	 * @return True if the subtitles are embedded, false otherwise.
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
			result.append(externalFile.toString());
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
	public File getPlayableExternalFile() {
		return getExternalFile();
	}

	/**
	 * @return the externalFile
	 */
	public File getExternalFile() {
		return externalFile;
	}

	/**
	 * Set external subs file, detect its Character Set and Language. When the {@code forcedLang} is not {@code null}, 
	 * based on the language tag in the file name e.g {@code subsname.en.srt}, than it has priority over the detected language.
	 * 
	 * @param externalFile the externalFile to set
	 * @param forcedLang language forced by file name language tag
	 */
	public void setExternalFile(File externalFile, String forcedLang) throws FileNotFoundException {
		if (externalFile == null) {
			throw new FileNotFoundException("Can't read file: no file supplied");
		} else if (!FileUtil.getFilePermissions(externalFile).isReadable()) {
			throw new FileNotFoundException("Insufficient permission to read " + externalFile.getAbsolutePath());
		}

		this.externalFile = externalFile;
		setFileSubsCharacterSet(forcedLang);
	}

	/**
	 * Detects and set Character Set and language of the subs file. When the {@code forcedLang} is not {@code null}
	 * than it as priority over the detected language.
	 * 
	 * @param forcedLang forced language
	 */
	private void setFileSubsCharacterSet(String forcedLang) {
		if (type.isPicture()) {
			subsCharacterSet = null;
		} else {
			try {
				CharsetMatch match = FileUtil.getFileCharsetMatch(externalFile);
				if (match != null) {
					subsCharacterSet = match.getName().toUpperCase(Locale.ROOT);
					// returned Charset can have additional info like ISO-8859-8-I but
					// FFmpeg video filter knows only ISO-8859-8 so extract the additional "-I".
					if (subsCharacterSet.split("-").length > 3) {
						subsCharacterSet = subsCharacterSet.substring(0, subsCharacterSet.lastIndexOf("-"));
					}

					if (forcedLang == null) { // set the detected language when the language is not specified in the filename
						lang = match.getLanguage();
					}

					LOGGER.debug("Set detected charset \"{}\" and language \"{}\" for {}", subsCharacterSet, lang, externalFile.getAbsolutePath());
				} else {
					subsCharacterSet = null;
					LOGGER.debug("No charset detected for {}", externalFile.getAbsolutePath());
				}

			} catch (IOException ex) {
				subsCharacterSet = null;
				LOGGER.warn("Exception during external file charset detection: ", ex.getMessage());
			}
		}
	}

	/**
	 * @deprecated use {@link #setSubCharacterSet(String)}
	 */
	public void setExternalFileCharacterSet(String charSet) {
		setSubCharacterSet(charSet);
	}

	public void setSubCharacterSet(String charSet) {
		subsCharacterSet = charSet;
	}

	/**
	 * @deprecated use {@link #getSubCharacterSet()}
	 */
	public String getExternalFileCharacterSet() {
		return getSubCharacterSet();
	}

	public String getSubCharacterSet() {
		return subsCharacterSet;
	}

	/**
	 * @return true if subtitles is UTF-8 encoded, false otherwise.
	 */
	public boolean isSubsUtf8() {
		return equalsIgnoreCase(subsCharacterSet, CHARSET_UTF_8);
	}

	/**
	 * @return true if external subtitles file is UTF-8 encoded, false otherwise.
	 */
	public boolean isExternalFileUtf8() {
		return FileUtil.isCharsetUTF8(subsCharacterSet);
	}

	/**
	 * @return true if external subtitles file is UTF-16 encoded, false otherwise.
	 */
	public boolean isExternalFileUtf16() {
		return FileUtil.isCharsetUTF16(subsCharacterSet);
	}

	/**
	 * @return true if external subtitles file is UTF-32 encoded, false otherwise.
	 */
	public boolean isExternalFileUtf32() {
		return FileUtil.isCharsetUTF32(subsCharacterSet);
	}

	/**
	 * @return true if external subtitles file is UTF-8, UTF-16 or UTF-32 encoded, false otherwise.
	 */
	public boolean isExternalFileUtf() {
		return (isExternalFileUtf8() || isExternalFileUtf16() || isExternalFileUtf32());
	}

	public void setLiveSub(String url) {
		setLiveSub(url, null);
	}

	public void setLiveSub(String url, String file) {
		liveSubURL = url;
		liveSubFile = file;
	}

	public String getLiveSubURL() {
		return liveSubURL;
	}

	public String getLiveSubFile() {
		return liveSubFile;
	}

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
