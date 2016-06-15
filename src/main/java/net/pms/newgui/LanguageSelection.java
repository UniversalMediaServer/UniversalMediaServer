/*
 * Universal Media Server, for streaming any medias to DLNA
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
package net.pms.newgui;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.StyleSheet;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.newgui.components.CustomHTMLEditorKit;
import net.pms.util.KeyedComboBoxModel;
import net.pms.util.Languages;
import net.pms.util.ProcessUtil;
import net.pms.util.StringUtil;
import net.pms.util.SwingUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates and handles the language selection dialog.
 *
 * @author Nadahar
 * @since 5.4.0
 */

public class LanguageSelection {
	private static final Logger LOGGER = LoggerFactory.getLogger(LanguageSelection.class);

	private JComboBox<String> jLanguage;
	private JOptionPane pane;
	private JPanel rootPanel = new JPanel();
	private JPanel selectionPanel = new JPanel();
	private JPanel languagePanel = new JPanel();
	private JButton selectButton = new JButton();
	private JButton applyButton = new JButton();
	private TitledBorder selectionPanelBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	private TitledBorder infoTextBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	private JTextArea descriptionText = new JTextArea();
	private JTextArea warningText = new JTextArea();
	private JEditorPane infoText = new JEditorPane();
	private final Component parentComponent;
	private KeyedComboBoxModel<String, String> keyedModel = new KeyedComboBoxModel<>();
	private Locale locale;
	private int textWidth;
	private JDialog dialog;
	private boolean aborted = false;
	private boolean rebootOnChange;

	public LanguageSelection(Component parentComponent, Locale initialLocale, boolean rebootOnChange) {
		this.parentComponent = parentComponent;
		if (initialLocale == null) {
			throw new IllegalArgumentException("initialLocale cannot be null");
		}
		this.locale = initialLocale;
		this.rebootOnChange = rebootOnChange;
	}

	public void show() {
		if (PMS.isHeadless()) {
			// Can only get here during startup in headless mode, there's no way to trigger it from the GUI
			LOGGER.info("No language is configured and the language selection dialog is unavailable in headless mode");
			LOGGER.info("Defaulting to OS locale {}", Locale.getDefault().getDisplayName());
			PMS.setLocale(Locale.getDefault());
		} else {
			pane = new JOptionPane(
				buildComponent(),
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.NO_OPTION,
				null,
				new JButton[]{applyButton, selectButton},
				selectButton
			);
			pane.setComponentOrientation(ComponentOrientation.getOrientation(locale));
			dialog = pane.createDialog(parentComponent, PMS.NAME);
			dialog.setModalityType(ModalityType.APPLICATION_MODAL);
			dialog.setIconImage(LooksFrame.readImageIcon("icon-32.png").getImage());
			setStrings();
			dialog.pack();
			dialog.setLocationRelativeTo(parentComponent);
			dialog.setVisible(true);
			dialog.dispose();

			if (pane.getValue() == null) {
				aborted = true;
			} else if (!((String) pane.getValue()).equals(PMS.getConfiguration().getLanguageRawString())) {
				if (rebootOnChange) {
					int response = JOptionPane.showConfirmDialog(
						parentComponent,
						String.format(buildString("Dialog.Restart", true), PMS.NAME, PMS.NAME),
						buildString("Dialog.Confirm"),
						JOptionPane.YES_NO_CANCEL_OPTION
					);
					if (response != JOptionPane.CANCEL_OPTION) {
						PMS.getConfiguration().setLanguage((String) pane.getValue());
						if (response == JOptionPane.YES_OPTION) {
							try {
								PMS.getConfiguration().save();
							} catch (ConfigurationException e) {
								LOGGER.error("Error while saving configuration: {}", e.getMessage());
								LOGGER.trace("", e);
							}
							ProcessUtil.reboot();
						}
					}
				} else {
					PMS.getConfiguration().setLanguage((String) pane.getValue());
				}
			}
		}
	}

	private class SelectButtonActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("select") && keyedModel.getSelectedKey() != null) {
				pane.setValue(keyedModel.getSelectedKey());
			}
		}
	}
	private class ApplyButtonActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("apply") && keyedModel.getSelectedKey() != null) {
				locale = Languages.toLocale(keyedModel.getSelectedKey());
				setStrings();
				dialog.pack();
				dialog.setLocationRelativeTo(parentComponent);
				dialog.repaint();
				applyButton.setEnabled(false);
				rootPanel.getRootPane().setDefaultButton(selectButton);
			}
		}
	}

	private class LanguageComboBoxActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("comboBoxChanged") && keyedModel.getSelectedKey() != null) {
				if (Languages.toLocale(keyedModel.getSelectedKey()).equals(locale)) {
					applyButton.setEnabled(false);
					rootPanel.getRootPane().setDefaultButton(selectButton);
				} else {
					applyButton.setEnabled(true);
					rootPanel.getRootPane().setDefaultButton(applyButton);
				}
			}
		}
	}

	private String buildString(String key, boolean paragraph, boolean html) {
		String result = Messages.getString(key, locale);
		String rootString = Messages.getRootString(key);
		if (!result.equals(rootString)) {
			if (paragraph && html) {
				result += "<br><br>" + rootString;
			} else if (paragraph) {
				result += "\n\n" + rootString;
			} else {
				result += String.format(" (%s)", rootString);
			}
		}
		return (html ? "<html>" : "") + result + (html ? "</html>" : "");
	}

	private String buildString(String key, boolean paragraph) {
		return buildString(key, paragraph, false);
	}

	private String buildString(String key) {
		return buildString(key, false, false);
	}

	private void setStrings() {
		dialog.setLocale(locale);
		dialog.applyComponentOrientation(ComponentOrientation.getOrientation(locale));

		selectionPanelBorder.setTitle(buildString("LanguageSelection.1"));

		selectionPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(5, 5, 5, 5),
			BorderFactory.createCompoundBorder(
				selectionPanelBorder,
				BorderFactory.createEmptyBorder(10, 5, 10, 5)
			)
		));


		String descriptionMessage = parentComponent != null ? "LanguageSelection.7" : "LanguageSelection.2";
		if (Messages.getString(descriptionMessage, locale).equals(Messages.getRootString(descriptionMessage))) {
			if (parentComponent != null) {
				descriptionText.setText(String.format(
					Messages.getString(descriptionMessage, locale),
					PMS.NAME
				));
			} else {
				descriptionText.setText(String.format(
					Messages.getString(descriptionMessage, locale),
					PMS.NAME,
					Messages.getString("LooksFrame.20", locale)
				));
			}
		} else {
			if (parentComponent != null) {
				descriptionText.setText(String.format(
					buildString(descriptionMessage, true),
					PMS.NAME,
					PMS.NAME
				));
			} else {
				descriptionText.setText(String.format(
					buildString(descriptionMessage, true),
					PMS.NAME,
					Messages.getString("LooksFrame.20", locale),
					PMS.NAME,
					Messages.getRootString("LooksFrame.20")
				));
			}
		}
		// Set the width of the text panels by font size to accommodate font scaling
		float avgCharWidth = SwingUtils.getComponentAverageCharacterWidth(descriptionText);
		textWidth = Math.round(avgCharWidth * 100);
		selectButton.setMargin(new Insets(Math.round((float) 0.5 * avgCharWidth), Math.round(4 * avgCharWidth), Math.round((float) 0.5 * avgCharWidth), Math.round(4 * avgCharWidth)));
		applyButton.setMargin(new Insets(Math.round((float) 0.5 * avgCharWidth), Math.round(4 * avgCharWidth), Math.round((float) 0.5 * avgCharWidth), Math.round(4 * avgCharWidth)));

		descriptionText.setPreferredSize(SwingUtils.getWordWrappedTextDimension(descriptionText, textWidth));

		keyedModel.setData(Languages.getLanguageTags(locale), Languages.getLanguageNames(locale));
		//Try to find a matching locale
		String languageTag = Languages.toLanguageTag(locale);
		int idx;
		if (languageTag != null) {
			idx = keyedModel.findKeyIndex(languageTag);
		} else {
			idx = -1;
		}
		if (idx < 0) {
			// Trying to translate a close match to "our" definition
			Locale tmpLocale = Languages.toLocale(locale);
			if (tmpLocale == null) {
				// Trying to find a supported locale based only on language and country
				tmpLocale = Languages.toLocale(new Locale(locale.getLanguage(), locale.getCountry()));
				if (tmpLocale == null) {
					// Trying to find a supported locale based only on language
					tmpLocale = Languages.toLocale(new Locale(locale.getLanguage()));
					if (tmpLocale == null) {
						// Giving up, defaulting to US English
						tmpLocale = Languages.toLocale("en-US");
					}
				}
			}
			// We should be guaranteed to get a valid tag here
			languageTag = Languages.toLanguageTag(tmpLocale);
		}
		keyedModel.setSelectedKey(languageTag);

		if (keyedModel.getSelectedKey() != null && Languages.warnCoverage(keyedModel.getSelectedKey())) {
			String localizedLanguageName = Messages.getString("Language." + keyedModel.getSelectedKey(), locale);
			if (Messages.getString("LanguageSelection.3", locale).equals(Messages.getRootString("LanguageSelection.3"))) {
				warningText.setText(String.format(
					Messages.getString("LanguageSelection.3", locale),
					localizedLanguageName,
					Languages.getLanguageCoverage(keyedModel.getSelectedKey()),
					localizedLanguageName
				));
			} else {
				int coverage = Languages.getLanguageCoverage(keyedModel.getSelectedKey());
				String rootLanguageName = Messages.getRootString("Language." + keyedModel.getSelectedKey());
				warningText.setText(String.format(
					buildString("LanguageSelection.3", true),
					localizedLanguageName,
					coverage,
					localizedLanguageName,
					rootLanguageName,
					coverage,
					rootLanguageName
				));
			}
		} else {
			warningText.setText("");
		}
		warningText.setPreferredSize(SwingUtils.getWordWrappedTextDimension(warningText, textWidth));

		infoTextBorder.setTitle(buildString("LanguageSelection.4"));

		infoText.setText(String.format(buildString("LanguageSelection.5", true, true), PMS.CROWDIN_LINK, PMS.CROWDIN_LINK));
		infoText.setPreferredSize(SwingUtils.getWordWrappedTextDimension(infoText, textWidth, StringUtil.stripHTML(infoText.getText())));

		selectButton.setText(buildString("Dialog.Select"));
		applyButton.setText(buildString("Dialog.Apply"));
	}

	private JComponent buildComponent() {
		// UIManager manages to get the background color wrong for text
		// components on OS X, so we apply the color manually
		Color backgroundColor = UIManager.getColor("Panel.background");
		rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.PAGE_AXIS));

		// It needs to be something in the title text, or the size calculation for the border will be wrong.
		selectionPanelBorder.setTitle(" ");
		selectionPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(5, 5, 5, 5),
			BorderFactory.createCompoundBorder(
				selectionPanelBorder,
				BorderFactory.createEmptyBorder(10, 5, 10, 5)
			)
		));
		selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.PAGE_AXIS));
		descriptionText.setEditable(false);
		descriptionText.setBackground(backgroundColor);
		descriptionText.setFocusable(false);
		descriptionText.setLineWrap(true);
		descriptionText.setWrapStyleWord(true);
		descriptionText.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));
		selectionPanel.add(descriptionText);

		jLanguage = new JComboBox<>(keyedModel);
		jLanguage.setEditable(false);
		jLanguage.setPreferredSize(new Dimension(50, jLanguage.getPreferredSize().height));
		jLanguage.addActionListener(new LanguageComboBoxActionListener());
		languagePanel.setLayout(new BoxLayout(languagePanel, BoxLayout.PAGE_AXIS));
		languagePanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		languagePanel.add(jLanguage);
		selectionPanel.add(languagePanel);

		warningText.setEditable(false);
		warningText.setFocusable(false);
		warningText.setBackground(backgroundColor);
		warningText.setFont(warningText.getFont().deriveFont(Font.BOLD));
		warningText.setLineWrap(true);
		warningText.setWrapStyleWord(true);
		warningText.setBorder(BorderFactory.createEmptyBorder(5, 15, 0, 15));
		selectionPanel.add(warningText);

		// It needs to be something in the title text, or the size calculation for the border will be wrong.
		infoTextBorder.setTitle(" ");
		infoText.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(5, 5, 5, 5),
			BorderFactory.createCompoundBorder(
				infoTextBorder,
				BorderFactory.createEmptyBorder(15, 20, 20, 20)
			)
		));
		infoText.setEditable(false);
		infoText.setFocusable(false);
		infoText.setBackground(backgroundColor);
		infoText.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

		// This exercise is to avoid using the default shared StyleSheet with padding
		CustomHTMLEditorKit editorKit = new CustomHTMLEditorKit();
		StyleSheet styleSheet = new StyleSheet();
		styleSheet.addRule("a { color: #0000EE; text-decoration:underline; }");
		editorKit.setStyleSheet(styleSheet);
		infoText.setEditorKit(editorKit);
		infoText.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					boolean error = false;
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(new URI(e.getDescription()));
						} catch (IOException | URISyntaxException ex) {
							LOGGER.error("Language selection failed to open translation page hyperlink: ", ex.getMessage());
							LOGGER.trace("", ex);
							error = true;
						}
					} else {
						LOGGER.warn("Desktop is not supported, the clicked translation page link can't be opened");
						error = true;
					}
					if (error) {
						JOptionPane.showOptionDialog(
							dialog,
							String.format(buildString("LanguageSelection.6", true), PMS.CROWDIN_LINK),
							buildString("Dialog.Error"),
							JOptionPane.DEFAULT_OPTION,
							JOptionPane.ERROR_MESSAGE,
							null,
							null,
							null);
					}
				}
			}

		});

		rootPanel.add(selectionPanel);
		rootPanel.add(infoText);

		applyButton.addActionListener(new ApplyButtonActionListener());
		applyButton.setActionCommand("apply");

		selectButton.addActionListener(new SelectButtonActionListener());
		selectButton.setActionCommand("select");

		return rootPanel;

	}

	public boolean isAborted() {
		return aborted;
	}
}
