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
package net.pms.newgui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectRenderers extends JPanel implements ItemListener, ActionListener {
	private static final long serialVersionUID = -2724796596060834064L;
	private final static PmsConfiguration configuration = PMS.getConfiguration();
	private final List<JCheckBox> checkBoxes = new ArrayList<>();
	private JButton selectAll = new JButton(Messages.getString("GeneralTab.7"));
	private JButton deselectAll = new JButton(Messages.getString("GeneralTab.8"));
	private static ArrayList<String> allRenderersNames;
	private static final Logger LOGGER = LoggerFactory.getLogger(SelectRenderers.class);

	public SelectRenderers() {
		super(new BorderLayout());
		JPanel checkPanel = new JPanel(new GridLayout(0, 3));

		selectAll.addActionListener(this);
		checkPanel.add(selectAll);

		deselectAll.addActionListener(this);
		checkPanel.add(deselectAll);

		checkPanel.add(new JLabel(""));
		checkPanel.add(new JLabel("____________________________"));
		checkPanel.add(new JLabel("____________________________"));
		checkPanel.add(new JLabel("____________________________"));

		String ignoredRenderers = configuration.getIgnoredRenderers();

		for (String rendererName : allRenderersNames) {
			JCheckBox checkbox = new JCheckBox(rendererName, !ignoredRenderers.contains(rendererName));
			checkbox.addItemListener(this);
			checkBoxes.add(checkbox);
			checkPanel.add(checkbox);
		}

		checkPanel.applyComponentOrientation(ComponentOrientation.getOrientation(new Locale(configuration.getLanguage())));
		add(checkPanel, BorderLayout.LINE_START);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	}

	/**
	 * Listens to the buttons.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source instanceof JButton) {
			if (source == selectAll) {
				deselectAll.setSelected(false);
				for (JCheckBox checkBox : checkBoxes) {
					checkBox.setSelected(true);
				}
			} else if (source == deselectAll) {
				selectAll.setSelected(false);
				for (JCheckBox checkBox : checkBoxes) {
					checkBox.setSelected(false);
				}
			}
		}
	}

	/**
	 * Listens to the check boxes.
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBox) {
			StringBuilder ignoredRenders = new StringBuilder();
			for (int i = 0; i < allRenderersNames.size(); i++) {
				if (!checkBoxes.get(i).isSelected()) {
					ignoredRenders.append(allRenderersNames.get(i)).append(",");
				}
			}

			configuration.setIgnoredRenderers(ignoredRenders.toString());
		}
	}

	/**
	 * Create the GUI and show it.
	 */
	public static void showDialog() {
		allRenderersNames = RendererConfiguration.getAllRenderersNames();
		int selectRenderers = JOptionPane.showOptionDialog(
			null,
			new SelectRenderers(),
			Messages.getString("GeneralTab.5"),
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			null
		);
		if (selectRenderers == JOptionPane.YES_OPTION) {
			try {
				configuration.save();
			} catch (ConfigurationException e) {
				LOGGER.error("Could not save configuration", e);
			}
		}
	}
}
