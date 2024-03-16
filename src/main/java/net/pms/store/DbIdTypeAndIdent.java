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
package net.pms.store;

/**
 */
public class DbIdTypeAndIdent {

	/**
	 * Media type
	 */
	public final DbIdMediaType type;

	/**
	 * resource to identify
	 */
	public final String ident;

	public DbIdTypeAndIdent(DbIdMediaType type, String ident) {
		super();
		this.type = type;
		if (ident == null) {
			this.ident = "";
		} else {
			this.ident = ident;
		}
	}

	@Override
	public String toString() {
		return type.toString() + ident;
	}

	/**
	 * Removes person role names in case one exist.
	 *
	 * @return
	 */
	public String getIdentUnprefixed() {
		if (ident.startsWith(DbIdMediaType.PERSON_COMPOSER_PREFIX)) {
			return ident.substring(DbIdMediaType.PERSON_COMPOSER_PREFIX.length());
		} else if (ident.startsWith(DbIdMediaType.PERSON_CONDUCTOR_PREFIX)) {
			return ident.substring(DbIdMediaType.PERSON_CONDUCTOR_PREFIX.length());
		} else if (ident.startsWith(DbIdMediaType.PERSON_ALBUMARTIST_PREFIX)) {
			return ident.substring(DbIdMediaType.PERSON_ALBUMARTIST_PREFIX.length());
		} else {
			return ident;
		}
	}
}
