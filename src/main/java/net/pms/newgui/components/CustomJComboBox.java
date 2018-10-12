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
package net.pms.newgui.components;

import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 * A subclass of {@link javax.swing.JComboBox} with a custom <code>ToolTip</code> handler
 */

public class CustomJComboBox<E> extends JComboBox<E> {

	private static final long serialVersionUID = -45894969088130959L;

	public CustomJComboBox() {
		super();
	}

	public CustomJComboBox(ComboBoxModel<E> aModel) {
		super(aModel);
	}

	public CustomJComboBox(E[] items) {
		super(items);
	}

	public CustomJComboBox(Vector<E> items) {
	    super(items);
	}
}
