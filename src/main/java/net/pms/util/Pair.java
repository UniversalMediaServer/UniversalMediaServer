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

import java.io.Serializable;
import java.util.Map;
import com.google.common.base.Objects;


/**
 * This class can be used to hold any pair of {@link Object}s and can be handy
 * for example to return two values from a method. It also implements
 * {@link Map.Entry} and {@link Comparable} for easy integration.
 *
 * To use {@link #compareTo(Pair)} both {@link Object}s must also implement
 * {@link Comparable} or be {@code null}. {@code null} values are sorted last.
 * The first {@link Object} is sorted first, the second {@link Object} is sorted
 * only when the first {@link Object} compares as 0 (equals).
 *
 * @param <K> the type of the first {@link Object}.
 * @param <V> the type of the second {@link Object}.
 *
 * @author Nadahar
 */
public class Pair<K, V> implements Map.Entry<K, V>, Comparable<Pair<K, V>>, Serializable {

	private static final long serialVersionUID = 1L;

	/** The first {@link Object} */
	protected final K first;

	/** The second {@link Object} */
	protected final V second;

	/**
	 * Creates a new instance with the specified {@link Object}s.
	 *
	 * @param first the first {@link Object}.
	 * @param second the second {@link Object}.
	 */
	public Pair(K first, V second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * @return The first {@link Object}.
	 */
	public K getFirst() {
		return first;
	}

	@Override
	public K getKey() {
		return first;
	}

	/**
	 * @return The first {@link Object}.
	 */
	public K getLeft() {
		return first;
	}

	/**
	 * @return The second {@link Object}.
	 */
	public V getSecond() {
		return second;
	}

	/**
	 * @return The second {@link Object}.
	 */
	public V getLast() {
		return second;
	}

	@Override
	public V getValue() {
		return second;
	}

	/**
	 * @return The second {@link Object}.
	 */
	public V getRight() {
		return second;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException("Pair is immutable");
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Pair<K, V> o) {
		if (first == null) {
			if (o.first != null) {
				return 1;
			}
		} else if (o.first == null) {
			return -1;
		}
		if (!(first instanceof Comparable<?>) || !(o.first instanceof Comparable<?>)) {
			throw new UnsupportedOperationException("K must implement Comparable");
		}
		int result = ((Comparable<K>) first).compareTo(o.first);
		if (result != 0) {
			return result;
		}
		if (second == null) {
			if (o.second != null) {
				return 1;
			}
		} else if (o.second == null) {
			return -1;
		}
		if (!(second instanceof Comparable<?>) || !(o.second instanceof Comparable<?>)) {
			throw new UnsupportedOperationException("V must implement Comparable");
		}
		return ((Comparable<V>) second).compareTo(o.second);
	}

	@Override
	public int hashCode() {
		// As per Map.Entry API specification
		return
			(getKey() == null ? 0 : getKey().hashCode()) ^
			(getValue() == null ? 0 : getValue().hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Map.Entry<?, ?>)) {
			return false;
		}
		Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
		return Objects.equal(getKey(), other.getKey()) && Objects.equal(getValue(), other.getValue());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append("[");
		if (getFirst() == null) {
			sb.append("null");
		} else if (getFirst() instanceof CharSequence) {
			sb.append("\"").append(getFirst()).append("\"");
		} else {
			sb.append(getFirst());
		}
		sb.append(", ");
		if (getSecond() == null) {
			sb.append("null");
		} else if (getSecond() instanceof CharSequence) {
			sb.append("\"").append(getSecond()).append("\"");
		} else {
			sb.append(getSecond());
		}
		sb.append("]");
		return sb.toString();
	}
}
