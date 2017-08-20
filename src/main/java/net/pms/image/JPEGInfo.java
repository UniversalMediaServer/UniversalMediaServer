package net.pms.image;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.util.ParseException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jpeg.HuffmanTablesDirectory;
import com.drew.metadata.jpeg.JpegComponent;
import com.drew.metadata.jpeg.JpegDirectory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
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
			if (directory instanceof JpegDirectory) {
				parsedInfo.format = ImageFormat.JPEG;
				if (
					((JpegDirectory) directory).containsTag(JpegDirectory.TAG_IMAGE_WIDTH) &&
					((JpegDirectory) directory).containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)
				) {
					parsedInfo.width = ((JpegDirectory) directory).getInteger(JpegDirectory.TAG_IMAGE_WIDTH);
					parsedInfo.height = ((JpegDirectory) directory).getInteger(JpegDirectory.TAG_IMAGE_HEIGHT);
				}
				if (((JpegDirectory) directory).containsTag(JpegDirectory.TAG_DATA_PRECISION)) {
					Integer i = ((JpegDirectory) directory).getInteger(JpegDirectory.TAG_DATA_PRECISION);
					if (i != null) {
						parsedInfo.bitDepth = i;
					}
				}
				if (((JpegDirectory) directory).containsTag(JpegDirectory.TAG_NUMBER_OF_COMPONENTS)) {
					Integer i = ((JpegDirectory) directory).getInteger(JpegDirectory.TAG_NUMBER_OF_COMPONENTS);
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
						for (int j = 0; j < parsedInfo.numComponents.intValue(); j++) {
							((JPEGParseInfo) parsedInfo).components.put(j, ((JpegDirectory) directory).getComponent(j));
						}
					}
				}
				if (((JpegDirectory) directory).containsTag(JpegDirectory.TAG_COMPRESSION_TYPE)) {
					Integer i = ((JpegDirectory) directory).getInteger(JpegDirectory.TAG_COMPRESSION_TYPE);
					if (i != null) {
						((JPEGParseInfo) parsedInfo).compressionType = CompressionType.typeOf(i.intValue());
					}
				}
				((JPEGParseInfo) parsedInfo).chromaSubsampling =
					JPEGSubsamplingNotation.calculateJPEGSubsampling((JpegDirectory) directory);
			} else if (directory instanceof JfifDirectory) {
				if (((JfifDirectory) directory).containsTag(JfifDirectory.TAG_VERSION)) {
					 Integer i = ((JfifDirectory) directory).getInteger(JfifDirectory.TAG_VERSION);
					 if (i != null) {
						 ((JPEGParseInfo) parsedInfo).jfifVersion = i;
					 }
				}
			} else if (directory instanceof HuffmanTablesDirectory){
				((JPEGParseInfo) parsedInfo).isTypicalHuffman =
					Boolean.valueOf(((HuffmanTablesDirectory) directory).isTypical());
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
		return jfifVersion != null ? jfifVersion.intValue() : UNKNOWN;
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
		return isTypicalHuffman != null ? isTypicalHuffman.booleanValue() : false;
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
			switch (value) {
				case 0:
					return BASELINE_HUFFMAN;
				case 1:
					return EXTENDED_SEQUENTIAL_HUFFMAN;
				case 2:
					return PROGRESSIVE_HUFFMAN;
				case 3:
					return LOSSLESS_HUFFMAN;
				case 5:
					return DIFFERENTIAL_SEQUENTIAL_HUFFMAN;
				case 6:
					return DIFFERENTIAL_PROGRESSIVE_HUFFMAN;
				case 7:
					return DIFFERENTIAL_LOSSLESS_HUFFMAN;
				case 8:
					return RESERVED;
				case 9:
					return EXTENDED_SEQUENTIAL_ARITHMETIC;
				case 10:
					return PROGRESSIVE_ARITHMETIC;
				case 11:
					return LOSSLESS_ARITHMETIC;
				case 13:
					return DIFFERENTIAL_SEQUENTIAL_ARITHMETIC;
				case 14:
					return DIFFERENTIAL_PROGRESSIVE_ARITHMETIC;
				case 15:
					return DIFFERENTIAL_LOSSLESS_ARITHMETIC;
				default:
					return null;
			}
		}

		public boolean isHuffman() {
			switch (this) {
				case BASELINE_HUFFMAN:
				case DIFFERENTIAL_LOSSLESS_HUFFMAN:
				case DIFFERENTIAL_PROGRESSIVE_HUFFMAN:
				case DIFFERENTIAL_SEQUENTIAL_HUFFMAN:
				case EXTENDED_SEQUENTIAL_HUFFMAN:
				case LOSSLESS_HUFFMAN:
				case PROGRESSIVE_HUFFMAN:
					return true;
				default:
					return false;
			}
		}
	}

	protected static class JPEGParseInfo extends ExifParseInfo {
		Integer jfifVersion;
		CompressionType compressionType;
		Map<Integer, JpegComponent> components = new HashMap<Integer, JpegComponent>(4);
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
			sb.append(", Typical Huffman = ").append(isTypicalHuffman.booleanValue() ? "True" : "False");
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
