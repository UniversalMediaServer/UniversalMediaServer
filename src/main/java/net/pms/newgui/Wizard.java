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

package net.pms.newgui;

import java.lang.reflect.InvocationTargetException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;

/**
 * Wizard to ask users to make the UMS initial setting
 */
public class Wizard {
	private static final Logger LOGGER = LoggerFactory.getLogger(Wizard.class);

	public static void run(final PmsConfiguration configuration) {
		// Total number of questions
		int numberOfQuestions = 5;

		// The current question number
		int currentQuestionNumber = 1;

		String status = new StringBuilder()
			.append(Messages.getString("Wizard.2"))
			.append(" %d ")
			.append(Messages.getString("Wizard.4"))
			.append(" ")
			.append(numberOfQuestions)
			.toString();

		Object[] okOptions = {
				Messages.getString("Dialog.OK")
			};

		Object[] yesNoOptions = {
				Messages.getString("Dialog.YES"),
				Messages.getString("Dialog.NO")
			};

		Object[] networkTypeOptions = {
				Messages.getString("Wizard.8"),
				Messages.getString("Wizard.9"),
				Messages.getString("Wizard.10")
			};

		Object[] defaultOptions = {
				Messages.getString("Wizard.DefaultsYes"),
				Messages.getString("Wizard.DefaultsNo")
			};

		int whetherToSelectDefaultOptions = JOptionPane.showOptionDialog(
			null,
			Messages.getString("Wizard.13"),
			String.format(status, currentQuestionNumber++),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			defaultOptions,
			defaultOptions[1]
		);

		if (whetherToSelectDefaultOptions == JOptionPane.NO_OPTION || whetherToSelectDefaultOptions == JOptionPane.CLOSED_OPTION) {
			configuration.setMinimized(false);
			configuration.setAutomaticMaximumBitrate(true);
			configuration.setMPEG2MainSettings("Automatic (Wired)");
			configuration.setx264ConstantRateFactor("Automatic (Wired)");
			configuration.setHideAdvancedOptions(true);
			configuration.setScanSharedFoldersOnStartup(true);
		} else {
			// Ask if they want UMS to start minimized
			int whetherToStartMinimized = JOptionPane.showOptionDialog(
				null,
				Messages.getString("Wizard.3"),
				String.format(status, currentQuestionNumber++),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				yesNoOptions,
				yesNoOptions[1]
			);

			if (whetherToStartMinimized == JOptionPane.YES_OPTION) {
				configuration.setMinimized(true);
			} else if (whetherToStartMinimized == JOptionPane.NO_OPTION) {
				configuration.setMinimized(false);
			}

			// Ask if they want to hide advanced options
			int whetherToHideAdvancedOptions = JOptionPane.showOptionDialog(
				null,
				Messages.getString("Wizard.11"),
				String.format(status, currentQuestionNumber++),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				yesNoOptions,
				yesNoOptions[0]
			);

			if (whetherToHideAdvancedOptions == JOptionPane.YES_OPTION) {
				configuration.setHideAdvancedOptions(true);
			} else if (whetherToHideAdvancedOptions == JOptionPane.NO_OPTION) {
				configuration.setHideAdvancedOptions(false);
			}

			// Ask if they want to scan shared folders
			int whetherToScanSharedFolders = JOptionPane.showOptionDialog(
				null,
				Messages.getString("Wizard.IsStartupScan"),
				String.format(status, currentQuestionNumber++),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				yesNoOptions,
				yesNoOptions[0]
			);

			if (whetherToScanSharedFolders == JOptionPane.YES_OPTION) {
				configuration.setScanSharedFoldersOnStartup(true);
			} else if (whetherToScanSharedFolders == JOptionPane.NO_OPTION) {
				configuration.setScanSharedFoldersOnStartup(false);
			}

			// Ask to set at least one shared folder
			JOptionPane.showOptionDialog(
				null,
				Messages.getString("Wizard.12"),
				String.format(status, currentQuestionNumber++),
				JOptionPane.OK_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null,
				okOptions,
				okOptions[0]
			);

			try {
				SwingUtilities.invokeAndWait(() -> {
					JFileChooser chooser;
					try {
						chooser = new JFileChooser();
					} catch (Exception ee) {
						chooser = new JFileChooser(new RestrictedFileSystemView());
					}

					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setDialogTitle(Messages.getString("Wizard.12"));
					chooser.setMultiSelectionEnabled(false);
					if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
						configuration.setOnlySharedDirectory(chooser.getSelectedFile().getAbsolutePath());
						// } else {
						// If the user cancels this option, the default directories
						// will be used.
					}
				});
			} catch (InterruptedException | InvocationTargetException e) {
				LOGGER.error("Error when saving folders: ", e);
			}
		}

		// The wizard finished, do not ask them again
		configuration.setRunWizard(false);

		// Save all changes
		try {
			configuration.save();
		} catch (ConfigurationException e) {
			LOGGER.error("Error when saving changed configuration: ", e);
		}
	}
}
