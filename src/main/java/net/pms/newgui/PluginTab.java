package net.pms.newgui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.DownloadPlugins;
import net.pms.configuration.PmsConfiguration;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.util.FormLayoutUtil;
import org.apache.commons.lang.StringUtils;

public class PluginTab {
	private final PmsConfiguration configuration;
	private static final String COL_SPEC = "left:pref, 2dlu, p, 2dlu , p, 2dlu, p, 2dlu, pref:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p";
	private JPanel pPlugins;

	PluginTab(PmsConfiguration configuration) {
		this.configuration = configuration;
		pPlugins = null;
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

		// Cred edit
		JButton credEdit = new JButton(Messages.getString("NetworkTab.54"));
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

		JComponent availablePluginsHeading = builder.addSeparator(Messages.getString("PluginTab.1"), FormLayoutUtil.flip(cc.xyw(1, 5, 9), colSpec, orientation));
		availablePluginsHeading = (JComponent) availablePluginsHeading.getComponent(0);
		availablePluginsHeading.setFont(availablePluginsHeading.getFont().deriveFont(Font.BOLD));

		final ArrayList<DownloadPlugins> plugins = DownloadPlugins.downloadList();

		String[] cols = {
			Messages.getString("NetworkTab.41"),
			Messages.getString("NetworkTab.42"),
			Messages.getString("NetworkTab.43"),
			Messages.getString("NetworkTab.53")
		};

		final JTable table = new JTable(plugins.size() + 1, cols.length) {
			@Override
			public boolean isCellEditable(int rowIndex, int vColIndex) {
				return false;
			}

			@Override
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);

				if (rowIndex == 0) {
					return "";
				}

				DownloadPlugins plugin = plugins.get(rowIndex - 1);
				return plugin.htmlString();
			}
		};

		for (int i = 0; i < cols.length; i++) {
			table.setValueAt(cols[i], 0, i);
		}

		for (int i = 0; i < plugins.size(); i++) {
			DownloadPlugins p = plugins.get(i);
			table.setValueAt(p.getName(), i + 1, 0);
			table.setValueAt(p.getRating(), i + 1, 1);
			table.setValueAt(p.getAuthor(), i + 1, 2);
			table.setValueAt(p.getDescription(), i + 1, 3);
		}

		// Define column widths
		TableColumn nameColumn = table.getColumnModel().getColumn(0);
		nameColumn.setMinWidth(70);
		TableColumn ratingColumn = table.getColumnModel().getColumn(1);
		ratingColumn.setPreferredWidth(45);
		TableColumn authorColumn = table.getColumnModel().getColumn(2);
		authorColumn.setMinWidth(100);
		TableColumn descriptionColumn = table.getColumnModel().getColumn(3);
		descriptionColumn.setMinWidth(300);
		descriptionColumn.setMaxWidth(600);

		builder.add(table, FormLayoutUtil.flip(cc.xyw(1, 7, 9), colSpec, orientation));

		JButton install = new JButton(Messages.getString("NetworkTab.39"));
		builder.add(install, FormLayoutUtil.flip(cc.xy(1, 9), colSpec, orientation));
		install.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ExternalFactory.localPluginsInstalled()) {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("NetworkTab.40")
					);
					return;
				}

				if (!configuration.isAdmin()) {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						"UMS must be run as administrator in order to install plugins.",
						"Permissions Error",
						JOptionPane.ERROR_MESSAGE
					);

					return;
				}

				final int[] rows = table.getSelectedRows();
				JPanel panel = new JPanel();
				GridLayout layout = new GridLayout(3, 1);
				panel.setLayout(layout);
				final JFrame frame = new JFrame(Messages.getString("NetworkTab.46"));
				frame.setSize(250, 110);
				JProgressBar progressBar = new JProgressBar();
				progressBar.setIndeterminate(true);
				panel.add(progressBar);
				final JLabel label = new JLabel("");
				final JLabel inst = new JLabel("");
				panel.add(inst);
				panel.add(label);
				frame.add(panel);

				// Center the installation progress window
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				Runnable r = new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < rows.length; i++) {
							if (rows[i] == 0) {
								continue;
							}
							DownloadPlugins plugin = plugins.get(rows[i] - 1);
							inst.setText(Messages.getString("NetworkTab.50") + ": " + plugin.getName());
							try {
								plugin.install(label);
							} catch (Exception e) {
							}
						}
						frame.setVisible(false);
					}
				};
				new Thread(r).start();
			}
		});

		cmp = builder.addSeparator(Messages.getString("PluginTab.0"), FormLayoutUtil.flip(cc.xyw(1, 11, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		pPlugins = new JPanel(new GridLayout());
		builder.add(pPlugins, FormLayoutUtil.flip(cc.xyw(1, 13, 9), colSpec, orientation));

		JPanel panel = builder.getPanel();
		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		return scrollPane;
	}

	public void addPlugins() {
		FormLayout layout = new FormLayout(
			"fill:10:grow",
			"p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p");
		pPlugins.setLayout(layout);
		for (final ExternalListener listener : ExternalFactory.getExternalListeners()) {
			if (!appendPlugin(listener)) {
				break;
			}
		}
	}

	public boolean appendPlugin(final ExternalListener listener) {
		final JComponent comp = listener.config();
		if (comp == null) {
			return true;
		}
		CellConstraints cc = new CellConstraints();
		JButton bPlugin = new JButton(listener.name());
		// Listener to show option screen
		bPlugin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
					comp, "Options", JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
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
		JButton del = null;
		for (Component c : pPlugins.getComponents()) {
			if (c instanceof JButton) {
				JButton button = (JButton) c;
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
}
