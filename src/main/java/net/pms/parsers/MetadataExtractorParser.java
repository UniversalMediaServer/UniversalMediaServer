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
package net.pms.parsers;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.bmp.BmpMetadataReader;
import com.drew.imaging.gif.GifMetadataReader;
import com.drew.imaging.ico.IcoMetadataReader;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.pcx.PcxMetadataReader;
import com.drew.imaging.png.PngMetadataReader;
import com.drew.imaging.psd.PsdMetadataReader;
import com.drew.imaging.raf.RafMetadataReader;
import com.drew.imaging.tiff.TiffMetadataReader;
import com.drew.imaging.webp.WebpMetadataReader;
import com.drew.lang.RandomAccessReader;
import com.drew.lang.RandomAccessStreamReader;
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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import net.pms.formats.Format;
import net.pms.image.ExifInfo;
import net.pms.image.ExifOrientation;
import net.pms.image.ImageFormat;
import net.pms.image.ImageIORuntimeException;
import net.pms.image.ImageIOTools;
import net.pms.image.ImageInfo;
import net.pms.media.MediaInfo;
import net.pms.util.ParseException;
import net.pms.util.ResettableInputStream;
import net.pms.util.UnknownFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataExtractorParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(JaudiotaggerParser.class);
	public static final String PARSER_NAME = "Drew";

	/**
	 * This class is not meant to be instantiated.
	 */
	private MetadataExtractorParser() {
	}

	/**
	 * Parses an image file and stores the results in the given
	 * {@link MediaInfo}. Parsing is performed using both
	 * <a href=https://github.com/drewnoakes/metadata-extractor>Metadata Extractor</a>
	 * and {@link ImageIO}. While Metadata Extractor offers more detailed
	 * information, {@link ImageIO} offers information that is convenient for
	 * image transformation with {@link ImageIO}. Parsing will be performed if
	 * just one of the two methods produces results, but some details will be
	 * missing if either one failed.
	 * <p><b>
	 * This method consumes and closes {@code inputStream}.
	 * </b>
	 * @param file the {@link File} to parse.
	 * @param media the {@link MediaInfo} instance to store the parsing
	 *              results to.
	 * @throws IOException if an IO error occurs or no information can be parsed.
	 *
	 */
	public static void parse(File file, MediaInfo media) throws IOException {
		final int maxBuffer = 1048576; // 1 MB
		if (file == null) {
			throw new IllegalArgumentException("parseImage: file cannot be null");
		}
		if (media == null) {
			throw new IllegalArgumentException("parseImage: media cannot be null");
		}

		boolean trace = LOGGER.isTraceEnabled();
		if (trace) {
			LOGGER.trace("Parsing image file \"{}\"", file.getAbsolutePath());
		}
		long size = file.length();
		ResettableInputStream inputStream = new ResettableInputStream(Files.newInputStream(file.toPath()), maxBuffer);
		try  {
			Metadata metadata;
			FileType fileType = null;
			try {
				fileType = FileTypeDetector.detectFileType(inputStream);
				metadata = getMetadata(inputStream, fileType);
			} catch (IOException e) {
				metadata = new Metadata();
				LOGGER.debug("Error reading \"{}\": {}", file.getAbsolutePath(), e.getMessage());
				LOGGER.trace("", e);
			} catch (ImageProcessingException e) {
				metadata = new Metadata();
				LOGGER.debug(
					"Error parsing {} metadata for \"{}\": {}",
					fileType != null ? fileType.toString().toUpperCase(Locale.ROOT) : "null",
					file.getAbsolutePath(),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			ImageFormat format = toImageFormat(fileType);
			if (format == null || format == ImageFormat.TIFF) {
				ImageFormat tmpformat = toImageFormat(metadata);
				if (tmpformat != null) {
					format = tmpformat;
				}
			}
			if (inputStream.isFullResetAvailable()) {
				inputStream.fullReset();
			} else {
				// If we can't reset it, close it and create a new
				inputStream.close();
				inputStream = new ResettableInputStream(Files.newInputStream(file.toPath()), maxBuffer);
			}
			ImageInfo imageInfo = null;
			try {
				imageInfo = readImageInfo(inputStream, size, metadata, false);
			} catch (UnknownFormatException | IIOException | ParseException e) {
				if (format == null) {
					throw new UnknownFormatException(
						"Unable to recognize image format for \"" + file.getAbsolutePath() + "\" - parsing failed",
						e
					);
				}
				LOGGER.debug(
					"Unable to parse \"{}\" with ImageIO because the format is unsupported, image information will be limited",
					file.getAbsolutePath()
				);
				LOGGER.trace("ImageIO parse failure reason: {}", e.getMessage());

				// Gather basic information from the data we have
				if (metadata != null) {
					try {
						imageInfo = ImageInfo.create(metadata, format, size, true, true);
					} catch (ParseException pe) {
						LOGGER.debug("Unable to parse metadata for \"{}\": {}", file.getAbsolutePath(), pe.getMessage());
						LOGGER.trace("", pe);
					}
				}
			}

			if (imageInfo == null && format == null) {
				throw new ParseException("Parsing of \"" + file.getAbsolutePath() + "\" failed");
			}

			if (format == null && imageInfo != null) {
				format = imageInfo.getFormat();
			} else if (imageInfo != null && imageInfo.getFormat() != null && format != null && format != imageInfo.getFormat()) {
				if (imageInfo.getFormat() == ImageFormat.TIFF && format.isRaw()) {
					if (format == ImageFormat.ARW && !isARW(metadata)) {
						// XXX Remove this if https://github.com/drewnoakes/metadata-extractor/issues/217 is fixed
						// Metadata extractor misidentifies some Photoshop created TIFFs for ARW, correct it
						format = toImageFormat(metadata);
						if (format == null) {
							format = ImageFormat.TIFF;
						}
						LOGGER.trace(
							"Correcting misidentified image format ARW to {} for \"{}\"",
							format,
							file.getAbsolutePath()
						);
					} else {
						/*
						 * ImageIO recognizes many RAW formats as TIFF because
						 * of their close relationship let's treat them as what
						 * they really are.
						 */
						imageInfo = ImageInfo.create(
							imageInfo.getWidth(),
							imageInfo.getHeight(),
							format,
							size,
							imageInfo.getBitDepth(),
							imageInfo.getNumComponents(),
							imageInfo.getColorSpace(),
							imageInfo.getColorSpaceType(),
							metadata,
							false,
							imageInfo.isImageIOSupported()
							);
						LOGGER.trace(
							"Correcting misidentified image format TIFF to {} for \"{}\"",
							format.toString(),
							file.getAbsolutePath()
						);
					}
				} else {
					LOGGER.debug(
						"Image parsing for \"{}\" was inconclusive, metadata parsing " +
						"detected {} format while ImageIO detected {}. Choosing {}.",
						file.getAbsolutePath(),
						format,
						imageInfo.getFormat(),
						imageInfo.getFormat()
					);
					format = imageInfo.getFormat();
				}
			}
			media.setImageInfo(imageInfo);
			media.setImageCount(1);
			if (format != null) {
				media.setContainer(format.toFormatConfiguration());
			}
			media.setSize(file.length());
			media.setMediaParser(PARSER_NAME);
			media.postParse(Format.IMAGE);
			if (trace) {
				LOGGER.trace("Parsing of image \"{}\" completed", file.getName());
			}
		} finally {
			inputStream.close();
		}
	}

	/**
	 * Tries to parse {@link ImageFormat} from a {@link Metadata} instance.
	 *
	 * @param metadata the {@link Metadata} to parse.
	 * @return The parsed {@link ImageFormat} or {@code null} if the parsing
	 *         fails.
	 */
	@SuppressWarnings("incomplete-switch")
	public static ImageFormat toImageFormat(Metadata metadata) {
		if (metadata == null) {
			throw new NullPointerException("metadata cannot be null");
		}

		// Check for known directories tied to a particular format
		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof BmpHeaderDirectory) {
				return ImageFormat.BMP;
			}
			if (directory instanceof GifHeaderDirectory) {
				return ImageFormat.GIF;
			}
			if (directory instanceof IcoDirectory) {
				return ImageFormat.ICO;
			}
			if (directory instanceof JfifDirectory || directory instanceof JpegDirectory) {
				return ImageFormat.JPEG;
			}
			if (directory instanceof PcxDirectory) {
				return ImageFormat.PCX;
			}
			if (directory instanceof PngDirectory) {
				return ImageFormat.PNG;
			}
			if (directory instanceof PsdHeaderDirectory) {
				return ImageFormat.PSD;
			}
			if (directory instanceof WebpDirectory) {
				return ImageFormat.WEBP;
			}
		}

		/*
		 * Check for Exif compression tag and parse known values to TIFF or
		 * various proprietary "raw" formats. Do not try to combine the two
		 * loops, Exif matching must only be attempted if the first match fails.
		 */
		ExifInfo.ExifCompression exifCompression = null;
		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof ExifIFD0Directory || directory instanceof ExifSubIFDDirectory) {
				if (((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_COMPRESSION)) {
					Integer i = ((ExifDirectoryBase) directory).getInteger(ExifDirectoryBase.TAG_COMPRESSION);
					if (i != null) {
						exifCompression = ExifInfo.ExifCompression.typeOf(i);
					}
				}
				if (directory.containsTag(ImageFormat.TAG_DNG_VERSION)) {
					return ImageFormat.DNG;
				}
			}
		}
		// Nothing found by specific tags, use a generic approach based on Exif compression
		if (exifCompression != null) {
			switch (exifCompression) {
				case ADOBE_DEFLATE, CCITT_1D, DEFLATE, IT8BL, IT8CTPAD, IT8LW, IT8MP, JBIG, JBIG2_TIFF_FX, JBIG_B_W, JBIG_COLOR, JPEG, JPEG_OLD_STYLE, LZW, PACKBITS, T4_GROUP_3_FAX, T6_GROUP_4_FAX, UNCOMPRESSED -> {
					return ImageFormat.TIFF;
				}
				case DCS, KODAC_DCR_COMPRESSED -> {
					return ImageFormat.DCR;
				}
				case KODAK_KDC_COMPRESSED -> {
					return ImageFormat.KDC;
				}
				case NIKON_NEF_COMPRESSED -> {
					return ImageFormat.NEF;
				}
				case PENTAX_PEF_COMPRESSED -> {
					return ImageFormat.PEF;
				}
				case SAMSUNG_SRW_COMPRESSED, SAMSUNG_SRW_COMPRESSED_2 -> {
					return ImageFormat.SRW;
				}
				case SONY_ARW_COMPRESSED -> {
					return ImageFormat.ARW;
				}
			}
		}

		// Parsing failed
		return null;
	}

	/**
	 * @return The image format for the given image file type.
	 */
	public static ImageFormat toImageFormat(FileType fileType) {
		if (fileType == null) {
			return null;
		}
		switch (fileType) {
			case Arw:
				return ImageFormat.ARW;
			case Bmp:
				return ImageFormat.BMP;
			case Cr2:
				return ImageFormat.CR2;
			case Crw:
				return ImageFormat.CRW;
			case Gif:
				return ImageFormat.GIF;
			case Ico:
				return ImageFormat.ICO;
			case Jpeg:
				return ImageFormat.JPEG;
			case Nef:
				return ImageFormat.NEF;
			case Orf:
				return ImageFormat.ORF;
			case Pcx:
				return ImageFormat.PCX;
			case Png:
				return ImageFormat.PNG;
			case Psd:
				return ImageFormat.PSD;
			case Raf:
				return ImageFormat.RAF;
			case Rw2:
				return ImageFormat.RW2;
			case Tiff:
				return ImageFormat.TIFF;
			case Riff:
			case WebP:
				return ImageFormat.WEBP;
			case Unknown:
			default:
				return null;
		}
	}

	/**
	 * There is a bug in Metadata Extractor that misidentifies some TIFF files
	 * as ARW files. This method is here to verify if such a misidentification
	 * has taken place or not.
	 *
	 * @param metadata the {@link Metadata} instance to evaluate.
	 * @return The result of the evaluation.
	 *
	 * XXX This method can be removed if https://github.com/drewnoakes/metadata-extractor/issues/217 is fixed
	 */
	public static boolean isARW(Metadata metadata) {
		if (metadata == null) {
			return false;
		}
		Collection<ExifSubIFDDirectory> directories = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);
		for (ExifSubIFDDirectory directory : directories) {
			if (
				directory.containsTag(ExifSubIFDDirectory.TAG_COMPRESSION) &&
				directory.getInteger(ExifSubIFDDirectory.TAG_COMPRESSION) != null &&
				directory.getInteger(ExifSubIFDDirectory.TAG_COMPRESSION) == 32767
			) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Reads image metadata for supported format.
	 *
	 * @param bytes the image for which to read metadata.
	 * @param format the {@link ImageFormat} of the image.
	 * @return The {@link Metadata} or {@code null} if {@code bytes} is
	 *         {@code null}.
	 * @throws ImageProcessingException If the image cannot be processed.
	 * @throws IOException If an error occurs during the operation.
	 */
	public static Metadata getMetadata(byte[] bytes, ImageFormat format) throws ImageProcessingException, IOException {
		return getMetadata(bytes, format, null);
	}

	/**
	 * Reads image metadata for supported format.
	 *
	 * @param bytes the image for which to read metadata.
	 * @param fileType the {@link FileType} of the image.
	 * @return The {@link Metadata} or {@code null} if {@code bytes} is
	 *         {@code null}.
	 * @throws ImageProcessingException If the image cannot be processed.
	 * @throws IOException If an error occurs during the operation.
	 */
	public static Metadata getMetadata(byte[] bytes, FileType fileType) throws ImageProcessingException, IOException {
		return getMetadata(bytes, null, fileType);
	}

	/**
	 * Reads image metadata for supported format. Either {@code format} or
	 * {@code FileType} must be non-null.
	 *
	 * @param bytes the image for which to read metadata.
	 * @param format the {@link ImageFormat} of the image.
	 * @param fileType the {@link FileType} of the image.
	 * @return The {@link Metadata} or {@code null} if {@code bytes} is
	 *         {@code null}.
	 * @throws ImageProcessingException If the image cannot be processed.
	 * @throws IOException If an error occurs during the operation.
	 */
	public static Metadata getMetadata(byte[] bytes, ImageFormat format, FileType fileType) throws ImageProcessingException, IOException {
		if (bytes == null) {
			return null;
		}
		return getMetadata(new ByteArrayInputStream(bytes), format, fileType);
	}

	/**
	 * Reads image metadata for supported formats.
	 *
	 * @param inputStream the image for which to read metadata.
	 * @param format the {@link ImageFormat} of the image.
	 * @return The {@link Metadata} or {@code null} if {@code bytes} is
	 *         {@code null}.
	 * @throws ImageProcessingException If the image cannot be processed.
	 * @throws IOException If an error occurs during the operation.
	 */
	public static Metadata getMetadata(InputStream inputStream, ImageFormat format) throws ImageProcessingException, IOException {
		return getMetadata(inputStream, format, null);
	}

	/**
	 * Reads image metadata for supported formats.
	 *
	 * @param inputStream the image for which to read metadata.
	 * @param fileType the {@link FileType} of the image.
	 * @return The {@link Metadata} or {@code null} if {@code bytes} is
	 *         {@code null}.
	 * @throws ImageProcessingException If the image cannot be processed.
	 * @throws IOException If an error occurs during the operation.
	 */
	public static Metadata getMetadata(InputStream inputStream, FileType fileType) throws ImageProcessingException, IOException {
		return getMetadata(inputStream, null, fileType);
	}

	/**
	 * Reads image metadata for supported formats. Either {@code format} or
	 * {@code FileType} must be non-null.
	 *
	 * @param inputStream the image for which to read metadata.
	 * @param format the {@link ImageFormat} of the image.
	 * @param fileType the {@link FileType} of the image.
	 * @return The {@link Metadata} or {@code null} if {@code bytes} is
	 *         {@code null}.
	 * @throws ImageProcessingException If the image cannot be processed.
	 * @throws IOException If an error occurs during the operation.
	 */
	@SuppressWarnings("null")
	public static Metadata getMetadata(
		InputStream inputStream,
		ImageFormat format,
		FileType fileType
	) throws ImageProcessingException, IOException {
		if (inputStream == null) {
			return null;
		}
		if (format == null && fileType == null) {
			throw new IllegalArgumentException("Either format or fileType must be non-null");
		}

		Metadata metadata;

		if (fileType != null) {
			metadata = switch (fileType) {
				case Bmp -> BmpMetadataReader.readMetadata(inputStream);
				case Gif -> GifMetadataReader.readMetadata(inputStream);
				case Ico -> IcoMetadataReader.readMetadata(inputStream);
				case Jpeg -> JpegMetadataReader.readMetadata(inputStream);
				case Pcx -> PcxMetadataReader.readMetadata(inputStream);
				case Png -> PngMetadataReader.readMetadata(inputStream);
				case Psd -> PsdMetadataReader.readMetadata(inputStream);
				case Raf -> RafMetadataReader.readMetadata(inputStream);
				case Riff, WebP -> WebpMetadataReader.readMetadata(inputStream);
				case Tiff, Arw, Cr2, Nef, Orf, Rw2 -> TiffMetadataReader.readMetadata(new RandomAccessStreamReader(
						inputStream, RandomAccessStreamReader.DEFAULT_CHUNK_LENGTH, -1
					));
				// Return an empty Metadata instance for unsupported formats
				default -> new Metadata();
			};
		} else {
			metadata = switch (format) {
				case BMP -> BmpMetadataReader.readMetadata(inputStream);
				case GIF -> GifMetadataReader.readMetadata(inputStream);
				case ICO -> IcoMetadataReader.readMetadata(inputStream);
				case JPEG -> JpegMetadataReader.readMetadata(inputStream);
				case DCX, PCX -> PcxMetadataReader.readMetadata(inputStream);
				case PNG -> PngMetadataReader.readMetadata(inputStream);
				case PSD -> PsdMetadataReader.readMetadata(inputStream);
				case RAF -> RafMetadataReader.readMetadata(inputStream);
				case TIFF, ARW, CR2, NEF, ORF, RW2 -> TiffMetadataReader.readMetadata(new RandomAccessStreamReader(
						inputStream, RandomAccessStreamReader.DEFAULT_CHUNK_LENGTH, -1
					));
				case WEBP -> WebpMetadataReader.readMetadata(inputStream);
				case SOURCE -> ImageMetadataReader.readMetadata(inputStream);
				// Return an empty Metadata instance for unsupported formats
				default ->  new Metadata();
			};
		}
		return metadata;
	}

	/**
	 * Tries to parse {@link ExifOrientation} from the given metadata. If it
	 * fails, {@link ExifOrientation#TOP_LEFT} is returned.
	 *
	 * @param metadata the {@link Metadata} to parse.
	 * @return The parsed {@link ExifOrientation} or
	 *         {@link ExifOrientation#TOP_LEFT}.
	 */
	public static ExifOrientation parseExifOrientation(Metadata metadata) {
		return parseExifOrientation(metadata, ExifOrientation.TOP_LEFT);
	}

	/**
	 * Tries to parse {@link ExifOrientation} from the given metadata. If it
	 * fails, {@code defaultOrientation} is returned.
	 *
	 * @param metadata the {@link Metadata} to parse.
	 * @param defaultOrientation the default to return if parsing fails.
	 * @return The parsed {@link ExifOrientation} or {@code defaultOrientation}.
	 */
	public static ExifOrientation parseExifOrientation(Metadata metadata, ExifOrientation defaultOrientation) {
		if (metadata == null) {
			return defaultOrientation;
		}
		try {
			for (Directory directory : metadata.getDirectories()) {
				if (directory instanceof ExifIFD0Directory exifIFD0Directory &&
					exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)
				) {
					return ExifOrientation.typeOf(exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION));
				}
			}
		} catch (MetadataException e) {
			return defaultOrientation;
		}
		return defaultOrientation;
	}

	/**
	 * Extracts an embedded Exif thumbnail from a {@link Metadata} instance.
	 *
	 * @param file the {@link File} to read the thumbnail from
	 * @param metadata the {@link Metadata} collected on the {@link File}
	 *                 previously.
	 * @return A byte array containing the thumbnail or {@code null} if no
	 *         thumbnail was found/could be extracted.
	 */
	@SuppressWarnings("unused")
	public static byte[] getThumbnailFromMetadata(File file, Metadata metadata) {
		if (metadata == null) {
			return null;
		}

		/*
		 * XXX Extraction of thumbnails was removed in version
		 * 2.10.0 of metadata-extractor because of a bug in
		 * related code. This section is deactivated while
		 * waiting for this to be made available again.
		 *
		 * Images supported by ImageIO or DCRaw aren't affected,
		 * so this only applied to very few images anyway.
		 * It could extract thumbnails for some "raw" images
		 * if DCRaw was disabled.
		 *
		// First check if there is a ExifThumbnailDirectory
		Collection<ExifThumbnailDirectory> directories = metadata.getDirectoriesOfType(ExifThumbnailDirectory.class);
		if (directories.isEmpty()) {
			return null;
		}

		// Now get the thumbnail data if they're there
		directories = metadata.getDirectoriesOfType(ExifThumbnailDirectory.class);
		for (ExifThumbnailDirectory directory : directories) {
			if (directory.hasThumbnailData()) {
				return directory.getThumbnailData();
			}
		}*/

		return null;
	}

	/**
	 * Finds the offset of the given Exif tag. Only the first IFD is searched.
	 *
	 * @param tagId the tag id to look for.
	 * @param reader a {@link RandomAccessReader} with a JPEG image.
	 * @return the offset of the given tag's value, or -1 if not found.
	 * @throws UnknownFormatException if the content isn't a JPEG.
	 * @throws IOException if any error occurs while reading.
	 */
	public static int getJPEGExifIFDTagOffset(int tagId, RandomAccessReader reader) throws UnknownFormatException, IOException {
		reader.setMotorolaByteOrder(true);
		if (reader.getUInt16(0) != 0xFFD8) {
			throw new UnknownFormatException("Content isn't JPEG");
		}

		byte jpegSegmentIdentifier = (byte) 0xFF;
		byte markerEOI = (byte) 0xD9;
		byte app1 = (byte) 0xE1;
		final String exifSegmentPreamble = "Exif\0\0";

		byte segmentIdentifier = reader.getInt8(2);
		byte segmentType = reader.getInt8(3);

		int pos = 4;

		while (
			segmentIdentifier != jpegSegmentIdentifier ||
			segmentType != app1 &&
			segmentType != markerEOI ||
			segmentType == app1 &&
			!exifSegmentPreamble.equals(new String(
				reader.getBytes(pos + 2, exifSegmentPreamble.length()),
				0,
				exifSegmentPreamble.length(),
				StandardCharsets.US_ASCII)
			)
		) {
			segmentIdentifier = segmentType;
			segmentType = reader.getInt8(pos++);
		}

		if (segmentType == markerEOI) {
			// Reached the end of the image without finding an Exif segment
			return -1;
		}

		int segmentLength = reader.getUInt16(pos) - 2;
		pos += 2 + exifSegmentPreamble.length();

		if (segmentLength < exifSegmentPreamble.length()) {
			throw new ParseException("Exif segment is too small");
		}

		int exifHeaderOffset = pos;

		short byteOrderIdentifier = reader.getInt16(pos);
		pos += 4; // Skip TIFF marker

		switch (byteOrderIdentifier) {
			// "MM"
			case 0x4d4d -> reader.setMotorolaByteOrder(true);
			// "II"
			case 0x4949 -> reader.setMotorolaByteOrder(false);
			default -> throw new ParseException("Can't determine Exif endianness from: 0x" + Integer.toHexString(byteOrderIdentifier));
		}

		pos = reader.getInt32(pos) + exifHeaderOffset;

		int tagCount = reader.getUInt16(pos);

		for (int tagNumber = 0; tagNumber < tagCount; tagNumber++) {
			int tagOffset = pos + 2 + (12 * tagNumber);
			int curTagId = reader.getUInt16(tagOffset);
			if (curTagId == tagId) {
				// tag found
				return tagOffset + 8;
			}
		}

		return -1;
	}

	/**
	 * Reads the resolution specified in the JPEG SOF header.
	 *
	 * @param reader a {@link RandomAccessReader} with a JPEG image.
	 * @return The JPEG SOF specified resolution or {@code null} if no SOF is
	 *         found.
	 * @throws UnknownFormatException if the content isn't a JPEG.
	 * @throws IOException if any error occurs while reading.
	 */
	public static Dimension getJPEGResolution(RandomAccessReader reader) throws UnknownFormatException, IOException {
		reader.setMotorolaByteOrder(true);
		if (reader.getUInt16(0) != 0xFFD8) {
			throw new UnknownFormatException("Content isn't JPEG");
		}

		byte jpegSegmentIdentifier = (byte) 0xFF;
		byte markerEOI = (byte) 0xD9;
		Set<Byte> sofs = Set.of(
			(byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC5,
			(byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA,
			(byte) 0xCB, (byte) 0xCD, (byte) 0xCE, (byte) 0xCF
		);

		byte segmentIdentifier = reader.getInt8(2);
		byte segmentType = reader.getInt8(3);

		int pos = 4;

		while (
			segmentIdentifier != jpegSegmentIdentifier ||
			!sofs.contains(segmentType) &&
			segmentType != markerEOI
		) {
			segmentIdentifier = segmentType;
			segmentType = reader.getInt8(pos++);
		}

		if (segmentType == markerEOI) {
			// Reached the end of the image without finding the SOF segment
			return null;
		}

		int segmentLength = reader.getUInt16(pos) - 2;
		pos += 3;

		if (segmentLength < 5) {
			throw new ParseException("SOF segment is too small");
		}

		return new Dimension(reader.getUInt16(pos + 2), reader.getUInt16(pos));
	}

	/**
	 * Tries to gather the data needed to populate a {@link ImageInfo} instance
	 * describing the input image.
	 *
	 * <p>
	 * This method does not close {@code inputStream}.
	 *
	 * @param inputStream the image whose information to gather.
	 * @param size the size of the image in bytes or
	 *             {@link ImageInfo#SIZE_UNKNOWN} if it can't be determined.
	 * @param metadata the {@link Metadata} instance to embed in the resulting
	 *                 {@link ImageInfo} instance.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @return An {@link ImageInfo} instance describing the input image.
	 * @throws UnknownFormatException if the format could not be determined.
	 * @throws IOException if an IO error occurred.
	 */
	public static ImageInfo readImageInfo(InputStream inputStream, long size, Metadata metadata, boolean applyExifOrientation) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("input == null!");
		}

		try (ImageInputStream stream = ImageIOTools.createImageInputStream(inputStream)) {
			Iterator<?> iter = ImageIO.getImageReaders(stream);
			if (!iter.hasNext()) {
				throw new UnknownFormatException("Unable to find a suitable image reader");
			}

			ImageReader reader = (ImageReader) iter.next();
			try {
				int width = -1;
				int height = -1;
				ImageFormat format = ImageFormat.toImageFormat(reader.getFormatName());
				if (format == null) {
					throw new UnknownFormatException("Unable to determine image format");
				}

				ColorModel colorModel = null;
				try {
					reader.setInput(stream, true, true);
					Iterator<ImageTypeSpecifier> iterator = reader.getImageTypes(0);
					if (iterator.hasNext()) {
						colorModel = iterator.next().getColorModel();
					}
					width = reader.getWidth(0);
					height = reader.getHeight(0);
				} catch (RuntimeException e) {
					throw new ImageIORuntimeException("Error reading image information: " + e.getMessage(), e);
				}

				boolean imageIOSupport;
				if (format == ImageFormat.TIFF) {
					// ImageIO thinks that it can read some "TIFF like" RAW formats,
					// but fails when it actually tries, so we have to test it.
					try {
						ImageReadParam param = reader.getDefaultReadParam();
						param.setSourceRegion(new Rectangle(1, 1));
						reader.read(0, param);
						imageIOSupport = true;
					} catch (IOException e) {
						// Catch anything here, we simply want to test if it fails.
						imageIOSupport = false;
					}
				} else {
					imageIOSupport = true;
				}

				return ImageInfo.create(
					width,
					height,
					format,
					size,
					colorModel,
					metadata,
					applyExifOrientation,
					imageIOSupport
				);
			} finally {
				reader.dispose();
			}
		}
	}

}
