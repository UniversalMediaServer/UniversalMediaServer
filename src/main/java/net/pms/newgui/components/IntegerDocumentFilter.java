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
package net.pms.newgui.components;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import org.apache.commons.lang3.StringUtils;

public class IntegerDocumentFilter extends DocumentFilter {

	private final int minValue, maxValue;
	private final boolean allowBlank;
	private JComponent feedBackTo = null;

	/**
	 * Create an integer {@link DocumentFilter} that only allows integers
	 * between {@code minValue} and {@code maxValue} to be entered.
	 *
	 * @param minValue
	 *            the minimum accepted value.
	 * @param maxValue
	 *            the maximum accepted value.
	 * @param allowBlank
	 *            whether or not a blank value is allowed.
	 * @param feedBackTo
	 *            the {@link JComponent} to provide error feedback to if the
	 *            change is rejected.
	 */
	public IntegerDocumentFilter(int minValue, int maxValue, boolean allowBlank, JComponent feedBackTo) {
		this(minValue, maxValue, allowBlank);
		this.feedBackTo = feedBackTo;
	}

	/**
	 * Create an integer {@link DocumentFilter} that only allows integers
	 * between {@code minValue} and {@code maxValue} to be entered.
	 *
	 * @param minValue
	 *            the minimum accepted value.
	 * @param maxValue
	 *            the maximum accepted value.
	 * @param allowBlank
	 *            whether or not a blank value is allowed.
	 */
	public IntegerDocumentFilter(int minValue, int maxValue, boolean allowBlank) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.allowBlank = allowBlank;
	}

	protected void provideErrorFeedback() {
		if (feedBackTo != null) {
			UIManager.getLookAndFeel().provideErrorFeedback(feedBackTo);
		}
	}

	@Override
	public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {

		if (!StringUtils.isBlank(string)) {
			Document document = fb.getDocument();
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(document.getText(0, document.getLength()));

			stringBuilder.insert(offset, string);

			if (testInteger(stringBuilder.toString())) {
				super.insertString(fb, offset, string, attr);
			} else {
				provideErrorFeedback();
			}
		}
	}

	private boolean testInteger(String s) {
		try {
			if (allowBlank && s.equals("")) {
				return true;
			}
			int i = Integer.parseInt(s);
			if (i >= minValue && i <= maxValue) {
				return true;
			} else {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {

		if (StringUtils.isBlank(text)) {
			text = "";
		}
		Document document = fb.getDocument();
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(document.getText(0, document.getLength()));
		stringBuilder.replace(offset, offset + length, text);

		if (testInteger(stringBuilder.toString())) {
			super.replace(fb, offset, length, text, attrs);
		} else {
			provideErrorFeedback();
		}

	}

	@Override
	public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {

		Document document = fb.getDocument();
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(document.getText(0, document.getLength()));
		stringBuilder.delete(offset, offset + length);
		if (allowBlank && StringUtils.isBlank(stringBuilder)) {
			super.remove(fb, 0, document.getLength());
		} else if (testInteger(stringBuilder.toString())) {
			super.remove(fb, offset, length);
		} else {
			provideErrorFeedback();
		}

	}
}