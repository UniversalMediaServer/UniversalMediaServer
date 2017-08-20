package net.pms.image;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.util.InvalidStateException;
import net.pms.util.ParseException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public abstract class ExifInfo extends ImageInfo {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExifInfo.class);

	protected final PhotometricInterpretation photometricInterpretation;
	protected final ExifOrientation exifOrientation;
	protected final ExifOrientation originalExifOrientation;
	protected final Integer exifVersion;
	protected final ExifCompression exifCompression;
	protected final ExifColorSpace exifColorSpace;
	protected final Boolean hasExifThumbnail;

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, ColorModel, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected ExifInfo(
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

		photometricInterpretation = ((ExifParseInfo) parsedInfo).photometricInterpretation;
		exifOrientation = applyExifOrientation ? ExifOrientation.TOP_LEFT : ((ExifParseInfo) parsedInfo).exifOrientation;
		originalExifOrientation = ((ExifParseInfo) parsedInfo).exifOrientation;
		exifVersion = ((ExifParseInfo) parsedInfo).exifVersion;
		exifCompression = ((ExifParseInfo) parsedInfo).exifCompression;
		exifColorSpace = ((ExifParseInfo) parsedInfo).exifColorSpace;
		hasExifThumbnail = ((ExifParseInfo) parsedInfo).hasExifThumbnail;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, int, int, ColorSpace, ColorSpaceType, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected ExifInfo(
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

		photometricInterpretation = ((ExifParseInfo) parsedInfo).photometricInterpretation;
		exifOrientation = applyExifOrientation ? ExifOrientation.TOP_LEFT : ((ExifParseInfo) parsedInfo).exifOrientation;
		originalExifOrientation = ((ExifParseInfo) parsedInfo).exifOrientation;
		exifVersion = ((ExifParseInfo) parsedInfo).exifVersion;
		exifCompression = ((ExifParseInfo) parsedInfo).exifCompression;
		exifColorSpace = ((ExifParseInfo) parsedInfo).exifColorSpace;
		hasExifThumbnail = ((ExifParseInfo) parsedInfo).hasExifThumbnail;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, Metadata, ImageFormat, long, boolean, boolean)}
	 * to instantiate.
	 */
	protected ExifInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		super(width, height, metadata, format, size, applyExifOrientation, throwOnParseFailure);

		photometricInterpretation = ((ExifParseInfo) parsedInfo).photometricInterpretation;
		exifOrientation = applyExifOrientation ? ExifOrientation.TOP_LEFT : ((ExifParseInfo) parsedInfo).exifOrientation;
		originalExifOrientation = ((ExifParseInfo) parsedInfo).exifOrientation;
		exifVersion = ((ExifParseInfo) parsedInfo).exifVersion;
		exifCompression = ((ExifParseInfo) parsedInfo).exifCompression;
		exifColorSpace = ((ExifParseInfo) parsedInfo).exifColorSpace;
		hasExifThumbnail = ((ExifParseInfo) parsedInfo).hasExifThumbnail;
	}

	/**
	 * Copy constructor
	 */
	protected ExifInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		boolean imageIOSupport,
		PhotometricInterpretation photometricInterpretation,
		ExifOrientation exifOrientation,
		ExifOrientation originalExifOrientation,
		Integer exifVersion,
		ExifCompression exifCompression,
		ExifColorSpace exifColorSpace,
		Boolean hasExifThumbnail
	) {
		super(width, height, format, size, bitDepth, numComponents, colorSpace, colorSpaceType, imageIOSupport);
		this.photometricInterpretation = photometricInterpretation;
		this.exifOrientation = exifOrientation;
		this.originalExifOrientation = originalExifOrientation;
		this.exifVersion = exifVersion;
		this.exifCompression = exifCompression;
		this.exifColorSpace = exifColorSpace;
		this.hasExifThumbnail = hasExifThumbnail;
	}

	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	@Override
	protected void parseMetadata(Metadata metadata) {
		if (metadata == null) {
			return;
		}

		((ExifParseInfo) parsedInfo).hasExifThumbnail = false;
		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof ExifIFD0Directory || directory instanceof ExifSubIFDDirectory) {
				if (
					// Prefer Exif SubIFD Exif image width and height as they seem to be more accurate.
					directory instanceof ExifSubIFDDirectory &&
					((ExifSubIFDDirectory) directory).containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH) &&
					((ExifSubIFDDirectory) directory).containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)
				) {
					parsedInfo.width = ((ExifSubIFDDirectory) directory).getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
					parsedInfo.height = ((ExifSubIFDDirectory) directory).getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
				} else if (
					(parsedInfo.width == null || parsedInfo.height == null || parsedInfo.width < 1 || parsedInfo.height < 1) &&
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_IMAGE_WIDTH) &&
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_IMAGE_HEIGHT)
				) {
					parsedInfo.width = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_IMAGE_WIDTH);
					parsedInfo.height = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_IMAGE_HEIGHT);
				}
				if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
					Integer i = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_ORIENTATION);
					if (i != null) {
						((ExifParseInfo) parsedInfo).exifOrientation = ExifOrientation.typeOf(i);
					}
				}
				if (
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_COMPRESSION) &&
					((ExifParseInfo) parsedInfo).exifCompression == null
				) {
					Integer i = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_COMPRESSION);
					if (i != null) {
						((ExifParseInfo) parsedInfo).exifCompression = ExifCompression.typeOf(i);
					}
				}
				if (
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_BITS_PER_SAMPLE) &&
					((ExifParseInfo) parsedInfo).exifCompression == ExifCompression.UNCOMPRESSED
				) {
					byte[] bytes = ((ExifDirectoryBase) directory).getByteArray(ExifDirectoryBase.TAG_BITS_PER_SAMPLE);
					if (bytes != null && bytes.length > 0) {
						Integer i;
						try {
							i = Integer.valueOf(ImagesUtil.getBitDepthFromArray(bytes));
						} catch (InvalidStateException e) {
							i = null;
							LOGGER.trace(
								"Unexpected bit depth array retrieved from Exif tag: {}",
								Arrays.toString(bytes)
							);
						}
						if (i != null && i.intValue() > 0) {
							parsedInfo.bitDepth = i;
						}
					}
				}
				if (
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_SAMPLES_PER_PIXEL) &&
					parsedInfo.numComponents == null
				) {
					Integer i = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_SAMPLES_PER_PIXEL);
					if (i != null) {
						parsedInfo.numComponents = i;
					}
				}
				if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_PHOTOMETRIC_INTERPRETATION)) {
					Integer i = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_PHOTOMETRIC_INTERPRETATION);
					if (i != null) {
						((ExifParseInfo) parsedInfo).photometricInterpretation = PhotometricInterpretation.typeOf(i);

						if (((ExifParseInfo) parsedInfo).photometricInterpretation != null) {
							switch (((ExifParseInfo) parsedInfo).photometricInterpretation) {
								case RGB:
								case RGB_PALETTE:
									parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
									break;
								case Y_CB_CR:
									parsedInfo.colorSpaceType = ColorSpaceType.TYPE_YCbCr;
									break;
								case CMYK:
									parsedInfo.colorSpaceType = ColorSpaceType.TYPE_CMYK;
									break;
								case CIE_LAB:
								case ICC_LAB:
								case ITU_LAB:
									parsedInfo.colorSpaceType = ColorSpaceType.TYPE_Lab;
									break;
								case PIXAR_LOG_LUV:
									parsedInfo.colorSpaceType = ColorSpaceType.TYPE_Luv;
									break;
								default:
							}
						}
					}
				}
				if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_EXIF_VERSION)) {
					byte[] bytes = ((ExifDirectoryBase) directory).getByteArray(ExifDirectoryBase.TAG_EXIF_VERSION);
					if (bytes != null) {
						((ExifParseInfo) parsedInfo).exifVersion = ImagesUtil.parseExifVersion(bytes);
					}
				}

				if (
					directory instanceof ExifIFD0Directory &&
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_COLOR_SPACE)
				) {
					Integer i = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_COLOR_SPACE);
					if (i != null) {
						((ExifParseInfo) parsedInfo).exifColorSpace = ExifColorSpace.typeOf(i.intValue());
					}
				}
			} else if (directory instanceof ExifThumbnailDirectory && !((ExifParseInfo) parsedInfo).hasExifThumbnail.booleanValue()) {
				if (((ExifThumbnailDirectory) directory).containsTag(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH)) {
					Integer i = ((ExifThumbnailDirectory) directory).getInteger(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH);
					((ExifParseInfo) parsedInfo).hasExifThumbnail = Boolean.valueOf(i != null && i.intValue() > 0);
				}
			}
		}
	}

	/**
	 * @return The unmodified {@link ExifOrientation} as read from the image.
	 *         {@link ImageInfo#getExifOrientation()} is altered when Exif
	 *         Orientation is "applied"/compensated for.
	 */
	public ExifOrientation getOriginalExifOrientation() {
		return originalExifOrientation;
	}

	/**
	 * @return The {@link PhotometricInterpretation} or {@code null} if unknown.
	 */
	public PhotometricInterpretation getPhotometricInterpretation() {
		return photometricInterpretation;
	}

	/**
	 * @return The Exif version multiplied with 100 or {@link ImageInfo#UNKNOWN}
	 *         if unknown.
	 */
	public int getExifVersion() {
		return exifVersion != null ? exifVersion.intValue() : UNKNOWN;
	}

	/**
	 * @return The {@link ExifCompression}.
	 */
	public ExifCompression getExifCompression() {
		return exifCompression;
	}

	/**
	 * @return the {@link ExifColorSpace}.
	 */
	public ExifColorSpace getExifColorSpace() {
		return exifColorSpace;
	}

	/**
	 * @return Whether the image has an embedded Exif thumbnail.
	 */
	public boolean hasExifThumbnail() {
		return hasExifThumbnail != null ? hasExifThumbnail.booleanValue() : false;
	}

	public enum PhotometricInterpretation {
		WHITE_IS_ZERO,
		BLACK_IS_ZERO,
		RGB,
		RGB_PALETTE,
		TRANSPARENCY_MASK,
		CMYK,
		Y_CB_CR,
		CIE_LAB,
		ICC_LAB,
		ITU_LAB,
		COLOR_FILTER_ARRAY,
		PIXAR_LOG_L,
		PIXAR_LOG_LUV,
		LINEAR_RAW;

		public static PhotometricInterpretation typeOf(int value) {
			switch (value) {
		        case 0:
		        	return WHITE_IS_ZERO;
		        case 1:
		        	return BLACK_IS_ZERO;
		        case 2:
		        	return RGB;
		        case 3:
		        	return RGB_PALETTE;
		        case 4:
		        	return TRANSPARENCY_MASK;
		        case 5:
		        	return CMYK;
		        case 6:
		        	return Y_CB_CR;
		        case 8:
		        	return CIE_LAB;
		        case 9:
		        	return ICC_LAB;
		        case 10:
		        	return ITU_LAB;
		        case 32803:
		        	return COLOR_FILTER_ARRAY;
		        case 32844:
		        	return PIXAR_LOG_L;
		        case 32845:
		        	return PIXAR_LOG_LUV;
		        case 34892:
		        	return LINEAR_RAW;
		        default:
		            return null;
			}
		}
	}

	public enum ExifCompression {
		ADOBE_DEFLATE,
		CCIRLEW,
		CCITT_1D,
		DCS,
		DEFLATE,
		IT8BL,
		IT8CTPAD,
		IT8LW,
		IT8MP,
		JBIG,
		JBIG2_TIFF_FX,
		JBIG_B_W,
		JBIG_COLOR,
		JPEG,
		JPEG_2000,
		JPEG_OLD_STYLE,
		KODAC_DCR_COMPRESSED,
		KODAK_262,
		KODAK_KDC_COMPRESSED,
		LOSSY_JPEG,
		LZW,
		MDI_BINARY_LEVEL_CODEC,
		MDI_PROGRESSIVE_TRANSFORM_CODEC,
		MDI_VECTOR,
		NEXT,
		NIKON_NEF_COMPRESSED,
		PACKBITS,
		PACKED_RAW,
		PENTAX_PEF_COMPRESSED,
		PIXAR_FILM,
		PIXAR_LOG,
		SAMSUNG_SRW_COMPRESSED,
		SAMSUNG_SRW_COMPRESSED_2,
		SGILOG,
		SGILOG24,
		SONY_ARW_COMPRESSED,
		T4_GROUP_3_FAX,
		T6_GROUP_4_FAX,
		THUNDERSCAN,
		UNCOMPRESSED;

		public static ExifCompression typeOf(int value) {
			switch (value) {
		        case 1:
		        	return UNCOMPRESSED;
		        case 2:
		        	return CCITT_1D;
		        case 3:
		        	return T4_GROUP_3_FAX;
		        case 4:
		        	return T6_GROUP_4_FAX;
		        case 5:
		        	return LZW;
		        case 6:
		        	return JPEG_OLD_STYLE;
		        case 7:
		        	return JPEG;
		        case 8:
		        	return ADOBE_DEFLATE;
		        case 9:
		        	return JBIG_B_W;
		        case 10:
		        	return JBIG_COLOR;
		        case 99:
		        	return JPEG;
		        case 262:
		        	return KODAK_262;
		        case 32766:
		        	return NEXT;
		        case 32767:
		        	return SONY_ARW_COMPRESSED;
		        case 32769:
		        	return PACKED_RAW;
		        case 32770:
		        	return SAMSUNG_SRW_COMPRESSED;
		        case 32771:
		        	return CCIRLEW;
		        case 32772:
		        	return SAMSUNG_SRW_COMPRESSED_2;
		        case 32773:
		        	return PACKBITS;
		        case 32809:
		        	return THUNDERSCAN;
		        case 32867:
		        	return KODAK_KDC_COMPRESSED;
		        case 32895:
		        	return IT8CTPAD;
		        case 32896:
		        	return IT8LW;
		        case 32897:
		        	return IT8MP;
		        case 32898:
		        	return IT8BL;
		        case 32908:
		        	return PIXAR_FILM;
		        case 32909:
		        	return PIXAR_LOG;
		        case 32946:
		        	return DEFLATE;
		        case 32947:
		        	return DCS;
		        case 34661:
		        	return JBIG;
		        case 34676:
		        	return SGILOG;
		        case 34677:
		        	return SGILOG24;
		        case 34712:
		        	return JPEG_2000;
		        case 34713:
		        	return NIKON_NEF_COMPRESSED;
		        case 34715:
		        	return JBIG2_TIFF_FX;
		        case 34718:
		        	return MDI_BINARY_LEVEL_CODEC;
		        case 34719:
		        	return MDI_PROGRESSIVE_TRANSFORM_CODEC;
		        case 34720:
		        	return MDI_VECTOR;
		        case 34892:
		        	return LOSSY_JPEG;
		        case 65000:
		        	return KODAK_KDC_COMPRESSED;
		        case 65535:
		        	return PENTAX_PEF_COMPRESSED;
				default:
					return null;
			}
		}
	}

	public enum ExifColorSpace {
		SRGB(1),
		UNCALIBRATED(65535),
		UNKNOWN(0);

		private int value;
		private ExifColorSpace(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static ExifColorSpace typeOf(int value) {
			switch (value) {
				case 1:
					return SRGB;
				case 65535:
					return UNCALIBRATED;
				default:
					return UNKNOWN;
			}
		}
	}

	protected static class ExifParseInfo extends ParseInfo {
		PhotometricInterpretation photometricInterpretation;
		ExifOrientation exifOrientation;
		Integer exifVersion;
		ExifCompression exifCompression;
		ExifColorSpace exifColorSpace;
		Boolean hasExifThumbnail;
	}

	@Override
	protected void buildToString(StringBuilder sb) {
		if (photometricInterpretation != null) {
			sb.append(", Photometric Interpretation = ").append(photometricInterpretation);
		}
		if (exifOrientation != null) {
			sb.append(", Exif Orientation = ").append(exifOrientation);
		}
		if (originalExifOrientation != null) {
			sb.append(", Original Exif Orientation = ").append(originalExifOrientation);
		}
		if (exifVersion != null) {
			sb.append(", Exif Version = ").append(exifVersion);
		}
		if (exifCompression != null) {
			sb.append(", Exif Compression = ").append(exifCompression);
		}
		if (exifColorSpace != null) {
			sb.append(", Exif Color Space = ").append(exifColorSpace);
		}
		if (hasExifThumbnail != null) {
			sb.append(", Has Exif Thumbnail = ").append(hasExifThumbnail.booleanValue() ? "True" : "False");
		}
	}
}
