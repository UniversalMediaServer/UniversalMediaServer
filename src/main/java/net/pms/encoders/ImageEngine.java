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

import java.io.File;
import net.pms.formats.Format;
import net.pms.image.ImageInfo;
import net.pms.io.OutputParams;
import net.pms.media.MediaInfo;
import net.pms.util.ExternalProgramInfo;

public abstract class ImageEngine extends Engine {

	protected ImageEngine(ExternalProgramInfo programInfo) {
		super(programInfo);
	}

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
	 */
	public abstract byte[] getImage(OutputParams params, String fileName, ImageInfo imageInfo);

	/**
	 * Extracts or generates a thumbnail for {@code fileName}.
	 *
	 * @param params the {@link OutputParams} to use.
	 * @param fileName the path of the image file to process.
	 * @param imageInfo the {@link ImageInfo} for the image file.
	 * @return A byte array containing the thumbnail or {@code null}.
	 */
	public abstract byte[] getThumbnail(OutputParams params, String fileName, ImageInfo imageInfo);

	/**
	 * Parses {@code file} and stores the result in {@code media}.
	 *
	 * @param media the {@link MediaInfo} instance to store the parse
	 *            results in.
	 * @param file the {@link File} to parse.
	 */
	public abstract void parse(MediaInfo media, File file);
}
