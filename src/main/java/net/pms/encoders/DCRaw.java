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
package net.pms.encoders;

import com.drew.lang.ByteArrayReader;
import com.sun.jna.Platform;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.util.exif.ExifFilterUtils;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.formats.Format;
import net.pms.image.ExifInfo;
import net.pms.image.ExifOrientation;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.io.InternalJavaProcessImpl;
import net.pms.io.ListProcessWrapperResult;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.SimpleProcessWrapper;
import net.pms.media.MediaInfo;
import net.pms.network.HTTPResource;
import net.pms.parsers.MetadataExtractorParser;
import net.pms.platform.windows.NTStatus;
import net.pms.store.StoreItem;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExecutableInfo.ExecutableInfoBuilder;
import net.pms.util.Version;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DCRaw extends ImageEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(DCRaw.class);
	public static final EngineId ID = StandardEngineId.DCRAW;

	/** The {@link Configuration} key for the custom DCRaw path. */
	public static final String KEY_DCRAW_PATH = "dcraw_path";

	/** The {@link Configuration} key for the DCRaw executable type. */
	public static final String KEY_DCRAW_EXECUTABLE_TYPE = "dcraw_executable_type";
	public static final String NAME = "DCRaw";

	// Not to be instantiated by anything but PlayerFactory
	DCRaw() {
		super(CONFIGURATION.getDCRawPaths());
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getConfigurablePathKey() {
		return KEY_DCRAW_PATH;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_DCRAW_EXECUTABLE_TYPE;
	}

	@Override
	public ProcessWrapper launchTranscode(
		StoreItem resource,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		if (media == null || resource == null) {
			return null;
		}

		final String filename = resource.getFileName();
		byte[] image = getImage(params, filename, media.getImageInfo());

		if (image == null) {
			return null;
		}
		return new InternalJavaProcessImpl(new ByteArrayInputStream(image));
	}

	@Override
	public String getMimeType() {
		return HTTPResource.JPEG_TYPEMIME;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int purpose() {
		return MISC_ENGINE;
	}

	/**
	 * Converts {@code fileName} into PPM format.
	 *
	 * @param params the {@link OutputParams} to use. Can be {@code null}.
	 * @param fileName the path of the image file to process.
	 * @param imageInfo the {@link ImageInfo} for the image file. Can be {@code null}.
	 * @return A byte array containing the converted image or {@code null}.
	 */
	@Override
	public byte[] getImage(OutputParams params, String fileName, ImageInfo imageInfo) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Decoding image \"{}\" with DCRaw", fileName);
		}
		if (params == null) {
			params = new OutputParams(PMS.getConfiguration());
		}

		params.setLog(false);

		// Setting the buffer to the size of the source file or 5 MB. The
		// output won't be the same size as the input, but it will hopefully
		// give us a somewhat relevant buffer size. Every time the buffer has
		// to grow, the whole buffer must be copied in memory.
		params.setOutputByteArrayStreamBufferSize(imageInfo != null &&
			imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ?
			(int) imageInfo.getSize() : 5000000);

		// First try to get the embedded thumbnail
		String[] cmdArray = new String[5];
		cmdArray[0] = EngineFactory.getEngineExecutable(ID);
		cmdArray[1] = "-c";
		cmdArray[2] = "-M";
		cmdArray[3] = "-w";
		cmdArray[4] = fileName;
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, true, params, false, true);
		pw.runInSameThread();
		byte[] bytes = pw.getOutputByteArray().toByteArray();
		List<String> results = pw.getResults();

		if (bytes == null || bytes.length == 0) {
			if (!results.isEmpty() && results.get(0).startsWith("Cannot decode file")) {
				LOGGER.warn("DCRaw could not decode image \"{}\"", fileName);
			} else if (!results.isEmpty()) {
				LOGGER.debug("DCRaw failed to decode image \"{}\": {}", fileName, StringUtils.join(results, "\n"));
			}
			return null;
		}
		return bytes;
	}

	/**
	 * Extracts or generates a thumbnail for {@code fileName}.
	 *
	 * @param params the {@link OutputParams} to use. Can be {@code null}.
	 * @param fileName the path of the image file to process.
	 * @param imageInfo the {@link ImageInfo} for the image file.
	 * @return A byte array containing the thumbnail or {@code null}.
	 */
	@Override
	public byte[] getThumbnail(OutputParams params, String fileName, ImageInfo imageInfo) {
		boolean trace = LOGGER.isTraceEnabled();
		if (trace) {
			LOGGER.trace("Extracting thumbnail from \"{}\" with DCRaw", fileName);
		}
		if (params == null) {
			params = new OutputParams(PMS.getConfiguration());
		}

		params.setLog(false);

		// This is a wild guess at a decent buffer size for an embedded thumbnail.
		// Every time the buffer has to grow, the whole buffer must be copied in memory.
		params.setOutputByteArrayStreamBufferSize(150000);

		// First try to get the embedded thumbnail
		String[] cmdArray = new String[6];
		cmdArray[0] = EngineFactory.getEngineExecutable(ID);
		cmdArray[1] = "-e";
		cmdArray[2] = "-c";
		cmdArray[3] = "-M";
		cmdArray[4] = "-w";
		cmdArray[5] = fileName;
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, true, params, false, true);
		pw.runInSameThread();
		byte[] bytes = pw.getOutputByteArray().toByteArray();

		List<String> results = pw.getResults();

		if (bytes.length > 0) {
			// DCRaw doesn't seem to apply Exif Orientation to embedded thumbnails, handle it
			boolean isJPEG = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
			ExifOrientation thumbnailOrientation = null;
			Dimension jpegResolution = null;
			int exifOrientationOffset = -1;
			if (isJPEG) {
				try {
					ByteArrayReader reader = new ByteArrayReader(bytes);
					exifOrientationOffset = MetadataExtractorParser.getJPEGExifIFDTagOffset(0x112, reader);
					jpegResolution = MetadataExtractorParser.getJPEGResolution(reader);
				} catch (IOException e) {
					exifOrientationOffset = -1;
					LOGGER.debug(
						"Unexpected error while trying to find Exif orientation offset in embedded thumbnail for \"{}\": {}",
						fileName,
						e.getMessage());
					LOGGER.trace("", e);
				}
				if (exifOrientationOffset > 0) {
					thumbnailOrientation = ExifOrientation.typeOf(bytes[exifOrientationOffset]);
				} else {
					LOGGER.debug("Couldn't find Exif orientation in the thumbnail extracted from \"{}\"", fileName);
				}
			}
			ExifOrientation imageOrientation = imageInfo instanceof ExifInfo ?
				((ExifInfo) imageInfo).getOriginalExifOrientation() : null;

			// There might be required to impose specific rules depending on the (RAW) format here

			if (imageOrientation != null && imageOrientation != thumbnailOrientation) {
				if (thumbnailOrientation != null && imageInfo != null) {
					if (
						imageInfo.getWidth() > 0 &&
						imageInfo.getHeight() > 0 &&
						jpegResolution != null &&
						jpegResolution.getWidth() > 0 &&
						jpegResolution.getHeight() > 0
					) {
						// Try to determine which orientation to trust
						double imageAspect;
						double thumbnailAspect;
						if (ImagesUtil.isExifAxesSwapNeeded(imageOrientation)) {
							imageAspect = (double) imageInfo.getHeight() / imageInfo.getWidth();
						} else {
							imageAspect = (double) imageInfo.getWidth() / imageInfo.getHeight();
						}
						if (ImagesUtil.isExifAxesSwapNeeded(thumbnailOrientation)) {
							thumbnailAspect = jpegResolution.getHeight() / jpegResolution.getWidth();
						} else {
							thumbnailAspect = jpegResolution.getWidth() / jpegResolution.getHeight();
						}

						if (Math.abs(imageAspect - thumbnailAspect) > 0.001d) {
							// The image and the thumbnail seems to have different aspect ratios, use that of the image
							bytes[exifOrientationOffset] = (byte) imageOrientation.getValue();
						}
					}
				} else if (imageOrientation != ExifOrientation.TOP_LEFT) {
					// Apply the orientation to the thumbnail
					try {
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						Thumbnails.of(new ByteArrayInputStream(bytes))
							.scale(1.0d)
							.addFilter(ExifFilterUtils.getFilterForOrientation(imageOrientation.getThumbnailatorOrientation()))
							.outputFormat("PNG") // PNG here to avoid further degradation from rotation
							.outputQuality(1.0f)
							.toOutputStream(outputStream);
						bytes = outputStream.toByteArray();
					} catch (IOException e) {
						LOGGER.error(
							"Unexpected error when trying to rotate thumbnail for \"{}\" - cancelling rotation: {}",
							fileName,
							e.getMessage()
						);
						LOGGER.trace("", e);
					}
				}

			}
		}

		if (bytes.length == 0 || !results.isEmpty() && results.get(0).contains("has no thumbnail")) {
			// No embedded thumbnail retrieved, generate thumbnail from the actual file
			if (trace) {
				LOGGER.trace(
					"No embedded thumbnail found in \"{}\", " +
					"trying to generate thumbnail from the image itself",
					fileName
				);
			}
			params.setOutputByteArrayStreamBufferSize(imageInfo != null &&
				imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ?
				(int) imageInfo.getSize() / 4 : 500000);
			cmdArray[1] = "-h";
			pw = new ProcessWrapperImpl(cmdArray, true, params);

			pw.runInSameThread();
			bytes = pw.getOutputByteArray().toByteArray();
		}
		if (trace && (bytes == null || bytes.length == 0)) {
			LOGGER.trace("Failed to generate thumbnail with DCRaw for image \"{}\"", fileName);
		}
		return bytes != null && bytes.length > 0 ? bytes : null;
	}

	@Override
	public boolean isCompatible(StoreItem resource) {
		return resource != null && resource.getFormat() != null && resource.getFormat().getIdentifier() == Format.Identifier.RAW;
	}

	@Override
	public boolean isCompatible(EncodingFormat encodingFormat) {
		return true;
	}

	@Override
	public boolean excludeFormat(Format extension) {
		return false;
	}

	@Override
	public @Nullable ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo) {
		executableInfo = testExecutableFile(executableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		ExecutableInfoBuilder result = executableInfo.modify();
		try {
			ListProcessWrapperResult output = SimpleProcessWrapper.runProcessListOutput(
				30000,
				1000,
				executableInfo.getPath().toString()
			);
			if (output.getError() != null) {
				result.errorType(ExecutableErrorType.GENERAL);
				result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + " \n" + output.getError().getMessage());
				result.available(Boolean.FALSE);
				LOGGER.debug("\"{}\" failed with error: {}", executableInfo.getPath(), output.getError().getMessage());
				return result.build();
			}
			if (!output.getOutput().isEmpty() && StringUtils.isBlank(output.getOutput().get(0))) {
				if (output.getOutput().size() > 1) {
					Pattern pattern = Pattern.compile("decoder\\s\"dcraw\"\\s(\\S+)", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(output.getOutput().get(1));
					if (matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
						result.version(new Version(matcher.group(1)));
					}
				}
				result.available(Boolean.TRUE);
			} else if (!output.getOutput().isEmpty()) {
				result.errorType(ExecutableErrorType.GENERAL);
				result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + " \n" + output.getOutput().get(0));
				result.available(Boolean.FALSE);
			} else {
				NTStatus ntStatus = Platform.isWindows() ? NTStatus.typeOf(output.getExitCode()) : null;
				if (ntStatus != null && ntStatus != NTStatus.STATUS_SUCCESS) {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + "\n\n" + ntStatus);
				} else {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + Messages.getString("UnknownError"));
				}
				result.available(Boolean.FALSE);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}
}
