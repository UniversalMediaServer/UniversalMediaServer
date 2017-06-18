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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import com.sun.jna.FromNativeContext;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;


/**
 * An abstract implementation of an array where the elements are placed adjacent
 * to each other and the end of the array is terminated by a defined terminator.
 * This is identical to how a C string is stored.
 * <p>
 * The content is read into a buffer when a new {@link Pointer} is set either
 * manually, by a constructor or by native code. This is to avoid having to
 * parse the pointer content over and over again, but makes it unsuitable to
 * store large arrays as the content is stored in memory twice.
 * <p>
 * For easy access to the content, the {@link List} interface is implemented.
 *
 * @param <E> the type of elements in this {@link TerminatedArray}.
 *
 * @author Nadahar
 */
public abstract class TerminatedArray<E> extends PointerType implements List<E> {

	/**
	 * Creates a new instance with the internal {@link Pointer} set to
	 * {@link Pointer#NULL}.
	 */
	public TerminatedArray() {
		super();
	}

	/**
	 * Creates a new instance with the internal {@link Pointer} set to {@code p}.
	 *
	 * @param p the {@link Pointer} to use for the new instance.
	 */
	public TerminatedArray(Pointer p) {
		super(p);
	}

	/**
	 * The internal {@link List} where the content from the {@link Pointer} is
	 * cached.
	 */
	protected List<E> buffer;

	/**
	 * @return The array terminator for this type of {@link TerminatedArray}.
	 */
	public abstract E getTerminator();

	/**
	 * The size of one element in the array in bytes.
	 *
	 * @return The element size.
	 */
	public abstract int getElementSize();

	/**
	 * Reads the element at index {@code i}.
	 *
	 * @param i the index of the element.
	 * @return The element.
	 */
	protected abstract E readElement(int i);

	/**
	 * Writes the element at index {@code i}.
	 *
	 * @param i the index of the element.
	 */
	protected abstract void writeElement(int i);

	/**
	 * Writes the terminator.
	 */
	protected abstract void writeTerminator();

	@Override
	public Object fromNative(Object nativeValue, FromNativeContext context) {
		setPointer((Pointer) nativeValue);
		// Always pass along null pointer values
		return nativeValue == null ? null : this;
	}

	@Override
	public Object toNative() {
		if (buffer == null) {
			return null;
		}
		long size = getElementSize() * (buffer.size() + 1);
		// Reuse the reserved memory if possible
		if (!(getPointer() instanceof Memory) || ((Memory) getPointer()).size() < size) {
			super.setPointer(new Memory(size));
		}
		for (int i = 0; i < buffer.size(); i++) {
			writeElement(i);
		}
		writeTerminator();
		return getPointer();
	}

	@Override
	public void setPointer(Pointer p) {
		super.setPointer(p);
		readElements();
	}

	/**
	 * Reads the data from the pointer into the internal array.
	 */
	protected void readElements() {
		if (getPointer() == null) {
			buffer = null;
			return;
		}
		E terminator = getTerminator();
		if (buffer == null) {
			buffer = new ArrayList<E>();
		} else {
			buffer.clear();
		}
		for (int i = 0;; i++) {
			E element = readElement(i);
			if (element == terminator) {
				break;
			}
			buffer.add(element);
		}
	}

	@Override
	public String toString() {
		return buffer == null ? "null" : buffer.toString();
	}

	/*
	 * Implementation of java.util.List interface
	 */

	@Override
	public int size() {
		return buffer == null ? 0 : buffer.size();
	}

	@Override
	public boolean isEmpty() {
		return buffer == null || buffer.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return buffer != null && buffer.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return buffer == null ? null : buffer.iterator();
	}

	@Override
	public Object[] toArray() {
		return buffer == null ? null : buffer.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return buffer == null ? null : buffer.toArray(a);
	}

	@Override
	public boolean add(E e) {
		return buffer != null && buffer.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return buffer != null && buffer.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return buffer != null && buffer.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return buffer != null && buffer.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return buffer != null && buffer.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return buffer != null && buffer.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return buffer != null && buffer.retainAll(c);
	}

	@Override
	public void clear() {
		if (buffer != null) {
			buffer.clear();
		}
	}

	@Override
	public E get(int index) {
		return buffer == null ? null : buffer.get(index);
	}

	@Override
	public E set(int index, E element) {
		if (buffer == null) {
			throw new IndexOutOfBoundsException("TerminatedArray is empty");
		}
		return buffer.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		if (buffer != null) {
			buffer.add(index, element);
		}
	}

	@Override
	public E remove(int index) {
		return buffer == null ? null : buffer.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return buffer == null ? -1 : buffer.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return buffer == null ? -1 : buffer.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return buffer == null ? null : buffer.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return buffer == null ? null : buffer.listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return buffer == null ? null : buffer.subList(fromIndex, toIndex);
	}
}
