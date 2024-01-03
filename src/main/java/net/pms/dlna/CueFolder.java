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
package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.pms.encoders.Engine;
import net.pms.encoders.EngineFactory;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import org.apache.commons.lang3.StringUtils;
import org.digitalmediaserver.cuelib.CueParser;
import org.digitalmediaserver.cuelib.CueSheet;
import org.digitalmediaserver.cuelib.FileData;
import org.digitalmediaserver.cuelib.Position;
import org.digitalmediaserver.cuelib.TrackData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CueFolder extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(CueFolder.class);
	private final File playlistfile;

	public File getPlaylistfile() {
		return playlistfile;
	}

	private final boolean valid = true;

	public CueFolder(File f) {
		playlistfile = f;
		setLastModified(playlistfile.lastModified());
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
	protected void resolveOnce() {
		if (playlistfile.length() < 10000000) {
			CueSheet sheet;
			try {
				sheet = CueParser.parse(playlistfile, null);
			} catch (IOException e) {
				LOGGER.info("Error in parsing cue: " + e.getMessage());
				return;
			}

			if (sheet != null) {
				List<FileData> files = sheet.getFileData();
				// only the first one
				if (!files.isEmpty()) {
					FileData f = files.get(0);
					List<TrackData> tracks = f.getTrackData();
					Engine defaultPlayer = null;
					MediaInfo originalMedia = null;
					ArrayList<DLNAResource> addedResources = new ArrayList<>();
					for (int i = 0; i < tracks.size(); i++) {
						TrackData track = tracks.get(i);
						if (i > 0) {
							double end = getTime(track.getIndices().get(0).getPosition());
							if (addedResources.isEmpty()) {
								// seems the first file was invalid or non existent
								return;
							}
							DLNAResource prec = addedResources.get(i - 1);
							int count = 0;
							while (prec.isFolder() && i + count < addedResources.size()) { // not used anymore
								prec = addedResources.get(i + count);
								count++;
							}
							prec.getSplitRange().setEnd(end);
							prec.getMedia().setDuration(prec.getSplitRange().getDuration());
							LOGGER.debug("Track #" + i + " split range: " + prec.getSplitRange().getStartOrZero() + " - " + prec.getSplitRange().getDuration());
						}
						Position start = track.getIndices().get(0).getPosition();
						RealFile realFile = new RealFile(new File(playlistfile.getParentFile(), f.getFile()));
						addChild(realFile);
						addedResources.add(realFile);

						if (i > 0 && realFile.getMedia() == null) {
							realFile.setMedia(new MediaInfo());
							realFile.getMedia().setMediaparsed(true);
						}
						realFile.syncResolve();
						if (i == 0) {
							originalMedia = realFile.getMedia();
							if (originalMedia == null) {
								LOGGER.trace("Couldn't resolve media \"{}\" for cue file \"{}\" - aborting", realFile.getName(), playlistfile.getAbsolutePath());
								return;
							}
						}
						realFile.getSplitRange().setStart(getTime(start));
						realFile.setSplitTrack(i + 1);

						// Assign a splitter engine if file is natively supported by renderer
						if (realFile.getEngine() == null) {
							if (defaultPlayer == null) {
								defaultPlayer = EngineFactory.getEngine(realFile);
							}

							realFile.setEngine(defaultPlayer);
						}

						if (realFile.getMedia() != null) {
							try {
								realFile.setMedia(originalMedia.clone());
							} catch (CloneNotSupportedException e) {
								LOGGER.info("Error in cloning media info: " + e.getMessage());
							}

							if (realFile.getMedia() != null && realFile.getMedia().getFirstAudioTrack() != null) {
								if (realFile.getFormat().isAudio()) {
									realFile.getMedia().getFirstAudioTrack().setSongname(track.getTitle());
								} else {
									realFile.getMedia().getFirstAudioTrack().setSongname("Chapter #" + (i + 1));
								}
								realFile.getMedia().getFirstAudioTrack().setTrack(i + 1);
								realFile.getMedia().setSize(-1);
								if (StringUtils.isNotBlank(sheet.getTitle())) {
									realFile.getMedia().getFirstAudioTrack().setAlbum(sheet.getTitle());
								}
								if (StringUtils.isNotBlank(sheet.getPerformer())) {
									realFile.getMedia().getFirstAudioTrack().setArtist(sheet.getPerformer());
								}
								if (StringUtils.isNotBlank(track.getPerformer())) {
									realFile.getMedia().getFirstAudioTrack().setArtist(track.getPerformer());
								}
							}

						}

					}

					if (!tracks.isEmpty() && !addedResources.isEmpty()) {
						DLNAResource lastTrack = addedResources.get(addedResources.size() - 1);
						TimeRange lastTrackSplitRange = lastTrack.getSplitRange();
						MediaInfo lastTrackMedia = lastTrack.getMedia();

						if (lastTrackSplitRange != null && lastTrackMedia != null) {
							lastTrackSplitRange.setEnd(lastTrackMedia.getDurationInSeconds());
							lastTrackMedia.setDuration(lastTrackSplitRange.getDuration());
							LOGGER.debug("Track #" + childrenNumber() + " split range: " + lastTrackSplitRange.getStartOrZero() + " - " + lastTrackSplitRange.getDuration());
						}
					}

					storeFileInCache(playlistfile, Format.PLAYLIST);
				}
			}
		}
	}

	private double getTime(Position p) {
		return p.getMinutes() * 60 + p.getSeconds() + ((double) p.getFrames() / 100);
	}
}
