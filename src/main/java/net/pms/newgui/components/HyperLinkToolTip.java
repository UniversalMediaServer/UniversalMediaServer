package net.pms.newgui.components;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.html.StyleSheet;
import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Based on https://community.oracle.com/thread/1363271
public class HyperLinkToolTip extends JToolTip {

	private static final long serialVersionUID = -8107203112982951774L;
	private static final Logger LOGGER = LoggerFactory.getLogger(HyperLinkToolTip.class);
	private static ColorUIResource fg = new ColorUIResource(PMS.getConfiguration().getToolTipForegroundColor());
	private static ColorUIResource bg = new ColorUIResource(PMS.getConfiguration().getToolTipBackgroundColor());
	private static CustomHTMLEditorKit editorKit;

	/*
	 * Creates a static, shared (between instances of this class only) instance
	 * of a EditorKit that is applied to all HyperLinkToolTips. A blank
	 * StyleSheet is also created and only hyperlinks are styled.
	 */
	static {
		editorKit = new CustomHTMLEditorKit();
		StyleSheet styleSheet = new StyleSheet();
		styleSheet.addRule("a { color: #0000EE; text-decoration:underline; }");
		editorKit.setStyleSheet(styleSheet);
	}

	private JEditorPane editorPane;

	public HyperLinkToolTip() {
		setLayout(new BorderLayout());
		editorPane = new JEditorPane();
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		editorPane.setEditorKit(editorKit);
		editorPane.setEditable(false);
		editorPane.setForeground(fg);
		editorPane.setBackground(bg);

		editorPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(new URI(e.getDescription()));
							hideToolTip();
						} catch (IOException | URISyntaxException e1) {
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

	@Override
	public void setTipText(String tipText) {
		editorPane.setText(tipText);
	}

	@Override
	public Dimension getPreferredSize() {
	    if (getLayout() != null) {
	        return getLayout().preferredLayoutSize(this);
	    }
	    return super.getPreferredSize();
	}

	private void hideToolTip() {
		setVisible(false);
	}
}
