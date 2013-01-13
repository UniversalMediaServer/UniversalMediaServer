/* ========================================================================
 * JCommon : a free general purpose class library for the Java(tm) platform
 * ========================================================================
 *
 * (C) Copyright 2000-2005, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jcommon/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * ------------------
 * KeyedComboBoxModel.java
 * ------------------
 * (C) Copyright 2004, by Thomas Morgner and Contributors.
 *
 * Original Author:  Thomas Morgner;
 * Contributor(s):   David Gilbert (for Object Refinery Limited);
 *
 * $Id: KeyedComboBoxModel.java,v 1.6 2006/12/03 15:33:33 taqua Exp $
 *
 * Changes
 * -------
 * 07-Jun-2004 : Added JCommon header (DG);
 *
 */
package net.pms.util;

import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * The KeyedComboBox model allows to define an internal key (the data element)
 * for every entry in the model.
 * <p/>
 * This class is usefull in all cases, where the public text differs from the
 * internal view on the data. A separation between presentation data and
 * processing data is a prequesite for localizing combobox entries. This model
 * does not allow selected elements, which are not in the list of valid
 * elements.
 *
 * @author Thomas Morgner
 * @author mail@tcox.org (Added generics)
 */
public class KeyedComboBoxModel implements ComboBoxModel {
	/**
	 * The internal data carrier to map keys to values and vice versa.
	 */
	private static class ComboBoxItemPair {
		/**
		 * The key.
		 */
		private Object key;
		/**
		 * The value for the key.
		 */
		private Object value;

		/**
		 * Creates a new item pair for the given key and value. The value can be
		 * changed later, if needed.
		 *
		 * @param key   the key
		 * @param value the value
		 */
		public ComboBoxItemPair(final Object key, final Object value) {
			this.key = key;
			this.value = value;
		}

		/**
		 * Returns the key.
		 *
		 * @return the key.
		 */
		public Object getKey() {
			return key;
		}

		/**
		 * Returns the value.
		 *
		 * @return the value for this key.
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * Redefines the value stored for that key.
		 *
		 * @param value the new value.
		 */
		@SuppressWarnings("unused")
		public void setValue(final Object value) {
			this.value = value;
		}
	}
	/**
	 * The index of the selected item.
	 */
	private int selectedItemIndex;
	private Object selectedItemValue;
	/**
	 * The data (contains ComboBoxItemPairs).
	 */
	private ArrayList<ComboBoxItemPair> data;
	/**
	 * The listeners.
	 */
	private ArrayList<ListDataListener> listdatalistener;
	/**
	 * The cached listeners as array.
	 */
	private transient ListDataListener[] tempListeners;
	private boolean allowOtherValue;

	/**
	 * Creates a new keyed combobox model.
	 */
	public KeyedComboBoxModel() {
		data = new ArrayList<ComboBoxItemPair>();
		listdatalistener = new ArrayList<ListDataListener>();
	}

	/**
	 * Creates a new keyed combobox model for the given keys and values. Keys
	 * and values must have the same number of items.
	 *
	 * @param keys   the keys
	 * @param values the values
	 */
	public KeyedComboBoxModel(final Object[] keys, final Object[] values) {
		this();
		setData(keys, values);
	}

	/**
	 * Replaces the data in this combobox model. The number of keys must be
	 * equals to the number of values.
	 *
	 * @param keys   the keys
	 * @param values the values
	 */
	public void setData(final Object[] keys, final Object[] values) {
		if (values.length != keys.length) {
			throw new IllegalArgumentException("Values and text must have the same length.");
		}

		data.clear();
		data.ensureCapacity(keys.length);

		for (int i = 0; i < values.length; i++) {
			add(keys[i], values[i]);
		}

		selectedItemIndex = -1;
		final ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, data.size() - 1);
		fireListDataEvent(evt);
	}

	/**
	 * Notifies all registered list data listener of the given event.
	 *
	 * @param evt the event.
	 */
	protected synchronized void fireListDataEvent(final ListDataEvent evt) {
		if (tempListeners == null) {
			tempListeners = listdatalistener.toArray(new ListDataListener[listdatalistener.size()]);
		}
		for (int i = 0; i < tempListeners.length; i++) {
			final ListDataListener l = tempListeners[i];
			if (l != null && evt != null) {
				l.contentsChanged(evt);
			}
		}
	}

	/**
	 * Returns the selected item.
	 *
	 * @return The selected item or <code>null</code> if there is no selection
	 */
	public Object getSelectedItem() {
		return selectedItemValue;
	}

	/**
	 * Defines the selected key. If the object is not in the list of values, no
	 * item gets selected.
	 *
	 * @param anItem the new selected item.
	 */
	public void setSelectedKey(final Object anItem) {
		if (anItem == null) {
			selectedItemIndex = -1;
			selectedItemValue = null;
		} else {
			final int newSelectedItem = findDataElementIndex(anItem);
			if (newSelectedItem == -1) {
				selectedItemIndex = -1;
				selectedItemValue = null;
			} else {
				selectedItemIndex = newSelectedItem;
				selectedItemValue = getElementAt(selectedItemIndex);
			}
		}
		fireListDataEvent(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1));
	}

	/**
	 * Set the selected item. The implementation of this  method should notify
	 * all registered <code>ListDataListener</code>s that the contents have
	 * changed.
	 *
	 * @param anItem the list object to select or <code>null</code> to clear the
	 *               selection
	 */
	public void setSelectedItem(final Object anItem) {
		if (anItem == null) {
			selectedItemIndex = -1;
			selectedItemValue = null;
		} else {
			final int newSelectedItem = findElementIndex(anItem);
			if (newSelectedItem == -1) {
				if (isAllowOtherValue()) {
					selectedItemIndex = -1;
					selectedItemValue = anItem;
				} else {
					selectedItemIndex = -1;
					selectedItemValue = null;
				}
			} else {
				selectedItemIndex = newSelectedItem;
				selectedItemValue = getElementAt(selectedItemIndex);
			}
		}
		fireListDataEvent(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1));
	}

	private boolean isAllowOtherValue() {
		return allowOtherValue;
	}

	public void setAllowOtherValue(final boolean allowOtherValue) {
		this.allowOtherValue = allowOtherValue;
	}

	/**
	 * Adds a listener to the list that's notified each time a change to the data
	 * model occurs.
	 *
	 * @param l the <code>ListDataListener</code> to be added
	 */
	public synchronized void addListDataListener(final ListDataListener l) {
		listdatalistener.add(l);
		tempListeners = null;
	}

	/**
	 * Returns the value at the specified index.
	 *
	 * @param index the requested index
	 * @return the value at <code>index</code>
	 */
	public Object getElementAt(final int index) {
		if (index >= data.size()) {
			return null;
		}

		final ComboBoxItemPair datacon = data.get(index);
		if (datacon == null) {
			return null;
		}
		return datacon.getValue();
	}

	/**
	 * Returns the key from the given index.
	 *
	 * @param index the index of the key.
	 * @return the the key at the specified index.
	 */
	public Object getKeyAt(final int index) {
		if (index >= data.size()) {
			return null;
		}

		if (index < 0) {
			return null;
		}

		final ComboBoxItemPair datacon = data.get(index);
		if (datacon == null) {
			return null;
		}
		return datacon.getKey();
	}

	/**
	 * Returns the selected data element or null if none is set.
	 *
	 * @return the selected data element.
	 */
	public Object getSelectedKey() {
		return getKeyAt(selectedItemIndex);
	}

	/**
	 * Returns the length of the list.
	 *
	 * @return the length of the list
	 */
	public int getSize() {
		return data.size();
	}

	/**
	 * Removes a listener from the list that's notified each time a change to
	 * the data model occurs.
	 *
	 * @param l the <code>ListDataListener</code> to be removed
	 */
	public void removeListDataListener(final ListDataListener l) {
		listdatalistener.remove(l);
		tempListeners = null;
	}

	/**
	 * Searches an element by its data value. This method is called by the
	 * setSelectedItem method and returns the first occurence of the element.
	 *
	 * @param anItem the item
	 * @return the index of the item or -1 if not found.
	 */
	private int findDataElementIndex(final Object anItem) {
		if (anItem == null) {
			throw new NullPointerException("Item to find must not be null");
		}

		for (int i = 0; i < data.size(); i++) {
			final ComboBoxItemPair datacon = data.get(i);
			if (anItem.equals(datacon.getKey())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Tries to find the index of element with the given key. The key must not
	 * be null.
	 *
	 * @param key the key for the element to be searched.
	 * @return the index of the key, or -1 if not found.
	 */
	public int findElementIndex(final Object key) {
		if (key == null) {
			throw new NullPointerException("Item to find must not be null");
		}

		for (int i = 0; i < data.size(); i++) {
			final ComboBoxItemPair datacon = data.get(i);
			if (key.equals(datacon.getValue())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Removes an entry from the model.
	 *
	 * @param key the key
	 */
	public void removeDataElement(final Object key) {
		final int idx = findDataElementIndex(key);
		if (idx == -1) {
			return;
		}

		data.remove(idx);
		final ListDataEvent evt = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, idx, idx);
		fireListDataEvent(evt);
	}

	/**
	 * Adds a new entry to the model.
	 *
	 * @param key    the key
	 * @param cbitem the display value.
	 */
	public void add(final Object key, final Object cbitem) {
		final ComboBoxItemPair con = new ComboBoxItemPair(key, cbitem);
		data.add(con);
		final ListDataEvent evt = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, data.size() - 2, data.size() - 2);
		fireListDataEvent(evt);
	}

	/**
	 * Removes all entries from the model.
	 */
	public void clear() {
		final int size = getSize();
		data.clear();
		final ListDataEvent evt = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, size - 1);
		fireListDataEvent(evt);
	}
}
