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
package net.pms.swing.gui.tabs.traces;

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
import net.pms.swing.components.CustomJButton;
import net.pms.util.DbgPacker;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbgPackerComponent implements ActionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbgPacker.class);
	private final DbgPacker dbgPacker;
	private String zippedLogFile;
	private CustomJButton openZip;

	DbgPackerComponent(DbgPacker dbgPacker) {
		this.dbgPacker = dbgPacker;
	}

	public JComponent config() {
		dbgPacker.poll();
		JPanel top = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 5, 0, 5);
		c.ipadx = 5;
		c.gridx = 0;
		c.gridy = 0;
		for (Map.Entry<File, Object> item : dbgPacker.getItems().entrySet()) {
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
			open.setToolTipText((exists ? "" : Messages.getGuiString("Create") + " ") + file.getAbsolutePath());
			open.addActionListener(this);
			c.gridx++;
			c.weightx = 0.0;
			top.add(open, c);
			c.gridx--;
			c.gridy++;
		}
		c.weightx = 2.0;
		CustomJButton debugPack = new CustomJButton(Messages.getGuiString("ZipSelectedFiles"));
		debugPack.setActionCommand("pack");
		debugPack.addActionListener(this);
		top.add(debugPack, c);
		openZip = new CustomJButton(MetalIconFactory.getTreeFolderIcon());
		openZip.setActionCommand("showzip");
		openZip.setToolTipText(Messages.getGuiString("OpenZipLocation"));
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
					if (f.exists() && JOptionPane.showConfirmDialog(null, Messages.getGuiString("OverwriteExistingFile"), "Confirm",
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
				for (Map.Entry<File, Object> item : dbgPacker.getItems().entrySet()) {
					if (item.getValue() != null && ((JCheckBox) item.getValue()).isSelected()) {
						File file = item.getKey();
						LOGGER.debug("Packing {}", file.getAbsolutePath());
						DbgPacker.writeToZip(zos, file);
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
						JOptionPane.showMessageDialog(null, Messages.getGuiString("CouldNotOpenExternalViewerPlease") + e2,
								Messages.getGuiString("Error"), JOptionPane.ERROR_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(null, Messages.getGuiString("CouldNotOpenExternalViewer") + e2,
								Messages.getGuiString("Error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else {
				JOptionPane.showMessageDialog(null,
						String.format(Messages.getGuiString("CantOpenXDoesntExist"), file.getAbsolutePath()), null,
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
				Messages.getGuiString("Options"), JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
				null);
	}
}
