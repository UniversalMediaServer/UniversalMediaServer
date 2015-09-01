package net.pms.fileprovider;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.newgui.LooksFrame;
import net.pms.newgui.NavigationShareTab;

public class FilesystemFileProvider implements FileProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemFileProvider.class);
	
	private boolean isActivated;
	
	private RootFolder rootFolder;
	private JComponent configurationPanel;

	@Override
	public JComponent getConfigurationPanel() {
		return configurationPanel;
	}

	@Override
	public DLNAResource getRootFolder() {
		return rootFolder;
	}

	@Override
	public String getName() {
		return Messages.getString("FilesystemFileProvider.1");
	}

	@Override
	public void activate() {		
		if(!isActivated) {
			LOGGER.debug(String.format("Start activating FileProvider %s", getName()));
			
			// Initialize the root folder
			rootFolder = new RootFolder();
			
			// Initialize the configuration panel
			NavigationShareTab navigationShareTab = new NavigationShareTab(PMS.getConfiguration());
			configurationPanel = navigationShareTab.build();
			
			isActivated = true;

			LOGGER.info(String.format("FileProvider %s has been activated", getName()));
		} else {
			LOGGER.info(String.format("FileProvider %s has already been activated", getName()));			
		}		
	}

	@Override
	public void deactivate() {		
		if(!isActivated) {
			LOGGER.debug(String.format("Start deactivating FileProvider %s", getName()));
			
			rootFolder = null;
			configurationPanel = null;
	
			isActivated = false;
			
			LOGGER.info(String.format("FileProvider %s has been deactivated", getName()));
		}
	}
}
