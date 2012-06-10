/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import net.pms.PMS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the subtitle information for media.
 * 
 * TODO: Change all instance variables to private. For backwards compatibility
 * with external plugin code the variables have all been marked as deprecated
 * instead of changed to private, but this will surely change in the future.
 * When everything has been changed to private, the deprecated note can be
 * removed.
 */
public class DLNAMediaSubtitle extends DLNAMediaLang implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaSubtitle.class);

	public static final int SUBRIP = 1;
	public static final int TEXT = 2;
	public static final int MICRODVD = 3;
	public static final int SAMI = 4;
	public static final int ASS = 5;
	public static final int VOBSUB = 6;
	public static final int EMBEDDED = 7;
	public static String subExtensions[] = new String[]{"srt", "txt", "sub", "smi", "ass", "idx"};

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int type;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String flavor;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public File file;
	private File utf8_file;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean is_file_utf8;

	public File getPlayableFile() {
		if (utf8_file != null) {
			return utf8_file;
		}
		return file;
	}

	public String getSubType() {
		switch (type) {
			case SUBRIP:
				return "SubRip";
			case TEXT:
				return "Text File";
			case MICRODVD:
				return "MicroDVD";
			case SAMI:
				return "Sami";
			case ASS:
				return "ASS/SSA";
			case VOBSUB:
				return "VobSub";
			case EMBEDDED:
				return "Embedded";
		}
		return "-";
	}

	/**
	 * Returns whether or not the subtitles are embedded.
	 *
	 * @return True if the subtitles are embedded, false otherwise.
	 * @since 1.51.0
	 */
	public boolean isEmbedded() {
		switch (type) {
			case ASS:
				// No file available means the subtitles are embedded
				return (file == null);
			case EMBEDDED:
				return true;
		}
		return false;
	}

	public String toString() {
		return "Sub: " + getSubType() + " / lang: " + getLang() + " / flavor: " + flavor + " / ID: " + getId() + " / FILE: " + (file != null ? file.getAbsolutePath() : "-");
	}

	public void checkUnicode() {
		if (file != null && file.exists() && file.length() > 3) {
			FileInputStream fis = null;
			try {
				int is_file_unicode = 0;

				fis = new FileInputStream(file);
				int b1 = fis.read();
				int b2 = fis.read();
				int b3 = fis.read();
				if (b1 == 255 && b2 == 254) {
					is_file_unicode = 1;
				} else if (b1 == 254 && b2 == 255) {
					is_file_unicode = 2;
				} else if (b1 == 239 && b2 == 187 && b3 == 191) {
					is_file_utf8 = true;
				}

				// MPlayer doesn't handle UTF-16 encoded subs
				if (is_file_unicode > 0) {
					is_file_utf8 = true;
					utf8_file = new File(PMS.getConfiguration().getTempFolder(), "utf8_" + file.getName());
					if (!utf8_file.exists()) {
						InputStreamReader r = new InputStreamReader(new FileInputStream(file), is_file_unicode == 1 ? "UTF-16" : "UTF-16BE");
						OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(utf8_file), "UTF-8");
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
	public int getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
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
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * @return the is_file_utf8
	 */
	public boolean isFileUtf8() {
		return is_file_utf8;
	}

	/**
	 * @param isFileUtf8 the is_file_utf8 to set
	 */
	public void setFileUtf8(boolean isFileUtf8) {
		is_file_utf8 = isFileUtf8;
	}
}
