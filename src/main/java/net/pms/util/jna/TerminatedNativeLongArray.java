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
package net.pms.util.jna;

import java.util.ArrayList;
import java.util.Collection;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;


/**
 * An abstract {@code NativeLong} implementation of {@link TerminatedArray}.
 *
 * @see TerminatedArray
 *
 * @author Nadahar
 */
public abstract class TerminatedNativeLongArray extends TerminatedArray<NativeLong> {

	/** The size of a {@link NativeLong} in bytes */
	protected static final int SIZE = NativeLong.SIZE;

	/**
	 * Creates a new instance with the internal {@link Pointer} set to
	 * {@link Pointer#NULL}.
	 */
	public TerminatedNativeLongArray() {
	}

	/**
	 * Creates a new instance with the internal {@link Pointer} set to {@code p}.
	 *
	 * @param p the {@link Pointer} to use for the new instance.
	 */
	public TerminatedNativeLongArray(Pointer p) {
		setPointer(p);
	}

	/**
	 * Creates a new instance with {@code source} as its content and the
	 * internal {@link Pointer} set to {@link Pointer#NULL}. The internal
	 * {@link Pointer} will be instantiated and memory allocated when
	 * {@link #toNative()} is called by JNA.
	 *
	 * @param source the {@link Collection} of {@link NativeLong}s.
	 */
	public TerminatedNativeLongArray(Collection<? extends NativeLong> source) {
		buffer = new ArrayList<NativeLong>(source);
	}

	@Override
	public abstract NativeLong getTerminator();

	@Override
	public int getElementSize() {
		return SIZE;
	}

	@Override
	public NativeLong readElement(int i) {
		return getPointer().getNativeLong((long) i * SIZE);
	}

	@Override
	protected void writeElement(int i) {
		getPointer().setNativeLong((long) i * SIZE, buffer.get(i));

	}

	@Override
	protected void writeTerminator() {
		getPointer().setNativeLong(buffer.size() * SIZE, getTerminator());
	}
}
