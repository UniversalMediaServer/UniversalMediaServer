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

import net.pms.encoders.AviSynthFFmpeg;
import net.pms.encoders.AviSynthMEncoder;
import net.pms.encoders.Engine;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.MEncoderVideo;
import net.pms.encoders.TsMuxeRVideo;
import net.pms.encoders.VLCVideo;
import net.pms.encoders.VideoLanVideoStreaming;
import net.pms.image.ImageInfo;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlnaHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(DlnaHelper.class);
	private static final String[] DLNA_LOCALES = {"NA", "JP", "EU"};

	/**
	 * This class is not meant to be instantiated.
	 */
	protected DlnaHelper() {
	}

	public static String getDlnaContentFeatures(StoreItem resource) {
		// TODO: Determine renderer's correct localization value
		int localizationValue = 1;
		String dlnaOrgPnFlags = getDlnaOrgPnFlags(resource, localizationValue);
		return (dlnaOrgPnFlags != null ? (dlnaOrgPnFlags + ";") : "") + getDlnaOrgOpFlags(resource) +
			";DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
	}

	public static String getDlnaImageContentFeatures(StoreResource resource, DLNAImageProfile profile, boolean thumbnailRequest) {
		StringBuilder sb = new StringBuilder();
		if (profile != null) {
			sb.append("DLNA.ORG_PN=").append(profile);
		}
		final MediaInfo mediaInfo = resource.getMediaInfo();
		ImageInfo thumbnailImageInf = null;
		if (resource.getThumbnailImageInfo() != null) {
			thumbnailImageInf = resource.getThumbnailImageInfo();
		} else if (mediaInfo != null && mediaInfo.getThumbnail() != null && mediaInfo.getThumbnail().getImageInfo() != null) {
			thumbnailImageInf = mediaInfo.getThumbnail().getImageInfo();
		}
		ImageInfo imageInfo = null;
		if (thumbnailRequest) {
			imageInfo = thumbnailImageInf;
		} else if (mediaInfo != null) {
			imageInfo = mediaInfo.getImageInfo();
		}

		if (profile != null && !thumbnailRequest && thumbnailImageInf != null && profile.useThumbnailSource(imageInfo, thumbnailImageInf)) {
			imageInfo = thumbnailImageInf;
		}
		if (profile != null && imageInfo != null) {
			DLNAImageProfile.HypotheticalResult hypotheticalResult = profile.calculateHypotheticalProperties(imageInfo);
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append("DLNA.ORG_CI=").append(hypotheticalResult.conversionNeeded ? "1" : "0");
		}
		if (sb.length() > 0) {
			sb.append(';');
		}
		sb.append("DLNA.ORG_FLAGS=00900000000000000000000000000000");

		return sb.toString();
	}

	protected static int getDLNALocalesCount() {
		return DLNA_LOCALES.length;
	}

	/**
	 * DLNA.ORG_OP flags
	 *
	 * Two booleans (binary digits) which determine what transport operations
	 * the renderer is allowed to perform (in the form of HTTP request headers):
	 * the first digit allows the renderer to send TimeSeekRange.DLNA.ORG (seek
	 * by time) headers; the second allows it to send RANGE (seek by byte)
	 * headers.
	 *
	 * 00 - no seeking (or even pausing) allowed 01 - seek by byte 10 - seek by
	 * time 11 - seek by both
	 *
	 * See here for an example of how these options can be mapped to keys on the
	 * renderer's controller:
	 *
	 * Note that seek-by-byte is the preferred option for streamed files [1] and
	 * seek-by-time is the preferred option for transcoded files.
	 *
	 * seek-by-time requires a) support by the renderer (via the SeekByTime
	 * renderer conf option) and b) support by the transcode engine.
	 *
	 * The seek-by-byte fallback doesn't work well with transcoded files [2],
	 * but it's better than disabling seeking (and pausing) altogether.
	 *
	 * @return String representation of the DLNA.ORG_OP flags
	 */
	protected static String getDlnaOrgOpFlags(StoreItem item) {
		String dlnaOrgOpFlags = "01"; // seek by byte (exclusive)
		final Renderer renderer = item.getDefaultRenderer();
		final Engine engine = item.getEngine();

		if (renderer.isSeekByTime() && engine != null && engine.isTimeSeekable()) {
			/**
			 * Some renderers - e.g. the PS3 and Panasonic TVs - behave
			 * erratically when transcoding if we keep the default seek-by-byte
			 * permission on when permitting seek-by-time.
			 *
			 * It's not clear if this is a bug in the DLNA libraries of these
			 * renderers or a bug in UMS, but setting an option in the renderer
			 * conf that disables seek-by-byte when we permit seek-by-time -
			 * e.g.:
			 *
			 * SeekByTime = exclusive
			 *
			 * works around it.
			 */

			/**
			 * TODO (e.g. in a beta release): set seek-by-time (exclusive) here
			 * for *all* renderers: seek-by-byte isn't needed here (both the
			 * renderer and the engine support seek-by-time) and may be buggy on
			 * other renderers than the ones we currently handle.
			 *
			 * In the unlikely event that a renderer *requires* seek-by-both
			 * here, it can opt in with (e.g.):
			 *
			 * SeekByTime = both
			 */
			if (renderer.isSeekByTimeExclusive()) {
				dlnaOrgOpFlags = "10"; // seek by time (exclusive)
			} else {
				dlnaOrgOpFlags = "11"; // seek by both
			}
		}

		return "DLNA.ORG_OP=" + dlnaOrgOpFlags;
	}

	/**
	 * Creates the DLNA.ORG_PN to send. DLNA.ORG_PN is a string that tells the
	 * renderer what type of file to expect, like its container, framerate,
	 * codecs and resolution. Some renderers will not play a file if it has the
	 * wrong DLNA.ORG_PN string, while others are fine with any string or even
	 * nothing.
	 *
	 * @param localizationValue
	 * @return String representation of the DLNA.ORG_PN flags
	 */
	protected static String getDlnaOrgPnFlags(StoreItem item, int localizationValue) {
		// Use device-specific UMS conf, if any
		String mime = item.getRendererMimeType();
		final Renderer renderer = item.getDefaultRenderer();
		final Engine engine = item.getEngine();
		final MediaInfo mediaInfo = item.getMediaInfo();
		final MediaSubtitle mediaSubtitle = item.getMediaSubtitle();
		final MediaVideo defaultVideoTrack = mediaInfo != null ? mediaInfo.getDefaultVideoTrack() : null;
		final MediaAudio defaultAudioTrack = mediaInfo != null ? mediaInfo.getDefaultAudioTrack() : null;
		final MediaAudio mediaAudio = item.getMediaAudio();
		MediaSubtitle resolvedSubtitle = mediaSubtitle;

		String dlnaOrgPnFlags = null;
		if (renderer.isDLNAOrgPNUsed() || renderer.isAccurateDLNAOrgPN()) {
			// TODO: See if this PS3 condition is still needed
			if (renderer.isPS3()) {
				if (mime.equals(HTTPResource.DIVX_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=AVI";
				} else if (mime.equals(HTTPResource.WMV_TYPEMIME) && defaultVideoTrack != null && defaultVideoTrack.isHDVideo()) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=" + getWmvOrgPN(mediaInfo, renderer, engine == null);
				}
			} else {
				if (mime.equals(HTTPResource.DIVX_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=AVI";
				} else if (mime.equals(HTTPResource.WMV_TYPEMIME) && defaultVideoTrack != null && defaultVideoTrack.isHDVideo()) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=" + getWmvOrgPN(mediaInfo, renderer, engine == null);
				} else if (mime.equals(HTTPResource.MPEG_TYPEMIME) || mime.equals(HTTPResource.HLS_TYPEMIME) || mime.equals(HTTPResource.HLS_APPLE_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMpegPsOrgPN(localizationValue);

					// If engine is not null, we are not streaming it
					if (engine != null) {
						// VLC Web Video (Legacy) and tsMuxeR always output
						// MPEG-TS
						boolean isOutputtingMPEGTS = TsMuxeRVideo.ID.equals(engine.getEngineId()) || VideoLanVideoStreaming.ID.equals(engine.getEngineId());

						// Check if the renderer settings make the current
						// engine always output MPEG-TS
						if (!isOutputtingMPEGTS && renderer.isTranscodeToMPEGTS() &&
							(MEncoderVideo.ID.equals(engine.getEngineId()) || FFMpegVideo.ID.equals(engine.getEngineId()) ||
								VLCVideo.ID.equals(engine.getEngineId()) || AviSynthFFmpeg.ID.equals(engine.getEngineId()) ||
								AviSynthMEncoder.ID.equals(engine.getEngineId()))) {
							isOutputtingMPEGTS = true;
						}

						// If the engine is capable of automatically muxing to
						// MPEG-TS and the setting is enabled, it might be
						// MPEG-TS
						if (!isOutputtingMPEGTS &&
							((renderer.getUmsConfiguration().isMencoderMuxWhenCompatible() && MEncoderVideo.ID.equals(engine.getEngineId())) ||
								(renderer.getUmsConfiguration().isFFmpegMuxWithTsMuxerWhenCompatible() &&
									FFMpegVideo.ID.equals(engine.getEngineId())))) {
							/*
							 * Media renderer needs ORG_PN to be accurate. If
							 * the value does not match the mediaInfo, it won't play
							 * the mediaInfo. Often we can lazily predict the
							 * correct value to send, but due to MEncoder
							 * needing to mux via tsMuxeR, we need to work it
							 * all out before even sending the file list to
							 * these devices. This is very time-consuming so we
							 * should a) avoid using this chunk of code whenever
							 * possible, and b) design a better system. Ideally
							 * we would just mux to MPEG-PS instead of MPEG-TS
							 * so we could know it will always be PS, but most
							 * renderers will not accept H.264 inside MPEG-PS.
							 * Another option may be to always produce MPEG-TS
							 * instead and we should check if that will be OK
							 * for all renderers.
							 *
							 * This code block comes from
							 * Engine.setAudioAndSubs()
							 */
							if (renderer.isAccurateDLNAOrgPN()) {
								if (resolvedSubtitle == null) {
									MediaAudio audio = mediaAudio != null ? mediaAudio : item.resolveAudioStream();
									resolvedSubtitle = item.resolveSubtitlesStream(audio == null ? null : audio.getLang(), false);
								}

								if (resolvedSubtitle == null) {
									LOGGER.trace("We do not want a subtitle for {}", item.getName());
								} else {
									LOGGER.trace("We do want a subtitle for {}", item.getName());
								}
							}

							/**
							 * If:
							 * - There are no subtitles
							 * - This is not a DVD track
							 * - The media is muxable
							 * - The renderer accepts the video codec muxed to MPEG-TS
							 * then the file is MPEG-TS
							 *
							 * Note: This is an oversimplified duplicate of the engine logic, that
							 * should be fixed.
							 */
							if (resolvedSubtitle == null &&
									!item.hasExternalSubtitles() &&
									mediaInfo != null &&
									mediaInfo.getDvdtrack() == null &&
									Engine.isMuxable(mediaInfo.getDefaultVideoTrack(), renderer) &&
									renderer.isVideoStreamTypeSupportedInTranscodingContainer(mediaInfo)) {
								isOutputtingMPEGTS = true;
							}
						}

						if (isOutputtingMPEGTS) {
							if (renderer.isTranscodeToH264() && !VideoLanVideoStreaming.ID.equals(engine.getEngineId())) {
								dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMpegTsH264OrgPN(localizationValue, false);
							} else if (renderer.isTranscodeToMPEG2()) {
								dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMpegTsMpeg2OrgPN(localizationValue, mediaInfo, false);
							}
						}
					} else if (mediaInfo != null && mediaInfo.isMpegTS() && defaultVideoTrack != null) {
						// In this block, we are streaming the file
						if (defaultVideoTrack.isH264()) {
							dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMpegTsH264OrgPN(localizationValue, engine == null);
						} else if (defaultVideoTrack.isMpeg2()) {
							dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMpegTsMpeg2OrgPN(localizationValue, mediaInfo, engine == null);
						}
					}
				} else if (mediaInfo != null && mime.equals(HTTPResource.MPEGTS_TYPEMIME)) {
					// patterns - on Sony BDP m2ts clips aren't listed without this
					if ((engine == null && defaultVideoTrack != null && defaultVideoTrack.isH264()) || (engine != null && renderer.isTranscodeToH264())) {
						dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMpegTsH264OrgPN(localizationValue, engine == null);
					} else if ((engine == null && defaultVideoTrack != null && defaultVideoTrack.isMpeg2()) || (engine != null && renderer.isTranscodeToMPEG2())) {
						dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMpegTsMpeg2OrgPN(localizationValue, mediaInfo, engine == null);
					}
				} else if (mediaInfo != null && mime.equals(HTTPResource.MP4_TYPEMIME)) {
					if (engine == null && defaultVideoTrack != null && defaultVideoTrack.isH265() && defaultAudioTrack != null &&
						(defaultAudioTrack.isAC3() || defaultAudioTrack.isEAC3() ||
							defaultAudioTrack.isHEAAC())) {
						dlnaOrgPnFlags = "DLNA.ORG_PN=DASH_HEVC_MP4_UHD_NA";
					} else if (engine == null && defaultVideoTrack != null && defaultVideoTrack.isH264()) {
						dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMp4H264OrgPN(mediaInfo, renderer, true);
					}
				} else if (mediaInfo != null && mime.equals(HTTPResource.MATROSKA_TYPEMIME)) {
					if (engine == null && defaultVideoTrack != null && defaultVideoTrack.isH264()) {
						dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMkvH264OrgPN(mediaInfo, renderer, engine == null);
					}
				} else if (mediaInfo != null && mime.equals(HTTPResource.ASF_TYPEMIME)) {
					if (engine == null && defaultVideoTrack != null && defaultVideoTrack.getCodec().equals("vc1") && defaultAudioTrack != null && defaultAudioTrack.isWMA()) {
						if (mediaInfo.getDefaultVideoTrack() != null && mediaInfo.getDefaultVideoTrack().isHDVideo()) {
							dlnaOrgPnFlags = "DLNA.ORG_PN=VC1_ASF_AP_L2_WMA";
						} else {
							dlnaOrgPnFlags = "DLNA.ORG_PN=VC1_ASF_AP_L1_WMA";
						}
					}
				} else if (mediaInfo != null && mime.equals(HTTPResource.JPEG_TYPEMIME)) {
					int width = mediaInfo.getWidth();
					int height = mediaInfo.getHeight();
					if (width > 1024 || height > 768) { // 1024 * 768
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_LRG";
					} else if (width > 640 || height > 480) { // 640 * 480
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_MED";
					} else if (width > 160 || height > 160) { // 160 * 160
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_SM";
					} else {
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_TN";
					}

				} else if (mime.equals(HTTPResource.AUDIO_MP3_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=MP3";
				} else if (mime.substring(0, 9).equals(HTTPResource.AUDIO_LPCM_TYPEMIME) || mime.equals(HTTPResource.AUDIO_WAV_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=LPCM";
				}
			}

			if (dlnaOrgPnFlags != null) {
				dlnaOrgPnFlags = "DLNA.ORG_PN=" + renderer.getDlnaProfileId(dlnaOrgPnFlags.substring(12));
			}
		}

		return dlnaOrgPnFlags;
	}

	private static String getMpegPsOrgPN(int index) {
		if (index == 1 || index == 2) {
			return "MPEG_PS_NTSC";
		}

		return "MPEG_PS_PAL";
	}

	private static String getMpegTsMpeg2OrgPN(int index, MediaInfo media, boolean isStreaming) {
		String orgPN = "MPEG_TS_";
		if (media != null && media.getDefaultVideoTrack() == null && media.getDefaultVideoTrack().isHDVideo()) {
			orgPN += "HD";
		} else {
			orgPN += "SD";
		}

		orgPN += (
			switch (index) {
				case 1 -> "_NA";
				case 2 -> "_JP";
				default -> "_EU";
			}
		);

		if (!isStreaming) {
			orgPN += "_ISO";
		}

		return orgPN;
	}

	private static String getMpegTsH264OrgPN(int index, boolean isStreaming) {
		String orgPN = "AVC_TS";

		orgPN += (
			switch (index) {
				case 1 -> "_NA";
				case 2 -> "_JP";
				default -> "_EU";
			}
		);

		if (!isStreaming) {
			orgPN += "_ISO";
		}

		return orgPN;
	}

	private static String getMkvH264OrgPN(MediaInfo media, Renderer renderer, boolean isStreaming) {
		String orgPN = "AVC_MKV";

		if (media == null || media.getDefaultVideoTrack() == null ||
			media.getDefaultVideoTrack().getFormatProfile() == null ||
			media.getDefaultVideoTrack().getFormatProfile().contains("high")) {
			orgPN += "_HP";
		} else {
			orgPN += "_MP";
		}

		orgPN += "_HD";

		if (media != null && media.getDefaultAudioTrack() != null) {
			if (
				(
					isStreaming &&
					media.getDefaultAudioTrack().isAACLC()
				) || (
					!isStreaming &&
					renderer.isTranscodeToAAC()
				)
			) {
				orgPN += "_AAC_MULT5";
			} else if (
				(
					isStreaming &&
					media.getDefaultAudioTrack().isAC3()
				) || (
					!isStreaming &&
					renderer.isTranscodeToAC3()
				)
			) {
				orgPN += "_AC3";
			} else if (
				isStreaming &&
				media.getDefaultAudioTrack().isDTS()
			) {
				orgPN += "_DTS";
			} else if (
				isStreaming &&
				media.getDefaultAudioTrack().isEAC3()
			) {
				orgPN += "_EAC3";
			} else if (
				isStreaming &&
				media.getDefaultAudioTrack().isHEAAC()
			) {
				orgPN += "_HEAAC_L4";
			}
		}

		return orgPN;
	}

	private static String getMp4H264OrgPN(MediaInfo media, Renderer renderer, boolean isStreaming) {
		String orgPN = "AVC_MP4";

		if (media != null && media.getDefaultVideoTrack() != null &&
			media.getDefaultVideoTrack().getFormatProfile() != null) {
			if (media.getDefaultVideoTrack().getFormatProfile().contains("high")) {
				if (isStreaming && media.getDefaultAudioTrack() != null && media.getDefaultAudioTrack().isHEAAC()) {
					orgPN += "_HD_HEAACv2_L6";
				} else {
					orgPN += "_HP_HD";
				}
			} else if (media.getDefaultVideoTrack().getFormatProfile().contains("baseline")) {
				orgPN += "_BL";
			} else {
				orgPN += "_MP_SD";
			}
		} else {
			orgPN += "_MP_SD";
		}

		if (media != null && media.getDefaultAudioTrack() != null) {
			if (
				(
					isStreaming &&
					(
						media.getDefaultAudioTrack().isAC3() ||
						media.getDefaultAudioTrack().isEAC3()
					)
				) || (
					!isStreaming &&
					renderer.isTranscodeToAC3()
				)
			) {
				orgPN += "_EAC3";
			} else if (
				isStreaming &&
				media.getDefaultAudioTrack().isDTS()
			) {
				orgPN += "_DTS";
			} else if (
				isStreaming &&
				media.getDefaultAudioTrack().isDTSHD()
			) {
				orgPN += "_DTSHD";
			}
		}

		return orgPN;
	}

	private static String getWmvOrgPN(MediaInfo media, Renderer renderer, boolean isStreaming) {
		String orgPN = "WMV";
		if (media != null && media.getDefaultVideoTrack() != null &&  media.getDefaultVideoTrack().isHDVideo()) {
			orgPN += "HIGH";
		} else {
			orgPN += "MED";
		}

		if (media != null && media.getDefaultAudioTrack() != null) {
			if (
				(
					isStreaming &&
					media.getDefaultAudioTrack().isWMA()
				) || (
					!isStreaming &&
					renderer.isTranscodeToWMV()
				)
			) {
				orgPN += "_FULL";
			} else if (
				isStreaming &&
				(
					media.getDefaultAudioTrack().isWMAPro() ||
					media.getDefaultAudioTrack().isWMA10()
				)
			) {
				orgPN += "_PRO";
			}
		}

		return orgPN;
	}

}
