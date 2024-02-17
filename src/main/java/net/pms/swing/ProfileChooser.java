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
package net.pms.swing;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import net.pms.Messages;

public class ProfileChooser {

	/**
	 * This class is not meant to be instantiated.
	 */
	private ProfileChooser() {
	}

	private static class ProfileChooserFileFilter extends FileFilter {
		// XXX: this is more restrictive than the environment variable/property (which accept any filename)
		// but should simplify things in the UI
		@Override
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().toLowerCase().endsWith(".conf");
		}

		@Override
		public String getDescription() {
			return Messages.getGuiString("ProfileFileOrFolder");
		}
	}

	public static void display() {
		final JFileChooser fc = new JFileChooser();

		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setDialogTitle(Messages.getGuiString("UniversalMediaServerProfileChooser"));
		fc.setFileFilter(new ProfileChooserFileFilter());

		int returnVal = fc.showDialog(null, Messages.getGuiString("Select"));

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			System.setProperty("ums.profile.path", file.getAbsolutePath());
		} // else the open command was cancelled by the user
	}

}
