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

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.sun.jna.Platform;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.*;
import java.awt.Font;
import java.io.File;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.Messages;
import net.pms.newgui.components.CustomJButton;
import net.pms.newgui.components.CustomJCheckBox;
import net.pms.newgui.components.OrientedPanelBuilder;
import net.pms.PMS;
import net.pms.util.KeyedComboBoxModel;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationShareTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(NavigationShareTab.class);
	public static final String ALL_DRIVES = Messages.getString("FoldTab.0");

	private static final String PANEL_COL_SPEC = "left:pref, 50dlu, pref, 150dlu, pref, 25dlu, pref, 9dlu, pref, default:grow";
	private static final String PANEL_ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, fill:default:grow";
	private static final String SHARED_FOLDER_COL_SPEC = "left:pref, left:pref, pref, pref, pref, pref, 0:grow";
	private static final String SHARED_FOLDER_ROW_SPEC = "p, 3dlu, p, 3dlu, fill:default:grow";

	private JTable FList;
	private SharedFoldersTableModel folderTableModel;
	private CustomJCheckBox hidevideosettings;
	private CustomJCheckBox hidetranscode;
	private CustomJCheckBox hidemedialibraryfolder;
	private CustomJCheckBox hideextensions;
	private CustomJCheckBox hideemptyfolders;
	private CustomJCheckBox hideengines;
	private CustomJButton but5;
	private JTextField seekpos;
	private CustomJCheckBox thumbgenCheckBox;
	private CustomJCheckBox mplayer_thumb;
	private CustomJCheckBox dvdiso_thumb;
	private CustomJCheckBox image_thumb;
	private CustomJCheckBox cacheenable;
	private CustomJCheckBox archive;
	private JComboBox sortmethod;
	private JComboBox audiothumbnail;
	private JTextField defaultThumbFolder;
	private CustomJCheckBox iphoto;
	private CustomJCheckBox aperture;
	public static CustomJCheckBox itunes;
	private CustomJButton select;
	private CustomJButton cachereset;
	private CustomJCheckBox ignorethewordthe;
	private JTextField atzLimit;
	private CustomJCheckBox liveSubtitles;
	private CustomJCheckBox prettifyfilenames;
	private CustomJCheckBox episodeTitles;
	private CustomJCheckBox newmediafolder;
	private CustomJCheckBox recentlyplayedfolder;
	private CustomJCheckBox resume;

	public SharedFoldersTableModel getDf() {
		return folderTableModel;
	}

	private final PmsConfiguration configuration;
	private LooksFrame looksFrame;

	NavigationShareTab(PmsConfiguration configuration, LooksFrame looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
	}

	private void updateModel() {
		if (folderTableModel.getRowCount() == 1 && folderTableModel.getValueAt(0, 0).equals(ALL_DRIVES)) {
			configuration.setFolders("");
		} else {
			StringBuilder folders = new StringBuilder();
			StringBuilder foldersMonitored = new StringBuilder();

			int i2 = 0;
			for (int i = 0; i < folderTableModel.getRowCount(); i++) {
				if (i > 0) {
					folders.append(",");
				}

				String directory = (String) folderTableModel.getValueAt(i, 0);
				boolean monitored = (boolean) folderTableModel.getValueAt(i, 1);

				// escape embedded commas. note: backslashing isn't safe as it conflicts with
				// Windows path separators:
				// http://ps3mediaserver.org/forum/viewtopic.php?f=14&t=8883&start=250#p43520
				folders.append(directory.replace(",", "&comma;"));
				if (monitored) {
					if (i2 > 0) {
						foldersMonitored.append(",");
					}
					i2++;

					foldersMonitored.append(directory.replace(",", "&comma;"));
				}
			}

			configuration.setFolders(folders.toString());
			configuration.setFoldersMonitored(foldersMonitored.toString());
		}
	}

	public JComponent build() {
		// Set basic layout
		OrientedPanelBuilder builder = new OrientedPanelBuilder(PANEL_COL_SPEC, PANEL_ROW_SPEC);
		builder.border(Borders.DLU4);
		builder.opaque(true);

		CellConstraints cc = builder.getCellConstraints();

		// Init all gui components
		initSimpleComponents(cc, builder);
		OrientedPanelBuilder builderSharedFolder = initSharedFoldersGuiComponents(cc);

		// Build gui with initialized components
		if (!configuration.isHideAdvancedOptions()) {
			JComponent cmp = builder._addSeparator(Messages.getString("FoldTab.13"), cc.xyw(1, 1, 10));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.add(thumbgenCheckBox, cc.xyw(1, 3, 3));
			builder._addLabel(Messages.getString("NetworkTab.16"), cc.xyw(4, 3, 2));
			builder.add(seekpos, cc.xy(6, 3));
			builder.add(image_thumb, cc.xy(9, 3));

			builder._addLabel(Messages.getString("FoldTab.26"), cc.xyw(1, 5, 3));
			builder.add(audiothumbnail, cc.xyw(4, 5, 3));
			builder.add(mplayer_thumb, cc.xy(9, 5));

			builder._addLabel(Messages.getString("FoldTab.27"), cc.xy(1, 7));
			builder.add(defaultThumbFolder, cc.xyw(4, 7, 2));
			builder.add(select, cc.xy(6, 7));
			builder.add(dvdiso_thumb, cc.xy(9, 7));

			cmp = builder._addSeparator(Messages.getString("NetworkTab.59"), cc.xyw(1, 9, 10));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder._addLabel(Messages.getString("FoldTab.18"), cc.xyw(1, 11, 3));
			builder.add(sortmethod, cc.xyw(4, 11, 3));
			builder.add(ignorethewordthe, cc.xy(9, 11));

			builder.add(prettifyfilenames, cc.xyw(1, 13, 5));
			builder.add(episodeTitles, cc.xy(9, 13));

			cmp = builder._addSeparator(Messages.getString("NetworkTab.60"), cc.xyw(1, 15, 10));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.add(hideextensions, cc.xyw(1, 17, 3));
			builder.add(hideengines, cc.xyw(4, 17, 3));
			builder.add(hideemptyfolders, cc.xy(9, 17));

			builder.add(itunes, cc.xy(1, 19));
			builder.add(iphoto, cc.xyw(4, 19, 3));
			builder.add(aperture, cc.xy(9, 19));

			builder.add(cacheenable, cc.xy(1, 21));
			builder.add(cachereset, cc.xyw(4, 21, 3));
			builder.add(hidemedialibraryfolder, cc.xy(9, 21));

			builder.add(archive, cc.xyw(1, 23, 3));
			builder.add(hidevideosettings, cc.xyw(4, 23, 3));
			builder.add(hidetranscode, cc.xy(9, 23));

			builder.add(liveSubtitles, cc.xyw(1, 25, 3));
			builder._addLabel(Messages.getString("FoldTab.37"), cc.xyw(4, 25, 2));
			builder.add(atzLimit, cc.xy(6, 25));
			builder.add(newmediafolder, cc.xy(9, 25));

			builder.add(resume, cc.xy(1, 27));
			builder.add(recentlyplayedfolder, cc.xyw(4, 27, 3));

			builder.add(builderSharedFolder.getPanel(), cc.xyw(1, 29, 10));
		} else {
			builder.add(builderSharedFolder.getPanel(), cc.xyw(1, 1, 10));
		}

		JPanel panel = builder._getPanel();

		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);

		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	private void initSimpleComponents(CellConstraints cc, OrientedPanelBuilder builder) {
		// Thumbnail seeking position
		seekpos = new JTextField("" + configuration.getThumbnailSeekPos());
		seekpos.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(seekpos.getText());
					configuration.setThumbnailSeekPos(ab);
					if (configuration.getUseCache()) {
						PMS.get().getDatabase().init(true);
					}
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
		thumbgenCheckBox = new CustomJCheckBox(Messages.getString("NetworkTab.2"), configuration.isThumbnailGenerationEnabled());
		thumbgenCheckBox.setContentAreaFilled(false);
		thumbgenCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setThumbnailGenerationEnabled((e.getStateChange() == ItemEvent.SELECTED));
				seekpos.setEnabled(configuration.isThumbnailGenerationEnabled());
				mplayer_thumb.setEnabled(configuration.isThumbnailGenerationEnabled());
			}
		});

		// Use MPlayer for video thumbnails
		mplayer_thumb = new CustomJCheckBox(Messages.getString("FoldTab.14"), configuration.isUseMplayerForVideoThumbs());
		mplayer_thumb.setToolTipText(Messages.getString("FoldTab.61"));
		mplayer_thumb.setContentAreaFilled(false);
		mplayer_thumb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setUseMplayerForVideoThumbs((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		if (configuration.isThumbnailGenerationEnabled()) {
			mplayer_thumb.setEnabled(true);
		} else {
			mplayer_thumb.setEnabled(false);
		}

		// DVD ISO thumbnails
		dvdiso_thumb = new CustomJCheckBox(Messages.getString("FoldTab.19"), configuration.isDvdIsoThumbnails());
		dvdiso_thumb.setContentAreaFilled(false);
		dvdiso_thumb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setDvdIsoThumbnails((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Image thumbnails
		image_thumb = new CustomJCheckBox(Messages.getString("FoldTab.21"), configuration.getImageThumbnailsEnabled());
		image_thumb.setContentAreaFilled(false);
		image_thumb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setImageThumbnailsEnabled((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Audio thumbnails import
		final KeyedComboBoxModel thumbKCBM = new KeyedComboBoxModel(new Object[]{"0", "1", "2"}, new Object[]{Messages.getString("FoldTab.35"), Messages.getString("FoldTab.23"), Messages.getString("FoldTab.24")});
		audiothumbnail = new JComboBox(thumbKCBM);
		audiothumbnail.setEditable(false);
		builder.orientLabelRenderer((JLabel)audiothumbnail.getRenderer());

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
		hidevideosettings = new CustomJCheckBox(Messages.getString("FoldTab.38"), configuration.getHideVideoSettings());
		hidevideosettings.setContentAreaFilled(false);
		hidevideosettings.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideVideoSettings((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Hide #--TRANSCODE--# folder
		hidetranscode = new CustomJCheckBox(Messages.getString("FoldTab.33"), configuration.getHideTranscodeEnabled());
		hidetranscode.setContentAreaFilled(false);
		hidetranscode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideTranscodeEnabled((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Hide cache folder
		hidemedialibraryfolder = new CustomJCheckBox(Messages.getString("FoldTab.32"), configuration.isHideMediaLibraryFolder());
		hidemedialibraryfolder.setContentAreaFilled(false);
		hidemedialibraryfolder.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideMediaLibraryFolder((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Browse compressed archives
		archive = new CustomJCheckBox(Messages.getString("NetworkTab.1"), configuration.isArchiveBrowsing());
		archive.setContentAreaFilled(false);
		archive.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setArchiveBrowsing(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		// Enable the cache
		cacheenable = new CustomJCheckBox(Messages.getString("NetworkTab.17"), configuration.getUseCache());
		cacheenable.setToolTipText(Messages.getString("FoldTab.48"));
		cacheenable.setContentAreaFilled(false);
		cacheenable.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setUseCache((e.getStateChange() == ItemEvent.SELECTED));
				cachereset.setEnabled(configuration.getUseCache());
				setScanLibraryEnabled(configuration.getUseCache());
			}
		});

		// Reset cache
		cachereset = new CustomJButton(Messages.getString("NetworkTab.18"));
		cachereset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int option = JOptionPane.showConfirmDialog(
					looksFrame,
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
		hideextensions = new CustomJCheckBox(Messages.getString("FoldTab.5"), configuration.isHideExtensions());
		hideextensions.setContentAreaFilled(false);
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
		hideengines = new CustomJCheckBox(Messages.getString("FoldTab.8"), configuration.isHideEngineNames());
		hideengines.setToolTipText(Messages.getString("FoldTab.46"));
		hideengines.setContentAreaFilled(false);
		hideengines.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideEngineNames((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Hide empty folders
		hideemptyfolders = new CustomJCheckBox(Messages.getString("FoldTab.31"), configuration.isHideEmptyFolders());
		hideemptyfolders.setToolTipText(Messages.getString("FoldTab.59"));
		hideemptyfolders.setContentAreaFilled(false);
		hideemptyfolders.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideEmptyFolders((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		// Show iTunes library
		itunes = new CustomJCheckBox(Messages.getString("FoldTab.30"), configuration.isShowItunesLibrary());
		itunes.setToolTipText(Messages.getString("FoldTab.47"));
		itunes.setContentAreaFilled(false);
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
		iphoto = new CustomJCheckBox(Messages.getString("FoldTab.29"), configuration.isShowIphotoLibrary());
		iphoto.setContentAreaFilled(false);
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
		aperture = new CustomJCheckBox(Messages.getString("FoldTab.34"), configuration.isShowApertureLibrary());
		aperture.setContentAreaFilled(false);
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
				String.valueOf(UMSUtils.SORT_LOC_SENS),  // alphabetical
				String.valueOf(UMSUtils.SORT_LOC_NAT),   // natural sort
				String.valueOf(UMSUtils.SORT_INS_ASCII), // ASCIIbetical
				String.valueOf(UMSUtils.SORT_MOD_NEW),   // newest first
				String.valueOf(UMSUtils.SORT_MOD_OLD),   // oldest first
				String.valueOf(UMSUtils.SORT_RANDOM),    // random
				String.valueOf(UMSUtils.SORT_NO_SORT)    // no sorting
			},
			new Object[]{
				Messages.getString("FoldTab.15"),
				Messages.getString("FoldTab.22"),
				Messages.getString("FoldTab.20"),
				Messages.getString("FoldTab.16"),
				Messages.getString("FoldTab.17"),
				Messages.getString("FoldTab.58"),
				Messages.getString("FoldTab.62")
			}
		);
		sortmethod = new JComboBox(kcbm);
		sortmethod.setEditable(false);
		builder.orientLabelRenderer((JLabel)sortmethod.getRenderer());
		kcbm.setSelectedKey("" + configuration.getSortMethod(null));

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
		ignorethewordthe = new CustomJCheckBox(Messages.getString("FoldTab.39"), configuration.isIgnoreTheWordThe());
		ignorethewordthe.setToolTipText(Messages.getString("FoldTab.44"));
		ignorethewordthe.setContentAreaFilled(false);
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

		liveSubtitles = new CustomJCheckBox(Messages.getString("FoldTab.42"), configuration.isHideLiveSubtitlesFolder());
		liveSubtitles.setContentAreaFilled(false);
		liveSubtitles.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideLiveSubtitlesFolder((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		prettifyfilenames = new CustomJCheckBox(Messages.getString("FoldTab.43"), configuration.isPrettifyFilenames());
		prettifyfilenames.setToolTipText(Messages.getString("FoldTab.45"));
		prettifyfilenames.setContentAreaFilled(false);
		prettifyfilenames.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setPrettifyFilenames((e.getStateChange() == ItemEvent.SELECTED));
				hideextensions.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
				episodeTitles.setEnabled((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		episodeTitles = new CustomJCheckBox(Messages.getString("FoldTab.63"), configuration.isUseInfoFromIMDB());
		episodeTitles.setToolTipText(Messages.getString("FoldTab.64"));
		episodeTitles.setContentAreaFilled(false);
		if (!configuration.isPrettifyFilenames()) {
			episodeTitles.setEnabled(false);
		}
		episodeTitles.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setUseInfoFromIMDB((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		newmediafolder = new CustomJCheckBox(Messages.getString("FoldTab.54"), configuration.isHideNewMediaFolder());
		newmediafolder.setToolTipText(Messages.getString("FoldTab.60"));
		newmediafolder.setContentAreaFilled(false);
		newmediafolder.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideNewMediaFolder((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		resume = new CustomJCheckBox(Messages.getString("NetworkTab.68"), configuration.isResumeEnabled());
		resume.setToolTipText(Messages.getString("NetworkTab.69"));
		resume.setContentAreaFilled(false);
		resume.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setResume((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		recentlyplayedfolder = new CustomJCheckBox(Messages.getString("FoldTab.55"), configuration.isHideRecentlyPlayedFolder());
		recentlyplayedfolder.setContentAreaFilled(false);
		recentlyplayedfolder.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setHideRecentlyPlayedFolder((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
	}

	private OrientedPanelBuilder initSharedFoldersGuiComponents(CellConstraints cc) {
		OrientedPanelBuilder builderFolder = new OrientedPanelBuilder(SHARED_FOLDER_COL_SPEC, SHARED_FOLDER_ROW_SPEC);
		builderFolder.opaque(true);

		JComponent cmp = builderFolder._addSeparator(Messages.getString("FoldTab.7"), cc.xyw(1, 1, 7));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		folderTableModel = new SharedFoldersTableModel();
		FList = new JTable(folderTableModel);

		builderFolder.orientLabelRenderer((JLabel)FList.getDefaultRenderer(FList.getColumnClass(0)));

		TableColumn column = FList.getColumnModel().getColumn(0);
		column.setMinWidth(650);

		/* An attempt to set the correct row height adjusted for font scaling.
		 * It sets all rows based on the font size of cell (0, 0). The + 4 is
		 * to allow 2 pixels above and below the text. */
		DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) FList.getCellRenderer(0,0);
		FontMetrics metrics = cellRenderer.getFontMetrics(cellRenderer.getFont());
		FList.setRowHeight(metrics.getLeading() + metrics.getMaxAscent() + metrics.getMaxDescent() + 4);
		FList.setIntercellSpacing(new Dimension(8, 2));

		CustomJButton but = new CustomJButton(LooksFrame.readImageIcon("button-adddirectory.png"));
		but.setToolTipText(Messages.getString("FoldTab.9"));
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser;
				try {
					chooser = new JFileChooser();
				} catch (Exception ee) {
					chooser = new JFileChooser(new RestrictedFileSystemView());
				}
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog((Component) e.getSource());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					((SharedFoldersTableModel) FList.getModel()).addRow(new Object[]{chooser.getSelectedFile().getAbsolutePath(), false});
					if (FList.getModel().getValueAt(0, 0).equals(ALL_DRIVES)) {
						((SharedFoldersTableModel) FList.getModel()).removeRow(0);
					}
					updateModel();
				}
			}
		});
		builderFolder.add(but, cc.xy(1, 3));

		CustomJButton but2 = new CustomJButton(LooksFrame.readImageIcon("button-remove.png"));
		but2.setToolTipText(Messages.getString("FoldTab.36"));
		but2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (FList.getSelectedRow() > -1) {
					((SharedFoldersTableModel) FList.getModel()).removeRow(FList.getSelectedRow());
					if (FList.getModel().getRowCount() == 0) {
						folderTableModel.addRow(new Object[]{ALL_DRIVES, false});
					}
					updateModel();
				}
			}
		});
		builderFolder.add(but2, cc.xy(2, 3));

		CustomJButton but3 = new CustomJButton(LooksFrame.readImageIcon("button-arrow-down.png"));
		but3.setToolTipText(Messages.getString("FoldTab.12"));
		but3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < FList.getRowCount() - 1; i++) {
					if (FList.isRowSelected(i)) {
						Object  value1 = FList.getValueAt(i, 0);
						boolean value2 = (boolean) FList.getValueAt(i, 1);

						FList.setValueAt(FList.getValueAt(i + 1, 0), i    , 0);
						FList.setValueAt(value1                    , i + 1, 0);
						FList.setValueAt(FList.getValueAt(i + 1, 1), i    , 1);
						FList.setValueAt(value2                    , i + 1, 1);
						FList.changeSelection(i + 1, 1, false, false);

						break;
					}
				}
			}
		});
		builderFolder.add(but3, cc.xy(3, 3));

		CustomJButton but4 = new CustomJButton(LooksFrame.readImageIcon("button-arrow-up.png"));
		but4.setToolTipText(Messages.getString("FoldTab.12"));
		but4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 1; i < FList.getRowCount(); i++) {
					if (FList.isRowSelected(i)) {
						Object  value1 = FList.getValueAt(i, 0);
						boolean value2 = (boolean) FList.getValueAt(i, 1);

						FList.setValueAt(FList.getValueAt(i - 1, 0), i    , 0);
						FList.setValueAt(value1                    , i - 1, 0);
						FList.setValueAt(FList.getValueAt(i - 1, 1), i    , 1);
						FList.setValueAt(value2                    , i - 1, 1);
						FList.changeSelection(i - 1, 1, false, false);

						break;

					}
				}
			}
		});
		builderFolder.add(but4, cc.xy(4, 3));

		but5 = new CustomJButton(LooksFrame.readImageIcon("button-scan.png"));
		but5.setToolTipText(Messages.getString("FoldTab.2"));
		but5.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (configuration.getUseCache()) {
					DLNAMediaDatabase database = PMS.get().getDatabase();

					if (database != null) {
						if (!database.isScanLibraryRunning()) {
							int option = JOptionPane.showConfirmDialog(
								looksFrame,
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
								looksFrame,
								Messages.getString("FoldTab.10"),
								Messages.getString("Dialog.Question"),
								JOptionPane.YES_NO_OPTION);
							if (option == JOptionPane.YES_OPTION) {
								database.stopScanLibrary();
								looksFrame.setStatusLine(null);
								setScanLibraryEnabled(false);
								but5.setToolTipText(Messages.getString("FoldTab.41"));
							}
						}
					}
				}
			}
		});
		builderFolder.add(but5, cc.xy(5, 3));
		but5.setEnabled(configuration.getUseCache());

		File[] folders = PMS.get().getSharedFoldersArray(false);
		if (folders != null && folders.length > 0) {
			File[] foldersMonitored = PMS.get().getSharedFoldersArray(true);
			for (File folder : folders) {
				boolean isMonitored = false;
				if (foldersMonitored != null && foldersMonitored.length > 0) {
					for (File folderMonitored : foldersMonitored) {
						if (folderMonitored.getAbsolutePath().equals(folder.getAbsolutePath())) {
							isMonitored = true;
						}
					}
				}
				folderTableModel.addRow(new Object[]{folder.getAbsolutePath(), isMonitored});
			}
		} else {
			folderTableModel.addRow(new Object[]{ALL_DRIVES, false});
		}

		JScrollPane pane = new JScrollPane(FList);
		Dimension d = FList.getPreferredSize();
		pane.setPreferredSize(new Dimension(d.width, FList.getRowHeight() * 2));
		builderFolder.add(pane, cc.xyw(1, 5, 7));

		return builderFolder;
	}

	public void setScanLibraryEnabled(boolean enabled) {
		but5.setEnabled(enabled);
		but5.setIcon(LooksFrame.readImageIcon("button-scan.png"));
		but5.setRolloverIcon(LooksFrame.readImageIcon("button-scan.png"));
		but5.setToolTipText(Messages.getString("FoldTab.2"));
	}

	public class SharedFoldersTableModel extends DefaultTableModel {
		private static final long serialVersionUID = -4247839506937958655L;

		public SharedFoldersTableModel() {
			super(new String[]{Messages.getString("FoldTab.56"), Messages.getString("FoldTab.57")}, 0);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Class clazz = String.class;
			switch (columnIndex) {
				case 1:
					clazz = Boolean.class;
					break;
				default:
					break;
			}
			return clazz;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 1;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			Vector rowVector = (Vector) dataVector.elementAt(row);
			if (aValue instanceof Boolean && column == 1) {
				rowVector.setElementAt((boolean) aValue, 1);
			} else {
				rowVector.setElementAt(aValue, column);
			}
			fireTableCellUpdated(row, column);
			updateModel();
		}
	}
}
