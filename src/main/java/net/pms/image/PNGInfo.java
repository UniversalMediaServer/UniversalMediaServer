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

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import net.pms.util.ParseException;
import com.drew.imaging.png.PngChunkType;
import com.drew.imaging.png.PngColorType;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.png.PngDirectory;

public class PNGInfo extends ImageInfo {
	private static final long serialVersionUID = 1L;
	protected final int colorType;
	protected final InterlaceMethod interlaceMethod;
	protected final boolean hasTransparencyChunk;
	protected final boolean isModifiedBitDepth;

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, ColorModel, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected PNGInfo(
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

		colorType = ((PNGParseInfo) parsedInfo).colorType.getNumericValue();
		interlaceMethod = ((PNGParseInfo) parsedInfo).interlaceMethod;
		hasTransparencyChunk = ((PNGParseInfo) parsedInfo).hasTransparencyChunk;
		isModifiedBitDepth = ((PNGParseInfo) parsedInfo).isModifiedBitDepth;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, int, int, ColorSpace, ColorSpaceType, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected PNGInfo(
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

		colorType = ((PNGParseInfo) parsedInfo).colorType.getNumericValue();
		interlaceMethod = ((PNGParseInfo) parsedInfo).interlaceMethod;
		hasTransparencyChunk = ((PNGParseInfo) parsedInfo).hasTransparencyChunk;
		isModifiedBitDepth = ((PNGParseInfo) parsedInfo).isModifiedBitDepth;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, Metadata, ImageFormat, long, boolean, boolean)}
	 * to instantiate.
	 */
	protected PNGInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		super(width, height, metadata, format, size, applyExifOrientation, throwOnParseFailure);

		colorType = ((PNGParseInfo) parsedInfo).colorType.getNumericValue();
		interlaceMethod = ((PNGParseInfo) parsedInfo).interlaceMethod;
		hasTransparencyChunk = ((PNGParseInfo) parsedInfo).hasTransparencyChunk;
		isModifiedBitDepth = ((PNGParseInfo) parsedInfo).isModifiedBitDepth;
	}

	/**
	 * Copy constructor
	 */
	protected PNGInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		boolean imageIOSupport,
		PngColorType colorType,
		InterlaceMethod interlaceMethod,
		boolean hasTransparencyChunk,
		boolean isModifiedBitDepth
	) {
		super(width, height, format, size, bitDepth, numComponents, colorSpace, colorSpaceType, imageIOSupport);
		this.colorType = colorType.getNumericValue();
		this.interlaceMethod = interlaceMethod;
		this.hasTransparencyChunk = hasTransparencyChunk;
		this.isModifiedBitDepth = isModifiedBitDepth;
	}

	/**
	 * @return The {@link PngColorType} or {@code null} if unknown.
	 */
	public PngColorType getColorType() {
		return PngColorType.fromNumericValue(colorType);
	}

	/**
	 * @return Whether or not the PNG has a tRNS chunk with transparency information.
	 */
	public boolean isHasTransparencyChunk() {
		return hasTransparencyChunk;
	}

	/**
	 * @return Whether or not the PNG has a sBIT chunk altering the bit depth.
	 */
	public boolean isModifiedBitDepth() {
		return isModifiedBitDepth;
	}

	/**
	 * @return The {@link InterlaceMethod} or {@code null} if unknown.
	 */
	public InterlaceMethod getInterlaceMethod() {
		return interlaceMethod;
	}

	@Override
	protected ParseInfo createParseInfo() {
		return new PNGParseInfo();
	}

	@Override
	protected void parseMetadata(Metadata metadata) throws ParseException {
		if (metadata == null) {
			return;
		}

		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof PngDirectory && PngChunkType.IHDR.equals(((PngDirectory) directory).getPngChunkType())) {
				parsedInfo.format = ImageFormat.PNG;
				if (
					((PngDirectory) directory).containsTag(PngDirectory.TAG_IMAGE_WIDTH) &&
					((PngDirectory) directory).containsTag(PngDirectory.TAG_IMAGE_HEIGHT)
				) {
					parsedInfo.width = ((PngDirectory) directory).getInteger(PngDirectory.TAG_IMAGE_WIDTH);
					parsedInfo.height = ((PngDirectory) directory).getInteger(PngDirectory.TAG_IMAGE_HEIGHT);
				}
				if (((PngDirectory) directory).containsTag(PngDirectory.TAG_BITS_PER_SAMPLE)) {
					parsedInfo.bitDepth = ((PngDirectory) directory).getInteger(PngDirectory.TAG_BITS_PER_SAMPLE);
				}
				if (((PngDirectory) directory).containsTag(PngDirectory.TAG_INTERLACE_METHOD)) {
					Integer i = ((PngDirectory) directory).getInteger(PngDirectory.TAG_INTERLACE_METHOD);
					if (i != null) {
						((PNGParseInfo) parsedInfo).interlaceMethod = InterlaceMethod.typeOf(i);
					}
				}
				if (((PngDirectory) directory).containsTag(PngDirectory.TAG_COLOR_TYPE)) {
					Integer i = ((PngDirectory) directory).getInteger(PngDirectory.TAG_COLOR_TYPE);
					if (i != null) {
						((PNGParseInfo) parsedInfo).colorType = PngColorType.fromNumericValue(i);
						switch (((PNGParseInfo) parsedInfo).colorType.getNumericValue()) {
							// Grayscale without alpha
							case 0 -> {
								parsedInfo.numComponents = 1;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_GRAY;
							}
							// RGB without alpha
							// Palette index
							case 2, 3 -> {
								parsedInfo.numComponents = 3;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
							}
							// Grayscale with alpha
							case 4 -> {
								parsedInfo.numComponents = 2;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_GRAY;
							}
							// RGB with alpha
							case 6 -> {
								parsedInfo.numComponents = 4;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
							}
							default -> {
								//nothing to do
							}
						}
					}
				}
			}
			if (directory instanceof PngDirectory && PngChunkType.tRNS.equals(((PngDirectory) directory).getPngChunkType())) {
				((PNGParseInfo) parsedInfo).hasTransparencyChunk = true;
				if (((PNGParseInfo) parsedInfo).colorType == null || ((PNGParseInfo) parsedInfo).numComponents == null) {
					throw new ParseException("PNG parsing failed with ancillary chunk tRNS appearing before critical chunk IHDR");
				}
				if (
					((PNGParseInfo) parsedInfo).colorType == PngColorType.GREYSCALE_WITH_ALPHA ||
					((PNGParseInfo) parsedInfo).colorType ==  PngColorType.TRUE_COLOR_WITH_ALPHA
				) {
					throw new ParseException(String.format(
						"PNG parsing failed with illegal combination of %s color type and tRNS transparency chunk",
						((PNGParseInfo) parsedInfo).colorType
					));
				}
				parsedInfo.numComponents++;
			}
			if (directory instanceof PngDirectory && PngChunkType.sBIT.equals(((PngDirectory) directory).getPngChunkType())) {
				((PNGParseInfo) parsedInfo).isModifiedBitDepth = true;
			}
		}
	}

	@Override
	public PNGInfo copy() {
		return new PNGInfo(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			imageIOSupport,
			PngColorType.fromNumericValue(colorType),
			interlaceMethod,
			hasTransparencyChunk,
			isModifiedBitDepth
		);
	}

	public enum InterlaceMethod {
		NONE, ADAM7, UNKNOWN;

		public static InterlaceMethod typeOf(int value) {
			return switch (value) {
				case 0 -> NONE;
				case 1 -> ADAM7;
				default -> UNKNOWN;
			};
		}
	}

	protected static class PNGParseInfo extends ParseInfo {
		PngColorType colorType;
		InterlaceMethod interlaceMethod;
		boolean hasTransparencyChunk = false;
		boolean isModifiedBitDepth = false;
	}

	@Override
	protected void buildToString(StringBuilder sb) {
		if (colorType >= 0) {
			sb.append(", Color Type = ").append(colorType);
		}
		if (interlaceMethod != null) {
			sb.append(", Interlace Method = ").append(interlaceMethod);
		}
		sb.append(", Has Transparency Chunk = ").append(hasTransparencyChunk ? "True" : "False")
			.append("Has Modified Bit Depth = ").append(isModifiedBitDepth ? "True" : "False");
	}
}
