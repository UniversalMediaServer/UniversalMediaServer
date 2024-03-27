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
package net.pms.swing.gui.tabs.traces;

import java.awt.event.ActionEvent;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import net.pms.configuration.UmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A modified JTextArea which only keeps a given number of lines and disposes
 * of the oldest first when the given number is exceeded.
 *
 * @author Nadahar
 */
public class TextAreaFIFO extends JTextArea {

	private static final Logger LOGGER = LoggerFactory.getLogger(TextAreaFIFO.class);

	private final Timer removeTimer;

	private int maxLines;

	public TextAreaFIFO(int lines, int removeDelayMS) {
		maxLines = lines;
		removeTimer = new Timer(removeDelayMS, (ActionEvent e) -> removeLines());
		getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (removeTimer.isRunning()) {
					return;
				}
				removeTimer.start();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				//nothing to do
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				//nothing to do
			}
		});
	}

	public void removeLines() {
		Element root = getDocument().getDefaultRootElement();
		int remove = root.getElementCount() - maxLines;
		if (remove > 0) {
			Element line = root.getElement(remove - 1);
			try {
				getDocument().remove(0, line.getEndOffset());
			} catch (BadLocationException ble) {
				LOGGER.warn("Can't remove {} excess line{}: {}", remove, remove == 1 ? "" : "s", ble);
			}
		}
	}

	/**
	 * Get how many lines {@link TextAreaFIFO} keeps
	 * @return the current number of kept lines
	 */
	public int getMaxLines() {
		return maxLines;
	}

	/**
	 * Set how many lines {@link TextAreaFIFO} should keep
	 * @param lines the new number of kept lines
	 */
	public void setMaxLines(int lines) {
		lines = Math.min(Math.max(lines, UmsConfiguration.getLoggingLogsTabLinebufferMin()), UmsConfiguration.getLoggingLogsTabLinebufferMax());
		maxLines = lines;
	}
}
