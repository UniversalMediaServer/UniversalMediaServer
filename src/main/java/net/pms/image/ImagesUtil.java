package net.pms.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.Positions;
import net.pms.dlna.DLNAImage;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAImageProfile.DLNAComplianceResult;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImageIOTools.ImageReaderResult;
import net.pms.image.thumbnailator.ExifFilterUtils;
import net.pms.util.BufferedImageType;
import net.pms.util.InvalidStateException;
import net.pms.util.ParseException;
import net.pms.util.ResettableInputStream;
import net.pms.util.UnknownFormatException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class ImagesUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImagesUtil.class);

	/**
	 * Parses an image file and stores the results in the given
	 * {@link DLNAMediaInfo}. Parsing is performed using both
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
	 * @param media the {@link DLNAMediaInfo} instance to store the parsing
	 *              results to.
	 * @throws IOException if an IO error occurs or no information can be parsed.
	 *
	 */
	public static void parseImage(File file, DLNAMediaInfo media) throws IOException {
		final int MAX_BUFFER = 1048576; // 1 MB
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
		ResettableInputStream inputStream = new ResettableInputStream(Files.newInputStream(file.toPath()), MAX_BUFFER);
		try  {
			Metadata metadata = null;
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
					fileType.toString().toUpperCase(Locale.ROOT),
					file.getAbsolutePath(),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			ImageFormat format = ImageFormat.toImageFormat(fileType);
			if (format == null || format == ImageFormat.TIFF) {
				ImageFormat tmpformat = ImageFormat.toImageFormat(metadata);
				if (tmpformat != null) {
					format = tmpformat;
				}
			}
			if (inputStream.isFullResetAvailable()) {
				inputStream.fullReset();
			} else {
				// If we can't reset it, close it and create a new
				inputStream.close();
				inputStream = new ResettableInputStream(Files.newInputStream(file.toPath()), MAX_BUFFER);
			}
			ImageInfo imageInfo = null;
			try {
				imageInfo = ImageIOTools.readImageInfo(inputStream, size , metadata, false);
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

			if (format == null) {
				format = imageInfo.getFormat();
			} else if (imageInfo != null && imageInfo.getFormat() != null && format != imageInfo.getFormat()) {
				if (imageInfo.getFormat() == ImageFormat.TIFF && format.isRaw()) {
					if (format == ImageFormat.ARW && !isARW(metadata)) {
						// XXX Remove this if https://github.com/drewnoakes/metadata-extractor/issues/217 is fixed
						// Metadata extractor misidentifies some Photoshop created TIFFs for ARW, correct it
						format = ImageFormat.toImageFormat(metadata);
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
			if (format != null) {
				media.setCodecV(format.toFormatConfiguration());
				media.setContainer(format.toFormatConfiguration());
			}
			if (trace) {
				LOGGER.trace("Parsing of image \"{}\" completed", file.getName());
			}
		} finally {
			inputStream.close();
		}
	}

	/**
	 * There is a bug in Metadata Extractor that misidentifies some TIFF files
	 * as ARW files. This method is here to verify if such a misidentification
	 * has taken place or not.
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
	 * @return The version number multiplied with 100 (last two digits are decimals).
	 */
	public static int parseExifVersion(byte[] bytes) {
		if (bytes == null) {
			return ImageInfo.UNKNOWN;
		}
		StringBuilder stringBuilder = new StringBuilder(4);
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] > 47 && bytes[i] < 58) {
				stringBuilder.append((char) bytes[i]);
			} else if (bytes[i] == 0 && i == bytes.length - 1) {
				// Some buggy C/C++ software doesn't properly format the string
				// so we end up with a null-terminated string without the leading zero.
				stringBuilder.insert(0, '0');
			}
		}
		while (stringBuilder.length() < 4) {
			stringBuilder.append("0");
		}
		try {
			return Integer.parseInt(stringBuilder.toString());
		} catch (NumberFormatException e) {
			LOGGER.debug("Failed to parse Exif version number from: {}", Arrays.toString(bytes));
			return 0;
		}
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
				if (directory instanceof ExifIFD0Directory) {
					if (((ExifIFD0Directory) directory).containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
						return ExifOrientation.typeOf(((ExifIFD0Directory) directory).getInt(ExifIFD0Directory.TAG_ORIENTATION));
					}
				}
			}
		} catch (MetadataException e) {
			return defaultOrientation;
		}
		return defaultOrientation;
	}

	/**
	 * Checks if the resolution axes must be swapped if the image is rotated
	 * according to the given Exif orientation.
	 *
	 * @param imageInfo the {@link ImageInfo} whose Exif orientation to evaluate.
	 * @return {@code true} if the axes should be swapped, {@code false}
	 *         otherwise.
	 */
	public static boolean isExifAxesSwapNeeded(ImageInfo imageInfo) {
		return imageInfo != null && isExifAxesSwapNeeded(imageInfo.getExifOrientation());
	}

	/**
	 * Checks if the resolution axes must be swapped if the image is rotated
	 * according to the given Exif orientation.
	 *
	 * @param orientation the Exif orientation to evaluate.
	 * @return {@code true} if the axes should be swapped, {@code false}
	 *         otherwise.
	 */

	public static boolean isExifAxesSwapNeeded(ExifOrientation orientation) {
		if (orientation == null) {
			return false;
		}
		switch (orientation) {
			case LEFT_TOP:
			case RIGHT_TOP:
			case RIGHT_BOTTOM:
			case LEFT_BOTTOM:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Calculates the resolution for the image described by {@code imageInfo} if
	 * it is scaled to {@code scaleWidth} width and {@code scaleHeight} height
	 * while preserving aspect ratio.
	 *
	 * @param actualWidth the width of the source image.
	 * @param actualHeight the height of the source image.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param scaleWidth the width to scale to.
	 * @param scaleHeight the height to scale to.
	 * @return A {@link Dimension} with the resulting resolution.
	 */
	public static Dimension calculateScaledResolution(
		ImageInfo imageInfo,
		ScaleType scaleType,
		int scaleWidth,
		int scaleHeight
	) {
		return calculateScaledResolution(
			imageInfo.getWidth(),
			imageInfo.getHeight(),
			scaleType,
			scaleWidth,
			scaleHeight
		);
	}

	/**
	 * Calculates the resolution for the image with {@code actualWidth} width
	 * and {@code actualHeight}) height if it is scaled to {@code scaleWidth}
	 * width and {@code scaleHeight} height while preserving aspect ratio.
	 *
	 * @param actualWidth the width of the source image.
	 * @param actualHeight the height of the source image.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param scaleWidth the width to scale to.
	 * @param scaleHeight the height to scale to.
	 * @return A {@link Dimension} with the resulting resolution.
	 */
	public static Dimension calculateScaledResolution(
		int actualWidth,
		int actualHeight,
		ScaleType scaleType,
		int scaleWidth,
		int scaleHeight
	) {
		if (scaleType == null) {
			throw new NullPointerException("scaleType cannot be null");
		}
		if (actualWidth < 1 || actualHeight < 1) {
			throw new IllegalArgumentException(String.format(
				"actualWidth (%d) and actualHeight (%d) must be positive", actualWidth, actualHeight
			));
		}
		if (scaleWidth < 1 || scaleHeight < 1) {
			throw new IllegalArgumentException(String.format(
				"scaleWidth (%d) and scaleHeight (%d) must be positive", scaleWidth, scaleHeight
			));
		}

		double scale = Math.min((double) scaleWidth / actualWidth, (double) scaleHeight / actualHeight);
		if (scaleType == ScaleType.MAX && scale > 1) {
			// Never scale up for ScaleType.MAX
			scale = 1;
		}
		return new Dimension(
			(int) Math.max(Math.round(actualWidth * scale), 1),
			(int) Math.max(Math.round(actualHeight * scale), 1)
		);
	}

	/**
	 * Retrieves a reference to the underlying byte array from a
	 * {@link ByteArrayInputStream} using reflection. Keep in mind that this
	 * this byte array is shared with the {@link ByteArrayInputStream}.
	 *
	 * @param inputStream the {@link ByteArrayInputStream} whose buffer to retrieve.
	 * @return The byte array or {@code null} if retrieval failed.
	 */
	public static byte[] retrieveByteArray(ByteArrayInputStream inputStream) {
		Field f;
		try {
			f = ByteArrayInputStream.class.getDeclaredField("buf");
			f.setAccessible(true);
			return (byte[]) f.get(inputStream);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			LOGGER.debug("Unexpected reflection failure in retrieveByteArray(): {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}
	}
	/**
	 * This attempts to get the underlying byte array directly from the
	 * {@link InputStream} if it is backed by a byte array, otherwise the
	 * {@link InputStream} is copied into a new byte array with
	 * {@link IOUtils#toByteArray(InputStream)}.
	 * <p><b>
	 * This method consumes and closes {@code inputStream}.
	 * </b>
	 * @param inputStream the <code>InputStream</code> to read.
     * @return The resulting byte array.
     * @throws IOException if an I/O error occurs
	 */
	public static byte[] toByteArray(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		}

		// Avoid copying the data if it's already a byte array
		if (inputStream instanceof ByteArrayInputStream) {
			byte[] bytes = retrieveByteArray((ByteArrayInputStream) inputStream);
			if (bytes != null) {
				return bytes;
			}
			// Reflection failed, use IOUtils to read the stream instead
		}
		try {
			return IOUtils.toByteArray(inputStream);
		} finally {
			inputStream.close();
		}
	}

	/**
	 * Converts an image to a different {@link ImageFormat}. Rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @return The converted image or {@code null} if the source is {@code null}
	 *         .
	 * @throws IOException if the operation fails.
	 */
	public static Image convertImage(
		Image inputImage,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail
	) throws IOException {
		return transcodeImage(
			inputImage,
			0,
			0,
			null,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			false
		);
	}

	/**
	 * Converts an image to a different {@link ImageFormat}. Rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @return The converted image or {@code null} if the source is {@code null}
	 *         .
	 * @throws IOException if the operation fails.
	 */
	public static Image convertImage(
		InputStream inputStream,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail
	) throws IOException {
		return transcodeImage(
			inputStream,
			0,
			0,
			null,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			false
		);
	}

	/**
	 * Converts an image to a different {@link ImageFormat}. Rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @return The converted image or {@code null} if the source is {@code null}
	 *         .
	 * @throws IOException if the operation fails.
	 */
	public static Image convertImage(
		byte[] inputByteArray,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			0,
			0,
			null,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			false
		);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio and rotates/flips the image according to
	 * Exif orientation. Format support is limited to that of {@link ImageIO}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width.
	 * @param height the new height.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public Image scaleImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			inputImage,
			width,
			height,
			scaleType,
			ImageFormat.SOURCE,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio and rotates/flips the image according to
	 * Exif orientation. Format support is limited to that of {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width.
	 * @param height the new height.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public Image scaleImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			inputStream,
			width,
			height,
			scaleType,
			ImageFormat.SOURCE,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio and rotates/flips the image according to
	 * Exif orientation. Format support is limited to that of {@link ImageIO}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width.
	 * @param height the new height.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */

	public Image scaleImage(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			width,
			height,
			scaleType,
			ImageFormat.SOURCE,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The converted image or {@code null} if the source is {@code null}
	 *         .
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		Image inputImage,
		DLNAImageProfile outputProfile,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputImage, 0, 0, null, outputProfile, true, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The converted image or {@code null} if the source is {@code null}
	 *         .
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		InputStream inputStream,
		DLNAImageProfile outputProfile,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputStream, 0, 0, null, outputProfile, true, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The converted image or {@code null} if the source is {@code null}
	 *         .
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		byte[] inputByteArray,
		DLNAImageProfile outputProfile,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputByteArray, 0, 0, null, outputProfile, true, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			null,
			inputImage,
			null,
			width,
			height,
			scaleType,
			outputFormat,
			null,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			null,
			null,
			inputStream,
			width,
			height,
			scaleType,
			outputFormat,
			null,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			null,
			null,
			width,
			height,
			scaleType,
			outputFormat,
			null,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			null,
			inputImage,
			null,
			width,
			height,
			scaleType,
			null,
			outputProfile,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			null,
			null,
			inputStream,
			width,
			height,
			scaleType,
			null,
			outputProfile,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			null,
			null,
			width,
			height,
			scaleType,
			null,
			outputProfile,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param inputImage the source {@link Image}.
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 *            Overridden by {@code outputProfile}.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to. This
	 *            overrides {@code outputFormat}.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	protected static Image transcodeImage(
		byte[] inputByteArray,
		Image inputImage,
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		if (inputByteArray == null && inputStream == null && inputImage == null) {
			return null;
		}
		if (
			(inputByteArray != null & inputImage != null) ||
			(inputByteArray != null & inputStream != null) ||
			(inputImage != null & inputStream != null)
		) {
			throw new IllegalArgumentException("Use either inputByteArray, inputImage or inputStream");
		}

		boolean trace = LOGGER.isTraceEnabled();
		if (trace) {
			StringBuilder sb = new StringBuilder();
			if (scaleType != null) {
				sb.append("ScaleType = ").append(scaleType);
			}
			if (width > 0 && height > 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("Width = ").append(width).append(", Height = ").append(height);
			}
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("PadToSize = ").append(padToSize ? "True" : "False");
			LOGGER.trace(
				"Converting {} image source to {} format and type {} using the following parameters: {}",
				inputByteArray != null ? "byte array" : inputImage != null ? "Image" : "input stream",
				outputProfile != null ? outputProfile : outputFormat,
				dlnaThumbnail ? "DLNAThumbnail" : dlnaCompliant ? "DLNAImage" : "Image",
				sb
			);
		}

		ImageIO.setUseCache(false);
		dlnaCompliant = dlnaCompliant || dlnaThumbnail;

		if (inputImage != null) {
			inputByteArray = inputImage.getBytes(false);
		} else if (inputStream != null) {
			inputByteArray = ImagesUtil.toByteArray(inputStream);
		}

		// outputProfile overrides outputFormat
		if (outputProfile != null) {
			if (dlnaThumbnail && outputProfile.equals(DLNAImageProfile.GIF_LRG)) {
				outputProfile = DLNAImageProfile.JPEG_LRG;
			}
			// Default to correct ScaleType for the profile
			if (scaleType == null) {
				if (DLNAImageProfile.JPEG_RES_H_V.equals(outputProfile)) {
					scaleType = ScaleType.EXACT;
				} else {
					scaleType = ScaleType.MAX;
				}
			}
			outputFormat = ImageFormat.toImageFormat(outputProfile);
			width = width > 0 ? width : outputProfile.getMaxWidth();
			height = height > 0 ? height : outputProfile.getMaxHeight();
		} else if (scaleType == null) {
			scaleType = ScaleType.MAX;
		}

		ImageReaderResult inputResult;
		try {
			inputResult = ImageIOTools.read(new ByteArrayInputStream(inputByteArray));
		} catch (IIOException e) {
			throw new UnknownFormatException("Unable to read image format", e);
		}

		if (inputResult.bufferedImage == null || inputResult.imageFormat == null) { // ImageIO doesn't support the image format
			throw new UnknownFormatException("Failed to transform image because the source format is unknown");
		}

		if (outputFormat == null || outputFormat == ImageFormat.SOURCE) {
			outputFormat = inputResult.imageFormat;
		}

		BufferedImage bufferedImage = inputResult.bufferedImage;
		boolean reencode = false;

		if (outputProfile == null && dlnaCompliant) {
			// Override output format to one valid for DLNA, defaulting to PNG
			// if the source image has alpha and JPEG if not.
			switch (outputFormat) {
				case GIF:
					if (dlnaThumbnail) {
						outputFormat = ImageFormat.JPEG;
					}
					break;
				case JPEG:
				case PNG:
					break;
				default:
					if (bufferedImage.getColorModel().hasAlpha()) {
						outputFormat = ImageFormat.PNG;
					} else {
						outputFormat = ImageFormat.JPEG;
					}
			}
		}

		Metadata metadata = null;
		ExifOrientation orientation;
		if (inputImage != null && inputImage.getImageInfo() != null) {
			orientation = inputImage.getImageInfo().getExifOrientation();
		} else {
			try {
				metadata = getMetadata(inputByteArray, inputResult.imageFormat);
			} catch (IOException | ImageProcessingException e) {
				LOGGER.error("Failed to read input image metadata: {}", e.getMessage());
				LOGGER.trace("", e);
				metadata = new Metadata();
			}
			if (metadata == null) {
				metadata = new Metadata();
			}
			orientation = parseExifOrientation(metadata);
		}

		if (orientation != ExifOrientation.TOP_LEFT) {
			// Rotate the image before doing all the other checks
			BufferedImage oldBufferedImage = bufferedImage;
			bufferedImage = Thumbnails.of(bufferedImage)
				.scale(1.0d)
				.addFilter(ExifFilterUtils.getFilterForOrientation(orientation.getThumbnailatorOrientation()))
				.asBufferedImage();
			oldBufferedImage.flush();
			// Re-parse the metadata after rotation as these are newly generated.
			ByteArrayOutputStream tmpOutputStream = new ByteArrayOutputStream(inputByteArray.length);
			Thumbnails.of(bufferedImage).scale(1.0d).outputFormat(outputFormat.toString()).toOutputStream(tmpOutputStream);
			try {
				metadata = getMetadata(tmpOutputStream.toByteArray(), outputFormat);
			} catch (IOException | ImageProcessingException e) {
				LOGGER.debug("Failed to read rotated image metadata: {}", e.getMessage());
				LOGGER.trace("", e);
				metadata = new Metadata();
			}
			if (metadata == null) {
				metadata = new Metadata();
			}
			reencode = true;
		}

		if (outputProfile == null && dlnaCompliant) {
			// Set a suitable output profile.
			if (width < 1 || height < 1) {
				outputProfile = DLNAImageProfile.getClosestDLNAProfile(
					bufferedImage.getWidth(),
					bufferedImage.getHeight(),
					outputFormat,
					true
				);
				width = outputProfile.getMaxWidth();
				height = outputProfile.getMaxHeight();
			} else {
				outputProfile = DLNAImageProfile.getClosestDLNAProfile(
					calculateScaledResolution(
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						scaleType,
						width,
						height
					),
					outputFormat,
					true
				);
				width = Math.min(width, outputProfile.getMaxWidth());
				height = Math.min(height, outputProfile.getMaxHeight());
			}
			if (DLNAImageProfile.JPEG_RES_H_V.equals(outputProfile)) {
				scaleType = ScaleType.EXACT;
			}
		}

		boolean convertColors =
			bufferedImage.getType() == BufferedImageType.TYPE_CUSTOM.getTypeId() ||
			bufferedImage.getType() == BufferedImageType.TYPE_BYTE_BINARY.getTypeId() ||
			bufferedImage.getColorModel().getColorSpace().getType() != ColorSpaceType.TYPE_RGB.getTypeId();

		// Impose DLNA format restrictions
		if (!reencode && outputFormat == inputResult.imageFormat && outputProfile != null) {
			DLNAComplianceResult complianceResult;
			switch (outputFormat) {
				case GIF:
				case JPEG:
				case PNG:
					ImageInfo imageInfo;
					// metadata is only null at this stage if inputImage != null and no rotation was necessary
					if (metadata == null) {
						imageInfo = inputImage.getImageInfo();
					}
					imageInfo = ImageInfo.create(
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						inputResult.imageFormat,
						ImageInfo.SIZE_UNKNOWN,
						bufferedImage.getColorModel(),
						metadata,
						false,
						true
					);
					complianceResult = DLNAImageProfile.checkCompliance(imageInfo, outputProfile);
					break;
				default:
					throw new IllegalStateException("Unexpected image format: " + outputFormat);
			}
			reencode = reencode || convertColors || !complianceResult.isFormatCorrect() || !complianceResult.isColorsCorrect();;
			if (!complianceResult.isResolutionCorrect()) {
				width = width > 0 && complianceResult.getMaxWidth() > 0 ?
					Math.min(width, complianceResult.getMaxWidth()) :
						width > 0 ? width : complianceResult.getMaxWidth();
				height = height > 0 && complianceResult.getMaxHeight() > 0 ?
					Math.min(height, complianceResult.getMaxHeight()) :
						height > 0 ? height : complianceResult.getMaxHeight();
			}
			if (trace) {
				if (complianceResult.isAllCorrect()) {
					LOGGER.trace("Image conversion DLNA compliance check: The source image is compliant");
				} else {
					LOGGER.trace(
						"Image conversion DLNA compliance check for {}: " +
						"The source image colors are {}, format is {} and resolution ({} x {}) is {}.\nFailures:\n  {}",
						outputProfile,
						complianceResult.isColorsCorrect() ? "compliant" : "non-compliant",
						complianceResult.isFormatCorrect() ? "compliant" : "non-compliant",
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						complianceResult.isResolutionCorrect() ? "compliant" : "non-compliant",
						StringUtils.join(complianceResult.getFailures(), "\n  ")
					);
				}
			}
		}

		if (convertColors) {
			// Preserve alpha channel if the output format supports it
			BufferedImageType outputImageType;
			if (
				(outputFormat == ImageFormat.PNG || outputFormat == ImageFormat.PSD) &&
				bufferedImage.getColorModel().getNumComponents() == 4
			) {
				outputImageType = bufferedImage.isAlphaPremultiplied() ?
					BufferedImageType.TYPE_4BYTE_ABGR_PRE :
					BufferedImageType.TYPE_4BYTE_ABGR;
			} else {
				outputImageType = BufferedImageType.TYPE_3BYTE_BGR;
			}

			BufferedImage convertedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), outputImageType.getTypeId());
			ColorConvertOp colorConvertOp = new ColorConvertOp(null);
			colorConvertOp.filter(bufferedImage, convertedImage);
			bufferedImage.flush();
			bufferedImage = convertedImage;
			reencode = true;
		}

		if (width < 1 ||
			height < 1 ||
			(
				scaleType == ScaleType.MAX &&
				bufferedImage.getWidth() <= width &&
				bufferedImage.getHeight() <= height
			) ||
			(
				scaleType == ScaleType.EXACT &&
				bufferedImage.getWidth() == width &&
				bufferedImage.getHeight() == height
			)
		) {
			//No resize, just convert
			if (!reencode && inputResult.imageFormat == outputFormat) {
				// Nothing to do, just return source

				// metadata is only null at this stage if inputImage != null
				Image result;
				if (dlnaThumbnail) {
					result = metadata == null ?
						new DLNAThumbnail(inputImage, outputProfile, false) :
						new DLNAThumbnail(inputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
				} else if (dlnaCompliant) {
					result = metadata == null ?
						new DLNAImage(inputImage, outputProfile, false) :
						new DLNAImage(inputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
				} else {
					result = metadata == null ?
						new Image(inputImage, false) :
						new Image(inputByteArray, outputFormat, bufferedImage, metadata, false);
				}
				bufferedImage.flush();

				if (trace) {
					LOGGER.trace(
						"No conversion is needed, returning source image with width = {}, height = {} and output {}.",
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						dlnaCompliant && outputProfile != null ? "profile: " + outputProfile : "format: " + outputFormat
					);
				}
				return result;
			} else if (!reencode) {
				// Convert format
				reencode = true;
			}
		} else {
			boolean force = DLNAImageProfile.JPEG_RES_H_V.equals(outputProfile);
			BufferedImage oldBufferedImage = bufferedImage;
			if (padToSize && force) {
				bufferedImage = Thumbnails.of(bufferedImage)
					.forceSize(width, height)
					.addFilter(new Canvas(width, height, Positions.CENTER, Color.BLACK))
					.asBufferedImage();
			} else if (padToSize) {
				bufferedImage = Thumbnails.of(bufferedImage)
					.size(width, height)
					.addFilter(new Canvas(width, height, Positions.CENTER, Color.BLACK))
					.asBufferedImage();
			} else if (force) {
				bufferedImage = Thumbnails.of(bufferedImage)
					.forceSize(width, height)
					.asBufferedImage();
			} else {
				bufferedImage = Thumbnails.of(bufferedImage)
					.size(width, height)
					.asBufferedImage();
			}
			oldBufferedImage.flush();
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Thumbnails.of(bufferedImage)
			.scale(1.0d)
			.outputFormat(outputFormat.toString())
			.outputQuality(1.0f)
			.toOutputStream(outputStream);

		byte[] outputByteArray = outputStream.toByteArray();

		Image result;
		if (dlnaThumbnail) {
			result = new DLNAThumbnail(outputByteArray, bufferedImage.getWidth(), bufferedImage.getHeight(), outputFormat, null, null, outputProfile, false);
		} else if (dlnaCompliant) {
			result = new DLNAImage(outputByteArray, bufferedImage.getWidth(), bufferedImage.getHeight(), outputFormat, null, null, outputProfile, false);
		} else {
			result = new Image(outputByteArray, bufferedImage.getWidth(), bufferedImage.getHeight(), outputFormat, null, null, true, false);
		}

		if (trace) {
			StringBuilder sb = new StringBuilder();
			sb.append("Convert colors = ").append(convertColors ? "True" : "False")
			.append(", Re-encode = ").append(reencode ? "True" : "False");

			LOGGER.trace(
				"Finished converting {} {} image{}. Output image resolution: {}, {}. Flags: {}",
				inputResult.width + "×" + inputResult.height,
				inputResult.imageFormat,
				orientation != ExifOrientation.TOP_LEFT ? " with orientation " + orientation : "",
				bufferedImage.getWidth() + "×" + bufferedImage.getHeight(),
				dlnaCompliant && outputProfile != null ? "profile: " + outputProfile : "format: " + outputFormat,
				sb
			);
		}

		bufferedImage.flush();
		return result;
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
	 * Reads image metadata for supported format.
	 *
	 * @param bytes the image for which to read metadata.
	 * @param format the {@link ImageFormat} of the image.
	 * @return The {@link Metadata} or {@code null} if {@code bytes} is
	 *         {@code null}.
	 * @throws ImageProcessingException
	 * @throws IOException
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
	 * @throws ImageProcessingException
	 * @throws IOException
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
	 * @throws ImageProcessingException
	 * @throws IOException
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
	 * @throws ImageProcessingException
	 * @throws IOException
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
	 * @throws ImageProcessingException
	 * @throws IOException
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
	 * @throws ImageProcessingException
	 * @throws IOException
	 */
	public static Metadata getMetadata(InputStream inputStream, ImageFormat format, FileType fileType) throws ImageProcessingException, IOException {
		if (inputStream == null) {
			return null;
		}
		if (format == null && fileType == null) {
			throw new IllegalArgumentException("Either format or fileType must be non-null");
		}

		Metadata metadata = null;

		if (fileType != null) {
			switch (fileType) {
				case Bmp:
		            metadata = BmpMetadataReader.readMetadata(inputStream);
					break;
				case Gif:
		            metadata = GifMetadataReader.readMetadata(inputStream);
					break;
				case Ico:
		            metadata = IcoMetadataReader.readMetadata(inputStream);
					break;
				case Jpeg:
		            metadata = JpegMetadataReader.readMetadata(inputStream);
					break;
				case Pcx:
		            metadata = PcxMetadataReader.readMetadata(inputStream);
					break;
				case Png:
		            metadata = PngMetadataReader.readMetadata(inputStream);
					break;
				case Psd:
		            metadata = PsdMetadataReader.readMetadata(inputStream);
					break;
				case Raf:
		            metadata = RafMetadataReader.readMetadata(inputStream);
					break;
				case Riff:
		            metadata = WebpMetadataReader.readMetadata(inputStream);
					break;
				case Tiff:
				case Arw:
				case Cr2:
				case Nef:
				case Orf:
				case Rw2:
					metadata = TiffMetadataReader.readMetadata(new RandomAccessStreamReader(
						inputStream, RandomAccessStreamReader.DEFAULT_CHUNK_LENGTH, -1
					));
					break;
				case Crw:
				case Unknown:
				default:
					// Return an empty Metadata instance for unsupported formats
					metadata = new Metadata();
			}
		} else {
			switch (format) {
				case BMP:
					metadata = BmpMetadataReader.readMetadata(inputStream);
					break;
				case GIF:
		            metadata = GifMetadataReader.readMetadata(inputStream);
					break;
				case ICO:
					metadata = IcoMetadataReader.readMetadata(inputStream);
					break;
				case JPEG:
					metadata = JpegMetadataReader.readMetadata(inputStream);
					break;
				case DCX:
				case PCX:
		            metadata = PcxMetadataReader.readMetadata(inputStream);
					break;
				case PNG:
					metadata = PngMetadataReader.readMetadata(inputStream);
					break;
				case PSD:
		            metadata = PsdMetadataReader.readMetadata(inputStream);
					break;
				case RAF:
		            metadata = RafMetadataReader.readMetadata(inputStream);
					break;
				case TIFF:
				case ARW:
				case CR2:
				case NEF:
				case ORF:
				case RW2:
					metadata = TiffMetadataReader.readMetadata(new RandomAccessStreamReader(
						inputStream, RandomAccessStreamReader.DEFAULT_CHUNK_LENGTH, -1
					));
					break;
				case WEBP:
		            metadata = WebpMetadataReader.readMetadata(inputStream);
					break;
				case SOURCE:
					metadata = ImageMetadataReader.readMetadata(inputStream);
					break;
				// Return an empty Metadata instance for unsupported formats
				case CRW:
				case CUR:
				case ICNS:
				case PNM:
				case WBMP:
				default:
					metadata = new Metadata();
			}
		}
		return metadata;
	}

	/**
	 * @param fileName the "file name" part of the HTTP request.
	 * @return The "decoded" {@link ImageProfile} or
	 *         {@link ImageProfile#JPEG_TN} if the parsing fails.
	 */
	public static DLNAImageProfile parseThumbRequest(String fileName) {
		if (fileName.startsWith("thumbnail0000")) {
			fileName = fileName.substring(13);

			return parseImageRequest(fileName, DLNAImageProfile.JPEG_TN);
		}
		LOGGER.warn("Could not parse thumbnail DLNAImageProfile from \"{}\"");
		return DLNAImageProfile.JPEG_TN;
	}

	/**
	 * @param fileName the "file name" part of the HTTP request.
	 * @param defaultProfile the {@link DLNAImageProfile} to return if parsing
	 *            fails.
	 * @return The "decoded" {@link ImageProfile} or {@code defaultProfile} if
	 *         the parsing fails.
	 */
	public static DLNAImageProfile parseImageRequest(String fileName, DLNAImageProfile defaultProfile) {
		Matcher matcher = Pattern.compile("^([A-Z]+_(?:(?!R)[A-Z]+|RES_?\\d+[Xx_]\\d+))_").matcher(fileName);
		if (matcher.find()) {
			DLNAImageProfile imageProfile = DLNAImageProfile.toDLNAImageProfile(matcher.group(1));
			if (imageProfile == null) {
				LOGGER.warn("Could not parse DLNAImageProfile from \"{}\"", matcher.group(1));
			} else {
				return imageProfile;
			}
		} else {
			LOGGER.debug("Embedded DLNAImageProfile not found in \"{}\"", fileName);
		}
		return defaultProfile;
	}

	/**
	 * Retrieves the bit depth from an array of bit depths for all components.
	 * The last component's bit depth is allowed to be different from the rest,
	 * but the others must be equal or an {@link InvalidStateException} is
	 * thrown.
	 *
	 * @param bitDepthArray the array of bit depths for all components.
	 * @return The bit depth value if it's constant for all but the last
	 *         component.
	 * @throws InvalidStateException If the bit depth values vary or
	 *             {@code bitDepthArray} is null or empty.
	 */
	public static int getBitDepthFromArray(int[] bitDepthArray) throws InvalidStateException {
		if (bitDepthArray == null || bitDepthArray.length == 0) {
			throw new InvalidStateException("The bit depth array cannot be null or empty");
		}

		try {
			return getConstantIntArrayValue(bitDepthArray);
		} catch (InvalidStateException e) {
			// Allow the last value to be different in it's the alpha
			if (bitDepthArray.length > 1) {
				int[] tmpArray = new int[bitDepthArray.length - 1];
				System.arraycopy(bitDepthArray, 0, tmpArray, 0, bitDepthArray.length - 1);
				return getConstantIntArrayValue(tmpArray);
			}
			throw e;
		}
	}

	/**
	 * Retrieves the bit depth from an array of bit depths for all components.
	 * The last component's bit depth is allowed to be different from the rest,
	 * but the others must be equal or an {@link InvalidStateException} is
	 * thrown.
	 *
	 * @param bitDepthArray the array of bit depths for all components.
	 * @return The bit depth value if it's constant for all but the last
	 *         component.
	 * @throws InvalidStateException If the bit depth values vary or
	 *             {@code bitDepthArray} is null or empty.
	 */
	public static byte getBitDepthFromArray(byte[] bitDepthArray) throws InvalidStateException {
		if (bitDepthArray == null || bitDepthArray.length == 0) {
			throw new InvalidStateException("The bit depth array cannot be null or empty");
		}

		try {
			return getConstantByteArrayValue(bitDepthArray);
		} catch (InvalidStateException e) {
			// Allow the last value to be different in it's the alpha
			if (bitDepthArray.length > 1) {
				byte[] tmpArray = new byte[bitDepthArray.length - 1];
				System.arraycopy(bitDepthArray, 0, tmpArray, 0, bitDepthArray.length - 1);
				return getConstantByteArrayValue(tmpArray);
			}
			throw e;
		}
	}

	/**
	 * Verifies and retrieves a constant integer value from an integer array. If
	 * the {@code intArray}'s values aren't all the same or {@code intArray} is
	 * {@code null} or empty, an {@link InvalidStateException} is thrown.
	 *
	 * @param intArray the integer array from which to extract the constant
	 *            integer value.
	 * @return The constant integer value.
	 * @throws InvalidStateException if the values aren't equal or the array is
	 *             {@code null} or empty.
	 */
	public static int getConstantIntArrayValue(int[] intArray) throws InvalidStateException {
		if (intArray == null || intArray.length == 0) {
			throw new InvalidStateException("The array cannot be null or empty");
		}

		if (intArray.length == 1) {
			return intArray[0];
		}

		Integer result = null;
		for (int i : intArray) {
			if (result == null) {
				result = Integer.valueOf(i);
			} else {
				if (result.intValue() != i) {
					throw new InvalidStateException("The array doesn't have a constant value: " + Arrays.toString(intArray));
				}
			}
		}
		return result.intValue();
	}

	/**
	 * Verifies and retrieves a constant byte value from a byte array. If
	 * the {@code byteArray}'s values aren't all the same or {@code byteArray} is
	 * {@code null} or empty, an {@link InvalidStateException} is thrown.
	 *
	 * @param byteArray the byte array from which to extract the constant
	 *            byte value.
	 * @return The constant byte value.
	 * @throws InvalidStateException if the values aren't equal or the array is
	 *             {@code null} or empty.
	 */
	public static byte getConstantByteArrayValue(byte[] byteArray) throws InvalidStateException {
		if (byteArray == null || byteArray.length == 0) {
			throw new InvalidStateException("The array cannot be null or empty");
		}

		if (byteArray.length == 1) {
			return byteArray[0];
		}

		Byte result = null;
		for (byte b : byteArray) {
			if (result == null) {
				result = Byte.valueOf(b);
			} else {
				if (result.byteValue() != b) {
					throw new InvalidStateException("The array doesn't have a constant value: " + Arrays.toString(byteArray));
				}
			}
		}
		return result.byteValue();
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

		byte SEGMENT_IDENTIFIER = (byte) 0xFF;
		byte MARKER_EOI = (byte) 0xD9;
		byte APP1 = (byte) 0xE1;
		final String EXIF_SEGMENT_PREAMBLE = "Exif\0\0";

        byte segmentIdentifier = reader.getInt8(2);
        byte segmentType = reader.getInt8(3);

		int pos = 4;

        while (
        	segmentIdentifier != SEGMENT_IDENTIFIER ||
        	segmentType != APP1 &&
        	segmentType != MARKER_EOI ||
        	segmentType == APP1 &&
    		!EXIF_SEGMENT_PREAMBLE.equals(new String(
        		reader.getBytes(pos + 2, EXIF_SEGMENT_PREAMBLE.length()),
        		0,
        		EXIF_SEGMENT_PREAMBLE.length(),
        		StandardCharsets.US_ASCII)
        	)
        ) {
        	segmentIdentifier = segmentType;
        	segmentType = reader.getInt8(pos++);
        }

        if (segmentType == MARKER_EOI) {
        	// Reached the end of the image without finding an Exif segment
        	return -1;
        }

        int segmentLength = reader.getUInt16(pos) - 2;
        pos += 2 + EXIF_SEGMENT_PREAMBLE.length();

        if (segmentLength < EXIF_SEGMENT_PREAMBLE.length()) {
        	throw new ParseException("Exif segment is too small");
        }

        int exifHeaderOffset = pos;

        short byteOrderIdentifier = reader.getInt16(pos);
        pos += 4; // Skip TIFF marker

        if (byteOrderIdentifier == 0x4d4d) { // "MM"
            reader.setMotorolaByteOrder(true);
        } else if (byteOrderIdentifier == 0x4949) { // "II"
            reader.setMotorolaByteOrder(false);
        } else {
            throw new ParseException("Can't determine Exif endianness from: 0x" + Integer.toHexString(byteOrderIdentifier));
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

		byte SEGMENT_IDENTIFIER = (byte) 0xFF;
		byte MARKER_EOI = (byte) 0xD9;
		Set<Byte> SOFS = new HashSet<>(Arrays.asList(
			(byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC5,
			(byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA,
			(byte) 0xCB, (byte) 0xCD, (byte) 0xCE, (byte) 0xCF
		));

        byte segmentIdentifier = reader.getInt8(2);
        byte segmentType = reader.getInt8(3);

		int pos = 4;

        while (
        	segmentIdentifier != SEGMENT_IDENTIFIER ||
        	!SOFS.contains(segmentType) &&
        	segmentType != MARKER_EOI
        ) {
        	segmentIdentifier = segmentType;
        	segmentType = reader.getInt8(pos++);
        }

        if (segmentType == MARKER_EOI) {
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
	 * Defines how image scaling is done. {@link #EXACT} will scale the image
	 * to the given resolution if the aspect ratio is the same, or scale the
	 * image so that it fills the given resolution as much as possible while
	 * keeping the aspect ratio if the aspect radio doesn't match. {@link #MAX}
	 * will make sure that the output image doesn't exceed the given
	 * resolution. {@link #MAX} will never upscale, only downscale if required.
	 */
	public enum ScaleType {
		/**
		 * Scale the image to the given resolution while keeping the aspect
		 * ratio. If the aspect ratio mismatch, one axis will be scaled to the
		 * given resolution while the other will be smaller.
		 */
		EXACT,
		/**
		 * Scale the image so that it is no bigger than the given resolution
		 * while keeping aspect ratio. Will never upscale.
		 */
		MAX
	}
}
