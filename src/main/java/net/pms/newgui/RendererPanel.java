package net.pms.newgui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.metal.MetalIconFactory;
import net.pms.Messages;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.newgui.components.CustomJButton;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
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
	}

	public JPanel buildPanel() {
		FormLayout layout = new FormLayout("left:pref, 400:grow");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(new EmptyBorder(10, 10, 10, 10));
		int y = 0;

		builder.appendRow(rspec);
		editBar = new JPanel();
		editBar.setLayout(new BoxLayout(editBar, BoxLayout.X_AXIS));
		builder.add(editBar, cc.xyw(1, ++y, 2));
		if (/*renderer.loaded &&*/ !renderer.isFileless()) {
			buildEditBar(false);
		}
		builder.appendRow(rspec);
		builder.addLabel(" ", cc.xy(1, ++y));

		y = addMap(renderer.getDetails(), builder, y);
		if (renderer.isUpnp()) {
			y = addStrings("Services", WordUtils.wrap(StringUtils.join(renderer.getUpnpServices(), ", "), 60).split("\n"),
				builder, y);
		}

		if (renderer.isControllable()) {
			builder.appendRow(rspec);
			builder.addLabel(" ", cc.xy(1, ++y));
			builder.appendRow(rspec);
			builder.addSeparator(Messages.getString("RendererPanel.1"), cc.xyw(1, ++y, 2));
			builder.appendRow(rspec);
			builder.add(new PlayerControlPanel(renderer.getPlayer()), cc.xyw(1, ++y, 2));
		}
		return builder.getPanel();
	}

	public void buildEditBar(boolean updateUI) {
		boolean customized = ((DeviceConfiguration) renderer).isCustomized();
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
		open.setToolTipText(Messages.getString("RendererPanel.5"));
		open.setFocusPainted(false);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				DeviceConfiguration d = (DeviceConfiguration) renderer;
				File f = chooseConf(d.getDeviceDir(), d.getDefaultFilename(d));
				if (f != null) {
					File file = DeviceConfiguration.createDeviceFile(d, f.getName(), true);
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
		final File ref = ((DeviceConfiguration) renderer).getConfiguration(DeviceConfiguration.RENDERER).getFile();
		final CustomJButton open = new CustomJButton(MetalIconFactory.getTreeLeafIcon());
		boolean exists = ref != null && ref.exists();
		open.setToolTipText(exists ? (Messages.getString("RendererPanel.3") + ": " + ref) : Messages.getString("RendererPanel.4"));
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
		if (!exists) {
			open.setText("!");
			open.setHorizontalTextPosition(JButton.CENTER);
			open.setForeground(Color.lightGray);
			open.setEnabled(false);
		}
		return open;
	}

	public JButton editButton(final boolean create) {
		final File file = create ? renderer.getUsableFile() : renderer.getFile();
		final CustomJButton open = new CustomJButton(((file != null && file.exists() || !create) ? "<html>"
			: "<html><font color=blue>" + Messages.getString("RendererPanel.2") + ":</font> ") + file.getName() + "</html>",
			MetalIconFactory.getTreeLeafIcon());
		open.setToolTipText(file.getAbsolutePath());
		open.setFocusPainted(false);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				boolean exists = file.isFile() && file.exists();
				File f = file;
				if (!exists && create) {
					f =  chooseConf(file.getParentFile(), file.getName());
					if (f != null) {
						File ref = chooseReferenceConf();
						if (ref != null) {
							renderer.createNewFile(renderer, f, true, ref);
							open.setText(f.getName());
							exists = true;
						}
					}
				}
				if (exists) {
					try {
						java.awt.Desktop.getDesktop().open(f);
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

	public File chooseConf(final File dir, final String filename) {
		final File file = new File(filename);
		JFileChooser fc = new JFileChooser(dir) {
			private static final long serialVersionUID = -3606991702534289691L;

			@Override
			public boolean isTraversable(File d) {
				return dir.equals(d); // Disable navigation
			}

			@Override
			public void approveSelection() {
				if (getSelectedFile().exists()) {
					switch (JOptionPane.showConfirmDialog(this, Messages.getString("RendererPanel.6"), Messages.getString("RendererPanel.7"), JOptionPane.YES_NO_CANCEL_OPTION)) {
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
		// Current dir must be set explicitly before setting selected file (despite constructor call above)
		fc.setCurrentDirectory(dir);
		fc.setSelectedFile(file);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setDialogTitle(Messages.getString("RendererPanel.8"));
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}

	public File chooseReferenceConf() {
		JFileChooser fc = new JFileChooser(RendererConfiguration.getRenderersDir());
		fc.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Conf Files", "conf");
		fc.addChoosableFileFilter(filter);
		fc.setAcceptAllFileFilterUsed(true);
		File defaultRef = new File(RendererConfiguration.getRenderersDir(), "DefaultRenderer.conf");
		if (defaultRef.exists()) {
			fc.setSelectedFile(defaultRef);
		}
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		switch (fc.showDialog(this, Messages.getString("RendererPanel.9"))) {
			case JFileChooser.APPROVE_OPTION:
				return fc.getSelectedFile();
			case JFileChooser.CANCEL_OPTION:
				return RendererConfiguration.NOFILE;
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

	public int addMap(Map<String, String> map, PanelBuilder builder, int y) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			y = addItem(entry.getKey(), entry.getValue(), builder, y);
		}
		return y;
	}

	public int addStrings(String title, String[] strings, PanelBuilder builder, int y) {
		for (String string : strings) {
			y = addItem(title, string, builder, y);
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
		removeAll();
		add(buildPanel());
		JFrame top = (JFrame) SwingUtilities.getWindowAncestor(this);
		if (top != null) {
			top.setTitle(renderer.getRendererName() + (renderer.isOffline() ? "  [offline]" : ""));
			top.pack();
		}
		ready = true;
	}
}
