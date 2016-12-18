/*
 * Universal Media Server, for streaming any media to DLNA
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
package net.pms.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import javax.swing.text.JTextComponent;

/**
 * This is a utility class for various reusable Swing methods. *
 */
public class SwingUtils {

	// We don't want this to be instanced, only static methods
	private SwingUtils() {
	}

	/**
	 * Returns the {@link Dimension} for the given {@link JTextComponent}
	 * subclass that will show the whole word wrapped text in the given width.
	 * It won't work for styled text of varied size or style, it's assumed that
	 * the whole text is rendered with the {@link JTextComponent}s font.
	 *
	 * @param textComponent the {@link JTextComponent} to calculate the {@link Dimension} for
	 * @param width the width of the resulting {@link Dimension}
	 * @param text the {@link String} which should be word wrapped
	 * @return The calculated {@link Dimension}
	 */
	public static Dimension getWordWrappedTextDimension(JTextComponent textComponent, int width, String text) {
		if (textComponent == null) {
			throw new IllegalArgumentException("textComponent cannot be null");
		}
		if (width < 1) {
			throw new IllegalArgumentException("width must be 1 or greater");
		}
		if (text == null) {
			text = textComponent.getText();
		}
		if (text.isEmpty()) {
			return new Dimension(width, 0);
		}

		FontMetrics metrics = textComponent.getFontMetrics(textComponent.getFont());
	    FontRenderContext rendererContext = metrics.getFontRenderContext();
	    float formatWidth = width - textComponent.getInsets().left - textComponent.getInsets().right;

	    int lines = 0;
	    String[] paragraphs = text.split("\n");
	    for (String paragraph : paragraphs) {
	    	if (paragraph.isEmpty()) {
	    		lines++;
	    	} else {
				AttributedString attributedText = new AttributedString(paragraph);
				attributedText.addAttribute(TextAttribute.FONT, textComponent.getFont());
			    AttributedCharacterIterator charIterator = attributedText.getIterator();
			    LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(charIterator, rendererContext);

			    lineMeasurer.setPosition(charIterator.getBeginIndex());
			    while (lineMeasurer.getPosition() < charIterator.getEndIndex()) {
			    	lineMeasurer.nextLayout(formatWidth);
			    	lines++;
			    }
	    	}
	    }

	    return new Dimension(width, metrics.getHeight() * lines + textComponent.getInsets().top + textComponent.getInsets().bottom);
	}

	/**
	 * Returns the {@link Dimension} for the given {@link JTextComponent}
	 * subclass that will show the whole word wrapped text in the given width.
	 * The {@link String} from {@link JTextComponent#getText()} will be used
	 * as the word wrapped text. It won't work for styled text of varied size
	 * or style, it's assumed that the whole text is rendered with the
	 * {@link JTextComponent}s font.
	 *
	 * @param textComponent the {@link JTextComponent} to calculate the {@link Dimension} for
	 * @param width the width of the resulting {@link Dimension}
	 * @return The calculated {@link Dimension}
	 */
	public static Dimension getWordWrappedTextDimension(JTextComponent textComponent, int width) {
		return getWordWrappedTextDimension(textComponent, width, null);
	}

	/**
	 * Calculates the average character width for the given {@link Component}.
	 * This can be useful as a scaling factor when designing for font scaling.
	 *
	 * @param component the {@link Component} for which to calculate the
	 *        average character width.
	 * @return The average width in pixels
	 */
	public static float getComponentAverageCharacterWidth(Component component) {
		FontMetrics metrics = component.getFontMetrics(component.getFont());
		int i = 0;
		float avgWidth = 0;
		for (int width : metrics.getWidths()) {
			avgWidth += width;
			i++;
		}
		return avgWidth / i;
	}

}
