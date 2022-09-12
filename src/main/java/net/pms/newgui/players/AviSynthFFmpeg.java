/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.newgui.players;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.newgui.GuiUtil;
import org.apache.commons.configuration.event.ConfigurationEvent;

public class AviSynthFFmpeg {
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static JCheckBox useffmpegsource2;
	private static JCheckBox interframegpu;
	private static JCheckBox multithreading;
	private static JCheckBox avisynthplusmode;
	private static JCheckBox interframe;
	private static JCheckBox convertfps;
	private static JCheckBox convert2dTo3d;

	public static JComponent config() {
		return config("GeneralSettings");
	}

	protected static JComponent config(String languageLabel) {
		
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu");
					
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString(languageLabel), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
		
		useffmpegsource2 = new JCheckBox(Messages.getString("EnableAvisynthUseFFMpegSource2"), CONFIGURATION.getFfmpegAvisynthUseFfmpegSource2());
		useffmpegsource2.setContentAreaFilled(false);
		useffmpegsource2.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthUseFfmpegSource2(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(useffmpegsource2), cc.xy(2, 3));

		multithreading = new JCheckBox(Messages.getString("EnableMultithreading"), CONFIGURATION.isFfmpegAviSynthMultithreading());
		multithreading.setContentAreaFilled(false);
		multithreading.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAviSynthMultithreading(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(multithreading), cc.xy(2, 5));
		
		avisynthplusmode = new JCheckBox(Messages.getString("EnableAviSynthPlusMode"), CONFIGURATION.isFfmpegAviSynthPlusMode());
		avisynthplusmode.setContentAreaFilled(false);
		avisynthplusmode.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAviSynthPlusMode(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(avisynthplusmode), cc.xy(2, 7));

		interframe = new JCheckBox(Messages.getString("EnableTrueMotion"), CONFIGURATION.getFfmpegAvisynthInterFrame());
		interframe.setContentAreaFilled(false);
		interframe.addActionListener((ActionEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthInterFrame(interframe.isSelected());
			if (CONFIGURATION.getFfmpegAvisynthInterFrame()) {
				JOptionPane.showMessageDialog(
						SwingUtilities.getWindowAncestor(interframe),
						Messages.getString("ThisFeatureVeryCpuintensive"),
						Messages.getString("Information"),
						JOptionPane.INFORMATION_MESSAGE
				);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(interframe), cc.xy(2, 9));

		interframegpu = new JCheckBox(Messages.getString("EnableGpuUseTrueMotion"), CONFIGURATION.getFfmpegAvisynthInterFrameGPU());
		interframegpu.setContentAreaFilled(false);
		interframegpu.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthInterFrameGPU((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(interframegpu), cc.xy(2, 11));

		convertfps = new JCheckBox(Messages.getString("EnableAvisynthVariableFramerate"), CONFIGURATION.getFfmpegAvisynthConvertFps());
		convertfps.setContentAreaFilled(false);
		convertfps.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthConvertFps((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(convertfps), cc.xy(2, 13));
		
		convert2dTo3d = new JCheckBox(Messages.getString("EnableAvisynth2Dto3DConversion"), CONFIGURATION.getFfmpegAvisynth2Dto3D());
		convert2dTo3d.setContentAreaFilled(false);
		convert2dTo3d.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynth2Dto3D((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(convert2dTo3d), cc.xy(2, 15));

		CONFIGURATION.addConfigurationListener((ConfigurationEvent event) -> {
			if (event.getPropertyName() == null) {
				return;
			}
			if ((!event.isBeforeUpdate()) && event.getPropertyName().equals(PmsConfiguration.KEY_GPU_ACCELERATION)) {
				interframegpu.setEnabled(CONFIGURATION.isGPUAcceleration());
			}
		});

		return builder.getPanel();
	}
}
