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

import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.UmsConfiguration;
import net.pms.external.update.AutoUpdater;
import net.pms.external.update.AutoUpdater.State;
import net.pms.platform.PlatformUtils;
import net.pms.util.FileUtil;

public class AutoUpdateDialog extends JDialog {

	private static final long serialVersionUID = 3809427933990495309L;
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static AutoUpdateDialog instance;

	private final AutoUpdater autoUpdater;
	private final JLabel stateLabel = new JLabel();
	private final JLabel hyperLinkLabel = new HyperLinkLabel();
	private final JButton okButton = new DownloadButton();
	private final JButton cancelButton = new CancelButton();
	private final JProgressBar downloadProgressBar = new JProgressBar();

	public static synchronized void showIfNecessary(Window parent, AutoUpdater autoUpdater, boolean isStartup) {
		if (autoUpdater.isUpdateAvailable() || !isStartup) {
			if (instance == null) {
				instance = new AutoUpdateDialog(parent, autoUpdater);
			}
			instance.setVisible(true);
		}
	}

	AutoUpdateDialog(Window parent, AutoUpdater autoUpdater) {
		super(parent, Messages.getGuiString("UniversalMediaServerAutoUpdate"));
		this.autoUpdater = autoUpdater;
		AutoUpdater.addChangeListener((ChangeEvent e) -> {
			if (SwingUtilities.isEventDispatchThread()) {
				throw new RuntimeException("Work is probably happening on event thread. Bad.");
			}
			update();
		});
		initComponents();
		setMinimumSize(new Dimension(0, 160));
		setLocationRelativeTo(parent);
		update();
	}

	private class DownloadButton extends JButton {
		DownloadButton() {
			super(Messages.getGuiString("Download"));
			setEnabled(false);
			this.setRequestFocusEnabled(false);
			addActionListener((ActionEvent e) -> {
				autoUpdater.getUpdateFromNetwork();
				autoUpdater.runUpdateAndExit();
			});
		}
	}

	private class CancelButton extends JButton {
		CancelButton() {
			super(Messages.getGuiString("NotNow"));
			setEnabled(true);
			this.setRequestFocusEnabled(false);
			addActionListener((ActionEvent e) -> {
				switch (autoUpdater.getState()) {
					case UPDATE_AVAILABLE, NO_UPDATE_AVAILABLE, ERROR -> AutoUpdateDialog.this.setVisible(false);
					case DOWNLOAD_IN_PROGRESS -> autoUpdater.cancelDownload();
					default -> {
						//nothing to do
					}
				}
			});
		}
	}

	private class HyperLinkLabel extends JLabel {
		HyperLinkLabel() {
			super(Messages.getGuiString("ClickHereSeeChangesRelease"));
			setForeground(Color.BLUE.darker());
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					PlatformUtils.INSTANCE.browseURI(Build.getReleasesPageUrl());
				}
				@Override
				public void mouseEntered(MouseEvent e) {
					setText(String.format("<html><a href=''>%s</a></html>", Messages.getGuiString("ClickHereSeeChangesRelease")));
				}
				@Override
				public void mouseExited(MouseEvent e) {
					setText(Messages.getGuiString("ClickHereSeeChangesRelease"));
				}
			});
		}
	}

	private void update() {
		SwingUtilities.invokeLater(this::updateOnGuiThread);
	}

	private void updateOnGuiThread() {
		State state = autoUpdater.getState();

		if (!SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException("Must be on event thread");
		}

		stateLabel.setText(getStateText());
		okButton.setEnabled(state == State.UPDATE_AVAILABLE);
		cancelButton.setEnabled(state == State.DOWNLOAD_IN_PROGRESS || state == State.UPDATE_AVAILABLE);

		updateCancelButton(state);

		if (state == State.DOWNLOAD_IN_PROGRESS) {
			downloadProgressBar.setEnabled(true);
			downloadProgressBar.setValue(Long.valueOf(autoUpdater.getBytesDownloaded()).intValue());
			downloadProgressBar.setMaximum(Long.valueOf(autoUpdater.getTotalBytes()).intValue());
		} else {
			downloadProgressBar.setEnabled(false);
			downloadProgressBar.setValue(0);
			downloadProgressBar.setMaximum(Integer.MAX_VALUE);
		}
	}

	private void updateCancelButton(State state) {
		switch (state) {
			case UPDATE_AVAILABLE, ERROR, NO_UPDATE_AVAILABLE -> {
				cancelButton.setText(Messages.getGuiString("Close"));
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
			}
			case DOWNLOAD_IN_PROGRESS -> {
				cancelButton.setText(Messages.getGuiString("Cancel"));
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
			}
			default -> {
				cancelButton.setEnabled(false);
				cancelButton.setVisible(false);
			}
		}
	}

	private String getStateText() {
		switch (autoUpdater.getState()) {
			case NOTHING_KNOWN -> {
				return Messages.getGuiString("CheckForUpdatesNotStarted");
			}
			case DOWNLOAD_FINISHED -> {
				return Messages.getGuiString("DownloadFinished");
			}
			case DOWNLOAD_IN_PROGRESS -> {
				return Messages.getGuiString("DownloadInProgress");
			}
			case ERROR -> {
				return getErrorStateText();
			}
			case NO_UPDATE_AVAILABLE -> {
				return Messages.getGuiString("NoUpdateAvailable");
			}
			case POLLING_SERVER -> {
				return Messages.getGuiString("ConnectingToServer");
			}
			case UPDATE_AVAILABLE -> {
				String permissionsReminder = "";

				// See if we have write permission in the program folder. We don't necessarily
				// need admin rights here.
				File file = new File(CONFIGURATION.getProfileDirectory());
				try {
					if (!FileUtil.getFilePermissions(file).isWritable()) {
						permissionsReminder = Messages.getGuiString("ButCantWriteProfileFolder");
						if (Platform.isWindows()) {
							permissionsReminder += "<br>" + Messages.getGuiString("TryRunningAsAdministrator");
						}
						cancelButton.setText(Messages.getGuiString("Close"));
						okButton.setEnabled(false);
						okButton.setVisible(false);
					}
				} catch (FileNotFoundException e) {
					// This should never happen
					permissionsReminder = "\n" + String.format(Messages.getGuiString("XNotFound"), file.getAbsolutePath());
					cancelButton.setText(Messages.getGuiString("Close"));
					okButton.setEnabled(false);
					okButton.setVisible(false);
				}

				return "<html>" + String.format(Messages.getGuiString("VersionXIsAvailable"), autoUpdater.getLatestVersion()) + permissionsReminder + "</html>";
			}
			default -> {
				return Messages.getGuiString("UnknownState");
			}

		}
	}

	private String getErrorStateText() {
		if (autoUpdater == null) {
			return Messages.getGuiString("AutoUpdate.9");
		}

		Throwable exception = autoUpdater.getErrorStateCause();
		if (exception == null) {
			return Messages.getGuiString("Error");
		}

		String message = exception.getMessage();
		if (message == null) {
			return Messages.getGuiString("Error");
		}

		return message;
	}

	private void initComponents() {
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(okButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)).addComponent(stateLabel).addComponent(hyperLinkLabel).addComponent(downloadProgressBar, GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)).addContainerGap()));

		layout.linkSize(SwingConstants.HORIZONTAL, cancelButton, okButton);

		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(stateLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(hyperLinkLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(downloadProgressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(okButton).addComponent(cancelButton)).addContainerGap()));

		pack();
	}

}
