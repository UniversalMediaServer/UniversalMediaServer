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
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import net.pms.configuration.RendererConfiguration;
import net.pms.network.UPNPHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererPanel extends JPanel {
	private static final long serialVersionUID = 5130146620433713605L;

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererPanel.class);

	private RendererConfiguration renderer;
	private CellConstraints cc = new CellConstraints();
	private static RowSpec rspec = RowSpec.decode("center:pref");

	public RendererPanel(final RendererConfiguration renderer) {
		this.renderer = renderer;

		FormLayout layout = new FormLayout("left:pref, 400:grow");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(new EmptyBorder(10,10,10,10));
		int y=0;

		builder.appendRow(rspec);
		builder.add(editButton(), cc.xyw(1, ++y, 2));
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
	}

	public JButton editButton() {
		final File file  = renderer.getFile(true);
		final CustomJButton open = new CustomJButton((file.exists() ? "<html>" :
			"<html><font color=blue>Start a new configuration file:</font> ") + file.getName() + "</html>",
			MetalIconFactory.getTreeLeafIcon());
		open.setToolTipText(file .getAbsolutePath());
		open.setFocusPainted(false);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				boolean exists = file.isFile() && file.exists();
				if (!exists) {
					File ref = chooseReferenceConf();
					renderer.createNewFile(renderer, file, true, ref);
					open.setText(file.getName());
				}
				try {
					java.awt.Desktop.getDesktop().open(file);
				} catch (IOException ioe) {
					LOGGER.debug("Failed to open default desktop application: " + ioe);
				}
			}
		});
		return open;
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
}


