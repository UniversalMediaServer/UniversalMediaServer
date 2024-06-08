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
package net.pms.swing.gui.tabs.navigation;

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
import net.pms.store.utils.StoreResourceSorter;
import net.pms.swing.SwingUtil;
import net.pms.swing.components.CustomJButton;
import net.pms.swing.components.KeyedComboBoxModel;
import net.pms.swing.components.RestrictedFileSystemView;
import net.pms.swing.gui.FormLayoutUtil;
import net.pms.swing.gui.JavaGui;
import net.pms.swing.gui.UmsFormBuilder;
import net.pms.util.CoverSupplier;
import net.pms.util.FullyPlayedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationShareTab {

	private static final Logger LOGGER = LoggerFactory.getLogger(NavigationShareTab.class);

	private final UmsConfiguration configuration;
	private final JavaGui looksFrame;

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
	private JCheckBox resume;
	private JCheckBox useSymlinksTargetFile;
	private JComboBox<String> fullyPlayedAction;
	private JTextField fullyPlayedOutputDirectory;
	private CustomJButton selectFullyPlayedOutputDirectory;
	private JTextField chapterInterval;
	private JComboBox<String> addVideoSuffix;

	// Settings for the visibility of virtual folders
	private JCheckBox isShowFolderServerSettings;
	private JCheckBox isShowFolderTranscode;
	private JCheckBox isChapterSupport;
	private JCheckBox isShowFolderMediaLibrary;
	private JCheckBox isShowFolderRecentlyPlayed;
	private JCheckBox isShowFolderLiveSubtitles;
	private JComponent component;

	public NavigationShareTab(UmsConfiguration configuration, JavaGui looksFrame) {
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
			builder.addSeparator(Messages.getGuiString("Thumbnails")).at(FormLayoutUtil.flip(cc.xyw(1, 1, 10), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(generateThumbnails)).at(FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));
			builder.addLabel(Messages.getGuiString("ThumbnailSeekingPosition")).at(FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));
			builder.add(seekPosition).at(FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(imageThumbnails)).at(FormLayoutUtil.flip(cc.xy(7, 3), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("AudioThumbnailsImport")).at(FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));
			builder.add(audioThumbnails).at(FormLayoutUtil.flip(cc.xyw(3, 5, 3), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(mplayerThumbnails)).at(FormLayoutUtil.flip(cc.xy(7, 5), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("AlternateVideoCoverArtFolder")).at(FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));
			builder.add(defaultThumbFolder).at(FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));
			builder.add(select).at(FormLayoutUtil.flip(cc.xy(5, 7), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(dvdIsoThumbnails)).at(FormLayoutUtil.flip(cc.xy(7, 7), colSpec, orientation));

			builder.addSeparator(Messages.getGuiString("FileSortingNaming")).at(FormLayoutUtil.flip(cc.xyw(1, 9, 10), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("FileOrder")).at(FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));
			builder.add(sortMethod).at(FormLayoutUtil.flip(cc.xyw(3, 11, 3), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(ignoreTheWordThe)).at(FormLayoutUtil.flip(cc.xy(7, 11), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(prettifyFilenames)).at(FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(hideExtensions)).at(FormLayoutUtil.flip(cc.xy(3, 13), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("AddSubtitlesInformationVideoNames")).at(FormLayoutUtil.flip(cc.xy(1, 15), colSpec, orientation));
			builder.add(addVideoSuffix).at(FormLayoutUtil.flip(cc.xyw(3, 15, 3), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(hideEngines)).at(FormLayoutUtil.flip(cc.xy(7, 15), colSpec, orientation));

			builder.addSeparator(Messages.getGuiString("VirtualFoldersFiles")).at(FormLayoutUtil.flip(cc.xyw(1, 17, 10), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(iTunes)).at(FormLayoutUtil.flip(cc.xy(1, 19), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(iPhoto)).at(FormLayoutUtil.flip(cc.xy(3, 19), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(aperture)).at(FormLayoutUtil.flip(cc.xy(7, 19), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("DatabaseCache")).at(FormLayoutUtil.flip(cc.xy(1, 21), colSpec, orientation));
			builder.add(cacheReset).at(FormLayoutUtil.flip(cc.xyw(3, 21, 3), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(isShowFolderMediaLibrary)).at(FormLayoutUtil.flip(cc.xy(7, 21), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(archive)).at(FormLayoutUtil.flip(cc.xy(1, 23), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(isShowFolderServerSettings)).at(FormLayoutUtil.flip(cc.xy(3, 23), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(isShowFolderTranscode)).at(FormLayoutUtil.flip(cc.xy(1, 25), colSpec, orientation));
			builder.add(isChapterSupport).at(FormLayoutUtil.flip(cc.xy(3, 25), colSpec, orientation));
			builder.add(chapterInterval).at(FormLayoutUtil.flip(cc.xy(7, 25), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(isShowFolderLiveSubtitles)).at(FormLayoutUtil.flip(cc.xy(1, 27), colSpec, orientation));
			builder.addLabel(Messages.getGuiString("MinimumItemLimitBeforeAZ")).at(FormLayoutUtil.flip(cc.xy(3, 27), colSpec, orientation));
			builder.add(atzLimit).at(FormLayoutUtil.flip(cc.xy(5, 27), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(resume)).at(FormLayoutUtil.flip(cc.xy(1, 29), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(isShowFolderRecentlyPlayed)).at(FormLayoutUtil.flip(cc.xy(3, 29), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(hideEmptyFolders)).at(FormLayoutUtil.flip(cc.xy(7, 29), colSpec, orientation));

			builder.add(SwingUtil.getPreferredSizeComponent(useSymlinksTargetFile)).at(FormLayoutUtil.flip(cc.xy(1, 31), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("FullyPlayedAction")).at(FormLayoutUtil.flip(cc.xy(1, 33), colSpec, orientation));
			builder.add(fullyPlayedAction).at(FormLayoutUtil.flip(cc.xyw(3, 33, 3), colSpec, orientation));
			builder.add(fullyPlayedOutputDirectory).at(FormLayoutUtil.flip(cc.xy(7, 33), colSpec, orientation));
			builder.add(selectFullyPlayedOutputDirectory).at(FormLayoutUtil.flip(cc.xy(9, 33), colSpec, orientation));
		}

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		component = new JScrollPane(
			panel,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		component.setBorder(BorderFactory.createEmptyBorder());
		return component;
	}

	public Component getComponent() {
		return component;
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
		generateThumbnails = new JCheckBox(Messages.getGuiString("GenerateThumbnails"), configuration.isThumbnailGenerationEnabled());
		generateThumbnails.setContentAreaFilled(false);
		generateThumbnails.addItemListener((ItemEvent e) -> {
			configuration.setThumbnailGenerationEnabled((e.getStateChange() == ItemEvent.SELECTED));
			seekPosition.setEnabled(configuration.isThumbnailGenerationEnabled());
			mplayerThumbnails.setEnabled(configuration.isThumbnailGenerationEnabled());
		});

		// Use MPlayer for video thumbnails
		mplayerThumbnails = new JCheckBox(Messages.getGuiString("UseMplayerVideoThumbnails"), configuration.isUseMplayerForVideoThumbs());
		mplayerThumbnails.setToolTipText(Messages.getGuiString("WhenSettingDisabledFfmpeg"));
		mplayerThumbnails.setContentAreaFilled(false);
		mplayerThumbnails.addItemListener((ItemEvent e) -> configuration.setUseMplayerForVideoThumbs((e.getStateChange() == ItemEvent.SELECTED)));
		mplayerThumbnails.setEnabled(configuration.isThumbnailGenerationEnabled());

		// DVD ISO thumbnails
		dvdIsoThumbnails = new JCheckBox(Messages.getGuiString("DvdIsoThumbnails"), configuration.isDvdIsoThumbnails());
		dvdIsoThumbnails.setContentAreaFilled(false);
		dvdIsoThumbnails.addItemListener((ItemEvent e) -> configuration.setDvdIsoThumbnails((e.getStateChange() == ItemEvent.SELECTED)));

		// Image thumbnails
		imageThumbnails = new JCheckBox(Messages.getGuiString("ImageThumbnails"), configuration.getImageThumbnailsEnabled());
		imageThumbnails.setContentAreaFilled(false);
		imageThumbnails.addItemListener((ItemEvent e) -> configuration.setImageThumbnailsEnabled((e.getStateChange() == ItemEvent.SELECTED)));

		// Audio thumbnails import
		final KeyedComboBoxModel<CoverSupplier, String> thumbKCBM = new KeyedComboBoxModel<>(
			new CoverSupplier[]{CoverSupplier.NONE, CoverSupplier.COVER_ART_ARCHIVE},
			new String[]{Messages.getGuiString("None"), Messages.getGuiString("DownloadFromCoverArtArchive")}
		);
		audioThumbnails = new JComboBox<>(thumbKCBM);
		audioThumbnails.setEditable(false);

		thumbKCBM.setSelectedKey(configuration.getAudioThumbnailMethod());

		audioThumbnails.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setAudioThumbnailMethod(thumbKCBM.getSelectedKey());
				LOGGER.info("Setting Audio thumbnails import: {}", thumbKCBM.getSelectedValue());
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
			int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getGuiString("ChooseAFolder"));
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				defaultThumbFolder.setText(chooser.getSelectedFile().getAbsolutePath());
				configuration.setAlternateThumbFolder(chooser.getSelectedFile().getAbsolutePath());
			}
		});

		// Show Server Settings folder
		isShowFolderServerSettings = new JCheckBox(Messages.getGuiString("ShowServerSettingsFolder"), configuration.isShowServerSettingsFolder());
		isShowFolderServerSettings.setToolTipText(Messages.getGuiString("WarningThisAllowsShutdownComputer"));
		isShowFolderServerSettings.setContentAreaFilled(false);
		isShowFolderServerSettings.addItemListener((ItemEvent e) -> configuration.setShowServerSettingsFolder((e.getStateChange() == ItemEvent.SELECTED)));

		// Show #--TRANSCODE--# folder
		isShowFolderTranscode = new JCheckBox(Messages.getGuiString("ShowTranscodeFolder"), configuration.isShowTranscodeFolder());
		isShowFolderTranscode.setContentAreaFilled(false);
		isShowFolderTranscode.addItemListener((ItemEvent e) -> {
			configuration.setShowTranscodeFolder((e.getStateChange() == ItemEvent.SELECTED));
			isChapterSupport.setEnabled(configuration.isShowTranscodeFolder());
		});

		// Chapters support in #--TRANSCODE--# folder
		isChapterSupport = new JCheckBox(Messages.getGuiString("ChaptersSupportInTranscodeFolder"), configuration.isChapterSupport());
		isChapterSupport.setEnabled(configuration.isShowTranscodeFolder());
		isChapterSupport.setContentAreaFilled(false);
		isChapterSupport.addItemListener((ItemEvent e) -> {
			configuration.setChapterSupport((e.getStateChange() == ItemEvent.SELECTED));
			chapterInterval.setEnabled(configuration.isShowTranscodeFolder() && configuration.isChapterSupport());
		});

		// Chapters interval in #--TRANSCODE--# folder
		chapterInterval = new JTextField("" + configuration.getChapterInterval());
		chapterInterval.setEnabled(configuration.isChapterSupport());
		chapterInterval.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(chapterInterval.getText());
					configuration.setChapterInterval(ab);
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse chapter interval from \"" + chapterInterval.getText() + "\"");
				}
			}
		});

		// Show Media Library folder
		isShowFolderMediaLibrary = new JCheckBox(Messages.getGuiString("ShowMediaLibraryFolder"), configuration.isShowMediaLibraryFolder());
		isShowFolderMediaLibrary.setContentAreaFilled(false);
		isShowFolderMediaLibrary.addItemListener((ItemEvent e) -> configuration.setShowMediaLibraryFolder((e.getStateChange() == ItemEvent.SELECTED)));
		isShowFolderMediaLibrary.setToolTipText(Messages.getGuiString("MediaLibraryFolderWillAvailable"));

		// Browse compressed archives
		archive = new JCheckBox(Messages.getGuiString("BrowseCompressedArchives"), configuration.isArchiveBrowsing());
		archive.setContentAreaFilled(false);
		archive.addItemListener((ItemEvent e) -> configuration.setArchiveBrowsing(e.getStateChange() == ItemEvent.SELECTED));

		// Reset cache
		cacheReset = new CustomJButton(Messages.getGuiString("ResetCache"));
		cacheReset.setToolTipText(Messages.getGuiString("CacheEmptiedExceptFullyPlayed"));
		cacheReset.addActionListener((ActionEvent e) -> {
			int option = JOptionPane.showConfirmDialog(looksFrame,
				Messages.getGuiString("CacheEmptiedExceptFullyPlayed") + "\n" + Messages.getGuiString("AreYouSure"),
				Messages.getGuiString("Question"),
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
		hideExtensions = new JCheckBox(Messages.getGuiString("HideFileExtensions"), configuration.isHideExtensions());
		hideExtensions.setContentAreaFilled(false);
		if (configuration.isPrettifyFilenames()) {
			hideExtensions.setEnabled(false);
		}
		hideExtensions.addItemListener((ItemEvent e) -> configuration.setHideExtensions((e.getStateChange() == ItemEvent.SELECTED)));

		// Hide transcoding engine names
		hideEngines = new JCheckBox(Messages.getGuiString("AddEnginesNamesAfterFilenames"), !configuration.isHideEngineNames());
		hideEngines.setToolTipText(Messages.getGuiString("IfEnabledEngineNameDisplayed"));
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
				Messages.getGuiString("None"),
				Messages.getGuiString("Basic"),
				Messages.getGuiString("Full")
			}
		);

		addVideoSuffix = new JComboBox<>(videoSuffixKCBM);
		addVideoSuffix.setEditable(false);
		addVideoSuffix.setToolTipText(Messages.getGuiString("AddsInformationAboutSelectedSubtitles"));

		videoSuffixKCBM.setSelectedKey(configuration.getSubtitlesInfoLevel());

		addVideoSuffix.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				LOGGER.debug(
					"Setting Subtitles Info Level to \"{}\"",
					videoSuffixKCBM.getSelectedValue()
				);
				configuration.setSubtitlesInfoLevel(videoSuffixKCBM.getSelectedKey());
			}
		});

		// Hide empty folders
		hideEmptyFolders = new JCheckBox(Messages.getGuiString("HideEmptyFolders"), configuration.isHideEmptyFolders());
		hideEmptyFolders.setToolTipText(Messages.getGuiString("ThisMakesBrowsingSlower"));
		hideEmptyFolders.setContentAreaFilled(false);
		hideEmptyFolders.addItemListener((ItemEvent e) -> configuration.setHideEmptyFolders((e.getStateChange() == ItemEvent.SELECTED)));

		// Use target file for symlinks
		useSymlinksTargetFile = new JCheckBox(Messages.getGuiString("UseTargetFileSymbolicLinks"), configuration.isUseSymlinksTargetFile());
		useSymlinksTargetFile.setToolTipText(Messages.getGuiString("TreatMultipleSymbolicLinks"));
		useSymlinksTargetFile.setContentAreaFilled(false);
		useSymlinksTargetFile.addItemListener((ItemEvent e) -> configuration.setUseSymlinksTargetFile((e.getStateChange() == ItemEvent.SELECTED)));

		// Show iTunes library
		iTunes = new JCheckBox(Messages.getGuiString("ShowItunesLibrary"), false);
		iTunes.setToolTipText(Messages.getGuiString("IfEnabledThreeNewVirtual"));
		iTunes.setContentAreaFilled(false);
		iTunes.setEnabled(false);

		// Show iPhoto library
		iPhoto = new JCheckBox(Messages.getGuiString("ShowIphotoLibrary"), false);
		iPhoto.setContentAreaFilled(false);
		iPhoto.setEnabled(false);

		// Show aperture library
		aperture = new JCheckBox(Messages.getGuiString("ShowApertureLibrary"), false);
		aperture.setContentAreaFilled(false);
		aperture.setEnabled(false);

		// File order
		final KeyedComboBoxModel<Integer, String> kcbm = new KeyedComboBoxModel<>(
			new Integer[]{
				StoreResourceSorter.SORT_TITLE_ASC,     // title ascending
				StoreResourceSorter.SORT_DATE_MOD_DESC, // modified date newest first
				StoreResourceSorter.SORT_DATE_MOD_ASC,  // modified date oldest first
				StoreResourceSorter.SORT_RANDOM,        // random
				StoreResourceSorter.SORT_NO_SORT        // no sorting
			},
			new String[]{
				Messages.getGuiString("ByDisplayName"),
				Messages.getGuiString("ByDateNewestFirst"),
				Messages.getGuiString("ByDateOldestFirst"),
				Messages.getGuiString("Random"),
				Messages.getGuiString("NoSorting")
			}
		);
		sortMethod = new JComboBox<>(kcbm);
		sortMethod.setEditable(false);
		kcbm.setSelectedKey(configuration.getSortMethod());

		sortMethod.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setSortMethod(kcbm.getSelectedKey());
				LOGGER.info("Setting File Order to: {}", kcbm.getSelectedValue());
			}
		});

		// Ignore the word "the" while sorting
		ignoreTheWordThe = new JCheckBox(Messages.getGuiString("IgnoreArticlesATheSorting"), configuration.isIgnoreTheWordAandThe());
		ignoreTheWordThe.setToolTipText(Messages.getGuiString("IfEnabledFilesWillOrdered"));
		ignoreTheWordThe.setContentAreaFilled(false);
		ignoreTheWordThe.addItemListener((ItemEvent e) -> configuration.setIgnoreTheWordAandThe((e.getStateChange() == ItemEvent.SELECTED)));

		atzLimit = new JTextField("" + configuration.getATZLimit());
		atzLimit.setToolTipText(Messages.getGuiString("IfNumberItemsFolderExceeds"));
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

		isShowFolderLiveSubtitles = new JCheckBox(Messages.getGuiString("ShowLiveSubtitlesFolder"), configuration.isShowLiveSubtitlesFolder());
		isShowFolderLiveSubtitles.setContentAreaFilled(false);
		isShowFolderLiveSubtitles.addItemListener((ItemEvent e) -> configuration.setShowLiveSubtitlesFolder((e.getStateChange() == ItemEvent.SELECTED)));

		prettifyFilenames = new JCheckBox(Messages.getGuiString("PrettifyFilenames"), configuration.isPrettifyFilenames());
		prettifyFilenames.setToolTipText(Messages.getGuiString("IfEnabledFilesWillAppear"));
		prettifyFilenames.setContentAreaFilled(false);
		prettifyFilenames.addItemListener((ItemEvent e) -> {
			configuration.setPrettifyFilenames((e.getStateChange() == ItemEvent.SELECTED));
			hideExtensions.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
		});

		resume = new JCheckBox(Messages.getGuiString("EnableVideoResuming"), configuration.isResumeEnabled());
		resume.setToolTipText(Messages.getGuiString("WhenEnabledPartiallyWatchVideo"));
		resume.setContentAreaFilled(false);
		resume.addItemListener((ItemEvent e) -> configuration.setResume((e.getStateChange() == ItemEvent.SELECTED)));

		isShowFolderRecentlyPlayed = new JCheckBox(Messages.getGuiString("ShowRecentlyPlayedFolder"), configuration.isShowRecentlyPlayedFolder());
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
				Messages.getGuiString("DoNothing"),
				Messages.getGuiString("MarkMedia"),
				Messages.getGuiString("HideMedia"),
				Messages.getGuiString("MoveFileToDifferentFolder"),
				Messages.getGuiString("MoveFileDifferentFolderMark"),
				Messages.getGuiString("MoveFileRecycleTrashBin")
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
			int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getGuiString("ChooseAFolder"));
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
