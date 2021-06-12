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
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.PmsConfiguration.SubtitlesInfoLevel;
import net.pms.newgui.components.CustomJButton;
import net.pms.util.CoverSupplier;
import net.pms.util.FormLayoutUtil;
import net.pms.util.FullyPlayedAction;
import net.pms.util.KeyedComboBoxModel;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationShareTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(NavigationShareTab.class);
	public static final String ALL_DRIVES = Messages.getString("FoldTab.0");

	private JCheckBox hideExtensions;
	private JCheckBox hideEmptyFolders;
	private JCheckBox hideEngines;
	private JTextField seekPosition;
	private JCheckBox generateThumbnails;
	private JCheckBox mplayerThumbnails;
	private JCheckBox dvdIsoThumbnails;
	private JCheckBox imageThumbnails;
	private JCheckBox cacheEnable;
	private JCheckBox archive;
	private JComboBox<String> sortMethod;
	private JComboBox<String> audioThumbnails;
	private JTextField defaultThumbFolder;
	private JCheckBox iPhoto;
	private JCheckBox aperture;
	public static JCheckBox iTunes;
	private CustomJButton select;
	private CustomJButton cacheReset;
	private JCheckBox ignoreTheWordThe;
	private JTextField atzLimit;
	private JCheckBox prettifyFilenames;
	private JCheckBox isUseInfoFromAPI;
	private JCheckBox resume;
	private JCheckBox useSymlinksTargetFile;
	private JComboBox<String> fullyPlayedAction;
	private JTextField fullyPlayedOutputDirectory;
	private CustomJButton selectFullyPlayedOutputDirectory;

	private JComboBox<String> addVideoSuffix;

	// Settings for the visibility of virtual folders
	private JCheckBox isShowFolderServerSettings;
	private JCheckBox isShowFolderTranscode;
	private JCheckBox isShowFolderMediaLibrary;
	private JCheckBox isShowFolderRecentlyPlayed;
	private JCheckBox isShowFolderLiveSubtitles;

	private final PmsConfiguration configuration;
	private LooksFrame looksFrame;

	NavigationShareTab(PmsConfiguration configuration, LooksFrame looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
	}

	private static final String PANEL_COL_SPEC = "left:pref,          3dlu,                pref, 3dlu,                       pref, 3dlu,               pref, 3dlu, pref, 3dlu, pref, default:grow";
	private static final String PANEL_ROW_SPEC =
		//                                //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		"p," +                            // Thumbnails
		"3dlu," +                         //
		"p," +                            //                      Generate thumbnails         Thumbnail seeking position:         [seeking position]               Image thumbnails
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"9dlu," +                         //
		"p," +                            // File sorting
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"9dlu," +                         //
		"p," +                            // Virtual folders
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"3dlu," +                         //
		"p," +                            //
		"9dlu," +                         //
		"fill:default:grow";              // Shared folders

	public JComponent build() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(PANEL_COL_SPEC, orientation);

		// Set basic layout
		FormLayout layout = new FormLayout(colSpec, PANEL_ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		// Init all gui components
		initSimpleComponents(cc);

		// Build gui with initialized components
		if (!configuration.isHideAdvancedOptions()) {
			JComponent cmp = builder.addSeparator(Messages.getString("FoldTab.13"),      FormLayoutUtil.flip(cc.xyw(1, 1, 10), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.add(GuiUtil.getPreferredSizeComponent(generateThumbnails),             FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));
			builder.addLabel(Messages.getString("NetworkTab.16"),                        FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));
			builder.add(seekPosition,                                                         FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(imageThumbnails),                  FormLayoutUtil.flip(cc.xy(7, 3), colSpec, orientation));

			builder.addLabel(Messages.getString("FoldTab.26"),                           FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));
			builder.add(audioThumbnails,                                                  FormLayoutUtil.flip(cc.xyw(3, 5, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(mplayerThumbnails),                FormLayoutUtil.flip(cc.xy(7, 5), colSpec, orientation));

			builder.addLabel(Messages.getString("FoldTab.27"),                           FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));
			builder.add(defaultThumbFolder,                                              FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));
			builder.add(select,                                                          FormLayoutUtil.flip(cc.xy(5, 7), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(dvdIsoThumbnails),                 FormLayoutUtil.flip(cc.xy(7, 7), colSpec, orientation));

			cmp = builder.addSeparator(Messages.getString("NetworkTab.59"),              FormLayoutUtil.flip(cc.xy(1, 9), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.addLabel(Messages.getString("FoldTab.18"),                           FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));
			builder.add(sortMethod,                                                      FormLayoutUtil.flip(cc.xyw(3, 11, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(ignoreTheWordThe),             FormLayoutUtil.flip(cc.xy(7, 11), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(prettifyFilenames),            FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(hideExtensions),               FormLayoutUtil.flip(cc.xy(3, 13), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isUseInfoFromAPI),             FormLayoutUtil.flip(cc.xy(7, 13), colSpec, orientation));

			builder.addLabel(Messages.getString("FoldTab.addSubtitlesInfo"),             FormLayoutUtil.flip(cc.xy(1, 15), colSpec, orientation));
			builder.add(addVideoSuffix,                                                  FormLayoutUtil.flip(cc.xyw(3, 15, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(hideEngines),                  FormLayoutUtil.flip(cc.xy(7, 15), colSpec, orientation));

			cmp = builder.addSeparator(Messages.getString("NetworkTab.60"),              FormLayoutUtil.flip(cc.xy(1, 17), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.add(GuiUtil.getPreferredSizeComponent(iTunes),                       FormLayoutUtil.flip(cc.xy(1, 19), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(iPhoto),                       FormLayoutUtil.flip(cc.xy(3, 19), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(aperture),                     FormLayoutUtil.flip(cc.xy(7, 19), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(cacheEnable),                  FormLayoutUtil.flip(cc.xy(1, 21), colSpec, orientation));
			builder.add(cacheReset,                                                      FormLayoutUtil.flip(cc.xyw(3, 21, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderMediaLibrary),     FormLayoutUtil.flip(cc.xy(7, 21), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(archive),                      FormLayoutUtil.flip(cc.xy(1, 23), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderServerSettings),   FormLayoutUtil.flip(cc.xy(3, 23), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderTranscode),        FormLayoutUtil.flip(cc.xy(7, 23), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderLiveSubtitles),    FormLayoutUtil.flip(cc.xy(1, 25), colSpec, orientation));
			builder.addLabel(Messages.getString("FoldTab.37"),                           FormLayoutUtil.flip(cc.xy(3, 25), colSpec, orientation));
			builder.add(atzLimit,                                                        FormLayoutUtil.flip(cc.xy(5, 25), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(resume),                       FormLayoutUtil.flip(cc.xy(1, 27), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderRecentlyPlayed),   FormLayoutUtil.flip(cc.xy(3, 27), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(hideEmptyFolders),             FormLayoutUtil.flip(cc.xy(7, 27), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(useSymlinksTargetFile),        FormLayoutUtil.flip(cc.xy(1, 29), colSpec, orientation));

			builder.addLabel(Messages.getString("FoldTab.72"),                           FormLayoutUtil.flip(cc.xy(1, 31), colSpec, orientation));
			builder.add(fullyPlayedAction,                                               FormLayoutUtil.flip(cc.xyw(3, 31, 3), colSpec, orientation));
			builder.add(fullyPlayedOutputDirectory,                                      FormLayoutUtil.flip(cc.xy(7, 31), colSpec, orientation));
			builder.add(selectFullyPlayedOutputDirectory,                                FormLayoutUtil.flip(cc.xy(9, 31), colSpec, orientation));
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
		seekPosition = new JTextField("" + configuration.getThumbnailSeekPos());
		seekPosition.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(seekPosition.getText());
					configuration.setThumbnailSeekPos(ab);
					if (configuration.getUseCache()) {
						PMS.get().getDatabase().init(true);
					}
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse thumbnail seek position from \"" + seekPosition.getText() + "\"");
				}

			}
		});
		if (configuration.isThumbnailGenerationEnabled()) {
			seekPosition.setEnabled(true);
		} else {
			seekPosition.setEnabled(false);
		}

		// Generate thumbnails
		generateThumbnails = new JCheckBox(Messages.getString("NetworkTab.2"), configuration.isThumbnailGenerationEnabled());
		generateThumbnails.setContentAreaFilled(false);
		generateThumbnails.addItemListener((ItemEvent e) -> {
			configuration.setThumbnailGenerationEnabled((e.getStateChange() == ItemEvent.SELECTED));
			seekPosition.setEnabled(configuration.isThumbnailGenerationEnabled());
			mplayerThumbnails.setEnabled(configuration.isThumbnailGenerationEnabled());
		});

		// Use MPlayer for video thumbnails
		mplayerThumbnails = new JCheckBox(Messages.getString("FoldTab.14"), configuration.isUseMplayerForVideoThumbs());
		mplayerThumbnails.setToolTipText(Messages.getString("FoldTab.61"));
		mplayerThumbnails.setContentAreaFilled(false);
		mplayerThumbnails.addItemListener((ItemEvent e) -> {
			configuration.setUseMplayerForVideoThumbs((e.getStateChange() == ItemEvent.SELECTED));
		});
		if (configuration.isThumbnailGenerationEnabled()) {
			mplayerThumbnails.setEnabled(true);
		} else {
			mplayerThumbnails.setEnabled(false);
		}

		// DVD ISO thumbnails
		dvdIsoThumbnails = new JCheckBox(Messages.getString("FoldTab.19"), configuration.isDvdIsoThumbnails());
		dvdIsoThumbnails.setContentAreaFilled(false);
		dvdIsoThumbnails.addItemListener((ItemEvent e) -> {
			configuration.setDvdIsoThumbnails((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Image thumbnails
		imageThumbnails = new JCheckBox(Messages.getString("FoldTab.21"), configuration.getImageThumbnailsEnabled());
		imageThumbnails.setContentAreaFilled(false);
		imageThumbnails.addItemListener((ItemEvent e) -> {
			configuration.setImageThumbnailsEnabled((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Audio thumbnails import
		final KeyedComboBoxModel<CoverSupplier, String> thumbKCBM = new KeyedComboBoxModel<>(
			new CoverSupplier[]{CoverSupplier.NONE, CoverSupplier.COVER_ART_ARCHIVE},
			new String[]{Messages.getString("FoldTab.35"), Messages.getString("FoldTab.73")}
		);
		audioThumbnails = new JComboBox<>(thumbKCBM);
		audioThumbnails.setEditable(false);

		thumbKCBM.setSelectedKey(configuration.getAudioThumbnailMethod());

		audioThumbnails.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setAudioThumbnailMethod(thumbKCBM.getSelectedKey());
				LOGGER.info("Setting {} {}", Messages.getRootString("FoldTab.26"), thumbKCBM.getSelectedValue());
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
		select.addActionListener((ActionEvent e) -> {
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
		});

		// Show Server Settings folder
		isShowFolderServerSettings = new JCheckBox(Messages.getString("FoldTab.ShowServerSettingsFolder"), configuration.isShowServerSettingsFolder());
		isShowFolderServerSettings.setContentAreaFilled(false);
		isShowFolderServerSettings.addItemListener((ItemEvent e) -> {
			configuration.setShowServerSettingsFolder((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Show #--TRANSCODE--# folder
		isShowFolderTranscode = new JCheckBox(Messages.getString("FoldTab.ShowTranscodeFolder"), configuration.isShowTranscodeFolder());
		isShowFolderTranscode.setContentAreaFilled(false);
		isShowFolderTranscode.addItemListener((ItemEvent e) -> {
			configuration.setShowTranscodeFolder((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Show Media Library folder
		isShowFolderMediaLibrary = new JCheckBox(Messages.getString("FoldTab.ShowMediaLibraryFolder"), configuration.isShowMediaLibraryFolder());
		isShowFolderMediaLibrary.setToolTipText(Messages.getString("NavigationSettingsTab.ShowMediaLibraryFolderTooltip"));
		isShowFolderMediaLibrary.setContentAreaFilled(false);
		isShowFolderMediaLibrary.addItemListener((ItemEvent e) -> {
			configuration.setShowMediaLibraryFolder((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Browse compressed archives
		archive = new JCheckBox(Messages.getString("NetworkTab.1"), configuration.isArchiveBrowsing());
		archive.setContentAreaFilled(false);
		archive.addItemListener((ItemEvent e) -> {
			configuration.setArchiveBrowsing(e.getStateChange() == ItemEvent.SELECTED);
		});

		// Enable the cache
		cacheEnable = new JCheckBox(Messages.getString("NavigationSettingsTab.EnableCache"), configuration.getUseCache());
		cacheEnable.setToolTipText(Messages.getString("NavigationSettingsTab.EnableCacheTooltip"));
		cacheEnable.setContentAreaFilled(false);
		cacheEnable.addItemListener((ItemEvent e) -> {
			configuration.setUseCache((e.getStateChange() == ItemEvent.SELECTED));
			cacheReset.setEnabled(configuration.getUseCache());
			SharedContentTab.setScanLibraryEnabled(configuration.getUseCache());
		});

		// Reset cache
		cacheReset = new CustomJButton(Messages.getString("NavigationSettingsTab.ResetCache"));
		cacheReset.addActionListener((ActionEvent e) -> {
			int option = JOptionPane.showConfirmDialog(
				looksFrame,
				Messages.getString("NavigationSettingsTab.CacheEmptiedExceptFullyPlayed") + "\n" + Messages.getString("NetworkTab.19"),
				Messages.getString("Dialog.Question"),
				JOptionPane.YES_NO_OPTION);
			if (option == JOptionPane.YES_OPTION) {
				PMS.get().getDatabase().init(true);
			}
		});
		cacheReset.setEnabled(configuration.getUseCache());

		// Hide file extensions
		hideExtensions = new JCheckBox(Messages.getString("FoldTab.5"), configuration.isHideExtensions());
		hideExtensions.setContentAreaFilled(false);
		if (configuration.isPrettifyFilenames()) {
			hideExtensions.setEnabled(false);
		}
		hideExtensions.addItemListener((ItemEvent e) -> {
			configuration.setHideExtensions((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Hide transcoding engine names
		hideEngines = new JCheckBox(Messages.getString("FoldTab.showEngineNamesAfterFilenames"), !configuration.isHideEngineNames());
		hideEngines.setToolTipText(Messages.getString("FoldTab.showEngineNamesAfterFilenamesToolTip"));
		hideEngines.setContentAreaFilled(false);
		hideEngines.addItemListener((ItemEvent e) -> {
			configuration.setHideEngineNames((e.getStateChange() != ItemEvent.SELECTED));
		});

		// Add subtitles information to video names
		final KeyedComboBoxModel<SubtitlesInfoLevel, String> videoSuffixKCBM = new KeyedComboBoxModel<>(
			new SubtitlesInfoLevel[] {
				SubtitlesInfoLevel.NONE,
				SubtitlesInfoLevel.BASIC,
				SubtitlesInfoLevel.FULL
			},
			new String[] {
				Messages.getString("Generic.None"),
				Messages.getString("Generic.Basic"),
				Messages.getString("Generic.Full")
			}
		);

		addVideoSuffix = new JComboBox<>(videoSuffixKCBM);
		addVideoSuffix.setEditable(false);
		addVideoSuffix.setToolTipText(Messages.getString("FoldTab.addSubtitlesInfoToolTip"));

		videoSuffixKCBM.setSelectedKey(configuration.getSubtitlesInfoLevel());

		addVideoSuffix.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				LOGGER.debug(
					"Setting \"{}\" to \"{}\"",
					Messages.getRootString("FoldTab.addSubtitlesInfo"),
					videoSuffixKCBM.getSelectedValue()
				);
				configuration.setSubtitlesInfoLevel(videoSuffixKCBM.getSelectedKey());
			}
		});

		// Hide empty folders
		hideEmptyFolders = new JCheckBox(Messages.getString("FoldTab.31"), configuration.isHideEmptyFolders());
		hideEmptyFolders.setToolTipText(Messages.getString("FoldTab.59"));
		hideEmptyFolders.setContentAreaFilled(false);
		hideEmptyFolders.addItemListener((ItemEvent e) -> {
			configuration.setHideEmptyFolders((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Use target file for symlinks
		useSymlinksTargetFile = new JCheckBox(Messages.getString("FoldTab.useSymlinksTargetFile"), configuration.isUseSymlinksTargetFile());
		useSymlinksTargetFile.setToolTipText(Messages.getString("FoldTab.useSymlinksTargetFileToolTip"));
		useSymlinksTargetFile.setContentAreaFilled(false);
		useSymlinksTargetFile.addItemListener((ItemEvent e) -> {
			configuration.setUseSymlinksTargetFile((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Show iTunes library
		iTunes = new JCheckBox(Messages.getString("FoldTab.30"), configuration.isShowItunesLibrary());
		iTunes.setToolTipText(Messages.getString("FoldTab.47"));
		iTunes.setContentAreaFilled(false);
		if (!(Platform.isMac() || Platform.isWindows())) {
			iTunes.setEnabled(false);
		}
		iTunes.addItemListener((ItemEvent e) -> {
			configuration.setShowItunesLibrary((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Show iPhoto library
		iPhoto = new JCheckBox(Messages.getString("FoldTab.29"), configuration.isShowIphotoLibrary());
		iPhoto.setContentAreaFilled(false);
		if (!Platform.isMac()) {
			iPhoto.setEnabled(false);
		}
		iPhoto.addItemListener((ItemEvent e) -> {
			configuration.setShowIphotoLibrary((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Show aperture library
		aperture = new JCheckBox(Messages.getString("FoldTab.34"), configuration.isShowApertureLibrary());
		aperture.setContentAreaFilled(false);
		if (!Platform.isMac()) {
			aperture.setEnabled(false);
		}
		aperture.addItemListener((ItemEvent e) -> {
			configuration.setShowApertureLibrary((e.getStateChange() == ItemEvent.SELECTED));
		});

		// File order
		final KeyedComboBoxModel<Integer, String> kcbm = new KeyedComboBoxModel<>(
			new Integer[]{
				UMSUtils.SORT_LOC_SENS,  // alphabetical
				UMSUtils.SORT_LOC_NAT,   // natural sort
				UMSUtils.SORT_INS_ASCII, // ASCIIbetical
				UMSUtils.SORT_MOD_NEW,   // newest first
				UMSUtils.SORT_MOD_OLD,   // oldest first
				UMSUtils.SORT_RANDOM,    // random
				UMSUtils.SORT_NO_SORT    // no sorting
			},
			new String[]{
				Messages.getString("FoldTab.15"),
				Messages.getString("FoldTab.22"),
				Messages.getString("FoldTab.20"),
				Messages.getString("FoldTab.16"),
				Messages.getString("FoldTab.17"),
				Messages.getString("FoldTab.58"),
				Messages.getString("FoldTab.62")
			}
		);
		sortMethod = new JComboBox<>(kcbm);
		sortMethod.setEditable(false);
		kcbm.setSelectedKey(configuration.getSortMethod(null));

		sortMethod.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setSortMethod(kcbm.getSelectedKey());
				LOGGER.info("Setting {} {}", Messages.getRootString("FoldTab.18"), kcbm.getSelectedValue());
			}
		});

		// Ignore the word "the" while sorting
		ignoreTheWordThe = new JCheckBox(Messages.getString("FoldTab.39"), configuration.isIgnoreTheWordAandThe());
		ignoreTheWordThe.setToolTipText(Messages.getString("FoldTab.44"));
		ignoreTheWordThe.setContentAreaFilled(false);
		ignoreTheWordThe.addItemListener((ItemEvent e) -> {
			configuration.setIgnoreTheWordAandThe((e.getStateChange() == ItemEvent.SELECTED));
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

		isShowFolderLiveSubtitles = new JCheckBox(Messages.getString("FoldTab.ShowLiveSubtitlesFolder"), configuration.isShowLiveSubtitlesFolder());
		isShowFolderLiveSubtitles.setContentAreaFilled(false);
		isShowFolderLiveSubtitles.addItemListener((ItemEvent e) -> {
			configuration.setShowLiveSubtitlesFolder((e.getStateChange() == ItemEvent.SELECTED));
		});

		prettifyFilenames = new JCheckBox(Messages.getString("FoldTab.43"), configuration.isPrettifyFilenames());
		prettifyFilenames.setToolTipText(Messages.getString("FoldTab.45"));
		prettifyFilenames.setContentAreaFilled(false);
		prettifyFilenames.addItemListener((ItemEvent e) -> {
			configuration.setPrettifyFilenames((e.getStateChange() == ItemEvent.SELECTED));
			hideExtensions.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
		});

		isUseInfoFromAPI = new JCheckBox(Messages.getString("FoldTab.UseInfoFromAPI"), configuration.isUseInfoFromIMDb());
		isUseInfoFromAPI.setToolTipText(Messages.getString("FoldTab.UseInfoFromAPITooltip"));
		isUseInfoFromAPI.setContentAreaFilled(false);
		isUseInfoFromAPI.addItemListener((ItemEvent e) -> {
			configuration.setUseInfoFromIMDb((e.getStateChange() == ItemEvent.SELECTED));
		});

		resume = new JCheckBox(Messages.getString("NetworkTab.68"), configuration.isResumeEnabled());
		resume.setToolTipText(Messages.getString("NetworkTab.69"));
		resume.setContentAreaFilled(false);
		resume.addItemListener((ItemEvent e) -> {
			configuration.setResume((e.getStateChange() == ItemEvent.SELECTED));
		});

		isShowFolderRecentlyPlayed = new JCheckBox(Messages.getString("FoldTab.ShowRecentlyPlayedFolder"), configuration.isShowRecentlyPlayedFolder());
		isShowFolderRecentlyPlayed.setContentAreaFilled(false);
		isShowFolderRecentlyPlayed.addItemListener((ItemEvent e) -> {
			configuration.setShowRecentlyPlayedFolder((e.getStateChange() == ItemEvent.SELECTED));
		});

		// Fully played action
		final KeyedComboBoxModel<FullyPlayedAction, String> fullyPlayedActionModel = new KeyedComboBoxModel<>(
			new FullyPlayedAction[]{
				FullyPlayedAction.NO_ACTION,
				FullyPlayedAction.MARK,
				FullyPlayedAction.HIDE_MEDIA,
				FullyPlayedAction.MOVE_FOLDER,
				FullyPlayedAction.MOVE_FOLDER_AND_MARK,
				FullyPlayedAction.MOVE_TRASH
			},
			new String[]{
				Messages.getString("FoldTab.67"),
				Messages.getString("FoldTab.68"),
				Messages.getString("FoldTab.69"),
				Messages.getString("FoldTab.70"),
				Messages.getString("FoldTab.MoveFolderAndMark"),
				Messages.getString("FoldTab.71")
			}
		);
		fullyPlayedAction = new JComboBox<>(fullyPlayedActionModel);
		fullyPlayedAction.setEditable(false);
		fullyPlayedActionModel.setSelectedKey(configuration.getFullyPlayedAction());
		fullyPlayedAction.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					configuration.setFullyPlayedAction(fullyPlayedActionModel.getSelectedKey());
					fullyPlayedOutputDirectory.setEnabled(
						configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER ||
						configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER_AND_MARK
					);
					selectFullyPlayedOutputDirectory.setEnabled(
						configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER ||
						configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER_AND_MARK
					);

					if (configuration.getUseCache() && fullyPlayedActionModel.getSelectedKey() == FullyPlayedAction.NO_ACTION) {
						PMS.get().getDatabase().init(true);
					}
				}
			}
		});

		// Watched video output directory
		fullyPlayedOutputDirectory = new JTextField(configuration.getFullyPlayedOutputDirectory());
		fullyPlayedOutputDirectory.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setFullyPlayedOutputDirectory(fullyPlayedOutputDirectory.getText());
			}
		});
		fullyPlayedOutputDirectory.setEnabled(
			configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER ||
			configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER_AND_MARK
		);

		// Watched video output directory selection button
		selectFullyPlayedOutputDirectory = new CustomJButton("...");
		selectFullyPlayedOutputDirectory.addActionListener(new ActionListener() {
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
					fullyPlayedOutputDirectory.setText(chooser.getSelectedFile().getAbsolutePath());
					configuration.setFullyPlayedOutputDirectory(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});

		selectFullyPlayedOutputDirectory.setEnabled(
			configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER ||
			configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER_AND_MARK
		);
	}
}
