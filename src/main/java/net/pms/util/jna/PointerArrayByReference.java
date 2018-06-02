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

import com.sun.jna.Pointer;


/**
 * A {@link Pointer} implementation of {@link ArrayByReference}.
 *
 * @author Nadahar
 */
public class PointerArrayByReference extends ArrayByReference<Pointer> {

	/**
	 * Creates an unallocated {@link PointerArrayByReference}.
	 */
	public PointerArrayByReference() {
	}

	/**
	 * Creates a new instance and allocates space for an array of {@code size} elements.
	 *
	 * @param size the number of {@link Pointer}'s in the array.
	 */
	public PointerArrayByReference(long size) {
		setSize(size, 0);
	}

	/**
	 * Sets a new array size allocating memory as needed with a re-allocating
	 * threshold of 100.
	 *
	 * @param size the new array size to allocate. Setting the size to 0 will
	 *            set the held {@link Pointer} to {@code null}.
	 */
	public void setSize(long size) {
		super.setSize(size, 100);
	}

	@Override
	public int getElementSize() {
		return Pointer.SIZE;
	}

	/**
	 * Stores the values from {@code pointer} allocating memory as needed with a
	 * re-allocating threshold of 100.
	 *
	 * @param pointers the array of {@link Pointer}s to write to the referenced
	 *            memory. Sending an empty array will set the held
	 *            {@link Pointer} to {@code null}.
	 */
	public void setArray(Pointer[] pointers) {
		super.setArray(pointers, 100);
	}

	@Override
	protected Pointer[] getElements() {
		return getPointer() == null ? null : getPointer().getPointerArray(0, (int) size);
	}

	@Override
	protected void setElements(Pointer[] array) {
		for (int i = 0; i < size; i++) {
			getPointer().setPointer(i * getElementSize(), array[i]);
		}
	}
}
