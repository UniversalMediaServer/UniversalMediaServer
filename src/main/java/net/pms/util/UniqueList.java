/*
 * Digital Media Server, for streaming digital media to UPnP AV or DLNA
 * compatible devices based on PS3 Media Server and Universal Media Server.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/.
 */
package net.pms.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An {@link ArrayList} implementation where duplicates (as defined by
 * {@link Object#equals}) isn't allowed. Adding elements that already exists
 * won't throw an {@link Exception}, but will simply fail to add the element and
 * return {@code false} as a consequence. Returning {@code false} to a
 * {@link List#add} operation violates the {@link List} interface contract.
 *
 * This class can be seen as somewhat of a hybrid between the {@link List} and
 * the {@link Set} interfaces, it implements the uniqueness requirement of the
 * {@link Set} interface while maintaining all the methods of the {@link List}
 * interface and it's ordered nature. Unlike the {@link Set} interface, it
 * doesn't require that all elements must be immutable.
 *
 * @param <E> the element type.
 *
 * @author Nadahar
 */
@NotThreadSafe
@SuppressWarnings("serial")
public class UniqueList<E> extends ArrayList<E> {

	/**
	 * Constructs an empty {@link UniqueList} with an initial capacity of ten.
	 */
	public UniqueList() {
		super();
	}

	/**
	 * Constructs an empty {@link UniqueList} with the specified initial
	 * capacity.
	 *
	 * @param initialCapacity the initial capacity of the list.
	 * @throws IllegalArgumentException if the specified initial capacity is
	 *             negative.
	 */
	public UniqueList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs a {@link UniqueList} containing the elements of the specified
	 * {@link Collection}, in the order they are returned by the
	 * {@link Collection}'s {@link Iterator}. If the specified
	 * {@link Collection} contains multiple instances of the same element,
	 * subsequent elements are dropped silently (only the first is kept).
	 *
	 * @param c the {@link Collection} whose elements are to be placed into this
	 *            {@link UniqueList}.
	 * @throws NullPointerException if the specified collection is {@code null}.
	 */
	public UniqueList(Collection<? extends E> c) {
		super(c.size());
		addAll(c);
	}

	/**
	 * Adds an element to the list if it is not already present.
	 * <p>
	 * <i>(Violation)</i> The {@link List} interface requires that this method
	 * always returns {@code true}. However this class may return {@code false}
	 * if this {@link UniqueList} already contains an element where
	 * {@link Object#equals(e)} is {@code true}.
	 *
	 * @param e element to be appended to this list
	 * @return {@code true} if object was added, {@code false} otherwise.
	 */
	@Override
	public boolean add(E e) {
		if (!contains(e)) {
			return super.add(e);
		}
		return false;
	}

	/**
	 * Inserts the specified element at the specified position in this list if
	 * it is not already present. If inserted, shifts the element currently at
	 * that position (if any) and any subsequent elements to the right (adds one
	 * to their indices).
	 * <p>
	 * <i>(Violation)</i> The {@link List} interface makes the assumption that
	 * the element is always inserted. This may not be the case with this
	 * implementation.
	 *
	 * @param index index at which the specified element is to be inserted.
	 * @param element element to be inserted.
	 * @throws IndexOutOfBoundsException {@inheritDoc}.
	 */
	@Override
	public void add(int index, E element) {
		if (!contains(element)) {
			super.add(index, element);
		}
	}

	/**
	 * Appends all of the elements in the specified {@link Collection} to the
	 * end of this {@link UniqueList} if they are not already present, in the
	 * order that they are returned by the specified {@link Collection}'s
	 * {@link Iterator}. If any of the elements are already present or the
	 * specified {@link Collection} contains multiple instances of the same
	 * element, subsequent elements are dropped silently (only the first is
	 * kept).
	 * <p>
	 * The behavior of this operation is undefined if the specified
	 * {@link Collection} is modified while the operation is in progress. (This
	 * implies that the behavior of this call is undefined if the specified
	 * {@link Collection} is this {@link UniqueList}, and this is nonempty).
	 * <p>
	 * <i>(Violation)</i> The {@link List}< interface makes the assumption that
	 * the elements are always inserted. This may not be the case with this
	 * implementation.
	 *
	 * @param c the {@link Collection} containing elements to be added.
	 * @return {@code true} if this {@link UniqueList} changed as a result of
	 *         the call.
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c == null || c.isEmpty()) {
			return false;
		}

		int oldSize = size();
		for (Iterator<? extends E> iterator = c.iterator(); iterator.hasNext();) {
			E e = iterator.next();
			if (!contains(e)) {
				super.add(e);
			}
		}
		return oldSize != size();
	}

	/**
	 * Inserts all of the elements in the specified {@link Collection} into this
	 * {@link UniqueList} if they are not already present, starting at the
	 * specified position. Shifts the element currently at that position (if
	 * any) and any subsequent elements to the right (increases their indices).
	 * The new elements will appear in the {@link List} in the order that they
	 * are returned by the specified {@link Collection}'s {@link Iterator}. If
	 * any of the elements are already present or the specified
	 * {@link Collection} contains multiple instances of the same element,
	 * subsequent adds are dropped silently (only the first is kept).
	 * <p>
	 * <i>(Violation)</i> The {@link List}< interface makes the assumption that
	 * the elements are always inserted. This may not be the case with this
	 * implementation.
	 *
	 * @param index the index at which to insert the first element from the
	 *            specified {@link Collection}.
	 * @param c the {@link Collection} containing elements to be added.
	 * @return {@code true} if this {@link UniqueList} changed as a result of
	 *         the call.
	 * @throws IndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		if (c == null || c.isEmpty()) {
			return false;
		}

		// Make a copy before removing items
		ArrayList<E> tmpList = new ArrayList<>();
		for (Iterator<? extends E> iterator = c.iterator(); iterator.hasNext();) {
			E e = iterator.next();
			if (!contains(e)) {
				tmpList.add(e);
			}
		}
		return tmpList.isEmpty() ? false : super.addAll(index, tmpList);
	}

	/**
	 * Replaces the element at the specified position in this {@link UniqueList}
	 * with the specified element. If the specified element already exist in the
	 * same position, no change will be made. If the specified element already
	 * exist in another position that element will be removed. The implication
	 * of this is that if the element already exists in a position lower than
	 * {@code index}, the resulting position of the specified element will be
	 * {@code index - 1}, but the relative position in relation to the other
	 * elements will be as expected.
	 *
	 * @param index the index of the element to replace.
	 * @param element the element to be stored at the specified position.
	 * @return The element previously at the specified position.
	 * @throws IndexOutOfBoundsException {@inheritDoc}.
	 */
	@Override
	public E set(int index, E element) {
		if (index < 0 || index >= size()) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
		}

		int idx = indexOf(element);
		if (idx == index) {
			return element;
		}
		if (idx > -1) {
			if (index > idx) {
				index--;
			}
			remove(element);
		}
		return super.set(index, element);
	}
}
