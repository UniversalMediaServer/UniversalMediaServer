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
package net.pms.newgui.engines;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.newgui.GuiUtil;
import org.apache.commons.configuration.event.ConfigurationEvent;

public class AviSynthMEncoder {
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static JTextArea textArea;

	/**
	 * This class is not meant to be instantiated.
	 */
	private AviSynthMEncoder() {
	}

	public static JComponent config() {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 12dlu, p, 3dlu, 0:grow"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("GeneralSettings_SentenceCase"), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		JCheckBox multithreading = new JCheckBox(Messages.getString("EnableMultithreading"), CONFIGURATION.getAvisynthMultiThreading());
		multithreading.setContentAreaFilled(false);
		multithreading.addItemListener((ItemEvent e) -> CONFIGURATION.setAvisynthMultiThreading((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(multithreading), cc.xy(2, 3));

		JCheckBox interframe = new JCheckBox(Messages.getString("EnableTrueMotion"), CONFIGURATION.getAvisynthInterFrame());
		interframe.setContentAreaFilled(false);
		interframe.addActionListener((ActionEvent e) -> {
			CONFIGURATION.setAvisynthInterFrame(interframe.isSelected());
			if (CONFIGURATION.getAvisynthInterFrame()) {
				JOptionPane.showMessageDialog(
						SwingUtilities.getWindowAncestor(interframe),
						Messages.getString("ThisFeatureVeryCpuintensive"),
						Messages.getString("Information"),
						JOptionPane.INFORMATION_MESSAGE
				);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(interframe), cc.xy(2, 5));

		JCheckBox interframegpu = new JCheckBox(Messages.getString("EnableGpuUseTrueMotion"), CONFIGURATION.getAvisynthInterFrameGPU());
		interframegpu.setContentAreaFilled(false);
		interframegpu.addItemListener((ItemEvent e) -> CONFIGURATION.setAvisynthInterFrameGPU((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(interframegpu), cc.xy(2, 7));

		JCheckBox convertfps = new JCheckBox(Messages.getString("EnableAvisynthVariableFramerate"), CONFIGURATION.getAvisynthConvertFps());
		convertfps.setContentAreaFilled(false);
		convertfps.addItemListener((ItemEvent e) -> CONFIGURATION.setAvisynthConvertFps((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(convertfps), cc.xy(2, 9));

		String aviSynthScriptInstructions = Messages.getString("AvisynthScriptFullyCustomizable") +
			Messages.getString("TheFollowingVariablesAvailable") +
			Messages.getString("MovieCompleteDirectshowsource") +
			Messages.getString("SubCompleteSubtitlesInstruction") +
			Messages.getString("MoviefilenameVideoFilename");
		JTextArea aviSynthScriptInstructionsContainer = new JTextArea(aviSynthScriptInstructions);
		aviSynthScriptInstructionsContainer.setEditable(false);
		aviSynthScriptInstructionsContainer.setBorder(BorderFactory.createEtchedBorder());
		aviSynthScriptInstructionsContainer.setBackground(new Color(255, 255, 192));
		aviSynthScriptInstructionsContainer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(130, 135, 144)), BorderFactory.createEmptyBorder(3, 5, 3, 5)));
		builder.add(aviSynthScriptInstructionsContainer, cc.xy(2, 11));

		String clip = CONFIGURATION.getAvisynthScript();
		if (clip == null) {
			clip = "";
		}
		StringBuilder sb = new StringBuilder();
		StringTokenizer st = new StringTokenizer(clip, PMS.AVS_SEPARATOR);
		int i = 0;
		while (st.hasMoreTokens()) {
			if (i > 0) {
				sb.append("\n");
			}
			sb.append(st.nextToken());
			i++;
		}
		textArea = new JTextArea(sb.toString());
		textArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				StringBuilder sb = new StringBuilder();
				StringTokenizer st = new StringTokenizer(textArea.getText(), "\n");
				int i = 0;
				while (st.hasMoreTokens()) {
					if (i > 0) {
						sb.append(PMS.AVS_SEPARATOR);
					}
					sb.append(st.nextToken());
					i++;
				}
				CONFIGURATION.setAvisynthScript(sb.toString());
			}
		});

		JScrollPane pane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setPreferredSize(new Dimension(500, 350));
		builder.add(pane, cc.xy(2, 13));

		CONFIGURATION.addConfigurationListener((ConfigurationEvent event) -> {
			if (event.getPropertyName() == null) {
				return;
			}
			if ((!event.isBeforeUpdate()) && interframegpu.isEnabled() != CONFIGURATION.isGPUAcceleration()) {
				interframegpu.setEnabled(CONFIGURATION.isGPUAcceleration());
			}
		});

		return builder.getPanel();
	}

}
