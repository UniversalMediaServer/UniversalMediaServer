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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import jwbroek.cuelib.*;
import net.pms.PMS;
import net.pms.dlna.Range.Time;
import net.pms.encoders.FFmpegAudio;
import net.pms.encoders.MEncoderVideo;
import net.pms.encoders.Player;
import net.pms.formats.Format;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CueFolder extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(CueFolder.class);
	private File playlistfile;

	public File getPlaylistfile() {
		return playlistfile;
	}
	private boolean valid = true;

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
				sheet = CueParser.parse(playlistfile);
			} catch (IOException e) {
				LOGGER.info("Error in parsing cue: " + e.getMessage());
				return;
			}

			if (sheet != null) {
				List<FileData> files = sheet.getFileData();
				// only the first one
				if (files.size() > 0) {
					FileData f = files.get(0);
					List<TrackData> tracks = f.getTrackData();
					Player defaultPlayer = null;
					DLNAMediaInfo originalMedia = null;
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
							realFile.setMedia(new DLNAMediaInfo());
							realFile.getMedia().setMediaparsed(true);
						}
						realFile.resolve();
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
						if (realFile.getPlayer() == null) {
							if (defaultPlayer == null) {
								/*
									XXX why are we creating new player instances? aren't they
									supposed to be singletons?

									TODO don't hardwire the player here; leave it to the
									player factory to select the right player for the
									resource's format e.g:

										defaultPlayer = PlayerFactory.getPlayer(realFile);
								*/
								if (realFile.getFormat() == null) {
									LOGGER.error("No file format known for file \"{}\", assuming it is a video for now.", realFile.getName());
									/*
										TODO (see above):

											r.resolveFormat(); // sets the format based on the filename
											defaultPlayer = PlayerFactory.getPlayer(realFile);
									*/
									defaultPlayer = new MEncoderVideo();
								} else {
									if (realFile.getFormat().isAudio()) {
										defaultPlayer = new FFmpegAudio();
									} else {
										defaultPlayer = new MEncoderVideo();
									}
								}
							}

							realFile.setPlayer(defaultPlayer);
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

					if (tracks.size() > 0 && addedResources.size() > 0) {
						DLNAResource lastTrack = addedResources.get(addedResources.size() - 1);
						Time lastTrackSplitRange = lastTrack.getSplitRange();
						DLNAMediaInfo lastTrackMedia = lastTrack.getMedia();

						if (lastTrackSplitRange != null && lastTrackMedia != null) {
							lastTrackSplitRange.setEnd(lastTrackMedia.getDurationInSeconds());
							lastTrackMedia.setDuration(lastTrackSplitRange.getDuration());
							LOGGER.debug("Track #" + childrenNumber() + " split range: " + lastTrackSplitRange.getStartOrZero() + " - " + lastTrackSplitRange.getDuration());
						}
					}

					PMS.get().storeFileInCache(playlistfile, Format.PLAYLIST);
				}
			}
		}
	}

	private double getTime(Position p) {
		return p.getMinutes() * 60 + p.getSeconds() + ((double) p.getFrames() / 100);
	}
}
