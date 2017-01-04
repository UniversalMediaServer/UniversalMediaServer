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
package net.pms.dlna;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.io.Serializable;
import javax.imageio.ImageIO;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
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
import net.pms.formats.ImageFormat;
import net.pms.util.ColorSpaceType;
import net.pms.util.Image;

/**
 * This holds information about a given image, and is used as a standard
 * image information container in {@link DLNAResource}, {@link DLNAThumbnail},
 * {@link DLNAThumbnailInputStream}, {@link DLNAImage} and {@link Image}.
 *
 * Size might not always be available (live- or web streams for example), and
 * should in those cases be set to {@link #SIZE_UNKNOWN}.
 *
 * The class itself is immutable although some of the objects it references
 * aren't. The definition of immutable requires any immutable class to also
 * be {@code final] because otherwise a subclass could break the immutability.
 * Instead, any subclasses must make sure not to break the immutability.
 *
 * @author Nadahar
 */
public class ImageInfo implements Serializable {

	/*
	 * Please note: This class is packed and stored in the database. Any changes
	 * to the data structure (fields) will invalidate any instances already
	 * stored, and will require a wipe of all rows with a stored instance.
	 */
	private static final long serialVersionUID = -6477092856365661709L;
	public static final long SIZE_UNKNOWN = Long.MIN_VALUE;
	protected final int width;
	protected final int height;
	protected final ImageFormat format;
	protected final long size;
	protected final int bitDepth;
	protected final int numComponents;
	protected final ColorSpace colorSpace;
	protected final ColorSpaceType colorSpaceType;
	protected final Metadata metadata;
	protected final int exifOrientation;
	protected final boolean imageIOSupport;

	/**
	 * Create a new {@link ImageInfo} instance populated with the information
	 * in the parameters.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes.
	 * @param colorModel the {@link ColorModel} used in the image or
	 *                   {@code null} if unknown.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param imageIOSupport whether or not {@link ImageIO} can read/parse this
	 *                       image.
	 */
	public ImageInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		ColorModel colorModel,
		Metadata metadata,
		boolean imageIOSupport
	) {
		this.width = width;
		this.height = height;
		this.format = format;
		this.size = size;
		if (colorModel != null) {
			this.bitDepth = colorModel.getNumComponents() > 0 ? colorModel.getPixelSize() / colorModel.getNumComponents() : -1;
			this.numComponents = colorModel.getNumComponents();
			this.colorSpace = colorModel.getColorSpace();
			this.colorSpaceType = ColorSpaceType.toColorSpaceType(colorModel.getColorSpace().getType());
		} else {
			this.bitDepth = -1;
			this.numComponents = -1;
			this.colorSpace = null;
			this.colorSpaceType = null;
		}
		this.metadata = metadata;
		this.exifOrientation = ImageInfo.parseExifOrientation(metadata);
		this.imageIOSupport = imageIOSupport;
	}

	/**
	 * Create a new {@link ImageInfo} instance populated with the information
	 * in the parameters.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes.
	 * @param bitDepth the number of bits per color model component for this
	 *                 image.
	 * @param numComponents the number of color model components for this
	 *                      image.
     * @param colorSpace the {@link ColorSpace} of this image.
	 * @param colorSpaceType the {@link ColorSpaceType} of this image. Only
	 *                       used if {@code ColorSpace} is {@code null}.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param imageIOSupport whether or not {@link ImageIO} can read/parse this
	 *                       image.
	 */
	public ImageInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		Metadata metadata,
		boolean imageIOSupport
	) {
		this.width = width;
		this.height = height;
		this.format = format;
		this.size = size;
		this.bitDepth = bitDepth;
		this.numComponents = numComponents;
		this.colorSpace = colorSpace;
		this.colorSpaceType = colorSpace != null ? ColorSpaceType.toColorSpaceType(colorSpace.getType()) : colorSpaceType;
		this.metadata = metadata;
		this.exifOrientation = ImageInfo.parseExifOrientation(metadata);
		this.imageIOSupport = imageIOSupport;
	}

	/**
	 * Tries to create an {@link ImageInfo} instance from {@link Metadata}.
	 * {@link ImageFormat} and size in bytes must be specified.
	 * If {@code metadata} is null or can't be parsed, an instance with invalid
	 * values is created or an {@link MetadataException} is thrown depending on
	 * {@code throwOnParseFailure}. The {@link ColorModel} in this instance
	 * will be {@code null}. This constructor should only be used if
	 * {@link ImageIO} can't parse the source. Instances created with this
	 * constructor will have {@code isImageIOSupport()} set to false.
	 *
	 * @param metadata the {@link Metadata} describing the image.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes.
	 * @param throwOnParseFailure if a {@link MetadataException} should be thrown
	 *                            instead of returning an instance with invalid
	 *                            resolution if parsing of resolution fails.
	 * @throws MetadataException if a parsing error occurs or parsing fails.
	 */
	public ImageInfo(Metadata metadata, ImageFormat format, long size, boolean throwOnParseFailure) throws MetadataException {
		this(-1, -1, metadata, format, size, throwOnParseFailure);
	}

	/**
	 * Tries to create an {@link ImageInfo} instance from {@link Metadata}.
	 * {@link ImageFormat} and size in bytes must be specified.
	 * If {@code metadata} is null or can't be parsed, an instance with invalid
	 * values is created or an {@link MetadataException} is thrown depending on
	 * {@code throwOnParseFailure}. The {@link ColorModel} in this instance
	 * will be {@code null}. This constructor should only be used if
	 * {@link ImageIO} can't parse the source. Instances created with this
	 * constructor will have {@code isImageIOSupport()} set to false.
	 *
	 * @param width the width of the image in pixels.
	 * @param height the height of the image in pixels.
	 * @param metadata the {@link Metadata} describing the image.
	 * @param format the {@link ImageFormat} for the image.
	 * @param size the size of the image in bytes.
	 * @param throwOnParseFailure if a {@link MetadataException} should be thrown
	 *                            instead of returning an instance with invalid
	 *                            resolution if parsing of resolution fails.
	 * @throws MetadataException if a parsing error occurs or parsing fails.
	 */
	public ImageInfo(int width, int height, Metadata metadata, ImageFormat format, long size, boolean throwOnParseFailure) throws MetadataException {
		int bitDepth = -1;
		int numComponents = -1;
		ColorSpaceType colorSpaceType = null;
		int orientation = 0;
		if (metadata != null) {
			for (Directory directory : metadata.getDirectories()) {
				if (directory instanceof PngDirectory) {
					if (
						((PngDirectory) directory).containsTag(PngDirectory.TAG_IMAGE_WIDTH) &&
						((PngDirectory) directory).containsTag(PngDirectory.TAG_IMAGE_HEIGHT)
					) {
						width = ((PngDirectory) directory).getInt(PngDirectory.TAG_IMAGE_WIDTH);
						height = ((PngDirectory) directory).getInt(PngDirectory.TAG_IMAGE_HEIGHT);
					}
					if (((PngDirectory) directory).containsTag(PngDirectory.TAG_BITS_PER_SAMPLE)) {
						bitDepth = ((PngDirectory) directory).getInt(PngDirectory.TAG_BITS_PER_SAMPLE);
					}
					if (((PngDirectory) directory).containsTag(PngDirectory.TAG_COLOR_TYPE)) {
						switch (((PngDirectory) directory).getInt(PngDirectory.TAG_COLOR_TYPE)) {
							case 0: // Grayscale without alpha
								numComponents = 1;
								colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case 2: // RGB without alpha
								numComponents = 3;
								colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case 3: // Palette index
								bitDepth = 8;
								numComponents = 3;
								colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case 4: // Grayscale with alpha
								numComponents = 2;
								colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case 6: // RGB with alpha
								numComponents = 4;
								colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							default:
						}
					}
				} else if (directory instanceof JpegDirectory) {
					if (
						((JpegDirectory) directory).containsTag(JpegDirectory.TAG_IMAGE_WIDTH) &&
						((JpegDirectory) directory).containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)
					) {
						width = ((JpegDirectory) directory).getInt(JpegDirectory.TAG_IMAGE_WIDTH);
						height = ((JpegDirectory) directory).getInt(JpegDirectory.TAG_IMAGE_HEIGHT);
					}
					if (((JpegDirectory) directory).containsTag(JpegDirectory.TAG_DATA_PRECISION)) {
						bitDepth = ((JpegDirectory) directory).getInt(JpegDirectory.TAG_DATA_PRECISION);
					}
					if (((JpegDirectory) directory).containsTag(JpegDirectory.TAG_NUMBER_OF_COMPONENTS)) {
						numComponents = ((JpegDirectory) directory).getInt(JpegDirectory.TAG_NUMBER_OF_COMPONENTS);
						/*
						 * This is "qualified" guessing based on the following assumption:
					     * Usually 1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK
					     *
					     * YCbCr/YIQ will be translated into RGB when read by ImageIO,
					     * and is therefore treated as RGB here.
					     */
						switch (numComponents) {
							case 1:
								colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case 3:
								colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case 4:
								colorSpaceType = ColorSpaceType.TYPE_CMYK;
								break;
							default:
						}
					}
				} else if (directory instanceof ExifIFD0Directory || directory instanceof ExifSubIFDDirectory) {
					if (
						((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_IMAGE_WIDTH) &&
						((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_IMAGE_HEIGHT)
					) {
						width = ((ExifDirectoryBase) directory).getInt(ExifDirectoryBase.TAG_IMAGE_WIDTH);
						height = ((ExifDirectoryBase) directory).getInt(ExifDirectoryBase.TAG_IMAGE_HEIGHT);
					}
					if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
						orientation = ((ExifDirectoryBase) directory).getInt(ExifDirectoryBase.TAG_ORIENTATION);
					}
					if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_BITS_PER_SAMPLE)) {
						bitDepth = ((ExifDirectoryBase) directory).getByteArray(ExifDirectoryBase.TAG_BITS_PER_SAMPLE)[0];
					}
					if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_SAMPLES_PER_PIXEL)) {
						numComponents = ((ExifDirectoryBase) directory).getInt(ExifDirectoryBase.TAG_SAMPLES_PER_PIXEL);
					}
					if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_PHOTOMETRIC_INTERPRETATION)) {
						/*
						 * This is "qualified" guessing based on the following assumption:
					     * 0 = WhiteIsZero
					     * 1 = BlackIsZero
					     * 2 = RGB
					     * 3 = RGB Palette
					     * 4 = Transparency Mask
					     * 5 = CMYK
					     * 6 = YCbCr
					     * 8 = CIELab
					     * 9 = ICCLab
					     * 10 = ITULab
					     * 32803 = Color Filter Array
					     * 32844 = Pixar LogL
					     * 32845 = Pixar LogLuv
					     * 34892 = Linear Raw
					     *
					     * YCbCr/YIQ will be translated into RGB when read by ImageIO,
					     * and is therefore treated as RGB here.
					     */
						switch (((ExifDirectoryBase) directory).getInt(ExifDirectoryBase.TAG_PHOTOMETRIC_INTERPRETATION)) {
							case 2:
							case 3:
							case 6:
								colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case 5:
								colorSpaceType = ColorSpaceType.TYPE_CMYK;
								break;
							case 8:
							case 9:
							case 10:
								colorSpaceType = ColorSpaceType.TYPE_Lab;
								break;
							case 32845:
								colorSpaceType = ColorSpaceType.TYPE_Luv;
								break;
							default:
						}
					}
				} else if (directory instanceof GifHeaderDirectory) {
					if (
						((GifHeaderDirectory) directory).containsTag(GifHeaderDirectory.TAG_IMAGE_WIDTH) &&
						((GifHeaderDirectory) directory).containsTag(GifHeaderDirectory.TAG_IMAGE_HEIGHT)
					) {
						width = ((GifHeaderDirectory) directory).getInt(GifHeaderDirectory.TAG_IMAGE_WIDTH);
						height = ((GifHeaderDirectory) directory).getInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT);
					}
					if (((GifHeaderDirectory) directory).containsTag(GifHeaderDirectory.TAG_BITS_PER_PIXEL)) {
						numComponents = 3;
						colorSpaceType = ColorSpaceType.TYPE_RGB;
						bitDepth = ((GifHeaderDirectory) directory).getInt(GifHeaderDirectory.TAG_BITS_PER_PIXEL) / 3;
					}
				} else if (directory instanceof BmpHeaderDirectory) {
					if (
						((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_IMAGE_WIDTH) &&
						((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_IMAGE_HEIGHT)
					) {
						width = ((BmpHeaderDirectory) directory).getInt(BmpHeaderDirectory.TAG_IMAGE_WIDTH);
						height = ((BmpHeaderDirectory) directory).getInt(BmpHeaderDirectory.TAG_IMAGE_HEIGHT);
					}
					if (
						((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_BITS_PER_PIXEL) &&
						((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_COMPRESSION)
					) {
						/* Only parsing the "standard" RGB compression type,
						 * anyone willing to figure out how to parse the other
						 * types are very welcome to do so.
						 */
						if (((BmpHeaderDirectory) directory).getInt(BmpHeaderDirectory.TAG_COMPRESSION) == 0) {
							numComponents = 3;
							colorSpaceType = ColorSpaceType.TYPE_RGB;
							bitDepth = ((BmpHeaderDirectory) directory).getInt(BmpHeaderDirectory.TAG_BITS_PER_PIXEL) / 3;
						}
					}
				} else if (directory instanceof IcoDirectory) {
					if (
						((IcoDirectory) directory).containsTag(IcoDirectory.TAG_IMAGE_WIDTH) &&
						((IcoDirectory) directory).containsTag(IcoDirectory.TAG_IMAGE_HEIGHT)
					) {
						width = ((IcoDirectory) directory).getInt(IcoDirectory.TAG_IMAGE_WIDTH);
						height = ((IcoDirectory) directory).getInt(IcoDirectory.TAG_IMAGE_HEIGHT);
					}
				} else if (directory instanceof JfifDirectory) {
					if (
						((JfifDirectory) directory).containsTag(JfifDirectory.TAG_RESX) &&
						((JfifDirectory) directory).containsTag(JfifDirectory.TAG_RESY)
					) {
						width = ((JfifDirectory) directory).getInt(JfifDirectory.TAG_RESX);
						height = ((JfifDirectory) directory).getInt(JfifDirectory.TAG_RESY);
					}
				} else if (directory instanceof PcxDirectory) {
					if (
						((PcxDirectory) directory).containsTag(PcxDirectory.TAG_XMIN) &&
						((PcxDirectory) directory).containsTag(PcxDirectory.TAG_XMAX) &&
						((PcxDirectory) directory).containsTag(PcxDirectory.TAG_YMIN) &&
						((PcxDirectory) directory).containsTag(PcxDirectory.TAG_YMAX)
					) {
						width = ((PcxDirectory) directory).getInt(PcxDirectory.TAG_XMAX) - ((PcxDirectory) directory).getInt(PcxDirectory.TAG_XMIN) + 1;
						height = ((PcxDirectory) directory).getInt(PcxDirectory.TAG_YMAX) - ((PcxDirectory) directory).getInt(PcxDirectory.TAG_YMIN) + 1;
					}
				} else if (directory instanceof PsdHeaderDirectory) {
					if (
						((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_IMAGE_WIDTH) &&
						((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_IMAGE_HEIGHT)
					) {
						width = ((PsdHeaderDirectory) directory).getInt(PsdHeaderDirectory.TAG_IMAGE_WIDTH);
						height = ((PsdHeaderDirectory) directory).getInt(PsdHeaderDirectory.TAG_IMAGE_HEIGHT);
					}
					if (((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL)) {
						bitDepth = ((PsdHeaderDirectory) directory).getInt(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL);
					}
					if (((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_COLOR_MODE)) {
						/*
					     * Bitmap = 0; Grayscale = 1; Indexed = 2; RGB = 3; CMYK = 4; Multichannel = 7; Duotone = 8; Lab = 9.
						 */
						switch (((PsdHeaderDirectory) directory).getInt(PsdHeaderDirectory.TAG_COLOR_MODE)) {
							case 1:
								numComponents = 1;
								colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case 2:
							case 3:
								numComponents = 3;
								colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case 4:
								numComponents = 4;
								colorSpaceType = ColorSpaceType.TYPE_CMYK;
								break;
							default:
						}
					}

				} else if (directory instanceof WebpDirectory) {
					if (
						((WebpDirectory) directory).containsTag(WebpDirectory.TAG_IMAGE_WIDTH) &&
						((WebpDirectory) directory).containsTag(WebpDirectory.TAG_IMAGE_HEIGHT)
					) {
						width = ((WebpDirectory) directory).getInt(WebpDirectory.TAG_IMAGE_WIDTH);
						height = ((WebpDirectory) directory).getInt(WebpDirectory.TAG_IMAGE_HEIGHT);
					}
				}
			}
		}
		if (throwOnParseFailure && (width < 0 || height < 0)) {
			throw new MetadataException("Failed to parse image resolution from metadata");
		}
		this.width = width;
		this.height = height;
		this.format = format;
		this.size = size;
		this.bitDepth = bitDepth;
		this.numComponents = numComponents;
		this.colorSpace = null;
		this.colorSpaceType = colorSpaceType;
		this.metadata = metadata;
		this.exifOrientation = orientation;
		this.imageIOSupport = false;
	}

	/**
	 * Tries to parse Exif orientation from the given metadata. If it fails in
	 * any way, {@code 0} (Normal) is returned.
	 *
	 * @param metadata the {@link Metadata} to parse.
	 * @return The Exif orientation value or {@code 0}.
	 */
	public static int parseExifOrientation(Metadata metadata) {
		if (metadata == null) {
			return 0;
		}
		try {
			for (Directory directory : metadata.getDirectories()) {
				if (directory instanceof ExifIFD0Directory) {
					if (((ExifIFD0Directory) directory).containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
						return ((ExifIFD0Directory) directory).getInt(ExifIFD0Directory.TAG_ORIENTATION);
					}
				}
			}
		} catch (MetadataException e) {
			return 0;
		}
		return 0;
	}

	/**
	 * @return The image width or -1 if unknown.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return The image height or -1 if unknown.
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
	 * @return The number of bits per pixel or -1 if unknown.
	 */
	public int getBitsPerPixel() {
		return bitDepth < 0 || numComponents < 0 ? -1 : bitDepth * numComponents;
	}

	/**
	 * The number of components describe how many "channels" the color model
	 * has. A grayscale image without alpha has 1, a RGB image without alpha
	 * has 3, a RGB image with alpha has 4 etc.
	 *
	 * @return The number of components in the {@link ColorModel} or -1 if
	 *         unknown.
	 */
	public int getNumComponents() {
		return numComponents;
	}

	/**
	 * @return The number of bits per color "channel" or -1 if unknown.
	 *
	 * @see #getBitPerPixel()
	 * @see #getNumColorComponents()
	 */
	public int getBitDepth() {
		return bitDepth;
	}

	/**
	 * @return The {@link Metadata} or {@code null} if unknown.
	 */
	public Metadata getMetadata() {
		return metadata;
	}

	/**
	 * @return The Exif orientation if any, or {@code 0} if unknown.
	 */
	public int getExifOrientation() {
		return exifOrientation;
	}

	/**
	 * @return Whether or not {@link ImageIO} can read/parse this image.
	 */
	public boolean isImageIOSupported() {
		return imageIOSupport;
	}

	/**
	 * {@link Metadata} isn't {@link Cloneable} or otherwise easily copied,
	 * so an instance of the {@link Metadata} instance to use for the copy must
	 * be provided.
	 *
	 * @param metadata the {@link Metadata} instance for the new
	 *                 {@link ImageInfo} instance.
	 * @return A copy of this {@link ImageInfo} instance.
	 */
	public ImageInfo copy(Metadata metadata) {
		return new ImageInfo(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			metadata,
			imageIOSupport
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(80);
		sb.append("ImageInfo: Format = ").append(format)
		.append(", Width = ").append(width)
		.append(", Height = ").append(height)
		.append(", Size = ").append(size == SIZE_UNKNOWN ? "Unknown" : size);
		return sb.toString();
	}

}
