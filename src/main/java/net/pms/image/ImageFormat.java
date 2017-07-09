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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import net.pms.dlna.protocolinfo.MimeType;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageFormat.class);
	public static final int TAG_DNG_VERSION = 0xC612;

	/**
	 * Converts from {@link DLNAImageProfile} to {@link ImageFormat}.
	 *
	 * @param imageProfile the {@link DLNAImageProfile} to convert.
	 * @return The corresponding {@link ImageFormat}.
	 */
	public static ImageFormat toImageFormat(DLNAImageProfile imageProfile) {
		if (imageProfile == null) {
			return null;
		}
		return imageProfile.getFormat();
	}

	/**
	 * Converts from {@link FileType} to {@link ImageFormat}.
	 *
	 * @param fileType the {@link FileType} to convert.
	 * @return The corresponding {@link ImageFormat}.
	 */
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
	 * Returns an {@link ImageFormat} based on a predefined list of
	 * {@link MimeType}s.
	 *
	 * @param mimeType the {@link MimeType} to resolve.
	 * @return The resolved {@link ImageFormat} or {@code null} if the
	 *         {@link MimeType} isn't among the predefined.
	 */
	public static ImageFormat toImageFormat(MimeType mimeType) {
		if (mimeType == null || isBlank(mimeType.getType())) {
			return null;
		}

		String type = mimeType.getType().toLowerCase(Locale.ROOT);
		if ("image".equals(type) && isNotBlank(mimeType.getSubtype())) {
			// image/
			switch (mimeType.getSubtype().toLowerCase(Locale.ROOT)) {
				case "arw":
				case "x-sony-arw":
					return ARW;
				case "bmp":
				case "x-bmp":
				case "x-bitmap":
				case "x-xbitmap":
				case "x-win-bitmap":
				case "x-windows-bmp":
				case "image/ms-bmp":
				case "x-ms-bmp":
					return BMP;
				case "cr2":
					return CR2;
				case "crw":
				case "x-canon-crw":
					return CRW;
				case "x-kodak-dcr":
					return DCR;
				case "dcx":
				case "x-dcx":
					return DCX;
				case "dng":
				case "x-adobe-dng":
					return DNG;
				case "gif":
				case "gi_":
					return GIF;
				case "icns":
					return ICNS;
				case "x-icon":
				case "vnd.microsoft.icon":
				case "ico":
				case "icon":
					return ICO;
				case "iff":
				case "x-iff":
					return IFF;
				case "jpeg":
				case "jpg":
				case "jpe_":
				case "pjpeg":
				case "vnd.swiftview-jpeg":
					return JPEG;
				case "x-kodak-kdc":
					return KDC;
				case "x-nikon-nef":
				case "nef":
					return NEF;
				case "x-olympus-orf":
					return ORF;
				case "pcx":
				case "x-pcx":
				case "x-pc-paintbrush":
					return PCX;
				case "pict":
				case "x-pict":
					return PICT;
				case "x-pentax-pef":
				case "/x-pentax-raw":
					return PEF;
				case "png":
				case "x-png":
					return PNG;
				case "x-portable-anymap":
				case "x‑portable‑bitmap":
				case "x‑portable‑graymap,":
				case "x‑portable‑pixmap":
				case "pbm":
				case "pgm":
				case "ppm":
				case "pnm":
					return PNM;
				case "vnd.adobe.photoshop":
				case "photoshop":
				case "x-photoshop":
				case "psd":
				case "x-psd":
					return PSD;
				case "x-fuji-raf":
				case "x-fujifilm-raf":
					return RAF;
				case "vnd.radiance":
					return RGBE;
				case "x-panasonic-raw":
				case "x-panasonic-rw2":
					return RW2;
				case "sgi":
				case "x-sgi":
				case "x-sgi-rgba":
					return SGI;
				case "tga":
				case "x-tga":
				case "x-targa":
					return TGA;
				case "tiff":
				case "x-tiff":
				case "tiff-fx":
					return TIFF;
				case "vnd.wap.wbmp":
					return WBMP;
				case "webp":
				case "x-webp":
					return WEBP;
				default:
					LOGGER.debug("Unable to resolve an image format from \"{}\"", mimeType);
			}
		} else if ("application".equals(type) && isNotBlank(mimeType.getSubtype())) {
			// application/
			switch (mimeType.getSubtype().toLowerCase(Locale.ROOT)) {
				case "bmp":
				case "x-bmp":
				case "x-win-bitmap":
					return BMP;
				case "ico":
				case "x-ico":
				case "octet-stream":
					return ICO;
				case "iff":
				case "x-iff":
					return IFF;
				case "pcx":
				case "x-pcx":
					return PCX;
				case "png":
				case "x-png":
					return PNG;
				case "photoshop":
				case "psd":
					return PSD;
				case "tga":
				case "x-tga":
					return TGA;
				default:
					LOGGER.debug("Unable to resolve an image format from \"{}\"", mimeType);
			}
		}
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
	 * @return The default file extension for this {@link ImageFormat}.
	 */
	public String getDefaultExtension() {
		// Only override those where the identifier and "standard extension" differ.
		switch (this) {
			case JPEG:
				return "jpg";
			case RGBE:
				return "hdr";
			case SOURCE:
				return "";
			default:
				return "" + super.toString().toLowerCase(Locale.ROOT);
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

	/**
	 * Returns the {@link ImageFormat} instance with identifier {@code enumName}
	 * (case sensitive). Performs basically the same as
	 * {@link ImageFormat#valueOf(String)}, but returns {@code null} instead of
	 * throwing exceptions if no match is found.
	 *
	 * @param enumName the identifier for the {@link ImageFormat} instance.
	 * @return The {@link ImageFormat} instance or {@code null}.
	 */
	public static ImageFormat typeOf(String enumName) {
		if (isBlank(enumName)) {
			return null;
		}
		for (ImageFormat format : ImageFormat.values()) {
			if (enumName.equals(format.toString())) {
				return format;
			}
		}
		return null;
	}
}
