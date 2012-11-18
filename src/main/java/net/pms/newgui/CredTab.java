package net.pms.newgui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.FileUtil;
import net.pms.util.FormLayoutUtil;

public class CredTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(CredTab.class);
	private final PmsConfiguration configuration;
	private static final String COL_SPEC = "left:pref, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, pref:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p";
	
	private PropertiesConfiguration cred;
	private String[] cols = {
			Messages.getString("CredTab.0"),
			Messages.getString("CredTab.1"),
			Messages.getString("CredTab.2"),
			Messages.getString("CredTab.3")
	};
	private JTable table;
	
	CredTab(PmsConfiguration configuration) {
		this.configuration = configuration;
		cred = new PropertiesConfiguration();
		cred.setListDelimiter((char) 0);
		table = new JTable(1, cols.length) {
			@Override
			public boolean isCellEditable(int rowIndex, int vColIndex) {
				return false;
			}
		};
	}
	
	public void init() {
		File cFile = configuration.getCredFile();
		if (cFile.isFile() && FileUtil.isFileReadable(cFile)) {
			try {
				cred.load(cFile);
				cred.setFile(cFile);
			} catch (ConfigurationException e) {
				LOGGER.warn("Could not load cred file "+cFile);
			}
		}
		else {
			LOGGER.warn("Cred file unreadable "+cFile);
		}
		refresh(table, cols);
	}
	
	public JComponent build() {
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);

		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.DLU4_BORDER);
		builder.setOpaque(true);

		CellConstraints cc = new CellConstraints();
		
		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), FormLayoutUtil.flip(cc.xyw(1, 1, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		// Edit Plugin Credential File button
		CustomJButton credEdit = new CustomJButton(Messages.getString("NetworkTab.54"));
		credEdit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JPanel tPanel = new JPanel(new BorderLayout());
				String cPath = (String) PMS.getConfiguration().getCustomProperty("cred.path");

				if (StringUtils.isEmpty(cPath)) {
					cPath = (String) PMS.getConfiguration().getProfileDirectory() + File.separator + "UMS.cred";
					PMS.getConfiguration().setCustomProperty("cred.path", cPath);
				}

				final File cred = new File(cPath);
				final boolean newFile = !cred.exists();
				final JTextArea textArea = new JTextArea();
				textArea.setFont(new Font("Courier", Font.PLAIN, 12));
				JScrollPane scrollPane = new JScrollPane(textArea);
				scrollPane.setPreferredSize(new java.awt.Dimension(900, 450));

				if (!newFile) {
					try {
						FileInputStream fis = new FileInputStream(cred);
						BufferedReader in = new BufferedReader(new InputStreamReader(fis));
						String line;
						StringBuilder sb = new StringBuilder();
						while ((line = in.readLine()) != null) {
							sb.append(line);
							sb.append("\n");
						}
						textArea.setText(sb.toString());
						fis.close();
					} catch (Exception e1) {
						return;
					}
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append("# Add credentials to the file");
					sb.append("\n");
					sb.append("# on the format tag=user,pwd");
					sb.append("\n");
					sb.append("# For example:");
					sb.append("\n");
					sb.append("# channels.xxx=name,secret");
					sb.append("\n");
					textArea.setText(sb.toString());
				}

				tPanel.add(scrollPane, BorderLayout.NORTH);

				Object[] options = {Messages.getString("LooksFrame.9"), Messages.getString("NetworkTab.45")};
				if (
					JOptionPane.showOptionDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
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
						FileOutputStream fos = new FileOutputStream(cred);
						fos.write(text.getBytes());
						fos.flush();
						fos.close();
						PMS.getConfiguration().reload();
					} catch (Exception e1) {
						JOptionPane.showMessageDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())), Messages.getString("NetworkTab.55") + e1.toString());
					}
				}
			}
		});
		builder.add(credEdit, FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("CredTab.4"), FormLayoutUtil.flip(cc.xyw(1, 5, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		//refresh(table, cols);

		table.setRowHeight(22);
		table.setIntercellSpacing(new Dimension(8, 0));

		// Define column widths
		TableColumn nameColumn = table.getColumnModel().getColumn(0);
		nameColumn.setMinWidth(70);
		TableColumn versionColumn = table.getColumnModel().getColumn(2);
		versionColumn.setPreferredWidth(45);
		TableColumn ratingColumn = table.getColumnModel().getColumn(2);
		ratingColumn.setPreferredWidth(45);
		TableColumn authorColumn = table.getColumnModel().getColumn(3);
		authorColumn.setPreferredWidth(45);

		builder.add(table, FormLayoutUtil.flip(cc.xyw(1, 7, 9), colSpec, orientation));
		
		// Add an "Add..." button
		CustomJButton add = new CustomJButton(Messages.getString("CredTab.5"));
		builder.add(add, FormLayoutUtil.flip(cc.xy(1, 9), colSpec, orientation));
		add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addEditDialog(table,cols,-1);
			}				
		});
		
		// Edit button
		CustomJButton edit = new CustomJButton(Messages.getString("CredTab.7"));
		builder.add(edit, FormLayoutUtil.flip(cc.xy(3, 9), colSpec, orientation));
		edit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addEditDialog(table,cols,table.getSelectedRow());
			}
		});
		
		// Delete button
		CustomJButton del = new CustomJButton(Messages.getString("CredTab.8"));
		builder.add(del, FormLayoutUtil.flip(cc.xy(5, 9), colSpec, orientation));
		del.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rows = table.getSelectedRows();
				if (rows.length > 0) {
					int n = JOptionPane.showConfirmDialog(
							(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
							Messages.getString("CredTab.9"),
						    "",
						    JOptionPane.YES_NO_OPTION);
					if (n == JOptionPane.YES_OPTION) {
						for (int i=0; i < rows.length; i++) {
							String key = (String) table.getValueAt(rows[i], 0);
							if (StringUtils.isNotEmpty((String) table.getValueAt(rows[i], 1))) {
								key = key + "." + (String) table.getValueAt(rows[i], 1);
							}
							cred.clearProperty(key);
						}
					}
					try {
						cred.save();
					} catch (ConfigurationException e1) {
						LOGGER.warn("Couldn't save cred file "+e1);
					}
					refresh(table, cols);
				}
			}
		});
				
		
		JPanel panel = builder.getPanel();
		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}
	
	private void addEditDialog(final JTable table, final String[] cols,int row) {
		JPanel panel = new JPanel();
		GridLayout layout = new GridLayout(5, 2);
		panel.setLayout(layout);
		final JFrame frame = new JFrame(Messages.getString("CredTab.6"));
		frame.setSize(270, 130);
		final JLabel owner = new JLabel(Messages.getString("CredTab.0"));
		final JLabel tag = new JLabel(Messages.getString("CredTab.1"));
		final JLabel usr = new JLabel(Messages.getString("CredTab.2"));
		final JLabel pwd = new JLabel(Messages.getString("CredTab.3"));
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
		
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
				String key = oText.getText();
				String pwd = new String(pText.getPassword());
				if (StringUtils.isEmpty(key) || 
						StringUtils.isEmpty(uText.getText()) ||
						StringUtils.isEmpty(pwd)) {
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
					LOGGER.warn("Error saving cred file "+e1);
				}
				refresh(table, cols);
			}
		});
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
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
		panel.add(ok);
		panel.add(cancel);
		frame.add(panel);

		// Center the installation progress window
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private void refresh(JTable table, String[] cols) {
		for (int i = 0; i < cols.length; i++) {
			table.setValueAt(cols[i], 0, i);
		}
		
		cred.reload();

		DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
		tableModel.setRowCount(1);
		
		TableColumn tcol = table.getColumnModel().getColumn(3);
		tcol.setCellRenderer(new PasswordCellRenderer());

		Iterator itr = cred.getKeys();
		int i=1;
		while(itr.hasNext()) {
			String key = (String) itr.next();
			//String val = (String) cred.getProperty(key);
			Object val = cred.getProperty(key); 
			String[] ownerTag = key.split("\\.", 2);
			ArrayList<String> usrPwd = null;
			if (val instanceof String) {
				usrPwd = new ArrayList<String>();
				usrPwd.add((String) val);
			}
			else if (val instanceof List<?>) {
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
			}
			i++;
		}
		tableModel.fireTableDataChanged();
	}
	
	private class PasswordCellRenderer extends DefaultTableCellRenderer {
		public PasswordCellRenderer() {
		}

		public Component getTableCellRendererComponent(
				JTable  tab,
				Object obj,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int col) {
			
			Component c = super.getTableCellRendererComponent(tab, obj, isSelected, hasFocus, row, col);
			
			if(row == 0) {
				return c;
			}	
			
			JLabel l = (JLabel)c;
			if(StringUtils.isNotEmpty(l.getText())) {
				l.setText("**************");
			}
			return l;
		}
}

}
