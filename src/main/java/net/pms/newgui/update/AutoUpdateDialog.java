package net.pms.newgui.update;

import com.sun.jna.Platform;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Observable;
import java.util.Observer;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.UmsConfiguration;
import net.pms.platform.PlatformUtils;
import net.pms.update.AutoUpdater;
import net.pms.update.AutoUpdater.State;
import net.pms.util.FileUtil;

public class AutoUpdateDialog extends JDialog implements Observer {
	private static final long serialVersionUID = 3809427933990495309L;
	private final AutoUpdater autoUpdater;
	private JLabel stateLabel = new JLabel();
	private JLabel hyperLinkLabel = new HyperLinkLabel();
	private JButton okButton = new DownloadButton();
	private JButton cancelButton = new CancelButton();
	private JProgressBar downloadProgressBar = new JProgressBar();
	private static AutoUpdateDialog instance;
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static synchronized void showIfNecessary(Window parent, AutoUpdater autoUpdater, boolean isStartup) {
		if (autoUpdater.isUpdateAvailable() || !isStartup) {
			if (instance == null) {
				instance = new AutoUpdateDialog(parent, autoUpdater);
			}
			instance.setVisible(true);
		}
	}

	AutoUpdateDialog(Window parent, AutoUpdater autoUpdater) {
		super(parent, Messages.getString("UniversalMediaServerAutoUpdate"));
		this.autoUpdater = autoUpdater;
		autoUpdater.addObserver(this);
		initComponents();
		setMinimumSize(new Dimension(0, 160));
		setLocationRelativeTo(parent);
		update();
	}

	private class DownloadButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 4762020878159496712L;

		DownloadButton() {
			super(Messages.getString("Download"));
			setEnabled(false);
			this.setRequestFocusEnabled(false);
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			autoUpdater.getUpdateFromNetwork();
			autoUpdater.runUpdateAndExit();
		}
	}

	private class CancelButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 4762020878159496713L;

		CancelButton() {
			super(Messages.getString("NotNow"));
			setEnabled(true);
			this.setRequestFocusEnabled(false);
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			switch (autoUpdater.getState()) {
				case UPDATE_AVAILABLE:
				case NO_UPDATE_AVAILABLE:
				case ERROR:
					AutoUpdateDialog.this.setVisible(false);
					break;
				case DOWNLOAD_IN_PROGRESS:
					autoUpdater.cancelDownload();
					break;
				default:
					break;
			}
		}
	}

	private class HyperLinkLabel extends JLabel implements MouseListener {
		private static final long serialVersionUID = 4762020878159496714L;

		HyperLinkLabel() {
			super(Messages.getString("ClickHereSeeChangesRelease"));
			setForeground(Color.BLUE.darker());
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(this);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			PlatformUtils.INSTANCE.browseURI(Build.getReleasesPageUrl());
		}

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {
			setText(String.format("<html><a href=''>%s</a></html>", Messages.getString("ClickHereSeeChangesRelease")));
		}

		@Override
		public void mouseExited(MouseEvent e) {
			setText(Messages.getString("ClickHereSeeChangesRelease"));
		}
	}

	@Override
	public void update(Observable source, Object data) {
		if (SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException("Work is probably happening on event thread. Bad.");
		}
		update();
	}

	private void update() {
		SwingUtilities.invokeLater(() -> {
			updateOnGuiThread();
		});
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
			downloadProgressBar.setValue(autoUpdater.getBytesDownloaded());
			downloadProgressBar.setMaximum(autoUpdater.getTotalBytes());
		} else {
			downloadProgressBar.setEnabled(false);
			downloadProgressBar.setValue(0);
			downloadProgressBar.setMaximum(Integer.MAX_VALUE);
		}
	}

	private void updateCancelButton(State state) {
		switch (state) {
			case UPDATE_AVAILABLE:
			case ERROR:
			case NO_UPDATE_AVAILABLE:
				cancelButton.setText(Messages.getString("Close"));
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
				break;
			case DOWNLOAD_IN_PROGRESS:
				cancelButton.setText(Messages.getString("Cancel"));
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
				break;
			default:
				cancelButton.setEnabled(false);
				cancelButton.setVisible(false);
				break;
		}
	}

	private String getStateText() {
		switch (autoUpdater.getState()) {
			case NOTHING_KNOWN:
				return Messages.getString("CheckForUpdatesNotStarted");
			case DOWNLOAD_FINISHED:
				return Messages.getString("DownloadFinished");
			case DOWNLOAD_IN_PROGRESS:
				return Messages.getString("DownloadInProgress");
			case ERROR:
				return getErrorStateText();
			case NO_UPDATE_AVAILABLE:
				return Messages.getString("NoUpdateAvailable");
			case POLLING_SERVER:
				return Messages.getString("ConnectingToServer");
			case UPDATE_AVAILABLE:
				String permissionsReminder = "";

				// See if we have write permission in the program folder. We don't necessarily
				// need admin rights here.
				File file = new File(CONFIGURATION.getProfileDirectory());
				try {
					if (!FileUtil.getFilePermissions(file).isWritable()) {
						permissionsReminder = Messages.getString("ButCantWriteProfileFolder");
						if (Platform.isWindows()) {
							permissionsReminder += "<br>" + Messages.getString("TryRunningAsAdministrator");
						}
						cancelButton.setText(Messages.getString("Close"));
						okButton.setEnabled(false);
						okButton.setVisible(false);
					}
				} catch (FileNotFoundException e) {
					// This should never happen
					permissionsReminder = "\n" + String.format(Messages.getString("XNotFound"), file.getAbsolutePath());
					cancelButton.setText(Messages.getString("Close"));
					okButton.setEnabled(false);
					okButton.setVisible(false);
				}

				return "<html>" + String.format(Messages.getString("VersionXIsAvailable"), autoUpdater.SERVER_PROPERTIES.getLatestVersion()) + permissionsReminder + "</html>";
			default:
				return Messages.getString("UnknownState");
		}
	}

	private String getErrorStateText() {
		if (autoUpdater == null) {
			return Messages.getString("AutoUpdate.9");
		}

		Throwable exception = autoUpdater.getErrorStateCause();
		if (exception == null) {
			return Messages.getString("Error");
		}

		String message = exception.getMessage();
		if (message == null) {
			return Messages.getString("Error");
		}

		return message;
	}

	// Code generated by Matisse
	private void initComponents() {
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(okButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)).addComponent(stateLabel).addComponent(hyperLinkLabel).addComponent(downloadProgressBar, GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)).addContainerGap()));

		layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[]{cancelButton, okButton});

		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(stateLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(hyperLinkLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(downloadProgressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(okButton).addComponent(cancelButton)).addContainerGap()));

		pack();
	}
}
