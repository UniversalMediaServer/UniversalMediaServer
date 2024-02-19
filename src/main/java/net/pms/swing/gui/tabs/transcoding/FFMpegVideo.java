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
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.encoders.FFmpegLogLevels;
import net.pms.swing.SwingUtil;
import net.pms.swing.gui.UmsFormBuilder;

public class FFMpegVideo {
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * This class is not meant to be instantiated.
	 */
	private FFMpegVideo() {
	}

	public static JComponent config() {
		return config("GeneralSettings");
	}

	private static JComponent config(String languageLabel) {
		FormLayout layout = new FormLayout(
			"left:pref, 3dlu, pref",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"
		);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();
		int y = 1;
		builder.addSeparator(Messages.getGuiString(languageLabel)).at(cc.xyw(1, y, 1));

		y += 2;
		builder.addLabel(Messages.getGuiString("LogLevel")).at(cc.xy(1, y));
		JComboBox<String> fFmpegLoggingLevel = new JComboBox<>(FFmpegLogLevels.getLabels());
		fFmpegLoggingLevel.setSelectedItem(CONFIGURATION.getFFmpegLoggingLevel());
		fFmpegLoggingLevel.setToolTipText(Messages.getGuiString("SetFfmpegLoggingLevelDecides"));
		fFmpegLoggingLevel.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				CONFIGURATION.setFFmpegLoggingLevel((String) e.getItem());
			}
		});
		fFmpegLoggingLevel.setEditable(true);
		builder.add(SwingUtil.getPreferredSizeComponent(fFmpegLoggingLevel)).at(cc.xy(3, y));

		y += 2;
		JCheckBox multithreading = new JCheckBox(Messages.getGuiString("EnableMultithreading"), CONFIGURATION.isFfmpegMultithreading());
		multithreading.setContentAreaFilled(false);
		multithreading.addItemListener((ItemEvent e) -> CONFIGURATION.setFfmpegMultithreading(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(multithreading)).at(cc.xy(1, y));

		y += 2;
		JCheckBox videoRemuxTsMuxer = new JCheckBox(Messages.getGuiString("RemuxVideosTsmuxer"), CONFIGURATION.isFFmpegMuxWithTsMuxerWhenCompatible());
		videoRemuxTsMuxer.setContentAreaFilled(false);
		videoRemuxTsMuxer.addItemListener((ItemEvent e) -> CONFIGURATION.setFFmpegMuxWithTsMuxerWhenCompatible(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(videoRemuxTsMuxer)).at(cc.xy(1, y));

		y += 2;
		JCheckBox fc = new JCheckBox(Messages.getGuiString("UseFontSettings"), CONFIGURATION.isFFmpegFontConfig());
		fc.setContentAreaFilled(false);
		fc.setToolTipText(Messages.getGuiString("FontSettingAppliedEmbeddedExternal"));
		fc.addItemListener((ItemEvent e) -> CONFIGURATION.setFFmpegFontConfig(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(fc)).at(cc.xy(1, y));

		y += 2;
		JCheckBox deferToMEncoderForSubtitles = new JCheckBox(Messages.getGuiString("DeferMencoderTranscodingProblematic"), CONFIGURATION.isFFmpegDeferToMEncoderForProblematicSubtitles());
		deferToMEncoderForSubtitles.setContentAreaFilled(false);
		deferToMEncoderForSubtitles.setToolTipText(Messages.getGuiString("MencoderMoreStableFfmpegTranscoding"));
		deferToMEncoderForSubtitles.addItemListener((ItemEvent e) -> CONFIGURATION.setFFmpegDeferToMEncoderForProblematicSubtitles(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(deferToMEncoderForSubtitles)).at(cc.xy(1, y));

		y += 2;
		JCheckBox isFFmpegSoX = new JCheckBox(Messages.getGuiString("UseSoxHigherQualityAudio"), CONFIGURATION.isFFmpegSoX());
		isFFmpegSoX.setContentAreaFilled(false);
		isFFmpegSoX.setToolTipText(Messages.getGuiString("ThisMayIncreaseAudioQuality"));
		isFFmpegSoX.addItemListener((ItemEvent e) -> CONFIGURATION.setFFmpegSoX(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(isFFmpegSoX)).at(cc.xy(1, y));

		y += 2;
		builder.add(new JLabel(Messages.getGuiString("GPUDecodingAccelerationMethod"))).at(cc.xy(1, y));

		String[] keys = CONFIGURATION.getFFmpegAvailableGPUDecodingAccelerationMethods();

		JComboBox<String> fFmpegGPUDecodingAccelerationMethod = new JComboBox<>(keys);
		fFmpegGPUDecodingAccelerationMethod.setSelectedItem(CONFIGURATION.getFFmpegGPUDecodingAccelerationMethod());
		fFmpegGPUDecodingAccelerationMethod.setToolTipText(Messages.getGuiString("RecommendationIsAuto"));
		fFmpegGPUDecodingAccelerationMethod.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				CONFIGURATION.setFFmpegGPUDecodingAccelerationMethod((String) e.getItem());
			}
		});
		fFmpegGPUDecodingAccelerationMethod.setEditable(true);
		builder.add(SwingUtil.getPreferredSizeComponent(fFmpegGPUDecodingAccelerationMethod)).at(cc.xy(3, y));

		y += 2;
		builder.addLabel(Messages.getGuiString("GpuDecodingThreadCount")).at(cc.xy(1, y));
		String[] threads = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};

		JComboBox<String> fFmpegGPUDecodingAccelerationThreadNumber = new JComboBox<>(threads);
		fFmpegGPUDecodingAccelerationThreadNumber.setSelectedItem(CONFIGURATION.getFFmpegGPUDecodingAccelerationThreadNumber());
		fFmpegGPUDecodingAccelerationThreadNumber.setToolTipText(Messages.getGuiString("TheNumberGpuThreads"));

		fFmpegGPUDecodingAccelerationThreadNumber.addItemListener((ItemEvent e) -> CONFIGURATION.setFFmpegGPUDecodingAccelerationThreadNumber((String) e.getItem()));
		fFmpegGPUDecodingAccelerationThreadNumber.setEditable(true);
		builder.add(SwingUtil.getPreferredSizeComponent(fFmpegGPUDecodingAccelerationThreadNumber)).at(cc.xy(3, y));

		y += 2;
		builder.add(new JLabel(Messages.getGuiString("AVCH264GPUEncodingAccelerationMethod"))).at(cc.xy(1, y));

		String[] keysH264 = UmsConfiguration.getFFmpegAvailableGPUH264EncodingAccelerationMethods();

		JComboBox<String> fFmpegGPUH264EncodingAccelerationMethod = new JComboBox<>(keysH264);
		fFmpegGPUH264EncodingAccelerationMethod.setSelectedItem(CONFIGURATION.getFFmpegGPUH264EncodingAccelerationMethod());
		fFmpegGPUH264EncodingAccelerationMethod.setToolTipText(Messages.getGuiString("NvidiaAndAmdEncoders"));
		fFmpegGPUH264EncodingAccelerationMethod.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				CONFIGURATION.setFFmpegGPUH264EncodingAccelerationMethod((String) e.getItem());
			}
		});
		fFmpegGPUH264EncodingAccelerationMethod.setEditable(true);
		builder.add(SwingUtil.getPreferredSizeComponent(fFmpegGPUH264EncodingAccelerationMethod)).at(cc.xy(3, y));

		y += 2;
		builder.add(new JLabel(Messages.getGuiString("HEVCH265GPUEncodingAccelerationMethod"))).at(cc.xy(1, y));

		String[] keysH265 = UmsConfiguration.getFFmpegAvailableGPUH265EncodingAccelerationMethods();

		JComboBox<String> fFmpegGPUH265EncodingAccelerationMethod = new JComboBox<>(keysH265);
		fFmpegGPUH265EncodingAccelerationMethod.setSelectedItem(CONFIGURATION.getFFmpegGPUH265EncodingAccelerationMethod());
		fFmpegGPUH265EncodingAccelerationMethod.setToolTipText(Messages.getGuiString("NvidiaAndAmdEncoders"));
		fFmpegGPUH265EncodingAccelerationMethod.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				CONFIGURATION.setFFmpegGPUH265EncodingAccelerationMethod((String) e.getItem());
			}
		});
		fFmpegGPUH265EncodingAccelerationMethod.setEditable(true);
		builder.add(SwingUtil.getPreferredSizeComponent(fFmpegGPUH265EncodingAccelerationMethod)).at(cc.xy(3, y));

		return builder.getPanel();
	}

}
