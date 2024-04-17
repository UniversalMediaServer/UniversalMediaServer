/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.store.container;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaTableWebResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.item.FeedItem;
import net.pms.store.item.RealFile;
import net.pms.store.item.WebAudioStream;
import net.pms.store.item.WebVideoStream;
import net.pms.store.utils.WebStreamMetadata;
import net.pms.store.utils.WebStreamMetadataCollector;
import net.pms.util.FileUtil;
import net.pms.util.HttpUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.UMSUtils;

public final class PlaylistFolder extends StoreContainer {

	private class Entry {
		private String fileName;
		private String title;

		@Override
		public String toString() {
			return "[" + fileName + "," + title + "]";
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistFolder.class);
	private final String uri;
	private final boolean isweb;
	private final int defaultContent;
	private boolean valid = true;

	public PlaylistFolder(Renderer renderer, String name, String uri, int type) {
		super(renderer, name, null);
		this.uri = uri;
		isweb = FileUtil.isUrl(uri);
		super.setLastModified(isweb ? 0 : new File(uri).lastModified());
		defaultContent = (type != 0 && type != Format.UNKNOWN) ? type : Format.VIDEO;
	}

	public PlaylistFolder(Renderer renderer, File f) {
		super(renderer, f.getName(), null);
		uri = f.getAbsolutePath();
		isweb = false;
		super.setLastModified(f.lastModified());
		defaultContent = Format.VIDEO;
	}

	public File getPlaylistfile() {
		if (isweb || FileUtil.isUrl(uri)) {
			return null;
		}
		return new File(uri);
	}

	@Override
	public boolean allowScan() {
		return true;
	}

	@Override
	public String getSystemName() {
		return isweb ? uri : ProcessUtil.getShortFileNameIfWideChars(uri);
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public void discoverChildren() {
		resolve();
	}

	private BufferedReader getBufferedReader() throws IOException {
		String extension;
		Charset charset;
		if (FileUtil.isUrl(uri)) {
			extension = FileUtil.getUrlExtension(uri);
		} else {
			extension = FileUtil.getExtension(uri);
		}
		if (extension != null) {
			extension = extension.toLowerCase(PMS.getLocale());
		}
		if (extension != null && (extension.equals("m3u8") || extension.equals(".cue"))) {
			charset = StandardCharsets.UTF_8;
		} else {
			charset = StandardCharsets.ISO_8859_1;
		}
		if (FileUtil.isUrl(uri)) {
			String body = "";
			try {
				body = HttpUtil.getStringBody(uri);
			} catch (IOException | InterruptedException e) {
				LOGGER.error("cannot retrieve external url", e);
			}
			StringReader sr = new StringReader(body);
			return new BufferedReader(sr);
		} else {
			File playlistfile = new File(uri);
			if (playlistfile.length() < 10000000) {
				FileInputStream inputStream = new FileInputStream(playlistfile);
				BOMInputStream bi = BOMInputStream.builder().setInputStream(inputStream).get();
				return new BufferedReader(new InputStreamReader(bi, charset));
			}
		}
		return null;
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		File thumbnailImage;
		if (!isweb) {
			thumbnailImage = new File(FilenameUtils.removeExtension(uri) + ".png");
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				thumbnailImage = new File(FilenameUtils.removeExtension(uri) + ".jpg");
			}
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				thumbnailImage = new File(FilenameUtils.getFullPath(uri) + "folder.png");
			}
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				thumbnailImage = new File(FilenameUtils.getFullPath(uri) + "folder.jpg");
			}
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				return super.getThumbnailInputStream();
			}
			DLNAThumbnailInputStream result = null;
			try {
				LOGGER.debug("PlaylistFolder albumart path : " + thumbnailImage.getAbsolutePath());
				result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(thumbnailImage));
			} catch (IOException e) {
				LOGGER.debug("An error occurred while getting thumbnail for \"{}\", using generic thumbnail instead: {}", getName(),
					e.getMessage());
				LOGGER.trace("", e);
			}
			return result != null ? result : super.getThumbnailInputStream();
		}
		return null;
	}

	@Override
	public void resolve() {
		getChildren().clear();
		if (getPlaylistfile() != null) {
			setLastModified(getPlaylistfile().lastModified());
		}
		resolveOnce();
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
							String valueType = line.substring(0, eq).toLowerCase();
							fileName = null;
							title = null;
							int index = 0;
							if (valueType.startsWith("file")) {
								index = Integer.parseInt(valueType.substring(4));
								fileName = value;
							} else if (valueType.startsWith("title")) {
								index = Integer.parseInt(valueType.substring(5));
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
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Adding " + (pls ? "PLS " : (m3u ? "M3U " : "")) + "entry: " + entry);
			}

			if (!FileUtil.isUrl(entry.fileName)) {
				resolveEntryAsLocalFile(entry);
			} else {
				// internet resource
				LOGGER.debug("entry is external link : {} ", uri);
				WebStreamMetadata meta = MediaTableWebResource.getWebStreamMetadata(entry.fileName);
				if (meta == null) {
					// retrieve meta and handle entry as local resource. Always done if we have a playlist entry within this playlist.
					LOGGER.debug("no metadata available. Getting metadata from stream {} ", entry.fileName);
					String ext = "." + FileUtil.getUrlExtension(entry.fileName);
					if (FormatFactory.getAssociatedFormat(ext) == null || FormatFactory.getAssociatedFormat(ext).getType() != Format.PLAYLIST) {
						WebStreamMetadataCollector.getInstance().collectMetadata(entry.fileName);
					}
					resolveEntryAsLocalFile(entry);
				} else {
					LOGGER.debug("using cached metadata for URL {} ", entry.fileName);
					StoreResource sr = meta.TYPE() == Format.VIDEO ?
						new WebVideoStream(renderer, entry.title, meta.URL(), meta.LOGO_URL()) :
						meta.TYPE() == Format.AUDIO ? new WebAudioStream(renderer, entry.title, meta.URL(), meta.LOGO_URL()) :
							meta.TYPE() == Format.IMAGE ?
								new FeedItem(renderer, entry.title, meta.URL(), meta.LOGO_URL(), null, Format.IMAGE) :
								meta.TYPE() == Format.PLAYLIST ? getPlaylist(renderer, entry.title, meta.URL(), 0) : null;
					if (sr instanceof WebAudioStream was) {
						addAudioInfo(was, meta);
					}
					if (sr != null) {
						addChild(sr);
						valid = true;
					}
				}
			}
		}

		for (StoreResource r : getChildren()) {
			r.syncResolve();
		}
	}

	private void addAudioInfo(WebAudioStream was, WebStreamMetadata meta) {
		if (was == null) {
			LOGGER.trace("web audio stream without meta data");
			return;
		}
		was.setMimeType(meta.CONTENT_TYPE());

		if (was.getMediaInfo() == null) {
			was.setMediaInfo(new MediaInfo());
		}
		if (was.getMediaAudio() == null) {
			was.setMediaAudio(new MediaAudio());
			was.getMediaInfo().setAudioMetadata(new MediaAudioMetadata());
		}
		if (was.getMediaAudio() == null) {
			was.setMediaAudio(new MediaAudio());
		}

		try {
			was.getMediaInfo().getAudioMetadata().setGenre(meta.GENRE());
			if (meta.BITRATE() != null) {
				was.getMediaInfo().setBitRate(meta.BITRATE());
			}
		} catch (Exception e) {
			LOGGER.debug("error setting values for mediaInfo", e);
		}

		try {
			if (meta.BITRATE() != null) {
				was.getMediaAudio().setBitRate(meta.BITRATE());
			}
			if (meta.SAMPLE_RATE() != null) {
				was.getMediaAudio().setSampleRate(meta.SAMPLE_RATE());
			}
		} catch (Exception e) {
			LOGGER.debug("error setting values for mediaAudio", e);
		}
	}


	private void resolveEntryAsLocalFile(Entry entry) {
		// local file entry
		if (entry.title == null) {
			entry.title = new File(entry.fileName).getName();
		}
		String ext = "." + FileUtil.getUrlExtension(entry.fileName);
		Format f = FormatFactory.getAssociatedFormat(ext);
		int type = f == null ? defaultContent : f.getType();

		if (!isweb && !FileUtil.isUrl(entry.fileName)) {
			File en = new File(FilenameUtils.concat(getPlaylistfile().getParent(), entry.fileName));
			if (en.exists()) {
				addChild(type == Format.PLAYLIST ? new PlaylistFolder(renderer, en) : new RealFile(renderer, en, entry.title));
				valid = true;
			}
		} else {
			String u = FileUtil.urlJoin(uri, entry.fileName);
			if (type == Format.PLAYLIST && !entry.fileName.endsWith(ext)) {
				// If the filename continues past the "extension" (i.e.
				// has
				// a query string) it's
				// likely not a nested playlist but a media item, for
				// instance Twitch TV media urls:
				// 'http://video10.iad02.hls.twitch.tv/.../index-live.m3u8?token=id=235...'
				type = defaultContent;
			}
			StoreResource d = type == Format.VIDEO ? new WebVideoStream(renderer, entry.title, u, null) :
				type == Format.AUDIO ? new WebAudioStream(renderer, entry.title, u, null) :
					type == Format.IMAGE ? new FeedItem(renderer, entry.title, u, null, null, Format.IMAGE) :
						type == Format.PLAYLIST ? getPlaylist(renderer, entry.title, u, 0) : null;
			if (d != null) {
				addChild(d);
				valid = true;
			}
		}
		if (!FileUtil.isUrl(entry.fileName)) {
			storeFileInCache(getPlaylistfile(), Format.PLAYLIST);
		}
		if (renderer.getUmsConfiguration().getSortMethod(getPlaylistfile()) == UMSUtils.SORT_RANDOM) {
			Collections.shuffle(getChildren());
		}
	}

	public static StoreContainer getPlaylist(Renderer renderer, String name, String uri, int type) {
		Format f = FormatFactory.getAssociatedFormat("." + FileUtil.getUrlExtension(uri));
		if (f != null && f.getType() == Format.PLAYLIST) {
			switch (f.getMatchedExtension()) {
				case "m3u", "m3u8", "pls" -> {
					return new PlaylistFolder(renderer, name, uri, type);
				}
				case "cue" -> {
					return FileUtil.isUrl(uri) ? null : new CueFolder(renderer, new File(uri));
				}
				case "ups" -> {
					return new Playlist(renderer, name, uri);
				}
				default -> {
					// nothing to do
				}
			}
		}
		return null;
	}

}
