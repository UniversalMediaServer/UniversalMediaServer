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
package net.pms.newgui.components;

import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.svg.SVGDocument;

/**
 * @author Surf@ceS
 */
public class SvgMultiResolutionImage extends AbstractMultiResolutionImage {

	private static final Logger LOGGER = LoggerFactory.getLogger(SvgMultiResolutionImage.class);
	private static final SAXSVGDocumentFactory FACTORY = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

	private final SVGDocument svg;
	private final BufferedImage baseImage;

	public SvgMultiResolutionImage(SVGDocument svg) {
		this.svg = svg;
		baseImage = getBufferedImage(svg);
	}

	public SvgMultiResolutionImage(URL imageResource) {
		this(getSVGDocument(imageResource));
	}

	public SvgMultiResolutionImage(URL imageResource, int width, int height) {
		svg = getSVGDocument(imageResource);
		baseImage = getResolutionVariant(svg, (float) width, (float) height);
	}

	@Override
	protected BufferedImage getBaseImage() {
		return baseImage;
	}

	@Override
	public BufferedImage getResolutionVariant(double destImageWidth, double destImageHeight) {
		return getResolutionVariant(svg, (float) destImageWidth, (float) destImageHeight);
	}

	@Override
	public List<Image> getResolutionVariants() {
		return Collections.unmodifiableList(List.of(baseImage));
	}

	public ImageIcon toImageIcon() {
		return new ImageIcon(this);
	}

	public static SVGDocument getSVGDocument(URL imageResource) {
		try {
			InputStream  rs = imageResource.openStream();
			return FACTORY.createSVGDocument(imageResource.toString(), rs);
		} catch (IOException ex) {
			LOGGER.error("SVG MultiResolution error", ex);
		}
		return null;
	}

	private static BufferedImage getBufferedImage(SVGDocument svg) {
		return getResolutionVariant(svg, null, null);
	}

	private static BufferedImage getResolutionVariant(SVGDocument svg, Float destImageWidth, Float destImageHeight) {
		if (svg == null) {
			return null;
		}
		Transcoder t = new PNGTranscoder();
		if (destImageWidth != null && destImageHeight != null) {
			t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, destImageWidth);
			t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, destImageHeight);
		}
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			// Create the transcoder input.
			TranscoderInput input = new TranscoderInput(svg);
			TranscoderOutput output = new TranscoderOutput(outputStream);
			t.transcode(input, output);
			outputStream.flush();
			byte[] imgData = outputStream.toByteArray();
			return ImageIO.read(new ByteArrayInputStream(imgData));
		} catch (IOException | TranscoderException ex) {
			LOGGER.error("SVG Transcoder error", ex);
		}
		return null;
	}

}
