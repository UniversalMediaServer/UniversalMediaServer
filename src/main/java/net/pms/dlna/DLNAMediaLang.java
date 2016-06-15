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

import net.pms.util.Iso639;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of the language information for subtitles or audio.
 * 
 * TODO: Change all instance variables to private. For backwards compatibility
 * with external plugin code the variables have all been marked as deprecated
 * instead of changed to private, but this will surely change in the future.
 * When everything has been changed to private, the deprecated note can be
 * removed.
 */
public class DLNAMediaLang {
	public static final String UND = "und";

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int id;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String lang;

	/**
	 * Returns the full language name for an audio or subtitle track based on a
	 * translation from the ISO 639 language code. If no code has been set,
	 * "Undetermined" is returned.
	 * 
	 * @return The language name
	 * @since 1.50
	 */
	public String getLangFullName() {
		if (StringUtils.isNotBlank(lang)) {
			return Iso639.getLanguage(lang);
		}
		return Iso639.getLanguage(DLNAMediaLang.UND);
	}

	public boolean matchCode(String code) {
		return Iso639.isCodesMatching(lang, code);
	}

	/**
	 * Returns the unique id for this language object
	 * 
	 * @return The id.
	 * @since 1.50
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets a unique id for this language object.
	 * 
	 * @param id The id to set.
	 * @since 1.50
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the IS0 639 language code for this language object. If you
	 * require the full language name, use {@link #getLangFullName()} instead.
	 * Special return values are "und" (for "undetermined") and "off" 
	 * (indicates an audio track or subtitle should be disabled).
	 * 
	 * @return The language code.
	 * @since 1.50
	 */
	public String getLang() {
		return lang;
	}
	/**
	 * Sets the ISO 639 language code for this language object. Special values
	 * are "und" (for "undetermined") and "off" (indicates an audio track or
	 * subtitle should be disabled).
	 * 
	 * @param lang The language code to set.
	 * @since 1.50
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}
}
