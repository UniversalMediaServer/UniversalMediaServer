package net.pms.image;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import net.pms.util.ParseException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.pcx.PcxDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class PCXInfo extends ImageInfo {

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, ColorModel, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected PCXInfo(
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
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, int, int, ColorSpace, ColorSpaceType, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected PCXInfo(
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
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, Metadata, ImageFormat, long, boolean, boolean)}
	 * to instantiate.
	 */
	protected PCXInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		super(width, height, metadata, format, size, applyExifOrientation, throwOnParseFailure);
	}

	/**
	 * Copy constructor
	 */
	protected PCXInfo(
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
		super(width, height, format, size, bitDepth, numComponents, colorSpace, colorSpaceType, imageIOSupport);
	}

	@Override
	protected ParseInfo createParseInfo() {
		return new ParseInfo();
	}

	@Override
	protected void parseMetadata(Metadata metadata) {
		if (metadata == null) {
			return;
		}

		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof PcxDirectory) {
				parsedInfo.format = ImageFormat.PCX;
				if (
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_XMIN) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_XMAX) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_YMIN) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_YMAX)
				) {
					Integer min = ((PcxDirectory) directory).getInteger(PcxDirectory.TAG_XMIN);
					Integer max = ((PcxDirectory) directory).getInteger(PcxDirectory.TAG_XMAX);
					if (min != null && max != null) {
						parsedInfo.width = max.intValue() - min.intValue() + 1;
					}
					min = ((PcxDirectory) directory).getInteger(PcxDirectory.TAG_YMIN);
					max = ((PcxDirectory) directory).getInteger(PcxDirectory.TAG_YMAX);
					if (min != null && max != null) {
						parsedInfo.height = max.intValue() - min.intValue() + 1;
					}
				}
			}
		}
	}

	@Override
	public PCXInfo copy() {
		return new PCXInfo(width, height, format, size, bitDepth, numComponents, colorSpace, colorSpaceType, imageIOSupport);
	}

	@Override
	protected void buildToString(StringBuilder sb) {
	}
}
