package net.pms.image;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import net.pms.util.ParseException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.photoshop.PsdHeaderDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class PSDInfo extends ImageInfo {

	protected final ColorMode colorMode;
	protected final Integer channelCount;

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, ColorModel, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected PSDInfo(
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

		colorMode = ((PSDParseInfo) parsedInfo).colorMode;
		channelCount = ((PSDParseInfo) parsedInfo).channelCount;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, int, int, ColorSpace, ColorSpaceType, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected PSDInfo(
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

		colorMode = ((PSDParseInfo) parsedInfo).colorMode;
		channelCount = ((PSDParseInfo) parsedInfo).channelCount;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, Metadata, ImageFormat, long, boolean, boolean)}
	 * to instantiate.
	 */
	protected PSDInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		super(width, height, metadata, format, size, applyExifOrientation, throwOnParseFailure);

		colorMode = ((PSDParseInfo) parsedInfo).colorMode;
		channelCount = ((PSDParseInfo) parsedInfo).channelCount;
	}

	/**
	 * Copy constructor
	 */
	protected PSDInfo(
		int width,
		int height,
		ImageFormat format,
		long size,
		int bitDepth,
		int numComponents,
		ColorSpace colorSpace,
		ColorSpaceType colorSpaceType,
		boolean imageIOSupport,
		ColorMode colorMode,
		Integer channelCount
	) {
		super(width, height, format, size, bitDepth, numComponents, colorSpace, colorSpaceType, imageIOSupport);
		this.colorMode = colorMode;
		this.channelCount = channelCount;
	}

	@Override
	protected ParseInfo createParseInfo() {
		return new PSDParseInfo();
	}

	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	@Override
	protected void parseMetadata(Metadata metadata) {
		if (metadata == null) {
			return;
		}

		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof PsdHeaderDirectory) {
				parsedInfo.format = ImageFormat.PSD;
				if (
					((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_IMAGE_WIDTH) &&
					((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_IMAGE_HEIGHT)
				) {
					parsedInfo.width = ((PsdHeaderDirectory) directory).getInteger(PsdHeaderDirectory.TAG_IMAGE_WIDTH);
					parsedInfo.height = ((PsdHeaderDirectory) directory).getInteger(PsdHeaderDirectory.TAG_IMAGE_HEIGHT);
				}
				if (((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL)) {
					Integer i = ((PsdHeaderDirectory) directory).getInteger(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL);
					if (i != null) {
						parsedInfo.bitDepth = i;
					}
				}
				if (((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_COLOR_MODE)) {
					Integer i = ((PsdHeaderDirectory) directory).getInteger(PsdHeaderDirectory.TAG_COLOR_MODE);
					if (i != null) {
						((PSDParseInfo) parsedInfo).colorMode = ColorMode.typeOf(i.intValue());
						/*
					     * Bitmap = 0; Grayscale = 1; Indexed = 2; RGB = 3; CMYK = 4; Multichannel = 7; Duotone = 8; Lab = 9.
						 */
						switch (((PSDParseInfo) parsedInfo).colorMode) {
							case GRAYSCALE:
								parsedInfo.numComponents = 1;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case INDEXED:
							case RGB:
								parsedInfo.numComponents = 3;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_RGB;
								break;
							case CMYK:
								parsedInfo.numComponents = 4;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_CMYK;
								break;
							case LAB:
								parsedInfo.numComponents = 3;
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_Lab;
							default:
						}
					}
				}
				if (((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_CHANNEL_COUNT)) {
					Integer i = ((PsdHeaderDirectory) directory).getInteger(PsdHeaderDirectory.TAG_CHANNEL_COUNT);
					if (i != null) {
						((PSDParseInfo) parsedInfo).channelCount = i;
					}
				}
			}
		}
	}


	/**
	 * @return The {@link ColorMode} or {@code null} if unknown.
	 */
	public ColorMode getColorMode() {
		return colorMode;
	}


	/**
	 * The number of channels in the image, including any alpha channels.
	 * Supported range is 1 to 56.
	 *
	 * @return The number of channels in the image or {@code null} if unknown.
	 */
	public Integer getChannelCount() {
		return channelCount;
	}

	@Override
	public PSDInfo copy() {
		return new PSDInfo(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			imageIOSupport,
			colorMode,
			channelCount
		);
	}

	public enum ColorMode {
		BITMAP,
		GRAYSCALE,
		INDEXED,
		RGB,
		CMYK,
		MULTI_CHANNEL,
		DUOTONE,
		LAB;

		public static ColorMode typeOf(int value) {
			switch (value) {
				case 0:
					return BITMAP;
				case 1:
					return GRAYSCALE;
				case 2:
					return INDEXED;
				case 3:
					return RGB;
				case 4:
					return CMYK;
				case 7:
					return MULTI_CHANNEL;
				case 8:
					return DUOTONE;
				case 9:
					return LAB;
				default:
					return null;
			}
		}
	}

	protected static class PSDParseInfo extends ParseInfo {
		ColorMode colorMode;
		Integer channelCount;
	}

	@Override
	protected void buildToString(StringBuilder sb) {
		if (colorMode != null) {
			sb.append(", Color Mode = ").append(colorMode);
		}
		if (channelCount != null) {
			sb.append(", Channel Count = ").append(channelCount);
		}
	}
}
