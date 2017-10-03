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
package net.pms.dlna;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import com.sun.jna.Platform;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import net.pms.PMS;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealFile extends MapFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealFile.class);

	private boolean useSuperThumb;

	public RealFile(File file) {
		getConf().getFiles().add(file);
		setLastModified(file.lastModified());
		useSuperThumb = false;
	}

	public RealFile(File file, String name) {
		getConf().getFiles().add(file);
		getConf().setName(name);
		setLastModified(file.lastModified());
		useSuperThumb = false;
	}

	@Override
	// FIXME: this is called repeatedly for invalid files e.g. files MediaInfo can't parse
	public boolean isValid() {
		File file = this.getFile();
		if (!file.isDirectory()) {
			resolveFormat();
		}

		if (getType() == Format.SUBTITLE) {
			// Don't add subtitles as separate resources
			return false;
		}
		if (getType() == Format.VIDEO && file.exists() && configuration.isAutoloadExternalSubtitles() && file.getName().length() > 4) {
			setHasExternalSubtitles(FileUtil.isSubtitlesExists(file, null));
		}

		boolean valid = file.exists() && (getFormat() != null || file.isDirectory());
		if (valid && getParent() != null && getParent().getDefaultRenderer() != null && getParent().getDefaultRenderer().isUseMediaInfo()) {
			// we need to resolve the DLNA resource now
			run();

			// Given that here getFormat() has already matched some (possibly plugin-defined) format:
			//    Format.UNKNOWN + bad parse = inconclusive
			//    known types    + bad parse = bad/encrypted file
			if (getType() != Format.UNKNOWN && getMedia() != null && (getMedia().isEncrypted() || getMedia().getContainer() == null || getMedia().getContainer().equals(DLNAMediaLang.UND))) {
				valid = false;
				if (getMedia().isEncrypted()) {
					LOGGER.info("The file {} is encrypted. It will be hidden", file.getAbsolutePath());
				} else {
					LOGGER.info("The file {} could not be parsed. It will be hidden", file.getAbsolutePath());
				}
			}

			// XXX isMediaInfoThumbnailGeneration is only true for the "default renderer"
			if (getParent().getDefaultRenderer().isMediaInfoThumbnailGeneration()) {
				checkThumbnail();
			}
		}

		return valid;
	}

	@Override
	public InputStream getInputStream() {
		try {
			return new FileInputStream(getFile());
		} catch (FileNotFoundException e) {
			LOGGER.debug("File not found: {}", getFile().getAbsolutePath());
		}

		return null;
	}

	@Override
	public long length() {
		if (getPlayer() != null && getPlayer().type() != Format.IMAGE) {
			return DLNAMediaInfo.TRANS_SIZE;
		} else if (getMedia() != null && getMedia().isMediaparsed()) {
			return getMedia().getSize();
		}
		return getFile().length();
	}

	@Override
	public boolean isFolder() {
		return getFile().isDirectory();
	}

	public File getFile() {
		return getConf().getFiles().get(0);
	}

	@Override
	public String getName() {
		if (this.getConf().getName() == null) {
			String name = null;
			File file = getFile();
			if (file.getName().trim().isEmpty()) {
				if (Platform.isWindows()) {
					name = PMS.get().getRegistry().getDiskLabel(file);
				}
				if (name != null && name.length() > 0) {
					name = file.getAbsolutePath().substring(0, 1) + ":\\ [" + name + "]";
				} else {
					name = file.getAbsolutePath().substring(0, 1);
				}
			} else {
				name = file.getName();
			}
			this.getConf().setName(name);
		}
		return this.getConf().getName().replaceAll("_imdb([^_]+)_", "");
	}

	@Override
	protected void resolveFormat() {
		if (getFormat() == null) {
			setFormat(FormatFactory.getAssociatedFormat(getFile().getAbsolutePath()));
		}

		super.resolveFormat();
	}

	@Override
	public String getSystemName() {
		return ProcessUtil.getShortFileNameIfWideChars(getFile().getAbsolutePath());
	}

	@Override
	public synchronized void resolve() {
		File file = getFile();
		if (file.isFile() && (getMedia() == null || !getMedia().isMediaparsed())) {
			boolean found = false;
			InputFile input = new InputFile();
			input.setFile(file);
			String fileName = file.getAbsolutePath();
			if (getSplitTrack() > 0) {
				fileName += "#SplitTrack" + getSplitTrack();
			}

			if (configuration.getUseCache()) {
				DLNAMediaDatabase database = PMS.get().getDatabase();

				if (database != null) {
					ArrayList<DLNAMediaInfo> medias;
					try {
						medias = database.getData(fileName, file.lastModified());

						if (medias.size() == 1) {
							setMedia(medias.get(0));
							getMedia().postParse(getType(), input);
							found = true;
						} else if (medias.size() > 1) {
							LOGGER.warn(
								"Found {} cached records for {} - this should be impossible, please file a bug report",
								medias.size(),
								getName()
							);
						}
					} catch (InvalidClassException e) {
						LOGGER.debug("Cached information about {} seems to be from a previous version, reparsing information", getName());
						LOGGER.trace("", e);
					} catch (IOException | SQLException e) {
						LOGGER.debug("Error while getting cached information about {}, reparsing information: {}", getName(), e.getMessage());
						LOGGER.trace("", e);
					}

				}
			}

			if (!found) {
				if (getMedia() == null) {
					setMedia(new DLNAMediaInfo());
				}

				if (getFormat() != null) {
					getFormat().parse(getMedia(), input, getType(), getParent().getDefaultRenderer());
					if (getMedia() != null && getMedia().isSLS()) {
						setFormat(getMedia().getAudioVariantFormat());
					}
				} else {
					// Don't think that will ever happen
					getMedia().parse(input, getFormat(), getType(), false, isResume(), getParent().getDefaultRenderer());
				}

				if (configuration.getUseCache() && getMedia().isMediaparsed() && !getMedia().isParsing()) {
					DLNAMediaDatabase database = PMS.get().getDatabase();

					if (database != null) {
						try {
							database.insertOrUpdateData(fileName, file.lastModified(), getType(), getMedia());
						} catch (SQLException e) {
							LOGGER.error(
								"Database error while trying to add parsed information for \"{}\" to the cache: {}",
								fileName,
								e.getMessage());
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("SQL error code: {}", e.getErrorCode());
								if (
									e.getCause() instanceof SQLException &&
									((SQLException) e.getCause()).getErrorCode() != e.getErrorCode()
								) {
									LOGGER.trace("Cause SQL error code: {}", ((SQLException) e.getCause()).getErrorCode());
								}
								LOGGER.trace("", e);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (useSuperThumb || getParent() instanceof FileTranscodeVirtualFolder && (getMediaSubtitle() != null || getMediaAudio() != null)) {
			return super.getThumbnailInputStream();
		}

		File file = getFile();
		File cachedThumbnail = null;
		MediaType mediaType = getMedia() != null ? getMedia().getMediaType() : MediaType.UNKNOWN;

		if (mediaType == MediaType.AUDIO || mediaType == MediaType.VIDEO) {
			String alternativeFolder = configuration.getAlternateThumbFolder();
			ArrayList<File> folders = new ArrayList<File>(2);
			if (file.getParentFile() != null) {
				folders.add(null);
			}
			if (isNotBlank(alternativeFolder)) {
				File thumbFolder = new File(alternativeFolder);
				if (thumbFolder.isDirectory() && thumbFolder.exists()) {
					folders.add(thumbFolder);
				}
			}

			for (File folder : folders) {
				File audioVideoFile = folder == null ? file : new File(folder, file.getName());
				HashSet<File> potentials = MapFile.getPotentialFileThumbnails(audioVideoFile, true);
				if (!potentials.isEmpty()) {
					// We have no rules for how to pick a particular one if there's multiple candidates
					cachedThumbnail = potentials.iterator().next();
					break;
				}
			}
			if (cachedThumbnail == null && mediaType == MediaType.AUDIO && getParent() != null && getParent() instanceof MapFile) {
				cachedThumbnail = ((MapFile) getParent()).getPotentialCover();
			}
		}

		if (file.isDirectory()) {
			cachedThumbnail = MapFile.getFolderThumbnail(file);
		}

		boolean hasAlreadyEmbeddedCoverArt = getType() == Format.AUDIO && getMedia() != null && getMedia().getThumb() != null;

		DLNAThumbnailInputStream result = null;
		try {
			if (cachedThumbnail != null && (!hasAlreadyEmbeddedCoverArt || file.isDirectory())) {
				result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(cachedThumbnail));
			} else if (getMedia() != null && getMedia().getThumb() != null) {
				result = getMedia().getThumbnailInputStream();
			}
		} catch (IOException e) {
			LOGGER.debug("An error occurred while getting thumbnail for \"{}\", using generic thumbnail instead: {}", getName(), e.getMessage());
			LOGGER.trace("", e);
		}
		return result != null ? result : super.getThumbnailInputStream();
	}

	@Override
	public void checkThumbnail() {
		InputFile input = new InputFile();
		input.setFile(getFile());
		checkThumbnail(input, getParent().getDefaultRenderer());
	}

	@Override
	protected String getThumbnailURL(DLNAImageProfile profile) {
		if (getType() == Format.IMAGE && !configuration.getImageThumbnailsEnabled()) {
			return null;
		}
		return super.getThumbnailURL(profile);
	}

	@Override
	public boolean isSubSelectable() {
		return true;
	}

	@Override
	public String write() {
		return getName() + ">" + getFile().getAbsolutePath();
	}

	public void ignoreThumbHandling() {
		useSuperThumb = true;
	}
}
