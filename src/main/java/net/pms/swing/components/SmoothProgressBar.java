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

import javax.swing.plaf.ProgressBarUI;

/**
 *
 * A progress bar with smooth transitions
 */
public class SmoothProgressBar extends CustomUIProgressBar {

	private static final long serialVersionUID = 4418306779403459913L;

	public SmoothProgressBar(int min, int max) {
		super(min, max, null);
	}

	public SmoothProgressBar(int min, int max, ProgressBarUI ui) {
		super(min, max, ui);
	}

	@Override
	public void setValue(int n) {
		int v = getValue();
		if (n != v) {
			int step = n > v ? 1 : -1;
			n += step;
			try {
				for (; v != n; v += step) {
					super.setValue(v);
					Thread.sleep(10);
				}
			} catch (InterruptedException e) {
			}
		}
	}
}
