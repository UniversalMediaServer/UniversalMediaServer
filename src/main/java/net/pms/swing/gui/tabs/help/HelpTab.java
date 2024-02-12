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
package net.pms.swing.gui.tabs.help;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import net.pms.platform.PlatformUtils;
import net.pms.swing.gui.UmsFormBuilder;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up the panel for the help tab and loads its contents from a file.
 */
public class HelpTab {

	private static final Logger LOGGER = LoggerFactory.getLogger(HelpTab.class);

	/**
	 * List of context sensitive help pages URLs. These URLs should be relative
	 * to the documentation directory and in the same order as the tabs. The
	 * value <code>null</code> means "don't care", activating the tab will not
	 * change the help page.
	 */
	private static final String[] HELP_PAGES = {
		"index.html",
		null,
		"general_configuration.html",
		null,
		"navigation_share.html",
		"transcoding.html",
		null,
		null
	};

	/**
	 * Relative location of a context sensitive help page in the documentation
	 * directory.
	 */
	private String helpPage = "index.html";
	private JEditorPane editorPane;

	/**
	 * Return the editor pane for the help tab containing the help contents.
	 * @return The editor pane for the help tab
	 */
	public JEditorPane getList() {
		return editorPane;
	}

	/**
	 * Set up the panel for the help tab and load its contents from a file.
	 * @return The component containing the help tab and its contents
	 */
	public JComponent build() {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"pref, fill:default:grow"
		);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();
		editorPane = new JEditorPane();
		editorPane.setEditable(false);
		editorPane.setContentType("text/html");
		editorPane.setBackground(Color.WHITE);
		HTMLEditorKit editorKit = new HTMLEditorKit();
		StyleSheet styleSheet = ((HTMLDocument) editorKit.createDefaultDocument()).getStyleSheet();
		buildStyleSheet(styleSheet);
		editorKit.setStyleSheet(styleSheet);
		editorPane.setEditorKit(editorKit);

		updateContents();

		// Enable internal anchor links
		editorPane.addHyperlinkListener((HyperlinkEvent event) -> {
			try {
				if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					String urlString = event.getURL().toExternalForm();
					if (urlString.startsWith("http://") || urlString.startsWith("https://") || urlString.startsWith("ftp://")) {
						// Open external links in the desktop web browser
						PlatformUtils.INSTANCE.browseURI(urlString);
					} else {
						// Open anchor links in the editorPane
						editorPane.setPage(event.getURL());
					}
				}
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}
		});

		JScrollPane pane = new JScrollPane(editorPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setPreferredSize(new Dimension(500, 400));
		pane.setBorder(BorderFactory.createEmptyBorder());
		builder.add(pane).at(cc.xy(2, 2));

		return builder.getPanel();
	}

	/**
	 * Load the current help page in the editor pane.
	 */
	public void updateContents() {
		if (editorPane != null) {
			File documentationDir = new File(PropertiesUtil.getProjectProperties().get("project.documentation.dir"));
			if (!documentationDir.exists()) {
				// Try to load help files from the source tree if not found to make it work while running from an IDE
				File sourceDocumentationDir = new File("src/main/external-resources/documentation");
				if (sourceDocumentationDir.exists()) {
					documentationDir = sourceDocumentationDir;
				}
			}
			File helpFile = new File(documentationDir, helpPage);
			if (helpFile.exists()) {
				try {
					// Display the HTML help file in the editor
					editorPane.setPage(helpFile.toURI().toURL());
				} catch (IOException e) {
					LOGGER.debug("Exception while trying to display help file: ", e);
				}
			} else {
				LOGGER.info("Couldn't find help file \"{}\". Help will not be available.", helpFile.getAbsolutePath());
			}
		}
	}

	/**
	 * This sets all sizes that should be relative to the font size in the HTML
	 * document. This is to respect the OS font size setting used for example
	 * on high DPI monitors.
	 *
	 * @param styleSheet the <code>StyleSheet</code> to modify
	 */
	public void buildStyleSheet(StyleSheet styleSheet) {
		int baseSize = editorPane.getFont().getSize();
		String rule = String.format(
			"body { font-size: %dpt; padding: %dpx; }",
			Math.round((double) baseSize * 7 / 6),
			Math.round((double) baseSize * 5 / 6)
		);
		styleSheet.addRule(rule);

		rule = String.format("h1 { font-size: %dpx; }", baseSize * 2);
		styleSheet.addRule(rule);

		rule = String.format("h2 { font-size: %dpx; }", Math.round(baseSize * 1.5));
		styleSheet.addRule(rule);

		rule = String.format("h3 { font-size: %dpx; }", Math.round(baseSize * 1.17));
		styleSheet.addRule(rule);

		rule = String.format("pre, tt { font-size: %dpt; }", baseSize);
		styleSheet.addRule(rule);

		rule = String.format("dd { margin-bottom: %dpx; }", Math.round((double) baseSize * 10 / 6));
		styleSheet.addRule(rule);
	}

	/**
	 * Sets the relative URL of a context sensitive help page located in the
	 * documentation directory.
	 *
	 * @param page The help page.
	 */
	public void setTabIndex(int selectedIndex) {
		if (selectedIndex != -1 && HELP_PAGES.length > selectedIndex && HELP_PAGES[selectedIndex] != null) {
			helpPage = HELP_PAGES[selectedIndex];
			// Update the contents of the help tab itself
			updateContents();
		}
	}

}
