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
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.newgui.CustomTabbedPaneUI;
import net.pms.newgui.GuiUtil;
import net.pms.newgui.util.FormLayoutUtil;
import net.pms.newgui.util.KeyedComboBoxModel;
import org.apache.commons.configuration.event.ConfigurationEvent;

public class AviSynthFFmpeg {
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String COMMON_COL_SPEC = "left:pref, 0:grow";
	private static final ComponentOrientation ORIENTATION = ComponentOrientation.getOrientation(PMS.getLocale());

	/**
	 * This class is not meant to be instantiated.
	 */
	private AviSynthFFmpeg() {
	}

	public static JComponent config() {
		return config("GeneralSettings");
	}

	protected static JComponent config(String languageLabel) {

		String colSpec = FormLayoutUtil.getColSpec(COMMON_COL_SPEC, ORIENTATION);

		FormLayout layout = new FormLayout(
			colSpec,
			"5*(pref, 3dlu), pref, 9dlu, pref, 9dlu:grow, pref");

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString(languageLabel), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		JCheckBox multithreading = new JCheckBox(Messages.getString("EnableMultithreading"), CONFIGURATION.isFfmpegAviSynthMultithreading());
		multithreading.setContentAreaFilled(false);
		multithreading.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAviSynthMultithreading(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(GuiUtil.getPreferredSizeComponent(multithreading), cc.xy(2, 3));

		JCheckBox interframe = new JCheckBox(Messages.getString("EnableTrueMotion"), CONFIGURATION.getFfmpegAvisynthInterFrame());
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
		builder.add(GuiUtil.getPreferredSizeComponent(interframe), cc.xy(2, 5));

		JCheckBox interframegpu = new JCheckBox(Messages.getString("EnableGpuUseTrueMotion"), CONFIGURATION.getFfmpegAvisynthInterFrameGPU());
		interframegpu.setContentAreaFilled(false);
		interframegpu.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthInterFrameGPU((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(interframegpu), cc.xy(2, 7));

		CONFIGURATION.addConfigurationListener((ConfigurationEvent event) -> {
			if (event.getPropertyName() == null) {
				return;
			}
			if ((!event.isBeforeUpdate()) && interframegpu.isEnabled() != CONFIGURATION.isGPUAcceleration()) {
				interframegpu.setEnabled(CONFIGURATION.isGPUAcceleration());
			}
		});

		JCheckBox convertfps = new JCheckBox(Messages.getString("EnableAvisynthVariableFramerate"), CONFIGURATION.getFfmpegAvisynthConvertFps());
		convertfps.setContentAreaFilled(false);
		convertfps.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthConvertFps((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(convertfps), cc.xy(2, 9));

		JCheckBox useFFMS2 = new JCheckBox(Messages.getString("UseFFMS2InsteadOfDirectShowSource"), CONFIGURATION.getFfmpegAvisynthUseFFMS2());
		useFFMS2.setContentAreaFilled(false);
		useFFMS2.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthUseFFMS2(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(GuiUtil.getPreferredSizeComponent(useFFMS2), FormLayoutUtil.flip(cc.xy(2, 11), colSpec, ORIENTATION));

		JTabbedPane setupTabbedPanel = new JTabbedPane();
		setupTabbedPanel.setUI(new CustomTabbedPaneUI());

		setupTabbedPanel.addTab(Messages.getString("2Dto3DConversionSettings"), build2dTo3dSetupPanel());


		if (!CONFIGURATION.isHideAdvancedOptions()) {
			builder.add(setupTabbedPanel, FormLayoutUtil.flip(cc.xywh(1, 13, 2, 3), colSpec, ORIENTATION));
		}

		return builder.getPanel();
	}

	private static JComponent build2dTo3dSetupPanel() {
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 3dlu, pref:grow", ORIENTATION);
		FormLayout layout = new FormLayout(colSpec, "$lgap, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, 2*(pref, 3dlu)");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DLU4);
		CellConstraints cc = new CellConstraints();

		JCheckBox convert2dTo3d = new JCheckBox(Messages.getString("Enable2Dto3DVideoConversion"), CONFIGURATION.isFfmpegAvisynth2Dto3D());
		convert2dTo3d.setContentAreaFilled(false);
		convert2dTo3d.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynth2Dto3D((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(convert2dTo3d), FormLayoutUtil.flip(cc.xy(1, 2), colSpec, ORIENTATION));

		builder.addLabel(Messages.getString("ConversionAlgorithm"), FormLayoutUtil.flip(cc.xy(1, 4), colSpec, ORIENTATION));

		String[] keys = new String[] {"1", "2"};
		String[] values = new String[] {
			Messages.getString("PulfrichBase"),
			Messages.getString("PulfrichandLighting")
		};

		final KeyedComboBoxModel<Integer, String> algorithmForConverting2Dto3D = new KeyedComboBoxModel<>(keys, values);
		JComboBox<String> algorithms = new JComboBox<>(algorithmForConverting2Dto3D);
		algorithms.setEditable(false);
		algorithmForConverting2Dto3D.setSelectedKey(CONFIGURATION.getFfmpegAvisynthConversionAlgorithm2Dto3D());
		algorithms.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthConversionAlgorithm2Dto3D(algorithmForConverting2Dto3D.getSelectedKey()));
		builder.add(GuiUtil.getPreferredSizeComponent(algorithms), FormLayoutUtil.flip(cc.xy(3, 4), colSpec, ORIENTATION));

		builder.addLabel(Messages.getString("FrameStretchFactor"), FormLayoutUtil.flip(cc.xy(1, 6), colSpec, ORIENTATION));

		String[] frameStretchFactors = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" };

		JComboBox<String> frameStretchFactor = new JComboBox<>(frameStretchFactors);
		frameStretchFactor.setSelectedItem(CONFIGURATION.getFfmpegAvisynthFrameStretchFactor());
		frameStretchFactor.setToolTipText(Messages.getString("SelectOrEnterFrameStretchFactorInPercent"));

		frameStretchFactor.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthFrameStretchFactor((String) e.getItem()));
		frameStretchFactor.setEditable(true);
		builder.add(GuiUtil.getPreferredSizeComponent(frameStretchFactor), cc.xy(3, 6));

		builder.addLabel(Messages.getString("LightingDepthOffsetFactor"), FormLayoutUtil.flip(cc.xy(1, 8), colSpec, ORIENTATION));

		String[] lightOffsetFactors = new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" };

		JComboBox<String> lightOffsetFactor = new JComboBox<>(lightOffsetFactors);
		lightOffsetFactor.setSelectedItem(CONFIGURATION.getFfmpegAvisynthLightOffsetFactor());
		lightOffsetFactor.setToolTipText(Messages.getString("SelectOrEnterLightingDepthOffsetFactor"));

		lightOffsetFactor.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthLightOffsetFactor((String) e.getItem()));
		lightOffsetFactor.setEditable(true);

		builder.add(GuiUtil.getPreferredSizeComponent(lightOffsetFactor), cc.xy(3, 8));

		JCheckBox horizontalResize = new JCheckBox(Messages.getString("ResizeVideoIfWidthLargerThan"), CONFIGURATION.isFfmpegAvisynthHorizontalResize());
		horizontalResize.setContentAreaFilled(false);
		horizontalResize.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthHorizontalResize((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(horizontalResize), FormLayoutUtil.flip(cc.xy(1, 10), colSpec, ORIENTATION));


		String[] resolutions = new String[] {"7680", "3840", "1920", "1280", "852", "768", "720", "704", "640", "544", "480", "352", "120" };

		JComboBox<String> horizontalResizeResolution = new JComboBox<>(resolutions);
		horizontalResizeResolution.setSelectedItem(CONFIGURATION.getFfmpegAvisynthHorizontalResizeResolution());
		horizontalResizeResolution.setToolTipText(Messages.getString("SelectOrEnterTheMaximumWidthOfTheInputVideo"));

		horizontalResizeResolution.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthHorizontalResizeResolution((String) e.getItem()));
		horizontalResizeResolution.setEditable(true);
		builder.add(GuiUtil.getPreferredSizeComponent(horizontalResizeResolution), cc.xy(3, 10));

		JCheckBox avisynthplusmode = new JCheckBox(Messages.getString("EnableAvisynthPlusMultithreading"), CONFIGURATION.isFfmpegAviSynthPlusMode());
		avisynthplusmode.setContentAreaFilled(false);
		avisynthplusmode.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAviSynthPlusMode(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(GuiUtil.getPreferredSizeComponent(avisynthplusmode), FormLayoutUtil.flip(cc.xy(1, 12), colSpec, ORIENTATION));

		JPanel panel = builder.getPanel();
		panel.applyComponentOrientation(ORIENTATION);

		return panel;
	}

}
