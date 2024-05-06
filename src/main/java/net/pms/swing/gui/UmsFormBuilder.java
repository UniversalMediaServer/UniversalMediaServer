/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.swing.gui;

import com.jgoodies.common.base.Strings;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 * @author Suf@ceS
 */
public class UmsFormBuilder extends FormBuilder {

	protected UmsFormBuilder() {
	}

	/**
	 * Creates and return a new UmsFormBuilder instance.
	 */
	public static UmsFormBuilder create() {
		return new UmsFormBuilder();
	}

	@Override
	public FormBuilder.ComponentAdder addSeparator(boolean expression, String markedText, Object... args) {
		int alignment = isLeftToRight() ? SwingConstants.LEFT : SwingConstants.RIGHT;
		String text = Strings.get(markedText, args);
		JComponent separator = getFactory().createSeparator(text, alignment);
		if (separator.getComponentCount() > 0) {
			Component label = separator.getComponent(0);
			label.setFont(label.getFont().deriveFont(Font.BOLD));
		}
		return addRaw(expression, separator);
	}

	public JComponent createBoldedSeparator(String markedText) {
		int alignment = isLeftToRight() ? SwingConstants.LEFT : SwingConstants.RIGHT;
		JComponent separator = getFactory().createSeparator(markedText, alignment);
		if (separator.getComponentCount() > 0) {
			Component label = separator.getComponent(0);
			label.setFont(label.getFont().deriveFont(Font.BOLD));
		}
		return separator;
	}

	private boolean isLeftToRight() {
		ComponentOrientation orientation = getPanel().getComponentOrientation();
		return orientation.isLeftToRight() || !orientation.isHorizontal();
	}

	@Override
	public UmsFormBuilder layout(FormLayout layout) {
		super.layout(layout);
		return this;
	}

	@Override
	public UmsFormBuilder opaque(boolean b) {
		super.opaque(b);
		return this;
	}

	@Override
	public UmsFormBuilder border(Border border) {
		super.border(border);
		return this;
	}

	public UmsFormBuilder appendRow(RowSpec rowSpec) {
		getLayout().appendRow(rowSpec);
		return this;
	}

}
