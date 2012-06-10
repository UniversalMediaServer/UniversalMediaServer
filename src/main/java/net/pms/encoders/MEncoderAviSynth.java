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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.util.StringTokenizer;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;

public class MEncoderAviSynth extends MEncoderVideo {
	public MEncoderAviSynth(PmsConfiguration configuration) {
		super(configuration);
	}

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


		JComponent cmp = builder.addSeparator(Messages.getString("MEncoderAviSynth.2"), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		multithreading = new JCheckBox(Messages.getString("MEncoderAviSynth.14"));
		multithreading.setContentAreaFilled(false);
		if (PMS.getConfiguration().getAvisynthMultiThreading()) {
			multithreading.setSelected(true);
		}
		multithreading.addItemListener(new ItemListener() {
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
						JOptionPane.INFORMATION_MESSAGE);
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
			public void itemStateChanged(ItemEvent e) {
				PMS.getConfiguration().setAvisynthConvertFps((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(convertfps, cc.xy(2, 9));

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
		builder.add(pane, cc.xy(2, 11));


		return builder.getPanel();
	}

	@Override
	public int purpose() {
		return VIDEO_SIMPLEFILE_PLAYER;
	}
	public static final String ID = "avsmencoder";

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
}
