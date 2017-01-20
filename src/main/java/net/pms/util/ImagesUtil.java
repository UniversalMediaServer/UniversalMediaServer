package net.pms.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.util.exif.ExifFilterUtils;
import net.pms.dlna.DLNAImage;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAImageProfile.DLNAComplianceResult;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.ImageInfo;
import net.pms.formats.ImageFormat;
import net.pms.image.ImageIORuntimeException;
import net.pms.util.CustomImageReader.ImageReaderResult;
import org.apache.commons.io.IOUtils;
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
import com.drew.imaging.tiff.TiffProcessingException;
import com.drew.imaging.tiff.TiffReader;
import com.drew.imaging.webp.WebpMetadataReader;
import com.drew.lang.RandomAccessFileReader;
import com.drew.lang.RandomAccessStreamReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.exif.ExifTiffHandler;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.ico.IcoDirectory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.pcx.PcxDirectory;
import com.drew.metadata.photoshop.PsdHeaderDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;

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
			if (inputStream.isFullResetAvailable()) {
				inputStream.fullReset();
			} else {
				// If we can't reset it, close it and create a new
				inputStream.close();
				inputStream = new ResettableInputStream(Files.newInputStream(file.toPath()), MAX_BUFFER);
			}
			ImageInfo imageInfo = null;
			try {
				imageInfo = CustomImageReader.readImageInfo(inputStream, size , metadata, true);
				// From this point on metadata is Exif orientation compensated.
			} catch (UnknownFormatException | IIOException e) {
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
						imageInfo = new ImageInfo(metadata, format, size, true, true);
					} catch (MetadataException me) {
						LOGGER.warn("Unable to parse metadata for \"{}\": {}", file.getAbsolutePath(), me.getMessage());
						LOGGER.trace("", me);
					}
				}
			}

			if (imageInfo == null && format == null) {
				throw new IOException("Parsing of \"" + file.getAbsolutePath() + "\" failed");
			}

			if (format == null) {
				format = imageInfo.getFormat();
			} else if (imageInfo != null && imageInfo.getFormat() != null && format != imageInfo.getFormat()) {
				if (imageInfo.getFormat() == ImageFormat.TIFF && format.isRaw()) {
					if (format == ImageFormat.ARW && !isARW(metadata)) {
						// XXX Remove this if https://github.com/drewnoakes/metadata-extractor/issues/217 is fixed
						// Metadata extractor misidentifies some Photoshop created TIFFs for ARW, correct it
						format = ImageFormat.TIFF;
						LOGGER.trace(
							"Correcting misidentified image format ARW to TIFF for \"{}\"",
							file.getAbsolutePath()
						);
					} else {
						/*
						 * ImageIO recognizes many RAW formats as TIFF because
						 * of their close relationship let's treat them as what
						 * they really are.
						 */
						imageInfo = new ImageInfo(
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
	 * Tries to parse {@link ExifOrientation} from the given metadata. If it
	 * fails, {@link ExifOrientation#TOP_LEFT} is returned.
	 *
	 * @param metadata the {@link Metadata} to parse.
	 * @return The parsed {@link ExifOrientation} or
	 *         {@link ExifOrientation#TOP_LEFT}.
	 */
	public static ExifOrientation parseExifOrientation(Metadata metadata) {
		if (metadata == null) {
			return ExifOrientation.TOP_LEFT;
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
			return ExifOrientation.TOP_LEFT;
		}
		return ExifOrientation.TOP_LEFT;
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
	 * @param metadata the {@link Metadata} whose Exif orientation to evaluate.
	 * @return {@code true} if the axes should be swapped, {@code false}
	 *         otherwise.
	 */
	public static boolean isExifAxesSwapNeeded(Metadata metadata) {
		return metadata != null && isExifAxesSwapNeeded(parseExifOrientation(metadata));
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
	 * <b>Use this method with care as it is a little bit tricky</b>. This
	 * method returns a new {@link ImageInfo} instance if any changes are
	 * required. If not, the argument is returned. If changes are needed, the
	 * old {@link ImageInfo} instance is left untouched but its {@link Metadata}
	 * instance is altered. This is because there's no way to copy/clone a
	 * {@link Metadata} instance. The existing {@link Metadata} instance is
	 * therefore altered and returned with the new {@link ImageInfo} instance -
	 * which effectively invalidates the old {@link ImageInfo} instance.
	 *
	 * <b>In short, make sure not to use the {@link ImageInfo} argument after
	 * calling this method, use the returned instance instead</b>
	 *
	 * This method transforms the {@link ImageInfo} instance and its
	 * {@link Metadata} instance as if the image was transformed according to
	 * Exif orientation. Resets Exif orientation to 1. Please note: This swaps
	 * the axes of the basic tags, custom maker note tags etc. is beyond the
	 * scope of this method. Thumbnail axes are also swapped.
	 *
	 * @param imageInfo the {@link ImageInfo} to transform according to its Exif
	 *            orientation.
	 */
	public static ImageInfo swapAxesIfNeeded(ImageInfo imageInfo) {
		if (imageInfo != null && imageInfo.getExifOrientation() != ExifOrientation.TOP_LEFT) {
			return imageInfo.copy(imageInfo.getMetadata(), true);
		}
		return imageInfo;
	}

	/**
	 * Transforms the {@link Metadata} instance as if the image was transformed
	 * according to Exif orientation. Resets Exif orientation to 1. Please note:
	 * This swaps the axes of the basic tags, custom maker note tags etc. is
	 * beyond the scope of this method. Thumbnail axes are also swapped.
	 *
	 * @param metadata the {@link Metadata} to transform according to its Exif
	 *            orientation.
	 */
	public static void swapAxesIfNeeded(Metadata metadata) {
		if (isExifAxesSwapNeeded(metadata)) {
			swapAxes(metadata);
		} else if (metadata != null) {
			for (Directory directory : metadata.getDirectories()) {
				if (directory instanceof ExifDirectoryBase ) {
					if (
						((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_ORIENTATION)
					) {
						directory.setInt(ExifDirectoryBase.TAG_ORIENTATION, 1);
					}
				}
			}
		}
	}

	/**
	 * Swaps the axes in the {@link Metadata} instance and resets Exif
	 * orientation to 1. Please note: This swaps the axes of the basic tags,
	 * custom maker note tags etc. is beyond the scope of this method. Thumbnail
	 * axes are also swapped.
	 *
	 * @param metadata the {@link Metadata} instance to perform the axes swap
	 *            on.
	 */
	public static void swapAxes(Metadata metadata) {
		for (Directory directory : metadata.getDirectories()) {
			if (directory instanceof PngDirectory) {
				if (
					((PngDirectory) directory).containsTag(PngDirectory.TAG_IMAGE_WIDTH) &&
					((PngDirectory) directory).containsTag(PngDirectory.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((PngDirectory) directory).getObject(PngDirectory.TAG_IMAGE_WIDTH);
					directory.setObject(PngDirectory.TAG_IMAGE_WIDTH, ((PngDirectory) directory).getObject(PngDirectory.TAG_IMAGE_HEIGHT));
					directory.setObject(PngDirectory.TAG_IMAGE_HEIGHT, value);
				}
				if (
					((PngDirectory) directory).containsTag(PngDirectory.TAG_PIXELS_PER_UNIT_X) &&
					((PngDirectory) directory).containsTag(PngDirectory.TAG_PIXELS_PER_UNIT_Y)
				) {
					Object value = ((PngDirectory) directory).getObject(PngDirectory.TAG_PIXELS_PER_UNIT_X);
					directory.setObject(PngDirectory.TAG_PIXELS_PER_UNIT_X, ((PngDirectory) directory).getObject(PngDirectory.TAG_PIXELS_PER_UNIT_Y));
					directory.setObject(PngDirectory.TAG_PIXELS_PER_UNIT_Y, value);
				}
			} else if (directory instanceof JpegDirectory) {
				if (
					((JpegDirectory) directory).containsTag(JpegDirectory.TAG_IMAGE_WIDTH) &&
					((JpegDirectory) directory).containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((JpegDirectory) directory).getObject(JpegDirectory.TAG_IMAGE_WIDTH);
					directory.setObject(JpegDirectory.TAG_IMAGE_WIDTH, ((JpegDirectory) directory).getObject(JpegDirectory.TAG_IMAGE_HEIGHT));
					directory.setObject(JpegDirectory.TAG_IMAGE_HEIGHT, value);
				}
			} else if (directory instanceof ExifDirectoryBase ) {
				if (
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_IMAGE_WIDTH) &&
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((ExifDirectoryBase) directory).getObject(ExifDirectoryBase.TAG_IMAGE_WIDTH);
					directory.setObject(ExifDirectoryBase.TAG_IMAGE_WIDTH, ((ExifDirectoryBase) directory).getObject(ExifDirectoryBase.TAG_IMAGE_HEIGHT));
					directory.setObject(ExifDirectoryBase.TAG_IMAGE_HEIGHT, value);
				}

				if (
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_RELATED_IMAGE_WIDTH) &&
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_RELATED_IMAGE_HEIGHT)
				) {
					Object value = ((ExifDirectoryBase) directory).getObject(ExifDirectoryBase.TAG_RELATED_IMAGE_WIDTH);
					directory.setObject(ExifDirectoryBase.TAG_RELATED_IMAGE_WIDTH, ((ExifDirectoryBase) directory).getObject(ExifDirectoryBase.TAG_RELATED_IMAGE_HEIGHT));
					directory.setObject(ExifDirectoryBase.TAG_RELATED_IMAGE_HEIGHT, value);
				}

				if (
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH) &&
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT)
				) {
					Object value = ((ExifDirectoryBase) directory).getObject(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH);
					directory.setObject(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH, ((ExifDirectoryBase) directory).getObject(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT));
					directory.setObject(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT, value);
				}
				if (
					((ExifDirectoryBase) directory).containsTag(ExifDirectoryBase.TAG_ORIENTATION)
				) {
					directory.setInt(ExifDirectoryBase.TAG_ORIENTATION, 1);
				}
			} else if (directory instanceof GifHeaderDirectory) {
				if (
					((GifHeaderDirectory) directory).containsTag(GifHeaderDirectory.TAG_IMAGE_WIDTH) &&
					((GifHeaderDirectory) directory).containsTag(GifHeaderDirectory.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((GifHeaderDirectory) directory).getObject(GifHeaderDirectory.TAG_IMAGE_WIDTH);
					directory.setObject(GifHeaderDirectory.TAG_IMAGE_WIDTH, ((GifHeaderDirectory) directory).getObject(GifHeaderDirectory.TAG_IMAGE_HEIGHT));
					directory.setObject(GifHeaderDirectory.TAG_IMAGE_HEIGHT, value);
				}
			} else if (directory instanceof BmpHeaderDirectory) {
				if (
					((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_IMAGE_WIDTH) &&
					((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((BmpHeaderDirectory) directory).getObject(BmpHeaderDirectory.TAG_IMAGE_WIDTH);
					directory.setObject(BmpHeaderDirectory.TAG_IMAGE_WIDTH, ((BmpHeaderDirectory) directory).getObject(BmpHeaderDirectory.TAG_IMAGE_HEIGHT));
					directory.setObject(BmpHeaderDirectory.TAG_IMAGE_HEIGHT, value);
				}
				if (
					((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_X_PIXELS_PER_METER) &&
					((BmpHeaderDirectory) directory).containsTag(BmpHeaderDirectory.TAG_Y_PIXELS_PER_METER)
				) {
					Object value = ((BmpHeaderDirectory) directory).getObject(BmpHeaderDirectory.TAG_X_PIXELS_PER_METER);
					directory.setObject(BmpHeaderDirectory.TAG_X_PIXELS_PER_METER, ((BmpHeaderDirectory) directory).getObject(BmpHeaderDirectory.TAG_Y_PIXELS_PER_METER));
					directory.setObject(BmpHeaderDirectory.TAG_Y_PIXELS_PER_METER, value);
				}
			} else if (directory instanceof IcoDirectory) {
				if (
					((IcoDirectory) directory).containsTag(IcoDirectory.TAG_IMAGE_WIDTH) &&
					((IcoDirectory) directory).containsTag(IcoDirectory.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((IcoDirectory) directory).getObject(IcoDirectory.TAG_IMAGE_WIDTH);
					directory.setObject(IcoDirectory.TAG_IMAGE_WIDTH, ((IcoDirectory) directory).getObject(IcoDirectory.TAG_IMAGE_HEIGHT));
					directory.setObject(IcoDirectory.TAG_IMAGE_HEIGHT, value);
				}
				if (
					((IcoDirectory) directory).containsTag(IcoDirectory.TAG_CURSOR_HOTSPOT_X) &&
					((IcoDirectory) directory).containsTag(IcoDirectory.TAG_CURSOR_HOTSPOT_Y)
				) {
					Object value = ((IcoDirectory) directory).getObject(IcoDirectory.TAG_CURSOR_HOTSPOT_X);
					directory.setObject(IcoDirectory.TAG_CURSOR_HOTSPOT_X, ((IcoDirectory) directory).getObject(IcoDirectory.TAG_CURSOR_HOTSPOT_Y));
					directory.setObject(IcoDirectory.TAG_CURSOR_HOTSPOT_Y, value);
				}
			} else if (directory instanceof JfifDirectory) {
				if (
					((JfifDirectory) directory).containsTag(JfifDirectory.TAG_RESX) &&
					((JfifDirectory) directory).containsTag(JfifDirectory.TAG_RESY)
				) {
					Object value = ((JfifDirectory) directory).getObject(JfifDirectory.TAG_RESX);
					directory.setObject(JfifDirectory.TAG_RESX, ((JfifDirectory) directory).getObject(JfifDirectory.TAG_RESY));
					directory.setObject(JfifDirectory.TAG_RESY, value);
				}
				if (
					((JfifDirectory) directory).containsTag(JfifDirectory.TAG_THUMB_WIDTH) &&
					((JfifDirectory) directory).containsTag(JfifDirectory.TAG_THUMB_HEIGHT)
				) {
					Object value = ((JfifDirectory) directory).getObject(JfifDirectory.TAG_THUMB_WIDTH);
					directory.setObject(JfifDirectory.TAG_THUMB_WIDTH, ((JfifDirectory) directory).getObject(JfifDirectory.TAG_THUMB_HEIGHT));
					directory.setObject(JfifDirectory.TAG_THUMB_HEIGHT, value);
				}
			} else if (directory instanceof PcxDirectory) {
				if (
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_XMIN) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_XMAX) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_YMIN) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_YMAX)
				) {
					Object value = ((PcxDirectory) directory).getObject(PcxDirectory.TAG_XMIN);
					directory.setObject(PcxDirectory.TAG_XMIN, ((PcxDirectory) directory).getObject(PcxDirectory.TAG_YMIN));
					directory.setObject(PcxDirectory.TAG_YMIN, value);
					value = ((PcxDirectory) directory).getObject(PcxDirectory.TAG_XMAX);
					directory.setObject(PcxDirectory.TAG_XMAX, ((PcxDirectory) directory).getObject(PcxDirectory.TAG_YMAX));
					directory.setObject(PcxDirectory.TAG_YMAX, value);
				}
				if (
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_HORIZONTAL_DPI) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_VERTICAL_DPI)
				) {
					Object value = ((PcxDirectory) directory).getObject(PcxDirectory.TAG_HORIZONTAL_DPI);
					directory.setObject(PcxDirectory.TAG_HORIZONTAL_DPI, ((PcxDirectory) directory).getObject(PcxDirectory.TAG_VERTICAL_DPI));
					directory.setObject(PcxDirectory.TAG_VERTICAL_DPI, value);
				}
				if (
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_HSCR_SIZE) &&
					((PcxDirectory) directory).containsTag(PcxDirectory.TAG_VSCR_SIZE)
				) {
					Object value = ((PcxDirectory) directory).getObject(PcxDirectory.TAG_HSCR_SIZE);
					directory.setObject(PcxDirectory.TAG_HSCR_SIZE, ((PcxDirectory) directory).getObject(PcxDirectory.TAG_VSCR_SIZE));
					directory.setObject(PcxDirectory.TAG_VSCR_SIZE, value);
				}
			} else if (directory instanceof PsdHeaderDirectory) {
				if (
					((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_IMAGE_WIDTH) &&
					((PsdHeaderDirectory) directory).containsTag(PsdHeaderDirectory.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((PsdHeaderDirectory) directory).getObject(PsdHeaderDirectory.TAG_IMAGE_WIDTH);
					directory.setObject(PsdHeaderDirectory.TAG_IMAGE_WIDTH, ((PsdHeaderDirectory) directory).getObject(PsdHeaderDirectory.TAG_IMAGE_HEIGHT));
					directory.setObject(PsdHeaderDirectory.TAG_IMAGE_HEIGHT, value);
				}
			} else if (directory instanceof WebpDirectory) {
				if (
					((WebpDirectory) directory).containsTag(WebpDirectory.TAG_IMAGE_WIDTH) &&
					((WebpDirectory) directory).containsTag(WebpDirectory.TAG_IMAGE_HEIGHT)
				) {
					Object value = ((WebpDirectory) directory).getObject(WebpDirectory.TAG_IMAGE_WIDTH);
					directory.setObject(WebpDirectory.TAG_IMAGE_WIDTH, ((WebpDirectory) directory).getObject(WebpDirectory.TAG_IMAGE_HEIGHT));
					directory.setObject(WebpDirectory.TAG_IMAGE_HEIGHT, value);
				}
			}
		}
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
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputImage the source {@link Image}.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail
	) throws IOException {
		return transcodeImage(inputImage, 0, 0, null, outputFormat, updateMetadata, dlnaCompliant, dlnaThumbnail, false);
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
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail
	) throws IOException {
		return transcodeImage(inputStream, 0, 0, null, outputFormat, updateMetadata, dlnaCompliant, dlnaThumbnail, false);
	}

	/**
	 * Converts an image to a different {@link ImageFormat}. Rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail
	) throws IOException {
		return transcodeImage(inputByteArray, 0, 0, null, outputFormat, updateMetadata, dlnaCompliant, dlnaThumbnail, false);
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
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputImage, width, height, scaleType, ImageFormat.SOURCE, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio and rotates/flips the image according to
	 * Exif orientation. Format support is limited to that of {@link ImageIO}.
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width.
	 * @param height the new height.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputStream, width, height, scaleType, ImageFormat.SOURCE, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
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
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputByteArray, width, height, scaleType, ImageFormat.SOURCE, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputImage the source {@link Image}.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputImage, 0, 0, null, outputProfile, updateMetadata, true, dlnaThumbnail, padToSize);
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
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputStream, 0, 0, null, outputProfile, updateMetadata, true, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputByteArray, 0, 0, null, outputProfile, updateMetadata, true, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(null, inputImage, null, width, height, scaleType, outputFormat, null, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(null, null, inputStream, width, height, scaleType, outputFormat, null, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputByteArray, null, null, width, height, scaleType, outputFormat, null, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(null, inputImage, null, width, height, scaleType, null, outputProfile, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(null, null, inputStream, width, height, scaleType, null, outputProfile, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcodeImage(inputByteArray, null, null, width, height, scaleType, null, outputProfile, updateMetadata, dlnaCompliant, dlnaThumbnail, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
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
	 * @param updateMetadata whether or not new metadata should be updated after
	 *            image transformation. This should only be disabled if the
	 *            output image won't be kept/reused.
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
		boolean updateMetadata,
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
		ImageIO.setUseCache(false);

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
			inputResult = CustomImageReader.read(new ByteArrayInputStream(inputByteArray));
		} catch (IIOException e) {
			throw new UnknownFormatException("Unable to read image format", e);
		}

		if (inputResult.bufferedImage == null || inputResult.imageFormat == null) { // ImageIO doesn't support the image format
			throw new UnknownFormatException("Failed to transform image because the source format is unknown");
		}

		if (outputFormat == null || outputFormat == ImageFormat.SOURCE) {
			outputFormat = inputResult.imageFormat;
		}

		if (outputProfile == null && (dlnaCompliant || dlnaThumbnail)) {
			// Override output format to one valid for DLNA, defaulting to JPEG
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
					outputFormat = ImageFormat.JPEG;
			}
		}

		Metadata metadata = null;
		try {
			metadata = inputImage != null ? inputImage.getMetadata() : getMetadata(inputByteArray, inputResult.imageFormat);
		} catch (IOException | ImageProcessingException e) {
			LOGGER.error("Failed to read input image metadata: {}", e.getMessage());
			LOGGER.trace("", e);
			metadata = new Metadata();
		}
		if (metadata == null) {
			metadata = new Metadata();
		}

		BufferedImage bufferedImage = inputResult.bufferedImage;
		boolean reencode = false;

		ExifOrientation orientation = parseExifOrientation(metadata);
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

		boolean convertColors =
			bufferedImage.getType() == BufferedImageType.TYPE_CUSTOM.getTypeId() ||
			bufferedImage.getColorModel().getColorSpace().getType() != ColorSpaceType.TYPE_RGB.getTypeId();

		// Impose DLNA format restrictions
		if (
			outputFormat == inputResult.imageFormat &&
			(
				dlnaCompliant ||
				dlnaThumbnail ||
				outputProfile != null
			)
		) {
			DLNAComplianceResult complianceResult;
			switch (outputFormat) {
				case GIF:
				case JPEG:
				case PNG:
					ImageInfo imageInfo = new ImageInfo(
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						inputResult.imageFormat,
						ImageInfo.SIZE_UNKNOWN,
						bufferedImage.getColorModel(),
						metadata,
						false,
						true
					);
					if (outputProfile != null) {
						complianceResult = DLNAImageProfile.checkCompliance(imageInfo, outputProfile);
					} else {
						complianceResult = DLNAImageProfile.checkCompliance(imageInfo, outputFormat);
					}
					break;
				default:
					throw new IllegalStateException("Unexpected image format: " + outputFormat);
			}
			convertColors |= !complianceResult.isColorsCorrect();
			reencode = convertColors || !complianceResult.isFormatCorrect();
			if (!complianceResult.isResolutionCorrect()) {
				width = width > 0 && complianceResult.getMaxWidth() > 0 ?
					Math.min(width, complianceResult.getMaxWidth()) :
						width > 0 ? width : complianceResult.getMaxWidth();
				height = height > 0 && complianceResult.getMaxHeight() > 0 ?
					Math.min(height, complianceResult.getMaxHeight()) :
						height > 0 ? height : complianceResult.getMaxHeight();
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

		if (width == 0 ||
			height == 0 ||
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
			if (!reencode && inputResult.imageFormat.equals(outputFormat)) {
				// Nothing to do, just return source
				Image result;
				if (dlnaThumbnail) {
					result = new DLNAThumbnail(inputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
				} else if (dlnaCompliant) {
					result = new DLNAImage(inputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
				} else {
					result = new Image(inputByteArray, outputFormat, bufferedImage, metadata, false);
				}
				bufferedImage.flush();
				return result;
			}
		} else {
			boolean force = outputProfile != null && DLNAImageProfile.JPEG_RES_H_V.equals(outputProfile);
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
		if (updateMetadata) {
			try {
				metadata = getMetadata(outputByteArray, outputFormat);
			} catch (IOException | ImageProcessingException e) {
				LOGGER.error("Failed to read output image metadata: {}", e.getMessage());
				LOGGER.trace("", e);
				metadata = new Metadata();
			}
		} else {
			metadata = null;
		}

		Image result;
		if (dlnaThumbnail) {
			result = new DLNAThumbnail(outputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
		} else if (dlnaCompliant) {
			result = new DLNAImage(outputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
		} else {
			result = new Image(outputByteArray, outputFormat, bufferedImage, metadata, false);
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

		// First check if there are any ExifThumbnailDirectory
		Collection<ExifThumbnailDirectory> directories = metadata.getDirectoriesOfType(ExifThumbnailDirectory.class);
		if (directories.isEmpty()) {
			return null;
		}

		// The Metadata instance we have didn't store the thumbnail data, read them again
		metadata = new Metadata();
		try {
	        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

	        try {
	            ExifTiffHandler handler = new ExifTiffHandler(metadata, true, null);
	            new TiffReader().processTiff(new RandomAccessFileReader(randomAccessFile), handler, 0);
	        } finally {
	            randomAccessFile.close();
	        }
		} catch (IOException | TiffProcessingException e) {
			LOGGER.debug("Error while reading \"{}\" metadata: {}", file.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
			return null;
		}

		// Now get the thumbnail data if they're there
		directories = metadata.getDirectoriesOfType(ExifThumbnailDirectory.class);
		for (ExifThumbnailDirectory directory : directories) {
			if (directory.hasThumbnailData()) {
				return directory.getThumbnailData();
			}
		}

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
				// Return an empty Metadata instance for unsupported formats
				case Crw:
				case Unknown:
				default:
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
		} else {
			LOGGER.warn("Could not parse thumbnail DLNAImageProfile from \"{}\"");
			return DLNAImageProfile.JPEG_TN;
		}
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

	//TODO (Nad) Move/Rename
	/**
	 * This is a wrapper around
	 * {@link ImageIO#write(RenderedImage, String, OutputStream)}
	 * that translate any thrown {@link RuntimeException} to an
	 * {@link ImageIORuntimeException} because {@link ImageIO} has the nasty
	 * habit of throwing {@link RuntimeException}s when something goes wrong.
	 *
	 * @see ImageIO#write(RenderedImage, String, OutputStream)
	 */
	public static boolean imageIOWrite(RenderedImage im, String formatName, OutputStream output) throws IOException {
		try {
			return ImageIO.write(im, formatName, output);
		} catch (RuntimeException e) {
			throw new ImageIORuntimeException(e.getMessage(), e);
		}
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
