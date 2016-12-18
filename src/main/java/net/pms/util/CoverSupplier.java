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
package net.pms.util;

/**
 * Defines the suppliers of covers/album art implemented by UMS
 * <ul>
 * <li>None</li>
 * <li>Cover Art Archive</li>
 * </ul>
 * The {@link CoverSupplier} class is final and cannot be sub-classed.</p>
 */
public class CoverSupplier {

	public static final int NONE_INT = 0;
	public static final int COVER_ART_ARCHIVE_INT = 1;

	public static final Integer NONE_INTEGER = NONE_INT;
	public static final Integer COVER_ART_ARCHIVE_INTEGER = COVER_ART_ARCHIVE_INT;

	/**
	 * <code>NONE</code> for no cover supplier.
	 */
	public static final CoverSupplier NONE = new CoverSupplier(NONE_INT, "None");

	/**
	 * <code>COVER_ART_ARCHIVE</code> for Cover Art Archive.
	 */
	public static final CoverSupplier COVER_ART_ARCHIVE = new CoverSupplier(COVER_ART_ARCHIVE_INT, "Cover Art Archive");

	public final int CoverSupplierInt;
	public final String CoverSupplierStr;

	/**
	 * Instantiate a {@link CoverSupplier} object.
	 */
	private CoverSupplier(int CoverSupplierInt, String CoverSupplierStr) {
		this.CoverSupplierInt = CoverSupplierInt;
		this.CoverSupplierStr = CoverSupplierStr;
	}

	/**
	 * Returns the string representation of this {@link CoverSupplier}.
	 */
	@Override
	public String toString() {
		return CoverSupplierStr;
	}

	/**
	 * Returns the integer representation of this {@link CoverSupplier}.
	 */
	public int toInt() {
		return CoverSupplierInt;
	}

	/**
	 * Converts a {@link CoverSupplier} to an {@link Integer} object.
	 *
	 * @return This {@link CoverSupplier}'s {@link Integer} mapping.
	 */
	public Integer toInteger() {
		switch (CoverSupplierInt) {
			case NONE_INT:
				return NONE_INTEGER;
			case COVER_ART_ARCHIVE_INT:
				return COVER_ART_ARCHIVE_INTEGER;
			default:
				throw new IllegalStateException("CoverSupplier " + CoverSupplierStr + ", " + CoverSupplierInt + " is unknown.");
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a {@link CoverSupplier}. If
	 * the conversion fails, this method returns {@link #NONE}.
	 */
	public static CoverSupplier toCoverSupplier(String sArg) {
		return toCoverSupplier(sArg, CoverSupplier.NONE);
	}

	/**
	 * Converts the integer passed as argument to a {@link CoverSupplier}. If the
	 * conversion fails, this method returns {@link #NONE}.
	 */
	public static CoverSupplier toCoverSupplier(int val) {
		return toCoverSupplier(val, CoverSupplier.NONE);
	}

	/**
	 * Converts the integer passed as argument to a {@link CoverSupplier}. If the
	 * conversion fails, this method returns the specified default.
	 */
	public static CoverSupplier toCoverSupplier(int val, CoverSupplier defaultCoverSupplier) {
		switch (val) {
			case NONE_INT:
				return NONE;
			case COVER_ART_ARCHIVE_INT:
				return COVER_ART_ARCHIVE;
			default:
				return defaultCoverSupplier;
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a {@link CoverSupplier}. If
	 * the conversion fails, this method returns the specified default.
	 */
	public static CoverSupplier toCoverSupplier(String sArg, CoverSupplier defaultCoverSupplier) {
		if (sArg == null) {
			return defaultCoverSupplier;
		}

		sArg = sArg.toLowerCase();
		switch (sArg.toLowerCase()) {
			case "none":
				return CoverSupplier.NONE;
			case "coverartarchive":
			case "coverartarchive.org":
			case "cover art archive":
				return CoverSupplier.COVER_ART_ARCHIVE;
			default:
				return defaultCoverSupplier;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + CoverSupplierInt;
		result = prime * result + ((CoverSupplierStr == null) ? 0 : CoverSupplierStr.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CoverSupplier)) {
			return false;
		}
		CoverSupplier other = (CoverSupplier) obj;
		if (CoverSupplierInt != other.CoverSupplierInt) {
			return false;
		}
		if (CoverSupplierStr == null) {
			if (other.CoverSupplierStr != null) {
				return false;
			}
		} else if (!CoverSupplierStr.equals(other.CoverSupplierStr)) {
			return false;
		}
		return true;
	}
}
