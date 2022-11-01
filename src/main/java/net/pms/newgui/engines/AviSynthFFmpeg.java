/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.newgui.engines;

import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.commons.configuration.event.ConfigurationEvent;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.newgui.CustomTabbedPaneUI;
import net.pms.newgui.GuiUtil;
import net.pms.newgui.util.FormLayoutUtil;
import net.pms.newgui.util.KeyedComboBoxModel;

public class AviSynthFFmpeg {
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static JCheckBox useFFMS2;
	private static JCheckBox multithreading;
	private static JCheckBox avisynthplusmode;
	private static JCheckBox interframe;
	private static JCheckBox interframegpu;
	private static JCheckBox convertfps;
	private static JCheckBox convert2dTo3d;
	private static JComboBox<String> algorithms;
	private static JComboBox<String> formats3D;
	private static JCheckBox horizontalResize;
	private static JComboBox<String> horizontalResizeResolution;

	private static final String COMMON_COL_SPEC = "left:pref, 0:grow";

	private static ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());

	public static JComponent config() {
		return config("GeneralSettings");
	}

	protected static JComponent config(String languageLabel) {

		String colSpec = FormLayoutUtil.getColSpec(COMMON_COL_SPEC, orientation);

		FormLayout layout = new FormLayout(
			colSpec,
			"4*(pref, 3dlu), pref, 9dlu, pref, 9dlu:grow, pref");

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString(languageLabel), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		multithreading = new JCheckBox(Messages.getString("EnableMultithreading"), CONFIGURATION.isFfmpegAviSynthMultithreading());
		multithreading.setContentAreaFilled(false);
		multithreading.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAviSynthMultithreading(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(multithreading), cc.xy(2, 3));

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
		builder.add(GuiUtil.getPreferredSizeComponent(interframe), cc.xy(2, 5));

		interframegpu = new JCheckBox(Messages.getString("EnableGpuUseTrueMotion"), CONFIGURATION.getFfmpegAvisynthInterFrameGPU());
		interframegpu.setContentAreaFilled(false);
		interframegpu.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthInterFrameGPU((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(interframegpu), cc.xy(2, 7));

		CONFIGURATION.addConfigurationListener((ConfigurationEvent event) -> {
			if (event.getPropertyName() == null) {
				return;
			}
			if ((!event.isBeforeUpdate()) && event.getPropertyName().equals(PmsConfiguration.KEY_GPU_ACCELERATION)) {
				interframegpu.setEnabled(CONFIGURATION.isGPUAcceleration());
			}
		});

		convertfps = new JCheckBox(Messages.getString("EnableAvisynthVariableFramerate"), CONFIGURATION.getFfmpegAvisynthConvertFps());
		convertfps.setContentAreaFilled(false);
		convertfps.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthConvertFps((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(convertfps), cc.xy(2, 9));

		JTabbedPane setupTabbedPanel = new JTabbedPane();
		setupTabbedPanel.setUI(new CustomTabbedPaneUI());

		setupTabbedPanel.addTab(Messages.getString("Avisynth2Dto3DSettings"), build2dTo3dSetupPanel());


		if (!CONFIGURATION.isHideAdvancedOptions()) {
			builder.add(setupTabbedPanel, FormLayoutUtil.flip(cc.xywh(1, 11, 2, 3), colSpec, orientation));
		}

		return builder.getPanel();
	}

	private static JComponent build2dTo3dSetupPanel() {
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 3dlu, pref:grow", orientation);
		FormLayout layout = new FormLayout(colSpec, "$lgap, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, 2*(pref, 3dlu)");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DLU4);
		CellConstraints cc = new CellConstraints();

		convert2dTo3d = new JCheckBox(Messages.getString("EnableAvisynth2Dto3DConversion"), CONFIGURATION.getFfmpegAvisynth2Dto3D());
		convert2dTo3d.setContentAreaFilled(false);
		convert2dTo3d.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				CONFIGURATION.setFfmpegAvisynth2Dto3D((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(convert2dTo3d), FormLayoutUtil.flip(cc.xy(1, 2), colSpec, orientation));

		builder.addLabel(Messages.getString("AlgorithmForConverting2Dto3D"), FormLayoutUtil.flip(cc.xy(1, 4), colSpec, orientation));

		Integer[] keys = new Integer[] {1, 2};
		String[] values = new String[] {
			Messages.getString("PulfrichBase2Dto3DAlgorithm"),
			Messages.getString("PulfrichLighting2Dto3DAlgorithm")
		};

		final KeyedComboBoxModel<Integer, String> algorithmForConverting2Dto3D = new KeyedComboBoxModel<>(keys, values);
		algorithms = new JComboBox<>(algorithmForConverting2Dto3D);
		algorithms.setEditable(false);
		algorithmForConverting2Dto3D.setSelectedKey(CONFIGURATION.getFfmpegAvisynthConversionAlgorithm2Dto3D());
		algorithms.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				CONFIGURATION.setFfmpegAvisynthConversionAlgorithm2Dto3D(algorithmForConverting2Dto3D.getSelectedKey());
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(algorithms), FormLayoutUtil.flip(cc.xy(3, 4), colSpec, orientation));

		builder.addLabel(Messages.getString("OutputFormat3D"), FormLayoutUtil.flip(cc.xy(1, 6), colSpec, orientation));

		keys = new Integer[] {1, 2, 3, 4, 5, 6};
		values = new String[] {
			Messages.getString("FullSBS3dFormat"),
			Messages.getString("FullTB3dFormat"),
			Messages.getString("HalfSBS3dFormat"),
			Messages.getString("HalfTB3dFormat"),
			Messages.getString("HalfUpSBS3dFormat"),
			Messages.getString("HalfUpTB3dFormat")
		};

		final KeyedComboBoxModel<Integer, String> outputFormat3D = new KeyedComboBoxModel<>(keys, values);
		formats3D = new JComboBox<>(outputFormat3D);
		formats3D.setEditable(false);
		outputFormat3D.setSelectedKey(CONFIGURATION.getFfmpegAvisynthOutputFormat3D());
		formats3D.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				CONFIGURATION.setFfmpegAvisynthOutputFormat3D(outputFormat3D.getSelectedKey());
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(formats3D), FormLayoutUtil.flip(cc.xy(3, 6), colSpec, orientation));

		horizontalResize = new JCheckBox(Messages.getString("HorizontalResize"), CONFIGURATION.getFfmpegAvisynthHorizontalResize());
		horizontalResize.setContentAreaFilled(false);
		horizontalResize.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				CONFIGURATION.setFfmpegAvisynthHorizontalResize((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(horizontalResize), FormLayoutUtil.flip(cc.xy(1, 8), colSpec, orientation));

		// builder.addLabel(Messages.getString("HorizontalResizeResolution"), cc.xy(1, 8));

		String[] resolutions = new String[] {"7680", "3840", "1920", "1280", "852", "768", "720", "704", "640", "544", "480", "352", "120" };

		horizontalResizeResolution = new JComboBox<>(resolutions);
		horizontalResizeResolution.setSelectedItem(CONFIGURATION.getFfmpegAvisynthHorizontalResizeResolution());
		horizontalResizeResolution.setToolTipText(Messages.getString("TheNumberGpuThreads"));

		horizontalResizeResolution.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthHorizontalResizeResolution((String) e.getItem());
		});
		horizontalResizeResolution.setEditable(true);
		builder.add(GuiUtil.getPreferredSizeComponent(horizontalResizeResolution), cc.xy(3, 8));

		avisynthplusmode = new JCheckBox(Messages.getString("EnableAvisynthPlusMode"), CONFIGURATION.isFfmpegAviSynthPlusMode());
		avisynthplusmode.setContentAreaFilled(false);
		avisynthplusmode.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAviSynthPlusMode(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(avisynthplusmode), FormLayoutUtil.flip(cc.xy(1, 10), colSpec, orientation));


		useFFMS2 = new JCheckBox(Messages.getString("EnableAvisynthUseFFMS2"), CONFIGURATION.getFfmpegAvisynthUseFFMS2());
		useFFMS2.setContentAreaFilled(false);
		useFFMS2.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegAvisynthUseFFMS2(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(useFFMS2), FormLayoutUtil.flip(cc.xy(1, 12), colSpec, orientation));

		JPanel panel = builder.getPanel();
		panel.applyComponentOrientation(orientation);

		return panel;
	}

}
