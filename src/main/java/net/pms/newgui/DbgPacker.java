package net.pms.newgui;

import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.metal.MetalIconFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.newgui.components.CustomJButton;
import net.pms.util.FileUtil;

public class DbgPacker extends net.pms.util.DbgPacker implements ActionListener {
	private String zippedLogFile;
	private CustomJButton openZip;

	public JComponent config() {
		poll();
		JPanel top = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 5, 0, 5);
		c.ipadx = 5;
		c.gridx = 0;
		c.gridy = 0;
		for (Map.Entry<File, Object> item : items.entrySet()) {
			File file = item.getKey();
			boolean exists = file.exists();
			JCheckBox box;
			if (item.getValue() == null) {
				box = new JCheckBox(file.getName(), exists);
				item.setValue(box);
			} else {
				box = (JCheckBox) item.getValue();
			}
			if (!exists) {
				box.setSelected(false);
				box.setEnabled(false);
			}
			c.weightx = 1.0;
			top.add(box, c);
			CustomJButton open = exists ? new CustomJButton(MetalIconFactory.getTreeLeafIcon()) :
					new CustomJButton("+");
			open.setActionCommand(file.getAbsolutePath());
			open.setToolTipText((exists ? "" : Messages.getString("Create") + " ") + file.getAbsolutePath());
			open.addActionListener(this);
			c.gridx++;
			c.weightx = 0.0;
			top.add(open, c);
			c.gridx--;
			c.gridy++;
		}
		c.weightx = 2.0;
		CustomJButton debugPack = new CustomJButton(Messages.getString("ZipSelectedFiles"));
		debugPack.setActionCommand("pack");
		debugPack.addActionListener(this);
		top.add(debugPack, c);
		openZip = new CustomJButton(MetalIconFactory.getTreeFolderIcon());
		openZip.setActionCommand("showzip");
		openZip.setToolTipText(Messages.getString("OpenZipLocation"));
		openZip.setEnabled(false);
		openZip.addActionListener(this);
		c.gridx++;
		c.weightx = 0.0;
		top.add(openZip, c);
		return top;
	}

	private boolean saveDialog() {
		JFileChooser fc = new JFileChooser() {
			private static final long serialVersionUID = -7279491708128801610L;

			@Override
			public void approveSelection() {
				File f = getSelectedFile();
				if (!f.isDirectory()) {
					if (f.exists() && JOptionPane.showConfirmDialog(null, Messages.getString("OverwriteExistingFile"), "Confirm",
							JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
						return;
					}
					super.approveSelection();
				}
			}
		};
		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				String s = f.getName();
				return f.isDirectory() || (s.endsWith(".zip") || s.endsWith(".ZIP"));
			}

			@Override
			public String getDescription() {
				return "*.zip";
			}
		});
		zippedLogFile = PMS.getConfiguration().getDefaultZippedLogFileFolder();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		Date date = new Date();
		String fileName = "ums_dbg_" + dateFormat.format(date) + ".zip";
		zippedLogFile = FileUtil.appendPathSeparator(zippedLogFile) + fileName;
		fc.setSelectedFile(new File(zippedLogFile));
		if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			zippedLogFile = fc.getSelectedFile().getPath();
			return true;
		}
		return false;
	}

	private void packDbg() {
		if (!saveDialog()) {
			return;
		}
		try {
			try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zippedLogFile))) {
				for (Map.Entry<File, Object> item : items.entrySet()) {
					if (item.getValue() != null && ((JCheckBox) item.getValue()).isSelected()) {
						File file = item.getKey();
						LOGGER.debug("Packing {}", file.getAbsolutePath());
						writeToZip(zos, file);
					}
				}
			}
			openZip.setEnabled(true);
		} catch (Exception e) {
			LOGGER.debug("Error packing zip file: {}", e.getLocalizedMessage());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String str = e.getActionCommand();
		if (str.equals("pack")) {
			packDbg();
		} else {
			// Open: "showzip" - zipped file folder
			// not "showzip" - one of the listed files
			File file = str.equals("showzip") ? new File(zippedLogFile).getParentFile() : new File(str);
			if (file.exists()) {
				try {
					java.awt.Desktop.getDesktop().open(file);
				} catch (IOException e2) {
					LOGGER.warn("Failed to open default desktop application: {}", e2);
					if (Platform.isWindows()) {
						JOptionPane.showMessageDialog(null, Messages.getString("CouldNotOpenExternalViewerPlease") + e2,
								Messages.getString("Error"), JOptionPane.ERROR_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(null, Messages.getString("CouldNotOpenExternalViewer") + e2,
								Messages.getString("Error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else {
				JOptionPane.showMessageDialog(null,
						String.format(Messages.getString("CantOpenXDoesntExist"), file.getAbsolutePath()), null,
						JOptionPane.INFORMATION_MESSAGE);
				reload((JComponent) e.getSource());
			}
		}
	}

	private void reload(JComponent c) {
		// Rebuild and restart
		LOGGER.debug("Reloading...");
		((Window) c.getTopLevelAncestor()).dispose();
		JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(openZip), config(),
				Messages.getString("Options"), JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
				null);
	}
}
