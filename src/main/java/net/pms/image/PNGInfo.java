package net.pms.image;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import net.pms.util.ParseException;
import com.drew.imaging.png.PngChunkType;
import com.drew.imaging.png.PngColorType;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.png.PngDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class PNGInfo extends ImageInfo {

	protected final PngColorType colorType;
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

		colorType = ((PNGParseInfo) parsedInfo).colorType;
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

		colorType = ((PNGParseInfo) parsedInfo).colorType;
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

		colorType = ((PNGParseInfo) parsedInfo).colorType;
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
		this.colorType = colorType;
		this.interlaceMethod = interlaceMethod;
		this.hasTransparencyChunk = hasTransparencyChunk;
		this.isModifiedBitDepth = isModifiedBitDepth;
	}

	/**
	 * @return The {@link PngColorType} or {@code null} if unknown.
	 */
	public PngColorType getColorType() {
		return colorType;
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
						switch (((PNGParseInfo) parsedInfo).colorType) {
							case Greyscale: // Grayscale without alpha
								parsedInfo.numComponents = 1;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case TrueColor: // RGB without alpha
								parsedInfo.numComponents = 3;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case IndexedColor: // Palette index
								parsedInfo.numComponents = 3;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case GreyscaleWithAlpha: // Grayscale with alpha
								parsedInfo.numComponents = 2;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case TrueColorWithAlpha: // RGB with alpha
								parsedInfo.numComponents = 4;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							default:
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
					((PNGParseInfo) parsedInfo).colorType == PngColorType.GreyscaleWithAlpha ||
					((PNGParseInfo) parsedInfo).colorType ==  PngColorType.TrueColorWithAlpha
				) {
					throw new ParseException(String.format(
						"PNG parsing failed with illegal combination of %s color type and tRNS transparancy chunk",
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
			colorType,
			interlaceMethod,
			hasTransparencyChunk,
			isModifiedBitDepth
		);
	}

	public enum InterlaceMethod {
		NONE, ADAM7, UNKNOWN;

		public static InterlaceMethod typeOf(int value) {
			switch (value) {
				case 0:
					return NONE;
				case 1:
					return ADAM7;
				default:
					return UNKNOWN;

			}
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
		if (colorType != null) {
			sb.append(", Color Type = ").append(colorType);
		}
		if (interlaceMethod != null) {
			sb.append(", Interlace Method = ").append(interlaceMethod);
		}
		sb.append(", Has Transparency Chunk = ").append(hasTransparencyChunk ? "True" : "False")
			.append("Has Modified Bit Depth = ").append(isModifiedBitDepth ? "True" : "False");
	}
}
