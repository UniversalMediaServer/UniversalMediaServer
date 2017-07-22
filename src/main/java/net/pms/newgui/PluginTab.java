package net.pms.newgui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.newgui.components.CustomJButton;
import net.pms.util.FormLayoutUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(PluginTab.class);
	private final PmsConfiguration configuration;
	private static final String COL_SPEC = "left:pref, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, pref:grow";
	private static final String ROW_SPEC = "p, 3dlu, fill:p, 3dlu, p, 15dlu, p, 8dlu, p, 8dlu, p, 3dlu, fill:p:grow, 3dlu, p, 3dlu, p";
	private JPanel pPlugins;
	private JPanel installedPluginsSeparator;
	private LooksFrame looksFrame;

	PluginTab(PmsConfiguration configuration, LooksFrame looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
		pPlugins = null;
		setupCred();
	}

	public JComponent build() {
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);

		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		// Installed Plugins section
		JComponent component;
		installedPluginsSeparator = (JPanel) builder.addSeparator(Messages.getString("PluginTab.0"), FormLayoutUtil.flip(cc.xyw(1, 7, 9), colSpec, orientation));
		installedPluginsSeparator.setVisible(false);
		component = (JComponent) installedPluginsSeparator.getComponent(0);
		component.setFont(component.getFont().deriveFont(Font.BOLD));

		pPlugins = new JPanel(new GridLayout());
		pPlugins.setVisible(false);
		builder.add(pPlugins, FormLayoutUtil.flip(cc.xyw(1, 9, 9), colSpec, orientation));

		// Credentials section
		component = builder.addSeparator(Messages.getString("PluginTab.8"), FormLayoutUtil.flip(cc.xyw(1, 11, 9), colSpec, orientation));
		component = (JComponent) component.getComponent(0);
		component.setFont(component.getFont().deriveFont(Font.BOLD));

		/* An attempt to set the correct row height adjusted for font scaling.
		 * It sets all rows based on the font size of cell (0, 0). The + 4 is
		 * to allow 2 pixels above and below the text. */
		DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) credTable.getCellRenderer(0,0);
		FontMetrics metrics = cellRenderer.getFontMetrics(cellRenderer.getFont());
		credTable.setRowHeight(metrics.getLeading() + metrics.getMaxAscent() + metrics.getMaxDescent() + 4);
		credTable.setFillsViewportHeight(true);

		credTable.setIntercellSpacing(new Dimension(8, 2));

		// Define column widths
		TableColumn ownerColumn = credTable.getColumnModel().getColumn(0);
		ownerColumn.setMinWidth(70);
		TableColumn tagColumn = credTable.getColumnModel().getColumn(2);
		tagColumn.setPreferredWidth(45);
		TableColumn usrColumn = credTable.getColumnModel().getColumn(2);
		usrColumn.setPreferredWidth(45);
		TableColumn pwdColumn = credTable.getColumnModel().getColumn(3);
		pwdColumn.setPreferredWidth(45);

		JScrollPane pane = new JScrollPane(credTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.setPreferredSize(new Dimension(200, 95));
		builder.add(pane, FormLayoutUtil.flip(cc.xyw(1, 13, 9), colSpec, orientation));

		// Add button
		CustomJButton add = new CustomJButton(Messages.getString("PluginTab.9"));
		builder.add(add, FormLayoutUtil.flip(cc.xy(1, 15), colSpec, orientation));
		add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addEditDialog(credTable, -1);
			}
		});

		// Edit button
		CustomJButton edit = new CustomJButton(Messages.getString("PluginTab.11"));
		builder.add(edit, FormLayoutUtil.flip(cc.xy(3, 15), colSpec, orientation));
		edit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addEditDialog(credTable, credTable.getSelectedRow());
			}
		});

		// Delete button
		CustomJButton del = new CustomJButton(Messages.getString("PluginTab.12"));
		builder.add(del, FormLayoutUtil.flip(cc.xy(5, 15), colSpec, orientation));
		del.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rows = credTable.getSelectedRows();
				if (rows.length > 0) {
					int n = JOptionPane.showConfirmDialog(
						looksFrame,
						Messages.getString("PluginTab.13"),
						"",
						JOptionPane.YES_NO_OPTION
					);

					if (n == JOptionPane.YES_OPTION) {
						for (int i=0; i < rows.length; i++) {
							String key = (String) credTable.getValueAt(rows[i], 0);
							if (StringUtils.isNotEmpty((String) credTable.getValueAt(rows[i], 1))) {
								key = key + "." + (String) credTable.getValueAt(rows[i], 1);
							}
							cred.clearProperty(key);
						}
					}

					try {
						cred.save();
					} catch (ConfigurationException e1) {
						LOGGER.warn("Couldn't save credentials file {}", e1.getMessage());
						LOGGER.trace("", e1);
					}

					refreshCred(credTable);
				}
			}
		});

		// Edit Plugin Credential File button
		CustomJButton credEdit = new CustomJButton(Messages.getString("NetworkTab.54"));
		credEdit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JPanel tPanel = new JPanel(new BorderLayout());

				final JTextArea textArea = new JTextArea();
				textArea.setFont(new Font("Courier", Font.PLAIN, 12));
				JScrollPane scrollPane = new JScrollPane(textArea);
				scrollPane.setPreferredSize(new Dimension(900, 450));

				try {
					configuration.initCred();
				} catch (IOException e2) {
					LOGGER.error("Could not create credentials file: {}", e2.getMessage());
					LOGGER.trace("", e2);
					return;
				}

				try {
					cred.save();
				} catch (ConfigurationException e3) {
					LOGGER.error("Could not save credentials file: {}", e3.getMessage());
					LOGGER.trace("", e3);
				}
				File f = configuration.getCredFile();

				try {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
						String line;
						StringBuilder sb = new StringBuilder();
						while ((line = in.readLine()) != null) {
							sb.append(line);
							sb.append("\n");
						}
						textArea.setText(sb.toString());
					}
				} catch (IOException e1) {
					LOGGER.error("Could not read credentials file: {}", e1.getMessage());
					LOGGER.trace("", e1);
					return;
				}

				tPanel.add(scrollPane, BorderLayout.NORTH);

				Object[] options = {Messages.getString("LooksFrame.9"), Messages.getString("NetworkTab.45")};
				if (
					JOptionPane.showOptionDialog(
						looksFrame,
						tPanel,
						Messages.getString("NetworkTab.54"),
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						options,
						null
					) == JOptionPane.OK_OPTION
				) {
					String text = textArea.getText();
					try {
						try (FileOutputStream fos = new FileOutputStream(f)) {
							fos.write(text.getBytes(StandardCharsets.UTF_8));
							fos.flush();
						}
						PMS.getConfiguration().reload();
						try {
							cred.refresh();
						} catch (ConfigurationException e2) {
							LOGGER.error("An error occurred while updating credentials: {}", e2);
							LOGGER.trace("", e2);
						}
						refreshCred(credTable);
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(looksFrame, Messages.getString("NetworkTab.55") + ": " + e1.getMessage());
					}
				}
			}
		});
		builder.add(credEdit, FormLayoutUtil.flip(cc.xy(7, 15), colSpec, orientation));

		JPanel panel = builder.getPanel();
		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	public void addPlugins() {
		FormLayout layout = new FormLayout(
			"fill:10:grow",
			"p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p");
		pPlugins.setLayout(layout);
		List<ExternalListener> externalListeners = ExternalFactory.getExternalListeners();
		for (final ExternalListener listener : externalListeners) {
			if (!appendPlugin(listener)) {
				break;
			}
		}
		pPlugins.setVisible(externalListeners.size() > 0);
		installedPluginsSeparator.setVisible(externalListeners.size() > 0);
	}

	public boolean appendPlugin(final ExternalListener listener) {
		final JComponent comp = listener.config();
		if (comp == null) {
			return true;
		}
		CellConstraints cc = new CellConstraints();
		CustomJButton bPlugin = new CustomJButton(listener.name());
		// Listener to show option screen
		bPlugin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showOptionDialog(
					looksFrame,
					comp,
					Messages.getString("Dialog.Options"),
					JOptionPane.CLOSED_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, null, null
				);
			}
		});
		int y = pPlugins.getComponentCount() + 1;
		if (y > 30) {
			return false;
		}
		pPlugins.add(bPlugin, cc.xy(1, y));
		return true;
	}

	public void removePlugin(ExternalListener listener) {
		CustomJButton del = null;
		for (Component c : pPlugins.getComponents()) {
			if (c instanceof CustomJButton) {
				CustomJButton button = (CustomJButton) c;
				if (button.getText().equals(listener.name())) {
					del = button;
					break;
				}
			}
		}

		if (del != null) {
			pPlugins.remove(del);
			pPlugins.repaint();
		}
	}

	private static void prepareTable(JTable table,String[] cols) {
		JTableHeader hdr = table.getTableHeader();
		TableColumnModel tcm = hdr.getColumnModel();

		for (int i = 0; i < cols.length; i++) {
			TableColumn tc = tcm.getColumn(i);
			tc.setHeaderValue( cols[i] );
		}

		hdr.repaint();
	}

	/**
	 * Credentials section
	 */

	private PropertiesConfiguration cred;
	private JTable credTable;

	private void setupCred() {
		cred = new PropertiesConfiguration();
		cred.setListDelimiter((char) 0);
		String[] cols = {
			Messages.getString("PluginTab.4"),
			Messages.getString("PluginTab.5"),
			Messages.getString("PluginTab.6"),
			Messages.getString("PluginTab.7")
		};
		credTable = new JTable(0, cols.length) {
			private static final long serialVersionUID = 1510535097140083493L;

			@Override
			public boolean isCellEditable(int rowIndex, int vColIndex) {
				return false;
			}
		};
		prepareTable(credTable, cols);
	}

	public void init() {
		File cFile = configuration.getCredFile();

		if (cFile != null) {
			try {
				cred.load(cFile);
				cred.setFile(cFile);
			} catch (ConfigurationException e) {
				LOGGER.warn("Can't load credentials file {}: {}", cFile, e.getMessage());
				LOGGER.trace("", e);
			}
		} else {
			LOGGER.debug("Something went seriously wrong - getCredFile() returned null!");
		}

		refreshCred(credTable);
	}

	private void refreshCred(JTable table) {
		cred.reload();

		DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
		tableModel.setRowCount(0);

		TableColumn tcol = credTable.getColumnModel().getColumn(3);
		tcol.setCellRenderer(new PasswordCellRenderer());

		Iterator<String> itr = cred.getKeys();

		int i = 0;
		while (itr.hasNext()) {
			String key = itr.next();
			if (StringUtils.isEmpty(key)) {
				continue;
			}
			Object val = cred.getProperty(key);
			String[] ownerTag = key.split("\\.", 2);
			ArrayList<String> usrPwd = null;

			if (val instanceof String) {
				usrPwd = new ArrayList<>();
				usrPwd.add((String) val);
			} else if (val instanceof List<?>) {
				usrPwd = (ArrayList<String>) val;
			}

			if (usrPwd == null) {
				continue;
			}

			for (String val1 : usrPwd) {
				tableModel.insertRow(i , (Object[]) null);
				table.setValueAt(ownerTag[0], i , 0);
				if (ownerTag.length > 1) {
					table.setValueAt(ownerTag[1], i , 1);
				}
				String[] tmp = val1.split(",", 2);
				if (tmp.length > 0) {
					table.setValueAt(tmp[0], i , 2);
				}
				if (tmp.length > 1) {
					table.setValueAt(tmp[1], i , 3);
				}

				i++;
			}
		}
		tableModel.fireTableDataChanged();
	}

	private void addEditDialog(final JTable table,int row) {
		JPanel panel = new JPanel();
		GridLayout layout = new GridLayout(0, 2);
		panel.setLayout(layout);
		final JFrame frame = new JFrame(Messages.getString("PluginTab.10"));
		frame.setSize(270, 130);
		final JLabel owner = new JLabel(Messages.getString("PluginTab.4"));
		final JLabel tag = new JLabel(Messages.getString("PluginTab.5"));
		final JLabel usr = new JLabel(Messages.getString("PluginTab.6"));
		final JLabel pwd = new JLabel(Messages.getString("PluginTab.7"));
		final JCheckBox hidden = new JCheckBox(Messages.getString("PluginTab.14"), false);
		JLabel empty = new JLabel(" ");
		String o = "";
		String t = "";
		String u = "";
		String p = "";
		if (row != -1) {
			o = (String) table.getValueAt(row, 0);
			t = (String) table.getValueAt(row, 1);
			u = (String) table.getValueAt(row, 2);
			p = (String) table.getValueAt(row, 3);
		}
		final JTextField oText = new JTextField(o);
		final JTextField tText = new JTextField(t);
		final JTextField uText = new JTextField(u);
		final JPasswordField pText = new JPasswordField(p);
		final char defEchoChar = pText.getEchoChar();

		JButton ok = new JButton(Messages.getString("Dialog.OK"));
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
				String key = oText.getText();
				String pwd = new String(pText.getPassword());
				if (
					StringUtils.isEmpty(key) ||
					StringUtils.isEmpty(uText.getText()) ||
					StringUtils.isEmpty(pwd)
				) {
					// ignore this
					return;
				}

				if (StringUtils.isNotEmpty(tText.getText())) {
					key = key + "." + tText.getText();
				}
				String val = uText.getText() + "," + pwd;
				cred.addProperty(key, val);
				try {
					cred.save();
				} catch (ConfigurationException e1) {
					LOGGER.warn("Error saving credentials file {}", e1);
					LOGGER.trace("", e1);
				}
				refreshCred(table);
			}
		});

		JButton cancel = new JButton(Messages.getString("NetworkTab.45"));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
			}
		});

		hidden.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					pText.setEchoChar((char) 0);
				} else {
					pText.setEchoChar(defEchoChar);
				}
			}
		});

		panel.add(owner);
		panel.add(oText);
		panel.add(tag);
		panel.add(tText);
		panel.add(usr);
		panel.add(uText);
		panel.add(pwd);
		panel.add(pText);
		panel.add(hidden);
		panel.add(empty);
		panel.add(ok);
		panel.add(cancel);
		frame.add(panel);

		// Center the installation progress window
		frame.setLocationRelativeTo(looksFrame);
		frame.setVisible(true);
	}

	private class PasswordCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1020393206165351323L;

		public PasswordCellRenderer() {
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable  tab,
			Object obj,
			boolean isSelected,
			boolean hasFocus,
			int row,
			int col
		) {
			Component c = super.getTableCellRendererComponent(tab, obj, isSelected, hasFocus, row, col);
			JLabel l = (JLabel)c;

			if (StringUtils.isNotEmpty(l.getText())) {
				l.setText("**************");
			}

			return l;
		}
	}
}
