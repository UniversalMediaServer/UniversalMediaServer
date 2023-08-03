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
package net.pms.image;

import java.util.Locale;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAImageProfile;

/**
 * Defines the different image format supported by the ImageIO parser
 * with the currently installed plugins plus the special value {@code SOURCE}.
 * If more plugins are added, more entries should be added here and in
 * {@link ImageIOTools#read(ImageInputStream)}.
 */
public enum ImageFormat {
	ARW,
	BMP,
	CR2,
	CRW,
	CUR,
	DCR,
	DCX,
	/** Adobe Digital Negative */
	DNG,
	/** Used for general "raw" formats without an explicit definition. */
	RAW,
	GIF,
	ICNS,
	ICO,
	/** Interchange File Format */
	IFF,
	JPEG,
	KDC,
	NEF,
	ORF,
	PCX,
	/** Apple PICT */
	PICT,
	PEF,
	PNG,
	PNM,
	PSD,
	RAF,
	/** Radiance HDR/RGBE */
	RGBE,
	RW2,
	/** Silicon Graphics SGI image format */
	SGI,
	/** A special code that means "whatever the source image uses" where that's relevant. */
	SOURCE,
	SRW,
	/** Truevision Targa Graphic */
	TGA,
	TIFF,
	WBMP,
	WEBP;

	public static final int TAG_DNG_VERSION = 0xC612;

	/**
	 * @return The image format for the given imageProfile.
	 */
	public static ImageFormat toImageFormat(DLNAImageProfile imageProfile) {
		if (imageProfile == null) {
			return null;
		}
		return imageProfile.getFormat();
	}

	/**
	 * Tries to parse {@link ImageFormat} from a text representation of an image
	 * format.
	 *
	 * @param formatName the {@link String} containing the format code.
	 * @return The parsed {@link ImageFormat} or {@code null} if the parsing
	 *         fails.
	 */
	public static ImageFormat toImageFormat(String formatName) {
		ImageFormat result = null;
		if (formatName != null) {
			formatName = formatName.toUpperCase(Locale.ROOT);
			if (formatName.contains("BMP")) {
				result = ImageFormat.BMP;
			} else if (formatName.contains("CUR")) {
				result = ImageFormat.CUR;
			} else if (formatName.contains("DCX")) {
				result = ImageFormat.DCX;
			} else if (formatName.contains("GIF")) {
				result = ImageFormat.GIF;
			} else if (formatName.contains("ICNS")) {
				result = ImageFormat.ICNS;
			} else if (formatName.contains("ICO")) {
				result = ImageFormat.ICO;
			} else if (formatName.equals("IFF")) {
				result = ImageFormat.IFF;
			} else if (formatName.contains("JPEG")) {
				result = ImageFormat.JPEG;
			} else if (formatName.contains("PCX")) {
				result = ImageFormat.PCX;
			} else if (formatName.contains("PIC") || formatName.contains("PCT")) {
				result = ImageFormat.PICT;
			} else if (formatName.contains("PNG")) {
				result = ImageFormat.PNG;
			} else if (formatName.contains("PNM") || formatName.contains("PBM") || formatName.contains("PGM") ||
					formatName.contains("PPM") || formatName.contains("PAM") || formatName.contains("PFM")) {
				result = ImageFormat.PNM;
			} else if (formatName.contains("PSD")) {
				result = ImageFormat.PSD;
			} else if (formatName.contains("RGBE") || formatName.contains("HDR") || formatName.contains("XYZE")) {
				result = ImageFormat.RGBE;
			} else if (formatName.contains("SGI") || formatName.equals("RLE")) {
				result = ImageFormat.SGI;
			} else if (formatName.contains("TGA") || formatName.contains("TARGA")) {
				result = ImageFormat.TGA;
			} else if (formatName.contains("TIFF")) {
				result = ImageFormat.TIFF;
			} else if (formatName.contains("WBMP")) {
				result = ImageFormat.WBMP;
			} else if (formatName.contains("WEBP")) {
				result = ImageFormat.WEBP;
			}
		}
		return result;
	}

	/**
	 * @return whether this format is one of the proprietary camera
	 *         image formats.
	 */
	public boolean isRaw() {
		return switch (this) {
			case ARW, CR2, CRW, DCR, DNG, RAW, KDC, NEF, ORF, PEF, RAF, RW2, SRW -> true;
			default -> false;
		};
	}

	/**
	 * If new plugins for {@link ImageIO} is added, this {@code switch} must
	 * be updated to reflect that.
	 *
	 * @return Whether or not the format is supported by {@link ImageIO}.
	 */
	public boolean supportedByImageIO() {
		return switch (this) {
			case BMP, CUR, DCX, GIF, ICNS, ICO, IFF, JPEG, PCX, PICT, PNG, PNM, PSD, RGBE, SGI, TGA, TIFF, WBMP -> true;
			default -> false;
		};
	}

	/**
	 * @return The {@link FormatConfiguration} value for this image format.
	 */
	public String toFormatConfiguration() {
		switch (this) {
			case ARW, CR2, CRW, DCR, DNG, RAW, KDC, NEF, ORF, PEF, RAF, RW2, SRW -> {
				return FormatConfiguration.RAW;
			}
			case BMP -> {
				return FormatConfiguration.BMP;
			}
			case CUR -> {
				return FormatConfiguration.CUR;
			}
			case DCX -> {
				return FormatConfiguration.PCX;
			}
			case GIF -> {
				return FormatConfiguration.GIF;
			}
			case ICNS -> {
				return FormatConfiguration.ICNS;
			}
			case ICO -> {
				return FormatConfiguration.ICO;
			}
			case JPEG -> {
				return FormatConfiguration.JPG;
			}
			case PCX -> {
				return FormatConfiguration.PCX;
			}
			case PNG -> {
				return FormatConfiguration.PNG;
			}
			case PNM -> {
				return FormatConfiguration.PNM;
			}
			case PSD -> {
				return FormatConfiguration.PSD;
			}
			case TIFF -> {
				return FormatConfiguration.TIFF;
			}
			case WBMP -> {
				return FormatConfiguration.WBMP;
			}
			case WEBP -> {
				return FormatConfiguration.WEBP;
			}
			case SOURCE -> throw new IllegalArgumentException("SOURCE cannot be translated into an actual format");
			default -> {
				return toString().toLowerCase(Locale.ROOT);
			}
		}
	}
}
