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

import java.io.*;
import java.net.Authenticator;
import java.net.URL;
import java.net.URLConnection;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.util.PropertiesUtil;
import static net.pms.util.StringUtil.convertURLToFileName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static final String AUDIO_MP3_TYPEMIME = "audio/mpeg";
	public static final String AUDIO_MP4_TYPEMIME = "audio/x-m4a";
	public static final String AUDIO_WAV_TYPEMIME = "audio/wav";
	public static final String AUDIO_WMA_TYPEMIME = "audio/x-ms-wma";
	public static final String AUDIO_FLAC_TYPEMIME = "audio/x-flac";
	public static final String AUDIO_OGG_TYPEMIME = "audio/x-ogg";
	public static final String AUDIO_LPCM_TYPEMIME = "audio/L16";
	public static final String MPEG_TYPEMIME = "video/mpeg";
	public static final String MP4_TYPEMIME = "video/mp4";
	public static final String AVI_TYPEMIME = "video/avi";
	public static final String DIVX_TYPEMIME = "video/x-divx";
	public static final String WMV_TYPEMIME = "video/x-ms-wmv";
	public static final String ASF_TYPEMIME = "video/x-ms-asf";
	public static final String MATROSKA_TYPEMIME = "video/x-matroska";
	public static final String VIDEO_TRANSCODE = "video/transcode";
	public static final String AUDIO_TRANSCODE = "audio/transcode";
	public static final String PNG_TYPEMIME = "image/png";
	public static final String JPEG_TYPEMIME = "image/jpeg";
	public static final String TIFF_TYPEMIME = "image/tiff";
	public static final String GIF_TYPEMIME = "image/gif";
	public static final String BMP_TYPEMIME = "image/bmp";
	public static final String THREEGPP_TYPEMIME = "video/3gpp";
	public static final String THREEGPP2_TYPEMIME = "video/3gpp2";
	public static final String MOV_TYPEMIME = "video/quicktime";
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
	protected static InputStream downloadAndSend(String u, boolean saveOnDisk) throws IOException {
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
	 * @param mimetype MIME type to customize.
	 * @param renderer media renderer to customize the MIME type for.
	 * @return The MIME type
	 */
	public String getRendererMimeType(String mimetype, RendererConfiguration renderer) {
		return renderer.getMimeType(mimetype);
	}

	public int getDLNALocalesCount() {
		return 3;
	}

	public final String getMPEG_PS_PALLocalizedValue(int index) {
		if (index == 1 || index == 2) {
			return "MPEG_PS_NTSC";
		}

		return "MPEG_PS_PAL";
	}

	public final String getMPEG_TS_SD_EU_ISOLocalizedValue(int index) {
		if (index == 1) {
			return "MPEG_TS_SD_NA_ISO";
		}

		if (index == 2) {
			return "MPEG_TS_SD_JP_ISO";
		}

		return "MPEG_TS_SD_EU_ISO";
	}

	public final String getMPEG_TS_SD_EULocalizedValue(int index) {
		if (index == 1) {
			return "MPEG_TS_SD_NA";
		}

		if (index == 2) {
			return "MPEG_TS_SD_JP";
		}

		return "MPEG_TS_SD_EU";
	}

	public final String getMPEG_TS_EULocalizedValue(int index, boolean isHD) {
		String definition = "SD";
		if (isHD) {
			definition = "HD";
		}

		if (index == 1) {
			return "MPEG_TS_" + definition + "_NA";
		}
		if (index == 2) {
			return "MPEG_TS_" + definition + "_JP";
		}

		return "MPEG_TS_" + definition + "_EU";
	}
}
