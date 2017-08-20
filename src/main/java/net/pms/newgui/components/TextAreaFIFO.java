/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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
package net.pms.newgui.components;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import net.pms.configuration.PmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A modified JTextArea which only keeps a given number of lines and disposes
 * of the oldest first when the given number is exceeded.
 *
 * @author Nadahar
 */
@SuppressWarnings("serial")
public class TextAreaFIFO extends JTextArea implements DocumentListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(TextAreaFIFO.class);
	private int maxLines;

    public TextAreaFIFO(int lines) {
        maxLines = lines;
        getDocument().addDocumentListener(this);
    }

    public void insertUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                removeLines();
            }
        });
    }

    public void removeUpdate(DocumentEvent e) {
    }

    public void changedUpdate(DocumentEvent e) {
    }

    public void removeLines() {
        Element root = getDocument().getDefaultRootElement();
        while (root.getElementCount() > maxLines) {
            Element firstLine = root.getElement(0);
            try {
                getDocument().remove(0, firstLine.getEndOffset());
            } catch (BadLocationException ble) {
            	LOGGER.warn("Can't remove excess lines: {}", ble);
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
		lines = Math.min(Math.max(lines, PmsConfiguration.LOGGING_LOGS_TAB_LINEBUFFER_MIN),PmsConfiguration.LOGGING_LOGS_TAB_LINEBUFFER_MAX);
    	maxLines = lines;
    }
}