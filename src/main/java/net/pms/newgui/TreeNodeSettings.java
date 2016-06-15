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
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import net.pms.Messages;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeNodeSettings extends DefaultMutableTreeNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(TreeNodeSettings.class);
	private static final long serialVersionUID = -337606760204027449L;
	private Player p;
	private JComponent otherConfigPanel;
	private boolean enable = true;
	private JPanel warningPanel;

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;

	}

	public Player getPlayer() {
		return p;
	}

	public TreeNodeSettings(String name, Player p, JComponent otherConfigPanel) {
		super(name);
		this.p = p;
		this.otherConfigPanel = otherConfigPanel;

	}

	public String id() {
		if (p != null) {
			return p.id();
		} else if (otherConfigPanel != null) {
			return "" + otherConfigPanel.hashCode();
		} else {
			return null;
		}
	}

	public JComponent getConfigPanel() {
		if (p != null) {
			if (PlayerFactory.getPlayers().contains(p)) {
				return p.config();
			} else {
				return getWarningPanel();
			}
		} else if (otherConfigPanel != null) {
			return otherConfigPanel;
		} else {
			return new JPanel();
		}
	}

	public JPanel getWarningPanel() {
		if (warningPanel == null) {
			BufferedImage bi = null;

			try {
				bi = ImageIO.read(LooksFrame.class.getResourceAsStream("/resources/images/icon-status-warning.png"));
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}

			ImagePanel ip = new ImagePanel(bi);

			FormLayout layout = new FormLayout(
				"0:grow, pref, 0:grow",
				"pref, 3dlu, pref, 12dlu, pref, 3dlu, pref, 3dlu, p, 3dlu, p, 3dlu, p"
			);

			PanelBuilder builder = new PanelBuilder(layout);
			builder.border(Borders.DIALOG);
			builder.opaque(false);
			CellConstraints cc = new CellConstraints();

			JLabel jl = new JLabel(Messages.getString("TreeNodeSettings.4"));
			builder.add(jl, cc.xy(2, 1, "center, fill"));
			jl.setFont(jl.getFont().deriveFont(Font.BOLD));

			builder.add(ip, cc.xy(2, 3, "center, fill"));

			warningPanel = builder.getPanel();
		}

		return warningPanel;
	}
}
