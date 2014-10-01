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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import net.pms.formats.v2.SubtitleType;
import static net.pms.formats.v2.SubtitleType.*;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the subtitle information for media.
 */
public class DLNAMediaSubtitle extends DLNAMediaLang implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaSubtitle.class);
	private SubtitleType type = UNKNOWN;

	/*
	 * This tells us whether the track is forced or not
	 */
	private String flavor;

	private File externalFile;
	private String externalFileCharacterSet;

	private String liveSubURL;
	private String liveSubFile;
	private boolean isStreamable = false;

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
		result.append(", flavor: ");
		result.append(flavor);
		result.append(", lang: ");
		result.append(getLang());

		if (externalFile != null) {
			result.append(", externalFile: ");
			result.append(externalFile.toString());
		}

		result.append(", externalFileCharacterSet: ");
		result.append(externalFileCharacterSet);

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
	 * @return the flavor
	 */
	public String getFlavor() {
		return flavor;
	}

	/**
	 * @param flavor the flavor to set
	 */
	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	/**
	 * @deprecated use FileUtil.convertFileFromUtf16ToUtf8() for UTF-16 -> UTF-8 conversion.
	 */
	@Deprecated
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
	 * @param externalFile the externalFile to set
	 */
	public void setExternalFile(File externalFile) throws FileNotFoundException {
		if (externalFile == null) {
			throw new FileNotFoundException("Can't read file: no file supplied");
		} else if (!FileUtil.isFileReadable(externalFile)) {
			throw new FileNotFoundException("Can't read file: " + externalFile.getAbsolutePath());
		}

		this.externalFile = externalFile;
		setExternalFileCharacterSet();
	}

	private void setExternalFileCharacterSet() {
		if (type.isPicture()) {
			externalFileCharacterSet = null;
		} else {
			try {
				externalFileCharacterSet = FileUtil.getFileCharset(externalFile);
			} catch (IOException ex) {
				externalFileCharacterSet = null;
				LOGGER.warn("Exception during external file charset detection.", ex);
			}
		}
	}

	public void setExternalFileCharacterSet(String charSet) {
		externalFileCharacterSet = charSet;
	}

	public String getExternalFileCharacterSet() {
		return externalFileCharacterSet;
	}

	/**
	 * @return true if external subtitles file is UTF-8 encoded, false otherwise.
	 */
	public boolean isExternalFileUtf8() {
		return FileUtil.isCharsetUTF8(externalFileCharacterSet);
	}

	/**
	 * @return true if external subtitles file is UTF-16 encoded, false otherwise.
	 */
	public boolean isExternalFileUtf16() {
		return FileUtil.isCharsetUTF16(externalFileCharacterSet);
	}

	/**
	 * @return true if external subtitles file is UTF-32 encoded, false otherwise.
	 */
	public boolean isExternalFileUtf32() {
		return FileUtil.isCharsetUTF32(externalFileCharacterSet);
	}

	/**
	 * @return true if external subtitles file is UTF-8 or UTF-16 encoded, false otherwise.
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
}
