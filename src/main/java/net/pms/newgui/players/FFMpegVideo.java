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
package net.pms.newgui.players;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.encoders.FFmpegLogLevels;
import net.pms.newgui.GuiUtil;

public class FFMpegVideo {
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static JComboBox<String> fFmpegLoggingLevel;
	private static JCheckBox multithreading;
	private static JCheckBox videoRemuxTsMuxer;
	private static JCheckBox fc;
	private static JCheckBox deferToMEncoderForSubtitles;
	private static JCheckBox isFFmpegSoX;
	private static JComboBox<String> fFmpegGPUDecodingAccelerationMethod;
	private static JComboBox<String> fFmpegGPUDecodingAccelerationThreadNumber;

	public static JComponent config() {
		return config("GeneralSettings");
	}

	private static JComponent config(String languageLabel) {
		FormLayout layout = new FormLayout(
			"left:pref, 3dlu, pref",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();
		int y = 1;
		JComponent cmp = builder.addSeparator(Messages.getString(languageLabel), cc.xyw(1, y, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		y += 2;
		builder.addLabel(Messages.getString("LogLevel"), cc.xy(1, y));
		fFmpegLoggingLevel = new JComboBox<>(FFmpegLogLevels.getLabels());
		fFmpegLoggingLevel.setSelectedItem(CONFIGURATION.getFFmpegLoggingLevel());
		fFmpegLoggingLevel.setToolTipText(Messages.getString("SetFfmpegLoggingLevelDecides"));
		fFmpegLoggingLevel.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				CONFIGURATION.setFFmpegLoggingLevel((String) e.getItem());
			}
		});
		fFmpegLoggingLevel.setEditable(true);
		builder.add(GuiUtil.getPreferredSizeComponent(fFmpegLoggingLevel), cc.xy(3, y));

		y += 2;
		multithreading = new JCheckBox(Messages.getString("EnableMultithreading"), CONFIGURATION.isFfmpegMultithreading());
		multithreading.setContentAreaFilled(false);
		multithreading.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFfmpegMultithreading(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(multithreading), cc.xy(1, y));

		y += 2;
		videoRemuxTsMuxer = new JCheckBox(Messages.getString("RemuxVideosTsmuxer"), CONFIGURATION.isFFmpegMuxWithTsMuxerWhenCompatible());
		videoRemuxTsMuxer.setContentAreaFilled(false);
		videoRemuxTsMuxer.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFFmpegMuxWithTsMuxerWhenCompatible(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(videoRemuxTsMuxer), cc.xy(1, y));

		y += 2;
		fc = new JCheckBox(Messages.getString("UseFontSettings"), CONFIGURATION.isFFmpegFontConfig());
		fc.setContentAreaFilled(false);
		fc.setToolTipText(Messages.getString("FontSettingAppliedEmbeddedExternal"));
		fc.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFFmpegFontConfig(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(fc), cc.xy(1, y));

		y += 2;
		deferToMEncoderForSubtitles = new JCheckBox(Messages.getString("DeferMencoderTranscodingProblematic"), CONFIGURATION.isFFmpegDeferToMEncoderForProblematicSubtitles());
		deferToMEncoderForSubtitles.setContentAreaFilled(false);
		deferToMEncoderForSubtitles.setToolTipText(Messages.getString("MencoderMoreStableFfmpegTranscoding"));
		deferToMEncoderForSubtitles.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFFmpegDeferToMEncoderForProblematicSubtitles(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(deferToMEncoderForSubtitles), cc.xy(1, y));

		y += 2;
		isFFmpegSoX = new JCheckBox(Messages.getString("UseSoxHigherQualityAudio"), CONFIGURATION.isFFmpegSoX());
		isFFmpegSoX.setContentAreaFilled(false);
		isFFmpegSoX.setToolTipText(Messages.getString("ThisMayIncreaseAudioQuality"));
		isFFmpegSoX.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFFmpegSoX(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(isFFmpegSoX), cc.xy(1, y));

		y += 2;
		builder.add(new JLabel(Messages.getString("GPUDecodingAccelerationMethod")), cc.xy(1, y));

		String[] keys = CONFIGURATION.getFFmpegAvailableGPUDecodingAccelerationMethods();

		fFmpegGPUDecodingAccelerationMethod = new JComboBox<>(keys);
		fFmpegGPUDecodingAccelerationMethod.setSelectedItem(CONFIGURATION.getFFmpegGPUDecodingAccelerationMethod());
		fFmpegGPUDecodingAccelerationMethod.setToolTipText(Messages.getString("RecommendationIsAuto"));
		fFmpegGPUDecodingAccelerationMethod.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				CONFIGURATION.setFFmpegGPUDecodingAccelerationMethod((String) e.getItem());
			}
		});
		fFmpegGPUDecodingAccelerationMethod.setEditable(true);
		builder.add(GuiUtil.getPreferredSizeComponent(fFmpegGPUDecodingAccelerationMethod), cc.xy(3, y));

		y += 2;
		builder.addLabel(Messages.getString("GpuDecodingThreadCount"), cc.xy(1, y));
		String[] threads = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};

		fFmpegGPUDecodingAccelerationThreadNumber = new JComboBox<>(threads);
		fFmpegGPUDecodingAccelerationThreadNumber.setSelectedItem(CONFIGURATION.getFFmpegGPUDecodingAccelerationThreadNumber());
		fFmpegGPUDecodingAccelerationThreadNumber.setToolTipText(Messages.getString("TheNumberGpuThreads"));

		fFmpegGPUDecodingAccelerationThreadNumber.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setFFmpegGPUDecodingAccelerationThreadNumber((String) e.getItem());
		});
		fFmpegGPUDecodingAccelerationThreadNumber.setEditable(true);
		builder.add(GuiUtil.getPreferredSizeComponent(fFmpegGPUDecodingAccelerationThreadNumber), cc.xy(3, y));

		return builder.getPanel();
	}

}
