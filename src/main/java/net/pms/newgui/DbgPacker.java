package net.pms.newgui;

import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.metal.MetalIconFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.external.DebugPacker;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.logging.LoggingConfig;
import net.pms.newgui.components.CustomJButton;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbgPacker implements ActionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(DbgPacker.class);

	private LinkedHashMap<File, JCheckBox> items;
	private String defaultLogFile, zippedLogFile;
	private CustomJButton openZip;

	public DbgPacker() {
		items = new LinkedHashMap<>();

		HashMap<String, String> logFilePaths = LoggingConfig.getLogFilePaths();
		if (!logFilePaths.isEmpty()) {
			defaultLogFile = LoggingConfig.getLogFilePaths().get("default.log");
			if (defaultLogFile == null) {
				// Just get the path of one of the files as we can't find the default
				Map.Entry<String, String> entry = logFilePaths.entrySet().iterator().next();
				defaultLogFile = entry.getValue();
			}
			zippedLogFile = new File(defaultLogFile).getParent().toString();
		} else {
			// Fall back to getting the default folder
			zippedLogFile = PMS.getConfiguration().getDefaultLogFilePath();
		}
		if (!zippedLogFile.isEmpty()) {
			zippedLogFile = FileUtil.appendPathSeparator(zippedLogFile) + "ums_dbg.zip";
		} else {
			LOGGER.error("Could not find destination folder for packed debug files");
		}
	}

	public JComponent config() {
		poll();
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
			open.setToolTipText((exists ? "" : Messages.getString("DbgPacker.1") + " ") + file.getAbsolutePath());
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
		openZip = new CustomJButton(MetalIconFactory.getTreeFolderIcon());
		openZip.setActionCommand("showzip");
		openZip.setToolTipText(Messages.getString("DbgPacker.3"));
		openZip.setEnabled(false);
		openZip.addActionListener(this);
		c.gridx++;
		c.weightx = 0.0;
		top.add(openZip, c);
		return top;
	}

	private void poll() {
		// call the client callbacks
		for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
			if (listener instanceof DebugPacker) {
				LOGGER.debug("Found client {}",listener.name());
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
		LOGGER.debug("Checking dbgpack property in UMS.conf");
		String f = (String) configuration.getCustomProperty("dbgpack");
		if (f != null) {
			add(f.split(","));
		}

		// add confs of connected renderers
		for (RendererConfiguration r : RendererConfiguration.getConnectedRenderersConfigurations()) {
			add(r.getFile());
			if (((DeviceConfiguration)r).isCustomized()) {
				add(((DeviceConfiguration)r).getParentFile());
			}
		}

		// add core items with the default logfile last (LinkedHashMap preserves insertion order)
		String profileDirectory = configuration.getProfileDirectory();

		// add virtual folders file if it exists
		String vfolders = configuration.getVirtualFoldersFile(null);
		if (StringUtils.isNotEmpty(vfolders)) {
			add(new File(profileDirectory, vfolders));
		}

		add(new File(profileDirectory, "WEB.conf"));
		add(new File(configuration.getProfilePath()));
		if (defaultLogFile != null && !defaultLogFile.isEmpty()){
			add(new File(defaultLogFile + ".prev"));
			add(new File(defaultLogFile));
		}
	}

	private void add(String[] files) {
		for (String file : files) {
			add(new File(file));
		}
	}

	private void add(File file) {
		if (file != null) {
			LOGGER.debug("adding {}",file.getAbsolutePath());
			try {
				items.put(file.getCanonicalFile(), null);
			} catch (IOException e) {
			}
		}
	}

	private void writeToZip(ZipOutputStream out, File f) throws Exception {
		byte[] buf = new byte[1024];
		int len;
		if (!f.exists()) {
			LOGGER.debug("DbgPack file {} does not exist - ignoring",f.getAbsolutePath());
			return;
		}
		try (FileInputStream in = new FileInputStream(f)) {
			out.putNextEntry(new ZipEntry(f.getName()));
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.closeEntry();
		}
	}

	public Set<File> getItems() {
		poll();
		return items.keySet();
	}

	private boolean saveDialog() {
		JFileChooser fc = new JFileChooser() {
			private static final long serialVersionUID = -7279491708128801610L;

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
				for (Map.Entry<File, JCheckBox> item : items.entrySet()) {
					if (item.getValue().isSelected()) {
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
			//   not "showzip" - one of the listed files
			File file = str.equals("showzip") ? new File(zippedLogFile).getParentFile() : new File(str);
			if (file.exists()) {
				try {
					java.awt.Desktop.getDesktop().open(file);
				} catch (IOException e2) {
					LOGGER.warn("Failed to open default desktop application: {}", e2);
					if (Platform.isWindows()) {
						JOptionPane.showMessageDialog(null, Messages.getString("DbgPacker.5") + e2, Messages.getString("TracesTab.6"),JOptionPane.ERROR_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(null, Messages.getString("DbgPacker.6") + e2, Messages.getString("TracesTab.6"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else {
				JOptionPane.showMessageDialog(
					null, String.format(Messages.getString("DbgPacker.7"), file.getAbsolutePath()), null, JOptionPane.INFORMATION_MESSAGE);
				reload((JComponent) e.getSource());
			}
		}
	}

	private void reload(JComponent c) {
		// Rebuild and restart
		LOGGER.debug("Reloading...");
		((Window) c.getTopLevelAncestor()).dispose();
		JOptionPane.showOptionDialog(
			SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
			config(),
			Messages.getString("Dialog.Options"),
			JOptionPane.CLOSED_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			null
		);
	}
}
