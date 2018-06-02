package net.pms.encoders;

import java.io.File;
import java.io.IOException;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.formats.Format;
import net.pms.image.ImageInfo;
import net.pms.io.OutputParams;


public abstract class ImagePlayer extends Player {

	@Override
	public int type() {
		return Format.IMAGE;
	}

	/**
	 * Converts {@code fileName} a {@link ImageIO} readable format.
	 *
	 * @param params the {@link OutputParams} to use.
	 * @param fileName the path of the image file to process.
	 * @param imageInfo the {@link ImageInfo} for the image file.
	 * @return A byte array containing the converted image or {@code null}.
	 * @throws IOException if an IO error occurs.
	 */
	public abstract byte[] getImage(OutputParams params, String fileName, ImageInfo imageInfo);

	/**
	 * Extracts or generates a thumbnail for {@code fileName}.
	 *
	 * @param params the {@link OutputParams} to use.
	 * @param fileName the path of the image file to process.
	 * @param imageInfo the {@link ImageInfo} for the image file.
	 * @return A byte array containing the thumbnail or {@code null}.
	 * @throws IOException if an IO error occurs.
	 */
	public abstract byte[] getThumbnail(OutputParams params, String fileName, ImageInfo imageInfo);

	/**
	 * Parses {@code file} and stores the result in {@code media}.
	 *
	 * @param media the {@link DLNAMediaInfo} instance to store the parse
	 *            results in.
	 * @param file the {@link File} to parse.
	 */
    public abstract void parse(DLNAMediaInfo media, File file);
}
