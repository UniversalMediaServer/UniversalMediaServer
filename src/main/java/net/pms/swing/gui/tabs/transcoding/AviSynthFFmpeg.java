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
import java.awt.ComponentOrientation;
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
import net.pms.swing.SwingUtil;
import net.pms.swing.components.CustomTabbedPaneUI;
import net.pms.swing.components.KeyedComboBoxModel;
import net.pms.swing.gui.FormLayoutUtil;
import net.pms.swing.gui.UmsFormBuilder;
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

		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		builder.addSeparator(Messages.getGuiString(languageLabel)).at(cc.xyw(2, 1, 1));

		JCheckBox multithreading = new JCheckBox(Messages.getGuiString("EnableMultithreading"), CONFIGURATION.isFfmpegAviSynthMultithreading());
		multithreading.setContentAreaFilled(false);
		multithreading.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAviSynthMultithreading(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(multithreading)).at(cc.xy(2, 3));

		JCheckBox interframe = new JCheckBox(Messages.getGuiString("EnableTrueMotion"), CONFIGURATION.getFfmpegAvisynthInterFrame());
		interframe.setContentAreaFilled(false);
		interframe.addActionListener((ActionEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthInterFrame(interframe.isSelected());
			if (CONFIGURATION.getFfmpegAvisynthInterFrame()) {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(interframe),
						Messages.getGuiString("ThisFeatureVeryCpuintensive"),
						Messages.getGuiString("Information"),
						JOptionPane.INFORMATION_MESSAGE
				);
			}
		});
		builder.add(SwingUtil.getPreferredSizeComponent(interframe)).at(cc.xy(2, 5));

		JCheckBox interframegpu = new JCheckBox(Messages.getGuiString("EnableGpuUseTrueMotion"), CONFIGURATION.getFfmpegAvisynthInterFrameGPU());
		interframegpu.setContentAreaFilled(false);
		interframegpu.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthInterFrameGPU((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(interframegpu)).at(cc.xy(2, 7));

		CONFIGURATION.addConfigurationListener((ConfigurationEvent event) -> {
			if (event.getPropertyName() == null) {
				return;
			}
			if ((!event.isBeforeUpdate()) && interframegpu.isEnabled() != CONFIGURATION.isGPUAcceleration()) {
				interframegpu.setEnabled(CONFIGURATION.isGPUAcceleration());
			}
		});

		JCheckBox convertfps = new JCheckBox(Messages.getGuiString("EnableAvisynthVariableFramerate"), CONFIGURATION.getFfmpegAvisynthConvertFps());
		convertfps.setContentAreaFilled(false);
		convertfps.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthConvertFps((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(convertfps)).at(cc.xy(2, 9));

		JCheckBox useFFMS2 = new JCheckBox(Messages.getGuiString("UseFFMS2InsteadOfDirectShowSource"), CONFIGURATION.getFfmpegAvisynthUseFFMS2());
		useFFMS2.setContentAreaFilled(false);
		useFFMS2.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthUseFFMS2(e.getStateChange() == ItemEvent.SELECTED));
		useFFMS2.setEnabled(CONFIGURATION.getFFMS2Path() != null);
		builder.add(SwingUtil.getPreferredSizeComponent(useFFMS2)).at(FormLayoutUtil.flip(cc.xy(2, 11), colSpec, ORIENTATION));

		JTabbedPane setupTabbedPanel = new JTabbedPane();
		setupTabbedPanel.setUI(new CustomTabbedPaneUI());

		setupTabbedPanel.addTab(Messages.getGuiString("2Dto3DConversionSettings"), build2dTo3dSetupPanel());


		if (!CONFIGURATION.isHideAdvancedOptions()) {
			builder.add(setupTabbedPanel).at(FormLayoutUtil.flip(cc.xywh(1, 13, 2, 3), colSpec, ORIENTATION));
		}

		return builder.getPanel();
	}

	private static JComponent build2dTo3dSetupPanel() {
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 3dlu, pref:grow", ORIENTATION);
		FormLayout layout = new FormLayout(colSpec, "$lgap, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, 2*(pref, 3dlu)");
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DLU4);
		CellConstraints cc = new CellConstraints();

		JCheckBox convert2dTo3d = new JCheckBox(Messages.getGuiString("Enable2Dto3DVideoConversion"), CONFIGURATION.isFfmpegAvisynth2Dto3D());
		convert2dTo3d.setContentAreaFilled(false);
		convert2dTo3d.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynth2Dto3D((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(convert2dTo3d)).at(FormLayoutUtil.flip(cc.xy(1, 2), colSpec, ORIENTATION));

		builder.addLabel(Messages.getGuiString("ConversionAlgorithm")).at(FormLayoutUtil.flip(cc.xy(1, 4), colSpec, ORIENTATION));

		Integer[] keys = new Integer[] {1, 2};
		String[] values = new String[] {
			Messages.getGuiString("PulfrichBase"),
			Messages.getGuiString("PulfrichandLighting")
		};

		final KeyedComboBoxModel<Integer, String> algorithmForConverting2Dto3D = new KeyedComboBoxModel<>(keys, values);
		JComboBox<String> algorithms = new JComboBox<>(algorithmForConverting2Dto3D);
		algorithms.setEditable(false);
		algorithmForConverting2Dto3D.setSelectedKey(CONFIGURATION.getFfmpegAvisynthConversionAlgorithm2Dto3D());
		algorithms.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthConversionAlgorithm2Dto3D(algorithmForConverting2Dto3D.getSelectedKey()));
		builder.add(SwingUtil.getPreferredSizeComponent(algorithms)).at(FormLayoutUtil.flip(cc.xy(3, 4), colSpec, ORIENTATION));

		builder.addLabel(Messages.getGuiString("FrameStretchFactor")).at(FormLayoutUtil.flip(cc.xy(1, 6), colSpec, ORIENTATION));

		String[] frameStretchFactors = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" };

		JComboBox<String> frameStretchFactor = new JComboBox<>(frameStretchFactors);
		frameStretchFactor.setSelectedItem(Integer.toString(CONFIGURATION.getFfmpegAvisynthFrameStretchFactor()));
		frameStretchFactor.setToolTipText(Messages.getGuiString("SelectOrEnterFrameStretchFactorInPercent"));

		frameStretchFactor.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthFrameStretchFactor(Integer.parseInt((String) e.getItem())));
		frameStretchFactor.setEditable(true);
		builder.add(SwingUtil.getPreferredSizeComponent(frameStretchFactor)).at(cc.xy(3, 6));

		builder.addLabel(Messages.getGuiString("LightingDepthOffsetFactor")).at(FormLayoutUtil.flip(cc.xy(1, 8), colSpec, ORIENTATION));

		String[] lightOffsetFactors = new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" };

		JComboBox<String> lightOffsetFactor = new JComboBox<>(lightOffsetFactors);
		lightOffsetFactor.setSelectedItem(Integer.toString(CONFIGURATION.getFfmpegAvisynthLightOffsetFactor()));
		lightOffsetFactor.setToolTipText(Messages.getGuiString("SelectOrEnterLightingDepthOffsetFactor"));

		lightOffsetFactor.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthLightOffsetFactor(Integer.parseInt((String) e.getItem())));
		lightOffsetFactor.setEditable(true);

		builder.add(SwingUtil.getPreferredSizeComponent(lightOffsetFactor)).at(cc.xy(3, 8));

		builder.addLabel(Messages.getGuiString("3DOutputFormat")).at(FormLayoutUtil.flip(cc.xy(1, 10), colSpec, ORIENTATION));

		keys = new Integer[] {1, 2, 3, 4, 5, 6};
		values = new String[] {
			Messages.getGuiString("SBSFullSideBySide"),
			Messages.getGuiString("TBOUFullTopBottom"),
			Messages.getGuiString("HSBSHalfSideBySide"),
			Messages.getGuiString("HTBHOUHalfTopBottom"),
			Messages.getGuiString("HSBSUpscaledHalfSideBySide"),
			Messages.getGuiString("HTBHOUUpscaledHalfTopBottom")
		};

		final KeyedComboBoxModel<Integer, String> outputFormat3D = new KeyedComboBoxModel<>(keys, values);
		JComboBox<String> formats3D = new JComboBox<>(outputFormat3D);
		formats3D.setEditable(false);
		outputFormat3D.setSelectedKey(CONFIGURATION.getFfmpegAvisynthOutputFormat3D());
		formats3D.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthOutputFormat3D(outputFormat3D.getSelectedKey()));
		builder.add(SwingUtil.getPreferredSizeComponent(formats3D)).at(FormLayoutUtil.flip(cc.xy(3, 10), colSpec, ORIENTATION));

		JCheckBox horizontalResize = new JCheckBox(Messages.getGuiString("ResizeVideoIfWidthLargerThan"), CONFIGURATION.isFfmpegAvisynthHorizontalResize());
		horizontalResize.setContentAreaFilled(false);
		horizontalResize.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthHorizontalResize((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(horizontalResize)).at(FormLayoutUtil.flip(cc.xy(1, 12), colSpec, ORIENTATION));


		String[] resolutions = new String[] {"7680", "3840", "1920", "1280", "852", "768", "720", "704", "640", "544", "480", "352", "120" };

		JComboBox<String> horizontalResizeResolution = new JComboBox<>(resolutions);
		horizontalResizeResolution.setSelectedItem(Integer.toString(CONFIGURATION.getFfmpegAvisynthHorizontalResizeResolution()));
		horizontalResizeResolution.setToolTipText(Messages.getGuiString("SelectOrEnterTheMaximumWidthOfTheInputVideo"));

		horizontalResizeResolution.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegAvisynthHorizontalResizeResolution(Integer.parseInt((String) e.getItem())));
		horizontalResizeResolution.setEditable(true);
		builder.add(SwingUtil.getPreferredSizeComponent(horizontalResizeResolution)).at(cc.xy(3, 12));

		JPanel panel = builder.getPanel();
		panel.applyComponentOrientation(ORIENTATION);
		panel.setEnabled(CONFIGURATION.getMvtools2Path() != null && CONFIGURATION.getDepanPath() != null && CONFIGURATION.getMasktools2Path() != null && CONFIGURATION.getConvert2dTo3dPath() != null && CONFIGURATION.getCropResizePath() != null);

		return panel;
	}

}
