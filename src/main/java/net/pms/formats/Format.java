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

import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.InputFile;
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class to store known information about a given format.
 */
public abstract class Format implements Cloneable {
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
		AC3,
		ADPCM,
		ADTS,
		AIFF,
		APE,
		ATRAC,
		AU,
		AUDIO_AS_VIDEO,
		ASS,
		BMP,
		DSD,
		DTS,
		DVRMS,
		EAC3,
		FLAC,
		GIF,
		IDX,
		ISO,
		JPG,
		M4A,
		MICRODVD,
		MKA,
		MKV,
		MLP,
		MP3,
		MPA,
		MPC,
		MPG,
		OGG,
		PNG,
		RA,
		RAW,
		SAMI,
		SHN,
		SUBRIP,
		SUP,
		THD,
		THREEGA,
		THREEG2A,
		TIF,
		TTA,
		TXT,
		WAV,
		WEB,
		WEBVTT,
		WMA,
		WV,
		CUSTOM,
		PLAYLIST
	}

	public static final int AUDIO    =  1;
	public static final int IMAGE    =  2;
	public static final int VIDEO    =  4;
	public static final int UNKNOWN  =  8;
	public static final int PLAYLIST = 16;
	public static final int ISO      = 32;
	public static final int SUBTITLE = 64;

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

	public boolean isSubtitle() {
		return (type & SUBTITLE) == SUBTITLE;
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
	 *
	 * @return True if this format matches an extension in the supplied lists,
	 * false otherwise.
	 *
	 * @see #match(String)
	 */
	public boolean skip(String... extensions) {
		for (String extensionsString : extensions) {
			if (extensionsString == null) {
				continue;
			}

			if ("*".equals(extensionsString)) {
				return true;
			}

			String[] extensionsArray = extensionsString.split(",");
			for (String extension : extensionsArray) {
				if (StringUtil.hasValue(extension) && extension.equalsIgnoreCase(matchedExtension)) {
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
}
