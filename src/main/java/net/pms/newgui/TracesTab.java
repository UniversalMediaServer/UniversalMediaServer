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

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import net.pms.logging.LoggingConfigFileLoader;
import net.pms.util.FormLayoutUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TracesTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(TracesTab.class);
	private PmsConfiguration configuration;

	class PopupTriggerMouseListener extends MouseAdapter {
		private JPopupMenu popup;
		private JComponent component;

		public PopupTriggerMouseListener(JPopupMenu popup, JComponent component) {
			this.popup = popup;
			this.component = component;
		}

		// Some systems trigger popup on mouse press, others on mouse release, we want to cater for both
		private void showMenuIfPopupTrigger(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popup.show(component, e.getX() + 3, e.getY() + 3);
			}
		}

		// According to the javadocs on isPopupTrigger, checking for popup trigger on mousePressed and mouseReleased 
		// Should be all that is required

		public void mousePressed(MouseEvent e) {
			showMenuIfPopupTrigger(e);
		}

		public void mouseReleased(MouseEvent e) {
			showMenuIfPopupTrigger(e);
		}
	}
	private JTextArea jList;

	TracesTab(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public JTextArea getList() {
		return jList;
	}

	public JComponent build() {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 10:grow", orientation);

		FormLayout layout = new FormLayout(
			colSpec,
			"fill:10:grow, p");
		PanelBuilder builder = new PanelBuilder(layout);
		//  builder.setBorder(Borders.DLU14_BORDER);
		builder.setOpaque(true);

		CellConstraints cc = new CellConstraints();

		//create trace text box
		jList = new JTextArea();
		jList.setEditable(false);
		jList.setBackground(Color.WHITE);
		jList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		final JPopupMenu popup = new JPopupMenu();
		JMenuItem defaultItem = new JMenuItem(Messages.getString("TracesTab.3"));

		defaultItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jList.setText("");
			}
		});

		popup.add(defaultItem);
		jList.addMouseListener(
			new PopupTriggerMouseListener(
			popup,
			jList));

		JScrollPane pane = new JScrollPane(jList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		builder.add(pane, cc.xyw(1, 1, 2));

		// Add buttons opening log files
		JPanel pLogFileButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		HashMap<String, String> logFiles = LoggingConfigFileLoader.getLogFilePaths();
		for (String loggerName : logFiles.keySet()) {
			JButton b = new JButton(loggerName);
			b.setToolTipText(logFiles.get(loggerName));
			b.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					File logFile = new File(((JButton) e.getSource()).getToolTipText());
					try {
						java.awt.Desktop.getDesktop().open(logFile);
					} catch (IOException e1) {
						LOGGER.error(String.format("Failed to open file %s in default editor", logFile), e1);
					}
				}
			});
			pLogFileButtons.add(b);
		}
		builder.add(pLogFileButtons, cc.xy(2, 2));

		return builder.getPanel();
	}
}
