package net.pms.newgui;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.border.EmptyBorder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.CellConstraints;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.DeviceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererPanel extends JPanel {
	private static final long serialVersionUID = 5130146620433713605L;

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererPanel.class);

	private RendererConfiguration renderer;
	private CellConstraints cc = new CellConstraints();
	private static RowSpec rspec = RowSpec.decode("center:pref");
	private JPanel editBar;
	private boolean ready = false;

	public RendererPanel(final RendererConfiguration renderer) {
		this.renderer = renderer;

		FormLayout layout = new FormLayout("left:pref, 400:grow");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(new EmptyBorder(10,10,10,10));
		int y=0;

		builder.appendRow(rspec);
		editBar = new JPanel();
		editBar.setLayout(new BoxLayout(editBar, BoxLayout.X_AXIS));
		builder.add(editBar,  cc.xyw(1, ++y, 2));
		if (renderer.loaded && ! renderer.isFileless()) {
			buildEditBar(false);
		}
		builder.appendRow(rspec);
		builder.addLabel(" ", cc.xy(1, ++y));
		if (renderer.isUpnpConnected()) {
			y = addMap(renderer.getUpnpDetails(), builder, y);
			y = addStrings("Services", WordUtils.wrap(StringUtils.join(renderer.getUpnpServices(), ", "), 60).split("\n"),
				builder, y);

			if (renderer.isUpnpControllable()) {
				builder.appendRow(rspec);
				builder.addLabel(" ", cc.xy(1, ++y));
				builder.appendRow(rspec);
				builder.addSeparator("UPNP Controls", cc.xyw(1, ++y, 2));
				builder.appendRow(rspec);
				builder.add(new PlayerControlPanel(renderer.getPlayer()), cc.xyw(1, ++y, 2));
			}
		} else {
			y = addItem("name", renderer.getRendererName(), builder, y);
			if (!(renderer.getAddress() == null)) {
				y = addItem("address", renderer.getAddress().toString().substring(1), builder, y);
			}
		}

		add(builder.getPanel());
		ready = true;
	}

	public void buildEditBar(boolean updateUI) {
		boolean customized = ((DeviceConfiguration)renderer).isCustomized();
		boolean repack = ready && editBar.getComponentCount() == 0;
		editBar.removeAll();
		editBar.add(customized ? referenceButton() : editButton(true));
		if (renderer.getFile() != null) {
			editBar.add(Box.createHorizontalGlue());
			editBar.add(customized ? editButton(false) : customizeButton());
		}
		if (repack) {
			SwingUtilities.getWindowAncestor(this).pack();
		} else if (updateUI) {
			editBar.updateUI();
		}
	}

	public JButton customizeButton() {
		final CustomJButton open = new CustomJButton("+", MetalIconFactory.getTreeLeafIcon());
		open.setHorizontalTextPosition(JButton.CENTER);
		open.setForeground(Color.lightGray);
		open.setToolTipText("Customize this device");
		open.setFocusPainted(false);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				DeviceConfiguration d = (DeviceConfiguration)renderer;
				String filename = chooseConfName(d.getDeviceDir(), d.getDefaultFilename(d));
				if (filename != null) {
					File file = DeviceConfiguration.createDeviceFile(d, filename, true);
					buildEditBar(true);
					try {
						java.awt.Desktop.getDesktop().open(file);
					} catch (IOException ioe) {
						LOGGER.debug("Failed to open default desktop application: " + ioe);
					}
				}
			}
		});
		return open;
	}

	public JButton referenceButton() {
		final File ref = ((DeviceConfiguration)renderer).getConfiguration(DeviceConfiguration.RENDERER).getFile();
		final CustomJButton open = new CustomJButton(MetalIconFactory.getTreeLeafIcon());
		boolean exists = ref != null && ref.exists();
		open.setToolTipText(exists ? ("Open the parent configuration: " + ref) : "No parent configuration");
		open.setFocusPainted(false);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					java.awt.Desktop.getDesktop().open(ref);
				} catch (IOException ioe) {
					LOGGER.debug("Failed to open default desktop application: " + ioe);
				}
			}
		});
		if (! exists) {
			open.setLabel("!");
			open.setHorizontalTextPosition(JButton.CENTER);
			open.setForeground(Color.lightGray);
			open.setEnabled(false);
		}
		return open;
	}

	public JButton editButton(final boolean create) {
		final File file  = create ? renderer.getUsableFile() : renderer.getFile();
		final CustomJButton open = new CustomJButton(((file != null && file.exists() || !create) ? "<html>" :
			"<html><font color=blue>Start a new configuration file:</font> ") + file.getName() + "</html>",
			MetalIconFactory.getTreeLeafIcon());
		open.setToolTipText(file.getAbsolutePath());
		open.setFocusPainted(false);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				boolean exists = file.isFile() && file.exists();
				if (!exists && create) {
					File ref = chooseReferenceConf();
					if (ref != null) {
						renderer.createNewFile(renderer, file, true, ref);
						open.setText(file.getName());
						exists = true;
					}
				}
				if (exists) {
					try {
						java.awt.Desktop.getDesktop().open(file);
					} catch (IOException ioe) {
						LOGGER.debug("Failed to open default desktop application: " + ioe);
					}
				} else {
					// Conf no longer exists, repair the edit bar
					buildEditBar(true);
				}
			}
		});
		return open;
	}

	public String chooseConfName(final File dir, final String filename) {
		final File file = new File(filename);
		JFileChooser fc = new JFileChooser(dir) {
			@Override
			public boolean isTraversable(File d) {
				return dir.equals(d); // Disable navigation
			}
			@Override
			public void approveSelection() {
				if(getSelectedFile().exists()){
					switch(JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "File Exists", JOptionPane.YES_NO_CANCEL_OPTION)){
						case JOptionPane.CANCEL_OPTION:
						case JOptionPane.NO_OPTION:
							setSelectedFile(file);
						case JOptionPane.CLOSED_OPTION:
							return;
					}
				}
				super.approveSelection();
			}
		};
		fc.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Conf Files", "conf");
		fc.addChoosableFileFilter(filter);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setSelectedFile(file);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setDialogTitle("Specify A File Name");
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile().getName();
		}
		return null;
	}

	public File chooseReferenceConf() {
		JFileChooser fc = new JFileChooser(RendererConfiguration.getRenderersDir());
		fc.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Conf Files", "conf");
		fc.addChoosableFileFilter(filter);
		fc.setAcceptAllFileFilterUsed(true);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fc.showDialog(this, "Select a reference file") == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}

	public int addItem(String key, String value, PanelBuilder builder, int y) {
		builder.appendRow(rspec);
		builder.addLabel(key.length() > 0 ? key + ":  " : "", cc.xy(1, ++y));
		JTextField val = new JTextField(value);
		val.setEditable(false);
		val.setBackground(Color.white);
		builder.add(val, cc.xy(2, y));
		return y;
	}

	public int addMap(Map<String,String> map, PanelBuilder builder, int y) {
		for (Map.Entry<String,String> entry : (Set<Map.Entry<String,String>>)map.entrySet()) {
			y = addItem(entry.getKey(), entry.getValue(), builder, y);
		}
		return y;
	}

	public int addStrings(String title, String[] strings, PanelBuilder builder, int y) {
		for (int i=0; i<strings.length; i++) {
			y = addItem(title, strings[i], builder, y);
			title = "";
		}
		return y;
	}

	public int addList(String title, List<String> list, PanelBuilder builder, int y) {
		for (String item : list) {
			y = addItem(title, item, builder, y);
			title = "";
		}
		return y;
	}

	public void update() {
		buildEditBar(true);
	}
}


