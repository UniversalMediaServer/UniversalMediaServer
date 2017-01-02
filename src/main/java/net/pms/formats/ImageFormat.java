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
package net.pms.formats;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import com.drew.imaging.FileType;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAImageProfile;
import net.pms.util.CustomImageReader;


/**
 * Defines the different image format supported by the ImageIO parser
 * with the currently installed plugins plus the special value {@code SOURCE}.
 * If more plugins are added, more entries should be added here and in
 * {@link CustomImageReader#read(ImageInputStream)}.
 */
public enum ImageFormat {
	ARW, BMP, CR2, CRW, CUR, DCX, GIF, ICNS, ICO, JPEG, NEF, ORF, PCX, PNG, PNM, PSD, RAF, RW2, SOURCE, TIFF, WBMP, WEBP;

	public static ImageFormat toImageFormat(DLNAImageProfile imageProfile) {
		if (imageProfile == null) {
			return null;
		}
		switch (imageProfile.toInt()) {
			case DLNAImageProfile.GIF_LRG_INT:
				return ImageFormat.GIF;
			case DLNAImageProfile.JPEG_LRG_INT:
			case DLNAImageProfile.JPEG_MED_INT:
			case DLNAImageProfile.JPEG_RES_H_V_INT:
			case DLNAImageProfile.JPEG_SM_INT:
			case DLNAImageProfile.JPEG_TN_INT:
				return ImageFormat.JPEG;
			case DLNAImageProfile.PNG_LRG_INT:
			case DLNAImageProfile.PNG_TN_INT:
				return ImageFormat.PNG;
			default:
				return null;
		}
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
	 * @return whether this format is one of the proprietary camera
	 *         image formats.
	 */
	public boolean isRaw() {
		switch (this) {
			case ARW:
			case CR2:
			case CRW:
			case NEF:
			case ORF:
			case RAF:
			case RW2:
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
			case JPEG:
			case PCX:
			case PNG:
			case PNM:
			case PSD:
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
				return FormatConfiguration.RAW;
			case BMP:
				return FormatConfiguration.BMP;
			case CR2:
				return FormatConfiguration.RAW;
			case CRW:
				return FormatConfiguration.RAW;
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
			case NEF:
				return FormatConfiguration.RAW;
			case ORF:
				return FormatConfiguration.RAW;
			case PCX:
				return FormatConfiguration.PCX;
			case PNG:
				return FormatConfiguration.PNG;
			case PNM:
				return FormatConfiguration.PNM;
			case PSD:
				return FormatConfiguration.PSD;
			case RAF:
				return FormatConfiguration.RAW;
			case RW2:
				return FormatConfiguration.RAW;
			case TIFF:
				return FormatConfiguration.TIFF;
			case WBMP:
				return FormatConfiguration.WBMP;
			case WEBP:
				return FormatConfiguration.WEBP;
			case SOURCE:
				throw new IllegalArgumentException("SOURCE cannot be translated into an actual format");
			default:
				return toString();
		}
	}
}
