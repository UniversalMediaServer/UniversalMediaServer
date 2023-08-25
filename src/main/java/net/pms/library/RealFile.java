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
package net.pms.library;

import com.sun.jna.Platform;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.library.virtual.SubSelFile;
import net.pms.library.virtual.VirtualFile;
import net.pms.media.MediaInfoStore;
import net.pms.media.MediaLang;
import net.pms.media.MediaStatusStore;
import net.pms.media.MediaType;
import net.pms.media.subtitle.MediaOnDemandSubtitle;
import net.pms.parsers.FFmpegParser;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.util.FileUtil;
import net.pms.util.FullyPlayedAction;
import net.pms.util.InputFile;
import net.pms.util.ProcessUtil;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealFile extends VirtualFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealFile.class);

	public RealFile(Renderer renderer, File file) {
		super(renderer);
		addFileToFiles(file);
		setLastModified(file.lastModified());
	}

	public RealFile(Renderer renderer, File file, String name) {
		super(renderer);
		addFileToFiles(file);
		this.name = name;
		setLastModified(file.lastModified());
	}

	public RealFile(Renderer renderer, File file, boolean isEpisodeWithinSeasonFolder) {
		super(renderer);
		addFileToFiles(file);
		setLastModified(file.lastModified());
		setIsEpisodeWithinSeasonFolder(isEpisodeWithinSeasonFolder);
	}

	public RealFile(Renderer renderer, File file, boolean isEpisodeWithinSeasonFolder, boolean isEpisodeWithinTVSeriesFolder) {
		super(renderer);
		addFileToFiles(file);
		setLastModified(file.lastModified());
		setIsEpisodeWithinSeasonFolder(isEpisodeWithinSeasonFolder);
		setIsEpisodeWithinTVSeriesFolder(isEpisodeWithinTVSeriesFolder);
	}

	/**
	 * Add the file to Files.
	 *
	 * @param file The file to add.
	 */
	private void addFileToFiles(File file) {
		if (file == null) {
			throw new NullPointerException("file shall not be empty");
		}
		Boolean useSymlinks = renderer.getUmsConfiguration().isUseSymlinksTargetFile();
		if (useSymlinks && FileUtil.isSymbolicLink(file)) {
			getFiles().add(FileUtil.getRealFile(file));
		} else {
			getFiles().add(file);
		}
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
			getFiles().remove(file);
			return false;
		}

		boolean valid = file.exists() && (getFormat() != null || file.isDirectory());
		if (valid && getParent() != null && getParent().getDefaultRenderer() != null && getParent().getDefaultRenderer().isUseMediaInfo()) {
			// we need to resolve the library resource now
			run();

			// Given that here getFormat() has already matched some (possibly plugin-defined) format:
			//    Format.UNKNOWN + bad parse = inconclusive
			//    known types    + bad parse = bad/encrypted file
			if (this.getType() != Format.UNKNOWN && getMediaInfo() != null) {
				if (getMediaInfo().getDefaultVideoTrack() != null && getMediaInfo().getDefaultVideoTrack().isEncrypted()) {
					valid = false;
					LOGGER.info("The file {} is encrypted. It will be hidden", file.getAbsolutePath());
				} else if (getMediaInfo().getContainer() == null || getMediaInfo().getContainer().equals(MediaLang.UND)) {
					// problematic media not parsed by MediaInfo try to parse it in a different way by ffmpeg, AudioFileIO or ImagesUtil
					// this is a quick fix for the MediaInfo insufficient parsing method
					getMediaInfo().setMediaParser(null);
					InputFile inputfile = new InputFile();
					inputfile.setFile(file);
					getMediaInfo().setContainer(null);
					FFmpegParser.parse(getMediaInfo(), inputfile, getFormat(), getType());
					if (getMediaInfo().getContainer() == null) {
						valid = false;
						LOGGER.info("The file {} could not be parsed. It will be hidden", file.getAbsolutePath());
					}
					//TODO: this should update the db
				}

				if (!valid) {
					getFiles().remove(file);
				}
			}

			// XXX isMediaInfoThumbnailGeneration is only true for the "default renderer"
			if (getParent().getDefaultRenderer().isMediaInfoThumbnailGeneration()) {
				checkThumbnail();
			}
		} else if (this.getType() == Format.UNKNOWN && !this.isFolder()) {
			getFiles().remove(file);
			return false;
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
		if (getEngine() != null && getEngine().type() != Format.IMAGE) {
			return TRANS_SIZE;
		} else if (getMediaInfo() != null && getMediaInfo().isMediaParsed()) {
			return getMediaInfo().getSize();
		}
		return getFile().length();
	}

	@Override
	public boolean isFolder() {
		return getFile().isDirectory();
	}

	public File getFile() {
		if (getFiles().isEmpty()) {
			return null;
		}

		return getFiles().get(0);
	}

	@Override
	public String getName() {
		if (name == null) {
			File file = getFile();

			// this probably happened because the file was removed after it could not be parsed by isValid()
			if (file == null) {
				return null;
			}

			if (file.getName().trim().isEmpty()) {
				if (Platform.isWindows()) {
					name = PlatformUtils.INSTANCE.getDiskLabel(file);
				}
				if (name != null && name.length() > 0) {
					name = file.getAbsolutePath().substring(0, 1) + ":\\ [" + name + "]";
				} else {
					name = file.getAbsolutePath().substring(0, 1);
				}
			} else {
				name = file.getName();
			}
		}
		return name.replaceAll("_imdb([^_]+)_", "");
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
		if (file == null) {
			LOGGER.error("RealFile points to no physical file. ");
			return;
		}

		if (file.isFile() && (getMediaInfo() == null || !getMediaInfo().isMediaParsed())) {
			String filename = file.getAbsolutePath();
			if (getSplitTrack() > 0) {
				filename += "#SplitTrack" + getSplitTrack();
			}
			InputFile input = new InputFile();
			input.setFile(file);
			setMediaInfo(MediaInfoStore.getMediaInfo(filename, file, getFormat(), getType()));
			setMediaStatus(MediaStatusStore.getMediaStatus(renderer.getAccountUserId(), filename));
		}
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		File file = getFile();
		File cachedThumbnail = null;
		MediaType mediaType = getMediaInfo() != null ? getMediaInfo().getMediaType() : MediaType.UNKNOWN;

		if (mediaType == MediaType.AUDIO || mediaType == MediaType.VIDEO) {
			String alternativeFolder = renderer.getUmsConfiguration().getAlternateThumbFolder();
			ArrayList<File> folders = new ArrayList<>(2);
			if (file.getParentFile() != null) {
				folders.add(null);
			}
			if (StringUtils.isNotBlank(alternativeFolder)) {
				File thumbFolder = new File(alternativeFolder);
				if (thumbFolder.isDirectory() && thumbFolder.exists()) {
					folders.add(thumbFolder);
				}
			}

			for (File folder : folders) {
				File audioVideoFile = folder == null ? file : new File(folder, file.getName());
				Set<File> potentials = VirtualFile.getPotentialFileThumbnails(audioVideoFile, true);
				if (!potentials.isEmpty()) {
					// We have no rules for how to pick a particular one if there's multiple candidates
					cachedThumbnail = potentials.iterator().next();
					break;
				}
			}
			if (cachedThumbnail == null && mediaType == MediaType.AUDIO && getParent() instanceof VirtualFile virtualFile) {
				cachedThumbnail = virtualFile.getPotentialCover();
			}
		}

		if (file.isDirectory()) {
			cachedThumbnail = VirtualFile.getFolderThumbnail(file);
		}

		boolean hasAlreadyEmbeddedCoverArt = getType() == Format.AUDIO && getMediaInfo() != null && getMediaInfo().getThumb() != null;

		DLNAThumbnailInputStream result = null;
		try {
			if (cachedThumbnail != null && (!hasAlreadyEmbeddedCoverArt || file.isDirectory())) {
				result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(cachedThumbnail));
			} else if (getMediaInfo() != null && getMediaInfo().getThumb() != null) {
				result = getMediaInfo().getThumbnailInputStream();
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
		checkThumbnail(input);
	}

	@Override
	public String getThumbnailURL(DLNAImageProfile profile) {
		if (getType() == Format.IMAGE && !renderer.getUmsConfiguration().getImageThumbnailsEnabled()) {
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

	private volatile String baseNamePrettified;
	private volatile String baseNameWithoutExtension;
	private final Object displayNameBaseLock = new Object();

	@Override
	public String getDisplayNameBase() {
		if (getParent() instanceof SubSelFile && getMediaSubtitle() instanceof MediaOnDemandSubtitle) {
			return ((MediaOnDemandSubtitle) getMediaSubtitle()).getName();
		}
		if (isFolder()) {
			return super.getDisplayNameBase();
		}
		if (renderer.getUmsConfiguration().isPrettifyFilenames() && getFormat() != null && getFormat().isVideo()) {
			return getBaseNamePrettified();
		} else if (renderer.getUmsConfiguration().isHideExtensions()) {
			return getBaseNameWithoutExtension();
		}

		return super.getDisplayNameBase();
	}

	private String getBaseNamePrettified() {
		synchronized (displayNameBaseLock) {
			if (baseNamePrettified == null) {
				baseNamePrettified = FileUtil.getFileNamePrettified(super.getDisplayNameBase(), getMediaInfo(), isEpisodeWithinSeasonFolder(), isEpisodeWithinTVSeriesFolder(), getFile().getAbsolutePath());
			}
			return baseNamePrettified;
		}
	}

	private String getBaseNameWithoutExtension() {
		synchronized (displayNameBaseLock) {
			if (baseNameWithoutExtension == null) {
				baseNameWithoutExtension = FileUtil.getFileNameWithoutExtension(super.getDisplayNameBase());
			}
			return baseNameWithoutExtension;
		}
	}

	@Override
	public synchronized void syncResolve() {
		super.syncResolve();
		checkCoverThumb();
	}

	/**
	 * Updates cover art archive table during scan process in case the file has already stored cover art.
	 *
	 * @param inputFile
	 */
	protected void checkCoverThumb() {
		if (getMediaInfo() != null && getMediaInfo().isAudio() && getMediaInfo().hasAudioMetadata()) {
			String mbReleaseId = getMediaInfo().getAudioMetadata().getMbidRecord();
			if (!StringUtils.isAllBlank(mbReleaseId)) {
				try {
					if (!MediaTableCoverArtArchive.hasCover(mbReleaseId)) {
						AudioFile af;
						if ("mp2".equalsIgnoreCase(FileUtil.getExtension(getFile()))) {
							af = AudioFileIO.readAs(getFile(), "mp3");
						} else {
							af = AudioFileIO.read(getFile());
						}
						Tag t = af.getTag();
						LOGGER.trace("no artwork in MediaTableCoverArtArchive table");
						if (t.getFirstArtwork() != null) {
							byte[] artBytes = t.getFirstArtwork().getBinaryData();
							MediaTableCoverArtArchive.writeMBID(mbReleaseId, new ByteArrayInputStream(artBytes));
							LOGGER.trace("added cover to MediaTableCoverArtArchive");
						} else {
							LOGGER.trace("no artwork in TAG");
						}
					} else {
						LOGGER.trace("cover already exists in MediaTableCoverArtArchive");
					}
				} catch (IOException | CannotReadException | InvalidAudioFrameException | ReadOnlyFileException | TagException e) {
					LOGGER.trace("checkCoverThumb failed.", e);
				}
			}
		}
	}

	@Override
	public boolean isFullyPlayedMark() {
		return getFile() != null &&
		(
			CONFIGURATION.getFullyPlayedAction() == FullyPlayedAction.MARK ||
			CONFIGURATION.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER_AND_MARK
		) &&
		getMediaStatus() != null &&
		getMediaStatus().isFullyPlayed();
	}

}
