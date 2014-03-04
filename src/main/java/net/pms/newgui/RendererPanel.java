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
import javax.swing.plaf.metal.MetalIconFactory;
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
		builder.border(Borders.EMPTY);
		int y=0;

		builder.appendRow(rspec);
		builder.add(editButton(), cc.xyw(1, ++y, 2));
		if (renderer.isUpnpConnected()) {
			builder.appendRow(rspec);
			builder.addLabel(" ", cc.xy(1, ++y));

			y = addMap(renderer.getUpnpDetails(), builder, y);
			y = addStrings("Services", WordUtils.wrap(StringUtils.join(renderer.getUpnpServices(), ", "), 60).split("\n"),
				builder, y);

			if (renderer.isUpnpControllable()) {
				builder.appendRow(rspec);
				builder.addLabel(" ", cc.xy(1, ++y));
				builder.appendRow(rspec);
				builder.addSeparator("UPNP Controls", cc.xyw(1, ++y, 2));
				builder.appendRow(rspec);
				builder.add(new PlayerControlPanel(new UPNPHelper.Player(renderer)), cc.xyw(1, ++y, 2));
			}
		}

		add(builder.getPanel());
	}

	public JButton editButton() {
		final File file  = renderer.getFile(true);
		final CustomJButton open = new CustomJButton((file .exists() ? "" : "Create a preliminary configuration file: ") + file.getName(), MetalIconFactory.getTreeLeafIcon());
		open.setToolTipText(file .getAbsolutePath());
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				boolean exists = file.isFile() && file.exists();
				if (!exists) {
					// TODO: implement a reference conf chooser here (eg adapt "Select Renderer")
					renderer.createNewFile(file, true, null);
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

	public int addItem(String key, String value, PanelBuilder builder, int y) {
		builder.appendRow(rspec);
		builder.addLabel(key.length() > 0 ? key + ":" : "", cc.xy(1, ++y));
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


