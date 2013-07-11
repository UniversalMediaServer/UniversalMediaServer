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
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.util.Locale;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.util.FormLayoutUtil;
import net.pms.util.KeyedComboBoxModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationShareTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(NavigationShareTab.class);
	public static final String ALL_DRIVES = Messages.getString("FoldTab.0");

	private static final String PANEL_COL_SPEC = "left:pref, 9dlu, pref, 3dlu, 25dlu, 9dlu, default:grow";
	private static final String PANEL_ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, fill:default:grow";
	private static final String SHARED_FOLDER_COL_SPEC = "left:pref, left:pref, pref, pref, pref, 0:grow";
	private static final String SHARED_FOLDER_ROW_SPEC = "p, 3dlu, p, 3dlu, fill:default:grow";

	private JList FList;
	private DefaultListModel df;
	private JCheckBox hidevideosettings;
	private JCheckBox hidetranscode;
	private JCheckBox hidemedialibraryfolder;
	private JCheckBox hideextensions;
	private JCheckBox hideemptyfolders;
	private JCheckBox hideengines;
	private CustomJButton but5;
	private JTextField seekpos;
	private JCheckBox thumbgenCheckBox;
	private JCheckBox mplayer_thumb;
	private JCheckBox dvdiso_thumb;
	private JCheckBox image_thumb;
	private JCheckBox cacheenable;
	private JCheckBox archive;
	private JComboBox sortmethod;
	private JComboBox audiothumbnail;
	private JTextField defaultThumbFolder;
	private JCheckBox iphoto;
	private JCheckBox aperture;
	public static JCheckBox itunes;
	private CustomJButton select;
	private CustomJButton cachereset;
	private JCheckBox ignorethewordthe;
	private JTextField atzLimit;
	private JCheckBox liveSubtitles;
	private JCheckBox prettifyfilenames;
	private JCheckBox resume;

	public DefaultListModel getDf() {
		return df;
	}

	private final PmsConfiguration configuration;

	NavigationShareTab(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	private void updateModel() {
		if (df.size() == 1 && df.getElementAt(0).equals(ALL_DRIVES)) {
			configuration.setFolders("");
		} else {
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < df.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}

				String entry = (String) df.getElementAt(i);

				// escape embedded commas. note: backslashing isn't safe as it conflicts with
				// Windows path separators:
				// http://ps3mediaserver.org/forum/viewtopic.php?f=14&t=8883&start=250#p43520
				sb.append(entry.replace(",", "&comma;"));
			}

			configuration.setFolders(sb.toString());
		}
	}

	public JComponent build() {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(PANEL_COL_SPEC, orientation);

		// Set basic layout
		FormLayout layout = new FormLayout(colSpec, PANEL_ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.DLU4_BORDER);
		builder.setOpaque(true);

		CellConstraints cc = new CellConstraints();

		// Init all gui components
		initSimpleComponents(cc);
		PanelBuilder builderSharedFolder = initSharedFoldersGuiComponents(cc);

		// Build gui with initialized components
		if (!configuration.isHideAdvancedOptions()) {
			JComponent cmp = builder.addSeparator(Messages.getString("FoldTab.13"), FormLayoutUtil.flip(cc.xyw(1, 1, 7), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.add(thumbgenCheckBox, FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));
			builder.addLabel(Messages.getString("NetworkTab.16"), FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));
			builder.add(seekpos, FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));
			builder.add(image_thumb, FormLayoutUtil.flip(cc.xy(7, 3), colSpec, orientation));

			builder.addLabel(Messages.getString("FoldTab.26"), FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));
			builder.add(audiothumbnail, FormLayoutUtil.flip(cc.xyw(3, 5, 3), colSpec, orientation));
			builder.add(mplayer_thumb, FormLayoutUtil.flip(cc.xy(7, 5), colSpec, orientation));

			builder.addLabel(Messages.getString("FoldTab.27"), FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));
			builder.add(defaultThumbFolder, FormLayoutUtil.flip(cc.xyw(3, 7, 2), colSpec, orientation));
			builder.add(select, FormLayoutUtil.flip(cc.xy(5, 7), colSpec, orientation));
			builder.add(dvdiso_thumb, FormLayoutUtil.flip(cc.xy(7, 7), colSpec, orientation));

			cmp = builder.addSeparator(Messages.getString("NetworkTab.59"), FormLayoutUtil.flip(cc.xyw(1, 9, 7), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.addLabel(Messages.getString("FoldTab.18"), FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));
			builder.add(sortmethod, FormLayoutUtil.flip(cc.xyw(3, 11, 3), colSpec, orientation));
			builder.add(ignorethewordthe, FormLayoutUtil.flip(cc.xy(7, 11), colSpec, orientation));

			builder.add(prettifyfilenames, FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));
			builder.add(hideengines, FormLayoutUtil.flip(cc.xy(3, 13), colSpec, orientation));
			builder.add(hideextensions, FormLayoutUtil.flip(cc.xy(7, 13), colSpec, orientation));

			cmp = builder.addSeparator(Messages.getString("NetworkTab.60"), FormLayoutUtil.flip(cc.xyw(1, 15, 7), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.add(itunes, FormLayoutUtil.flip(cc.xy(1, 17), colSpec, orientation));
			builder.add(iphoto, FormLayoutUtil.flip(cc.xy(3, 17), colSpec, orientation));
			builder.add(aperture, FormLayoutUtil.flip(cc.xy(7, 17), colSpec, orientation));

			builder.add(cacheenable, FormLayoutUtil.flip(cc.xy(1, 19), colSpec, orientation));
			builder.add(cachereset, FormLayoutUtil.flip(cc.xyw(3, 19, 3), colSpec, orientation));
			builder.add(hidemedialibraryfolder, FormLayoutUtil.flip(cc.xy(7, 19), colSpec, orientation));

			builder.add(hideemptyfolders, FormLayoutUtil.flip(cc.xy(1, 21), colSpec, orientation));
			builder.add(hidevideosettings, FormLayoutUtil.flip(cc.xy(3, 21), colSpec, orientation));
			builder.add(hidetranscode, FormLayoutUtil.flip(cc.xy(7, 21), colSpec, orientation));

			builder.add(liveSubtitles, FormLayoutUtil.flip(cc.xy(1, 23), colSpec, orientation));
			builder.addLabel(Messages.getString("FoldTab.37"), FormLayoutUtil.flip(cc.xy(3, 23), colSpec, orientation));
			builder.add(atzLimit, FormLayoutUtil.flip(cc.xy(5, 23), colSpec, orientation));
			builder.add(archive, FormLayoutUtil.flip(cc.xy(7, 23), colSpec, orientation));

			builder.add(resume, FormLayoutUtil.flip(cc.xy(1, 25), colSpec, orientation));

			builder.add(builderSharedFolder.getPanel(), FormLayoutUtil.flip(cc.xyw(1, 27, 7), colSpec, orientation));
		} else {
			builder.add(builderSharedFolder.getPanel(), FormLayoutUtil.flip(cc.xyw(1, 1, 7), colSpec, orientation));
		}

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);

		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	private void initSimpleComponents(CellConstraints cc) {
		// Thumbnail seeking position
		seekpos = new JTextField("" + configuration.getThumbnailSeekPos());
		seekpos.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(seekpos.getText());
					configuration.setThumbnailSeekPos(ab);
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse thumbnail seek position from \"" + seekpos.getText() + "\"");
				}

			}
		});
		if (configuration.isThumbnailGenerationEnabled()) {
			seekpos.setEnabled(true);
		} else {
			seekpos.setEnabled(false);
		}

		// Generate thumbnails
		thumbgenCheckBox = new JCheckBox(Messages.getString("NetworkTab.2"));
		thumbgenCheckBox.setContentAreaFilled(false);
		thumbgenCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setThumbnailGenerationEnabled((e.getStateChange() == ItemEvent.SELECTED));
				seekpos.setEnabled(configuration.isThumbnailGenerationEnabled());
			}
		});
		if (configuration.isThumbnailGenerationEnabled()) {
			thumbgenCheckBox.setSelected(true);
		}

		// Use MPlayer for video thumbnails
		mplayer_thumb = new JCheckBox(Messages.getString("FoldTab.14"));
		mplayer_thumb.setContentAreaFilled(false);
		mplayer_thumb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setUseMplayerForVideoThumbs((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		if (configuration.isUseMplayerForVideoThumbs()) {
			mplayer_thumb.setSelected(true);
		}

		// DVD ISO thumbnails
		dvdiso_thumb = new JCheckBox(Messages.getString("FoldTab.19"));
		dvdiso_thumb.setContentAreaFilled(false);
		dvdiso_thumb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setDvdIsoThumbnails((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		if (configuration.isDvdIsoThumbnails()) {
			dvdiso_thumb.setSelected(true);
		}

		// Image thumbnails
		image_thumb = new JCheckBox(Messages.getString("FoldTab.21"));
		image_thumb.setContentAreaFilled(false);
		image_thumb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setImageThumbnailsEnabled((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		if (configuration.getImageThumbnailsEnabled()) {
			image_thumb.setSelected(true);
		}

		// Audio thumbnails import
		final KeyedComboBoxModel thumbKCBM = new KeyedComboBoxModel(new Object[]{"0", "1", "2"}, new Object[]{Messages.getString("FoldTab.35"), Messages.getString("FoldTab.23"), Messages.getString("FoldTab.24")});
		audiothumbnail = new JComboBox(thumbKCBM);
		audiothumbnail.setEditable(false);

		thumbKCBM.setSelectedKey("" + configuration.getAudioThumbnailMethod());

		audiothumbnail.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {

					try {
						configuration.setAudioThumbnailMethod(Integer.parseInt((String) thumbKCBM.getSelectedKey()));
					} catch (NumberFormatException nfe) {
						LOGGER.debug("Could not parse audio thumbnail method from \"" + thumbKCBM.getSelectedKey() + "\"");
					}

				}
			}
		});

		// Alternate video cover art folder
		defaultThumbFolder = new JTextField(configuration.getAlternateThumbFolder());
		defaultThumbFolder.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAlternateThumbFolder(defaultThumbFolder.getText());
			}
		});

		// Alternate video cover art folder button
		select = new CustomJButton("...");
		select.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser;
				try {
					chooser = new JFileChooser();
				} catch (Exception ee) {
					chooser = new JFileChooser(new RestrictedFileSystemView());
				}
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getString("FoldTab.28"));
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					defaultThumbFolder.setText(chooser.getSelectedFile().getAbsolutePath());
					configuration.setAlternateThumbFolder(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});

		// Hide Server Settings folder
		hidevideosettings = new JCheckBox(Messages.getString("FoldTab.38"));
		hidevideosettings.setContentAreaFilled(false);
		if (configuration.getHideVideoSettings()) {
			hidevideosettings.setSelected(true);
		}
		hidevideosettings.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideVideoSettings((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Hide #--TRANSCODE--# folder
		hidetranscode = new JCheckBox(Messages.getString("FoldTab.33"));
		hidetranscode.setContentAreaFilled(false);
		if (configuration.getHideTranscodeEnabled()) {
			hidetranscode.setSelected(true);
		}
		hidetranscode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideTranscodeEnabled((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Hide cache folder
		hidemedialibraryfolder = new JCheckBox(Messages.getString("FoldTab.32"));
		hidemedialibraryfolder.setContentAreaFilled(false);
		if (configuration.isHideMediaLibraryFolder()) {
			hidemedialibraryfolder.setSelected(true);
		}
		hidemedialibraryfolder.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideMediaLibraryFolder((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Browse compressed archives
		archive = new JCheckBox(Messages.getString("NetworkTab.1"));
		archive.setContentAreaFilled(false);
		archive.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setArchiveBrowsing(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		if (configuration.isArchiveBrowsing()) {
			archive.setSelected(true);
		}

		// Enable the cache
		cacheenable = new JCheckBox(Messages.getString("NetworkTab.17"));
		cacheenable.setToolTipText(Messages.getString("FoldTab.48"));
		cacheenable.setContentAreaFilled(false);
		cacheenable.setSelected(configuration.getUseCache());
		cacheenable.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setUseCache((e.getStateChange() == ItemEvent.SELECTED));
				cachereset.setEnabled(configuration.getUseCache());
				if ((LooksFrame) PMS.get().getFrame() != null) {
					((LooksFrame) PMS.get().getFrame()).getNt().setScanLibraryEnabled(configuration.getUseCache());
				}
			}
		});

		// Reset cache
		cachereset = new CustomJButton(Messages.getString("NetworkTab.18"));
		cachereset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int option = JOptionPane.showConfirmDialog(
					(Component) PMS.get().getFrame(),
					Messages.getString("NetworkTab.13") + Messages.getString("NetworkTab.19"),
					Messages.getString("Dialog.Question"),
					JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION) {
					PMS.get().getDatabase().init(true);
				}

			}
		});
		cachereset.setEnabled(configuration.getUseCache());

		// Hide file extensions
		hideextensions = new JCheckBox(Messages.getString("FoldTab.5"));
		hideextensions.setContentAreaFilled(false);
		if (configuration.isHideExtensions()) {
			hideextensions.setSelected(true);
		}
		if (configuration.isPrettifyFilenames()) {
			hideextensions.setEnabled(false);
		}
		hideextensions.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideExtensions((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Hide transcoding engine names
		hideengines = new JCheckBox(Messages.getString("FoldTab.8"));
		hideengines.setToolTipText(Messages.getString("FoldTab.46"));
		hideengines.setContentAreaFilled(false);
		if (configuration.isHideEngineNames()) {
			hideengines.setSelected(true);
		}
		hideengines.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideEngineNames((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Hide empty folders
		hideemptyfolders = new JCheckBox(Messages.getString("FoldTab.31"));
		hideemptyfolders.setContentAreaFilled(false);
		if (configuration.isHideEmptyFolders()) {
			hideemptyfolders.setSelected(true);
		}
		hideemptyfolders.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideEmptyFolders((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Show iTunes library
		itunes = new JCheckBox(Messages.getString("FoldTab.30"));
		itunes.setToolTipText(Messages.getString("FoldTab.47"));
		itunes.setContentAreaFilled(false);
		if (configuration.isShowItunesLibrary()) {
			itunes.setSelected(true);
		}
		if (!(Platform.isMac() || Platform.isWindows())) {
			itunes.setEnabled(false);
		}
		itunes.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setShowItunesLibrary((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Show iPhoto library
		iphoto = new JCheckBox(Messages.getString("FoldTab.29"));
		iphoto.setContentAreaFilled(false);
		if (configuration.isShowIphotoLibrary()) {
			iphoto.setSelected(true);
		}
		if (!Platform.isMac()) {
			iphoto.setEnabled(false);
		}
		iphoto.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setShowIphotoLibrary((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Show aperture library
		aperture = new JCheckBox(Messages.getString("FoldTab.34"));
		aperture.setContentAreaFilled(false);
		if (configuration.isShowApertureLibrary()) {
			aperture.setSelected(true);
		}
		if (!Platform.isMac()) {
			aperture.setEnabled(false);
		}
		aperture.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setShowApertureLibrary((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// File order
		final KeyedComboBoxModel kcbm = new KeyedComboBoxModel(
			new Object[]{
				"0", // alphabetical
				"4", // natural sort
				"3", // ASCIIbetical
				"1", // newest first
				"2"  // oldest first
			},
			new Object[]{
				Messages.getString("FoldTab.15"),
				Messages.getString("FoldTab.22"),
				Messages.getString("FoldTab.20"),
				Messages.getString("FoldTab.16"),
				Messages.getString("FoldTab.17")
			}
		);
		sortmethod = new JComboBox(kcbm);
		sortmethod.setEditable(false);
		kcbm.setSelectedKey("" + configuration.getSortMethod());

		sortmethod.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {

					try {
						configuration.setSortMethod(Integer.parseInt((String) kcbm.getSelectedKey()));
					} catch (NumberFormatException nfe) {
						LOGGER.debug("Could not parse sort method from \"" + kcbm.getSelectedKey() + "\"");
					}

				}
			}
		});

		// Ignore the word "the" while sorting
		ignorethewordthe = new JCheckBox(Messages.getString("FoldTab.39"));
		ignorethewordthe.setToolTipText(Messages.getString("FoldTab.44"));
		ignorethewordthe.setContentAreaFilled(false);
		if (configuration.isIgnoreTheWordThe()) {
			ignorethewordthe.setSelected(true);
		}
		ignorethewordthe.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setIgnoreTheWordThe((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		atzLimit = new JTextField("" + configuration.getATZLimit());
		atzLimit.setToolTipText(Messages.getString("FoldTab.49"));
		atzLimit.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(atzLimit.getText());
					configuration.setATZLimit(ab);
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse ATZ limit from \"" + atzLimit.getText() + "\"");
					LOGGER.debug("The full error was: " + nfe);
				}
			}
		});

		liveSubtitles = new JCheckBox(Messages.getString("FoldTab.42"));
		liveSubtitles.setContentAreaFilled(false);
		if (configuration.isHideLiveSubtitlesFolder()) {
			liveSubtitles.setSelected(true);
		}
		liveSubtitles.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideLiveSubtitlesFolder((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		prettifyfilenames = new JCheckBox(Messages.getString("FoldTab.43"));
		prettifyfilenames.setToolTipText(Messages.getString("FoldTab.45"));
		prettifyfilenames.setContentAreaFilled(false);
		if (configuration.isPrettifyFilenames()) {
			prettifyfilenames.setSelected(true);
		}
		prettifyfilenames.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setPrettifyFilenames((e.getStateChange() == ItemEvent.SELECTED));
				hideextensions.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
			}
		});

		resume = new JCheckBox(Messages.getString("NetworkTab.68"));
		resume.setToolTipText(Messages.getString("NetworkTab.69"));
		resume.setContentAreaFilled(false);
		if (configuration.isResumeEnabled()) {
			resume.setSelected(true);
		}
		resume.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setResume((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
	}

	private PanelBuilder initSharedFoldersGuiComponents(CellConstraints cc) {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(SHARED_FOLDER_COL_SPEC, orientation);

		FormLayout layoutFolders = new FormLayout(colSpec, SHARED_FOLDER_ROW_SPEC);
		PanelBuilder builderFolder = new PanelBuilder(layoutFolders);
		builderFolder.setOpaque(true);

		JComponent cmp = builderFolder.addSeparator(Messages.getString("FoldTab.7"), FormLayoutUtil.flip(cc.xyw(1, 1, 6), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		CustomJButton but = new CustomJButton(LooksFrame.readImageIcon("button-adddirectory.png"));
		but.setToolTipText(Messages.getString("FoldTab.9"));
		but.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				JFileChooser chooser;
				try {
					chooser = new JFileChooser();
				} catch (Exception ee) {
					chooser = new JFileChooser(new RestrictedFileSystemView());
				}
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog((Component) e.getSource());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					((DefaultListModel) FList.getModel()).add(FList.getModel().getSize(), chooser.getSelectedFile().getAbsolutePath());
					if (FList.getModel().getElementAt(0).equals(ALL_DRIVES)) {
						((DefaultListModel) FList.getModel()).remove(0);
					}
					updateModel();
				}
			}
		});
		builderFolder.add(but, FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		CustomJButton but2 = new CustomJButton(LooksFrame.readImageIcon("button-remove.png"));
		but2.setToolTipText(Messages.getString("FoldTab.36"));
		//but2.setBorder(BorderFactory.createEtchedBorder());
		but2.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if (FList.getSelectedIndex() > -1) {
					((DefaultListModel) FList.getModel()).remove(FList.getSelectedIndex());
					if (FList.getModel().getSize() == 0) {
						((DefaultListModel) FList.getModel()).add(0, ALL_DRIVES);
					}
					updateModel();
				}
			}
		});
		builderFolder.add(but2, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		CustomJButton but3 = new CustomJButton(LooksFrame.readImageIcon("button-arrow-down.png"));
		but3.setToolTipText(Messages.getString("FoldTab.12"));
		// but3.setBorder(BorderFactory.createEmptyBorder());
		but3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultListModel model = ((DefaultListModel) FList.getModel());
				for (int i = 0; i < model.size() - 1; i++) {
					if (FList.isSelectedIndex(i)) {
						String value = model.get(i).toString();
						model.set(i, model.get(i + 1));
						model.set(i + 1, value);
						FList.setSelectedIndex(i + 1);
						updateModel();
						break;
					}
				}
			}
		});

		builderFolder.add(but3, FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));
		CustomJButton but4 = new CustomJButton(LooksFrame.readImageIcon("button-arrow-up.png"));
		but4.setToolTipText(Messages.getString("FoldTab.12"));
		//  but4.setBorder(BorderFactory.createEmptyBorder());
		but4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultListModel model = ((DefaultListModel) FList.getModel());
				for (int i = 1; i < model.size(); i++) {
					if (FList.isSelectedIndex(i)) {
						String value = model.get(i).toString();

						model.set(i, model.get(i - 1));
						model.set(i - 1, value);
						FList.setSelectedIndex(i - 1);
						updateModel();
						break;

					}
				}
			}
		});
		builderFolder.add(but4, FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

		but5 = new CustomJButton(LooksFrame.readImageIcon("button-scan.png"));
		but5.setToolTipText(Messages.getString("FoldTab.2"));
		//but5.setBorder(BorderFactory.createEmptyBorder());
		but5.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (configuration.getUseCache()) {
					DLNAMediaDatabase database = PMS.get().getDatabase();

					if (database != null) {
						if (!database.isScanLibraryRunning()) {
							int option = JOptionPane.showConfirmDialog(
								(Component) PMS.get().getFrame(),
								Messages.getString("FoldTab.3") + Messages.getString("FoldTab.4"),
								Messages.getString("Dialog.Question"),
								JOptionPane.YES_NO_OPTION);
							if (option == JOptionPane.YES_OPTION) {
								database.scanLibrary();
								but5.setIcon(LooksFrame.readImageIcon("button-scan-busy.gif"));
								but5.setRolloverIcon(LooksFrame.readImageIcon("button-scan-cancel.png"));
								but5.setToolTipText(Messages.getString("FoldTab.40"));
							}
						} else {
							int option = JOptionPane.showConfirmDialog(
								(Component) PMS.get().getFrame(),
								Messages.getString("FoldTab.10"),
								Messages.getString("Dialog.Question"),
								JOptionPane.YES_NO_OPTION);
							if (option == JOptionPane.YES_OPTION) {
								database.stopScanLibrary();
								PMS.get().getFrame().setStatusLine(null);
								if ((LooksFrame) PMS.get().getFrame() != null) {
									((LooksFrame) PMS.get().getFrame()).getNt().setScanLibraryEnabled(false);
								}
								but5.setToolTipText(Messages.getString("FoldTab.41"));
							}
						}
					}
				}
			}
		});

		builderFolder.add(but5, FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));
		but5.setEnabled(configuration.getUseCache());

		df = new DefaultListModel();
		File[] folders = PMS.get().getFoldersConf();
		if (folders != null && folders.length > 0) {
			for (File file : folders) {
				df.addElement(file.getAbsolutePath());
			}
		} else {
			df.addElement(ALL_DRIVES);
		}
		FList = new JList();
		FList.setModel(df);
		JScrollPane pane = new JScrollPane(FList);
		builderFolder.add(pane, FormLayoutUtil.flip(cc.xyw(1, 5, 6), colSpec, orientation));

		return builderFolder;
	}

	public void setScanLibraryEnabled(boolean enabled) {
		but5.setEnabled(enabled);
		but5.setIcon(LooksFrame.readImageIcon("button-scan.png"));
		but5.setRolloverIcon(LooksFrame.readImageIcon("button-scan.png"));
		but5.setToolTipText(Messages.getString("FoldTab.2"));
	}
}
