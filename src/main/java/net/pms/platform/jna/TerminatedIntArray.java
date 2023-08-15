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
package net.pms.platform.jna;

import com.sun.jna.Pointer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An abstract {@code Integer} implementation of {@link TerminatedArray}.
 *
 * @see TerminatedArray
 *
 * @author Nadahar
 */
public abstract class TerminatedIntArray extends TerminatedArray<Integer> {

	/** The size of an integer in bytes */
	protected static final int SIZE = Integer.SIZE / 8;

	/**
	 * Creates a new instance with the internal {@link Pointer} set to
	 * {@link Pointer#NULL}.
	 */
	protected TerminatedIntArray() {
	}

	/**
	 * Creates a new instance with the internal {@link Pointer} set to {@code p}.
	 *
	 * @param p the {@link Pointer} to use for the new instance.
	 */
	protected TerminatedIntArray(Pointer p) {
		setPointer(p);
	}

	/**
	 * Creates a new instance with {@code source} as its content and the
	 * internal {@link Pointer} set to {@link Pointer#NULL}. The internal
	 * {@link Pointer} will be instantiated and memory allocated when
	 * {@link #toNative()} is called by JNA.
	 *
	 * @param source the {@link Collection} of {@link Integer}s.
	 */
	protected TerminatedIntArray(Collection<? extends Integer> source) {
		buffer = new ArrayList<>(source);
	}

	@Override
	public abstract Integer getTerminator();

	@Override
	public int getElementSize() {
		return SIZE;
	}

	@Override
	public Integer readElement(int i) {
		return getPointer().getInt((long) i * SIZE);
	}

	@Override
	protected void writeElement(int i) {
		getPointer().setInt((long) i * SIZE, buffer.get(i));

	}

	@Override
	protected void writeTerminator() {
		getPointer().setInt(buffer.size() * SIZE, getTerminator());
	}
}
