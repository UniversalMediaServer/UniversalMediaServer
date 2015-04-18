package net.pms.newgui.components;


import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ToolTipUI;

import net.pms.PMS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Source: https://community.oracle.com/thread/1363271
public class HyperLinkToolTip extends JToolTip {

	private static final long serialVersionUID = -8107203112982951774L;
	private static final Logger LOGGER = LoggerFactory.getLogger(HyperLinkToolTip.class);	
	private static ColorUIResource fg = new ColorUIResource(PMS.getConfiguration().getToolTipForegroundColor());
	private static ColorUIResource bg = new ColorUIResource(PMS.getConfiguration().getToolTipBackgroundColor());
	
	private JEditorPane editorPane;

	public HyperLinkToolTip() {
		setLayout(new BorderLayout());
		editorPane = new JEditorPane();
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		editorPane.setContentType("text/html");
		editorPane.setEditable(false);
		editorPane.setForeground(fg);
		editorPane.setBackground(bg);

		editorPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if(Desktop.isDesktopSupported())
					{
						try {
							Desktop.getDesktop().browse(new URI(e.getDescription()));
							closeToolTip();
						} catch (IOException e1) {
							LOGGER.error("Failed to open hyperlink", e1);
						} catch (URISyntaxException e1) {
							LOGGER.error("Failed to open hyperlink", e1);
						}
					} else {
						LOGGER.warn("Desktop is not supported, the clicked link can't be opened");						
					}
				}
			}
		});
		add(editorPane);
	}

	public void setTipText(String tipText) {
		editorPane.setText(tipText);
	}

	public void updateUI() {
		setUI(new ToolTipUI() { });
	}
	
	private void closeToolTip() {
		setVisible(false);
	}
}
