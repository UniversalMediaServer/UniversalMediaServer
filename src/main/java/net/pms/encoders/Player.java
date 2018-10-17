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
package net.pms.encoders;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.util.FileUtil;
import net.pms.util.Iso639;
import net.pms.util.OpenSubtitle;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(Player.class);

	public static final int VIDEO_SIMPLEFILE_PLAYER = 0;
	public static final int AUDIO_SIMPLEFILE_PLAYER = 1;
	public static final int VIDEO_WEBSTREAM_PLAYER = 2;
	public static final int AUDIO_WEBSTREAM_PLAYER = 3;
	public static final int MISC_PLAYER = 4;
	public static final String NATIVE = "NATIVE";

	public abstract int purpose();
	public abstract JComponent config();
	public abstract String id();
	public abstract String name();
	public abstract int type();
	/**
	 * Must be used to control all access to {@link #available}
	 */
	protected final ReentrantReadWriteLock availableLock = new ReentrantReadWriteLock();
	/**
	 * Used to determine if the player can be used, e.g if the binary is
	 * accessible. All access must be guarded with {@link #availableLock}.
	 */
	boolean available = false;

	/**
	 * Must be used to control all access to {@link #enabled}
	 */
	protected final ReentrantReadWriteLock enabledLock = new ReentrantReadWriteLock();
	/**
	 * All access must be guarded with {@link #enabledLock}.
	 */
	boolean enabled = false;

	// FIXME this is an implementation detail (and not a very good one).
	// it's entirely up to engines how they construct their command lines.
	// need to get rid of this
	public abstract String[] args();

	public abstract String mimeType();
	public abstract String executable();
	protected static final PmsConfiguration _configuration = PMS.getConfiguration();
	protected PmsConfiguration configuration = _configuration;

	public boolean avisynth() {
		return false;
	}

	public boolean excludeFormat(Format extension) {
		return false;
	}

	public boolean isPlayerCompatible(RendererConfiguration renderer) {
		return true;
	}

	public boolean isInternalSubtitlesSupported() {
		return true;
	}

	public boolean isExternalSubtitlesSupported() {
		return true;
	}

	public boolean isTimeSeekable() {
		return false;
	}

	/**
	 * Used to determine if the player can be used, e.g if the binary is
	 * accessible. Threadsafe.
	 */
	public boolean isAvailable() {
		availableLock.readLock().lock();
		try {
			return available == true;
		} finally {
			availableLock.readLock().unlock();
		}
	}

	void setAvailable(boolean available) {
		availableLock.writeLock().lock();
		try {
			this.available = available;
		} finally {
			availableLock.writeLock().unlock();
		}
	}

	/**
	 * Threadsafe.
	 */
	public boolean isEnabled() {
		enabledLock.readLock().lock();
		try {
			return enabled == true;
		} finally {
			enabledLock.readLock().unlock();
		}
	}

	void setEnabled(boolean enabled) {
		enabledLock.writeLock().lock();
		try {
			this.enabled = enabled;
		} finally {
			enabledLock.writeLock().unlock();
		}
		_configuration.setEngineEnabled(id(), enabled);
	}

	public void toggleEnabled() {
		enabledLock.writeLock().lock();
		try {
			enabled = !enabled;
		} finally {
			enabledLock.writeLock().unlock();
		}
		_configuration.setEngineEnabled(id(), enabled);
	}

	/**
	 * Convenience method to check that a player is both available and enabled
	 */
	public boolean isActive() {
		return isAvailable() && isEnabled();
	}

	/**
	 * Each engine capable of video hardware acceleration must override this
	 * method and set
	 * <p>
	 * <code>return true</code>.
	 *
	 * @return false
	 */
	public boolean isGPUAccelerationReady() {
		return false;
	}

	/**
	 * @deprecated Use {@link #launchTranscode(net.pms.dlna.DLNAResource, net.pms.dlna.DLNAMediaInfo, net.pms.io.OutputParams)} instead.
	 */
	@Deprecated
	public final ProcessWrapper launchTranscode(String filename, DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		return launchTranscode(dlna, media, params);
	}

	public abstract ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException;

	@Override
	public String toString() {
		return name();
	}

	/**
	 * @deprecated Use {@link #setAudioAndSubs(String fileName, DLNAMediaInfo media, OutputParams params)} instead.
	 */
	@Deprecated
	public void setAudioAndSubs(String fileName, DLNAMediaInfo media, OutputParams params, PmsConfiguration configuration) {
		setAudioAndSubs(fileName, media, params);
	}

	/**
	 * This method populates the supplied {@link OutputParams} object with the correct audio track (aid)
	 * and subtitles (sid), based on the given filename, its MediaInfo metadata and PMS configuration settings.
	 *
	 * @param fileName
	 * The file name used to determine the availability of subtitles.
	 * @param media
	 * The MediaInfo metadata for the file.
	 * @param params
	 * The parameters to populate.
	 */
	public static void setAudioAndSubs(String fileName, DLNAMediaInfo media, OutputParams params) {
		setAudioOutputParameters(media, params);
		setSubtitleOutputParameters(fileName, media, params);
	}

	/**
	 * This method populates the supplied {@link OutputParams} object with the correct audio track (aid)
	 * based on the MediaInfo metadata and PMS configuration settings.
	 *
	 * @param media
	 * The MediaInfo metadata for the file.
	 * @param params
	 * The parameters to populate.
	 */
	public static void setAudioOutputParameters(DLNAMediaInfo media, OutputParams params) {
		// Use device-specific pms conf
		PmsConfiguration configuration = PMS.getConfiguration(params);
		if (params.aid == null && media != null && media.getFirstAudioTrack() != null) {
			// check for preferred audio
			DLNAMediaAudio dtsTrack = null;
			StringTokenizer st = new StringTokenizer(configuration.getAudioLanguages(), ",");
			while (st.hasMoreTokens()) {
				String lang = st.nextToken().trim();
				LOGGER.trace("Looking for an audio track with lang: " + lang);
				for (DLNAMediaAudio audio : media.getAudioTracksList()) {
					if (audio.matchCode(lang)) {
						params.aid = audio;
						LOGGER.trace("Matched audio track: " + audio);
						return;
					}

					if (dtsTrack == null && audio.isDTS()) {
						dtsTrack = audio;
					}
				}
			}

			// preferred audio not found, take a default audio track, dts first if available
			if (dtsTrack != null) {
				params.aid = dtsTrack;
				LOGGER.trace("Found priority audio track with DTS: " + dtsTrack);
			} else {
				params.aid = media.getAudioTracksList().get(0);
				LOGGER.trace("Chose a default audio track: " + params.aid);
			}
		}
	}

	/**
	 * This method populates the supplied {@link OutputParams} object with the correct subtitles (sid)
	 * based on the given filename, its MediaInfo metadata and PMS configuration settings.
	 *
	 * TODO: Rewrite this crazy method to be more concise and logical.
	 *
	 * @param fileName
	 * The file name used to determine the availability of subtitles.
	 * @param media
	 * The MediaInfo metadata for the file.
	 * @param params
	 * The parameters to populate.
	 */
	public static void setSubtitleOutputParameters(String fileName, DLNAMediaInfo media, OutputParams params) {
		// Use device-specific pms conf
		PmsConfiguration configuration = PMS.getConfiguration(params);
		String currentLang = null;
		DLNAMediaSubtitle matchedSub = null;

		if (params.aid != null) {
			currentLang = params.aid.getLang();
		}

		if (params.sid != null && params.sid.getId() == -1) {
			LOGGER.trace("Don't want subtitles!");
			params.sid = null;
			return;
		}

		/**
		 * Check for live subtitles
		 */
		if (params.sid != null && !StringUtils.isEmpty(params.sid.getLiveSubURL())) {
			LOGGER.debug("Live subtitles " + params.sid.getLiveSubURL());
			try {
				matchedSub = params.sid;
				String file = OpenSubtitle.fetchSubs(matchedSub.getLiveSubURL(), matchedSub.getLiveSubFile());
				if (!StringUtils.isEmpty(file)) {
					matchedSub.setExternalFile(new File(file), null);
					params.sid = matchedSub;
					return;
				}
			} catch (IOException e) {
			}
		}

		StringTokenizer st = new StringTokenizer(configuration.getAudioSubLanguages(), ";");

		/**
		 * Check for external and internal subtitles matching the user's language
		 * preferences
		 */
		boolean matchedInternalSubtitles = false;
		boolean matchedExternalSubtitles = false;
		while (st.hasMoreTokens()) {
			String pair = st.nextToken();
			if (pair.contains(",")) {
				String audio = pair.substring(0, pair.indexOf(','));
				String sub = pair.substring(pair.indexOf(',') + 1);
				audio = audio.trim();
				sub = sub.trim();
				LOGGER.trace("Searching for a match for: " + currentLang + " with " + audio + " and " + sub);

				if (Iso639.isCodesMatching(audio, currentLang) || (currentLang != null && audio.equals("*"))) {
					if (sub.equals("off")) {
						/**
						 * Ignore the "off" language for external subtitles if the user setting is enabled
						 * TODO: Prioritize multiple external subtitles properly instead of just taking the first one we load
						 */
						if (configuration.isForceExternalSubtitles()) {
							for (DLNAMediaSubtitle present_sub : media.getSubtitleTracksList()) {
								if (present_sub.getExternalFile() != null) {
									matchedSub = present_sub;
									matchedExternalSubtitles = true;
									LOGGER.trace("Ignoring the \"off\" language because there are external subtitles");
									break;
								}
							}
						}
						if (!matchedExternalSubtitles) {
							matchedSub = new DLNAMediaSubtitle();
							matchedSub.setLang("off");
						}
					} else {
						for (DLNAMediaSubtitle present_sub : media.getSubtitleTracksList()) {
							if (present_sub.matchCode(sub) || sub.equals("*")) {
								if (present_sub.getExternalFile() != null) {
									if (configuration.isAutoloadExternalSubtitles()) {
										// Subtitle is external and we want external subtitles, look no further
										matchedSub = present_sub;
										LOGGER.trace("Matched external subtitles track: " + matchedSub);
										break;
									} else {
										// Subtitle is external but we do not want external subtitles, keep searching
										LOGGER.trace("External subtitles ignored because of user setting: " + present_sub);
									}
								} else if (!matchedInternalSubtitles) {
									matchedSub = present_sub;
									LOGGER.trace("Matched internal subtitles track: " + matchedSub);
									if (configuration.isAutoloadExternalSubtitles()) {
										// Subtitle is internal and we will wait to see if an external one is available instead
										matchedInternalSubtitles = true;
									} else {
										// Subtitle is internal and we will use it
										break;
									}
								}
							}
						}
					}

					if (matchedSub != null && !matchedInternalSubtitles) {
						break;
					}
				}
			}
		}

		/**
		 * Check for external subtitles that were skipped in the above code block
		 * because they didn't match language preferences, if there wasn't already
		 * a match and the user settings specify it.
		 */
		if (matchedSub == null && configuration.isForceExternalSubtitles()) {
			for (DLNAMediaSubtitle present_sub : media.getSubtitleTracksList()) {
				if (present_sub.getExternalFile() != null) {
					matchedSub = present_sub;
					LOGGER.trace("Matched external subtitles track that did not match language preferences: " + matchedSub);
					break;
				}
			}
		}

		/**
		 * Disable chosen subtitles if the user has disabled all subtitles or
		 * if the language preferences have specified the "off" language.
		 *
		 * TODO: Can't we save a bunch of looping by checking for isDisableSubtitles
		 * just after the Live Subtitles check above?
		 */
		if (matchedSub != null && params.sid == null) {
			if (configuration.isDisableSubtitles() || (matchedSub.getLang() != null && matchedSub.getLang().equals("off"))) {
				LOGGER.trace("Disabled the subtitles: " + matchedSub);
			} else {
				params.sid = matchedSub;
			}
		}

		/**
		 * Check for forced subtitles.
		 */
		if (!configuration.isDisableSubtitles() && params.sid == null && media != null) {
			// Check for subtitles again
			File video = new File(fileName);
			FileUtil.isSubtitlesExists(video, media, false);

			if (configuration.isAutoloadExternalSubtitles()) {
				boolean forcedSubsFound = false;
				// Priority to external subtitles
				for (DLNAMediaSubtitle sub : media.getSubtitleTracksList()) {
					if (matchedSub != null && matchedSub.getLang() != null && matchedSub.getLang().equals("off")) {
						st = new StringTokenizer(configuration.getForcedSubtitleTags(), ",");

						while (sub.getSubtitlesTrackTitleFromMetadata() != null && st.hasMoreTokens()) {
							String forcedTags = st.nextToken();
							forcedTags = forcedTags.trim();

							if (
								sub.getSubtitlesTrackTitleFromMetadata().toLowerCase().contains(forcedTags) &&
								Iso639.isCodesMatching(sub.getLang(), configuration.getForcedSubtitleLanguage())
							) {
								LOGGER.trace("Forcing preferred subtitles: " + sub.getLang() + "/" + sub.getSubtitlesTrackTitleFromMetadata());
								LOGGER.trace("Forced subtitles track: " + sub);

								if (sub.getExternalFile() != null) {
									LOGGER.trace("Found external forced file: " + sub.getExternalFile().getAbsolutePath());
								}
								params.sid = sub;
								forcedSubsFound = true;
								break;
							}
						}
						if (forcedSubsFound == true) {
							break;
						}
					} else {
						LOGGER.trace("Found subtitles track: " + sub);

						if (sub.getExternalFile() != null) {
							LOGGER.trace("Found external file: " + sub.getExternalFile().getAbsolutePath());
							params.sid = sub;
							break;
						}
					}
				}
			}
			if (
				matchedSub != null &&
				matchedSub.getLang() != null &&
				matchedSub.getLang().equals("off")
			) {
				return;
			}

			if (params.sid == null) {
				st = new StringTokenizer(UMSUtils.getLangList(params.mediaRenderer), ",");
				while (st.hasMoreTokens()) {
					String lang = st.nextToken();
					lang = lang.trim();
					LOGGER.trace("Looking for a subtitle track with lang: " + lang);
					for (DLNAMediaSubtitle sub : media.getSubtitleTracksList()) {
						if (
							sub.matchCode(lang) &&
							!(
								!configuration.isAutoloadExternalSubtitles() &&
								sub.getExternalFile() != null
							)
						) {
							params.sid = sub;
							LOGGER.trace("Matched subtitles track: " + params.sid);
							return;
						}
					}
				}
			}
		}
	}

	/**
	 * @see #convertToModX(int, int)
	 */
	@Deprecated
	public int convertToMod4(int number) {
		return convertToModX(number, 4);
	}

	/**
	 * Convert number to be divisible by mod.
	 *
	 * @param number the number to convert
	 * @param mod the number to divide by
	 *
	 * @return the number divisible by mod
	 */
	public static int convertToModX(int number, int mod) {
		if (number % mod != 0) {
			number -= (number % mod);
		}

		return number;
	}

	/**
	 * Returns whether or not the player can handle a given resource.
	 * If the resource is <code>null</code> compatibility cannot be
	 * determined and <code>false</code> will be returned.
	 *
	 * @param resource
	 * The {@link DLNAResource} to be matched.
	 * @return True when the resource can be handled, false otherwise.
	 * @since 1.60.0
	 */
	public abstract boolean isCompatible(DLNAResource resource);

	/**
	 * Returns whether or not another player has the same
	 * name and id as this one.
	 *
	 * @param other
	 * The other player.
	 * @return True if names and ids match, false otherwise.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof Player)) {
			return false;
		}
		Player otherPlayer = (Player) other;
		if (this.name() == null) {
			if (otherPlayer.name() != null) {
				return false;
			}
		} else if (!this.name().equals(otherPlayer.name())) {
			return false;
		}
		if (this.id() == null) {
			if (otherPlayer.id() != null) {
				return false;
			}
		} else if (!this.id().equals(otherPlayer.id())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (name() == null ? 0 : name().hashCode());
		result = prime * result + (id() == null ? 0 : id().hashCode());
		return result;
	}
}
