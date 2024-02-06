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
package net.pms.swing.components;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.util.XMLResourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

/**
 * @author Surf@ceS
 */
public class SvgMultiResolutionImage extends AbstractMultiResolutionImage {

	private static final Logger LOGGER = LoggerFactory.getLogger(SvgMultiResolutionImage.class);
	private static final SAXSVGDocumentFactory FACTORY = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

	private final SVGDocument svg;
	private final ImageFilter filter;
	private final Image baseImage;
	private final List<Image> variants = new ArrayList<>();

	public SvgMultiResolutionImage(SVGDocument svg, ImageFilter filter, Float width, Float height) {
		this.svg = svg;
		this.filter = filter;
		baseImage = getResolutionVariant(svg, filter, width, height);
		if (baseImage != null) {
			variants.add(baseImage);
		}
	}

	public SvgMultiResolutionImage(SVGDocument svg, ImageFilter filter) {
		this(svg, filter, null, null);
	}

	public SvgMultiResolutionImage(SVGDocument svg) {
		this(svg, null);
	}

	public SvgMultiResolutionImage(URL imageResource) {
		this(getSVGDocument(imageResource));
	}

	public SvgMultiResolutionImage(InputStream imageStream) {
		this(getSVGDocument(imageStream));
	}

	public SvgMultiResolutionImage(URL imageResource, int width, int height) {
		this(getSVGDocument(imageResource), null, (float) width, (float) height);
	}

	@Override
	public Image getBaseImage() {
		return baseImage;
	}

	@Override
	public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
		synchronized (variants) {
			for (Image variant : variants) {
				if (variant.getHeight(null) == destImageHeight && variant.getWidth(null) == destImageHeight) {
					return variant;
				}
			}
			Image variant = getResolutionVariant(svg, filter, (float) destImageWidth, (float) destImageHeight);
			variants.add(variant);
			return variant;
		}
	}

	@Override
	public List<Image> getResolutionVariants() {
		return Collections.unmodifiableList(variants);
	}

	public ImageIcon toImageIcon() {
		return new ImageIcon(this);
	}

	public SVGDocument getSVGDocument() {
		return svg;
	}

	public static SVGDocument getSVGDocument(URL imageResource) {
		try {
			return getSVGDocument(imageResource.openStream());
		} catch (IOException ex) {
			LOGGER.error("SVG MultiResolution error", ex);
		}
		return null;
	}

	private static SVGDocument getSVGDocument(InputStream imageStream) {
		try {
			return FACTORY.createSVGDocument(null, imageStream);
		} catch (IOException ex) {
			LOGGER.error("SVG MultiResolution error", ex);
		}
		return null;
	}

	private static Image getResolutionVariant(SVGDocument svg, ImageFilter filter, Float destImageWidth, Float destImageHeight) {
		if (svg == null) {
			return null;
		}
		SvgTranscoder t = new SvgTranscoder();
		if (destImageWidth != null && destImageHeight != null) {
			t.addTranscodingHint(SvgTranscoder.KEY_WIDTH, destImageWidth);
			t.addTranscodingHint(SvgTranscoder.KEY_HEIGHT, destImageHeight);
			float height = svg.getRootElement().getHeight().getBaseVal().getValue();
			float width = svg.getRootElement().getWidth().getBaseVal().getValue();
			if (destImageWidth < width || destImageHeight < height) {
				//show small id
				Element elem = svg.getElementById("small");
				if (elem != null) {
					elem.setAttribute("opacity", "1");
				}
			}
		}
		try {
			// Create the transcoder input.
			TranscoderInput input = new TranscoderInput(svg);
			t.transcode(input, null);
			Image variant = t.getBufferedImage();
			if (filter != null && variant != null) {
				ImageProducer producer = new FilteredImageSource(variant.getSource(), filter);
				variant = Toolkit.getDefaultToolkit().createImage(producer);
			}
			return variant;
		} catch (TranscoderException ex) {
			LOGGER.error("SVG Transcoder error", ex);
		}
		return null;
	}

}