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

import com.sun.jna.FromNativeContext;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;


/**
 * An abstract implementation of a referenced fixed size array where the
 * elements are placed adjacent to each other without a terminator.
 *
 * @param <E> the type of elements in this {@link FixedArrayByReference}.
 *
 * @author Nadahar
 */
public abstract class FixedArrayByReference<E> extends PointerType {

	/**
	 * Creates and allocates a new {@link FixedArrayByReference}.
	 *
	 * @param fixedSize the size of this fixed size array.
	 */
	public FixedArrayByReference(long fixedSize) {
		super();
		size = Math.max(fixedSize, 0);
		if (size > 0) {
			// Allocate new memory
			super.setPointer(new Memory(getElementSize() * size));
		}
	}

	/** The array size. */
	protected final long size;

	/**
	 * The size of one element in this array in bytes.
	 *
	 * @return The element size in bytes.
	 */
	public abstract int getElementSize();

	/**
	 * Reads the native array elements.
	 *
	 * @return An array containing the retrieved native elements.
	 */
	protected abstract E[] getElements();

	/**
	 * Writes {@code array} to the native array.
	 *
	 * @param array the array whose content to write.
	 */
	protected abstract void setElements(E[] array);

	/**
	 * @return The number of elements in the array.
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @return Whether or not the currently held {@link Pointer} is deallocated
	 *         by Java's Garbage Collector or not. If it isn't, you have to free
	 *         it manually by sending {@link #getPointer()} to the native free
	 *         function after use.
	 */
	public boolean isGarbageCollected() {
		return getPointer() == null || getPointer() instanceof Memory;
	}

	/**
	 * Stores the values from {@code array} allocating memory as needed.
	 *
	 * @param array the array of {@link E}s to write to the referenced memory.
	 *            The array size must be the same as defined for this
	 *            {@link FixedArrayByReference}.
	 */
	public void setArray(E[] array) {
		if (array == null) {
			throw new NullPointerException("array cannot be null");
		}
		if (array.length != size) {
			throw new IllegalArgumentException("array size must be " + size);
		}

		if (size < 1) {
			super.setPointer(Pointer.NULL);
			return;
		} else if (getPointer() == null) {
			// Allocate new memory
			super.setPointer(new Memory(getElementSize() * size));
		}

		setElements(array);
	}

	/**
	 * Generates and returns an array of {@link E}s. This is a copy of the
	 * referenced memory and any changes will not be reflected in the referenced
	 * memory.
	 *
	 * @return An array of {@link E} containing the values of the referenced array.
	 */
	public E[] getArray() {
		if (size > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("Array to big, please read it \"manually\" using getPointer.readX");
		}
		return getPointer() == null ? null : getElements();
	}

	/**
	 * Sets the held {@link Pointer} to the referenced memory. This is only
	 * allowed if {@link #isGarbageCollected()} is {@code true}, as it could
	 * otherwise cause a memory leak.
	 *
	 * @param p the {@link Pointer} to set.
	 * @throws UnsupportedOperationException if the held {@link Pointer} is not
	 *             {@code null} and is pointing to memory not reserved by Java.
	 */
	@Override
	public void setPointer(Pointer p) {
		if (isGarbageCollected()) {
			super.setPointer(p);
		} else {
			throw new UnsupportedOperationException(
					"The internal pointer points to native memory, setting it could lead to memory leak and is not supported"
			);
		}
	}

	@Override
	public Object fromNative(Object nativeValue, FromNativeContext context) {
		// Always pass along null pointer values
		if (nativeValue == null) {
			return null;
		}
		super.setPointer((Pointer) nativeValue);
		return this;
	}
}
