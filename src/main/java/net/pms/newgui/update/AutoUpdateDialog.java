package net.pms.newgui.update;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.pms.update.AutoUpdater;
import net.pms.update.AutoUpdater.State;

public class AutoUpdateDialog extends JDialog implements Observer {
	private static final long serialVersionUID = 3809427933990495309L;
	private final AutoUpdater autoUpdater;
	private JLabel stateLabel = new JLabel();
	private JButton okButton = new DownloadButton();
	private JButton cancelButton = new CancelButton();
	private JProgressBar downloadProgressBar = new JProgressBar();
	private static AutoUpdateDialog instance;

	public synchronized static void showIfNecessary(Window parent, AutoUpdater autoUpdater) {
		if (autoUpdater.isUpdateAvailable()) {
			if (instance == null) {
				instance = new AutoUpdateDialog(parent, autoUpdater);
			}
			instance.setVisible(true);
		}
	}

	AutoUpdateDialog(Window parent, AutoUpdater autoUpdater) {
		super(parent, "Universal Media Server Auto Update");
		this.autoUpdater = autoUpdater;
		autoUpdater.addObserver(this);
		initComponents();
		setResizable(false);
		setMinimumSize(new Dimension(0, 120));
		setLocationRelativeTo(parent);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		update();
	}

	private class DownloadButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 4762020878159496712L;

		DownloadButton() {
			super("Download");
			setEnabled(false);
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			autoUpdater.getUpdateFromNetwork();

			if (!autoUpdater.isDownloadCancelled()) {
				autoUpdater.runUpdateAndExit();
			}
		}
	}

	private class CancelButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 4762020878159496713L;

		CancelButton() {
			super("Not Now");
			setEnabled(true);
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			switch (autoUpdater.getState()) {
				case UPDATE_AVAILABLE:
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

	@Override
	public void update(Observable source, Object data) {
		if (SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException("Work is probably happening on event thread. Bad.");
		}
		update();
	}

	private void update() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateOnGuiThread();
			}
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
				cancelButton.setText("Not Now");
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
				break;
			case DOWNLOAD_IN_PROGRESS:
				cancelButton.setText("Cancel");
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
				break;
			case ERROR:
				cancelButton.setText("Close");
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
				return "Check for updates not started";
			case DOWNLOAD_FINISHED:
				return "Download finished";
			case DOWNLOAD_IN_PROGRESS:
				return "Download in progress";
			case ERROR:
				return getErrorStateText();
			case NO_UPDATE_AVAILABLE:
				return "No update available";
			case POLLING_SERVER:
				return "Connecting to server";
			case UPDATE_AVAILABLE:
				return "An update is available";
			default:
				return "Unknown state";
		}
	}

	private String getErrorStateText() {
		if (autoUpdater == null) {
			return "No auto updater";
		}

		Throwable exception = autoUpdater.getErrorStateCause();
		if (exception == null) {
			return "Error";
		}

		String message = exception.getMessage();
		if (message == null) {
			return "Error";
		}

		return message;
	}

	// Code generated by Matisse
	private void initComponents() {
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(stateLabel).addComponent(downloadProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)).addContainerGap()));

		layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[]{cancelButton, okButton});

		layout.setVerticalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(stateLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(downloadProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(okButton).addComponent(cancelButton)).addContainerGap()));

		pack();
	}
}
