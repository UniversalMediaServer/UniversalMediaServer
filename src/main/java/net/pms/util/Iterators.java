/*
 * Universal Media Server, for streaming any media to DLNA
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
import java.util.Iterator;
import java.util.List;

/**
 * Helper class to combine several iterators or lists into one iterator
 *
 * @author Nadahar
 */
public class Iterators<E> {

	private List<E> iterators = new ArrayList<>();

	/**
	 * Add all the elements of an iterator
	 * @param iterator the iterator
	 */
	public void addIterator(Iterator<E> iterator) {
		if (iterator != null) {
			while (iterator.hasNext()) {
				iterators.add(iterator.next());
			}
		}
	}

	/**
	 * Add all the elements of a list
	 * @param list the list
	 */
	public void addList(List<E> list) {
		if (list != null) {
			iterators.addAll(list);
		}
	}

	/**
	 * @return the number of elements
	 */
	public int size() {
		return iterators.size();
	}

	/**
	 * Removes all of the elements
	 */
	public void clear() {
		iterators.clear();
	}

	public Iterator<E> combinedIterator() {
		return iterators.iterator();
	}
}
