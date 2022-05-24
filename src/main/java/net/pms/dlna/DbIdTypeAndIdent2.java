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

/**
 * Will be DbIdTypeAndIdent but as some dev use windows git, only changing case fail.
 * Should be renamed on the next release, then dev will have time to import DbIdTypeAndIdent2
 * on their git, that will allow rename to DbIdTypeAndIdent.
 */
public class DbIdTypeAndIdent2 {

	/**
	 * Media type
	 */
	public final DbIdMediaType type;

	/**
	 * resource to identify
	 */
	public final String ident;

	public DbIdTypeAndIdent2(DbIdMediaType type, String ident) {
		super();
		this.type = type;
		if (ident == null) {
			this.ident = "";
		} else {
			this.ident = ident;
		}
	}
}
