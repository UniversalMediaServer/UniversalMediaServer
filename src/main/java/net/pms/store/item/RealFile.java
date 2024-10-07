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
package net.pms.store.item;

import com.sun.jna.Platform;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Set;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaLang;
import net.pms.media.MediaType;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.parsers.FFmpegParser;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.store.MediaInfoStore;
import net.pms.store.MediaStatusStore;
import net.pms.store.StoreItem;
import net.pms.store.SystemFileResource;
import net.pms.store.SystemFilesHelper;
import net.pms.store.container.ChapterFileTranscodeVirtualFolder;
import net.pms.store.container.VirtualFolder;
import net.pms.util.FileUtil;
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

public class RealFile extends StoreItem implements SystemFileResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealFile.class);

	private final Object displayNameBaseLock = new Object();
	private String lang;
	private volatile String baseNamePrettified;

	private final File file;
	private boolean addToMediaLibrary = true;
	private String name;
	private volatile String baseNameWithoutExtension;
	private int splitTrack;

	public RealFile(Renderer renderer, File file) {
		this(renderer, file, null);
	}

	public RealFile(Renderer renderer, File file, String name) {
		super(renderer);
		this.file = file;
		this.name = name;
		setLastModified(file.lastModified());
	}

	/**
	 * Check if this this a new resource.
	 *
	 * @return true : File is an empty container. Upload by AV client is still missing. No need to check any formats yet.
	 */
	private boolean isUploadResource(File file, int type) {
		if (type == Format.AUDIO || type == Format.VIDEO) {
			try {
				if (Files.size(file.toPath()) == UmsContentDirectoryService.EMPTY_FILE_CONTENT.length()) {
					LOGGER.trace("isUploadResource true for {}", file.getAbsolutePath());
					return true;
				}
			} catch (Exception e) {
				LOGGER.error("cannot check file size", e);
			}
		}
		return false;
	}

	@Override
	public boolean isValid() {
		if (file == null || !file.exists() || !file.isFile()) {
			return false;
		}
		resolveFormat();

		if (getType() == Format.SUBTITLE) {
			// Don't add subtitles as separate resources
			return false;
		}

		if (isUploadResource(file, getType())) {
			return true;
		}

		boolean valid = getFormat() != null;
		if (valid && getParent() != null && getParent().getDefaultRenderer() != null && getParent().getDefaultRenderer().isUseMediaInfo()) {
			// we need to resolve the store resource now
			run();

			// Given that here getFormat() has already matched some (possibly plugin-defined) format:
			//    Format.UNKNOWN + bad parse = inconclusive
			//    known types    + bad parse = bad/encrypted file
			if (getType() != Format.UNKNOWN && getMediaInfo() != null) {
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

			}

			// XXX isMediaInfoThumbnailGeneration is only true for the "default renderer"
			if (getParent().getDefaultRenderer().isMediaInfoThumbnailGeneration()) {
				checkThumbnail();
			}
		} else if (getType() == Format.UNKNOWN) {
			return false;
		}

		return valid;
	}

	@Override
	public boolean isRendererAllowed() {
		return renderer.hasShareAccess(getFile());
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
		if (isTranscoded() && getTranscodingSettings().getEngine().type() != Format.IMAGE) {
			return TRANS_SIZE;
		} else if (getMediaInfo() != null && getMediaInfo().isMediaParsed()) {
			return getMediaInfo().getSize();
		}
		return getFile().length();
	}

	public File getFile() {
		return file;
	}

	@Override
	public void resolveFormat() {
		if (getFormat() == null) {
			setFormat(FormatFactory.getAssociatedFormat(getFile().getAbsolutePath()));
		}

		super.resolveFormat();
	}

	/**
	 * @return The path to the mediaInfo source.
	 */
	@Override
	public String getFileName() {
		return ProcessUtil.getShortFileNameIfWideChars(getFile().getAbsolutePath());
	}

	@Override
	public String getSystemName() {
		String filename = getFileName();
		if (isInsideTranscodeFolder() || getParent() instanceof ChapterFileTranscodeVirtualFolder) {
			if (isTranscoded()) {
				filename = filename.concat(">e").concat(getTranscodingSettings().getEngine().getName());
			}
			if (getMediaAudio() != null && getMediaAudio().getId() != 0) {
				filename = filename.concat(">a").concat(String.valueOf(getMediaAudio().getId()));
			}
			if (getMediaSubtitle() != null && getMediaSubtitle().getId() != 0) {
				filename = filename.concat(">s").concat(String.valueOf(getMediaSubtitle().getId()));
			}
			if (getSplitRange() != null) {
				filename = filename.concat(">c").concat(String.valueOf(getSplitRange().getStartOrZero()));
			}
		}
		return filename;
	}

	@Override
	public synchronized void resolve() {
		if (file == null) {
			LOGGER.error("RealFile points to no physical file. ");
			return;
		}

		if (file.isFile() && (getMediaInfo() == null || !getMediaInfo().isMediaParsed())) {
			Boolean useSymlinks = renderer.getUmsConfiguration().isUseSymlinksTargetFile();
			String filename;
			if (useSymlinks && FileUtil.isSymbolicLink(file)) {
				filename = FileUtil.getRealFile(file).getAbsolutePath();
			} else {
				filename = file.getAbsolutePath();
			}
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
				Set<File> potentials = SystemFilesHelper.getPotentialFileThumbnails(audioVideoFile, true);
				if (!potentials.isEmpty()) {
					// We have no rules for how to pick a particular one if there's multiple candidates
					cachedThumbnail = potentials.iterator().next();
					break;
				}
			}
			if (cachedThumbnail == null && mediaType == MediaType.AUDIO && getParent() instanceof VirtualFolder virtualFolder) {
				cachedThumbnail = virtualFolder.getPotentialCover();
			}
		}

		boolean hasAlreadyEmbeddedCoverArt = getType() == Format.AUDIO && getMediaInfo() != null && getMediaInfo().getThumbnail() != null;
		DLNAThumbnailInputStream result = null;
		try {
			if (cachedThumbnail != null && !hasAlreadyEmbeddedCoverArt) {
				result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(cachedThumbnail));
			} else if (getMediaInfo() != null && getMediaInfo().getThumbnail() != null) {
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

	@Override
	public synchronized void syncResolve() {
		super.syncResolve();
		if (mediaInfo != null && mediaInfo.isVideo() && getFile() != null) {
			MediaInfoStore.setMetadataFromFileName(getFile(), mediaInfo);
		}
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
	public boolean isFullyPlayedAware() {
		return true;
	}

	@Override
	public boolean isFullyPlayed() {
		return getFile() != null &&
		getMediaStatus() != null &&
		getMediaStatus().isFullyPlayed();
	}

	@Override
	public void setFullyPlayed(boolean fullyPlayed) {
		if (getFile() != null) {
			MediaStatusStore.setFullyPlayed(getFile().getAbsolutePath(), renderer.getAccountUserId(), fullyPlayed, null);
		}
	}

	@Override
	public String getName() {
		if (name == null) {

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

	/**
	 * Returns the localized display name.
	 *
	 * @return The localized display name.
	 */
	@Override
	public String getLocalizedDisplayName(String lang) {
		synchronized (displayNameBaseLock) {
			lang = CONFIGURATION.getTranslationLanguage(lang);
			if (lang != null && !lang.equals(this.lang) || this.lang != null) {
				//language changed
				this.lang = lang;
				baseNamePrettified = null;
			}
		}
		return getDisplayName();
	}

	@Override
	public String getDisplayNameBase() {
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

	protected String getBaseNamePrettified(boolean isEpisodeWithinSeasonFolder, boolean isEpisodeWithinTVSeriesFolder) {
		synchronized (displayNameBaseLock) {
			if (baseNamePrettified == null) {
				MediaVideoMetadata videoMetadata = getMediaInfo() != null ? getMediaInfo().getVideoMetadata() : null;
				baseNamePrettified = FileUtil.getFileNamePrettified(super.getDisplayNameBase(), videoMetadata, isEpisodeWithinSeasonFolder, isEpisodeWithinTVSeriesFolder, file.getAbsolutePath(), lang);
			}
			return baseNamePrettified;
		}
	}

	protected String getBaseNamePrettified() {
		return getBaseNamePrettified(false, false);
	}

	private String getBaseNameWithoutExtension() {
		synchronized (displayNameBaseLock) {
			if (baseNameWithoutExtension == null) {
				baseNameWithoutExtension = FileUtil.getFileNameWithoutExtension(super.getDisplayNameBase());
			}
			return baseNameWithoutExtension;
		}
	}

	public void setAddToMediaLibrary(boolean value) {
		addToMediaLibrary = value;
	}

	@Override
	public boolean isAddToMediaLibrary() {
		return addToMediaLibrary;
	}

	/**
	 * Returns the number of the track to split from this resource.
	 *
	 * @return the splitTrack
	 * @since 1.50
	 */
	protected int getSplitTrack() {
		return splitTrack;
	}

	/**
	 * Sets the number of the track from this resource to split.
	 *
	 * @param splitTrack The track number.
	 * @since 1.50
	 */
	public void setSplitTrack(int splitTrack) {
		this.splitTrack = splitTrack;
	}

	@Override
	public File getSystemFile() {
		return getFile();
	}

}
