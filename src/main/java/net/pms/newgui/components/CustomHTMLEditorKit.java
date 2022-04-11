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

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;


/**
 * This is a custom {@link HTMLEditorKit} that doesn't use the application
 * shared {@link StyleSheet}. If using the standard {@link HTMLEditorKit}
 * any changes to the {@link StyleSheet} will apply to all
 * {@link HTMLEditorKit}s in the application, practically rendering
 * {@link StyleSheet}s useless.
 *
 * @author Nadahar
 * @since 5.4.0
 */
public class CustomHTMLEditorKit extends HTMLEditorKit {

	private static final long serialVersionUID = -4110333075630471497L;
	private StyleSheet customStyleSheet;

	@Override
	public void setStyleSheet(StyleSheet styleSheet) {
		customStyleSheet = styleSheet;
	}
	@Override
	public StyleSheet getStyleSheet() {
		if (customStyleSheet == null) {
			customStyleSheet = super.getStyleSheet();
		}
		return customStyleSheet;
	}
}
