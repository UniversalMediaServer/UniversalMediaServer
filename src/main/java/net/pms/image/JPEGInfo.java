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
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jpeg.HuffmanTablesDirectory;
import com.drew.metadata.jpeg.JpegComponent;
import com.drew.metadata.jpeg.JpegDirectory;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.util.ParseException;

public class JPEGInfo extends ExifInfo {
	protected final Map<Integer, JpegComponent> components;
	protected final Integer jfifVersion;
	protected final CompressionType compressionType;
	protected final Boolean isTypicalHuffman;
	protected final JPEGSubsamplingNotation chromaSubsampling;

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, ColorModel, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected JPEGInfo(
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

		jfifVersion = ((JPEGParseInfo) parsedInfo).jfifVersion;
		compressionType = ((JPEGParseInfo) parsedInfo).compressionType;
		components = ((JPEGParseInfo) parsedInfo).components;
		isTypicalHuffman = ((JPEGParseInfo) parsedInfo).isTypicalHuffman;
		chromaSubsampling = ((JPEGParseInfo) parsedInfo).chromaSubsampling;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, ImageFormat, long, int, int, ColorSpace, ColorSpaceType, Metadata, boolean, boolean)}
	 * to instantiate.
	 */
	protected JPEGInfo(
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

		jfifVersion = ((JPEGParseInfo) parsedInfo).jfifVersion;
		compressionType = ((JPEGParseInfo) parsedInfo).compressionType;
		components = ((JPEGParseInfo) parsedInfo).components;
		isTypicalHuffman = ((JPEGParseInfo) parsedInfo).isTypicalHuffman;
		chromaSubsampling = ((JPEGParseInfo) parsedInfo).chromaSubsampling;
	}

	/**
	 * Use
	 * {@link ImageInfo#create(int, int, Metadata, ImageFormat, long, boolean, boolean)}
	 * to instantiate.
	 */
	protected JPEGInfo(
		int width,
		int height,
		Metadata metadata,
		ImageFormat format,
		long size,
		boolean applyExifOrientation,
		boolean throwOnParseFailure
	) throws ParseException {
		super(width, height, metadata, format, size, applyExifOrientation, throwOnParseFailure);

		jfifVersion = ((JPEGParseInfo) parsedInfo).jfifVersion;
		compressionType = ((JPEGParseInfo) parsedInfo).compressionType;
		components = ((JPEGParseInfo) parsedInfo).components;
		isTypicalHuffman = ((JPEGParseInfo) parsedInfo).isTypicalHuffman;
		chromaSubsampling = ((JPEGParseInfo) parsedInfo).chromaSubsampling;
	}

	/**
	 * Copy constructor
	 */
	protected JPEGInfo(
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
		Integer jfifVersion,
		ExifColorSpace exifColorSpace,
		CompressionType compressionType,
		Map<Integer, JpegComponent> components,
		boolean hasExifThumbnail,
		boolean isTypicalHuffman,
		JPEGSubsamplingNotation chromaSubsampling
	) {
		super(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			imageIOSupport,
			photometricInterpretation,
			exifOrientation,
			originalExifOrientation,
			exifVersion,
			exifCompression,
			exifColorSpace,
			hasExifThumbnail
		);
		this.jfifVersion = jfifVersion;
		this.compressionType = compressionType;
		this.components = components;
		this.isTypicalHuffman = isTypicalHuffman;
		this.chromaSubsampling = chromaSubsampling;
	}

	@Override
	protected ParseInfo createParseInfo() {
		return new JPEGParseInfo();
	}

	@Override
	protected void parseMetadata(Metadata metadata) {
		if (metadata == null) {
			return;
		}
		super.parseMetadata(metadata);

		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof JpegDirectory jpegDirectory) {
				parsedInfo.format = ImageFormat.JPEG;
				if (
					jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_WIDTH) &&
					jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)
				) {
					parsedInfo.width = jpegDirectory.getInteger(JpegDirectory.TAG_IMAGE_WIDTH);
					parsedInfo.height = jpegDirectory.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT);
				}
				if (jpegDirectory.containsTag(JpegDirectory.TAG_DATA_PRECISION)) {
					Integer i = jpegDirectory.getInteger(JpegDirectory.TAG_DATA_PRECISION);
					if (i != null) {
						parsedInfo.bitDepth = i;
					}
				}
				if (jpegDirectory.containsTag(JpegDirectory.TAG_NUMBER_OF_COMPONENTS)) {
					Integer i = jpegDirectory.getInteger(JpegDirectory.TAG_NUMBER_OF_COMPONENTS);
					if (i != null) {
						parsedInfo.numComponents = i;
						/*
						 * This is "qualified" guessing based on the following assumption:
					     * Usually 1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK
					     *
					     */
						switch (parsedInfo.numComponents) {
							case 1:
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_GRAY;
								break;
							case 3:
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_YCbCr;
								break;
							case 4:
								parsedInfo.colorSpaceType = ColorSpaceType.TYPE_CMYK;
								break;
							default:
						}

						((JPEGParseInfo) parsedInfo).components.clear();
						for (int j = 0; j < parsedInfo.numComponents; j++) {
							((JPEGParseInfo) parsedInfo).components.put(j, jpegDirectory.getComponent(j));
						}
					}
				}
				if (jpegDirectory.containsTag(JpegDirectory.TAG_COMPRESSION_TYPE)) {
					Integer i = jpegDirectory.getInteger(JpegDirectory.TAG_COMPRESSION_TYPE);
					if (i != null) {
						((JPEGParseInfo) parsedInfo).compressionType = CompressionType.typeOf(i);
					}
				}
				((JPEGParseInfo) parsedInfo).chromaSubsampling =
					JPEGSubsamplingNotation.calculateJPEGSubsampling(jpegDirectory);
			} else if (directory instanceof JfifDirectory jfifDirectory) {
				if (jfifDirectory.containsTag(JfifDirectory.TAG_VERSION)) {
					Integer i = jfifDirectory.getInteger(JfifDirectory.TAG_VERSION);
					if (i != null) {
						((JPEGParseInfo) parsedInfo).jfifVersion = i;
					}
				}
			} else if (directory instanceof HuffmanTablesDirectory huffmanTablesDirectory) {
				((JPEGParseInfo) parsedInfo).isTypicalHuffman = huffmanTablesDirectory.isTypical();
			}
		}
	}

	/**
	 * @return The {@link Map} of {@link JpegComponent}s.
	 */
	public Map<Integer, JpegComponent> getComponents() {
		return Collections.unmodifiableMap(components);
	}

	/**
	 *
	 * @param componentIndex the (zero-based) component index.
	 *
	 * @return The {@link JpegComponent} or {@code null} if it doesn't exist.
	 *
	 * @see #getNumComponents()
	 */
	public JpegComponent getComponent(int componentIndex) {
		return components.get(componentIndex);
	}

	/**
	 * @return The JFIF version multiplied with 100 or {@link ImageInfo#UNKNOWN}
	 *         if unknown.
	 */
	public int getJFIFVersion() {
		return jfifVersion != null ? jfifVersion : UNKNOWN;
	}

	/**
	 * @return The {@link CompressionType}.
	 */
	public CompressionType getCompressionType() {
		return compressionType;
	}

	/**
	 * @return Whether or not the Huffman tables of this JPEG are the "typical"
	 *         tables defined in K.3 - K.3 in the JPEG standard.
	 */
	public boolean isTypicalHuffman() {
		return isTypicalHuffman != null && isTypicalHuffman;
	}

	/**
	 * @return The {@link JPEGSubsamplingNotation}.
	 */
	public JPEGSubsamplingNotation getChromaSubsampling() {
		return chromaSubsampling;
	}

	@Override
	public JPEGInfo copy() {
		return new JPEGInfo(
			width,
			height,
			format,
			size,
			bitDepth,
			numComponents,
			colorSpace,
			colorSpaceType,
			imageIOSupport,
			photometricInterpretation,
			exifOrientation,
			originalExifOrientation,
			exifVersion,
			exifCompression,
			jfifVersion,
			exifColorSpace,
			compressionType,
			components,
			hasExifThumbnail,
			isTypicalHuffman,
			chromaSubsampling
		);
	}

	public enum CompressionType {
		BASELINE_HUFFMAN,
		EXTENDED_SEQUENTIAL_HUFFMAN,
		PROGRESSIVE_HUFFMAN,
		LOSSLESS_HUFFMAN,
		DIFFERENTIAL_SEQUENTIAL_HUFFMAN,
		DIFFERENTIAL_PROGRESSIVE_HUFFMAN,
		DIFFERENTIAL_LOSSLESS_HUFFMAN,
		RESERVED,
		EXTENDED_SEQUENTIAL_ARITHMETIC,
		PROGRESSIVE_ARITHMETIC,
		LOSSLESS_ARITHMETIC,
		DIFFERENTIAL_SEQUENTIAL_ARITHMETIC,
		DIFFERENTIAL_PROGRESSIVE_ARITHMETIC,
		DIFFERENTIAL_LOSSLESS_ARITHMETIC;

		public static CompressionType typeOf(int value) {
			return switch (value) {
				case 0 -> BASELINE_HUFFMAN;
				case 1 -> EXTENDED_SEQUENTIAL_HUFFMAN;
				case 2 -> PROGRESSIVE_HUFFMAN;
				case 3 -> LOSSLESS_HUFFMAN;
				case 5 -> DIFFERENTIAL_SEQUENTIAL_HUFFMAN;
				case 6 -> DIFFERENTIAL_PROGRESSIVE_HUFFMAN;
				case 7 -> DIFFERENTIAL_LOSSLESS_HUFFMAN;
				case 8 -> RESERVED;
				case 9 -> EXTENDED_SEQUENTIAL_ARITHMETIC;
				case 10 -> PROGRESSIVE_ARITHMETIC;
				case 11 -> LOSSLESS_ARITHMETIC;
				case 13 -> DIFFERENTIAL_SEQUENTIAL_ARITHMETIC;
				case 14 -> DIFFERENTIAL_PROGRESSIVE_ARITHMETIC;
				case 15 -> DIFFERENTIAL_LOSSLESS_ARITHMETIC;
				default -> null;
			};
		}

		public boolean isHuffman() {
			return switch (this) {
				case BASELINE_HUFFMAN, DIFFERENTIAL_LOSSLESS_HUFFMAN, DIFFERENTIAL_PROGRESSIVE_HUFFMAN, DIFFERENTIAL_SEQUENTIAL_HUFFMAN, EXTENDED_SEQUENTIAL_HUFFMAN, LOSSLESS_HUFFMAN, PROGRESSIVE_HUFFMAN -> true;
				default -> false;
			};
		}
	}

	protected static class JPEGParseInfo extends ExifParseInfo {
		Integer jfifVersion;
		CompressionType compressionType;
		Map<Integer, JpegComponent> components = new HashMap<>(4);
		Boolean isTypicalHuffman;
		JPEGSubsamplingNotation chromaSubsampling;
	}

	@Override
	protected void buildToString(StringBuilder sb) {
		super.buildToString(sb);
		if (jfifVersion != null) {
			sb.append(", JFIF Version = ").append(Integer.toHexString(jfifVersion));
		}
		if (compressionType != null) {
			sb.append(", Compression Type = ").append(compressionType);
		}
		if (isTypicalHuffman != null) {
			sb.append(", Typical Huffman = ").append(isTypicalHuffman ? "True" : "False");
		}
		if (chromaSubsampling != null) {
			sb.append(", Chroma Subsampling = ").append(chromaSubsampling);
		}
		if (components != null) {
			sb.append(", Components: [");
			boolean first = true;
			for (Entry<Integer, JpegComponent> component : components.entrySet()) {
				if (!first) {
					sb.append(", ");
				}
				sb.append(component.getKey()).append(" (").append(component.getValue().getComponentName()).append("): ")
					.append(component.getValue().getHorizontalSamplingFactor()).append(" x ")
					.append(component.getValue().getVerticalSamplingFactor());
				first = false;
			}
			sb.append("]");
		}
	}
}
