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
import com.drew.metadata.pcx.PcxDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import net.pms.util.ParseException;

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
			if (directory instanceof PcxDirectory pcxDirectory) {
				parsedInfo.format = ImageFormat.PCX;
				if (
					pcxDirectory.containsTag(PcxDirectory.TAG_XMIN) &&
					pcxDirectory.containsTag(PcxDirectory.TAG_XMAX) &&
					pcxDirectory.containsTag(PcxDirectory.TAG_YMIN) &&
					pcxDirectory.containsTag(PcxDirectory.TAG_YMAX)
				) {
					Integer min = pcxDirectory.getInteger(PcxDirectory.TAG_XMIN);
					Integer max = pcxDirectory.getInteger(PcxDirectory.TAG_XMAX);
					if (min != null && max != null) {
						parsedInfo.width = max - min + 1;
					}
					min = pcxDirectory.getInteger(PcxDirectory.TAG_YMIN);
					max = pcxDirectory.getInteger(PcxDirectory.TAG_YMAX);
					if (min != null && max != null) {
						parsedInfo.height = max - min + 1;
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
