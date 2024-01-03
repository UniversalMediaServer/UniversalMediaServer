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
import com.sun.jna.Platform;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.OutputParams;
import net.pms.media.audio.MediaAudio;
import net.pms.media.MediaInfo;
import net.pms.newgui.GuiUtil;
import net.pms.newgui.components.CustomJButton;
import net.pms.newgui.util.FormLayoutUtil;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEncoderVideo {
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(MEncoderVideo.class);
	private static final String COL_SPEC = "left:pref, 3dlu, p:grow, 3dlu, right:p:grow, 3dlu, p:grow, 3dlu, right:p:grow,3dlu, p:grow, 3dlu, right:p:grow,3dlu, pref:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p";
	private static JTextField mencoderNoassScale;
	private static JTextField mencoderNoassSubpos;
	private static JTextField mencoderNoassBlur;
	private static JTextField mencoderNoassOutline;
	private static JTextField mencoderCustomOptions;
	private static JTextField subq;
	private static JTextField scaleX;
	private static JTextField scaleY;
	private static JCheckBox intelligentsync;
	private static JTextField ocw;
	private static JTextField och;

	/**
	 * This class is not meant to be instantiated.
	 */
	private MEncoderVideo() {
	}

	public static JComponent config() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);

		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("GeneralSettings_SentenceCase"), FormLayoutUtil.flip(cc.xyw(1, 1, 15), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		JCheckBox mencodermt = new JCheckBox(Messages.getString("EnableMultithreading"), CONFIGURATION.getMencoderMT());
		mencodermt.setContentAreaFilled(false);
		mencodermt.addActionListener((ActionEvent e) -> {
			CONFIGURATION.setMencoderMT(mencodermt.isSelected());
		});
		mencodermt.setEnabled(Platform.isWindows() || Platform.isMac());
		builder.add(GuiUtil.getPreferredSizeComponent(mencodermt), FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		JCheckBox skipLoopFilter = new JCheckBox(Messages.getString("SkipLoopFilterDeblocking"), CONFIGURATION.getSkipLoopFilterEnabled());
		skipLoopFilter.setContentAreaFilled(false);
		skipLoopFilter.setToolTipText(Messages.getString("CanDegradeQuality"));
		skipLoopFilter.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setSkipLoopFilterEnabled((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(skipLoopFilter), FormLayoutUtil.flip(cc.xyw(3, 3, 12), colSpec, orientation));

		JCheckBox noskip = new JCheckBox(Messages.getString("AvSyncAlternativeMethod"), CONFIGURATION.isMencoderNoOutOfSync());
		noskip.setContentAreaFilled(false);
		noskip.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMencoderNoOutOfSync((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(noskip), FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));

		CustomJButton button = new CustomJButton(Messages.getString("CodecSpecificParametersAdvanced"));
		button.addActionListener((ActionEvent e) -> {
			JPanel codecPanel = new JPanel(new BorderLayout());
			final JTextArea textArea = new JTextArea();
			textArea.setText(CONFIGURATION.getMencoderCodecSpecificConfig());
			textArea.setFont(new Font("Courier", Font.PLAIN, 12));
			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(900, 100));

			final JTextArea textAreaDefault = new JTextArea();
			textAreaDefault.setText(net.pms.encoders.MEncoderVideo.DEFAULT_CODEC_CONF_SCRIPT);
			textAreaDefault.setBackground(Color.WHITE);
			textAreaDefault.setFont(new Font("Courier", Font.PLAIN, 12));
			textAreaDefault.setEditable(false);
			textAreaDefault.setEnabled(CONFIGURATION.isMencoderIntelligentSync());
			JScrollPane scrollPaneDefault = new JScrollPane(textAreaDefault);
			scrollPaneDefault.setPreferredSize(new Dimension(900, 450));

			JPanel customPanel = new JPanel(new BorderLayout());
			intelligentsync = new JCheckBox(Messages.getString("UseApplicationDefaults"), CONFIGURATION.isMencoderIntelligentSync());
			intelligentsync.setContentAreaFilled(false);
			intelligentsync.addItemListener((ItemEvent e1) -> {
				CONFIGURATION.setMencoderIntelligentSync(e1.getStateChange() == ItemEvent.SELECTED);
				textAreaDefault.setEnabled(CONFIGURATION.isMencoderIntelligentSync());
			});

			JLabel label = new JLabel(Messages.getString("CustomParameters"));
			customPanel.add(label, BorderLayout.NORTH);
			customPanel.add(scrollPane, BorderLayout.SOUTH);

			codecPanel.add(intelligentsync, BorderLayout.NORTH);
			codecPanel.add(scrollPaneDefault, BorderLayout.CENTER);
			codecPanel.add(customPanel, BorderLayout.SOUTH);

			while (JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(codecPanel),
					codecPanel, Messages.getString("EditCodecSpecificParameters"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {
				String newCodecparam = textArea.getText();
				MediaInfo fakemedia = new MediaInfo();
				MediaAudio audio = new MediaAudio();
				audio.setCodecA("ac3");
				fakemedia.setCodecV("mpeg4");
				fakemedia.setContainer("matroska");
				fakemedia.setDuration(45d * 60);
				audio.getAudioProperties().setNumberOfChannels(2);
				fakemedia.setWidth(1280);
				fakemedia.setHeight(720);
				audio.setSampleFrequency("48000");
				fakemedia.setFrameRate("23.976");
				fakemedia.getAudioTracksList().add(audio);
				String[] result = net.pms.encoders.MEncoderVideo.getSpecificCodecOptions(newCodecparam, fakemedia, new OutputParams(CONFIGURATION), "dummy.mpg", "dummy.srt", false, true);

				if (result.length > 0 && result[0].startsWith("@@")) {
					String errorMessage = result[0].substring(2);
					JOptionPane.showMessageDialog(
							SwingUtilities.getWindowAncestor(codecPanel),
							errorMessage,
							Messages.getString("Error"),
							JOptionPane.ERROR_MESSAGE
					);
				} else {
					CONFIGURATION.setMencoderCodecSpecificConfig(newCodecparam);
					break;
				}
			}
		});
		builder.add(button, FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));

		JCheckBox forcefps = new JCheckBox(Messages.getString("ForceFramerateParsedFfmpeg"), CONFIGURATION.isMencoderForceFps());
		forcefps.setContentAreaFilled(false);
		forcefps.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMencoderForceFps(e.getStateChange() == ItemEvent.SELECTED);
		});

		builder.add(GuiUtil.getPreferredSizeComponent(forcefps), FormLayoutUtil.flip(cc.xyw(1, 7, 2), colSpec, orientation));

		JCheckBox yadif = new JCheckBox(Messages.getString("DeinterlaceFilter_Sentencecase"), CONFIGURATION.isMencoderYadif());
		yadif.setContentAreaFilled(false);
		yadif.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMencoderYadif(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(yadif), FormLayoutUtil.flip(cc.xyw(3, 7, 7), colSpec, orientation));

		JCheckBox scaler = new JCheckBox(Messages.getString("ChangeVideoResolution"));
		scaler.setContentAreaFilled(false);
		scaler.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMencoderScaler(e.getStateChange() == ItemEvent.SELECTED);
			scaleX.setEnabled(CONFIGURATION.isMencoderScaler());
			scaleY.setEnabled(CONFIGURATION.isMencoderScaler());
		});
		builder.add(GuiUtil.getPreferredSizeComponent(scaler), FormLayoutUtil.flip(cc.xyw(3, 5, 6), colSpec, orientation));

		builder.addLabel(Messages.getString("Width"), FormLayoutUtil.flip(cc.xy(9, 5, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));
		scaleX = new JTextField("" + CONFIGURATION.getMencoderScaleX());
		scaleX.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					CONFIGURATION.setMencoderScaleX(Integer.parseInt(scaleX.getText()));
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse scaleX from \"" + scaleX.getText() + "\"");
				}
			}
		});
		builder.add(scaleX, FormLayoutUtil.flip(cc.xy(11, 5), colSpec, orientation));

		builder.addLabel(Messages.getString("Height"), FormLayoutUtil.flip(cc.xy(13, 5, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));
		scaleY = new JTextField("" + CONFIGURATION.getMencoderScaleY());
		scaleY.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					CONFIGURATION.setMencoderScaleY(Integer.parseInt(scaleY.getText()));
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse scaleY from \"" + scaleY.getText() + "\"");
				}
			}
		});
		builder.add(scaleY, FormLayoutUtil.flip(cc.xy(15, 5), colSpec, orientation));

		if (CONFIGURATION.isMencoderScaler()) {
			scaler.setSelected(true);
		} else {
			scaleX.setEnabled(false);
			scaleY.setEnabled(false);
		}

		JCheckBox videoremux = new JCheckBox(Messages.getString("RemuxVideosTsmuxer"), CONFIGURATION.isMencoderMuxWhenCompatible());
		videoremux.setContentAreaFilled(false);
		videoremux.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMencoderMuxWhenCompatible((e.getStateChange() == ItemEvent.SELECTED));
		});
		builder.add(GuiUtil.getPreferredSizeComponent(videoremux), FormLayoutUtil.flip(cc.xyw(1, 9, 13), colSpec, orientation));

		JCheckBox normalizeaudio = new JCheckBox(Messages.getString("NormalizeAudioVolume"), CONFIGURATION.isMEncoderNormalizeVolume());
		normalizeaudio.setContentAreaFilled(false);
		normalizeaudio.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMEncoderNormalizeVolume((e.getStateChange() == ItemEvent.SELECTED));
		});
		// Uncomment this if volume normalizing in MEncoder is ever fixed.
		// builder.add(normalizeaudio, FormLayoutUtil.flip(cc.xyw(1, 13, 13), colSpec, orientation));

		builder.addLabel(Messages.getString("CustomOptionsVf"), FormLayoutUtil.flip(cc.xy(1, 15), colSpec, orientation));
		mencoderCustomOptions = new JTextField(CONFIGURATION.getMencoderCustomOptions());
		mencoderCustomOptions.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderCustomOptions(mencoderCustomOptions.getText());
			}
		});
		builder.add(mencoderCustomOptions, FormLayoutUtil.flip(cc.xyw(3, 15, 13), colSpec, orientation));

		builder.addLabel(Messages.getString("AddBordersOverscanCompensation"), FormLayoutUtil.flip(cc.xy(1, 17), colSpec, orientation));

		builder.addLabel(Messages.getString("Width") + " (%)", FormLayoutUtil.flip(cc.xy(1, 17, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));
		ocw = new JTextField(CONFIGURATION.getMencoderOverscanCompensationWidth());
		ocw.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderOverscanCompensationWidth(ocw.getText());
			}
		});
		builder.add(ocw, FormLayoutUtil.flip(cc.xy(3, 17), colSpec, orientation));

		builder.addLabel(Messages.getString("Height") + " (%)", FormLayoutUtil.flip(cc.xy(5, 17), colSpec, orientation));
		och = new JTextField(CONFIGURATION.getMencoderOverscanCompensationHeight());
		och.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderOverscanCompensationHeight(och.getText());
			}
		});
		builder.add(och, FormLayoutUtil.flip(cc.xy(7, 17), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("SubtitlesSettings"), FormLayoutUtil.flip(cc.xyw(1, 19, 15), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.addLabel(Messages.getString("PlaintextSubtitlesSettings"), FormLayoutUtil.flip(cc.xy(1, 27), colSpec, orientation));
		builder.addLabel(Messages.getString("FontScale"), FormLayoutUtil.flip(cc.xy(1, 27, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));

		mencoderNoassScale = new JTextField(CONFIGURATION.getMencoderNoAssScale());
		mencoderNoassScale.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderNoAssScale(mencoderNoassScale.getText());
			}
		});

		builder.addLabel(Messages.getString("FontOutline"), FormLayoutUtil.flip(cc.xy(5, 27), colSpec, orientation));

		mencoderNoassOutline = new JTextField(CONFIGURATION.getMencoderNoAssOutline());
		mencoderNoassOutline.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderNoAssOutline(mencoderNoassOutline.getText());
			}
		});

		builder.addLabel(Messages.getString("FontBlur"), FormLayoutUtil.flip(cc.xy(9, 27), colSpec, orientation));

		mencoderNoassBlur = new JTextField(CONFIGURATION.getMencoderNoAssBlur());
		mencoderNoassBlur.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderNoAssBlur(mencoderNoassBlur.getText());
			}
		});

		builder.addLabel(Messages.getString("MarginPercentage"), FormLayoutUtil.flip(cc.xy(13, 27), colSpec, orientation));

		mencoderNoassSubpos = new JTextField(CONFIGURATION.getMencoderNoAssSubPos());
		mencoderNoassSubpos.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderNoAssSubPos(mencoderNoassSubpos.getText());
			}
		});

		builder.add(mencoderNoassScale, FormLayoutUtil.flip(cc.xy(3, 27), colSpec, orientation));
		builder.add(mencoderNoassOutline, FormLayoutUtil.flip(cc.xy(7, 27), colSpec, orientation));
		builder.add(mencoderNoassBlur, FormLayoutUtil.flip(cc.xy(11, 27), colSpec, orientation));
		builder.add(mencoderNoassSubpos, FormLayoutUtil.flip(cc.xy(15, 27), colSpec, orientation));

		JCheckBox ass = new JCheckBox(Messages.getString("UseAssSubtitlesStyling"), CONFIGURATION.isMencoderAss());
		ass.setContentAreaFilled(false);
		ass.addItemListener((ItemEvent e) -> {
			if (e != null) {
				CONFIGURATION.setMencoderAss(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(ass), FormLayoutUtil.flip(cc.xy(1, 23), colSpec, orientation));
		ass.getItemListeners()[0].itemStateChanged(null);

		JCheckBox fc = new JCheckBox(Messages.getString("FonconfigEmbeddedFonts"), CONFIGURATION.isMencoderFontConfig());
		fc.setContentAreaFilled(false);
		fc.addItemListener((ItemEvent e) -> {
			CONFIGURATION.setMencoderFontConfig(e.getStateChange() == ItemEvent.SELECTED);
		});
		builder.add(GuiUtil.getPreferredSizeComponent(fc), FormLayoutUtil.flip(cc.xyw(3, 23, 5), colSpec, orientation));

		builder.addLabel(Messages.getString("DvdvobsubSubtitlesQuality"), FormLayoutUtil.flip(cc.xy(1, 29), colSpec, orientation));
		subq = new JTextField(CONFIGURATION.getMencoderVobsubSubtitleQuality());
		subq.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				CONFIGURATION.setMencoderVobsubSubtitleQuality(subq.getText());
			}
		});
		builder.add(subq, FormLayoutUtil.flip(cc.xyw(3, 29, 1), colSpec, orientation));

		CONFIGURATION.addConfigurationListener((ConfigurationEvent event) -> {
			if (event.getPropertyName() == null) {
				return;
			}
			if ((!event.isBeforeUpdate())) {
				boolean enabled = !CONFIGURATION.isDisableSubtitles();
				if (ass.isEnabled() != enabled) {
					ass.setEnabled(enabled);
					fc.setEnabled(enabled);
					mencoderNoassScale.setEnabled(enabled);
					mencoderNoassOutline.setEnabled(enabled);
					mencoderNoassBlur.setEnabled(enabled);
					mencoderNoassSubpos.setEnabled(enabled);
					ocw.setEnabled(enabled);
					och.setEnabled(enabled);
					subq.setEnabled(enabled);

					if (enabled) {
						ass.getItemListeners()[0].itemStateChanged(null);
					}
				}
			}
		});

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

}
