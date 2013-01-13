package net.pms.newgui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.metal.MetalIconFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.external.DebugPacker;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.logging.LoggingConfigFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbgPacker implements ActionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(TracesTab.class);

	private boolean init;
	private LinkedHashMap<File, JCheckBox> items;
	private String debug_log, dbg_zip;

	public DbgPacker() {
		init = true;
		items = new LinkedHashMap<File, JCheckBox>();
		debug_log = LoggingConfigFileLoader.getLogFilePaths().get("debug.log");
		dbg_zip = debug_log.replace("debug.log", "ums_dbg.zip");
	}

	public JComponent config() {
		if (init) {
			poll();
			init = false;
		}
		JPanel top = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 5, 0, 5);
		c.ipadx = 5;
		c.gridx = 0;
		c.gridy = 0;
		for (Map.Entry<File, JCheckBox> item : items.entrySet()) {
			File file = item.getKey();
			boolean exists = file.exists();
			JCheckBox box = item.getValue();
			if (box == null) {
				box = new JCheckBox(file.getName(), exists);
				item.setValue(box);
			}
			if (!exists) {
				box.setSelected(false);
				box.setEnabled(false);
			}
			c.weightx = 1.0;
			top.add(box, c);
			CustomJButton open = exists ? new CustomJButton(MetalIconFactory.getTreeLeafIcon()) : new CustomJButton("+");
			open.setActionCommand(file.getAbsolutePath());
			open.setToolTipText((exists ? ""
				: Messages.getString("DbgPacker.1") + " ") + file.getAbsolutePath());
			open.addActionListener(this);
			c.gridx++;
			c.weightx = 0.0;
			top.add(open, c);
			c.gridx--;
			c.gridy++;
		}
		c.weightx = 2.0;
		CustomJButton debugPack = new CustomJButton(Messages.getString("DbgPacker.2"));
		debugPack.setActionCommand("pack");
		debugPack.addActionListener(this);
		top.add(debugPack, c);
		CustomJButton open = new CustomJButton(MetalIconFactory.getTreeFolderIcon());
		open.setActionCommand("showzip");
		open.setToolTipText(Messages.getString("DbgPacker.3"));
		open.addActionListener(this);
		c.gridx++;
		c.weightx = 0.0;
		top.add(open, c);
		return top;
	}

	private void poll() {
		// call the client callbacks
		for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
			if (listener instanceof DebugPacker) {
				LOGGER.debug("found client " + listener.name());
				Object obj = ((DebugPacker) listener).dbgpack_cb();
				if (obj instanceof String) {
					add(((String) obj).split(","));
				} else if (obj instanceof String[]) {
					add((String[]) obj);
				}
			}
		}
		PmsConfiguration configuration = PMS.getConfiguration();
		// check dbgpack property in UMS.conf
		LOGGER.debug("checking dbgpack property in UMS.conf");
		String f = (String) configuration.getCustomProperty("dbgpack");
		if (f != null) {
			add(f.split(","));
		}
		// add core items with debug.log last (LinkedHashMap preserves insertion order)
		String profileDirectory = configuration.getProfileDirectory();
		add(new File(profileDirectory, "WEB.conf"));
		add(new File(configuration.getProfilePath()));
		add(new File(debug_log));
	}

	private void add(String[] files) {
		for (String file : files) {
			LOGGER.debug("adding " + file);
			try {
				items.put(new File(file).getCanonicalFile(), null);
			} catch (IOException e) {
			}
		}
	}

	private void add(File file) {
		LOGGER.debug("adding " + file.getAbsolutePath());
		try {
			items.put(file.getCanonicalFile(), null);
		} catch (IOException e) {
		}
	}

	private void writeToZip(ZipOutputStream out, File f) throws Exception {
		byte[] buf = new byte[1024];
		int len;
		if (!f.exists()) {
			LOGGER.debug("DbgPack file " + f.getAbsolutePath() + " does not exist - ignoring");
			return;
		}
		FileInputStream in = new FileInputStream(f);
		out.putNextEntry(new ZipEntry(f.getName()));
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.closeEntry();
		in.close();
	}

	private boolean saveDialog() {
		JFileChooser fc = new JFileChooser() {
			@Override
			public void approveSelection() {
				File f = getSelectedFile();
				if (!f.isDirectory()) {
					if (f.exists() && JOptionPane.showConfirmDialog(null, Messages.getString("DbgPacker.4"), "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
						return;
					}
					super.approveSelection();
				}
			}
		};
		fc.setFileFilter(
			new FileFilter() {
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
		fc.setSelectedFile(new File(dbg_zip));
		if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			dbg_zip = fc.getSelectedFile().getPath();
			return true;
		}
		return false;
	}

	private void packDbg() {
		if (!saveDialog()) {
			return;
		}
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dbg_zip));
			for (Map.Entry<File, JCheckBox> item : items.entrySet()) {
				if (item.getValue().isSelected()) {
					File file = item.getKey();
					LOGGER.debug("packing " + file.getAbsolutePath());
					writeToZip(zos, file);
				}
			}
			zos.close();
		} catch (Exception e) {
			LOGGER.debug("error packing zip file " + e);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String str = e.getActionCommand();
		if (str.equals("pack")) {
			packDbg();
		} else {
			// open
			try {
				File file = str.equals("showzip") ? new File(dbg_zip).getParentFile() : new File(str);
				boolean exists = file.isFile() && file.exists();
				if (!exists) {
					file.createNewFile();
				}
				java.awt.Desktop.getDesktop().open(file);
				if (!exists) {
					reload((JComponent) e.getSource());
				}
			} catch (IOException e1) {
				LOGGER.debug(String.format("Failed to open '%s' in default desktop application %s", str, e1));
			}
		}
	}

	private void reload(JComponent c) {
		// rebuild and restart
		LOGGER.debug("reloading.");
		init = true;
		((Window) c.getTopLevelAncestor()).dispose();
		JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
			config(), "Options", JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
	}
}
