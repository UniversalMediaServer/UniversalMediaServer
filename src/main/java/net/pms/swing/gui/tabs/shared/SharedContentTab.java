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
package net.pms.swing.gui.tabs.shared;

import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.FeedAudioContent;
import net.pms.configuration.sharedcontent.FeedContent;
import net.pms.configuration.sharedcontent.FeedImageContent;
import net.pms.configuration.sharedcontent.FeedVideoContent;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentArray;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.configuration.sharedcontent.SharedContentListener;
import net.pms.configuration.sharedcontent.StreamAudioContent;
import net.pms.configuration.sharedcontent.StreamContent;
import net.pms.configuration.sharedcontent.StreamVideoContent;
import net.pms.configuration.sharedcontent.VirtualFolderContent;
import net.pms.database.MediaDatabase;
import net.pms.store.MediaScanner;
import net.pms.store.MediaStatusStore;
import net.pms.store.container.Feed;
import net.pms.swing.SwingUtil;
import net.pms.swing.components.AnimatedIcon;
import net.pms.swing.components.JAnimatedButton;
import net.pms.swing.components.JImageButton;
import net.pms.swing.components.RestrictedFileSystemView;
import net.pms.swing.gui.FormLayoutUtil;
import net.pms.swing.gui.JavaGui;
import net.pms.swing.gui.UmsFormBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedContentTab implements SharedContentListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(SharedContentTab.class);
	private static final String BUTTON_ADD_FOLDER = "button-add-folder." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_ADD_WEBCONTENT = "button-add-webcontent." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_ARROW_DOWN = "button-arrow-down." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_ARROW_UP = "button-arrow-up." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_CANCEL = "button-cancel." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_CANCEL_PRESSED = "button-cancel_pressed." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_REMOVE_FOLDER = "button-remove-folder." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_SCAN = "button-scan." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_SCAN_BUSY = "button-scan-busy." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_SCAN_BUSY_DISABLED = "button-scan-busy_disabled." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final JAnimatedButton SCAN_BUTTON = new JAnimatedButton(BUTTON_SCAN);
	private static final AnimatedIcon SCAN_NORMAL_ICON = (AnimatedIcon) SCAN_BUTTON.getIcon();
	private static final AnimatedIcon SCAN_ROLLOVER_ICON = (AnimatedIcon) SCAN_BUTTON.getRolloverIcon();
	private static final AnimatedIcon SCAN_PRESSED_ICON = (AnimatedIcon) SCAN_BUTTON.getPressedIcon();
	private static final AnimatedIcon SCAN_DISABLED_ICON = (AnimatedIcon) SCAN_BUTTON.getDisabledIcon();
	private static final AnimatedIcon SCAN_BUSY_ICON = new AnimatedIcon(SCAN_BUTTON, BUTTON_SCAN_BUSY);
	private static final AnimatedIcon SCAN_BUSY_ROLLOVER_ICON = new AnimatedIcon(SCAN_BUTTON, BUTTON_CANCEL);
	private static final AnimatedIcon SCAN_BUSY_PRESSED_ICON = new AnimatedIcon(SCAN_BUTTON, BUTTON_CANCEL_PRESSED);
	private static final AnimatedIcon SCAN_BUSY_DISABLED_ICON = new AnimatedIcon(SCAN_BUTTON, BUTTON_SCAN_BUSY_DISABLED);
	private static final String PANEL_COL_SPEC = "left:pref, 50dlu, pref, 150dlu, pref, 25dlu, pref, 9dlu, pref, default:grow, pref, 25dlu";
	private static final String PANEL_ROW_SPEC = "fill:default:grow";
	private static final String SHARED_FOLDER_COL_SPEC = "left:pref, left:pref, pref, pref, pref, pref, 0:grow";
	private static final String SHARED_FOLDER_ROW_SPEC = "2*(p, 3dlu), fill:default:grow";

	private final UmsConfiguration configuration;
	private final JavaGui looksFrame;

	private String[] typesReadable;
	private String readableTypeFolder;
	private String readableTypeFolders;
	private String readableTypeAudioFeed;
	private String readableTypeVideoFeed;
	private String readableTypeImageFeed;
	private String readableTypeAudioStream;
	private String readableTypeVideoStream;

	private JTable sharedContentList;
	private SharedContentTableModel sharedContentTableModel;
	private SharedContentArray sharedContentArray;

	public SharedContentTab(UmsConfiguration configuration, JavaGui looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
		updateReadableTypes();
	}

	public JComponent build() {
		//update localized types
		updateReadableTypes();
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(PANEL_COL_SPEC, orientation);

		// Set basic layout
		FormLayout layout = new FormLayout(colSpec, PANEL_ROW_SPEC);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		// Init all gui components
		JPanel sharedContentPanel = initSharedContentGuiComponents(cc).build();

		// Load WEB.conf after we are sure the GUI has initialized
		SharedContentConfiguration.addListener(this);

		builder.add(sharedContentPanel).at(FormLayoutUtil.flip(cc.xyw(1, 1, 12), colSpec, orientation));

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

	private void updateReadableTypes() {
		typesReadable = new String[]{
			Messages.getGuiString("Folder"),
			Messages.getGuiString("VirtualFolders"),
			Messages.getGuiString("Podcast"),
			Messages.getGuiString("VideoFeed"),
			Messages.getGuiString("ImageFeed"),
			Messages.getGuiString("AudioStream"),
			Messages.getGuiString("VideoStream")
		};
		readableTypeFolder = typesReadable[0];
		readableTypeFolders = typesReadable[1];
		readableTypeAudioFeed = typesReadable[2];
		readableTypeVideoFeed = typesReadable[3];
		readableTypeImageFeed = typesReadable[4];
		readableTypeAudioStream = typesReadable[5];
		readableTypeVideoStream = typesReadable[6];
	}

	/**
	 * This parses the web sources config and populates the shared section of
	 * this tab.
	 */
	@Override
	public synchronized void updateSharedContent() {
		sharedContentArray = SharedContentConfiguration.getSharedContentArray();
		refreshSharedContent();
	}

	private UmsFormBuilder initSharedContentGuiComponents(CellConstraints cc) {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(SHARED_FOLDER_COL_SPEC, orientation);

		FormLayout layoutFolders = new FormLayout(colSpec, SHARED_FOLDER_ROW_SPEC);
		UmsFormBuilder builderFolder = UmsFormBuilder.create().layout(layoutFolders);
		builderFolder.opaque(true);

		builderFolder.addSeparator(Messages.getGuiString("SharedContent")).at(FormLayoutUtil.flip(cc.xyw(1, 1, 7), colSpec, orientation));

		sharedContentTableModel = new SharedContentTableModel();
		sharedContentList = new JTable(sharedContentTableModel);
		TableColumn column = sharedContentList.getColumnModel().getColumn(3);
		column.setMinWidth(500);

		sharedContentList.addMouseListener(new TableMouseListener(sharedContentList));
		addContentsFullyPlayedPopupMenu(sharedContentList);

		/*
		 * An attempt to set the correct row height adjusted for font scaling.
		 * It sets all rows based on the font size of cell (0, 0). The + 4 is
		 * to allow 2 pixels above and below the text.
		 */
		DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) sharedContentList.getCellRenderer(0, 0);
		FontMetrics metrics = cellRenderer.getFontMetrics(cellRenderer.getFont());
		sharedContentList.setRowHeight(metrics.getLeading() + metrics.getMaxAscent() + metrics.getMaxDescent() + 4);
		sharedContentList.setIntercellSpacing(new Dimension(8, 2));

		JImageButton addFolderButton = new JImageButton(BUTTON_ADD_FOLDER);
		addFolderButton.setToolTipText(Messages.getGuiString("AddFolder"));
		addFolderButton.addActionListener((ActionEvent e) -> {
			JFileChooser chooser;
			try {
				chooser = new JFileChooser();
				if (Platform.isWindows()) {
					chooser.setFileSystemView(new ShortcutFileSystemView());
				}
			} catch (Exception ee) {
				chooser = new JFileChooser(new RestrictedFileSystemView());
				LOGGER.debug("Using RestrictedFileSystemView because {}", ee.getMessage());
			}
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog((java.awt.Component) e.getSource());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				int firstSelectedRow = sharedContentList.getSelectedRow();
				if (firstSelectedRow >= 0) {
					sharedContentArray.add(firstSelectedRow, new FolderContent(chooser.getSelectedFile().getAbsoluteFile()));
				} else {
					sharedContentArray.add(new FolderContent(chooser.getSelectedFile().getAbsoluteFile()));
				}
				SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
			}
		});
		builderFolder.add(addFolderButton).at(FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		JImageButton addWebContentButton = new JImageButton(BUTTON_ADD_WEBCONTENT);
		addWebContentButton.setToolTipText(Messages.getGuiString("AddNewWebContent"));
		addWebContentButton.addActionListener((ActionEvent e) -> {
			JLabel labelType = new JLabel(Messages.getGuiString("TypeColon"));
			JLabel labelFolders = new JLabel(Messages.getGuiString("FoldersSlashDelimited"));
			JLabel labelName = new JLabel(Messages.getGuiString("NameColon"));
			JLabel labelSource = new JLabel(Messages.getGuiString("SourceURLColon"));

			JTextField newEntryFolders = new JTextField(25);
			newEntryFolders.setText("Web/");

			JTextField newEntrySource = new JTextField(50);

			JTextField newEntryName = new JTextField(25);
			newEntryName.setEnabled(false);
			newEntryName.setText(Messages.getGuiString("NamesSetAutomaticallyFeeds"));

			String[] typesWebContent = {
				typesReadable[2],
				typesReadable[3],
				typesReadable[4],
				typesReadable[5],
				typesReadable[6]
			};
			JComboBox<String> newEntryType = new JComboBox<>(typesWebContent);
			newEntryType.setEditable(false);
			newEntryType.addItemListener((ItemEvent e1) -> {
				if (readableTypeAudioFeed.equals(e1.getItem().toString()) ||
						readableTypeVideoFeed.equals(e1.getItem().toString()) ||
						readableTypeImageFeed.equals(e1.getItem().toString())) {
					newEntryName.setEnabled(false);
					newEntryName.setText(Messages.getGuiString("NamesSetAutomaticallyFeeds"));
				} else if (readableTypeAudioStream.equals(e1.getItem().toString()) ||
						readableTypeVideoStream.equals(e1.getItem().toString())) {
					newEntryName.setEnabled(true);
					newEntryName.setText("");
				}
			});

			JPanel addNewWebContentPanel = new JPanel();

			labelType.setLabelFor(newEntryType);
			labelFolders.setLabelFor(newEntryFolders);
			labelName.setLabelFor(newEntryName);
			labelSource.setLabelFor(newEntrySource);

			GroupLayout layout = new GroupLayout(addNewWebContentPanel);
			addNewWebContentPanel.setLayout(layout);

			layout.setHorizontalGroup(
					layout
							.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.addGroup(
													layout
															.createParallelGroup()
															.addComponent(labelType)
															.addComponent(newEntryType, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
															.addComponent(labelFolders)
															.addComponent(newEntryFolders, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
															.addComponent(labelName)
															.addComponent(newEntryName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
															.addComponent(labelSource)
															.addComponent(newEntrySource)
											)
											.addContainerGap()
							)
			);

			layout.setVerticalGroup(
					layout
							.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.addComponent(labelType)
											.addComponent(newEntryType)
											.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
											.addComponent(labelFolders)
											.addComponent(newEntryFolders)
											.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
											.addComponent(labelName)
											.addComponent(newEntryName)
											.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
											.addComponent(labelSource)
											.addComponent(newEntrySource)
											.addContainerGap()
							)
			);

			int result = JOptionPane.showConfirmDialog(null, addNewWebContentPanel, Messages.getGuiString("AddNewWebContent"), JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				sharedContentList.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				sharedContentList.setEnabled(false);
				try {
					String resourceName = null;
					if (!StringUtils.isBlank(newEntrySource.getText())) {
						try {
							if (
								readableTypeImageFeed.equals(newEntryType.getSelectedItem().toString()) ||
								readableTypeAudioFeed.equals(newEntryType.getSelectedItem().toString()) ||
								readableTypeVideoFeed.equals(newEntryType.getSelectedItem().toString())
							) {
								String uri = Feed.getFeedUrl(newEntrySource.getText());
								resourceName = Feed.getFeedTitle(uri);
							} else if (
								readableTypeVideoStream.equals(newEntryType.getSelectedItem().toString()) ||
								readableTypeAudioStream.equals(newEntryType.getSelectedItem().toString())
							) {
								resourceName = newEntryName.getText();
							}
						} catch (Exception e2) {
							LOGGER.debug("Error while getting feed title on add: " + e);
						}
					}
					String selectedItem = newEntryType.getSelectedItem().toString();
					if (selectedItem.equals(readableTypeAudioFeed)) {
						sharedContentArray.add(new FeedAudioContent(newEntryFolders.getText(), resourceName, newEntrySource.getText()));
					} else if (selectedItem.equals(readableTypeImageFeed)) {
						sharedContentArray.add(new FeedImageContent(newEntryFolders.getText(), resourceName, newEntrySource.getText()));
					} else if (selectedItem.equals(readableTypeVideoFeed)) {
						sharedContentArray.add(new FeedVideoContent(newEntryFolders.getText(), resourceName, newEntrySource.getText()));
					} else if (selectedItem.equals(readableTypeAudioStream)) {
						sharedContentArray.add(new StreamAudioContent(newEntryFolders.getText(), resourceName, newEntrySource.getText()));
					} else if (selectedItem.equals(readableTypeVideoStream)) {
						sharedContentArray.add(new StreamVideoContent(newEntryFolders.getText(), resourceName, newEntrySource.getText()));
					}
					refreshSharedContent();
					sharedContentList.changeSelection(((SharedContentTableModel) sharedContentList.getModel()).getRowCount() - 1, 1, false, false);
					SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
				} finally {
					sharedContentList.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					sharedContentList.setEnabled(true);
				}
			}
		});
		builderFolder.add(addWebContentButton).at(FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JImageButton removeButton = new JImageButton(BUTTON_REMOVE_FOLDER);
		removeButton.setToolTipText(Messages.getGuiString("RemoveSelectedSharedContent"));
		removeButton.addActionListener((ActionEvent e) -> {
			int currentlySelectedRow = sharedContentList.getSelectedRow();
			if (currentlySelectedRow > -1) {
				if (currentlySelectedRow > 0) {
					sharedContentList.changeSelection(currentlySelectedRow - 1, 1, false, false);
				}
				sharedContentArray.remove(currentlySelectedRow);
				SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
			}
		});
		builderFolder.add(removeButton).at(FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		JImageButton arrowDownButton = new JImageButton(BUTTON_ARROW_DOWN);
		arrowDownButton.setToolTipText(Messages.getGuiString("MoveSelectedContentDown"));
		arrowDownButton.addActionListener((ActionEvent e) -> {
			int index = sharedContentList.getSelectedRow();
			if (index < sharedContentArray.size()) {
				SharedContent sharedContent = sharedContentArray.remove(index);
				sharedContentArray.add(index + 1, sharedContent);
				sharedContentList.changeSelection(index + 1, 1, false, false);
				SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
			}
		});
		builderFolder.add(arrowDownButton).at(FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

		JImageButton arrowUpButton = new JImageButton(BUTTON_ARROW_UP);
		arrowUpButton.setToolTipText(Messages.getGuiString("MoveSelectedContentUp"));
		arrowUpButton.addActionListener((ActionEvent e) -> {
			int index = sharedContentList.getSelectedRow();
			if (index > 0) {
				SharedContent sharedContent = sharedContentArray.remove(index);
				sharedContentArray.add(index - 1, sharedContent);
				sharedContentList.changeSelection(index - 1, 1, false, false);
				SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
			}
		});
		builderFolder.add(arrowUpButton).at(FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));

		SCAN_BUTTON.setToolTipText(Messages.getGuiString("ScanAllSharedFolders"));
		SCAN_BUSY_ICON.start();
		SCAN_BUSY_DISABLED_ICON.start();
		SCAN_BUTTON.addActionListener((ActionEvent e) -> {
			if (MediaScanner.isMediaScanRunning()) {
				int option = JOptionPane.showConfirmDialog(looksFrame,
						Messages.getGuiString("DoYouWantStopScan"),
						Messages.getGuiString("Question"),
						JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION) {
					MediaScanner.stopMediaScan();
					looksFrame.setStatusLine(Messages.getGuiString("CancelingScan"));
					SCAN_BUTTON.setEnabled(false);
					SCAN_BUTTON.setToolTipText(Messages.getGuiString("CancelingScan"));
				}
			} else {
				MediaScanner.startMediaScan();
				SCAN_BUTTON.setIcon(SCAN_BUSY_ICON);
				SCAN_BUTTON.setRolloverIcon(SCAN_BUSY_ROLLOVER_ICON);
				SCAN_BUTTON.setPressedIcon(SCAN_BUSY_PRESSED_ICON);
				SCAN_BUTTON.setDisabledIcon(SCAN_BUSY_DISABLED_ICON);
				SCAN_BUTTON.setToolTipText(Messages.getGuiString("CancelScanningSharedFolders"));
			}
		});
		/*
		 * Hide the scan button in basic mode since it's better to let it be done in
		 * realtime.
		 */
		if (!configuration.isHideAdvancedOptions()) {
			builderFolder.add(SCAN_BUTTON).at(FormLayoutUtil.flip(cc.xy(6, 3), colSpec, orientation));
		}
		JCheckBox scanOnStartup = new JCheckBox(Messages.getGuiString("ScanSharedFoldersStartup"));
		scanOnStartup.setSelected(configuration.isScanSharedFoldersOnStartup());
		scanOnStartup.setContentAreaFilled(false);
		scanOnStartup.addItemListener((ItemEvent e) -> configuration.setScanSharedFoldersOnStartup((e.getStateChange() == ItemEvent.SELECTED)));
		scanOnStartup.setToolTipText(Messages.getGuiString("ThisControlsUmsScanShared"));
		builderFolder.add(scanOnStartup).at(FormLayoutUtil.flip(cc.xy(7, 3), colSpec, orientation));

		JScrollPane pane = new JScrollPane(sharedContentList);
		Dimension d = sharedContentList.getPreferredSize();
		pane.setPreferredSize(new Dimension(d.width, sharedContentList.getRowHeight() * 2));
		builderFolder.add(pane).at(FormLayoutUtil.flip(cc.xyw(1, 5, 7), colSpec, orientation));

		return builderFolder;
	}

	private synchronized void refreshSharedContent() {
		if (sharedContentList == null) {
			return;
		}
		int previouslySelectedRow = sharedContentList.getSelectedRow();
		sharedContentList.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		sharedContentList.setEnabled(false);

		try {
			// Remove any existing rows
			((SharedContentTableModel) sharedContentList.getModel()).setRowCount(0);
			for (SharedContent sharedContent : sharedContentArray) {
				if (sharedContent instanceof FolderContent folder && folder.getFile() != null) {
					sharedContentTableModel.addRow(new Object[]{readableTypeFolder, null, null, folder.getFile().getPath(), folder.isMonitored(), folder.isActive()});
				} else if (sharedContent instanceof VirtualFolderContent virtualFolder) {
					List<String> childs = new ArrayList<>();
					for (SharedContent child : virtualFolder.getChilds()) {
						if (child != null) {
							childs.add(child.toString());
						}
					}
					sharedContentTableModel.addRow(new Object[]{readableTypeFolders, virtualFolder.getParent(), virtualFolder.getName(), String.join(", ", childs), null, virtualFolder.isActive()});
				} else if (sharedContent instanceof StreamAudioContent streamAudio) {
					sharedContentTableModel.addRow(new Object[]{readableTypeAudioStream, streamAudio.getParent(), streamAudio.getName(), streamAudio.getUri(), null, streamAudio.isActive()});
				} else if (sharedContent instanceof StreamVideoContent streamVideo) {
					sharedContentTableModel.addRow(new Object[]{readableTypeVideoStream, streamVideo.getParent(), streamVideo.getName(), streamVideo.getUri(), null, streamVideo.isActive()});
				} else if (sharedContent instanceof FeedAudioContent feedAudio) {
					sharedContentTableModel.addRow(new Object[]{readableTypeAudioFeed, feedAudio.getParent(), feedAudio.getName(), feedAudio.getUri(), null, feedAudio.isActive()});
				} else if (sharedContent instanceof FeedImageContent feedImage) {
					sharedContentTableModel.addRow(new Object[]{readableTypeImageFeed, feedImage.getParent(), feedImage.getName(), feedImage.getUri(), null, feedImage.isActive()});
				} else if (sharedContent instanceof FeedVideoContent feedVideo) {
					sharedContentTableModel.addRow(new Object[]{readableTypeVideoFeed, feedVideo.getParent(), feedVideo.getName(), feedVideo.getUri(), false, feedVideo.isActive()});
				}
			}
			// Re-select any row that was selected before we (re)parsed the config
			if (previouslySelectedRow != -1) {
				sharedContentList.changeSelection(previouslySelectedRow, 1, false, false);
				Rectangle selectionToScrollTo = sharedContentList.getCellRect(previouslySelectedRow, 1, true);
				if (!selectionToScrollTo.isEmpty()) {
					sharedContentList.scrollRectToVisible(selectionToScrollTo);
				}
			}
		} finally {
			sharedContentList.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			sharedContentList.setEnabled(true);
		}
	}

	private void addContentsFullyPlayedPopupMenu(JComponent component) {
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItemMarkPlayed = new JMenuItem(Messages.getGuiString("MarkContentsFullyPlayed"));
		JMenuItem menuItemMarkUnplayed = new JMenuItem(Messages.getGuiString("MarkContentsUnplayed"));

		menuItemMarkPlayed.addActionListener((ActionEvent e) -> {
			int selectedIndex = sharedContentList.getSelectedRow();
			if (sharedContentArray.get(selectedIndex) instanceof FolderContent folderContent) {
				String path = folderContent.getFile().getAbsolutePath();
				Connection connection = null;
				try {
					connection = MediaDatabase.getConnectionIfAvailable();
					if (connection != null) {
						MediaStatusStore.setDirectoryFullyPlayed(connection, path, 0, true);
					}
				} finally {
					MediaDatabase.close(connection);
				}
			}
		});

		menuItemMarkUnplayed.addActionListener((ActionEvent e) -> {
			int selectedIndex = sharedContentList.getSelectedRow();
			if (sharedContentArray.get(selectedIndex) instanceof FolderContent folderContent) {
				String path = folderContent.getFile().getAbsolutePath();
				Connection connection = null;
				try {
					connection = MediaDatabase.getConnectionIfAvailable();
					if (connection != null) {
						MediaStatusStore.setDirectoryFullyPlayed(connection, path, 0, false);
					}
				} finally {
					MediaDatabase.close(connection);
				}
			}
		});

		popupMenu.add(menuItemMarkPlayed);
		popupMenu.add(menuItemMarkUnplayed);

		component.setComponentPopupMenu(popupMenu);
	}

	public void setMediaScanEnabled(boolean running) {
		SCAN_BUTTON.setIcon(running ? SCAN_BUSY_ICON : SCAN_NORMAL_ICON);
		SCAN_BUTTON.setRolloverIcon(running ? SCAN_BUSY_ROLLOVER_ICON : SCAN_ROLLOVER_ICON);
		SCAN_BUTTON.setPressedIcon(running ? SCAN_BUSY_PRESSED_ICON : SCAN_PRESSED_ICON);
		SCAN_BUTTON.setDisabledIcon(running ? SCAN_BUSY_DISABLED_ICON : SCAN_DISABLED_ICON);
		SCAN_BUTTON.setToolTipText(running ?
				Messages.getGuiString("CancelScanningSharedFolders") :
				Messages.getGuiString("ScanAllSharedFolders")
		);
	}

	private class SharedContentTableModel extends DefaultTableModel {

		private static final long serialVersionUID = -4247839506937958655L;

		public SharedContentTableModel() {
			// Column headings
			super(new String[]{
				Messages.getGuiString("Type"),
				Messages.getGuiString("Path"),
				Messages.getGuiString("Name"),
				Messages.getGuiString("Source"),
				Messages.getGuiString("Status"),
				Messages.getGuiString("Enable")
			}, 0);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 4 || columnIndex == 5 ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return isMonitoredCheckbox(row, column) || isActiveCheckbox(row, column);
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (isMonitoredCheckbox(row, column)) {
				//Monitored
				((FolderContent) sharedContentArray.get(row)).setMonitored((Boolean) aValue);
				SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
			} else if (isActiveCheckbox(row, column)) {
				//Active
				sharedContentArray.get(row).setActive((Boolean) aValue);
				SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
			}
		}

		private boolean isMonitoredCheckbox(int row, int column) {
			return (column == 4 && sharedContentArray != null && sharedContentArray.get(row) instanceof FolderContent);
		}

		private boolean isActiveCheckbox(int row, int column) {
			return (column == 5 && sharedContentArray != null && sharedContentArray.get(row) != null);
		}
	}

	private class TableMouseListener extends MouseAdapter {

		private final JTable table;

		public TableMouseListener(JTable table) {
			this.table = table;
		}

		@Override
		public void mousePressed(MouseEvent event) {
			// selects the row at which point the mouse is clicked
			Point point = event.getPoint();
			int currentRow = table.rowAtPoint(point);
			table.setRowSelectionInterval(currentRow, currentRow);

			//let check the checkbox
			int currentColumn = table.columnAtPoint(point);
			if (currentColumn == 4 || currentColumn == 5) {
				return;
			}
			// more than one click in the same event triggers edit mode
			if (event.getClickCount() == 2) {
				SharedContent sharedContent = sharedContentArray.get(currentRow);
				String currentType = (String) sharedContentList.getValueAt(currentRow, 0);
				String currentFolders = (String) sharedContentList.getValueAt(currentRow, 1);
				String currentName = (String) sharedContentList.getValueAt(currentRow, 2);
				String currentSource = (String) sharedContentList.getValueAt(currentRow, 3);

				if (sharedContent instanceof FolderContent folder) {
					JFileChooser chooser;
					try {
						chooser = new JFileChooser();
						chooser.setSelectedFile(folder.getFile());
						if (Platform.isWindows()) {
							chooser.setFileSystemView(new ShortcutFileSystemView());
						}
					} catch (Exception ee) {
						chooser = new JFileChooser(new RestrictedFileSystemView());
						LOGGER.debug("Using RestrictedFileSystemView because {}", ee.getMessage());
					}
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int returnVal = chooser.showOpenDialog((java.awt.Component) event.getSource());
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						folder.setFile(chooser.getSelectedFile().getAbsoluteFile());
						SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
					}
					return;
				}

				int currentTypeIndex = Arrays.asList(typesReadable).indexOf(currentType);

				JTextField newEntryName = new JTextField(25);
				if (readableTypeAudioFeed.equals(currentType) ||
						readableTypeVideoFeed.equals(currentType) ||
						readableTypeImageFeed.equals(currentType)) {
					newEntryName.setEnabled(false);
					if (!StringUtils.isBlank(currentName)) {
						newEntryName.setText(currentName);
					} else {
						newEntryName.setText(Messages.getGuiString("NamesSetAutomaticallyFeeds"));
					}
				} else {
					newEntryName.setEnabled(true);
					newEntryName.setText(currentName);
				}

				JTextField newEntryFolders = new JTextField(25);
				newEntryFolders.setText(currentFolders);

				JTextField newEntrySource = new JTextField(50);
				newEntrySource.setText(currentSource);
				if (readableTypeFolders.equals(currentType)) {
					newEntrySource.setEnabled(false);
				}

				JComboBox<String> newEntryType = new JComboBox<>(typesReadable);
				newEntryType.setEditable(false);
				newEntryType.setSelectedIndex(currentTypeIndex);
				newEntryType.setEnabled(false);

				JPanel addNewSharedContentPanel = new JPanel();

				JLabel labelType = new JLabel(Messages.getGuiString("TypeColon"));
				JLabel labelFolders = new JLabel(Messages.getGuiString("FoldersSlashDelimited"));
				JLabel labelName = new JLabel(Messages.getGuiString("NameColon"));
				JLabel labelSource = new JLabel(Messages.getGuiString("SourceURLColon"));

				labelName.setLabelFor(newEntryName);
				labelType.setLabelFor(newEntryType);
				labelFolders.setLabelFor(newEntryFolders);
				labelSource.setLabelFor(newEntrySource);

				GroupLayout layout = new GroupLayout(addNewSharedContentPanel);
				addNewSharedContentPanel.setLayout(layout);

				layout.setHorizontalGroup(
						layout
								.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addGroup(
										layout
												.createSequentialGroup()
												.addContainerGap()
												.addGroup(
														layout
																.createParallelGroup()
																.addComponent(labelType)
																.addComponent(newEntryType, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
																.addComponent(labelFolders)
																.addComponent(newEntryFolders, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
																.addComponent(labelName)
																.addComponent(newEntryName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
																.addComponent(labelSource)
																.addComponent(newEntrySource)
												)
												.addContainerGap()
								)
				);

				layout.setVerticalGroup(
						layout
								.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addGroup(
										layout
												.createSequentialGroup()
												.addContainerGap()
												.addComponent(labelType)
												.addComponent(newEntryType)
												.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
												.addComponent(labelFolders)
												.addComponent(newEntryFolders)
												.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
												.addComponent(labelName)
												.addComponent(newEntryName)
												.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
												.addComponent(labelSource)
												.addComponent(newEntrySource)
												.addContainerGap()
								)
				);

				int result = JOptionPane.showConfirmDialog(null, addNewSharedContentPanel, Messages.getGuiString("AddNewWebContent"), JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION) {
					if (sharedContent instanceof VirtualFolderContent virtualFolder) {
						virtualFolder.setName(newEntryName.getText());
						virtualFolder.setParent(newEntryFolders.getText());
					} else if (sharedContent instanceof StreamContent stream) {
						stream.setName(newEntryName.getText());
						stream.setParent(newEntryFolders.getText());
						stream.setUri(newEntrySource.getText());
					} else if (sharedContent instanceof FeedContent feed) {
						feed.setName(newEntryName.getText());
						feed.setParent(newEntryFolders.getText());
						feed.setUri(newEntrySource.getText());
					}
					SharedContentConfiguration.updateSharedContent(sharedContentArray, true);
				}
			}
		}
	}

}
