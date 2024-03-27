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

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.gif.GifControlDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import net.pms.util.ParseException;

@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class GIFInfo extends ImageInfo {
	protected final String formatVersion;
	protected final boolean hasTransparency;

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, ColorModel, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected GIFInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		ColorModel colorModel,
		Metadata metadata,
		boolean applyExifOrientation,
		boolean imageIOSupport
	) throws ParseException {
		super(width, height, format, size, colorModel, metadata, applyExifOrientation, imageIOSupport);

		formatVersion = ((GIFParseInfo) parsedInfo).formatVersion;
		hasTransparency = ((GIFParseInfo) parsedInfo).hasTransparency != null &&
			((GIFParseInfo) parsedInfo).hasTransparency;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, int, int, ColorSpace, ColorSpaceType, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected GIFInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		Metadata metadata,
		boolean applyExifOrientation,
		boolean imageIOSupport
	) throws ParseException {
		super(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			metadata,
			applyExifOrientation,
			imageIOSupport
		);

		formatVersion = ((GIFParseInfo) parsedInfo).formatVersion;
		hasTransparency = ((GIFParseInfo) parsedInfo).hasTransparency != null &&
			((GIFParseInfo) parsedInfo).hasTransparency;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, Metadata, ImageFormat, long, boolean, boolean)}
	 * to instantiate.
	 */
	protected GIFInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		super(width, height, metadata, format, size, applyExifOrientation, throwOnParseFailure);

		formatVersion = ((GIFParseInfo) parsedInfo).formatVersion;
		hasTransparency = ((GIFParseInfo) parsedInfo).hasTransparency != null &&
			((GIFParseInfo) parsedInfo).hasTransparency;
	}

	/**
	 * Copy constructor
	 */
	protected GIFInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		boolean imageIOSupport,
		String formatVersion,
		boolean hasTransparency
	) {
		super(width, height, format, size, bitDepth, numComponents, colorSpace, colorSpaceType, imageIOSupport);
		this.formatVersion = formatVersion;
		this.hasTransparency = hasTransparency;
	}

	@Override
	protected ParseInfo createParseInfo() {
		return new GIFParseInfo();
	}

	@Override
	protected void parseMetadata(Metadata metadata) throws ParseException {
		if (metadata == null) {
			return;
		}

		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof GifHeaderDirectory gifHeaderDirectory) {
				parsedInfo.format = ImageFormat.GIF;
				parsedInfo.numComponents = 3;
				if (
					gifHeaderDirectory.containsTag(GifHeaderDirectory.TAG_IMAGE_WIDTH) &&
					gifHeaderDirectory.containsTag(GifHeaderDirectory.TAG_IMAGE_HEIGHT)
				) {
					parsedInfo.width = gifHeaderDirectory.getInteger(GifHeaderDirectory.TAG_IMAGE_WIDTH);
					parsedInfo.height = gifHeaderDirectory.getInteger(GifHeaderDirectory.TAG_IMAGE_HEIGHT);
				}
				if (gifHeaderDirectory.containsTag(GifHeaderDirectory.TAG_BITS_PER_PIXEL)) {
					parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
					Integer i = gifHeaderDirectory.getInteger(GifHeaderDirectory.TAG_BITS_PER_PIXEL);
					if (i != null) {
						parsedInfo.bitDepth = i;
					}
				}
				if (gifHeaderDirectory.containsTag(GifHeaderDirectory.TAG_GIF_FORMAT_VERSION)) {
					String s = gifHeaderDirectory.getString(GifHeaderDirectory.TAG_GIF_FORMAT_VERSION, "US-ASCII");
					if (s != null) {
						((GIFParseInfo) parsedInfo).formatVersion = s;
					}
				}
			} else if (directory instanceof GifControlDirectory gifControlDirectory) {
				boolean isTransparent = gifControlDirectory.isTransparent();
				((GIFParseInfo) parsedInfo).hasTransparency = isTransparent;
				if (isTransparent) {
					if (parsedInfo.numComponents == null) {
						throw new ParseException(
							"Invalid GIF image - Graphic Control Extension block encountered before the Header block"
						);
					}
					parsedInfo.numComponents = 4;
				}
			}
		}
	}

	@Override
	public GIFInfo copy() {
		return new GIFInfo(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			imageIOSupport,
			formatVersion,
			hasTransparency
		);
	}

	/**
	 * @return The format version {@link String} from the start of the GIF
	 *         header.
	 */
	public String getFormatVersion() {
		return formatVersion;
	}

	/**
	 * @return Whether the GIF has a "background" color/transparency.
	 */
	public boolean isHasTransparency() {
		return hasTransparency;
	}

	protected static class GIFParseInfo extends ParseInfo {
		String formatVersion;
		Boolean hasTransparency;
	}

	@Override
	protected void buildToString(StringBuilder sb) {
		if (formatVersion != null) {
			sb.append(", GIF Version = ").append(formatVersion);
		}
		sb.append(", Has Transparency = ").append(hasTransparency ? "True" : "False");
	}
}
