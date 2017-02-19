package net.pms.image;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import net.pms.util.ParseException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class BMPInfo extends ImageInfo {

	protected final CompressionType compressionType;

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, ColorModel, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected BMPInfo(
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

		compressionType = ((BMPParseInfo) parsedInfo).compressionType;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, int, int, ColorSpace, ColorSpaceType, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected BMPInfo(
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

		compressionType = ((BMPParseInfo) parsedInfo).compressionType;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, Metadata, ImageFormat, long, boolean, boolean)}
	 * to instantiate.
	 */
	protected BMPInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		super(width, height, metadata, format, size, applyExifOrientation, throwOnParseFailure);

		compressionType = ((BMPParseInfo) parsedInfo).compressionType;
	}

	/**
	 * Copy constructor
	 */
	protected BMPInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		boolean imageIOSupport,
		CompressionType compressionType
	) {
		super(width, height, format, size, bitDepth, numComponents, colorSpace, colorSpaceType, imageIOSupport);

		this.compressionType = compressionType;
	}

	@Override
	protected ParseInfo createParseInfo() {
		return new BMPParseInfo();
	}

	/**
	 * @return The {@link CompressionType}.
	 */
	public CompressionType getCompressionType() {
		return compressionType;
	}

	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	@Override
	protected void parseMetadata(Metadata metadata) {
		if (metadata == null) {
			return;
		}

		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof BmpHeaderDirectory) {
				parsedInfo.format = ImageFormat.BMP;
				if (
					((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_IMAGE_WIDTH) &&
					((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_IMAGE_HEIGHT)
				) {
					parsedInfo.width = ((BmpHeaderDirectory) directory).getInteger(BmpHeaderDirectory.TAG_IMAGE_WIDTH);
					parsedInfo.height = ((BmpHeaderDirectory) directory).getInteger(BmpHeaderDirectory.TAG_IMAGE_HEIGHT);
				}
			}
			if (
				((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_BITS_PER_PIXEL) &&
				((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_COMPRESSION) &&
				((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_HEADER_SIZE)
			) {
				Integer compression = ((BmpHeaderDirectory) directory).getInteger(BmpHeaderDirectory.TAG_COMPRESSION);
				Integer headerSize = ((BmpHeaderDirectory) directory).getInteger(BmpHeaderDirectory.TAG_HEADER_SIZE);
				Integer bitsPerPixel = ((BmpHeaderDirectory) directory).getInteger(BmpHeaderDirectory.TAG_BITS_PER_PIXEL);

				if (compression != null && headerSize != null && bitsPerPixel != null) {
					CompressionType compressionType =
						CompressionType.typeOf(compression.intValue(), headerSize.intValue());
					((BMPParseInfo) parsedInfo).compressionType = compressionType;
					if (parsedInfo.bitDepth != null) {
						switch (compressionType) {
							case BIT_FIELDS:
							case HUFFMAN_1D:
							case NONE:
							case PNG:
							case RLE24:
							case RLE4:
							case RLE8:
								// XXX Further parsing of the different
								// BITMAPINFOHEADER versions is needed for
								// accurate detection - the below is
								// "qualified guessing"
								switch (bitsPerPixel.intValue()) {
									case 1:
										parsedInfo.numComponents = 1;
										parsedInfo.colorSpaceType = ColorSpaceType.TYPE_GRAY;
										parsedInfo.bitDepth = 1;
										break;
									case 2:
									case 4:
									case 8:
										parsedInfo.numComponents = 3;
										parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
										parsedInfo.bitDepth = 8;
										break;
									case 16:
										// assuming 5:6:5 - could also be 5:5:5:1
										parsedInfo.numComponents = 3;
										parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
										parsedInfo.bitDepth = 5;
										break;
									case 24:
										parsedInfo.numComponents = 3;
										parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
										parsedInfo.bitDepth = 8;
										break;
									case 32:
										parsedInfo.numComponents = 4;
										parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
										parsedInfo.bitDepth = 8;
										break;
									default:
								}
								break;
							case JPEG:
								parsedInfo.numComponents = 3;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
								parsedInfo.bitDepth = bitsPerPixel.intValue() / 3;
								break;
							case RGBA_BIT_FIELDS:
								parsedInfo.numComponents = 4;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
								parsedInfo.bitDepth = bitsPerPixel.intValue() / 4;
								break;
							case CMYK_NONE:
							case CMYK_RLE4:
							case CMYK_RLE8:
								parsedInfo.numComponents = 4;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_CMYK;
								parsedInfo.bitDepth = bitsPerPixel.intValue() / 4;
								break;
							case UNKNOWN:
							default:
						}
					}
				}
			}
		}
	}

	@Override
	public BMPInfo copy() {
		return new BMPInfo(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			imageIOSupport,
			compressionType
		);
	}

	public enum CompressionType {
		NONE,
		RLE8,
		RLE4,
		BIT_FIELDS,
		HUFFMAN_1D,
		JPEG,
		RLE24,
		PNG,
		RGBA_BIT_FIELDS,
		CMYK_NONE,
		CMYK_RLE8,
		CMYK_RLE4,
		UNKNOWN;


		@Override
		public String toString() {
			switch (this) {
				case BIT_FIELDS:
					return "RGB bit field masks";
				case CMYK_NONE:
					return "CMYK no compression";
				case CMYK_RLE4:
					return "CMYK RLE 4";
				case CMYK_RLE8:
					return "CMYK RLE 8";
				case HUFFMAN_1D:
					return "Huffman 1D";
				case JPEG:
					return "JPEG";
				case NONE:
					return "No compression";
				case PNG:
					return "PNG";
				case RGBA_BIT_FIELDS:
					return "RGBA bit field masks";
				case RLE24:
					return "RLE 24-bit/pixel";
				case RLE4:
					return "RLE 4-bit/pixel";
				case RLE8:
					return "RLE 8-bit/pixel";
				case UNKNOWN:
					return "Unknown compression";
				default:
					return super.toString();
			}
		}

		public static CompressionType typeOf(int value, int headerSize) {
			switch (value) {
				case 0:
					return NONE;
				case 1:
					return RLE8;
				case 2:
					return RLE4;
				case 3:
					return headerSize == 64 ? BIT_FIELDS : HUFFMAN_1D;
				case 4:
					return headerSize == 64 ? JPEG : RLE24;
				case 5:
					return PNG;
				case 6:
					return RGBA_BIT_FIELDS;
				case 11:
					return CMYK_NONE;
				case 12:
					return CMYK_RLE8;
				case 13:
					return CMYK_RLE4;
				default:
					return UNKNOWN;
			}
		}
	}

	protected static class BMPParseInfo extends ParseInfo {
		CompressionType compressionType;
	}

	@Override
	protected void buildToString(StringBuilder sb) {
		if (compressionType != null) {
			sb.append(", Compression Type = ").append(compressionType);
		}
	}
}
