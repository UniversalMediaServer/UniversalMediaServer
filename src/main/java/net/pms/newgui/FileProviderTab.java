package net.pms.newgui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import net.pms.Messages;
import net.pms.fileprovider.FileProvider;
import net.pms.fileprovider.FileProviderFactory;
import net.pms.newgui.components.CustomComboBoxItem;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The file provider tab allows to select the file provider to use and to configure it.<br>
 * If no file provider plugins have been added to the plugins directory, the default file provider
 * {@link FileSystemFileProvider} will be used.
 */
public class FileProviderTab extends JPanel {
	private static final long serialVersionUID = -4011891950242053249L;	
	private final JComboBox<CustomComboBoxItem<FileProvider>> cbFileProviders = new JComboBox<CustomComboBoxItem<FileProvider>>();
	
	/**
	 * The Constructor.
	 */
	public FileProviderTab() {
		setLayout(new GridLayout());
		build();
	}

	private void build() {
		// Set basic layout
		FormLayout layout;
		PanelBuilder builder;

		CellConstraints cc = new CellConstraints();

		List<FileProvider> fileProviders = FileProviderFactory.getFileProviders();
		FileProvider activeFileProvider = FileProviderFactory.getActiveFileProvider();

		if (fileProviders.size() > 1) {
			layout = new FormLayout("3dlu, p, 3dlu, p, 3dlu, p, 0:g", // columns
					"3dlu, p, 3dlu, d, 0:g"); // rows
			builder = new PanelBuilder(layout);
			
			// Add the file provider selection section
			builder.addLabel(Messages.getString("FileProviderTab.2"), cc.xy(2, 2));
			cbFileProviders.removeAllItems();
			for (FileProvider fileProvider : fileProviders) {
				CustomComboBoxItem<FileProvider> cbItem = new CustomComboBoxItem<FileProvider>(fileProvider.getName(), fileProvider);
				cbFileProviders.addItem(cbItem);
				
				if(cbItem.getUserObject() == activeFileProvider) {
					// Select the active file provider in the combo box
					cbFileProviders.setSelectedItem(cbItem);
				}
			}
			builder.add(cbFileProviders, cc.xy(4, 2));
			JButton bUseFileProvider = new JButton(Messages.getString("FileProviderTab.1"));
			bUseFileProvider.addActionListener(new ActionListener() {				
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(ActionEvent e) {
					// Set the new file provider
					CustomComboBoxItem<FileProvider> selectedItem = (CustomComboBoxItem<FileProvider>)cbFileProviders.getSelectedItem();
					FileProviderFactory.setActiveFileProvider(selectedItem.getUserObject());
					
					// Rebuild the content of the panel to reflect the changes
					build();
				}
			});
			builder.add(bUseFileProvider, cc.xy(6, 2));

			// Add the active file provider panel
			JPanel pFileProvider = new JPanel(new BorderLayout());
			pFileProvider.setBorder(BorderFactory.createTitledBorder(activeFileProvider.getName()));
			pFileProvider.add(activeFileProvider.getConfigurationPanel(), BorderLayout.CENTER);
			builder.add(pFileProvider, cc.xyw(1, 4, 7));
		} else {
			layout = new FormLayout("3dlu, p, 3dlu, p, 3dlu, p, 0:g", // columns
					"d, 0:g"); // rows
			builder = new PanelBuilder(layout);
			
			// Only add the active (default) file provider panel, as there are no others to choose from
			builder.add(activeFileProvider.getConfigurationPanel(), cc.xyw(1, 1, 7));
		}

		removeAll();
		add(builder.build());
	}
}
