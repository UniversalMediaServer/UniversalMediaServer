package net.pms.network.api;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.RootFolder;

public class ApiHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

	private DLNAMediaDatabase database = PMS.get().getDatabase();

	public ApiHandler() {
	}

	/**
	 * Handle API calls
	 *
	 * @param method
	 * @param content
	 * @param response
	 * @param substring
	 */
	public void handleApiRequest(HttpMethod method, String content, StringBuilder response, String uri) {
		switch (uri) {
			case "rescan":
				rescanLibrary();
				break;
			case "rescanFileOrFolder":
				rescanLibraryFileOrFolder(content);
				break;
		}
	}

	/**
	 * Starts partial rescan
	 *
	 * @param filename This is the partial root of the scan. If a file is given,
	 *            the parent folder will be scanned.
	 */
	private void rescanLibraryFileOrFolder(String filename) {
		if (hasSameBasePath(PMS.getConfiguration().getSharedFolders(), filename) ||
			hasSameBasePath(RootFolder.getDefaultFolders(), filename)) {

			LOGGER.debug("rescanning file or folder : " + filename);

			if (!database.isScanLibraryRunning()) {
				Runnable scan = new Runnable() {

					@Override
					public void run() {
						File file = new File(filename);
						if (file.isFile()) {
							file = file.getParentFile();
						}
						DLNAResource dir = new RealFile(file);
						dir.setDefaultRenderer(RendererConfiguration.getDefaultConf());
						dir.doRefreshChildren();
						PMS.get().getRootFolder(null).scan(dir);
					}
				};
				Thread scanThread = new Thread(scan, "rescanLibraryFileOrFolder");
				scanThread.start();
			}
		} else {
			LOGGER.warn("given file or folder doesn't share same base path as this server : " + filename);
		}
	}

	private boolean hasSameBasePath(List<Path> dirs, String content) {
		for (Path path : dirs) {
			if (content.startsWith(path.toString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * rescan library
	 */
	private void rescanLibrary() {
		if (!database.isScanLibraryRunning()) {
			database.scanLibrary();
		} else {
			LOGGER.warn("library scan already in progress");
		}
	}
}
