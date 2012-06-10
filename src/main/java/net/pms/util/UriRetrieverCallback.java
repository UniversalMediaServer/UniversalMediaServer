package net.pms.util;

public interface UriRetrieverCallback {
	void progressMade(String uri, int bytesDownloaded, int totalBytes) throws CancelDownloadException;

	public class CancelDownloadException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}
