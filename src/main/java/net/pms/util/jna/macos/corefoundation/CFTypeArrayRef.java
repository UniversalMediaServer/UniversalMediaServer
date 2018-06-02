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

import com.sun.jna.Pointer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.util.jna.ArrayByReference;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFTypeRef;


/**
 * A {@link CFTypeRef} implementation of {@link ArrayByReference}.
 *
 * @author Nadahar
 */
public class CFTypeArrayRef extends ArrayByReference<CFTypeRef> {

	/**
	 * Creates an unallocated {@link CFTypeArrayRef}.
	 */
	public CFTypeArrayRef() {
		super();
	}

	/**
	 * Creates a new instance and allocates space for an array of {@code size}
	 * elements.
	 *
	 * @param size the number of {@link CFTypeRef}'s in the array.
	 */
	public CFTypeArrayRef(long size) {
		setSize(Pointer.SIZE * size, 0);
	}

	/**
	 * Creates a new instance and allocates and initializes the array using the
	 * content of {@code cfTypeRefs}.
	 *
	 * @param cfTypeRefs the array of {@link CFTypeRef}s to write to the
	 *            referenced memory. Sending an empty array will set the held
	 *            {@link Pointer} to {@code null}.
	 */
	public CFTypeArrayRef(CFTypeRef[] cfTypeRefs) {
		setArray(cfTypeRefs, 0);
	}

	/**
	 * A cache of the referenced array
	 */
	protected CFTypeRef[] array;

	/**
	 * Sets a new array size allocating memory as needed with a re-allocating
	 * threshold of 100.
	 *
	 * @param size the new array size to allocate. Setting the size to 0 will
	 *            set the held {@link Pointer} to {@code null}.
	 */
	public void setSize(long size) {
		if (size != getSize()) {
			array = null;
			super.setSize(size, 100);
		}
	}

	/**
	 * Stores the values from {@code cfTypeRefs} allocating memory as needed
	 * with a re-allocating threshold of 100.
	 *
	 * @param cfTypeRefs the array of {@link CFTypeRef}s to write to the
	 *            referenced memory. Sending an empty array will set the held
	 *            {@link Pointer} to {@code null}.
	 */
	public void setArray(CFTypeRef[] cfTypeRefs) {
		setArray(cfTypeRefs, 100);
	}

	/**
	 * Stores the values from {@code cfTypeRefs} allocating memory as needed.
	 *
	 * @param cfTypeRefs the array of {@link CFTypeRef}s to write to the referenced
	 *            memory. Sending an empty array will set the held
	 *            {@link Pointer} to {@code null}.
	 * @param reAllocateThreshold decides how much the new required allocation
	 *            can be smaller than the current allocation in bytes before
	 *            memory should be reallocated. This only has effect is memory
	 *            is already reserved and the new array size is smaller than the
	 *            previous.
	 */
	@Override
	public void setArray(CFTypeRef[] cfTypeRefs, long reAllocateThreshold) {
		super.setArray(cfTypeRefs, reAllocateThreshold);
		if (array == null || cfTypeRefs.length != array.length) {
			array = new CFTypeRef[cfTypeRefs.length];
		}
		if (cfTypeRefs.length > 0) {
			System.arraycopy(cfTypeRefs, 0, array, 0, cfTypeRefs.length);
		}
	}


	/**
	 * Returns a cached array of {@link CFTypeRef}s. This is a copy of the
	 * referenced memory and any changes will not be reflected in the referenced
	 * memory.
	 *
	 * @return An array containing the values of the referenced {@link CFTypeRef} array.
	 */
	@Override
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public CFTypeRef[] getArray() {
		if (array == null) {
			array = super.getArray();
		}
		return array;
	}

	@Override
	public int getElementSize() {
		return Pointer.SIZE;
	}

	@Override
	protected CFTypeRef[] getElements() {
		CFTypeRef[] result = new CFTypeRef[(int) size];
		for (int i = 0; i < size; i++) {
			result[i] = new CFTypeRef(getPointer().getPointer(i * getElementSize()));
		}
		return result;
	}

	@Override
	protected void setElements(CFTypeRef[] array) {
		for (int i = 0; i < size; i++) {
			getPointer().setPointer(i * getElementSize(), array[i].getPointer());
		}
	}

	@Override
	public String toString() {
		if (array == null) {
			array = super.getArray();
			if (array == null) {
				return "null";
			}
		}
		if (size == 0 || array.length == 0) {
			return getClass().getSimpleName() + ": empty";
		}
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(": [");

		for (int i = 0; i < size && i < array.length; i++) {
			sb.append(array[i].toString());
			if (i < size - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}
}
