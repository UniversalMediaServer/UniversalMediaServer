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
import net.pms.newgui.GuiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Based on https://community.oracle.com/thread/1363271
public class HyperLinkToolTip extends JToolTip {

	private static final long serialVersionUID = -8107203112982951774L;
	private static final Logger LOGGER = LoggerFactory.getLogger(HyperLinkToolTip.class);
	private static ColorUIResource fg = new ColorUIResource(PMS.getConfiguration().getToolTipForegroundColor());
	private static ColorUIResource bg = new ColorUIResource(PMS.getConfiguration().getToolTipBackgroundColor());

	private JEditorPane editorPane;
	private boolean ltr;

	public HyperLinkToolTip() {
		this(PMS.isLeftToRightLocale());
	}

	public HyperLinkToolTip(boolean ltr) {
		this.ltr = ltr;
		setLayout(new BorderLayout());
		editorPane = new JEditorPane();
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		editorPane.setContentType("text/html");
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
							closeToolTip();
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
		editorPane.setText(GuiUtil.htmlify(tipText, ltr));
	}

	@Override
	public void updateUI() {
		setUI(new ToolTipUI() { });
	}

	private void closeToolTip() {
		setVisible(false);
	}
}
