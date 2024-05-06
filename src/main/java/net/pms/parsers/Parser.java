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
package net.pms.parsers;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.formats.AudioAsVideo;
import net.pms.formats.Format;
import net.pms.image.ExifInfo;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.network.HTTPResource;
import net.pms.store.ThumbnailSource;
import net.pms.util.InputFile;
import net.pms.util.UnknownFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final String MANUAL_PARSER = "NO_PARSER";

	/**
	 * This class is not meant to be instantiated.
	 */
	protected Parser() {
	}

	/**
	 * Chooses which parsing method to parse the file with.
	 */
	public static void parse(MediaInfo media, InputFile file, Format ext, int type) {
		//ensure media is not already parsing or is parsed
		media.waitMediaParsing(5);
		if (media.isMediaParsed()) {
			return;
		}
		media.resetParser();
		if (file.getFile() != null) {
			// Special parsing for RealAudio 1.0 and 2.0 which isn't handled by MediaInfo or JAudioTagger
			if (ext.getIdentifier() == Format.Identifier.RA && RealAudioParser.parse(media, file, type)) {
				return;
			}
			// Special parsing for raw image
			if (ext.getIdentifier() == Format.Identifier.RAW && DCRawParser.parse(media, file, type)) {
				return;
			}
			// MediaInfo can't correctly parse ADPCM, DFF, DSF or PNM
			if (MediaInfoParser.isValid() &&
					ext.getIdentifier() != Format.Identifier.ADPCM &&
					ext.getIdentifier() != Format.Identifier.DFF &&
					ext.getIdentifier() != Format.Identifier.DSF &&
					ext.getIdentifier() != Format.Identifier.PNM) {
				MediaInfoParser.parse(media, file.getFile(), type);
			} else if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				JaudiotaggerParser.parse(media, file.getFile(), ext);
			} else {
				FFmpegParser.parse(media, file, ext, type);
			}
		} else {
			FFmpegParser.parse(media, file, ext, type);
		}
	}

	public static void postParse(MediaInfo mediaInfo, int type) {
		String container = mediaInfo.getContainer();
		String codecA = mediaInfo.getDefaultAudioTrack() != null ? mediaInfo.getDefaultAudioTrack().getCodec() : null;
		String codecV = mediaInfo.getDefaultVideoTrack() != null ? mediaInfo.getDefaultVideoTrack().getCodec() : null;
		String mimeType = null;

		if (container != null) {
			mimeType = switch (container) {
				case FormatConfiguration.AVI -> HTTPResource.AVI_TYPEMIME;
				case FormatConfiguration.ASF -> HTTPResource.ASF_TYPEMIME;
				case FormatConfiguration.FLV -> HTTPResource.FLV_TYPEMIME;
				case FormatConfiguration.M4V -> HTTPResource.M4V_TYPEMIME;
				case FormatConfiguration.MP4 -> HTTPResource.MP4_TYPEMIME;
				case FormatConfiguration.MPEGPS -> HTTPResource.MPEG_TYPEMIME;
				case FormatConfiguration.MPEGTS -> HTTPResource.MPEGTS_TYPEMIME;
				case FormatConfiguration.MPEGTS_HLS -> HTTPResource.HLS_TYPEMIME;
				case FormatConfiguration.WMV -> HTTPResource.WMV_TYPEMIME;
				case FormatConfiguration.MOV -> HTTPResource.MOV_TYPEMIME;
				case FormatConfiguration.ADPCM -> HTTPResource.AUDIO_ADPCM_TYPEMIME;
				case FormatConfiguration.ADTS -> HTTPResource.AUDIO_ADTS_TYPEMIME;
				case FormatConfiguration.M4A -> HTTPResource.AUDIO_M4A_TYPEMIME;
				case FormatConfiguration.AC3 -> HTTPResource.AUDIO_AC3_TYPEMIME;
				case FormatConfiguration.AU -> HTTPResource.AUDIO_AU_TYPEMIME;
				case FormatConfiguration.DFF -> HTTPResource.AUDIO_DFF_TYPEMIME;
				case FormatConfiguration.DSF -> HTTPResource.AUDIO_DSF_TYPEMIME;
				case FormatConfiguration.EAC3 -> HTTPResource.AUDIO_EAC3_TYPEMIME;
				case FormatConfiguration.MPA -> HTTPResource.AUDIO_MPA_TYPEMIME;
				case FormatConfiguration.MP2 -> HTTPResource.AUDIO_MP2_TYPEMIME;
				case FormatConfiguration.AIFF -> HTTPResource.AUDIO_AIFF_TYPEMIME;
				case FormatConfiguration.ATRAC -> HTTPResource.AUDIO_ATRAC_TYPEMIME;
				case FormatConfiguration.MKA -> HTTPResource.AUDIO_MKA_TYPEMIME;
				case FormatConfiguration.MLP -> HTTPResource.AUDIO_MLP_TYPEMIME;
				case FormatConfiguration.MONKEYS_AUDIO -> HTTPResource.AUDIO_APE_TYPEMIME;
				case FormatConfiguration.MPC -> HTTPResource.AUDIO_MPC_TYPEMIME;
				case FormatConfiguration.OGG -> HTTPResource.OGG_TYPEMIME;
				case FormatConfiguration.OGA -> HTTPResource.AUDIO_OGA_TYPEMIME;
				case FormatConfiguration.RA -> HTTPResource.AUDIO_RA_TYPEMIME;
				case FormatConfiguration.RM -> HTTPResource.RM_TYPEMIME;
				case FormatConfiguration.SHORTEN -> HTTPResource.AUDIO_SHN_TYPEMIME;
				case FormatConfiguration.THREEGA -> HTTPResource.AUDIO_THREEGPPA_TYPEMIME;
				case FormatConfiguration.TRUEHD -> HTTPResource.AUDIO_TRUEHD_TYPEMIME;
				case FormatConfiguration.TTA -> HTTPResource.AUDIO_TTA_TYPEMIME;
				case FormatConfiguration.WAVPACK -> HTTPResource.AUDIO_WV_TYPEMIME;
				case FormatConfiguration.WEBA -> HTTPResource.AUDIO_WEBM_TYPEMIME;
				case FormatConfiguration.WEBP -> HTTPResource.WEBP_TYPEMIME;
				case FormatConfiguration.WMA, FormatConfiguration.WMA10 -> HTTPResource.AUDIO_WMA_TYPEMIME;
				case FormatConfiguration.BMP -> HTTPResource.BMP_TYPEMIME;
				case FormatConfiguration.GIF -> HTTPResource.GIF_TYPEMIME;
				case FormatConfiguration.JPEG -> HTTPResource.JPEG_TYPEMIME;
				case FormatConfiguration.PNG -> HTTPResource.PNG_TYPEMIME;
				case FormatConfiguration.TIFF -> HTTPResource.TIFF_TYPEMIME;
				default -> mimeType;
			};
		}

		if (mimeType == null) {
			if (codecV != null && !codecV.equals(MediaLang.UND)) {
				if ("matroska".equals(container) || "mkv".equals(container)) {
					mimeType = HTTPResource.MATROSKA_TYPEMIME;
				} else if ("ogg".equals(container)) {
					mimeType = HTTPResource.OGG_TYPEMIME;
				} else if ("3gp".equals(container)) {
					mimeType = HTTPResource.THREEGPP_TYPEMIME;
				} else if ("3g2".equals(container)) {
					mimeType = HTTPResource.THREEGPP2_TYPEMIME;
				} else if ("webm".equals(container)) {
					mimeType = HTTPResource.WEBM_TYPEMIME;
				} else if (container != null && container.startsWith("flash")) {
					mimeType = HTTPResource.FLV_TYPEMIME;
				} else if (codecV.startsWith("h264") || codecV.equals("h263") || codecV.equals("mpeg4") || codecV.equals("mp4")) {
					mimeType = HTTPResource.MP4_TYPEMIME;
				} else if (codecV.contains("mpeg") || codecV.contains("mpg")) {
					mimeType = HTTPResource.MPEG_TYPEMIME;
				}
			} else if ((codecV == null || codecV.equals(MediaLang.UND)) && codecA != null) {
				if ("ogg".equals(container) || "oga".equals(container)) {
					mimeType = HTTPResource.AUDIO_OGA_TYPEMIME;
				} else if ("3gp".equals(container)) {
					mimeType = HTTPResource.AUDIO_THREEGPPA_TYPEMIME;
				} else if ("3g2".equals(container)) {
					mimeType = HTTPResource.AUDIO_THREEGPP2A_TYPEMIME;
				} else if ("adts".equals(container)) {
					mimeType = HTTPResource.AUDIO_ADTS_TYPEMIME;
				} else if ("matroska".equals(container) || "mkv".equals(container)) {
					mimeType = HTTPResource.AUDIO_MKA_TYPEMIME;
				} else if ("webm".equals(container)) {
					mimeType = HTTPResource.AUDIO_WEBM_TYPEMIME;
				} else if (codecA.contains("mp3")) {
					mimeType = HTTPResource.AUDIO_MP3_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.MPA)) {
					mimeType = HTTPResource.AUDIO_MPA_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.MP2)) {
					mimeType = HTTPResource.AUDIO_MP2_TYPEMIME;
				} else if (codecA.contains("flac")) {
					mimeType = HTTPResource.AUDIO_FLAC_TYPEMIME;
				} else if (codecA.contains("vorbis")) {
					mimeType = HTTPResource.AUDIO_VORBIS_TYPEMIME;
				} else if (codecA.contains("asf") || codecA.startsWith("wm")) {
					mimeType = HTTPResource.AUDIO_WMA_TYPEMIME;
				} else if (codecA.contains("pcm") || codecA.contains("wav") || codecA.contains("dts")) {
					mimeType = HTTPResource.AUDIO_WAV_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.TRUEHD)) {
					mimeType = HTTPResource.AUDIO_TRUEHD_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DTS)) {
					mimeType = HTTPResource.AUDIO_DTS_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DTSHD)) {
					mimeType = HTTPResource.AUDIO_DTSHD_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.EAC3)) {
					mimeType = HTTPResource.AUDIO_EAC3_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.ADPCM)) {
					mimeType = HTTPResource.AUDIO_ADPCM_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DFF)) {
					mimeType = HTTPResource.AUDIO_DFF_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DSF)) {
					mimeType = HTTPResource.AUDIO_DSF_TYPEMIME;
				}
			}

			if (mimeType == null) {
				mimeType = HTTPResource.getDefaultMimeType(type);
			}
		}
		mediaInfo.setMimeType(mimeType);
	}

	/**
	 * Parse media for Thumb.
	 */
	public static DLNAThumbnail getThumbnail(MediaInfo media, InputFile inputFile, Format ext, int type, Double seekPosition) {
		if (inputFile != null) {
			File file = inputFile.getFile();

			if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				return JaudiotaggerParser.getThumbnail(media, file);
			} else if (type == Format.IMAGE && file != null) {
				// Create the thumbnail image
				try {
					if (media.getImageInfo() instanceof ExifInfo exifInfo && exifInfo.hasExifThumbnail() && !exifInfo.isImageIOSupported()) {
						/*
						 * XXX Extraction of thumbnails was removed in version
						 * 2.10.0 of metadata-extractor because of a bug in
						 * related code. This section is deactivated while
						 * waiting for this to be made available again.
						 *
						 * Images supported by ImageIO or DCRaw aren't affected,
						 * so this only applied to very few images anyway.
						 * It could extract thumbnails for some "raw" images
						 * if DCRaw was disabled.
						 *
						 */
					} else {
						// This will fail with UnknownFormatException for any image formats not supported by ImageIO
						DLNAThumbnail thumb = DLNAThumbnail.toThumbnail(
								Files.newInputStream(file.toPath()),
								320,
								320,
								ImagesUtil.ScaleType.MAX,
								ImageFormat.SOURCE,
								false
						);
						if (thumb != null) {
							media.setThumbnailSource(ThumbnailSource.EMBEDDED);
							return thumb;
						}
					}
				} catch (EOFException e) {
					LOGGER.debug(
							"Error generating thumbnail for \"{}\": Unexpected end of file, probably corrupt file or read error.",
							file.getName()
					);
				} catch (UnknownFormatException e) {
					LOGGER.debug("Could not generate thumbnail for \"{}\" because the format is unknown: {}", file.getName(), e.getMessage());
				} catch (IOException e) {
					LOGGER.debug("Error generating thumbnail for \"{}\": {}", file.getName(), e.getMessage());
					LOGGER.trace("", e);
				}
			} else if (type == Format.VIDEO) {
				DLNAThumbnail thumb;
				if (seekPosition == null && media.hasVideoMetadata() && media.getVideoMetadata().getPoster() != null) {
					//API Poster
					thumb = JavaHttpClient.getThumbnail(media.getVideoMetadata().getPoster());
					if (thumb != null) {
						//if here, metadata was localized
						media.setThumbnailSource(ThumbnailSource.TMDB_LOC);
						return thumb;
					}
				}
				if (CONFIGURATION.isUseMplayerForVideoThumbs()) {
					//Mplayer parsing
					thumb = MPlayerParser.getThumbnail(media, inputFile, seekPosition);
					if (thumb != null) {
						return thumb;
					}
				}
				//FFmpeg parsing
				thumb = FFmpegParser.getThumbnail(media, inputFile, seekPosition);
				if (thumb != null) {
					return thumb;
				}
			}
		}
		return null;
	}

}
