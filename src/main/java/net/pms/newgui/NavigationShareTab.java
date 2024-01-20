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
package net.pms.newgui;

import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.UmsConfiguration.SubtitlesInfoLevel;
import net.pms.database.MediaDatabase;
import net.pms.newgui.components.CustomJButton;
import net.pms.newgui.util.FormLayoutUtil;
import net.pms.newgui.util.KeyedComboBoxModel;
import net.pms.util.CoverSupplier;
import net.pms.util.FullyPlayedAction;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationShareTab {

	private static final Logger LOGGER = LoggerFactory.getLogger(NavigationShareTab.class);

	private JCheckBox hideExtensions;
	private JCheckBox hideEmptyFolders;
	private JCheckBox hideEngines;
	private JTextField seekPosition;
	private JCheckBox generateThumbnails;
	private JCheckBox mplayerThumbnails;
	private JCheckBox dvdIsoThumbnails;
	private JCheckBox imageThumbnails;
	private JCheckBox archive;
	private JComboBox<String> sortMethod;
	private JComboBox<String> audioThumbnails;
	private JTextField defaultThumbFolder;
	private JCheckBox iPhoto;
	private JCheckBox aperture;
	private JCheckBox iTunes;
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

	private final UmsConfiguration configuration;
	private final LooksFrame looksFrame;

	NavigationShareTab(UmsConfiguration configuration, LooksFrame looksFrame) {
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
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout).border(Paddings.DLU4).opaque(true);

		CellConstraints cc = new CellConstraints();

		// Init all gui components
		initSimpleComponents(cc);

		// Build gui with initialized components
		if (!configuration.isHideAdvancedOptions()) {
			builder.addSeparator(Messages.getString("Thumbnails")).at(FormLayoutUtil.flip(cc.xyw(1, 1, 10), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(generateThumbnails)).at(FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));
			builder.addLabel(Messages.getString("ThumbnailSeekingPosition")).at(FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));
			builder.add(seekPosition).at(FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(imageThumbnails)).at(FormLayoutUtil.flip(cc.xy(7, 3), colSpec, orientation));

			builder.addLabel(Messages.getString("AudioThumbnailsImport")).at(FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));
			builder.add(audioThumbnails).at(FormLayoutUtil.flip(cc.xyw(3, 5, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(mplayerThumbnails)).at(FormLayoutUtil.flip(cc.xy(7, 5), colSpec, orientation));

			builder.addLabel(Messages.getString("AlternateVideoCoverArtFolder")).at(FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));
			builder.add(defaultThumbFolder).at(FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));
			builder.add(select).at(FormLayoutUtil.flip(cc.xy(5, 7), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(dvdIsoThumbnails)).at(FormLayoutUtil.flip(cc.xy(7, 7), colSpec, orientation));

			builder.addSeparator(Messages.getString("FileSortingNaming")).at(FormLayoutUtil.flip(cc.xyw(1, 9, 10), colSpec, orientation));

			builder.addLabel(Messages.getString("FileOrder")).at(FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));
			builder.add(sortMethod).at(FormLayoutUtil.flip(cc.xyw(3, 11, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(ignoreTheWordThe)).at(FormLayoutUtil.flip(cc.xy(7, 11), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(prettifyFilenames)).at(FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(hideExtensions)).at(FormLayoutUtil.flip(cc.xy(3, 13), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isUseInfoFromAPI)).at(FormLayoutUtil.flip(cc.xy(7, 13), colSpec, orientation));

			builder.addLabel(Messages.getString("AddSubtitlesInformationVideoNames")).at(FormLayoutUtil.flip(cc.xy(1, 15), colSpec, orientation));
			builder.add(addVideoSuffix).at(FormLayoutUtil.flip(cc.xyw(3, 15, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(hideEngines)).at(FormLayoutUtil.flip(cc.xy(7, 15), colSpec, orientation));

			builder.addSeparator(Messages.getString("VirtualFoldersFiles")).at(FormLayoutUtil.flip(cc.xyw(1, 17, 10), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(iTunes)).at(FormLayoutUtil.flip(cc.xy(1, 19), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(iPhoto)).at(FormLayoutUtil.flip(cc.xy(3, 19), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(aperture)).at(FormLayoutUtil.flip(cc.xy(7, 19), colSpec, orientation));

			builder.addLabel(Messages.getString("DatabaseCache")).at(FormLayoutUtil.flip(cc.xy(1, 21), colSpec, orientation));
			builder.add(cacheReset).at(FormLayoutUtil.flip(cc.xyw(3, 21, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderMediaLibrary)).at(FormLayoutUtil.flip(cc.xy(7, 21), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(archive)).at(FormLayoutUtil.flip(cc.xy(1, 23), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderServerSettings)).at(FormLayoutUtil.flip(cc.xy(3, 23), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderTranscode)).at(FormLayoutUtil.flip(cc.xy(7, 23), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderLiveSubtitles)).at(FormLayoutUtil.flip(cc.xy(1, 25), colSpec, orientation));
			builder.addLabel(Messages.getString("MinimumItemLimitBeforeAZ")).at(FormLayoutUtil.flip(cc.xy(3, 25), colSpec, orientation));
			builder.add(atzLimit).at(FormLayoutUtil.flip(cc.xy(5, 25), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(resume)).at(FormLayoutUtil.flip(cc.xy(1, 27), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(isShowFolderRecentlyPlayed)).at(FormLayoutUtil.flip(cc.xy(3, 27), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(hideEmptyFolders)).at(FormLayoutUtil.flip(cc.xy(7, 27), colSpec, orientation));

			builder.add(GuiUtil.getPreferredSizeComponent(useSymlinksTargetFile)).at(FormLayoutUtil.flip(cc.xy(1, 29), colSpec, orientation));

			builder.addLabel(Messages.getString("FullyPlayedAction")).at(FormLayoutUtil.flip(cc.xy(1, 31), colSpec, orientation));
			builder.add(fullyPlayedAction).at(FormLayoutUtil.flip(cc.xyw(3, 31, 3), colSpec, orientation));
			builder.add(fullyPlayedOutputDirectory).at(FormLayoutUtil.flip(cc.xy(7, 31), colSpec, orientation));
			builder.add(selectFullyPlayedOutputDirectory).at(FormLayoutUtil.flip(cc.xy(9, 31), colSpec, orientation));
		}

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
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
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse thumbnail seek position from \"" + seekPosition.getText() + "\"");
				}

			}
		});
		seekPosition.setEnabled(configuration.isThumbnailGenerationEnabled());

		// Generate thumbnails
		generateThumbnails = new JCheckBox(Messages.getString("GenerateThumbnails"), configuration.isThumbnailGenerationEnabled());
		generateThumbnails.setContentAreaFilled(false);
		generateThumbnails.addItemListener((ItemEvent e) -> {
			configuration.setThumbnailGenerationEnabled((e.getStateChange() == ItemEvent.SELECTED));
			seekPosition.setEnabled(configuration.isThumbnailGenerationEnabled());
			mplayerThumbnails.setEnabled(configuration.isThumbnailGenerationEnabled());
		});

		// Use MPlayer for video thumbnails
		mplayerThumbnails = new JCheckBox(Messages.getString("UseMplayerVideoThumbnails"), configuration.isUseMplayerForVideoThumbs());
		mplayerThumbnails.setToolTipText(Messages.getString("WhenSettingDisabledFfmpeg"));
		mplayerThumbnails.setContentAreaFilled(false);
		mplayerThumbnails.addItemListener((ItemEvent e) -> configuration.setUseMplayerForVideoThumbs((e.getStateChange() == ItemEvent.SELECTED)));
		mplayerThumbnails.setEnabled(configuration.isThumbnailGenerationEnabled());

		// DVD ISO thumbnails
		dvdIsoThumbnails = new JCheckBox(Messages.getString("DvdIsoThumbnails"), configuration.isDvdIsoThumbnails());
		dvdIsoThumbnails.setContentAreaFilled(false);
		dvdIsoThumbnails.addItemListener((ItemEvent e) -> configuration.setDvdIsoThumbnails((e.getStateChange() == ItemEvent.SELECTED)));

		// Image thumbnails
		imageThumbnails = new JCheckBox(Messages.getString("ImageThumbnails"), configuration.getImageThumbnailsEnabled());
		imageThumbnails.setContentAreaFilled(false);
		imageThumbnails.addItemListener((ItemEvent e) -> configuration.setImageThumbnailsEnabled((e.getStateChange() == ItemEvent.SELECTED)));

		// Audio thumbnails import
		final KeyedComboBoxModel<CoverSupplier, String> thumbKCBM = new KeyedComboBoxModel<>(
			new CoverSupplier[]{CoverSupplier.NONE, CoverSupplier.COVER_ART_ARCHIVE},
			new String[]{Messages.getString("None"), Messages.getString("DownloadFromCoverArtArchive")}
		);
		audioThumbnails = new JComboBox<>(thumbKCBM);
		audioThumbnails.setEditable(false);

		thumbKCBM.setSelectedKey(configuration.getAudioThumbnailMethod());

		audioThumbnails.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setAudioThumbnailMethod(thumbKCBM.getSelectedKey());
				LOGGER.info("Setting {} {}", Messages.getRootString("AudioThumbnailsImport"), thumbKCBM.getSelectedValue());
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
			int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getString("ChooseAFolder"));
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				defaultThumbFolder.setText(chooser.getSelectedFile().getAbsolutePath());
				configuration.setAlternateThumbFolder(chooser.getSelectedFile().getAbsolutePath());
			}
		});

		// Show Server Settings folder
		isShowFolderServerSettings = new JCheckBox(Messages.getString("ShowServerSettingsFolder"), configuration.isShowServerSettingsFolder());
		isShowFolderServerSettings.setToolTipText(Messages.getString("WarningThisAllowsShutdownComputer"));
		isShowFolderServerSettings.setContentAreaFilled(false);
		isShowFolderServerSettings.addItemListener((ItemEvent e) -> configuration.setShowServerSettingsFolder((e.getStateChange() == ItemEvent.SELECTED)));

		// Show #--TRANSCODE--# folder
		isShowFolderTranscode = new JCheckBox(Messages.getString("ShowTranscodeFolder"), configuration.isShowTranscodeFolder());
		isShowFolderTranscode.setContentAreaFilled(false);
		isShowFolderTranscode.addItemListener((ItemEvent e) -> configuration.setShowTranscodeFolder((e.getStateChange() == ItemEvent.SELECTED)));

		// Show Media Library folder
		isShowFolderMediaLibrary = new JCheckBox(Messages.getString("ShowMediaLibraryFolder"), configuration.isShowMediaLibraryFolder());
		isShowFolderMediaLibrary.setContentAreaFilled(false);
		isShowFolderMediaLibrary.addItemListener((ItemEvent e) -> configuration.setShowMediaLibraryFolder((e.getStateChange() == ItemEvent.SELECTED)));
		isShowFolderMediaLibrary.setToolTipText(Messages.getString("MediaLibraryFolderWillAvailable"));

		// Browse compressed archives
		archive = new JCheckBox(Messages.getString("BrowseCompressedArchives"), configuration.isArchiveBrowsing());
		archive.setContentAreaFilled(false);
		archive.addItemListener((ItemEvent e) -> configuration.setArchiveBrowsing(e.getStateChange() == ItemEvent.SELECTED));

		// Reset cache
		cacheReset = new CustomJButton(Messages.getString("ResetCache"));
		cacheReset.setToolTipText(Messages.getString("CacheEmptiedExceptFullyPlayed"));
		cacheReset.addActionListener((ActionEvent e) -> {
			int option = JOptionPane.showConfirmDialog(
				looksFrame,
				Messages.getString("CacheEmptiedExceptFullyPlayed") + "\n" + Messages.getString("AreYouSure"),
				Messages.getString("Question"),
				JOptionPane.YES_NO_OPTION);
			if (option == JOptionPane.YES_OPTION) {
				MediaDatabase.initForce();
				try {
					MediaDatabase.resetCache();
				} catch (SQLException e2) {
					LOGGER.debug("Error when re-initializing after manual cache reset:", e2);
				}
			}
		});

		// Hide file extensions
		hideExtensions = new JCheckBox(Messages.getString("HideFileExtensions"), configuration.isHideExtensions());
		hideExtensions.setContentAreaFilled(false);
		if (configuration.isPrettifyFilenames()) {
			hideExtensions.setEnabled(false);
		}
		hideExtensions.addItemListener((ItemEvent e) -> configuration.setHideExtensions((e.getStateChange() == ItemEvent.SELECTED)));

		// Hide transcoding engine names
		hideEngines = new JCheckBox(Messages.getString("AddEnginesNamesAfterFilenames"), !configuration.isHideEngineNames());
		hideEngines.setToolTipText(Messages.getString("IfEnabledEngineNameDisplayed"));
		hideEngines.setContentAreaFilled(false);
		hideEngines.addItemListener((ItemEvent e) -> configuration.setHideEngineNames((e.getStateChange() != ItemEvent.SELECTED)));

		// Add subtitles information to video names
		final KeyedComboBoxModel<SubtitlesInfoLevel, String> videoSuffixKCBM = new KeyedComboBoxModel<>(
			new SubtitlesInfoLevel[] {
				SubtitlesInfoLevel.NONE,
				SubtitlesInfoLevel.BASIC,
				SubtitlesInfoLevel.FULL
			},
			new String[] {
				Messages.getString("None"),
				Messages.getString("Basic"),
				Messages.getString("Full")
			}
		);

		addVideoSuffix = new JComboBox<>(videoSuffixKCBM);
		addVideoSuffix.setEditable(false);
		addVideoSuffix.setToolTipText(Messages.getString("AddsInformationAboutSelectedSubtitles"));

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
		hideEmptyFolders = new JCheckBox(Messages.getString("HideEmptyFolders"), configuration.isHideEmptyFolders());
		hideEmptyFolders.setToolTipText(Messages.getString("ThisMakesBrowsingSlower"));
		hideEmptyFolders.setContentAreaFilled(false);
		hideEmptyFolders.addItemListener((ItemEvent e) -> configuration.setHideEmptyFolders((e.getStateChange() == ItemEvent.SELECTED)));

		// Use target file for symlinks
		useSymlinksTargetFile = new JCheckBox(Messages.getString("UseTargetFileSymbolicLinks"), configuration.isUseSymlinksTargetFile());
		useSymlinksTargetFile.setToolTipText(Messages.getString("TreatMultipleSymbolicLinks"));
		useSymlinksTargetFile.setContentAreaFilled(false);
		useSymlinksTargetFile.addItemListener((ItemEvent e) -> configuration.setUseSymlinksTargetFile((e.getStateChange() == ItemEvent.SELECTED)));

		// Show iTunes library
		iTunes = new JCheckBox(Messages.getString("ShowItunesLibrary"), false);
		iTunes.setToolTipText(Messages.getString("IfEnabledThreeNewVirtual"));
		iTunes.setContentAreaFilled(false);
		iTunes.setEnabled(false);

		// Show iPhoto library
		iPhoto = new JCheckBox(Messages.getString("ShowIphotoLibrary"), false);
		iPhoto.setContentAreaFilled(false);
		iPhoto.setEnabled(false);

		// Show aperture library
		aperture = new JCheckBox(Messages.getString("ShowApertureLibrary"), false);
		aperture.setContentAreaFilled(false);
		aperture.setEnabled(false);

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
				Messages.getString("AlphabeticalAZ"),
				Messages.getString("Alphanumeric"),
				Messages.getString("Asciibetical"),
				Messages.getString("ByDateNewestFirst"),
				Messages.getString("ByDateOldestFirst"),
				Messages.getString("Random"),
				Messages.getString("NoSorting")
			}
		);
		sortMethod = new JComboBox<>(kcbm);
		sortMethod.setEditable(false);
		kcbm.setSelectedKey(configuration.getSortMethod(null));

		sortMethod.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setSortMethod(kcbm.getSelectedKey());
				LOGGER.info("Setting {} {}", Messages.getRootString("FileOrder"), kcbm.getSelectedValue());
			}
		});

		// Ignore the word "the" while sorting
		ignoreTheWordThe = new JCheckBox(Messages.getString("IgnoreArticlesATheSorting"), configuration.isIgnoreTheWordAandThe());
		ignoreTheWordThe.setToolTipText(Messages.getString("IfEnabledFilesWillOrdered"));
		ignoreTheWordThe.setContentAreaFilled(false);
		ignoreTheWordThe.addItemListener((ItemEvent e) -> configuration.setIgnoreTheWordAandThe((e.getStateChange() == ItemEvent.SELECTED)));

		atzLimit = new JTextField("" + configuration.getATZLimit());
		atzLimit.setToolTipText(Messages.getString("IfNumberItemsFolderExceeds"));
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

		isShowFolderLiveSubtitles = new JCheckBox(Messages.getString("ShowLiveSubtitlesFolder"), configuration.isShowLiveSubtitlesFolder());
		isShowFolderLiveSubtitles.setContentAreaFilled(false);
		isShowFolderLiveSubtitles.addItemListener((ItemEvent e) -> configuration.setShowLiveSubtitlesFolder((e.getStateChange() == ItemEvent.SELECTED)));

		prettifyFilenames = new JCheckBox(Messages.getString("PrettifyFilenames"), configuration.isPrettifyFilenames());
		prettifyFilenames.setToolTipText(Messages.getString("IfEnabledFilesWillAppear"));
		prettifyFilenames.setContentAreaFilled(false);
		prettifyFilenames.addItemListener((ItemEvent e) -> {
			configuration.setPrettifyFilenames((e.getStateChange() == ItemEvent.SELECTED));
			hideExtensions.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
		});

		isUseInfoFromAPI = new JCheckBox(Messages.getString("UseInfoFromOurApi"), configuration.isUseInfoFromIMDb());
		isUseInfoFromAPI.setToolTipText(Messages.getString("UsesInformationApiAllowBrowsing"));
		isUseInfoFromAPI.setContentAreaFilled(false);
		isUseInfoFromAPI.addItemListener((ItemEvent e) -> configuration.setUseInfoFromIMDb((e.getStateChange() == ItemEvent.SELECTED)));

		resume = new JCheckBox(Messages.getString("EnableVideoResuming"), configuration.isResumeEnabled());
		resume.setToolTipText(Messages.getString("WhenEnabledPartiallyWatchVideo"));
		resume.setContentAreaFilled(false);
		resume.addItemListener((ItemEvent e) -> configuration.setResume((e.getStateChange() == ItemEvent.SELECTED)));

		isShowFolderRecentlyPlayed = new JCheckBox(Messages.getString("ShowRecentlyPlayedFolder"), configuration.isShowRecentlyPlayedFolder());
		isShowFolderRecentlyPlayed.setContentAreaFilled(false);
		isShowFolderRecentlyPlayed.addItemListener((ItemEvent e) -> configuration.setShowRecentlyPlayedFolder((e.getStateChange() == ItemEvent.SELECTED)));

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
				Messages.getString("DoNothing"),
				Messages.getString("MarkMedia"),
				Messages.getString("HideMedia"),
				Messages.getString("MoveFileToDifferentFolder"),
				Messages.getString("MoveFileDifferentFolderMark"),
				Messages.getString("MoveFileRecycleTrashBin")
			}
		);
		fullyPlayedAction = new JComboBox<>(fullyPlayedActionModel);
		fullyPlayedAction.setEditable(false);
		fullyPlayedActionModel.setSelectedKey(configuration.getFullyPlayedAction());
		fullyPlayedAction.addItemListener((ItemEvent e) -> {
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
		selectFullyPlayedOutputDirectory.addActionListener((ActionEvent e) -> {
			JFileChooser chooser;
			try {
				chooser = new JFileChooser();
			} catch (Exception ee) {
				chooser = new JFileChooser(new RestrictedFileSystemView());
			}
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getString("ChooseAFolder"));
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				fullyPlayedOutputDirectory.setText(chooser.getSelectedFile().getAbsolutePath());
				configuration.setFullyPlayedOutputDirectory(chooser.getSelectedFile().getAbsolutePath());
			}
		});

		selectFullyPlayedOutputDirectory.setEnabled(
			configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER ||
			configuration.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER_AND_MARK
		);
	}

}
