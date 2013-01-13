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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.logging.LoggingConfigFileLoader;
import net.pms.util.FormLayoutUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		@Override
		public void mousePressed(MouseEvent e) {
			showMenuIfPopupTrigger(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			showMenuIfPopupTrigger(e);
		}
	}

	private JTextArea jList;
	protected JScrollPane jListPane;

	TracesTab(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public JTextArea getList() {
		return jList;
	}
	
	public void append(String msg) {
		DateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
		Date date = new Date();

		String[] messageDisplay = msg.replaceFirst("]", "string that should never match").split("string that should never match");
		getList().append(dateFormat.format(date) + " " + messageDisplay[1]);
		final JScrollBar vbar = jListPane.getVerticalScrollBar();

		// If scrollbar was already at the bottom we schedule a new
		// scroll event to scroll to the bottom again
		if (vbar.getMaximum() == vbar.getValue() + vbar.getVisibleAmount()) {
			EventQueue.invokeLater (new Runnable() {
				@Override
				public void run() {
					vbar.setValue(vbar.getMaximum());
				}
			});
		}
	}

	public JComponent build() {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 10:grow", orientation);

		FormLayout layout = new FormLayout(
			colSpec,
			"fill:10:grow, p"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setOpaque(true);

		CellConstraints cc = new CellConstraints();

		// Create traces text box
		jList = new JTextArea();
		jList.setEditable(false);
		jList.setBackground(Color.WHITE);
		jList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		final JPopupMenu popup = new JPopupMenu();
		JMenuItem defaultItem = new JMenuItem(Messages.getString("TracesTab.3"));

		defaultItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jList.setText("");
			}
		});

		popup.add(defaultItem);
		jList.addMouseListener(new PopupTriggerMouseListener(popup, jList));

		jListPane = new JScrollPane(jList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jListPane.setBorder(BorderFactory.createEmptyBorder());
		builder.add(jListPane, cc.xyw(1, 1, 2));

		// Add buttons to open logfiles (there may be more than one)
		JPanel pLogFileButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		HashMap<String, String> logFiles = LoggingConfigFileLoader.getLogFilePaths();
		for (String loggerName : logFiles.keySet()) {
			String loggerNameDisplay = loggerName;
			if ("debug.log".equals(loggerName)) {
				loggerNameDisplay = Messages.getString("TracesTab.5");
			}
			CustomJButton b = new CustomJButton(loggerNameDisplay);
			b.setToolTipText(logFiles.get(loggerName));
			b.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					File logFile = new File(((CustomJButton) e.getSource()).getToolTipText());
					try {
						java.awt.Desktop.getDesktop().open(logFile);
					} catch (IOException ioe) {
						LOGGER.error(String.format("Failed to open file %s in default editor", logFile), ioe);
					} catch (UnsupportedOperationException usoe) {
						LOGGER.error(String.format("Failed to open file %s in default editor", logFile), usoe);
					}
				}
			});
			pLogFileButtons.add(b);
		}
		builder.add(pLogFileButtons, cc.xy(2, 2));

		CustomJButton packDbg = new CustomJButton(Messages.getString("TracesTab.4"));
		packDbg.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JComponent comp = PMS.get().dbgPack().config();
				String[] cancelStr = {"Close"};
				JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
					comp, "Options", JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE, null, cancelStr, null);
			}
		});
		builder.add(packDbg, cc.xy(1, 2));

		return builder.getPanel();
	}
}
