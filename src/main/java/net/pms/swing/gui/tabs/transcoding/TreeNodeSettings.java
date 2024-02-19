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
package net.pms.swing.gui.tabs.transcoding;

import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultMutableTreeNode;
import net.pms.Messages;
import net.pms.encoders.Engine;
import net.pms.swing.SwingUtil;
import net.pms.swing.gui.JavaGui;
import net.pms.swing.gui.UmsFormBuilder;
import net.pms.util.StringUtil;

public class TreeNodeSettings extends DefaultMutableTreeNode {

	private static final long serialVersionUID = -337606760204027449L;
	private static final String ICON_ENGINE_WARNING = (SwingUtil.HDPI_AWARE ? "icon-engine-warning.svg" : "icon-status-warning.png");

	private final Engine engine;
	private final JComponent otherConfigPanel;

	private JPanel warningPanel;

	public Engine getPlayer() {
		return engine;
	}

	public TreeNodeSettings(String name, Engine engine, JComponent otherConfigPanel) {
		super(name);
		this.engine = engine;
		this.otherConfigPanel = otherConfigPanel;

	}

	public String id() {
		if (engine != null) {
			return engine.getEngineId().toString();
		} else if (otherConfigPanel != null) {
			return "" + otherConfigPanel.hashCode();
		} else {
			return null;
		}
	}

	public JComponent getConfigPanel() {
		if (engine != null) {
			if (engine.isAvailable()) {
				return getConfigPanel(engine.getName());
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
			ImageIcon warningIcon = JavaGui.readImageIcon(ICON_ENGINE_WARNING);

			FormLayout layout = new FormLayout(
				"10dlu, pref, 10dlu, pref:grow, 10dlu",
				"5dlu, pref, 3dlu, pref:grow, 5dlu"
			);

			UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
			builder.border(Paddings.DIALOG);
			builder.opaque(false);
			CellConstraints cc = new CellConstraints();

			builder.add(warningIcon).at(cc.xywh(2, 1, 1, 4, CellConstraints.CENTER, CellConstraints.TOP));

			JLabel warningLabel = new JLabel(Messages.getGuiString("ThisEngineNotLoaded"));
			builder.add(warningLabel).at(cc.xy(4, 2, CellConstraints.LEFT, CellConstraints.CENTER));
			warningLabel.setFont(warningLabel.getFont().deriveFont(Font.BOLD));

			if (StringUtil.hasValue(engine.getStatusTextFull())) {
				JTextArea stateText = new JTextArea(engine.getStatusTextFull());
				stateText.setPreferredSize(new Dimension());
				stateText.setEditable(false);
				stateText.setLineWrap(true);
				stateText.setWrapStyleWord(true);
				builder.add(stateText).at(cc.xy(4, 4, CellConstraints.FILL, CellConstraints.FILL));
			}
			warningPanel = builder.getPanel();
		}

		return warningPanel;
	}

	public static JComponent getConfigPanel(String name) {
		return switch (name) {
			case net.pms.encoders.AviSynthFFmpeg.NAME -> AviSynthFFmpeg.config();
			case net.pms.encoders.AviSynthMEncoder.NAME -> AviSynthMEncoder.config();
			case net.pms.encoders.FFMpegVideo.NAME -> FFMpegVideo.config();
			case net.pms.encoders.FFmpegAudio.NAME -> FFmpegAudio.config();
			case net.pms.encoders.MEncoderVideo.NAME -> MEncoderVideo.config();
			case net.pms.encoders.TsMuxeRVideo.NAME -> TsMuxeRVideo.config();
			case net.pms.encoders.VLCVideo.NAME -> VLCVideo.config();
			default -> null;
		};
	}

}
