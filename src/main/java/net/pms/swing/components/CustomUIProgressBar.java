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
package net.pms.swing.components;

import java.io.Serializable;
import javax.swing.JProgressBar;
import javax.swing.plaf.ProgressBarUI;

/**
 * A JProgressBar with a persistent custom ProgressBarUI.
 *
 * This is to prevent replacement of the initial custom ui with the laf's
 * default ProgressBarUI as a result of future invocations of
 * JProgressBar.UpdateUI().
 */
public class CustomUIProgressBar extends JProgressBar implements Serializable {

	private final transient ProgressBarUI customUi;

	public CustomUIProgressBar(int min, int max, ProgressBarUI ui) {
		super(min, max);
		this.customUi = ui;
		setUI(ui);
	}

	@Override
	public final void setUI(ProgressBarUI ui) {
		// Always prefer our own ui if we have one
		super.setUI(this.customUi != null ? this.customUi : ui);
	}
}
