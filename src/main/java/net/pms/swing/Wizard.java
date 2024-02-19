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

import java.lang.reflect.InvocationTargetException;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.swing.components.RestrictedFileSystemView;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard to ask users to make the UMS initial setting
 */
public class Wizard {

	private static final Logger LOGGER = LoggerFactory.getLogger(Wizard.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private Wizard() {
	}

	public static void run(final UmsConfiguration configuration) {
		ImageIcon icon = new ImageIcon(SwingUtil.getAppIconImage());

		// Total number of questions
		int numberOfQuestions = 5;

		// The current question number
		int currentQuestionNumber = 0;

		String status = new StringBuilder()
				.append(Messages.getGuiString("ConfigurationWizardQuestion"))
				.append(" %d ")
				.append(Messages.getGuiString("Of"))
				.append(" ")
				.append(numberOfQuestions)
				.toString();

		Object[] okOptions = {
			Messages.getGuiString("OK")
		};

		Object[] yesNoOptions = {
			Messages.getGuiString("Yes"),
			Messages.getGuiString("No")
		};

		Object[] defaultOptions = {
			Messages.getGuiString("YesRunTheWizard"),
			Messages.getGuiString("NoUseTheDefaults")
		};

		int whetherToSelectDefaultOptions = JOptionPane.showOptionDialog(null,
				Messages.getGuiString("WouldYouLikeRunWizard"),
				String.format(status, ++currentQuestionNumber),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				icon,
				defaultOptions,
				defaultOptions[1]
		);

		if (whetherToSelectDefaultOptions == JOptionPane.NO_OPTION || whetherToSelectDefaultOptions == JOptionPane.CLOSED_OPTION) {
			configuration.setAutomaticMaximumBitrate(true);
			configuration.setMPEG2MainSettings("Automatic (Wired)");
			configuration.setx264ConstantRateFactor("Automatic (Wired)");
			configuration.setHideAdvancedOptions(true);
			configuration.setScanSharedFoldersOnStartup(true);
		} else {
			// Ask if they want to hide advanced options
			int whetherToHideAdvancedOptions = JOptionPane.showOptionDialog(null,
					Messages.getGuiString("ShouldUmsHideAdvancedOptions"),
					String.format(status, ++currentQuestionNumber),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					icon,
					yesNoOptions,
					yesNoOptions[0]
			);

			if (whetherToHideAdvancedOptions == JOptionPane.YES_OPTION) {
				configuration.setHideAdvancedOptions(true);
			} else if (whetherToHideAdvancedOptions == JOptionPane.NO_OPTION) {
				configuration.setHideAdvancedOptions(false);
			}

			// Ask if they want to scan shared folders
			int whetherToScanSharedFolders = JOptionPane.showOptionDialog(null,
					Messages.getGuiString("ShouldUmsScanFoldersStartup"),
					String.format(status, ++currentQuestionNumber),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					icon,
					yesNoOptions,
					yesNoOptions[0]
			);

			if (whetherToScanSharedFolders == JOptionPane.YES_OPTION) {
				configuration.setScanSharedFoldersOnStartup(true);
			} else if (whetherToScanSharedFolders == JOptionPane.NO_OPTION) {
				configuration.setScanSharedFoldersOnStartup(false);
			}

			// Ask to set at least one shared folder
			JOptionPane.showOptionDialog(null,
					Messages.getGuiString("FinallyChooseFolderShare"),
					String.format(status, ++currentQuestionNumber),
					JOptionPane.OK_OPTION,
					JOptionPane.INFORMATION_MESSAGE,
					icon,
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
					chooser.setDialogTitle(Messages.getGuiString("FinallyChooseFolderShare"));
					chooser.setMultiSelectionEnabled(false);
					if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
						SharedContentConfiguration.addFolderShared(chooser.getSelectedFile());
						// } else {
						// If the user cancels this option, the default directories
						// will be used.
					}
				});
			} catch (InvocationTargetException e) {
				LOGGER.error("Error when saving folders: ", e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
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
