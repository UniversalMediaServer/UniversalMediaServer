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
import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
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
import net.pms.util.FileUtil;
import net.pms.util.HttpUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.UMSUtils;

public final class PlaylistFolder extends StoreContainer {

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
			if (!FileUtil.isUrl(uri)) {
				if (entry.title == null) {
					entry.title = new File(entry.fileName).getName();
				}
				LOGGER.debug("Adding " + (pls ? "PLS " : (m3u ? "M3U " : "")) + "entry: " + entry);

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
						// If the filename continues past the "extension" (i.e. has
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
				storeFileInCache(getPlaylistfile(), Format.PLAYLIST);
				if (renderer.getUmsConfiguration().getSortMethod(getPlaylistfile()) == UMSUtils.SORT_RANDOM) {
					Collections.shuffle(getChildren());
				}
			} else {
				LOGGER.debug("entry is external link : {} ", uri);
				int type = 0;
				String albumArtUrl = null;
				String ext = "." + FileUtil.getUrlExtension(entry.fileName);

				// TODO header information should be stored/cached in a table for faster access
				HttpHeaders headHeaders = HttpUtil.getHeaders(entry.fileName);
				HttpHeaders inputStreamHeaders = null;

				if (FileUtil.getUrlExtension(entry.fileName) == null) {
					// We have no file extension. Acquire type from "content-type"
					type = getTypeFrom(headHeaders, type);
					albumArtUrl = getAlbumArtUrlFrom(headHeaders);
					if (type == 0) {
						LOGGER.trace("HEAD request : NO content type set for resource {}. Trying GET request ...", entry.fileName);
						inputStreamHeaders = HttpUtil.getHeadersFromInputStreamRequest(entry.fileName);
						type = getTypeFrom(inputStreamHeaders, type);
						albumArtUrl = updateAlbumArtIfNotExists(inputStreamHeaders, albumArtUrl);
						if (type == 0) {
							LOGGER.warn("couldn't determine stream content type for.", entry.fileName);
						}
					}
				} else {
					Format f = FormatFactory.getAssociatedFormat(ext);
					type = f == null ? defaultContent : f.getType();
				}

				String u = FileUtil.urlJoin(uri, entry.fileName);
				if (type == Format.PLAYLIST && !entry.fileName.endsWith(ext)) {
					// If the filename continues past the "extension" (i.e. has
					// a query string) it's
					// likely not a nested playlist but a media item, for
					// instance Twitch TV media urls:
					// 'http://video10.iad02.hls.twitch.tv/.../index-live.m3u8?token=id=235...'
					type = defaultContent;
				} else {
					// check resource type
					if (StringUtils.isAllBlank(albumArtUrl)) {
						albumArtUrl = updateAlbumArtIfNotExists(headHeaders, albumArtUrl);
						if (StringUtils.isAllBlank(albumArtUrl)) {
							if (inputStreamHeaders == null) {
								inputStreamHeaders = HttpUtil.getHeadersFromInputStreamRequest(entry.fileName);
							}
							albumArtUrl = updateAlbumArtIfNotExists(inputStreamHeaders, albumArtUrl);
						}
					}
				}
				StoreResource d = type == Format.VIDEO ? new WebVideoStream(renderer, entry.title, u, albumArtUrl) :
					type == Format.AUDIO ? new WebAudioStream(renderer, entry.title, u, albumArtUrl) :
						type == Format.IMAGE ? new FeedItem(renderer, entry.title, u, albumArtUrl, null, Format.IMAGE) :
							type == Format.PLAYLIST ? getPlaylist(renderer, entry.title, u, 0) : null;
				if (d instanceof WebAudioStream was) {
					if (inputStreamHeaders != null) {
						addAudioFormat(was, inputStreamHeaders);
					} else {
						addAudioFormat(was, headHeaders);
					}
				}
				if (d != null) {
					addChild(d);
					valid = true;
				}
			}
		}

		for (StoreResource r : getChildren()) {
			r.syncResolve();
		}
	}

	private String updateAlbumArtIfNotExists(HttpHeaders headers, String albumArtUrl) {
		if (StringUtils.isAllBlank(albumArtUrl)) {
			return getAlbumArtUrlFrom(headers);
		}
		return null;
	}

	/*
	 * Extracts audio information from ice or icecast protocol.
	 */
	private void addAudioFormat(WebAudioStream was, HttpHeaders headers) {
		if (headers == null) {
			LOGGER.trace("web audio stream without header info.");
			return;
		}
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
		Optional<String> value = null;
		value = headers.firstValue("icy-br");
		if (value.isPresent()) {
			was.getMediaInfo().setBitRate(parseIntValue(value) != null ? parseIntValue(value) : null);
			was.getMediaAudio().setBitRate(parseIntValue(value) != null ? parseIntValue(value) : null);
		}
		value = headers.firstValue("icy-sr");
		if (value.isPresent()) {
			was.getMediaAudio().setSampleRate(parseIntValue(value) != null ? parseIntValue(value) : null);
		}

		value = headers.firstValue("icy-genre");
		if (value.isPresent()) {
			was.getMediaInfo().getAudioMetadata().setGenre(value.get());
		}

		value = headers.firstValue("content-type");
		if (value.isPresent()) {
			was.getMediaAudio().setCodec(value.get());
			was.setMimeType(value.get());
		}
	}

	private Integer parseIntValue(Optional<String> value) {
		try {
			return Integer.parseInt(value.get());
		} catch (Exception e) {
			return null;
		}
	}
	private String getAlbumArtUrlFrom(HttpHeaders headers) {
		// Icecast protocol support
		if (headers.firstValue("icy-logo").isEmpty()) {
			LOGGER.trace("icy-logo not set ...");
			return null;
		}
		return headers.firstValue("icy-logo").get();
	}

	private int getTypeFrom(HttpHeaders headers, int currentType) {
		if (headers.firstValue("content-type").isEmpty()) {
			LOGGER.trace("web server has no content type set ...");
			return currentType;
		} else {
			String contentType = headers.firstValue("content-type").get();
			if (contentType.startsWith("audio")) {
				return Format.AUDIO;
			} else if (contentType.startsWith("video")) {
				return Format.VIDEO;
			} else if (contentType.startsWith("image")) {
				return Format.IMAGE;
			} else {
				LOGGER.trace("web server has no content type set ...");
				return currentType;
			}
		}
	}

	private String extractAlbumArtFrom(HttpHeaders headers) {
		// Icecast protocol support
		return headers.firstValue("icy-name").get();
	}

	private String extractTitleFrom(HttpHeaders headers) {
		// Icecast protocol support
		return headers.firstValue("icy-name").get();
	}

	private static class Entry {

		private String fileName;
		private String title;

		@Override
		public String toString() {
			return "[" + fileName + "," + title + "]";
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
					//nothing to do
				}
			}
		}
		return null;
	}

}
