/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.image;

import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import com.drew.imaging.FileType;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.ico.IcoDirectory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.pcx.PcxDirectory;
import com.drew.metadata.photoshop.PsdHeaderDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAImageProfile;
import net.pms.image.ExifInfo.ExifCompression;


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

	public static ImageFormat toImageFormat(DLNAImageProfile imageProfile) {
		if (imageProfile == null) {
			return null;
		}
		return imageProfile.getFormat();
	}

	public static ImageFormat toImageFormat(FileType fileType) {
		if (fileType == null) {
			return null;
		}
		switch (fileType) {
			case Arw:
				return ImageFormat.ARW;
			case Bmp:
				return ImageFormat.BMP;
			case Cr2:
				return ImageFormat.CR2;
			case Crw:
				return ImageFormat.CRW;
			case Gif:
				return ImageFormat.GIF;
			case Ico:
				return ImageFormat.ICO;
			case Jpeg:
				return ImageFormat.JPEG;
			case Nef:
				return ImageFormat.NEF;
			case Orf:
				return ImageFormat.ORF;
			case Pcx:
				return ImageFormat.PCX;
			case Png:
				return ImageFormat.PNG;
			case Psd:
				return ImageFormat.PSD;
			case Raf:
				return ImageFormat.RAF;
			case Rw2:
				return ImageFormat.RW2;
			case Tiff:
				return ImageFormat.TIFF;
			case Riff:
				return ImageFormat.WEBP;
			case Unknown:
			default:
				return null;
		}
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
        	} else if (
        		formatName.contains("PIC") ||
        		formatName.contains("PCT")
        	) {
        		result = ImageFormat.PICT;
        	} else if (formatName.contains("PNG")) {
        		result = ImageFormat.PNG;
        	} else if (
        		formatName.contains("PNM") ||
        		formatName.contains("PBM") ||
        		formatName.contains("PGM") ||
        		formatName.contains("PPM") ||
        		formatName.contains("PAM") ||
        		formatName.contains("PFM")
        	) {
        		result = ImageFormat.PNM;
        	} else if (formatName.contains("PSD")) {
        		result = ImageFormat.PSD;
        	} else if (
        		formatName.contains("RGBE") ||
        		formatName.contains("HDR") ||
        		formatName.contains("XYZE")
        	) {
        		result = ImageFormat.RGBE;
        	} else if (
        		formatName.contains("SGI") ||
        		formatName.equals("RLE")
        	) {
        		result = ImageFormat.SGI;
        	} else if (
        		formatName.contains("TGA") ||
        		formatName.contains("TARGA")
        	) {
        		result = ImageFormat.TGA;
        	} else if (formatName.contains("TIFF")) {
        		result = ImageFormat.TIFF;
        	} else if (formatName.contains("WBMP")) {
        		result = ImageFormat.WBMP;
        	}
        }
        return result;
    }

	/**
	 * Tries to parse {@link ImageFormat} from a {@link Metadata} instance.
	 *
	 * @param metadata the {@link Metadata} to parse.
	 * @return The parsed {@link ImageFormat} or {@code null} if the parsing
	 *         fails.
	 */
	@SuppressWarnings("incomplete-switch")
	public static ImageFormat toImageFormat(Metadata metadata) {
		if (metadata == null) {
			throw new NullPointerException("metadata cannot be null");
		}

		// Check for known directories tied to a particular format
		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof BmpHeaderDirectory) {
				return ImageFormat.BMP;
			}
			if (directory instanceof GifHeaderDirectory) {
				return ImageFormat.GIF;
			}
			if (directory instanceof IcoDirectory) {
				return ImageFormat.ICO;
			}
			if (directory instanceof JfifDirectory || directory instanceof JpegDirectory) {
				return ImageFormat.JPEG;
			}
			if (directory instanceof PcxDirectory) {
				return ImageFormat.PCX;
			}
			if (directory instanceof PngDirectory) {
				return ImageFormat.PNG;
			}
			if (directory instanceof PsdHeaderDirectory) {
				return ImageFormat.PSD;
			}
			if (directory instanceof WebpDirectory) {
				return ImageFormat.WEBP;
			}
		}

		/*
		 * Check for Exif compression tag and parse known values to TIFF or
		 * various proprietary "raw" formats. Do not try to combine the two
		 * loops, Exif matching must only be attempted if the first match fails.
		 */
		ExifCompression exifCompression = null;
		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof ExifIFD0Directory || directory instanceof ExifSubIFDDirectory) {
				if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_COMPRESSION)) {
					Integer i = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_COMPRESSION);
					if (i != null) {
						exifCompression = ExifCompression.typeOf(i.intValue());
					}
				}
				if (directory.containsTag(TAG_DNG_VERSION)) {
					return ImageFormat.DNG;
				}
			}
		}
		// Nothing found by specific tags, use a generic approach based on Exif compression
		if (exifCompression != null) {
			switch (exifCompression) {
				case ADOBE_DEFLATE:
				case CCITT_1D:
				case DEFLATE:
				case IT8BL:
				case IT8CTPAD:
				case IT8LW:
				case IT8MP:
				case JBIG:
				case JBIG2_TIFF_FX:
				case JBIG_B_W:
				case JBIG_COLOR:
				case JPEG:
				case JPEG_OLD_STYLE:
				case LZW:
				case PACKBITS:
				case T4_GROUP_3_FAX:
				case T6_GROUP_4_FAX:
				case UNCOMPRESSED:
					return ImageFormat.TIFF;
				case DCS:
				case KODAC_DCR_COMPRESSED:
					return ImageFormat.DCR;
				case KODAK_KDC_COMPRESSED:
					return ImageFormat.KDC;
				case NIKON_NEF_COMPRESSED:
					return ImageFormat.NEF;
				case PENTAX_PEF_COMPRESSED:
					return ImageFormat.PEF;
				case SAMSUNG_SRW_COMPRESSED:
				case SAMSUNG_SRW_COMPRESSED_2:
					return ImageFormat.SRW;
				case SONY_ARW_COMPRESSED:
					return ImageFormat.ARW;
			}
		}

		// Parsing failed
		return null;
	}

	/**
	 * @return whether this format is one of the proprietary camera
	 *         image formats.
	 */
	public boolean isRaw() {
		switch (this) {
			case ARW:
			case CR2:
			case CRW:
			case DCR:
			case DNG:
			case RAW:
			case KDC:
			case NEF:
			case ORF:
			case PEF:
			case RAF:
			case RW2:
			case SRW:
				return true;
			default:
				return false;
		}
	}

	/**
	 * If new plugins for {@link ImageIO} is added, this {@code switch} must
	 * be updated to reflect that.
	 *
	 * @return Whether or not the format is supported by {@link ImageIO}.
	 */
	public boolean supportedByImageIO() {
		switch (this) {
			case BMP:
			case CUR:
			case DCX:
			case GIF:
			case ICNS:
			case ICO:
			case IFF:
			case JPEG:
			case PCX:
			case PICT:
			case PNG:
			case PNM:
			case PSD:
			case RGBE:
			case SGI:
			case TGA:
			case TIFF:
			case WBMP:
				return true;
			default:
				return false;
		}
	}

	/**
	 * @return The {@link FormatConfiguration} value for this image format.
	 */
	public String toFormatConfiguration() {
		switch (this) {
			case ARW:
			case CR2:
			case CRW:
			case DCR:
			case DNG:
			case RAW:
			case KDC:
			case NEF:
			case ORF:
			case PEF:
			case RAF:
			case RW2:
			case SRW:
				return FormatConfiguration.RAW;
			case BMP:
				return FormatConfiguration.BMP;
			case CUR:
				return FormatConfiguration.CUR;
			case DCX:
				return FormatConfiguration.PCX;
			case GIF:
				return FormatConfiguration.GIF;
			case ICNS:
				return FormatConfiguration.ICNS;
			case ICO:
				return FormatConfiguration.ICO;
			case JPEG:
				return FormatConfiguration.JPG;
			case PCX:
				return FormatConfiguration.PCX;
			case PNG:
				return FormatConfiguration.PNG;
			case PNM:
				return FormatConfiguration.PNM;
			case PSD:
				return FormatConfiguration.PSD;
			case TIFF:
				return FormatConfiguration.TIFF;
			case WBMP:
				return FormatConfiguration.WBMP;
			case WEBP:
				return FormatConfiguration.WEBP;
			case SOURCE:
				throw new IllegalArgumentException("SOURCE cannot be translated into an actual format");
			default:
				return toString().toLowerCase(Locale.ROOT);
		}
	}
}
