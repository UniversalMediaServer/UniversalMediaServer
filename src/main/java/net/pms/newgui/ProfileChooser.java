package net.pms.newgui;

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
			return Messages.getString("ProfileFileOrFolder");
		}
	}

	public static void display() {
		final JFileChooser fc = new JFileChooser();

		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setDialogTitle(Messages.getString("UniversalMediaServerProfileChooser"));
		fc.setFileFilter(new ProfileChooserFileFilter());

		int returnVal = fc.showDialog(null, Messages.getString("Select"));

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			System.setProperty("ums.profile.path", file.getAbsolutePath());
		} // else the open command was cancelled by the user
	}
}
