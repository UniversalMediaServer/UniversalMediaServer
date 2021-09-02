package net.pms.newgui.update;

import com.sun.jna.Platform;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Observable;
import java.util.Observer;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.PmsConfiguration;
import net.pms.update.AutoUpdater;
import net.pms.update.AutoUpdater.State;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoUpdateDialog extends JDialog implements Observer {
	private static final long serialVersionUID = 3809427933990495309L;
	private final AutoUpdater autoUpdater;
	private JLabel stateLabel = new JLabel();
	private JLabel hyperLinkLabel = new HyperLinkLabel();
	private JButton okButton = new DownloadButton();
	private JButton cancelButton = new CancelButton();
	private JProgressBar downloadProgressBar = new JProgressBar();
	private static AutoUpdateDialog instance;
	private static final Logger LOGGER = LoggerFactory.getLogger(AutoUpdateDialog.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public synchronized static void showIfNecessary(Window parent, AutoUpdater autoUpdater, boolean isStartup) {
		if (autoUpdater.isUpdateAvailable() || !isStartup) {
			if (instance == null) {
				instance = new AutoUpdateDialog(parent, autoUpdater);
			}
			instance.setVisible(true);
		}
	}

	AutoUpdateDialog(Window parent, AutoUpdater autoUpdater) {
		super(parent, Messages.getString("AutoUpdate.0"));
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
			super(Messages.getString("AutoUpdate.10"));
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
			super(Messages.getString("AutoUpdate.11"));
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

	private class HyperLinkLabel extends JLabel implements MouseListener{
		private static final long serialVersionUID = 4762020878159496714L;

		HyperLinkLabel(){
			super(Messages.getString("AutoUpdate.14"));
			setForeground(Color.BLUE.darker());
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(this);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(new URI(Build.getReleasesPageUrl()));
			} catch (IOException | URISyntaxException ex){
				LOGGER.error(ex.getMessage());
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {
			setText(String.format("<html><a href=''>%s</a></html>",Messages.getString("AutoUpdate.14")));
		}

		@Override
		public void mouseExited(MouseEvent e) {
			setText(Messages.getString("AutoUpdate.14"));
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
				cancelButton.setText(Messages.getString("Dialog.Close"));
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
				break;
			case DOWNLOAD_IN_PROGRESS:
				cancelButton.setText(Messages.getString("NetworkTab.45"));
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
				return Messages.getString("AutoUpdate.1");
			case DOWNLOAD_FINISHED:
				return Messages.getString("AutoUpdate.2");
			case DOWNLOAD_IN_PROGRESS:
				return Messages.getString("AutoUpdate.3");
			case ERROR:
				return getErrorStateText();
			case NO_UPDATE_AVAILABLE:
				return Messages.getString("AutoUpdate.4");
			case POLLING_SERVER:
				return Messages.getString("AutoUpdate.5");
			case UPDATE_AVAILABLE:
				String permissionsReminder = "";

				// See if we have write permission in the program folder. We don't necessarily
				// need admin rights here.
				File file = new File(CONFIGURATION.getProfileDirectory());
				try {
					if (!FileUtil.getFilePermissions(file).isWritable()) {
						permissionsReminder = Messages.getString("AutoUpdate.NoPermissions");
						if (Platform.isWindows()) {
							permissionsReminder += "<br>" + Messages.getString("AutoUpdate.13");
						}
						cancelButton.setText(Messages.getString("Dialog.Close"));
						okButton.setEnabled(false);
						okButton.setVisible(false);
					}
				} catch (FileNotFoundException e) {
					// This should never happen
					permissionsReminder = "\n" + String.format(Messages.getString("TracesTab.21"), file.getAbsolutePath());
					cancelButton.setText(Messages.getString("Dialog.Close"));
					okButton.setEnabled(false);
					okButton.setVisible(false);
				}

				return "<html>" + String.format(Messages.getString("AutoUpdate.VersionXIsAvailable"), autoUpdater.SERVER_PROPERTIES.getLatestVersion()) + permissionsReminder + "</html>";
			default:
				return Messages.getString("AutoUpdate.8");
		}
	}

	private String getErrorStateText() {
		if (autoUpdater == null) {
			return Messages.getString("AutoUpdate.9");
		}

		Throwable exception = autoUpdater.getErrorStateCause();
		if (exception == null) {
			return Messages.getString("Dialog.Error");
		}

		String message = exception.getMessage();
		if (message == null) {
			return Messages.getString("Dialog.Error");
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
