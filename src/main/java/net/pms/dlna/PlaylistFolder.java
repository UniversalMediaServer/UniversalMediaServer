package net.pms.dlna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.pms.PMS;
import net.pms.formats.Format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaylistFolder extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistFolder.class);
	private File playlistfile;
	private boolean valid = true;

	public File getPlaylistfile() {
		return playlistfile;
	}

	public PlaylistFolder(File f) {
		playlistfile = f;
		setLastmodified(playlistfile.lastModified());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return playlistfile.getName();
	}

	@Override
	public String getSystemName() {
		return playlistfile.getName();
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public synchronized void resolve() {
		if (playlistfile.length() < 10000000) {
			ArrayList<Entry> entries = new ArrayList<Entry>();
			boolean m3u = false;
			boolean pls = false;
			try {
				BufferedReader br = new BufferedReader(new FileReader(playlistfile));
				String line;
				while (!m3u && !pls && (line = br.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("#EXTM3U")) {
						m3u = true;
						LOGGER.debug("Reading m3u playlist: " + playlistfile.getName());
					} else if (line.length() > 0 && line.equals("[playlist]")) {
						pls = true;
						LOGGER.debug("Reading PLS playlist: " + playlistfile.getName());
					}
				}
				String fileName = null;
				String title = null;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (pls) {
						if (line.length() > 0 && !line.startsWith("#")) {
							int eq = line.indexOf("=");
							if (eq != -1) {
								String value = line.substring(eq + 1);
								String var = line.substring(0, eq).toLowerCase();
								fileName = null;
								title = null;
								int index = 0;
								if (var.startsWith("file")) {
									index = Integer.valueOf(var.substring(4));
									fileName = value;
								} else if (var.startsWith("title")) {
									index = Integer.valueOf(var.substring(5));
									title = value;
								}
								if (index > 0) {
									while (entries.size() < index) {
										entries.add(null);
									}
									Entry entry = entries.get(index - 1);
									if (entry == null) {
										entry = new Entry();
										entries.set(index - 1, entry);
									}
									if (fileName != null) {
										entry.fileName = fileName;
									}
									if (title != null) {
										entry.title = title;
									}
								}
							}
						}
					} else if (m3u) {
						if (line.startsWith("#EXTINF:")) {
							line = line.substring(8).trim();
							if (line.matches("^-?\\d+,.+")) {
								title = line.substring(line.indexOf(",") + 1).trim();
							} else {
								title = line;
							}
						} else if (!line.startsWith("#") && !line.matches("^\\s*$")) {
							// Non-comment and non-empty line contains the filename
							fileName = line;
							Entry entry = new Entry();
							entry.fileName = fileName;
							entry.title = title;
							entries.add(entry);
							title = null;
						}
					}
				}
				br.close();
			} catch (NumberFormatException e) {
				LOGGER.error(null, e);
			} catch (IOException e) {
				LOGGER.error(null, e);
			}
			for (Entry entry : entries) {
				if (entry == null) {
					continue;
				}
				String fileName = entry.fileName;
				LOGGER.debug("Adding " + (pls ? "PLS " : (m3u ? "M3U " : "")) + "entry: " + entry);
				if (!fileName.toLowerCase().startsWith("http://") && !fileName.toLowerCase().startsWith("mms://")) {
					File en1 = new File(playlistfile.getParentFile(), fileName);
					File en2 = new File(fileName);
					if (en1.exists()) {
						addChild(new RealFile(en1, entry.title));
						valid = true;
					} else {
						if (en2.exists()) {
							addChild(new RealFile(en2, entry.title));
							valid = true;
						}
					}
				}
			}
			PMS.get().storeFileInCache(playlistfile, Format.PLAYLIST);
			for (DLNAResource r : getChildren()) {
				r.resolve();
			}
		}
	}

	private static class Entry {
		public String fileName;
		public String title;

		@Override
		public String toString() {
			return "[" + fileName + "," + title + "]";
		}
	}
}
