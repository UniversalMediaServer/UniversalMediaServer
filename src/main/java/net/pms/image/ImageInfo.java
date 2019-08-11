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

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.io.Serializable;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.metadata.Metadata;
import net.pms.dlna.DLNAImage;
import net.pms.dlna.DLNAImageInputStream;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.image.ExifInfo.ExifParseInfo;
import net.pms.util.InvalidStateException;
import net.pms.util.ParseException;

/**
 * This holds information about a given image, and is used as a standard image
 * information container in {@link DLNAResource}, {@link DLNAThumbnail},
 * {@link DLNAThumbnailInputStream}, {@link DLNAImageInputStream},
 * {@link DLNAImage} and {@link Image}.
 *
 * Size might not always be available (live- or web streams for example), and
 * will in those cases be and be set to {@link #SIZE_UNKNOWN}. Unknown integer
 * values are and should be set to {@link #UNKNOWN}.
 *
 * The class itself is immutable although some of the objects it or it's
 * subclasses references might not be. The definition of immutable requires any
 * immutable class to also be {@code final} because otherwise a subclass could
 * break the immutability. Instead, any subclasses must make sure not to break
 * the immutability.
 *
 * @author Nadahar
 */
@SuppressWarnings("serial")
public abstract class ImageInfo implements Serializable {

	/*
	 * Please note: This class is packed and stored in the database. Any changes
	 * to the data structure (fields) will invalidate any instances already
	 * stored.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageInfo.class);
	/**
	 * Used to symbolize an unknown image size.
	 */
	public static final long SIZE_UNKNOWN = Long.MIN_VALUE;
	/**
	 * Used to symbolize unknown int values.
	 */
	public static final int UNKNOWN = Integer.MIN_VALUE;
	protected final int width;
	protected final int height;
	protected final ImageFormat format;
	protected final long size;
	protected final int bitDepth;
	protected final int numComponents;
	protected final ColorSpace colorSpace;
	protected final ColorSpaceType colorSpaceType;
	protected final boolean imageIOSupport;

	/**
	 * This field is used to make the parsing result available to subclass
	 * constructors so that they can initialize their final fields. It is only
	 * used during construction, and isn't kept when serialized.
	 */
	protected transient ParseInfo parsedInfo = null;

	/**
	 * This method is responsible for creating a {@link ParseInfo} instance of
	 * the correct type for that subclass.
	 *
	 * @return A new {@link ParseInfo} instance of the correct type.
	 */
	protected abstract ParseInfo createParseInfo();

	/**
	 * This method is responsible for parsing a {@link Metadata} instance and
	 * storing it in a {@link ParseInfo} instance.
	 *
	 * @param metadata the {@link Metadata} instance to parse.
	 * @throws ParseException if the parsing fails.
	 */
	protected abstract void parseMetadata(Metadata metadata) throws ParseException;

	/**
	 * Creates a new {@link ImageInfo} instance populated with the information
	 * in the parameters.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes or {@link #SIZE_UNKNOWN}.
	 * @param colorModel the {@link ColorModel} used in the image or
	 *            {@code null} if unknown.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @param imageIOSupport whether or not {@link ImageIO} can read/parse this
	 *            image.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails or parsing of
	 *             {@code metadata} fails.
	 */
	public static ImageInfo create(
		int width,
		int height,
		ImageFormat format,
		long size,
		ColorModel colorModel,
		Metadata metadata,
		boolean applyExifOrientation,
		boolean imageIOSupport
	) throws ParseException {
		if (format == null && metadata != null) {
			format = ImageFormat.toImageFormat(metadata);
			if (format == null) {
				throw new ParseException("Unable to determine image format from metadata");
			}
		}
		if (format == null) {
			throw new IllegalArgumentException("Both format and metadata cannot be null");
		}

		if (format.isRaw()) {
			return new RAWInfo(
				width, height, format, size, colorModel, metadata,
				applyExifOrientation, imageIOSupport
			);
		}

		switch (format) {
			case ICNS:
			case IFF:
			case PICT:
			case PNM:
			case RGBE:
			case SGI:
			case TGA:
			case WBMP:
				return new GenericImageInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case BMP:
				return new BMPInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case CUR:
				return new CURInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case DCX:
				return new PCXInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case GIF:
				return new GIFInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case ICO:
				return new ICOInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case JPEG:
				return new JPEGInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case PCX:
				return new PCXInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case PNG:
				return new PNGInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case PSD:
				return new PSDInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case TIFF:
				return new TIFFInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			case WEBP:
				return new WebPInfo(
					width, height, format, size, colorModel, metadata,
					applyExifOrientation, imageIOSupport
				);
			default:
				throw new IllegalStateException("Format " + format + " is unknown for ImageInfo.create()");
		}
	}

	/**
	 * Creates a new {@link ImageInfo} instance populated with the information in
	 * the parameters.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes or {@link #SIZE_UNKNOWN}.
	 * @param colorModel the {@link ColorModel} used in the image or
	 *            {@code null} if unknown.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @param imageIOSupport whether or not {@link ImageIO} can read/parse this
	 *            image.
	 * @throws ParseException if parsing of {@code metadata} fails.
	 */
	protected ImageInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		ColorModel colorModel,
		Metadata metadata,
		boolean applyExifOrientation,
		boolean imageIOSupport
	) throws ParseException {
		parsedInfo = createParseInfo();
		parseMetadata(metadata);
		compareResolution(width, height, parsedInfo);
		if (
			(width < 1 || height < 1) &&
			parsedInfo.width != null && parsedInfo.height != null
		) {
			width = parsedInfo.width.intValue();
			height = parsedInfo.height.intValue();
		}
		if (
			applyExifOrientation &&
			parsedInfo instanceof ExifParseInfo &&
			((ExifParseInfo) parsedInfo).exifOrientation != null &&
			ImagesUtil.isExifAxesSwapNeeded(((ExifParseInfo) parsedInfo).exifOrientation)
		) {
			this.width = height;
			this.height = width;
		} else {
			this.width = width;
			this.height = height;
		}
		compareFormat(format, parsedInfo);
		this.format = format != null ? format : parsedInfo.format;

		this.size =
			!applyExifOrientation ||
			!(parsedInfo instanceof ExifParseInfo) ||
			((ExifParseInfo) parsedInfo).exifOrientation == ExifOrientation.TOP_LEFT ?
				size :
				SIZE_UNKNOWN;

		if (colorModel == null ||
			// ImageIO will parse CMYK JPEGs with a RGB color model.
			// Prefer the parsed info in that situation
			this instanceof JPEGInfo && parsedInfo.colorSpaceType == ColorSpaceType.TYPE_CMYK
		) {
			this.bitDepth = parsedInfo.bitDepth != null ? parsedInfo.bitDepth : UNKNOWN;
			this.numComponents = parsedInfo.numComponents != null ? parsedInfo.numComponents : UNKNOWN;
			this.colorSpace = null;
			this.colorSpaceType = parsedInfo.colorSpaceType;
		} else {
			int bitDepth = UNKNOWN;
			if (colorModel.getNumComponents() > 0) {
				try {
					bitDepth = ImagesUtil.getBitDepthFromArray(colorModel.getComponentSize());
				} catch (InvalidStateException e) {
					LOGGER.trace(
						"Unexpected bit depth array retrieved from ColorModel: {}",
						Arrays.toString(colorModel.getComponentSize())
					);
				}
			}
			int numComponents = colorModel.getNumComponents();
			ColorSpaceType colorSpaceType = ColorSpaceType.toColorSpaceType(colorModel.getColorSpace().getType());

			compareColorModel(bitDepth, numComponents, colorSpaceType, parsedInfo);

			this.bitDepth =
				bitDepth == UNKNOWN && parsedInfo.bitDepth != null ?
					parsedInfo.bitDepth.intValue() :
					bitDepth;
			this.numComponents =
				numComponents == UNKNOWN && parsedInfo.numComponents != null ?
					parsedInfo.numComponents.intValue() :
					numComponents;
			this.colorSpace = colorModel.getColorSpace();
			this.colorSpaceType =
				colorSpaceType == null && parsedInfo.colorSpaceType != null ?
					parsedInfo.colorSpaceType :
					colorSpaceType;
		}
		this.imageIOSupport = imageIOSupport;
	}

	/**
	 * Creates a new {@link ImageInfo} instance populated with the information
	 * in the parameters.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes or {@link #SIZE_UNKNOWN}.
	 * @param bitDepth the number of bits per color model component for this
	 *            image.
	 * @param numComponents the number of color model components for this image.
	 * @param colorSpace the {@link ColorSpace} of this image.
	 * @param colorSpaceType the {@link ColorSpaceType} of this image. Only used
	 *            if {@code ColorSpace} is {@code null}.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @param imageIOSupport whether or not {@link ImageIO} can read/parse this
	 *            image.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails or parsing of
	 *             {@code metadata} fails.
	 */
	public static ImageInfo create(
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
		if (format == null && metadata != null) {
			format = ImageFormat.toImageFormat(metadata);
			if (format == null) {
				throw new ParseException("Unable to determine image format from metadata");
			}
		}
		if (format == null) {
			throw new IllegalArgumentException("Both format and metadata cannot be null");
		}

		if (format.isRaw()) {
			return new RAWInfo(
				width, height, format, size, bitDepth, numComponents,
				colorSpace, colorSpaceType, metadata, applyExifOrientation,
				imageIOSupport
			);
		}

		switch (format) {
			case ICNS:
			case IFF:
			case PICT:
			case PNM:
			case RGBE:
			case SGI:
			case TGA:
			case WBMP:
				return new GenericImageInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case BMP:
				return new BMPInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case CUR:
				return new CURInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case DCX:
				return new PCXInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case GIF:
				return new GIFInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case ICO:
				return new ICOInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case JPEG:
				return new JPEGInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case PCX:
				return new PCXInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case PNG:
				return new PNGInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case PSD:
				return new PSDInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case TIFF:
				return new TIFFInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			case WEBP:
				return new WebPInfo(
					width, height, format, size, bitDepth, numComponents,
					colorSpace, colorSpaceType, metadata, applyExifOrientation,
					imageIOSupport
				);
			default:
				throw new IllegalStateException("Format " + format + " is unknown for this ImageInfo.create()");
		}
	}

	/**
	 * Creates a new {@link ImageInfo} instance populated with the information
	 * in the parameters.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes or {@link #SIZE_UNKNOWN}.
	 * @param bitDepth the number of bits per color model component for this
	 *                 image.
	 * @param numComponents the number of color model components for this
	 *                      image.
     * @param colorSpace the {@link ColorSpace} of this image.
	 * @param colorSpaceType the {@link ColorSpaceType} of this image. Only
	 *                       used if {@code ColorSpace} is {@code null}.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @param imageIOSupport whether or not {@link ImageIO} can read/parse this
	 *                       image.
	 * @throws ParseException if parsing of {@code metadata} fails.
	 */
	protected ImageInfo(
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
		parsedInfo = createParseInfo();
		parseMetadata(metadata);
		compareResolution(width, height, parsedInfo);
		if (
			(width < 1 || height < 1) &&
			parsedInfo.width != null && parsedInfo.height != null
		) {
			width = parsedInfo.width.intValue();
			height = parsedInfo.height.intValue();
		}
		if (
			applyExifOrientation &&
			parsedInfo instanceof ExifParseInfo &&
			((ExifParseInfo) parsedInfo).exifOrientation != null &&
			ImagesUtil.isExifAxesSwapNeeded(((ExifParseInfo) parsedInfo).exifOrientation)
		) {
			this.width = height;
			this.height = width;
		} else {
			this.width = width;
			this.height = height;
		}

		compareFormat(format, parsedInfo);
		this.format = format != null ? format : parsedInfo.format;

		this.size =
			!applyExifOrientation ||
			!(parsedInfo instanceof ExifParseInfo) ||
			((ExifParseInfo) parsedInfo).exifOrientation == ExifOrientation.TOP_LEFT ?
				size :
				SIZE_UNKNOWN;

		if (this instanceof JPEGInfo && parsedInfo.colorSpaceType == ColorSpaceType.TYPE_CMYK) {
			// ImageIO will parse CMYK JPEGs with a RGB color model.
			// Prefer the parsed info in that situation
			this.bitDepth = parsedInfo.bitDepth != null ? parsedInfo.bitDepth.intValue() : bitDepth;
			this.numComponents = parsedInfo.numComponents != null ? parsedInfo.numComponents.intValue() : numComponents;
			this.colorSpace = null;
			this.colorSpaceType = parsedInfo.colorSpaceType;
		} else {
			colorSpaceType = colorSpace != null ? ColorSpaceType.toColorSpaceType(colorSpace.getType()) : colorSpaceType;
			compareColorModel(bitDepth, numComponents, colorSpaceType, parsedInfo);

			this.bitDepth =
				bitDepth == UNKNOWN && parsedInfo.bitDepth != null ?
					parsedInfo.bitDepth.intValue() :
					bitDepth;
			this.numComponents =
				numComponents == UNKNOWN && parsedInfo.numComponents != null ?
					parsedInfo.numComponents.intValue() :
					numComponents;
			this.colorSpace = colorSpace;
			this.colorSpaceType =
				colorSpaceType == null && parsedInfo.colorSpaceType != null ?
					parsedInfo.colorSpaceType :
					colorSpaceType;
		}
		this.imageIOSupport = imageIOSupport;
	}

	/**
	 * Tries to create an {@link ImageInfo} instance from {@link Metadata}. If
	 * {@code metadata} is null or can't be parsed, an instance with invalid
	 * values is created or an {@link ParseException} is thrown depending on
	 * {@code throwOnParseFailure}. The {@link ColorModel} in this instance will
	 * be {@code null}. This constructor should only be used if {@link ImageIO}
	 * can't parse the source. Instances created with this constructor will have
	 * {@code isImageIOSupport()} set to false.
	 *
	 * @param metadata the {@link Metadata} describing the image.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes or {@link #SIZE_UNKNOWN}.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @param throwOnParseFailure if a {@link ParseException} should be thrown
	 *            instead of returning an instance with invalid resolution if
	 *            parsing of resolution fails.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails or parsing of
	 *             {@code metadata} fails.
	 */
	public static ImageInfo create(
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		return create(UNKNOWN, UNKNOWN, metadata, format, size, applyExifOrientation, throwOnParseFailure);
	}

	/**
	 * Tries to create an {@link ImageInfo} instance from {@link Metadata}. If
	 * {@code metadata} is null or can't be parsed, an instance with invalid
	 * values is created or an {@link ParseException} is thrown depending on
	 * {@code throwOnParseFailure}. The {@link ColorModel} in this instance will
	 * be {@code null}. This constructor should only be used if {@link ImageIO}
	 * can't parse the source. Instances created with this constructor will have
	 * {@code isImageIOSupport()} set to false.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes or {@link #SIZE_UNKNOWN}.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @param throwOnParseFailure if a {@link ParseException} should be thrown
	 *            instead of returning an instance with invalid resolution if
	 *            parsing of resolution fails.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails or parsing of
	 *             {@code metadata} fails.
	 */
	public static ImageInfo create(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		if (format == null && metadata != null) {
			format = ImageFormat.toImageFormat(metadata);
			if (format == null) {
				throw new ParseException("Unable to determine image format from metadata");
			}
		}
		if (format == null) {
			throw new IllegalArgumentException("Both format and metadata cannot be null");
		}

		if (format.isRaw()) {
			return new RAWInfo(
				width, height, metadata, format, size,
				applyExifOrientation, throwOnParseFailure
			);
		}

		switch (format) {
			case ICNS:
			case IFF:
			case PICT:
			case PNM:
			case RGBE:
			case SGI:
			case TGA:
			case WBMP:
				return new GenericImageInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case BMP:
				return new BMPInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case CUR:
				return new CURInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case DCX:
				return new PCXInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case GIF:
				return new GIFInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case ICO:
				return new ICOInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case JPEG:
				return new JPEGInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case PCX:
				return new PCXInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case PNG:
				return new PNGInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case PSD:
				return new PSDInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case TIFF:
				return new TIFFInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			case WEBP:
				return new WebPInfo(
					width, height, metadata, format, size,
					applyExifOrientation, throwOnParseFailure
				);
			default:
				throw new IllegalStateException("Format " + format + " is unknown for ImageInfo.create()");
		}
	}

	/**
	 * Tries to create an {@link ImageInfo} instance from {@link Metadata}. If
	 * {@code metadata} is null or can't be parsed, an instance with invalid
	 * values is created or an {@link ParseException} is thrown depending on
	 * {@code throwOnParseFailure}. The {@link ColorModel} in this instance will
	 * be {@code null}. This constructor should only be used if {@link ImageIO}
	 * can't parse the source. Instances created with this constructor will have
	 * {@code isImageIOSupport()} set to false.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes or {@link #SIZE_UNKNOWN}.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @param throwOnParseFailure if a {@link ParseException} should be thrown
	 *            instead of returning an instance with invalid resolution if
	 *            parsing of resolution fails.
	 * @throws ParseException if a parsing error occurs or parsing fails.
	 */
	protected ImageInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		parsedInfo = createParseInfo();
		parseMetadata(metadata);
		compareResolution(width, height, parsedInfo);
		if (
			(width < 1 || height < 1) &&
			parsedInfo.width != null && parsedInfo.height != null
		) {
			width = parsedInfo.width.intValue();
			height = parsedInfo.height.intValue();
		}

		if (throwOnParseFailure && (width < 0 || height < 0)) {
			throw new ParseException("Failed to parse image resolution from metadata");
		}

		if (
			applyExifOrientation &&
			parsedInfo instanceof ExifParseInfo &&
			((ExifParseInfo) parsedInfo).exifOrientation != null &&
			ImagesUtil.isExifAxesSwapNeeded(((ExifParseInfo) parsedInfo).exifOrientation)
		) {
			this.width = height;
			this.height = width;
		} else {
			this.width = width;
			this.height = height;
		}

		compareFormat(format, parsedInfo);
		this.format = format != null ? format : parsedInfo.format;

		this.size =
			!applyExifOrientation ||
			!(parsedInfo instanceof ExifParseInfo) ||
			((ExifParseInfo) parsedInfo).exifOrientation == ExifOrientation.TOP_LEFT ?
				size :
				SIZE_UNKNOWN;
		this.bitDepth = parsedInfo.bitDepth != null ? parsedInfo.bitDepth.intValue() : UNKNOWN;
		this.numComponents = parsedInfo.numComponents != null ? parsedInfo.numComponents.intValue() : UNKNOWN;
		this.colorSpace = null;
		this.colorSpaceType = parsedInfo.colorSpaceType;
		this.imageIOSupport = false; // Implied by calling this constructor
	}

	/**
	 * Copy constructor
	 */
	protected ImageInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		boolean imageIOSupport
	) {
		this.width = width;
		this.height = height;
		this.format = format;
		this.size = size;
		this.bitDepth = bitDepth;
		this.numComponents = numComponents;
		this.colorSpace = colorSpace;
		this.colorSpaceType = colorSpaceType;
		this.imageIOSupport = imageIOSupport;
	}

	/**
	 * @return The image width or {@link #UNKNOWN} if unknown.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return The image height or {@link #UNKNOWN} if unknown.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @return The {@link ImageFormat} or {@code null} if unknown.
	 */
	public ImageFormat getFormat() {
		return format;
	}

	/**
	 * @return The image size in bytes or {@link #SIZE_UNKNOWN}
	 *         if the size is unknown.
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @return The {@link ColorSpace} or {@code null} if unknown.
	 */
	public ColorSpace getColorSpace() {
		return colorSpace;
	}

	/**
	 * @return The {@link ColorSpaceType} or {@code null} if unknown.
	 */
	public ColorSpaceType getColorSpaceType() {
		if (colorSpace != null) {
			return ColorSpaceType.toColorSpaceType(colorSpace.getType());
		}
		return colorSpaceType;
	}

	/**
	 * @return The number of bits per pixel or {@link #UNKNOWN} if unknown.
	 */
	public int getBitsPerPixel() {
		return bitDepth < 0 || numComponents < 0 ? UNKNOWN : bitDepth * numComponents;
	}

	/**
	 * The number of components describe how many "channels" the color model
	 * has. A grayscale image without alpha has 1, a RGB image without alpha has
	 * 3, a RGB image with alpha has 4 etc.
	 *
	 * @return The number of components in the {@link ColorModel} or
	 *         {@link #UNKNOWN} if unknown.
	 */
	public int getNumComponents() {
		return numComponents;
	}

	/**
	 * @return The number of bits per color "channel" or {@link #UNKNOWN} if
	 *         unknown.
	 *
	 * @see #getBitPerPixel()
	 * @see #getNumColorComponents()
	 */
	public int getBitDepth() {
		return bitDepth;
	}

	/**
	 * @return Whether or not {@link ImageIO} can read/parse this image.
	 */
	public boolean isImageIOSupported() {
		return imageIOSupport;
	}

	/**
	 * @return The {@link ExifOrientation} or {@link ExifOrientation#TOP_LEFT} if unknown.
	 */
	public ExifOrientation getExifOrientation() {
		if (!(this instanceof ExifInfo)) {
			return ExifOrientation.TOP_LEFT;
		}

		return ((ExifInfo) this).exifOrientation != null ?
			((ExifInfo) this).exifOrientation :
			ExifOrientation.TOP_LEFT
		;
	}

	/**
	 * @return A copy of this {@link ImageInfo} instance.
	 */
	public abstract ImageInfo copy();

	protected abstract void buildToString(StringBuilder sb);

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(80);
		sb.append(getClass().getSimpleName())
			.append(": [Format = ").append(format)
			.append(", Resolution = ").append(width == UNKNOWN ? "Unknown" : width)
			.append("×").append(height == UNKNOWN ? "Unknown" : height)
			.append(", Size = ").append(size == SIZE_UNKNOWN ? "Unknown" : size)
			.append(", Bit Depth = ").append(bitDepth == UNKNOWN ? "Unknown" : bitDepth)
			.append(", Number of Components = ").append(numComponents == UNKNOWN ? "Unknown" : numComponents);
		if (colorSpace != null) {
			sb.append(", Color Space = [");
			for (int i = 0; i < colorSpace.getNumComponents(); i++) {
				if (i != 0) {
					sb.append(", ");
				}
				sb.append(colorSpace.getName(i));
			}
			sb.append("]");
		}
		if (colorSpaceType != null) {
			sb.append(", Color Space Type = ").append(colorSpaceType);
		}
		sb.append(", ImageIO Support = ").append(imageIOSupport ? "True" : "False");
		buildToString(sb);
		sb.append("]");

		return sb.toString();
	}

	/**
	 * Compares the parsed and the given resolution and logs a warning if they
	 * mismatch.
	 */
	protected void compareResolution(int width, int height, ParseInfo parsedInfo) {
		if (parsedInfo == null) {
			return;
		}

		int parsedWidth = parsedInfo.width != null ? parsedInfo.width.intValue() : UNKNOWN;
		int parsedHeight = parsedInfo.height!= null ? parsedInfo.height.intValue() : UNKNOWN;
		if (this instanceof RAWInfo) {
			// DCRaw decodes pixels that's normally hidden because they are too
			// expensive to decode for CPU restrained devices, so the resolution
			// might be some pixels larger.
			if (parsedInfo.width != null && width >= parsedInfo.width.intValue() && width <= (parsedInfo.width.intValue() + 40)) {
				parsedWidth = Integer.valueOf(width);
			}
			if (parsedInfo.height != null && height >= parsedInfo.height.intValue() && height <= (parsedInfo.height.intValue() + 40)) {
				parsedHeight = Integer.valueOf(height);
			}
		}

		if (
			width > 0 && parsedInfo.width != null && width != parsedWidth ||
			height > 0 && parsedInfo.height != null && height != parsedHeight
		) {
			LOGGER.debug(
				"Warning: Parsed image resolution ({} x {}) mismatches given image " +
				"resolution ({} x {}) - using given resolution",
				parsedInfo.width,
				parsedInfo.height,
				width,
				height
			);
		}
	}

	/**
	 * Compares the parsed and the given {@link ImageFormat} and logs a warning
	 * if they mismatch.
	 */
	protected void compareFormat(ImageFormat format, ParseInfo parsedInfo) {
		if (parsedInfo == null) {
			return;
		}

		if (parsedInfo.format != null && format != null && format != parsedInfo.format) {
			LOGGER.debug(
				"Warning: Parsed image format ({}) mismatches given image " +
				"format ({}) - using given format",
				parsedInfo.format,
				format
			);
		}
	}

	/**
	 * Compares the parsed and the given color model information and logs a
	 * warning if any information mismatch.
	 */
	protected void compareColorModel(int bitDepth, int numComponents, ColorSpaceType colorSpaceType, ParseInfo parsedInfo) {
		if (parsedInfo == null) {
			return;
		}

		if (bitDepth != UNKNOWN && parsedInfo.bitDepth != null && bitDepth != parsedInfo.bitDepth.intValue()) {
			if (
				!(this instanceof GIFInfo) ||
				parsedInfo.bitDepth >= 8
			) {
				// It seems like ImageIO will parse GIFs with any bit depth as 8, no reason to log that
				LOGGER.debug(
					"Warning: Parsed image bit depth ({}) mismatches given color model " +
					"bit depth ({}) - using given color model bit depth",
					parsedInfo.bitDepth,
					bitDepth
				);
			}
		}

		if (numComponents != UNKNOWN && parsedInfo.numComponents != null && numComponents != parsedInfo.numComponents.intValue()) {
			LOGGER.debug(
				"Warning: Parsed image number of components ({}) mismatches given color model " +
				"number of components ({}) - using given color model number of components",
				parsedInfo.numComponents,
				numComponents
			);
		}

		if (colorSpaceType != null && parsedInfo.colorSpaceType != null && colorSpaceType != parsedInfo.colorSpaceType) {
			if (
				!(this instanceof JPEGInfo) ||
				parsedInfo.colorSpaceType != ColorSpaceType.TYPE_YCbCr &&
				colorSpaceType != ColorSpaceType.TYPE_RGB
			) {
				// ImageIO (TwelveMonkeys) will convert YCbCr to RGB when reading JPEGs, no reason to log that
				LOGGER.debug(
					"Warning: Parsed image color space type ({}) mismatches given color model " +
					"color space type ({}) - using given color model color space type",
					parsedInfo.colorSpaceType,
					colorSpaceType
				);
			}
		}
	}

	protected static class ParseInfo {
		Integer width;
		Integer height;
		ImageFormat format;
		Integer bitDepth;
		Integer numComponents;
		ColorSpaceType colorSpaceType;
	}
}
