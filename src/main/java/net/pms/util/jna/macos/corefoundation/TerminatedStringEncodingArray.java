/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
package net.pms.util.jna.macos.corefoundation;

import java.util.Collection;
import java.util.EnumSet;

import com.sun.jna.Pointer;
import net.pms.util.jna.TerminatedArray;
import net.pms.util.jna.TerminatedIntArray;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringBuiltInEncodings;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringRef;


/**
 * A representation of an array of {@code CFStringEncoding} terminated by
 * {@link CFStringBuiltInEncodings#kCFStringEncodingInvalidId}.
 *
 * @see TerminatedArray
 *
 * @author Nadahar
 */
public class TerminatedStringEncodingArray extends TerminatedIntArray {

	/**
	 * Creates a new instance with the internal {@link Pointer} set to
	 * {@link Pointer#NULL}.
	 */
	public TerminatedStringEncodingArray() {
	}

	/**
	 * Creates a new instance with the internal {@link Pointer} set to {@code p}.
	 *
	 * @param p the {@link Pointer} to use for the new instance.
	 */
	public TerminatedStringEncodingArray(Pointer p) {
		super(p);
	}

	/**
	 * Creates a new instance with {@code source} as its content and the
	 * internal {@link Pointer} set to {@link Pointer#NULL}. The internal
	 * {@link Pointer} will be instantiated and memory allocated when
	 * {@link #toNative()} is called by JNA.
	 *
	 * @param source the {@link Collection} of valid {@code CFStringEncoding}
	 *            {@link Integer}s.
	 */
	public TerminatedStringEncodingArray(Collection<? extends Integer> source) {
		super(source);
	}

	/**
	 * Returns the {@link CFStringBuiltInEncodings} value of the encoding at
	 * {@code index} or {@code null} it the encoding doesn't represent one of
	 * the {@link CFStringBuiltInEncodings}.
	 *
	 * @param index the index of the encoding.
	 * @return A {@link CFStringBuiltInEncodings} instance or {@code null}.
	 */
	public CFStringBuiltInEncodings getBuiltInEncoding(int index) {
		return CFStringBuiltInEncodings.typeOf(get(index));
	}

	/**
	 * Returns only the encodings that can be represented as
	 * {@link CFStringBuiltInEncodings} instances as an {@link EnumSet}.
	 *
	 * @return The built-in encodings in this
	 *         {@link TerminatedStringEncodingArray}.
	 */
	public EnumSet<CFStringBuiltInEncodings> getBuiltInEncodings() {
		EnumSet<CFStringBuiltInEncodings> result = EnumSet.noneOf(CFStringBuiltInEncodings.class);
		if (!isEmpty()) {
			for (int i = 0; i < size(); i++) {
				CFStringBuiltInEncodings encoding = getBuiltInEncoding(i);
				if (encoding != null) {
					result.add(encoding);
				}
			}
		}
		return result;
	}

	/**
	 * Returns a string representation of the element at {@code index}.
	 *
	 * @param index the index of the element.
	 * @param includeValue whether the integer value should be included.
	 * @return The {@link String} representation.
	 */
	public String getString(int index, boolean includeValue) {
		CFStringRef cfString = CoreFoundation.INSTANCE.CFStringGetNameOfEncoding(get(index));
		if (cfString == null) {
			return includeValue ? get(index) + ": error" : "error";
		}
		return includeValue ? get(index) + ": " + cfString.toString() : cfString.toString();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * Returns a string representation of this
	 * {@link TerminatedStringEncodingArray}.
	 *
	 * @param includeValue whether the integer values should be included.
	 * @return The {@link String} representation.
	 */
	public String toString(boolean includeValue) {
		if (isEmpty()) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(getString(i, includeValue));
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public Integer getTerminator() {
		return CFStringBuiltInEncodings.kCFStringEncodingInvalidId.getValue();
	}
}
