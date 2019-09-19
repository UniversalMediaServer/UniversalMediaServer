package net.pms.dlna;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import net.pms.PMS;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaylistFolder extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistFolder.class);
	private String name;
	private String uri;
	private boolean valid = true;
	private boolean isweb = false;
	private int defaultContent = Format.VIDEO;

	public File getPlaylistfile() {
		return isweb ? null : new File(uri);
	}

	public PlaylistFolder(String name, String uri, int type) {
		this.name = name;
		this.uri = uri;
		isweb = FileUtil.isUrl(uri);
		setLastModified(isweb ? 0 : getPlaylistfile().lastModified());
		defaultContent = (type != 0 && type != Format.UNKNOWN) ? type : Format.VIDEO;
	}

	public PlaylistFolder(File f) {
		name = f.getName();
		uri = f.getAbsolutePath();
		isweb = false;
		setLastModified(f.lastModified());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSystemName() {
		return isweb ? uri : ProcessUtil.getShortFileNameIfWideChars(uri);
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

	private BufferedReader getBufferedReader() throws IOException {
		String extension;
		Charset charset;
		if (FileUtil.isUrl(uri)) {
			extension = FileUtil.getUrlExtension(uri).toLowerCase(PMS.getLocale());
		} else {
			extension = FileUtil.getExtension(uri).toLowerCase(PMS.getLocale());
		}
		if (extension != null && (extension.equals("m3u8") || extension.equals(".cue"))) {
			charset = StandardCharsets.UTF_8;
		} else {
			charset = StandardCharsets.ISO_8859_1;
		}
		if (FileUtil.isUrl(uri)) {
			return new BufferedReader(new InputStreamReader(new BOMInputStream(new URL(uri).openStream()), charset));
		} else {
			File playlistfile = new File(uri);
			if (playlistfile.length() < 10000000) {
				return new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(playlistfile)), charset));
			}
		}
		return null;
	}

	@Override
	protected void resolveOnce() {
		ArrayList<Entry> entries = new ArrayList<>();
		boolean m3u = false;
		boolean pls = false;
		try (BufferedReader br = getBufferedReader()) {
			String line;
			while (!m3u && !pls && (line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#EXTM3U")) {
					m3u = true;
					LOGGER.debug("Reading m3u playlist: " + getName());
				} else if (line.length() > 0 && line.equals("[playlist]")) {
					pls = true;
					LOGGER.debug("Reading PLS playlist: " + getName());
				}
			}
			String fileName;
			String title = null;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (pls) {
					if (line.length() > 0 && !line.startsWith("#")) {
						int eq = line.indexOf('=');
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
							title = line.substring(line.indexOf(',') + 1).trim();
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
		} catch (NumberFormatException | IOException e) {
			LOGGER.error(null, e);
		}

		for (Entry entry : entries) {
			if (entry == null) {
				continue;
			}
			if (entry.title == null) {
				entry.title = new File(entry.fileName).getName();
			}
			LOGGER.debug("Adding " + (pls ? "PLS " : (m3u ? "M3U " : "")) + "entry: " + entry);

			String ext = "." + FileUtil.getUrlExtension(entry.fileName);
			Format f = FormatFactory.getAssociatedFormat(ext);
			int type = f == null ? defaultContent : f.getType();

			if (! isweb && ! FileUtil.isUrl(entry.fileName)) {
				File en = new File(FilenameUtils.concat(getPlaylistfile().getParent(), entry.fileName));
				if (en.exists()) {
					addChild(type == Format.PLAYLIST ? new PlaylistFolder(en) : new RealFile(en, entry.title));
					valid = true;
				}
			} else {
				String u = FileUtil.urlJoin(uri, entry.fileName);
				if (type == Format.PLAYLIST && ! entry.fileName.endsWith(ext)) {
					// If the filename continues past the "extension" (i.e. has a query string) it's
					// likely not a nested playlist but a media item, for instance Twitch TV media urls:
					//    'http://video10.iad02.hls.twitch.tv/.../index-live.m3u8?token=id=235...'
					type = defaultContent;
				}
				DLNAResource d =
					type == Format.VIDEO ? new WebVideoStream(entry.title, u, null) :
					type == Format.AUDIO ? new WebAudioStream(entry.title, u, null) :
					type == Format.IMAGE ? new FeedItem(entry.title, u, null, null, Format.IMAGE) :
					type == Format.PLAYLIST ? getPlaylist(entry.title, u, 0) : null;
				if (d != null) {
					addChild(d);
					valid = true;
				}
			}
		}
		if (! isweb) {
			PMS.get().storeFileInCache(getPlaylistfile(), Format.PLAYLIST);
		}
		if (configuration.getSortMethod(getPlaylistfile()) == UMSUtils.SORT_RANDOM) {
			Collections.shuffle(getChildren());
		}

		for (DLNAResource r : getChildren()) {
			r.syncResolve();
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

	public static DLNAResource getPlaylist(String name, String uri, int type) {
		Format f = FormatFactory.getAssociatedFormat("." + FileUtil.getUrlExtension(uri));
		if (f != null && f.getType() == Format.PLAYLIST) {
			switch (f.getMatchedExtension()) {
				case "m3u":
				case "m3u8":
				case "pls":
					return new PlaylistFolder(name, uri, type);
				case "cue":
					return FileUtil.isUrl(uri) ? null : new CueFolder(new File(uri));
				case "ups":
					return new Playlist(name, uri);
			}
		}
		return null;
	}
}
