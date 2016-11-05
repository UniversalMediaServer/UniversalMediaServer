/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.newgui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultMutableTreeNode;
import net.pms.Messages;
import net.pms.encoders.Player;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeNodeSettings extends DefaultMutableTreeNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(TreeNodeSettings.class);
	private static final long serialVersionUID = -337606760204027449L;
	private Player player;
	private JComponent otherConfigPanel;
	private JPanel warningPanel;

	public Player getPlayer() {
		return player;
	}

	public TreeNodeSettings(String name, Player p, JComponent otherConfigPanel) {
		super(name);
		this.player = p;
		this.otherConfigPanel = otherConfigPanel;

	}

	public String id() {
		if (player != null) {
			return player.id();
		} else if (otherConfigPanel != null) {
			return "" + otherConfigPanel.hashCode();
		} else {
			return null;
		}
	}

	public JComponent getConfigPanel() {
		if (player != null) {
			if (player.isAvailable()) {
				return player.config();
			}
			return getWarningPanel();
		} else if (otherConfigPanel != null) {
			return otherConfigPanel;
		} else {
			return new JPanel();
		}
	}

	private JPanel getWarningPanel() {
		if (warningPanel == null) {
			BufferedImage warningIcon = null;

			try {
				warningIcon = ImageIO.read(LooksFrame.class.getResourceAsStream("/resources/images/icon-status-warning.png"));
			} catch (IOException e) {
				LOGGER.debug("Error reading icon-status-warning: ", e.getMessage());
				LOGGER.trace("", e);
			}

			ImagePanel iconPanel = new ImagePanel(warningIcon);

			FormLayout layout = new FormLayout(
				"10dlu, pref, 10dlu, pref:grow, 10dlu",
				"5dlu, pref, 3dlu, pref:grow, 5dlu"
			);

			PanelBuilder builder = new PanelBuilder(layout);
			builder.border(Borders.DIALOG);
			builder.opaque(false);
			CellConstraints cc = new CellConstraints();

			builder.add(iconPanel, cc.xywh(2, 1, 1, 4, CellConstraints.CENTER, CellConstraints.TOP));

			JLabel warningLabel = new JLabel(Messages.getString("TreeNodeSettings.4"));
			builder.add(warningLabel, cc.xy(4, 2, CellConstraints.LEFT, CellConstraints.CENTER));
			warningLabel.setFont(warningLabel.getFont().deriveFont(Font.BOLD));

			if (StringUtil.hasValue(player.getStatusText(true))) {
				JTextArea stateText = new JTextArea(player.getStatusText(true));
				stateText.setPreferredSize(new Dimension());
				stateText.setEditable(false);
				stateText.setLineWrap(true);
				stateText.setWrapStyleWord(true);
				builder.add(stateText, cc.xy(4, 4, CellConstraints.FILL, CellConstraints.FILL));
			}
			warningPanel = builder.getPanel();
		}

		return warningPanel;
	}
}
