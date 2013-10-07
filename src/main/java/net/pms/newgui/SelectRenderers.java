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

public class SelectRenderers extends JPanel implements ItemListener {
	private static final long serialVersionUID = -2724796596060834064L;
	private final static PmsConfiguration configuration = PMS.getConfiguration();
	Locale locale = new Locale(configuration.getLanguage());
	ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
	private final List<JCheckBox> checkBoxes;
	private JCheckBox selectAll = new JCheckBox(Messages.getString("GeneralTab.7"));
	private JCheckBox deselectAll = new JCheckBox(Messages.getString("GeneralTab.8"));
	private static ArrayList<String> allRenderersNames;
	private static final Logger LOGGER = LoggerFactory.getLogger(SelectRenderers.class);

	public SelectRenderers() {
		super(new BorderLayout());
		String rendererName;
		JPanel checkPanel = new JPanel(new GridLayout(0, 3));

		selectAll.setSelected(false);
		selectAll.addItemListener(this);
		checkPanel.add(selectAll);

		deselectAll.setSelected(false);
		deselectAll.addItemListener(this);
		checkPanel.add(deselectAll);

		checkPanel.add(new JLabel(""));
		checkPanel.add(new JLabel("____________________________"));
		checkPanel.add(new JLabel("____________________________"));
		checkPanel.add(new JLabel("____________________________"));

		checkBoxes = new ArrayList<>();

		for (int i = 0; i < allRenderersNames.size(); i++) {
			rendererName = allRenderersNames.get(i);
			JCheckBox checkbox = new JCheckBox(rendererName);
			if (configuration.getIgnoredRenderers().contains(rendererName)) {
				checkbox.setSelected(false);
			} else {
				checkbox.setSelected(true);
			}

			checkbox.addItemListener(this);
			checkBoxes.add(checkbox);
			checkPanel.add(checkBoxes.get(i));
		}

		checkPanel.applyComponentOrientation(orientation);
		add(checkPanel, BorderLayout.LINE_START);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	}

	/**
	 * Listens to the check boxes.
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		StringBuilder ignoredRenders = new StringBuilder();
		if (source == selectAll) {
			deselectAll.setSelected(false);
			for (int i = 0; i < checkBoxes.size(); i++) {
				checkBoxes.get(i).setSelected(true);
			}
		} else if (source == deselectAll) {
			selectAll.setSelected(false);
			for (int i = 0; i < checkBoxes.size(); i++) {
				checkBoxes.get(i).setSelected(false);
			}
		} else {
			selectAll.setSelected(false);
			deselectAll.setSelected(false);
		}

		for (int i = 0; i < allRenderersNames.size(); i++) {
			if (!checkBoxes.get(i).isSelected()) {
				ignoredRenders.append(allRenderersNames.get(i)).append(",");
			}
		}

		configuration.setIgnoredRenderers(ignoredRenders.toString());
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
			null, null, null);
		if (selectRenderers == JOptionPane.YES_OPTION) {
			try {
				configuration.save();
			} catch (ConfigurationException e) {
				LOGGER.error("Could not save configuration", e);
			}
		}
	}
}
