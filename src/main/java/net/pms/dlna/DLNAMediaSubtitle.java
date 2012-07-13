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

import java.io.*;
import net.pms.PMS;
import net.pms.formats.v2.SubtitleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the subtitle information for media.
 */
public class DLNAMediaSubtitle extends DLNAMediaLang implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaSubtitle.class);

	private SubtitleType type = SubtitleType.UNKNOWN;

	/*
	 * This tells us whether the track is forced or not
	 */
	private String flavor;

	private File externalFile;
	private boolean isExternalFileUtf8;
	private File externalFileConvertedToUtf8;

	public File getPlayableExternalFile() {
		if (externalFileConvertedToUtf8 != null) {
			return externalFileConvertedToUtf8;
		}
		return externalFile;
	}

	/**
	 * Returns whether or not the subtitles are embedded.
	 *
	 * @return True if the subtitles are embedded, false otherwise.
	 * @since 1.51.0
	 */
	public boolean isEmbedded() {
		return (externalFile == null);
	}

	public String toString() {
		return "Sub: " + (type != null ? type.getDescription() : "null") + " / lang: " + getLang() + " / flavor: " + flavor + " / ID: " + getId() + " / FILE: " + (externalFile != null ? externalFile.getAbsolutePath() : "-");
	}

	public void checkUnicode() {
		if (externalFile != null && externalFile.canRead() && externalFile.length() > 3) {
			FileInputStream fis = null;
			try {
				int is_file_unicode = 0;

				fis = new FileInputStream(externalFile);
				int b1 = fis.read();
				int b2 = fis.read();
				int b3 = fis.read();
				if (b1 == 255 && b2 == 254) {
					is_file_unicode = 1;
				} else if (b1 == 254 && b2 == 255) {
					is_file_unicode = 2;
				} else if (b1 == 239 && b2 == 187 && b3 == 191) {
					isExternalFileUtf8 = true;
				}

				// MPlayer doesn't handle UTF-16 encoded subs
				if (is_file_unicode > 0) {
					isExternalFileUtf8 = true;
					externalFileConvertedToUtf8 = new File(PMS.getConfiguration().getTempFolder(), "utf8_" + externalFile.getName());
					if (!externalFileConvertedToUtf8.exists()) {
						InputStreamReader r = new InputStreamReader(new FileInputStream(externalFile), is_file_unicode == 1 ? "UTF-16" : "UTF-16BE");
						OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(externalFileConvertedToUtf8), "UTF-8");
						int c;
						while ((c = r.read()) != -1) {
							osw.write(c);
						}
						osw.close();
						r.close();
					}
				}
			} catch (IOException e) {
				LOGGER.error(null, e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						LOGGER.debug("Caught exception", e);
					}
				}
			}
		}
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
	 * @return the externalFile
	 */
	public File getExternalFile() {
		return externalFile;
	}

	/**
	 * @param externalFile the externalFile to set
	 */
	public void setExternalFile(File externalFile) {
		this.externalFile = externalFile;
	}

	/**
	 * @return the isExternalFileUtf8
	 */
	public boolean isExternalFileUtf8() {
		return isExternalFileUtf8;
	}
}
