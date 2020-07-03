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
package net.pms.network;

import static net.pms.util.StringUtil.convertURLToFileName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.util.PropertiesUtil;

/**
 * Implements any item that can be transfered through the HTTP pipes.
 * In the PMS case, this item represents media files.
 * @see DLNAResource
 */
public class HTTPResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPResource.class);
	public static final String UNKNOWN_VIDEO_TYPEMIME = "video/mpeg";
	public static final String UNKNOWN_IMAGE_TYPEMIME = "image/jpeg";
	public static final String UNKNOWN_AUDIO_TYPEMIME = "audio/mpeg";
	public static final String AUDIO_TRANSCODE = "audio/transcode";
	public static final String VIDEO_TRANSCODE = "video/transcode";
	public static final String AUDIO_AC3_TYPEMIME = "audio/vnd.dolby.dd-raw";
	public static final String AUDIO_ADPCM_TYPEMIME = "audio/x-adpcm";
	public static final String AUDIO_ADTS_TYPEMIME = "audio/vnd.dlna.adts";
	public static final String AUDIO_AIFF_TYPEMIME = "audio/aiff";
	public static final String AUDIO_APE_TYPEMIME = "audio/x-ape";
	public static final String AUDIO_ATRAC_TYPEMIME = "audio/x-sony-oma";
	public static final String AUDIO_AU_TYPEMIME = "audio/basic";
	public static final String AUDIO_DSF_TYPEMIME = "audio/x-dsf";
	public static final String AUDIO_DFF_TYPEMIME = "audio/x-dff";
	public static final String AUDIO_DTS_TYPEMIME = "audio/vnd.dts";
	public static final String AUDIO_DTSHD_TYPEMIME = "audio/vnd.dts.hd";
	public static final String AUDIO_EAC3_TYPEMIME = "audio/eac3";
	public static final String AUDIO_FLAC_TYPEMIME = "audio/x-flac";
	public static final String AUDIO_LPCM_TYPEMIME = "audio/L16";
	public static final String AUDIO_M4A_TYPEMIME = "audio/mp4";
	public static final String AUDIO_MKA_TYPEMIME = "audio/x-matroska";
	public static final String AUDIO_MLP_TYPEMIME = "audio/vnd.dolby.mlp";
	public static final String AUDIO_MP3_TYPEMIME = "audio/mpeg";
	public static final String AUDIO_MP2_TYPEMIME = "audio/mpeg";
	public static final String AUDIO_MPA_TYPEMIME = "audio/mpeg";
	public static final String AUDIO_MPC_TYPEMIME = "audio/x-musepack";
	public static final String AUDIO_OGA_TYPEMIME = "audio/ogg";
	public static final String AUDIO_RA_TYPEMIME = "audio/vnd.rn-realaudio";
	public static final String AUDIO_SHN_TYPEMIME = "audio/x-shn";
	public static final String AUDIO_THREEGPPA_TYPEMIME = "audio/3gpp";
	public static final String AUDIO_THREEGPP2A_TYPEMIME = "audio/3gpp2";
	public static final String AUDIO_TRUEHD_TYPEMIME = "audio/vnd.dolby.mlp";
	public static final String AUDIO_TTA_TYPEMIME = "audio/x-tta";
	public static final String AUDIO_VORBIS_TYPEMIME = "audio/ogg";
	public static final String AUDIO_WAV_TYPEMIME = "audio/wav";
	public static final String AUDIO_WEBM_TYPEMIME = "audio/webm";
	public static final String AUDIO_WMA_TYPEMIME = "audio/x-ms-wma";
	public static final String AUDIO_WV_TYPEMIME = "audio/x-wavpack";
	public static final String ASF_TYPEMIME = "video/x-ms-asf";
	public static final String AVI_TYPEMIME = "video/avi";
	public static final String BMP_TYPEMIME = "image/bmp";
	public static final String DIVX_TYPEMIME = "video/x-divx";
	public static final String FLV_TYPEMIME = "video/x-flv";
	public static final String GIF_TYPEMIME = "image/gif";
	public static final String JPEG_TYPEMIME = "image/jpeg";
	public static final String M4V_TYPEMIME = "video/x-m4v";
	public static final String MATROSKA_TYPEMIME = "video/x-matroska";
	public static final String MOV_TYPEMIME = "video/quicktime";
	public static final String MP4_TYPEMIME = "video/mp4";
	public static final String MPEG_TYPEMIME = "video/mpeg";
	public static final String MPEGTS_TYPEMIME = "video/vnd.dlna.mpeg-tts";
	public static final String PNG_TYPEMIME = "image/png";
	public static final String RM_TYPEMIME = "application/vnd.rn-realmedia";
	public static final String THREEGPP_TYPEMIME = "video/3gpp";
	public static final String THREEGPP2_TYPEMIME = "video/3gpp2";
	public static final String TIFF_TYPEMIME = "image/tiff";
	public static final String WMV_TYPEMIME = "video/x-ms-wmv";
	public static final String OGG_TYPEMIME = "video/ogg";
	public static final String WEBM_TYPEMIME = "video/webm";
	public HTTPResource() { }

	/**
	 * Returns for a given item type the default MIME type associated. This is used in the HTTP transfers
	 * as in the client might do different things for different MIME types.
	 * @param type Type for which the default MIME type is needed.
	 * @return Default MIME associated with the file type.
	 */
	public static String getDefaultMimeType(int type) {
		String mimeType = HTTPResource.UNKNOWN_VIDEO_TYPEMIME;

		if (type == Format.VIDEO) {
			mimeType = HTTPResource.UNKNOWN_VIDEO_TYPEMIME;
		} else if (type == Format.IMAGE) {
			mimeType = HTTPResource.UNKNOWN_IMAGE_TYPEMIME;
		} else if (type == Format.AUDIO) {
			mimeType = HTTPResource.UNKNOWN_AUDIO_TYPEMIME;
		}

		return mimeType;
	}

	/**
	 * Returns an InputStream associated with the fileName.
	 * @param fileName TODO Absolute or relative file path.
	 * @return If found, an InputStream associated with the fileName. null otherwise.
	 */
	protected InputStream getResourceInputStream(String fileName) {
		fileName = "/resources/" + fileName;
		fileName = fileName.replaceAll("//", "/");
		ClassLoader cll = this.getClass().getClassLoader();
		InputStream is = cll.getResourceAsStream(fileName.substring(1));

		while (is == null && cll.getParent() != null) {
			cll = cll.getParent();
			is = cll.getResourceAsStream(fileName.substring(1));
		}

		return is;
	}

	/**
	 * Creates an InputStream based on a URL. This is used while accessing external resources
	 * like online radio stations.
	 * @param u URL.
	 * @param saveOnDisk If true, the file is first downloaded to the temporary folder.
	 * @return InputStream that can be used for sending to the media renderer.
	 * @throws IOException
	 * @see #downloadAndSendBinary(String)
	 */
	public static InputStream downloadAndSend(String u, boolean saveOnDisk) throws IOException {
		URL url = new URL(u);
		File f = null;

		if (saveOnDisk) {
			String host = url.getHost();
			String hostName = convertURLToFileName(host);
			String fileName = url.getFile();
			fileName = convertURLToFileName(fileName);
			File hostDir = new File(PMS.getConfiguration().getTempFolder(), hostName);

			if (!hostDir.isDirectory()) {
				if (!hostDir.mkdir()) {
					LOGGER.debug("Cannot create directory: {}", hostDir.getAbsolutePath());
				}
			}

			f = new File(hostDir, fileName);

			if (f.exists()) {
				return new FileInputStream(f);
			}
		}

		byte[] content = downloadAndSendBinary(u, saveOnDisk, f);
		return new ByteArrayInputStream(content);
	}

	/**
	 * Overloaded method for {@link #downloadAndSendBinary(String, boolean, File)}, without storing a file on the filesystem.
	 * @param u URL to retrieve.
	 * @return byte array.
	 * @throws IOException
	 */
	protected static byte[] downloadAndSendBinary(String u) throws IOException {
		return downloadAndSendBinary(u, false, null);
	}

	/**
	 * Returns a byte array representation of the file given by the URL. The file is downloaded and optionally stored on the filesystem.
	 * @param u URL to retrieve.
	 * @param saveOnDisk If true, store the file on the filesystem.
	 * @param f If saveOnDisk is true, then store the contents of the file represented by u in the associated File. f needs to be opened before
	 * calling this function.
	 * @return The byte array
	 * @throws IOException
	 */
	protected static byte[] downloadAndSendBinary(String u, boolean saveOnDisk, File f) throws IOException {
		URL url = new URL(u);

		// The URL may contain user authentication information
		Authenticator.setDefault(new HTTPResourceAuthenticator());
		HTTPResourceAuthenticator.addURL(url);

		LOGGER.debug("Retrieving " + url.toString());
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		URLConnection conn = url.openConnection();

		// GameTrailers blocks user-agents that identify themselves as "Java"
		conn.setRequestProperty("User-agent", PropertiesUtil.getProjectProperties().get("project.name") + " " + PMS.getVersion());

		CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
		if (
			cookieManager != null &&
			cookieManager.getCookieStore() != null &&
			cookieManager.getCookieStore().getCookies() != null &&
			cookieManager.getCookieStore().getCookies().size() > 0
		) {
			// While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
			conn.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
		}

		FileOutputStream fOUT;
		try (InputStream in = conn.getInputStream()) {
			fOUT = null;
			if (saveOnDisk && f != null) {
				// fileName = convertURLToFileName(fileName);
				fOUT = new FileOutputStream(f);
			}
			byte[] buf = new byte[4096];
			int n;
			while ((n = in.read(buf)) > -1) {
				bytes.write(buf, 0, n);

				if (fOUT != null) {
					fOUT.write(buf, 0, n);
				}
			}
		}

		if (fOUT != null) {
			fOUT.close();
		}

		return bytes.toByteArray();
	}

	/**
	 * Returns the supplied MIME type customized for the supplied media renderer according to the renderer's aliasing rules.
	 * @param renderer media renderer to customize the MIME type for.
	 * @param resource the resource
	 * @return The MIME type
	 */
	public String getRendererMimeType(RendererConfiguration renderer, DLNAResource resource) {
		return renderer.getMimeType(resource);
	}

	public int getDLNALocalesCount() {
		return 3;
	}

	public final String getMPEG_PS_OrgPN(int index) {
		if (index == 1 || index == 2) {
			return "MPEG_PS_NTSC";
		}

		return "MPEG_PS_PAL";
	}

	public final String getMPEG_TS_MPEG2_ISOOrgPN(int index, DLNAMediaInfo media) {
		String orgPN = "MPEG_TS_";
		if (media != null && media.isHDVideo()) {
			orgPN += "HD";
		} else {
			orgPN += "SD";
		}

		if (index == 1) {
			return orgPN + "_NA_ISO";
		} else if (index == 2) {
			return orgPN + "_JP_ISO";
		}

		return orgPN + "_EU_ISO";
	}

	public final String getMPEG_TS_MPEG2_OrgPN(int index, DLNAMediaInfo media, RendererConfiguration mediaRenderer) {
		String orgPN = "MPEG_TS_";
		if (media != null && media.isHDVideo()) {
			orgPN += "HD";
		} else {
			orgPN += "SD";
		}

		if (index == 1) {
			return orgPN + "_NA";
		} else if (index == 2) {
			return orgPN + "_JP";
		}

		return orgPN + "_EU";
	}

	public final String getMPEG_TS_H264_OrgPN(int index, DLNAMediaInfo media, RendererConfiguration mediaRenderer, boolean isStreaming) {
		String orgPN = "AVC_TS";
		if (media != null && media.isHDVideo()) {
			orgPN += "_HD";
		} else {
			orgPN += "_SD";
		}

		if (media != null && media.getFirstAudioTrack() != null) {
			if (
				(
					isStreaming &&
					media.getFirstAudioTrack().isAACLC()
				) ||
				(
					!isStreaming &&
					mediaRenderer.isTranscodeToAAC()
				)
			) {
				orgPN += "_AAC";
			} else if (
				(
					isStreaming &&
					media.getFirstAudioTrack().isAC3()
				) ||
				(
					!isStreaming &&
					mediaRenderer.isTranscodeToAC3()
				)
			) {
				orgPN += "_AC3";
			}
		}

		if (index == 1) {
			return orgPN + "_NA";
		} else if (index == 2) {
			return orgPN + "_JP";
		}

		return orgPN + "_EU";
	}
}
