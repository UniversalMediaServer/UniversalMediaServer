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
package net.pms.swing.gui.tabs.transcoding;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import net.pms.Messages;

public class FontFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		String name = f.getName().toUpperCase();
		if (name.endsWith("TTC") || name.endsWith("TTF") || name.endsWith(".DESC")) {
			return true;
		}
		return f.isDirectory();
	}

	@Override
	public String getDescription() {
		return Messages.getGuiString("TruetypeFonts");
	}

}
