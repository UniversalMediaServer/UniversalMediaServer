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
package net.pms.formats;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.InputFile;
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class to store known information about a given format.
 */
public abstract class Format implements Cloneable, Serializable {
	private static final Tika TIKA = new Tika();
	private static final Logger LOGGER = LoggerFactory.getLogger(Format.class);
	private String icon = null;
	protected int type = UNKNOWN;
	protected Format secondaryFormat;

	/**
	 * The extension or protocol that was matched for a
	 * particular filename or URL. Requires {@link #match(String)}
	 * to be called first.
	 */
	private String matchedExtension;

	public enum Identifier {
		AUDIO_AS_VIDEO,
		BMP,
		DVRMS,
		FLAC,
		GIF,
		ISO,
		JPG,
		M4A,
		MKV,
		MP3,
		MPG,
		OGG,
		PNG,
		RAW,
		TIF,
		WAV,
		WEB,
		CUSTOM,
		PLAYLIST
	}

	public static final int AUDIO    =  1;
	public static final int IMAGE    =  2;
	public static final int VIDEO    =  4;
	public static final int UNKNOWN  =  8;
	public static final int PLAYLIST = 16;
	public static final int ISO      = 32;

	public int getType() {
		return type;
	}

	/**
	 * Returns the extension or protocol that was matched
	 * for a particular filename or URL. Requires {@link #match(String)} to be
	 * called first.
	 *
	 * @return The matched extension or protocol.
	 */
	public String getMatchedExtension() {
		return matchedExtension;
	}

	/**
	 * Sets the extension or protocol that was matched
	 * for a particular filename or URL.
	 *
	 * @param extension the extension or protocol that was matched.
	 * @since 1.90.0
	 */
	public void setMatchedExtension(String extension) {
		matchedExtension = extension;
	}

	public Format getSecondaryFormat() {
		return secondaryFormat;
	}

	public void setSecondaryFormat(Format secondaryFormat) {
		this.secondaryFormat = secondaryFormat;
	}

	public void setType(int type) {
		if (isUnknown()) {
			this.type = type;
		}
	}

	/**
	 * @deprecated Use {@link #getSupportedExtensions} instead.
	 */
	@Deprecated
	public String[] getId() {
		return getSupportedExtensions();
	}

	/**
	 * Returns a list of file extensions to use to identify
	 * a particular format e.g. "mp3" or "mpg". Extensions
	 * are expected to be in lower case. The default value is
	 * <code>null</code>, indicating no matching should be done
	 * by file extension.
	 *
	 * @return An array of extensions.
	 * @since 1.90.0
	 */
	public String[] getSupportedExtensions() {
		return null;
	}

	/**
	 * @deprecated Use {@link #isCompatible(DLNAMediaInfo, RendererConfiguration)} instead.
	 * <p>
	 * Returns whether or not a format can be handled by the PS3 natively.
	 * This means the format can be streamed to PS3 instead of having to be
	 * transcoded.
	 *
	 * @return True if the format can be handled by PS3, false otherwise.
	 */
	@Deprecated
	public abstract boolean ps3compatible();

	/**
	 * Returns whether or not media can be handled by the renderer natively,
	 * based on the given media information and renderer. If the format can be
	 * streamed (as opposed to having to be transcoded), <code>true</code> will
	 * be returned.
	 *
	 * @param media
	 *            The media information.
	 * @param renderer
	 *            The renderer for which to check. If <code>null</code> is set
	 *            as renderer, the default renderer configuration will be used.
	 *
	 * @return True if the format can be handled by the renderer, false
	 *         otherwise.
	 *
	 * @since 1.50.1
	 */
	public boolean isCompatible(DLNAMediaInfo media, RendererConfiguration renderer) {
		RendererConfiguration referenceRenderer;

		if (renderer != null) {
			// Use the provided renderer as reference
			referenceRenderer = renderer;
		} else {
			// Use the default renderer as reference
			referenceRenderer = RendererConfiguration.getDefaultConf();
		}

		// Let the renderer configuration decide on native compatibility
		return referenceRenderer.isCompatible(media, this);
	}

	public abstract boolean transcodable();

	public String mimeType() {
		return HTTPResource.getDefaultMimeType(type);
	}

	public void setIcon(String filename) {
		icon = filename;
	}

	public String getIcon() {
		return icon;
	}

	/**
	 * Returns whether or not this format matches the supplied filename.
	 * Returns false if the filename is a URI, otherwise matches
	 * against the file extensions returned by {@link #getSupportedExtensions()}.
	 *
	 * @param filename the filename to match
	 * @return <code>true</code> if the format matches, <code>false</code> otherwise.
	 */
	public boolean match(String filename) {
		if (filename == null) {
			return false;
		}

		filename = filename.toLowerCase();
		String[] supportedExtensions = getSupportedExtensions();

		if (supportedExtensions != null) {
			String protocol = FileUtil.getProtocol(filename);
			if (protocol != null) { // URIs are handled by WEB.match
				return false;
			}

			for (String extension : supportedExtensions) {
				String ext = extension.toLowerCase();
				if (filename.endsWith("." + ext)) {
					setMatchedExtension(ext);
					return true;
				}
			}
		}

		return false;
	}

	public boolean isVideo() {
		return (type & VIDEO) == VIDEO;
	}

	public boolean isAudio() {
		return (type & AUDIO) == AUDIO;
	}

	public boolean isImage() {
		return (type & IMAGE) == IMAGE;
	}

	public boolean isUnknown() {
		return (type & UNKNOWN) == UNKNOWN;
	}

	@Override
	protected Object clone() {
		Object o = null;
		try {
			o = super.clone();
		} catch (CloneNotSupportedException e) {
			LOGGER.error(null, e);
		}
		return o;
	}

	public Format duplicate() {
		return (Format) this.clone();
	}

	@Deprecated
	public void parse(DLNAMediaInfo media, InputFile file, int type) {
		parse(media, file, type, null);
	}

	/**
	 * Chooses which parsing method to parse the file with.
	 */
	public void parse(DLNAMediaInfo media, InputFile file, int type, RendererConfiguration renderer) {
		if (renderer != null && renderer.isUseMediaInfo()) {
			renderer.getFormatConfiguration().parse(media, file, this, type, renderer);
		} else {
			media.parse(file, this, type, false, false, renderer);
		}

		LOGGER.trace("Parsing results for file \"{}\": {}", file.toString(), media.toString());
	}

	/**
	 * Returns whether or not the matched extension of this format is among
	 * the list of supplied extensions.
	 *
	 * @param extensions String of comma-separated extensions
	 * @param moreExtensions String of comma-separated extensions
	 *
	 * @return True if this format matches an extension in the supplied lists,
	 * false otherwise.
	 *
	 * @see #match(String)
	 */
	public boolean skip(String extensions, String moreExtensions) {
		if ("*".equals(extensions)) {
			return true;
		}

		if (extensions != null && extensions.length() > 0) {
			StringTokenizer st = new StringTokenizer(extensions, ",");

			while (st.hasMoreTokens()) {
				String id = st.nextToken().toLowerCase();

				if (matchedExtension != null && matchedExtension.toLowerCase().equals(id)) {
					return true;
				}
			}
		}

		if (moreExtensions != null && moreExtensions.length() > 0) {
			StringTokenizer st = new StringTokenizer(moreExtensions, ",");

			while (st.hasMoreTokens()) {
				String id = st.nextToken().toLowerCase();

				if (matchedExtension != null && matchedExtension.toLowerCase().equals(id)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Return the class name for string representation.
	 * @return The name.
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	/**
	 * Returns the specific identifier for the format.
	 *
	 * @return The identifier.
	 */
	public abstract Identifier getIdentifier();
	
	public static String getExtension(String mimetype) {
		String ext = null;
		// Some defaults
		// Audio
		if (mimetype.equalsIgnoreCase("audio/mpeg")) {
			ext = ".mp3";
		} else if (mimetype.equalsIgnoreCase("audio/vnd.dlna.adts")) {
			ext = ".m4a";
		} else if (mimetype.equalsIgnoreCase("audio/x-flac") || mimetype.equalsIgnoreCase("audio/flac")) {
			ext = ".flac";
		} else if (mimetype.equalsIgnoreCase("audio/wav")
//				|| mimetype.equalsIgnoreCase("audio/x-ms-wav")
				) {
			ext = ".wav";
		} else if (mimetype.equalsIgnoreCase("audio/l16")) {
			ext = ".l16";
			
		// Video	
		} else if (mimetype.equalsIgnoreCase("video/mpeg") || mimetype.equalsIgnoreCase("video/mp4")) {
			ext = ".mp4";
		}
		
		if (ext != null)
			return ext;
		
		List<String> list = null;
		try {
			list = MimeTypes.getDefaultMimeTypes().forName(mimetype).getExtensions();
			Iterator<String> iterator = list.iterator();
			while (iterator.hasNext()) {
				String extension = (String) iterator.next();
				// Prefer . + 3 ext
				if (extension.length() == 4) {
					ext = extension;
					break;
				}
			}
		} catch (MimeTypeException e) {
			LOGGER.warn("Unknown mime type: {}", mimetype);
		}
		return ext;
	}
	
	public static String getMimetype(String filename) {
		String mimetype = TIKA.detect(filename);
		
		if (filename.endsWith(".l16"))
			mimetype = "audio/L16";
		if (filename.endsWith(".avi"))
			mimetype = "video/avi";
		return mimetype;
	}
	
	/**
	 * Whether UMS supports the mimetype
	 * 
	 * @param mimetype
	 * @return
	 */
	public static boolean isSupportedMimetype(String mimetype) {
		boolean supported = true;
		
		if ("audio/L24".equalsIgnoreCase(mimetype)
				|| "video/vnd.dlna.mpeg-tts".equalsIgnoreCase(mimetype)
				|| "video/mpeg2".equalsIgnoreCase(mimetype)
				)
				supported = false;
		return supported;
	}
}
