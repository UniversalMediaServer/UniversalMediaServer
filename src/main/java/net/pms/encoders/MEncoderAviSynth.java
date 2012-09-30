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
package net.pms.encoders;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.util.StringTokenizer;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEncoderAviSynth extends MEncoderVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(MEncoderAviSynth.class);
	public MEncoderAviSynth(PmsConfiguration configuration) {
		super(configuration);
	}

	public static final String ID = "avsmencoder";

	private JTextArea textArea;
	private JCheckBox convertfps;
	private JCheckBox interframe;
	private JCheckBox interframegpu;
	private JCheckBox multithreading;

	@Override
	public JComponent config() {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu,  0:grow"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		multithreading = new JCheckBox(Messages.getString("MEncoderVideo.35"));
		multithreading.setContentAreaFilled(false);
		if (PMS.getConfiguration().getAvisynthMultiThreading()) {
			multithreading.setSelected(true);
		}
		multithreading.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				PMS.getConfiguration().setAvisynthMultiThreading((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(multithreading, cc.xy(2, 3));

		interframe = new JCheckBox(Messages.getString("MEncoderAviSynth.13"));
		interframe.setContentAreaFilled(false);
		if (PMS.getConfiguration().getAvisynthInterFrame()) {
			interframe.setSelected(true);
		}
		interframe.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PMS.getConfiguration().setAvisynthInterFrame(interframe.isSelected());
				if (PMS.getConfiguration().getAvisynthInterFrame()) {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("MEncoderAviSynth.16"),
						"Information",
						JOptionPane.INFORMATION_MESSAGE
					);
				}
			}
		});
		builder.add(interframe, cc.xy(2, 5));

		interframegpu = new JCheckBox(Messages.getString("MEncoderAviSynth.15"));
		interframegpu.setContentAreaFilled(false);
		if (PMS.getConfiguration().getAvisynthInterFrameGPU()) {
			interframegpu.setSelected(true);
		}
		interframegpu.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				PMS.getConfiguration().setAvisynthInterFrameGPU((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(interframegpu, cc.xy(2, 7));

		convertfps = new JCheckBox(Messages.getString("MEncoderAviSynth.3"));
		convertfps.setContentAreaFilled(false);
		if (PMS.getConfiguration().getAvisynthConvertFps()) {
			convertfps.setSelected(true);
		}
		convertfps.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				PMS.getConfiguration().setAvisynthConvertFps((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(convertfps, cc.xy(2, 9));

		String aviSynthScriptInstructions = Messages.getString("MEncoderAviSynth.4") +
			Messages.getString("MEncoderAviSynth.5") +
			Messages.getString("MEncoderAviSynth.6") +
			Messages.getString("MEncoderAviSynth.7") +
			Messages.getString("MEncoderAviSynth.8");
		JTextArea aviSynthScriptInstructionsContainer = new JTextArea(aviSynthScriptInstructions);
		aviSynthScriptInstructionsContainer.setEditable(false);
		aviSynthScriptInstructionsContainer.setBorder(BorderFactory.createEtchedBorder());
		aviSynthScriptInstructionsContainer.setBackground(new Color(255, 255, 192));
		aviSynthScriptInstructionsContainer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(130, 135, 144)), BorderFactory.createEmptyBorder(3, 5, 3, 5)));
		builder.add(aviSynthScriptInstructionsContainer, cc.xy(2, 11));

		String clip = PMS.getConfiguration().getAvisynthScript();
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
		textArea.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

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
				PMS.getConfiguration().setAvisynthScript(sb.toString());
			}
		});

		JScrollPane pane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setPreferredSize(new Dimension(500, 350));
		builder.add(pane, cc.xy(2, 13));


		return builder.getPanel();
	}

	@Override
	public int purpose() {
		return VIDEO_SIMPLEFILE_PLAYER;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean avisynth() {
		return true;
	}

	@Override
	public String name() {
		return "AviSynth/MEncoder";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		if (resource == null || resource.getFormat().getType() != Format.VIDEO) {
			return false;
		}

		DLNAMediaSubtitle subtitle = resource.getMediaSubtitle();

		// Check whether the subtitle actually has a language defined,
		// Uninitialized DLNAMediaSubtitle objects have a null language.
		if (subtitle != null && subtitle.getLang() != null) {
			// The resource needs a subtitle, but this engine does not support subtitles
			return false;
		}

		try {
			String audioTrackName = resource.getMediaAudio().toString();
			String defaultAudioTrackName = resource.getMedia().getAudioTracksList().get(0).toString();
	
			if (!audioTrackName.equals(defaultAudioTrackName)) {
				// This engine only supports playback of the default audio track
				return false;
			}
		} catch (NullPointerException e) {
			LOGGER.trace("MEncoder/AviSynth cannot determine compatibility based on audio track for " + resource.getSystemName());
		} catch (IndexOutOfBoundsException e) {
			LOGGER.trace("MEncoder/AviSynth cannot determine compatibility based on default audio track for " + resource.getSystemName());
		}

		Format format = resource.getFormat();

		if (format != null) {
			Format.Identifier id = format.getIdentifier();

			if (id.equals(Format.Identifier.MKV)
					|| id.equals(Format.Identifier.MPG)) {
				return true;
			}
		}

		return false;
	}
}
