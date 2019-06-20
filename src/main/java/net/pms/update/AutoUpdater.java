package net.pms.update;

import com.sun.jna.Platform;
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.util.Observable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.UriFileRetriever;
import net.pms.util.UriRetrieverCallback;
import net.pms.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks for and downloads new versions of PMS.
 *
 * @author Tim Cox (mail@tcox.org)
 */
public class AutoUpdater extends Observable implements UriRetrieverCallback {
	private static final String TARGET_FILENAME = "new-version.exe";
	private static final Logger LOGGER = LoggerFactory.getLogger(AutoUpdater.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	public static enum State {
		NOTHING_KNOWN, POLLING_SERVER, NO_UPDATE_AVAILABLE, UPDATE_AVAILABLE, DOWNLOAD_IN_PROGRESS, DOWNLOAD_FINISHED, EXECUTING_SETUP, ERROR
	}

	private final String serverUrl;
	private final UriFileRetriever uriRetriever = new UriFileRetriever();
	public static final AutoUpdaterServerProperties serverProperties = new AutoUpdaterServerProperties();
	private final Version currentVersion;
	private Executor executor = Executors.newSingleThreadExecutor();
	private State state = State.NOTHING_KNOWN;
	private Object stateLock = new Object();
	private Throwable errorStateCause;
	private int bytesDownloaded = -1;
	private int totalBytes = -1;
	private boolean downloadCancelled = false;

	public AutoUpdater(String updateServerUrl, String currentVersion) {
		this.serverUrl = updateServerUrl; // may be null if updating is disabled
		this.currentVersion = new Version(currentVersion);
	}

	public void pollServer() {
		if (serverUrl != null) { // don't poll if the server URL is null
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						doPollServer();
					} catch (UpdateException e) {
						setErrorState(e);
					}
				}
			});
		}
	}

	private void doPollServer() throws UpdateException {
		assertNotInErrorState();

		try {
			setState(State.POLLING_SERVER);
			byte[] propertiesAsData = uriRetriever.get(serverUrl);
			synchronized (stateLock) {
				serverProperties.loadFrom(propertiesAsData);
				setState(isUpdateAvailable() ? State.UPDATE_AVAILABLE : State.NO_UPDATE_AVAILABLE);
			}
		} catch (IOException e) {
			wrapException("Cannot download properties", e);
		}
	}

	public void getUpdateFromNetwork() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					doGetUpdateFromNetwork();
				} catch (UpdateException e) {
					setErrorState(e);
				}
			}
		});
	}

	public void runUpdateAndExit() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					doRunUpdateAndExit();
				} catch (UpdateException e) {
					setErrorState(e);
				}
			}
		});
	}

	private void setErrorState(UpdateException e) {
		synchronized (stateLock) {
			setState(State.ERROR);
			errorStateCause = e;
		}
	}

	private void doGetUpdateFromNetwork() throws UpdateException {
		assertNotInErrorState();
		assertUpdateIsAvailable();

		setState(State.DOWNLOAD_IN_PROGRESS);
		downloadUpdate();
		setState(State.DOWNLOAD_FINISHED);
	}

	private void doRunUpdateAndExit() throws UpdateException {
		synchronized (stateLock) {
			if (state == State.DOWNLOAD_FINISHED) {
				setState(State.EXECUTING_SETUP);
				launchExe();
				System.exit(0);
			}
		}
	}

	private void launchExe() throws UpdateException {
		try {
			File exe = new File(configuration.getProfileDirectory(), TARGET_FILENAME);
			Desktop desktop = Desktop.getDesktop();
			desktop.open(exe);
		} catch (IOException e) {
			LOGGER.debug("Failed to run update after downloading: {}", e);
			wrapException(Messages.getString("AutoUpdate.UnableToRunUpdate"), e);
		}
	}

	private void assertUpdateIsAvailable() throws UpdateException {
		synchronized (stateLock) {
			if (!serverProperties.isStateValid()) {
				throw new UpdateException("Server error. Try again later.");
			}

			if (!isUpdateAvailable()) {
				throw new UpdateException("Attempt to perform non-existent update");
			}
		}
	}

	private void assertNotInErrorState() throws UpdateException {
		synchronized (stateLock) {
			if (state == State.ERROR) {
				throw new UpdateException("Update system must be reset after an error.");
			}
		}
	}

	private void setState(State value) {
		synchronized (stateLock) {
			state = value;

			if (state == State.DOWNLOAD_FINISHED) {
				bytesDownloaded = totalBytes;
			} else if (state != State.DOWNLOAD_IN_PROGRESS) {
				bytesDownloaded = -1;
				totalBytes = -1;
			}

			if (state != State.ERROR) {
				errorStateCause = null;
			}
		}

		setChanged();
		notifyObservers();
	}

	public boolean isUpdateAvailable() {
		// TODO (tcox): Make updates work on Linux and Mac
		return Version.isPmsUpdatable(currentVersion, serverProperties.getLatestVersion());
	}

	private void downloadUpdate() throws UpdateException {
		String downloadUrl = serverProperties.getDownloadUrl();

		if (Platform.is64Bit()) {
			downloadUrl = downloadUrl.replace(".exe", "-standalone-x64.exe");
		} else {
			downloadUrl = downloadUrl.replace(".exe", "-standalone-x86.exe");
		}

		File target = new File(configuration.getProfileDirectory(), TARGET_FILENAME);

		try {
			uriRetriever.getFile(new URI(downloadUrl), target, this);
		} catch (Exception e) {
			// when the file download is canceled by user or an error happens
			// during downloading than delete the partially downloaded file
			target.delete();
			wrapException("Cannot download update", e);
		}
	}

	private void wrapException(String message, Throwable cause) throws UpdateException {
		throw new UpdateException("Error: " + message, cause);
	}

	@Override
	public void progressMade(String uri, int bytesDownloaded, int totalBytes) throws CancelDownloadException {
		synchronized (stateLock) {
			this.bytesDownloaded = bytesDownloaded;
			this.totalBytes = totalBytes;

			if (downloadCancelled) {
				setErrorState(new UpdateException("Download cancelled"));
				throw new CancelDownloadException();
			}
		}

		setChanged();
		notifyObservers();
	}

	public State getState() {
		synchronized (stateLock) {
			return state;
		}
	}

	public Throwable getErrorStateCause() {
		synchronized (stateLock) {
			return errorStateCause;
		}
	}

	public int getBytesDownloaded() {
		synchronized (stateLock) {
			return bytesDownloaded;
		}
	}

	public int getTotalBytes() {
		synchronized (stateLock) {
			return totalBytes;
		}
	}

	public void cancelDownload() {
		synchronized (stateLock) {
			downloadCancelled = true;
		}
	}

	public boolean isDownloadCancelled() {
		synchronized (stateLock) {
			return downloadCancelled;
		}
	}
}
