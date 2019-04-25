/*
 * Universal Media Server, for streaming any medias to DLNA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * A utility class for {@link Collection} utilities.
 */
public class CollectionUtils {

	/**
	 * Not to be instantiated.
	 */
	private CollectionUtils() {
	}

	/**
	 * This class is a container for holding the results of the
	 * {@link CollectionUtils#diff} methods where those items only found in
	 * {@code first} and {@code second} is returned separately.
	 *
	 * @param <T> The type used to store the items.
	 *
	 * @author Nadahar
	 */
	public static class DiffResult<T> {
		private final T firstOnly;
		private final T secondOnly;

		/**
		 * Creates a new instance with the specified items unique for
		 * {@code first} and {@code second}.
		 *
		 * @param firstOnly the items only found in {@code first}.
		 * @param secondOnly the items only found in {@code second}.
		 */
		public DiffResult(T firstOnly, T secondOnly) {
			this.firstOnly = firstOnly;
			this.secondOnly = secondOnly;
		}

		/**
		 * @return The element(s) that only exist(s) in {@code first}.
		 */
		public T getFirstOnly() {
			return firstOnly;
		}

		/**
		 * @return The element(s) that only exist(s) in {@code second}.
		 */
		public T getSecondOnly() {
			return secondOnly;
		}
	}

	/**
	 * Compares two {@link Collection}s of the same type {@code <E>} and returns
	 * those items unique to either of the collections. Items that exist in both
	 * aren't returned.
	 *
	 * @param <E> the type of elements in the {@link Collection}s.
	 * @param first the first {@link Collection} to compare.
	 * @param second the second {@link Collection} to compare.
	 * @return The {@link DiffResult} of type {@code List<E>} containing the
	 *         items unique to either {@link Collection}.
	 */
	public static <E> DiffResult<List<E>> diff(Collection<E> first, Collection<E> second) {
		if ((first == null || first.isEmpty()) && (second == null || second.isEmpty())) {
			return new DiffResult<List<E>>(new ArrayList<E>(), new ArrayList<E>());
		}
		if (first == null || first.isEmpty()) {
			return new DiffResult<List<E>>(new ArrayList<E>(), new ArrayList<E>(second));
		}
		if (second == null || second.isEmpty()) {
			return new DiffResult<List<E>>(new ArrayList<E>(first), new ArrayList<E>());
		}

		ArrayList<E> firstArray = new ArrayList<E>(first);
		firstArray.removeAll(second);
		ArrayList<E> secondArray = new ArrayList<E>(second);
		secondArray.removeAll(first);
		return new DiffResult<List<E>>(firstArray, secondArray);
	}

	/**
	 * Compares two arrays of the same type {@code <E>} and returns those items
	 * unique to either of the collections. Items that exist in both aren't
	 * returned.
	 *
	 * @param <E> the type of elements in the {@link Collection}s.
	 * @param first the first array of {@code E} to compare.
	 * @param second the second array of {@code E} to compare.
	 * @return The {@link DiffResult} of type {@code List<E>} containing the
	 *         items unique to either array.
	 */
	public static <E> DiffResult<List<E>> diff(E[] first, E[] second) {
		if ((first == null || first.length == 0) && (second == null || second.length == 0)) {
			return new DiffResult<List<E>>(new ArrayList<E>(), new ArrayList<E>());
		}
		if (first == null || first.length == 0) {
			return new DiffResult<List<E>>(new ArrayList<E>(), Arrays.asList(second));
		}
		if (second == null || second.length == 0) {
			return new DiffResult<List<E>>(Arrays.asList(first), new ArrayList<E>());
		}

		ArrayList<E> firstArray = new ArrayList<E>(Arrays.asList(first));
		firstArray.removeAll(Arrays.asList(second));
		ArrayList<E> secondArray = new ArrayList<E>(Arrays.asList(second));
		secondArray.removeAll(Arrays.asList(first));
		return new DiffResult<List<E>>(firstArray, secondArray);
	}
}
