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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JComponent;

import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.external.FinalizeTranscoderArgsListener;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.util.FileUtil;
import net.pms.util.Iso639;

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
	public abstract String[] args();
	public abstract String mimeType();
	public abstract String executable();
	private static List<FinalizeTranscoderArgsListener> finalizeTranscodeArgsListeners =
		new ArrayList<FinalizeTranscoderArgsListener>();

	public static void initializeFinalizeTranscoderArgsListeners() {
		for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
			if (listener instanceof FinalizeTranscoderArgsListener) {
				finalizeTranscodeArgsListeners.add((FinalizeTranscoderArgsListener) listener);
			}
		}
	}

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

	public abstract ProcessWrapper launchTranscode(
		String filename,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException;

	@Override
	public String toString() {
		return name();
	}

	protected String[] finalizeTranscoderArgs(
		Player player,
		String filename,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params,
		String[] cmdArgs) {
		if (finalizeTranscodeArgsListeners.isEmpty()) {
			return cmdArgs;
		} else {
			// make it mutable
			List<String> cmdList = new ArrayList<String>(Arrays.asList(cmdArgs));

			for (FinalizeTranscoderArgsListener listener : finalizeTranscodeArgsListeners) {
				try {
					cmdList = listener.finalizeTranscoderArgs(
							player,
							filename,
							dlna,
							media,
							params,
							cmdList);
				} catch (Throwable t) {
					LOGGER.error(String.format("Failed to call finalizeTranscoderArgs on listener of type=%s", listener.getClass()), t);
				}
			}

			String[] cmdArray = new String[cmdList.size()];
			cmdList.toArray(cmdArray);
			return cmdArray;
		}
	}

	public void setAudioAndSubs(String fileName, DLNAMediaInfo media, OutputParams params, PmsConfiguration configuration) {
		if (params.aid == null && media != null) {
			// check for preferred audio
			StringTokenizer st = new StringTokenizer(configuration.getMencoderAudioLanguages(), ",");
			while (st != null && st.hasMoreTokens()) {
				String lang = st.nextToken();
				lang = lang.trim();
				LOGGER.trace("Looking for an audio track with lang: " + lang);
				for (DLNAMediaAudio audio : media.getAudioCodes()) {
					if (audio.matchCode(lang)) {
						params.aid = audio;
						LOGGER.trace("Matched audio track: " + audio);
						st = null;
						break;
					}
				}
			}
		}

		if (params.aid == null && media.getAudioCodes().size() > 0) {
			// take a default audio track, dts first if possible
			for (DLNAMediaAudio audio : media.getAudioCodes()) {
				if (audio.isDTS()) {
					params.aid = audio;
					LOGGER.trace("Found priority audio track with DTS: " + audio);
					break;
				}
			}

			if (params.aid == null) {
				params.aid = media.getAudioCodes().get(0);
				LOGGER.trace("Choosed a default audio track: " + params.aid);
			}
		}

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

		StringTokenizer st1 = new StringTokenizer(configuration.getMencoderAudioSubLanguages(), ";");

		while (st1.hasMoreTokens()) {
			String pair = st1.nextToken();
			if (pair.contains(",")) {
				String audio = pair.substring(0, pair.indexOf(","));
				String sub = pair.substring(pair.indexOf(",") + 1);
				audio = audio.trim();
				sub = sub.trim();
				LOGGER.trace("Search a match for: " + currentLang + " with " + audio + " and " + sub);

				if (Iso639.isCodesMatching(audio, currentLang) || (currentLang != null && audio.equals("*"))) {
					if (sub.equals("off")) {
						matchedSub = new DLNAMediaSubtitle();
						matchedSub.setLang("off");
					} else {
						for (DLNAMediaSubtitle present_sub : media.getSubtitlesCodes()) {
							if (present_sub.matchCode(sub) || sub.equals("*")) {
								matchedSub = present_sub;
								LOGGER.trace(" Found a match: " + matchedSub);
								break;
							}
						}
					}

					if (matchedSub != null) {
						break;
					}
				}
			}
		}

		if (matchedSub != null && params.sid == null) {
			if (matchedSub.getLang() != null && matchedSub.getLang().equals("off")) {
				LOGGER.trace(" Disabled the subtitles: " + matchedSub);
			} else {
				params.sid = matchedSub;
			}
		}

		if (!configuration.isMencoderDisableSubs() && params.sid == null && media != null) {
			// Check for subtitles again
			File video = new File(fileName);
			FileUtil.doesSubtitlesExists(video, media, false);

			if (configuration.getUseSubtitles()) {
				boolean forcedSubsFound = false;
				// Priority to external subtitles
				for (DLNAMediaSubtitle sub : media.getSubtitlesCodes()) {
					if (matchedSub !=null && matchedSub.getLang() !=null && matchedSub.getLang().equals("off")) {
						StringTokenizer st = new StringTokenizer(configuration.getMencoderForcedSubTags(), ",");

						while (st != null && sub.getFlavor() != null && st.hasMoreTokens()) {
							String forcedTags = st.nextToken();
							forcedTags = forcedTags.trim();

							if (sub.getFlavor().toLowerCase().indexOf(forcedTags) > -1
									&& Iso639.isCodesMatching(sub.getLang(), configuration.getMencoderForcedSubLanguage())) {

								LOGGER.trace("Forcing preferred subtitles : " + sub.getLang() + "/" + sub.getFlavor());
								LOGGER.trace("Forced subtitles track : " + sub);

								if (sub.getFile() != null) {
									LOGGER.trace("Found external forced file : " + sub.getFile().getAbsolutePath());
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
							if (sub.getFile() != null) {
								LOGGER.trace("Found external file: " + sub.getFile().getAbsolutePath());
								params.sid = sub;
								break;
							}
					}
				}
			}
			if (
				matchedSub !=null && 
				matchedSub.getLang() !=null && 
				matchedSub.getLang().equals("off")
			) {
				return;
			}

			if (params.sid == null) {
				StringTokenizer st = new StringTokenizer(configuration.getMencoderSubLanguages(), ",");
				while (st != null && st.hasMoreTokens()) {
					String lang = st.nextToken();
					lang = lang.trim();
					LOGGER.trace("Looking for a subtitle track with lang: " + lang);
					for (DLNAMediaSubtitle sub : media.getSubtitlesCodes()) {
						if (sub.matchCode(lang)) {
							params.sid = sub;
							LOGGER.trace("Matched sub track: " + params.sid);
							st = null;
							break;
						}
					}
				}
			}
		}
	}
}