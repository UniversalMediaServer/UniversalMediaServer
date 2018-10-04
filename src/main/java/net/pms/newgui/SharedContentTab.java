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
import net.pms.newgui.components.AnimatedIcon;
import net.pms.newgui.components.JAnimatedButton;
import net.pms.newgui.components.JImageButton;
import net.pms.util.FormLayoutUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedContentTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(SharedContentTab.class);
	public static final String ALL_DRIVES = Messages.getString("FoldTab.0");

	private JTable FList;
	private SharedFoldersTableModel folderTableModel;
	private WebContentTableModel webContentTableModel;
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

	public SharedFoldersTableModel getDf() {
		return folderTableModel;
	}

	private final PmsConfiguration configuration;
	private LooksFrame looksFrame;

	SharedContentTab(PmsConfiguration configuration, LooksFrame looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
	}

	private void updateSharedFoldersModel() {
		if (folderTableModel.getRowCount() == 1 && folderTableModel.getValueAt(0, 0).equals(ALL_DRIVES)) {
			configuration.setFolders("");
		} else {
			StringBuilder folders = new StringBuilder();
			StringBuilder foldersMonitored = new StringBuilder();

			int i2 = 0;
			for (int i = 0; i < folderTableModel.getRowCount(); i++) {
				if (i > 0) {
					folders.append(',');
				}

				String directory = (String) folderTableModel.getValueAt(i, 0);
				boolean monitored = (boolean) folderTableModel.getValueAt(i, 1);

				// escape embedded commas. note: backslashing isn't safe as it conflicts with
				// Windows path separators:
				// http://ps3mediaserver.org/forum/viewtopic.php?f=14&t=8883&start=250#p43520
				folders.append(directory.replace(",", "&comma;"));
				if (monitored) {
					if (i2 > 0) {
						foldersMonitored.append(',');
					}
					i2++;

					foldersMonitored.append(directory.replace(",", "&comma;"));
				}
			}

			configuration.setFolders(folders.toString());
			configuration.setFoldersMonitored(foldersMonitored.toString());
		}
	}

	private void updateWebContentModel() {
		if (folderTableModel.getRowCount() == 1 && folderTableModel.getValueAt(0, 0).equals(ALL_DRIVES)) {
			configuration.setFolders("");
		} else {
			StringBuilder folders = new StringBuilder();
			StringBuilder foldersMonitored = new StringBuilder();

			int i2 = 0;
			for (int i = 0; i < folderTableModel.getRowCount(); i++) {
				if (i > 0) {
					folders.append(',');
				}

				String directory = (String) folderTableModel.getValueAt(i, 0);
				boolean monitored = (boolean) folderTableModel.getValueAt(i, 1);

				// escape embedded commas. note: backslashing isn't safe as it conflicts with
				// Windows path separators:
				// http://ps3mediaserver.org/forum/viewtopic.php?f=14&t=8883&start=250#p43520
				folders.append(directory.replace(",", "&comma;"));
				if (monitored) {
					if (i2 > 0) {
						foldersMonitored.append(',');
					}
					i2++;

					foldersMonitored.append(directory.replace(",", "&comma;"));
				}
			}

			configuration.setFolders(folders.toString());
			configuration.setFoldersMonitored(foldersMonitored.toString());
		}
	}

	private static final String PANEL_COL_SPEC = "left:pref,          50dlu,                pref, 150dlu,                       pref, 25dlu,               pref, 9dlu, pref, default:grow, pref, 25dlu";
	private static final String PANEL_ROW_SPEC = "fill:default:grow, 9dlu, fill:default:grow";
	private static final String SHARED_FOLDER_COL_SPEC = "left:pref, left:pref, pref, pref, pref, 10dlu, 0:grow";
	private static final String SHARED_FOLDER_ROW_SPEC = "p, 3dlu, p, 3dlu, fill:default:grow";

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
		PanelBuilder builderSharedFolder = initSharedFoldersGuiComponents(cc);
		PanelBuilder builderWebContent   = initWebContentGuiComponents(cc);

		builder.add(builderSharedFolder.getPanel(), FormLayoutUtil.flip(cc.xyw(1, 1, 12), colSpec, orientation));
		builder.add(builderWebContent.getPanel(),   FormLayoutUtil.flip(cc.xyw(1, 3, 12), colSpec, orientation));

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
		FList = new JTable(folderTableModel);
		TableColumn column = FList.getColumnModel().getColumn(0);
		column.setMinWidth(650);

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItemMarkPlayed = new JMenuItem(Messages.getString("FoldTab.75"));
		JMenuItem menuItemMarkUnplayed = new JMenuItem(Messages.getString("FoldTab.76"));

		menuItemMarkPlayed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = (String) FList.getValueAt(FList.getSelectedRow(), 0);
				TableFilesStatus.setDirectoryFullyPlayed(path, true);
			}
		});

		menuItemMarkUnplayed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = (String) FList.getValueAt(FList.getSelectedRow(), 0);
				TableFilesStatus.setDirectoryFullyPlayed(path, false);
			}
		});

		popupMenu.add(menuItemMarkPlayed);
		popupMenu.add(menuItemMarkUnplayed);

		FList.setComponentPopupMenu(popupMenu);
		FList.addMouseListener(new TableMouseListener(FList));

		/* An attempt to set the correct row height adjusted for font scaling.
		 * It sets all rows based on the font size of cell (0, 0). The + 4 is
		 * to allow 2 pixels above and below the text. */
		DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) FList.getCellRenderer(0,0);
		FontMetrics metrics = cellRenderer.getFontMetrics(cellRenderer.getFont());
		FList.setRowHeight(metrics.getLeading() + metrics.getMaxAscent() + metrics.getMaxDescent() + 4);
		FList.setIntercellSpacing(new Dimension(8, 2));

		JImageButton but = new JImageButton("button-add-folder.png");
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
					((SharedFoldersTableModel) FList.getModel()).addRow(new Object[]{chooser.getSelectedFile().getAbsolutePath(), true});
					if (FList.getModel().getValueAt(0, 0).equals(ALL_DRIVES)) {
						((SharedFoldersTableModel) FList.getModel()).removeRow(0);
					}
					updateSharedFoldersModel();
				}
			}
		});
		builderFolder.add(but, FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		JImageButton but2 = new JImageButton("button-remove-folder.png");
		but2.setToolTipText(Messages.getString("FoldTab.36"));
		but2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (FList.getSelectedRow() > -1) {
					if (FList.getModel().getRowCount() == 0) {
						folderTableModel.addRow(new Object[]{ALL_DRIVES, false});
					} else {
						PMS.get().getDatabase().removeMediaEntriesInFolder((String) FList.getValueAt(FList.getSelectedRow(), 0));
					}
					((SharedFoldersTableModel) FList.getModel()).removeRow(FList.getSelectedRow());
					updateSharedFoldersModel();
				}
			}
		});
		builderFolder.add(but2, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JImageButton but3 = new JImageButton("button-arrow-down.png");
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
		builderFolder.add(but3, FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		JImageButton but4 = new JImageButton("button-arrow-up.png");
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
		builderFolder.add(but4, FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

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
			builderFolder.add(scanButton, FormLayoutUtil.flip(cc.xy(5, 3), colSpec, orientation));
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
		builderFolder.add(pane, FormLayoutUtil.flip(cc.xyw(1, 5, 7), colSpec, orientation));

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
		FList = new JTable(webContentTableModel);
		TableColumn column = FList.getColumnModel().getColumn(0);
		column.setMinWidth(650);

		FList.addMouseListener(new TableMouseListener(FList));

		/* An attempt to set the correct row height adjusted for font scaling.
		 * It sets all rows based on the font size of cell (0, 0). The + 4 is
		 * to allow 2 pixels above and below the text. */
		DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) FList.getCellRenderer(0,0);
		FontMetrics metrics = cellRenderer.getFontMetrics(cellRenderer.getFont());
		FList.setRowHeight(metrics.getLeading() + metrics.getMaxAscent() + metrics.getMaxDescent() + 4);
		FList.setIntercellSpacing(new Dimension(8, 2));

		JImageButton but = new JImageButton("button-add-folder.png");
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
					((WebContentTableModel) FList.getModel()).addRow(new Object[]{chooser.getSelectedFile().getAbsolutePath(), true});
					if (FList.getModel().getValueAt(0, 0).equals(ALL_DRIVES)) {
						((WebContentTableModel) FList.getModel()).removeRow(0);
					}
					updateWebContentModel();
				}
			}
		});
		builderFolder.add(but, FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		JImageButton but2 = new JImageButton("button-remove-folder.png");
		but2.setToolTipText(Messages.getString("FoldTab.36"));
		but2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (FList.getSelectedRow() > -1) {
					if (FList.getModel().getRowCount() == 0) {
						webContentTableModel.addRow(new Object[]{ALL_DRIVES, false});
					} else {
						PMS.get().getDatabase().removeMediaEntriesInFolder((String) FList.getValueAt(FList.getSelectedRow(), 0));
					}
					((WebContentTableModel) FList.getModel()).removeRow(FList.getSelectedRow());
					updateWebContentModel();
				}
			}
		});
		builderFolder.add(but2, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JImageButton but3 = new JImageButton("button-arrow-down.png");
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
		builderFolder.add(but3, FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		JImageButton but4 = new JImageButton("button-arrow-up.png");
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
		builderFolder.add(but4, FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

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
				webContentTableModel.addRow(new Object[]{folder.getAbsolutePath(), isMonitored});
			}
		}

		JScrollPane pane = new JScrollPane(FList);
		Dimension d = FList.getPreferredSize();
		pane.setPreferredSize(new Dimension(d.width, FList.getRowHeight() * 2));
		builderFolder.add(pane, FormLayoutUtil.flip(cc.xyw(1, 5, 7), colSpec, orientation));

		return builderFolder;
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
			super(new String[]{Messages.getString("FoldTab.56"), Messages.getString("FoldTab.65")}, 0);
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
			updateSharedFoldersModel();
		}
	}

	public class WebContentTableModel extends DefaultTableModel {
		private static final long serialVersionUID = -4247839506937958655L;

		public WebContentTableModel() {
			super(new String[]{Messages.getString("SharedContentTab.Source"), Messages.getString("FoldTab.65")}, 0);
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
		}
	}
}
