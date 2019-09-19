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
 * An abstract implementation of a referenced array where the elements are
 * placed adjacent to each other without a terminator.
 *
 * @param <E> the type of elements in this {@link ArrayByReference}.
 *
 * @author Nadahar
 */
public abstract class ArrayByReference<E> extends PointerType {

	/**
	 * Creates an unallocated {@link ArrayByReference}.
	 */
	public ArrayByReference() {
		super();
	}

	/** The current array size. */
	protected long size;

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
	 * Sets a new array size allocating memory as needed.
	 *
	 * <b>Please note that calling {@link #setSize} while holding a
	 * {@link Pointer} allocated by native code will lead to memory leak as the
	 * reference to the allocated memory is lost</b>. Instead, free the pointer
	 * by sending {@link #getPointer()} to the native free function before
	 * calling {@link #setSize}. {@link #isGarbageCollected()} can be used to
	 * check if the currently held {@link Pointer} is allocated by Java and will
	 * be deallocated by the GC or not.
	 *
	 * @param size the new array size to allocate. Setting the size to 0 will
	 *            set the held {@link Pointer} to {@code null}.
	 * @param reAllocateThreshold decides how much the new required allocation
	 *            can be smaller than the current allocation in bytes before
	 *            memory should be reallocated. This only has effect is memory
	 *            is already reserved and the new {@code size} is smaller than
	 *            the previous.
	 */
	public void setSize(long size, long reAllocateThreshold) {
		if (size < 1) {
			super.setPointer(Pointer.NULL);
			this.size = 0;
		} else if (
			getPointer() instanceof Memory &&
			getElementSize() * size < ((Memory) getPointer()).size() &&
			Math.abs(((Memory) getPointer()).size() - getElementSize() * size) <= reAllocateThreshold
		) {
			// Reuse the allocated memory if the reduction in size is less than
			// or equal to reAllocateThreshold bytes.
			this.size = size;
		} else {
			// Allocate new memory
			super.setPointer(new Memory(getElementSize() * size));
			this.size = size;
		}
	}

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
	 * <b>Please note that calling {@link #setArray} while holding a
	 * {@link Pointer} allocated by native code will lead to memory leak as the
	 * reference to the allocated memory is lost</b>. Instead, free the pointer
	 * by sending {@link #getPointer()} to the native free function before
	 * calling {@link #setArray}. {@link #isGarbageCollected()} can be used to
	 * check if the currently held {@link Pointer} is allocated by Java and will
	 * be deallocated by the GC or not.
	 *
	 * @param array the array of {@link E}s to write to the referenced memory.
	 *            Sending an empty array will set the held {@link Pointer} to
	 *            {@code null}.
	 * @param reAllocateThreshold decides how much the new required allocation
	 *            can be smaller than the current allocation in bytes before
	 *            memory should be reallocated. This only has effect is memory
	 *            is already reserved and the new array size is smaller than the
	 *            previous.
	 */
	public void setArray(E[] array, long reAllocateThreshold) {
		if (array == null) {
			throw new NullPointerException("array cannot be null");
		}
		setSize(array.length, reAllocateThreshold);
		if (array.length > 0) {
			setElements(array);
		}
	}

	/**
	 * Generates and returns an array of {@link E}s. This is a copy of the
	 * referenced memory and any changes will not be reflected in the referenced
	 * memory.
	 *
	 * @return An array of {@link E} containing the values of the referenced array.
	 */
	public E[] getArray() {
		if (getPointer() == null) {
			return null;
		}
		if (size > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("Array to big, please read it \"manually\" using getPointer.readX");
		}
		return getElements();
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
