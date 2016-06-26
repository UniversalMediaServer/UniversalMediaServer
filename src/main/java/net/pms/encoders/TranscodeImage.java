/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms.encoders;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.InternalJavaProcessImpl;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.network.HTTPResource;
import net.pms.util.PlayerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranscodeImage extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(TranscodeImage.class);

	public TranscodeImage() {
	}

	public byte[] ConvertImageToJpeg(File file) {
		LOGGER.trace("The file \"{}\" is transcoded to the JPEG format.", file.getAbsolutePath());
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedImage bi = ImageIO.read(file);
	        ImageIO.write(bi, FormatConfiguration.JPG, baos);
	        baos.flush();
	        byte[] image = baos.toByteArray();
	    	baos.close();
	    	return image;
	    } catch(IOException e) {
	    	LOGGER.trace("Could not convert the file \"{}\" to the JPEG format: ", file.getAbsolutePath(), e.getMessage());
	    }

		return null;
	}

	private String ID = "transcodeimage";
	
	@Override
	public int purpose() {
		return MISC_PLAYER;
	}

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Transcode Image";
	}

	@Override
	public int type() {
		return Format.IMAGE;
	}

	@Override
	public String[] args() {
		return null;
	}

	@Override
	public String mimeType() {
		return HTTPResource.JPEG_TYPEMIME;
	}

	@Override
	public String executable() {
		return NATIVE;
	}

	@Override
	public ProcessWrapper launchTranscode(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		params.waitbeforestart = 0;
		byte[] image = ConvertImageToJpeg(new File(dlna.getSystemName()));
		dlna.getMedia().setSize(image.length);
		ProcessWrapper pw = new InternalJavaProcessImpl(new ByteArrayInputStream(image));
		return pw;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		if (PlayerUtil.isImage(resource)) {
			return true;
		}

		return false;
	}
}
