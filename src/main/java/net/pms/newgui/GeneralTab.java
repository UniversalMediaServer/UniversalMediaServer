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

import java.awt.BorderLayout;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.DownloadPlugins;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.network.NetworkConfiguration;
import net.pms.util.FormLayoutUtil;
import net.pms.util.KeyedComboBoxModel;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(GeneralTab.class);

	private static final String COL_SPEC = "left:pref, 2dlu, p, 2dlu , p, 2dlu, p, 2dlu, pref:grow";
	private static final String ROW_SPEC = "p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, 3dlu,p, 3dlu, p, 15dlu, p, 3dlu,p, 3dlu, p,  3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p,3dlu, p, 3dlu, p, 15dlu, p,3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p";

	private JCheckBox smcheckBox;
	private JCheckBox autoUpdateCheckBox;
	private JCheckBox newHTTPEngine;
	private JCheckBox preventSleep;
	private JTextField host;
	private JTextField port;
	private JComboBox langs;
	private JComboBox networkinterfacesCBX;
	private JTextField ip_filter;
	private JTextField maxbitrate;
	private JComboBox renderers;
	private JPanel pPlugins;
	private final PmsConfiguration configuration;

	GeneralTab(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public JComponent build() {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);

		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.DLU4_BORDER);
		builder.setOpaque(true);

		CellConstraints cc = new CellConstraints();

		smcheckBox = new JCheckBox(Messages.getString("NetworkTab.3"));
		smcheckBox.setContentAreaFilled(false);
		smcheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setMinimized((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		if (configuration.isMinimized()) {
			smcheckBox.setSelected(true);
		}

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"),
				FormLayoutUtil.flip(cc.xyw(1, 1, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
		builder.addLabel(Messages.getString("NetworkTab.0"), 
				FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));
		final KeyedComboBoxModel kcbm = new KeyedComboBoxModel(new Object[] {
				"ar", "bg", "ca", "zhs", "zht", "cz", "da", "nl", "en", "fi", "fr",
				"de", "el", "iw", "is", "it", "ja", "ko", "no", "pl", "pt", "br",
				"ro", "ru", "sl", "es", "sv", "tr" }, new Object[] {
				"Arabic", "Bulgarian", "Catalan", "Chinese (Simplified)",
				"Chinese (Traditional)", "Czech", "Danish", "Dutch", "English",
				"Finnish", "French", "German", "Greek", "Hebrew", "Icelandic", "Italian",
				"Japanese", "Korean", "Norwegian", "Polish", "Portuguese",
				"Portuguese (Brazilian)", "Romanian", "Russian", "Slovenian",
				"Spanish", "Swedish", "Turkish" });
		langs = new JComboBox(kcbm);
		langs.setEditable(false);
		String defaultLang = null;
		if (configuration.getLanguage() != null && configuration.getLanguage().length() > 0) {
			defaultLang = configuration.getLanguage();
		} else {
			defaultLang = Locale.getDefault().getLanguage();
		}
		if (defaultLang == null) {
			defaultLang = "en";
		}
		kcbm.setSelectedKey(defaultLang);
		if (langs.getSelectedIndex() == -1) {
			langs.setSelectedIndex(0);
		}

		langs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					configuration.setLanguage((String) kcbm.getSelectedKey());

				}
			}
		});

		builder.add(langs, FormLayoutUtil.flip(cc.xyw(3, 7, 7), colSpec, orientation));

		builder.add(smcheckBox, FormLayoutUtil.flip(cc.xyw(1, 9, 9), colSpec, orientation));

		JButton service = new JButton(Messages.getString("NetworkTab.4"));
		service.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (PMS.get().installWin32Service()) {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("NetworkTab.11") +
						Messages.getString("NetworkTab.12"),
						"Information",
						JOptionPane.INFORMATION_MESSAGE);

				} else {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("NetworkTab.14"),
						"Error",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		builder.add(service, FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));

		if (System.getProperty(LooksFrame.START_SERVICE) != null || !Platform.isWindows()) {
			service.setEnabled(false);
		}

		JButton checkForUpdates = new JButton(Messages.getString("NetworkTab.8"));

		checkForUpdates.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LooksFrame frame = (LooksFrame) PMS.get().getFrame();
				frame.checkForUpdates();
			}
		});

		builder.add(checkForUpdates, FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));

		autoUpdateCheckBox = new JCheckBox(Messages.getString("NetworkTab.9"));
		autoUpdateCheckBox.setContentAreaFilled(false);
		autoUpdateCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setAutoUpdate((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		if (configuration.isAutoUpdate()) {
			autoUpdateCheckBox.setSelected(true);
		}

		builder.add(autoUpdateCheckBox, FormLayoutUtil.flip(cc.xyw(7, 13, 3), colSpec, orientation));

		if (!Build.isUpdatable()) {
			checkForUpdates.setEnabled(false);
			autoUpdateCheckBox.setEnabled(false);
		}
		
		// Add find plugin support here
		JButton checkForPlugins=new JButton(Messages.getString("NetworkTab.39"));
		checkForPlugins.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!ExternalFactory.localPluginsInstalled()) {
					JOptionPane.showMessageDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
															Messages.getString("NetworkTab.40"));
					return;
				}
				final ArrayList<DownloadPlugins> plugins=DownloadPlugins.downloadList();
				if(plugins.isEmpty())
					return;
				String[] cols = {Messages.getString("NetworkTab.41"), Messages.getString("NetworkTab.42"),
								Messages.getString("NetworkTab.43")};
				JTable tab=new JTable(plugins.size()+1,cols.length) {
					 public String getToolTipText(MouseEvent e) {
						 java.awt.Point p = e.getPoint();
						 int rowIndex = rowAtPoint(p);
						 if(rowIndex==0)
							 return "";
						 DownloadPlugins plugin=plugins.get(rowIndex-1);
						 return plugin.htmlString();
					 }
				};
				for(int i=0;i<cols.length;i++) {
					tab.setValueAt(cols[i], 0, i);
				}
				tab.setCellEditor(null);
				for(int i=0;i<plugins.size();i++) {
					DownloadPlugins p=plugins.get(i);
					tab.setValueAt(p.getName(), i+1, 0);
					tab.setValueAt(p.getRating(),i+1,1);
					tab.setValueAt(p.getAuthor(),i+1,2);
				}
				String[] opts={Messages.getString("NetworkTab.44"),Messages.getString("NetworkTab.45")};
				int id=JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())), 
							tab, "Plugins", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, null);
				if(id!=0) // cancel, do nothing
					return;
				// Install the stuff
				final int[] rows=tab.getSelectedRows();
				JPanel panel=new JPanel();
				GridLayout layout = new GridLayout(3,1);
				panel.setLayout(layout);
				final JFrame frame=new JFrame(Messages.getString("NetworkTab.46"));
				frame.setSize(250, 110);
				JProgressBar progressBar=new JProgressBar();
				progressBar.setIndeterminate(true);
				panel.add(progressBar);
				final JLabel label = new JLabel("");
				final JLabel inst = new JLabel("");
				panel.add(inst);
				panel.add(label);
				frame.add(panel);
				frame.setVisible(true);
				Runnable r=new Runnable() {
					public void run() {
						for(int i=0;i<rows.length;i++) {
							if(rows[i]==0)
								continue;
							DownloadPlugins plugin=plugins.get(rows[i]-1);
							inst.setText(Messages.getString("NetworkTab.50")+": "+plugin.getName());
							try {
								plugin.install(label);
							} catch (Exception e) {
								LOGGER.debug("download of plugin "+plugin.getName()+
										" failed "+e);
							}
						}
						frame.setVisible(false);
					}
				};
				new Thread(r).start();
			}
		});
		builder.add(checkForPlugins, FormLayoutUtil.flip(cc.xy(1, 14), colSpec, orientation));
		// Conf edit
		JButton confEdit=new JButton(Messages.getString("NetworkTab.51"));
		confEdit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JPanel tPanel = new JPanel(new BorderLayout());
				JPanel bPanel = new JPanel(new BorderLayout());
				final File conf = new File(PMS.getConfiguration().getProfilePath());
				final JTextArea textArea = new JTextArea();
				textArea.setFont(new Font("Courier", Font.PLAIN, 12));
				JScrollPane scrollPane = new JScrollPane(textArea);
				scrollPane.setPreferredSize(new java.awt.Dimension(900, 450));
				try {
					FileInputStream fis = new FileInputStream(conf);
					BufferedReader in = new BufferedReader(new InputStreamReader(fis)); 
					String line;
					StringBuffer sb = new StringBuffer();
					while ((line = in.readLine()) != null) {
						sb.append(line);
						sb.append("\n");
					}
					textArea.setText(sb.toString());
					fis.close();
				}
				catch (Exception e1) {
					return;
				}
				tPanel.add(scrollPane,BorderLayout.NORTH);
				Object[] options = { Messages.getString("LooksFrame.9"),  Messages.getString("NetworkTab.45")};
				if (JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						tPanel, Messages.getString("NetworkTab.51"), 
						JOptionPane.OK_CANCEL_OPTION, 
						JOptionPane.PLAIN_MESSAGE, null, options, null) == JOptionPane.OK_OPTION) {
					String text=textArea.getText();
					try {
						FileOutputStream fos = new FileOutputStream(conf);
						fos.write(text.getBytes());
						fos.flush();
						fos.close();
						PMS.getConfiguration().reload();
					} catch (Exception e1) {
						JOptionPane.showMessageDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
								Messages.getString("NetworkTab.52") + e1.toString());
						return;
								
					}
				}
			}
		});
		builder.add(confEdit, FormLayoutUtil.flip(cc.xy(7, 14), colSpec, orientation));

		host = new JTextField(configuration.getServerHostname());
		host.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setHostname(host.getText());
			}
		});

		port = new JTextField(configuration.getServerPort() != 5001 ? "" + configuration.getServerPort() : "");
		port.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				try {
					String p = port.getText();
					if (StringUtils.isEmpty(p)) {
						p = "5001";
					}
					int ab = Integer.parseInt(p);
					configuration.setServerPort(ab);
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse port from \"" + port.getText() + "\"");
				}

			}
		});

		cmp = builder.addSeparator(Messages.getString("NetworkTab.22"), FormLayoutUtil.flip(cc.xyw(1, 21, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		final KeyedComboBoxModel networkInterfaces = createNetworkInterfacesModel();
		networkinterfacesCBX = new JComboBox(networkInterfaces);
		networkInterfaces.setSelectedKey(configuration.getNetworkInterface());
		networkinterfacesCBX.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					configuration.setNetworkInterface((String) networkInterfaces.getSelectedKey());
				}
			}
		});

		ip_filter = new JTextField(configuration.getIpFilter());
		ip_filter.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setIpFilter(ip_filter.getText());
			}
		});

		maxbitrate = new JTextField(configuration.getMaximumBitrate());
		maxbitrate.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				PMS.getConfiguration().setMaximumBitrate(maxbitrate.getText());
			}
		});

		builder.addLabel(Messages.getString("NetworkTab.20"), FormLayoutUtil.flip(cc.xy(1, 23), colSpec, orientation));
		builder.add(networkinterfacesCBX, FormLayoutUtil.flip(cc.xyw(3, 23, 7), colSpec, orientation));
		builder.addLabel(Messages.getString("NetworkTab.23"), FormLayoutUtil.flip(cc.xy(1, 25), colSpec, orientation));
		builder.add(host, FormLayoutUtil.flip(cc.xyw(3, 25, 7), colSpec, orientation));
		builder.addLabel(Messages.getString("NetworkTab.24"), FormLayoutUtil.flip(cc.xy(1, 27), colSpec, orientation));
		builder.add(port, FormLayoutUtil.flip(cc.xyw(3, 27, 7), colSpec, orientation));
		builder.addLabel(Messages.getString("NetworkTab.30"), FormLayoutUtil.flip(cc.xy(1, 29), colSpec, orientation));
		builder.add(ip_filter, FormLayoutUtil.flip(cc.xyw(3, 29, 7), colSpec, orientation));
		builder.addLabel(Messages.getString("NetworkTab.35"), FormLayoutUtil.flip(cc.xy(1, 31), colSpec, orientation));
		builder.add(maxbitrate, FormLayoutUtil.flip(cc.xyw(3, 31, 7), colSpec, orientation));


		cmp = builder.addSeparator(Messages.getString("NetworkTab.31"), FormLayoutUtil.flip(cc.xyw(1, 33, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		newHTTPEngine = new JCheckBox(Messages.getString("NetworkTab.32"));
		newHTTPEngine.setSelected(configuration.isHTTPEngineV2());
		newHTTPEngine.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setHTTPEngineV2((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(newHTTPEngine, FormLayoutUtil.flip(cc.xyw(1, 35, 9), colSpec, orientation));

		preventSleep = new JCheckBox(Messages.getString("NetworkTab.33"));
		preventSleep.setSelected(configuration.isPreventsSleep());
		preventSleep.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setPreventsSleep((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(preventSleep, FormLayoutUtil.flip(cc.xyw(1, 37, 9), colSpec, orientation));

		JCheckBox fdCheckBox = new JCheckBox(Messages.getString("NetworkTab.38"));
		fdCheckBox.setContentAreaFilled(false);
		fdCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setRendererForceDefault((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		
		if (configuration.isRendererForceDefault()) {
			fdCheckBox.setSelected(true);
		}

		builder.addLabel(Messages.getString("NetworkTab.36"), FormLayoutUtil.flip(cc.xy(1, 39), colSpec, orientation));
		
		ArrayList<RendererConfiguration> allConfs = RendererConfiguration.getAllRendererConfigurations();
		ArrayList<Object> keyValues = new ArrayList<Object>();
		ArrayList<Object> nameValues = new ArrayList<Object>();
		keyValues.add("");
		nameValues.add(Messages.getString("NetworkTab.37"));

		if (allConfs != null) {
			for (RendererConfiguration renderer : allConfs) {
				if (renderer != null) {
					keyValues.add(renderer.getRendererName());
					nameValues.add(renderer.getRendererName());
				}
			}
		}

		final KeyedComboBoxModel renderersKcbm = new KeyedComboBoxModel(
				(Object[]) keyValues.toArray(new Object[keyValues.size()]),
				(Object[]) nameValues.toArray(new Object[nameValues.size()]));
		renderers = new JComboBox(renderersKcbm);
		renderers.setEditable(false);
		String defaultRenderer = configuration.getRendererDefault();
		renderersKcbm.setSelectedKey(defaultRenderer);

		if (renderers.getSelectedIndex() == -1) {
			renderers.setSelectedIndex(0);
		}

		builder.add(renderers, FormLayoutUtil.flip(cc.xyw(3, 39, 7), colSpec, orientation));

		builder.add(fdCheckBox, FormLayoutUtil.flip(cc.xyw(1, 41, 9), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("NetworkTab.34"), FormLayoutUtil.flip(cc.xyw(1, 43, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		pPlugins = new JPanel(new GridLayout());
		builder.add(pPlugins, FormLayoutUtil.flip(cc.xyw(1, 45, 9), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);
		
		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		return scrollPane;
	}

	private KeyedComboBoxModel createNetworkInterfacesModel() {
		List<String> keys = NetworkConfiguration.getInstance().getKeys();
		List<String> names = NetworkConfiguration.getInstance().getDisplayNames();
		keys.add(0, "");
		names.add(0, "");
		final KeyedComboBoxModel networkInterfaces = new KeyedComboBoxModel(keys.toArray(), names.toArray());
		return networkInterfaces;
	}

	/**
	 * Add the renderer configuration selection after they have been intialized.
	 */
	public void addRenderers() {
		ArrayList<RendererConfiguration> allConfs = RendererConfiguration.getAllRendererConfigurations();
		ArrayList<Object> keyValues = new ArrayList<Object>();
		ArrayList<Object> nameValues = new ArrayList<Object>();
		keyValues.add("");
		nameValues.add(Messages.getString("NetworkTab.37"));
		
		if (allConfs != null) {
			for (RendererConfiguration renderer : allConfs) {
				if (renderer != null) {
					keyValues.add(renderer.getRendererName());
					nameValues.add(renderer.getRendererName());
				}
			}
		}
		
		final KeyedComboBoxModel renderersKcbm = new KeyedComboBoxModel(
				(Object[]) keyValues.toArray(new Object[keyValues.size()]),
				(Object[]) nameValues.toArray(new Object[nameValues.size()]));
		renderers.setModel(renderersKcbm);
		renderers.setEditable(false);
		String defaultRenderer = configuration.getRendererDefault();
		renderersKcbm.setSelectedKey(defaultRenderer);
		
		if (renderers.getSelectedIndex() == -1) {
			renderers.setSelectedIndex(0);
		}

		renderers.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					LOGGER.info("Setting renderer default: \"" + renderersKcbm.getSelectedKey() + "\"");
					configuration.setRendererDefault((String) renderersKcbm.getSelectedKey());
				}
			}
		});
	}

	public void addPlugins() {
		FormLayout layout = new FormLayout(
				"fill:10:grow",
		"p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p, p");
		pPlugins.setLayout(layout);
		for (final ExternalListener listener : ExternalFactory.getExternalListeners()) {
			if(!appendPlugin(listener)) {
				LOGGER.warn("Plugin limit of 30 has been reached");
				break;
			}
		}
	}
	
	public boolean appendPlugin(final ExternalListener listener) {
		final JComponent comp = listener.config();
		if(comp == null) {
			return true;
		}
		CellConstraints cc = new CellConstraints();
		JButton bPlugin = new JButton(listener.name());
		// listener to show option screen
		bPlugin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())), 
						comp, "Options", JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
			}	
		});	
		int y = pPlugins.getComponentCount() + 1;
		if(y > 30) {
			return false;
		}
		pPlugins.add(bPlugin, cc.xy(1, y));
		return true;
	}
	
	public void removePlugin(ExternalListener listener) {
		JButton del = null;
		for(Component c : pPlugins.getComponents()) {
			if(c instanceof JButton) {
				JButton button = (JButton)c;
				if(button.getText().equals(listener.name())) {
					del = button;
					break;
				}
			}
		}
		if(del != null) {
			pPlugins.remove(del);
			pPlugins.repaint();
		}
	}
}
