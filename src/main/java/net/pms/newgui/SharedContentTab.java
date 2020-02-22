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
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.database.TableFilesStatus;
import net.pms.dlna.DLNAMediaDatabase;
import static net.pms.dlna.RootFolder.parseFeedKey;
import static net.pms.dlna.RootFolder.parseFeedValue;
import net.pms.newgui.components.AnimatedIcon;
import net.pms.newgui.components.JAnimatedButton;
import net.pms.newgui.components.JImageButton;
import net.pms.util.FormLayoutUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedContentTab {
	private static final Vector<String> FOLDERS_COLUMN_NAMES = new Vector<>(
		Arrays.asList(new String[] {Messages.getString("Generic.Folder"), Messages.getString("FoldTab.65")})
	);
	public static final String ALL_DRIVES = Messages.getString("FoldTab.0");
	private static final Logger LOGGER = LoggerFactory.getLogger(SharedContentTab.class);

	private JPanel sharedPanel;
	private JPanel sharedFoldersPanel;
	private JPanel sharedWebContentPanel;
	private JTable sharedFolders;
	public static JTable webContentList;
	private SharedFoldersTableModel folderTableModel;
	public static WebContentTableModel webContentTableModel;
	public static JCheckBox itunes;
	private JCheckBox isScanSharedFoldersOnStartup;
	private static final JAnimatedButton scanButton = new JAnimatedButton("button-scan.png");
	private static final AnimatedIcon scanNormalIcon = (AnimatedIcon) scanButton.getIcon();
	private static final AnimatedIcon scanRolloverIcon = (AnimatedIcon) scanButton.getRolloverIcon();
	private static final AnimatedIcon scanPressedIcon = (AnimatedIcon) scanButton.getPressedIcon();
	private static final AnimatedIcon scanDisabledIcon = (AnimatedIcon) scanButton.getDisabledIcon();
	private static final AnimatedIcon scanBusyIcon = new AnimatedIcon(scanButton, "button-scan-busy.png");
	private static final AnimatedIcon scanBusyRolloverIcon = new AnimatedIcon(scanButton, "button-cancel.png");
	private static final AnimatedIcon scanBusyPressedIcon = new AnimatedIcon(scanButton, "button-cancel_pressed.png");
	private static final AnimatedIcon scanBusyDisabledIcon = new AnimatedIcon(scanButton, "button-scan-busy_disabled.png");
	private static final JImageButton addButton = new JImageButton("button-add-folder.png");
	private static final JImageButton removeButton = new JImageButton("button-remove-folder.png");
	private static final JImageButton arrowDownButton = new JImageButton("button-arrow-down.png");
	private static final JImageButton arrowUpButton = new JImageButton("button-arrow-up.png");

	private static final String[] TYPES_READABLE = new String[]{
		Messages.getString("SharedContentTab.AudioFeed"),
		Messages.getString("SharedContentTab.VideoFeed"),
		Messages.getString("SharedContentTab.ImageFeed"),
		Messages.getString("SharedContentTab.AudioStream"),
		Messages.getString("SharedContentTab.VideoStream"),
	};

	public SharedFoldersTableModel getDf() {
		return folderTableModel;
	}

	private final PmsConfiguration configuration;
	private LooksFrame looksFrame;

	SharedContentTab(PmsConfiguration configuration, LooksFrame looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
	}

	private void updateWebContentModel() {
		if (webContentTableModel.getRowCount() == 0) {
			configuration.writeWebConfigurationFile();
		} else {
			List<String> entries = new ArrayList<>();

			for (int i = 0; i < webContentTableModel.getRowCount(); i++) {
				String readableType = (String) webContentTableModel.getValueAt(i, 0);
				String folders = (String) webContentTableModel.getValueAt(i, 1);
				String configType;

				String readableTypeImageFeed   = TYPES_READABLE[2];
				String readableTypeVideoFeed   = TYPES_READABLE[1];
				String readableTypeAudioFeed   = TYPES_READABLE[0];
				String readableTypeAudioStream = TYPES_READABLE[3];
				String readableTypeVideoStream = TYPES_READABLE[4];

				if (readableType.equals(readableTypeImageFeed)) {
					configType = "imagefeed";
				} else if (readableType.equals(readableTypeVideoFeed)) {
					configType = "videofeed";
				} else if (readableType.equals(readableTypeAudioFeed)) {
					configType = "audiofeed";
				} else if (readableType.equals(readableTypeAudioStream)) {
					configType = "audiostream";
				} else if (readableType.equals(readableTypeVideoStream)) {
					configType = "videostream";
				} else {
					// Skip the whole row if another value was used
					continue;
				}

				String source = (String) webContentTableModel.getValueAt(i, 2);

				StringBuilder entryToAdd = new StringBuilder();
				entryToAdd.append(configType).append(".").append(folders).append("=").append(source);
				entries.add(entryToAdd.toString());
			}

			configuration.writeWebConfigurationFile(entries);
		}
	}

	private static final String PANEL_COL_SPEC = "left:pref,          50dlu,                pref, 150dlu,                       pref, 25dlu,               pref, 9dlu, pref, default:grow, pref, 25dlu";
	private static final String PANEL_ROW_SPEC = "fill:default:grow, 9dlu, fill:default:grow";
	private static final String SHARED_FOLDER_COL_SPEC = "left:pref, left:pref, pref, pref, pref, pref, 0:grow";
	private static final String SHARED_FOLDER_ROW_SPEC = "2*(p, 3dlu), fill:default:grow";

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
		sharedFoldersPanel = initSharedFoldersGuiComponents(cc).build();
		sharedWebContentPanel = initWebContentGuiComponents(cc).build();

		// Load WEB.conf after we are sure the GUI has initialized
		String webConfPath = configuration.getWebConfPath();
		File webConf = new File(webConfPath);
		if (webConf.exists() && configuration.getExternalNetwork()) {
			parseWebConf(webConf);
		}

		builder.add(sharedFoldersPanel,    FormLayoutUtil.flip(cc.xyw(1, 1, 12), colSpec, orientation));
		builder.add(sharedWebContentPanel, FormLayoutUtil.flip(cc.xyw(1, 3, 12), colSpec, orientation));

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

	private PanelBuilder initSharedFoldersGuiComponents(CellConstraints cc) {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(SHARED_FOLDER_COL_SPEC, orientation);

		FormLayout layoutFolders = new FormLayout(colSpec, SHARED_FOLDER_ROW_SPEC);
		PanelBuilder builderFolder = new PanelBuilder(layoutFolders);
		builderFolder.opaque(true);

		JComponent cmp = builderFolder.addSeparator(Messages.getString("FoldTab.7"), FormLayoutUtil.flip(cc.xyw(1, 1, 7), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		folderTableModel = new SharedFoldersTableModel();
		sharedFolders = new JTable(folderTableModel);

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItemMarkPlayed = new JMenuItem(Messages.getString("FoldTab.75"));
		JMenuItem menuItemMarkUnplayed = new JMenuItem(Messages.getString("FoldTab.76"));

		menuItemMarkPlayed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = (String) sharedFolders.getValueAt(sharedFolders.getSelectedRow(), 0);
				TableFilesStatus.setDirectoryFullyPlayed(path, true);
			}
		});

		menuItemMarkUnplayed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = (String) sharedFolders.getValueAt(sharedFolders.getSelectedRow(), 0);
				TableFilesStatus.setDirectoryFullyPlayed(path, false);
			}
		});

		popupMenu.add(menuItemMarkPlayed);
		popupMenu.add(menuItemMarkUnplayed);

		sharedFolders.setComponentPopupMenu(popupMenu);

		/* An attempt to set the correct row height adjusted for font scaling.
		 * It sets all rows based on the font size of cell (0, 0). The + 4 is
		 * to allow 2 pixels above and below the text. */
		DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) sharedFolders.getCellRenderer(0,0);
		FontMetrics metrics = cellRenderer.getFontMetrics(cellRenderer.getFont());
		sharedFolders.setRowHeight(metrics.getLeading() + metrics.getMaxAscent() + metrics.getMaxDescent() + 4);
		sharedFolders.setIntercellSpacing(new Dimension(8, 2));

		final JPanel tmpsharedPanel = sharedPanel;

		addButton.setToolTipText(Messages.getString("FoldTab.9"));
		addButton.addActionListener(new ActionListener() {
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
					int firstSelectedRow = sharedFolders.getSelectedRow();
					if (firstSelectedRow >= 0) {
						((SharedFoldersTableModel) sharedFolders.getModel()).insertRow(
							firstSelectedRow,
							new Object[]{chooser.getSelectedFile().getAbsolutePath(), true}
						);
					} else {
						((SharedFoldersTableModel) sharedFolders.getModel()).addRow(
							new Object[]{chooser.getSelectedFile().getAbsolutePath(), true}
						);
					}
				}
			}
		});
		builderFolder.add(addButton, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));
		
		removeButton.setToolTipText(Messages.getString("FoldTab.36"));
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rows = sharedFolders.getSelectedRows();
				if (rows.length > 0) {
					if (rows.length > 1) {
						if (
							JOptionPane.showConfirmDialog(
								tmpsharedPanel,
								String.format(Messages.getString("SharedFolders.ConfirmRemove"), rows.length),
								Messages.getString("Dialog.Confirm"),
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE
							) != JOptionPane.YES_OPTION
						) {
							return;
						}
					}
					for (int i = rows.length - 1; i >= 0; i--) {
						PMS.get().getDatabase().removeMediaEntriesInFolder((String) sharedFolders.getValueAt(sharedFolders.getSelectedRow(), 0));
						((SharedFoldersTableModel) sharedFolders.getModel()).removeRow(rows[i]);
					}
				}
			}
		});
		builderFolder.add(removeButton, FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		arrowDownButton.setToolTipText(Messages.getString("SharedContentTab.ArrowDown"));
		arrowDownButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < sharedFolders.getRowCount() - 1; i++) {
					if (sharedFolders.isRowSelected(i)) {
						Object  value1 = sharedFolders.getValueAt(i, 0);
						boolean value2 = (boolean) sharedFolders.getValueAt(i, 1);

						sharedFolders.setValueAt(sharedFolders.getValueAt(i + 1, 0), i    , 0);
						sharedFolders.setValueAt(value1                            , i + 1, 0);
						sharedFolders.setValueAt(sharedFolders.getValueAt(i + 1, 1), i    , 1);
						sharedFolders.setValueAt(value2                            , i + 1, 1);
						sharedFolders.changeSelection(i + 1, 1, false, false);

						break;
					}
				}
			}
		});
		builderFolder.add(arrowDownButton, FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

		arrowUpButton.setToolTipText(Messages.getString("SharedContentTab.ArrowUp"));
		arrowUpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 1; i < sharedFolders.getRowCount(); i++) {
					if (sharedFolders.isRowSelected(i)) {
						Object  value1 = sharedFolders.getValueAt(i, 0);
						boolean value2 = (boolean) sharedFolders.getValueAt(i, 1);

						sharedFolders.setValueAt(sharedFolders.getValueAt(i - 1, 0), i    , 0);
						sharedFolders.setValueAt(value1                            , i - 1, 0);
						sharedFolders.setValueAt(sharedFolders.getValueAt(i - 1, 1), i    , 1);
						sharedFolders.setValueAt(value2                            , i - 1, 1);
						sharedFolders.changeSelection(i - 1, 1, false, false);

						break;

					}
				}
			}
		});
		builderFolder.add(arrowUpButton, FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));

		scanButton.setToolTipText(Messages.getString("FoldTab.2"));
		scanBusyIcon.start();
		scanBusyDisabledIcon.start();
		scanButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (configuration.getUseCache()) {
					DLNAMediaDatabase database = PMS.get().getDatabase();

					if (database != null) {
						if (database.isScanLibraryRunning()) {
							int option = JOptionPane.showConfirmDialog(
								looksFrame,
								Messages.getString("FoldTab.10"),
								Messages.getString("Dialog.Question"),
								JOptionPane.YES_NO_OPTION);
							if (option == JOptionPane.YES_OPTION) {
								database.stopScanLibrary();
								looksFrame.setStatusLine(Messages.getString("FoldTab.41"));
								scanButton.setEnabled(false);
								scanButton.setToolTipText(Messages.getString("FoldTab.41"));
							}
						} else {
							database.scanLibrary();
							scanButton.setIcon(scanBusyIcon);
							scanButton.setRolloverIcon(scanBusyRolloverIcon);
							scanButton.setPressedIcon(scanBusyPressedIcon);
							scanButton.setDisabledIcon(scanBusyDisabledIcon);
							scanButton.setToolTipText(Messages.getString("FoldTab.40"));
						}
					}
				}
			}
		});

		/*
		 * Hide the scan button in basic mode since it's better to let it be done in
		 * realtime.
		 */
		if (!configuration.isHideAdvancedOptions()) {
			builderFolder.add(scanButton, FormLayoutUtil.flip(cc.xy(6, 3), colSpec, orientation));
		}

		scanButton.setEnabled(configuration.getUseCache());

		isScanSharedFoldersOnStartup = new JCheckBox(Messages.getString("NetworkTab.StartupScan"), configuration.isScanSharedFoldersOnStartup());
		isScanSharedFoldersOnStartup.setToolTipText(Messages.getString("NetworkTab.StartupScanTooltip"));
		isScanSharedFoldersOnStartup.setContentAreaFilled(false);
		isScanSharedFoldersOnStartup.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setScanSharedFoldersOnStartup((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		builderFolder.add(isScanSharedFoldersOnStartup, FormLayoutUtil.flip(cc.xy(7, 3), colSpec, orientation));

		updateSharedFolders();

		JScrollPane pane = new JScrollPane(sharedFolders);
		Dimension d = sharedFolders.getPreferredSize();
		pane.setPreferredSize(new Dimension(d.width, sharedFolders.getRowHeight() * 2));
		builderFolder.add(pane, FormLayoutUtil.flip(
			cc.xyw(1, 5, 7, CellConstraints.DEFAULT, CellConstraints.FILL),
			colSpec,
			orientation
		));

		return builderFolder;
	}

	private PanelBuilder initWebContentGuiComponents(CellConstraints cc) {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(SHARED_FOLDER_COL_SPEC, orientation);

		FormLayout layoutFolders = new FormLayout(colSpec, SHARED_FOLDER_ROW_SPEC);
		PanelBuilder builderFolder = new PanelBuilder(layoutFolders);
		builderFolder.opaque(true);

		JComponent cmp = builderFolder.addSeparator(Messages.getString("SharedContentTab.WebContent"), FormLayoutUtil.flip(cc.xyw(1, 1, 7), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		webContentTableModel = new WebContentTableModel();
		webContentList = new JTable(webContentTableModel);
		TableColumn column = webContentList.getColumnModel().getColumn(2);
		column.setMinWidth(500);

		webContentList.addMouseListener(new TableMouseListener(webContentList));

		/*
		 * An attempt to set the correct row height adjusted for font scaling.
		 * It sets all rows based on the font size of cell (0, 0). The + 4 is
		 * to allow 2 pixels above and below the text.
		 */
		DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) webContentList.getCellRenderer(0, 0);
		FontMetrics metrics = cellRenderer.getFontMetrics(cellRenderer.getFont());
		webContentList.setRowHeight(metrics.getLeading() + metrics.getMaxAscent() + metrics.getMaxDescent() + 4);
		webContentList.setIntercellSpacing(new Dimension(8, 2));

		JImageButton but = new JImageButton("button-add-webcontent.png");
		but.setToolTipText(Messages.getString("SharedContentTab.AddNewWebContent"));
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox<String> newEntryType = new JComboBox<>(TYPES_READABLE);
				newEntryType.setEditable(false);

				JTextField newEntryFolders = new JTextField(25);
				newEntryFolders.setText("Web,");

				JTextField newEntrySource = new JTextField(50);

				JPanel addNewWebContentPanel = new JPanel();

				JLabel labelType = new JLabel(Messages.getString("SharedContentTab.TypeColon"));
				JLabel labelFolders = new JLabel(Messages.getString("SharedContentTab.FoldersColon"));
				JLabel labelSource = new JLabel(Messages.getString("SharedContentTab.SourceURLColon"));

				labelType.setLabelFor(newEntryType);
				labelFolders.setLabelFor(newEntryFolders);
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
								.addComponent(labelSource)
								.addComponent(newEntrySource)
								.addContainerGap()
						)
				);

				int result = JOptionPane.showConfirmDialog(null, addNewWebContentPanel, Messages.getString("SharedContentTab.AddNewWebContent"), JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION) {
					((WebContentTableModel) webContentList.getModel()).addRow(new Object[]{newEntryType.getSelectedItem(), newEntryFolders.getText(), newEntrySource.getText()});
					updateWebContentModel();
				}
			}
		});
		builderFolder.add(but, FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		JImageButton but2 = new JImageButton("button-remove-folder.png");
		but2.setToolTipText(Messages.getString("SharedContentTab.RemoveSelectedWebContent"));
		but2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (webContentList.getSelectedRow() > -1) {
					PMS.get().getDatabase().removeMediaEntriesInFolder((String) webContentList.getValueAt(webContentList.getSelectedRow(), 0));
					((WebContentTableModel) webContentList.getModel()).removeRow(webContentList.getSelectedRow());
					updateWebContentModel();
				}
			}
		});
		builderFolder.add(but2, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JImageButton but3 = new JImageButton("button-arrow-down.png");
		but3.setToolTipText(Messages.getString("SharedContentTab.MoveSelectedWebContentDown"));
		but3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < webContentList.getRowCount() - 1; i++) {
					if (webContentList.isRowSelected(i)) {
						Object type   = webContentList.getValueAt(i, 0);
						Object folder = webContentList.getValueAt(i, 1);
						Object source = webContentList.getValueAt(i, 2);

						webContentList.setValueAt(webContentList.getValueAt(i + 1, 0), i    , 0);
						webContentList.setValueAt(type                               , i + 1, 0);
						webContentList.setValueAt(webContentList.getValueAt(i + 1, 1), i    , 1);
						webContentList.setValueAt(folder                             , i + 1, 1);
						webContentList.setValueAt(webContentList.getValueAt(i + 1, 2), i    , 2);
						webContentList.setValueAt(source                             , i + 1, 2);
						webContentList.changeSelection(i + 1, 1, false, false);

						break;
					}
				}
			}
		});
		builderFolder.add(but3, FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		JImageButton but4 = new JImageButton("button-arrow-up.png");
		but4.setToolTipText(Messages.getString("SharedContentTab.MoveSelectedWebContentUp"));
		but4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 1; i < webContentList.getRowCount(); i++) {
					if (webContentList.isRowSelected(i)) {
						Object type   = webContentList.getValueAt(i, 0);
						Object folder = webContentList.getValueAt(i, 1);
						Object source = webContentList.getValueAt(i, 2);

						webContentList.setValueAt(webContentList.getValueAt(i - 1, 0), i    , 0);
						webContentList.setValueAt(type                               , i - 1, 0);
						webContentList.setValueAt(webContentList.getValueAt(i - 1, 1), i    , 1);
						webContentList.setValueAt(folder                             , i - 1, 1);
						webContentList.setValueAt(webContentList.getValueAt(i - 1, 2), i    , 2);
						webContentList.setValueAt(source                             , i - 1, 2);
						webContentList.changeSelection(i - 1, 1, false, false);

						break;
					}
				}
			}
		});
		builderFolder.add(but4, FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

		JScrollPane pane = new JScrollPane(webContentList);
		Dimension d = webContentList.getPreferredSize();
		pane.setPreferredSize(new Dimension(d.width, webContentList.getRowHeight() * 2));
		builderFolder.add(pane, FormLayoutUtil.flip(cc.xyw(1, 5, 7), colSpec, orientation));

		return builderFolder;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateSharedFolders() {
		List<Path> folders = configuration.getSharedFolders();
		Vector<Vector<?>> newDataVector = new Vector<>();
		if (!folders.isEmpty()) {
			List<Path> foldersMonitored = configuration.getMonitoredFolders();
			for (Path folder : folders) {
				Vector rowVector = new Vector();
				rowVector.add(folder.toString());
				rowVector.add(Boolean.valueOf(foldersMonitored.contains(folder)));
				newDataVector.add(rowVector);
			}
		}
		folderTableModel.setDataVector(newDataVector, FOLDERS_COLUMN_NAMES);
		TableColumn column = sharedFolders.getColumnModel().getColumn(0);
		column.setMinWidth(600);
	}

	public static void setScanLibraryEnabled(boolean enabled) {
		scanButton.setEnabled(enabled);
		scanButton.setIcon(scanNormalIcon);
		scanButton.setRolloverIcon(scanRolloverIcon);
		scanButton.setPressedIcon(scanPressedIcon);
		scanButton.setDisabledIcon(scanDisabledIcon);
		scanButton.setToolTipText(Messages.getString("FoldTab.2"));
	}

	/**
	 * @todo combine with setScanLibraryEnabled after we are in sync with DMS
	 */
	public static void setScanLibraryBusy() {
		scanButton.setIcon(scanBusyIcon);
		scanButton.setRolloverIcon(scanBusyRolloverIcon);
		scanButton.setPressedIcon(scanBusyPressedIcon);
		scanButton.setDisabledIcon(scanBusyDisabledIcon);
		scanButton.setToolTipText(Messages.getString("FoldTab.40"));
	}

	public class SharedFoldersTableModel extends DefaultTableModel {
		private static final long serialVersionUID = -4247839506937958655L;

		public SharedFoldersTableModel() {
			super(FOLDERS_COLUMN_NAMES, 0);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 1 ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 1;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void setValueAt(Object aValue, int row, int column) {
			Vector rowVector = (Vector) dataVector.elementAt(row);
			if (aValue instanceof Boolean && column == 1) {
				rowVector.setElementAt(aValue, 1);
			} else {
				rowVector.setElementAt(aValue, column);
			}
			fireTableCellUpdated(row, column);
			configuration.setSharedFolders(folderTableModel.getDataVector());
		}

		@Override
		public void insertRow(int row, Vector rowData) {
			super.insertRow(row, rowData);
			configuration.setSharedFolders(folderTableModel.getDataVector());
		}

		@Override
		public void removeRow(int row) {
			super.removeRow(row);
			configuration.setSharedFolders(folderTableModel.getDataVector());
		}
	}

	public class WebContentTableModel extends DefaultTableModel {
		private static final long serialVersionUID = -4247839506937958655L;

		public WebContentTableModel() {
			// Column headings
			super(new String[]{
				Messages.getString("SharedContentTab.Type"),
				Messages.getString("SharedContentTab.FolderName"),
				Messages.getString("SharedContentTab.Source"),
			}, 0);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			Vector rowVector = (Vector) dataVector.elementAt(row);
			rowVector.setElementAt(aValue, column);
			fireTableCellUpdated(row, column);
			updateWebContentModel();
		}
	}

	public class TableMouseListener extends MouseAdapter {
		private JTable table;

		public TableMouseListener(JTable table) {
			this.table = table;
		}

		@Override
		public void mousePressed(MouseEvent event) {
			// selects the row at which point the mouse is clicked
			Point point = event.getPoint();
			int currentRow = table.rowAtPoint(point);
			table.setRowSelectionInterval(currentRow, currentRow);

			// more than one click in the same event triggers edit mode
			if (event.getClickCount() == 2) {
				String currentType    = (String) webContentList.getValueAt(currentRow, 0);
				String currentFolders = (String) webContentList.getValueAt(currentRow, 1);
				String currentSource  = (String) webContentList.getValueAt(currentRow, 2);

				int currentTypeIndex = Arrays.asList(TYPES_READABLE).indexOf(currentType);

				JComboBox<String> newEntryType = new JComboBox<>(TYPES_READABLE);
				newEntryType.setEditable(false);
				newEntryType.setSelectedIndex(currentTypeIndex);

				JTextField newEntryFolders = new JTextField(25);
				newEntryFolders.setText(currentFolders);

				JTextField newEntrySource = new JTextField(50);
				newEntrySource.setText(currentSource);

				JPanel addNewWebContentPanel = new JPanel();

				JLabel labelType = new JLabel(Messages.getString("SharedContentTab.TypeColon"));
				JLabel labelFolders = new JLabel(Messages.getString("SharedContentTab.FoldersColon"));
				JLabel labelSource = new JLabel(Messages.getString("SharedContentTab.SourceURLColon"));

				labelType.setLabelFor(newEntryType);
				labelFolders.setLabelFor(newEntryFolders);
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
								.addComponent(labelSource)
								.addComponent(newEntrySource)
								.addContainerGap()
						)
				);
		
				int result = JOptionPane.showConfirmDialog(null, addNewWebContentPanel, Messages.getString("SharedContentTab.AddNewWebContent"), JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION) {
					webContentList.setValueAt(newEntryType.getSelectedItem(), currentRow, 0);
					webContentList.setValueAt(newEntryFolders.getText(),      currentRow, 1);
					webContentList.setValueAt(newEntrySource.getText(),       currentRow, 2);
					updateWebContentModel();
				}
			}
		}
	}

	/**
	 * This parses the web config and populates the web section of this tab.
	 *
	 * @param webConf
	 */
	public static void parseWebConf(File webConf) {
		try {
			// Remove any existing rows
			((WebContentTableModel) webContentList.getModel()).setRowCount(0);

			try (LineNumberReader br = new LineNumberReader(new InputStreamReader(new FileInputStream(webConf), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();

					if (line.length() > 0 && !line.startsWith("#") && line.indexOf('=') > -1) {
						String key = line.substring(0, line.indexOf('='));
						String value = line.substring(line.indexOf('=') + 1);
						String[] keys = parseFeedKey(key);
						String sourceType = keys[0];
						String folderName = keys[1] == null ? null : keys[1];

						try {
							if (
								sourceType.equals("imagefeed") ||
								sourceType.equals("audiofeed") ||
								sourceType.equals("videofeed") ||
								sourceType.equals("audiostream") ||
								sourceType.equals("videostream")
							) {
								String[] values = parseFeedValue(value);
								String uri = values[0];

								String readableType = "";
								switch (sourceType) {
									case "imagefeed":
										readableType = "Image feed";
										break;
									case "videofeed":
										readableType = "Video feed";
										break;
									case "audiofeed":
										readableType = "Podcast";
										break;
									case "audiostream":
										readableType = "Audio stream";
										break;
									case "videostream":
										readableType = "Video stream";
										break;
									default:
										break;
								}

								webContentTableModel.addRow(new Object[]{readableType, folderName, uri});
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							// catch exception here and go with parsing
							LOGGER.info("Error at line " + br.getLineNumber() + " of WEB.conf: " + e.getMessage());
							LOGGER.debug(null, e);
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			LOGGER.debug("Can't read web configuration file {}", e.getMessage());
		} catch (IOException e) {
			LOGGER.warn("Unexpected error in WEB.conf: " + e.getMessage());
			LOGGER.debug("", e);
		}
	}
}
