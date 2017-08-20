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
package net.pms.util;

import java.awt.Color;
import java.awt.color.ColorSpace;

/**
 * This class is an extension of {@link FormattableColor} with predefined
 * formatted hexadecimal output used for subtitle colors.
 *
 * @author Nadahar
 */
@SuppressWarnings("serial")
public class SubtitleColor extends FormattableColor {

	/**
	 * @see Color#Color(int)
	 */
	public SubtitleColor(int rgb) {
		super(rgb);
	}

	/**
	 * @see Color#Color(int, boolean)
	 */
	public SubtitleColor(int rgba, boolean hasalpha) {
		super(rgba, hasalpha);
	}

	/**
	 * @see Color#Color(int, int, int)
	 */
	public SubtitleColor(int r, int g, int b) {
		super(r, g, b);
	}

	/**
	 * @see Color#Color(float, float, float)
	 */
	public SubtitleColor(float r, float g, float b) {
		super(r, g, b);
	}

	/**
	 * @see Color#Color(ColorSpace, float[], float)
	 */
	public SubtitleColor(ColorSpace cspace, float[] components, float alpha) {
		super(cspace, components, alpha);
	}

	/**
	 * @see Color#Color(int, int, int, int)
	 */
	public SubtitleColor(int r, int g, int b, int a) {
		super(r, g, b, a);
	}

	/**
	 * @see Color#Color(float, float, float, float)
	 */
	public SubtitleColor(float r, float g, float b, float a) {
		super(r, g, b, a);
	}

	/**
	 * Creates a {@link SubtitleColor} instance from a {@link Color} instance.
	 *
	 * @param color the {@link Color} to use for the new instance.
	 */
	public SubtitleColor(Color color) {
		super(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	/**
	 * Tries to create a {@link SubtitleColor} instance from {@code color}.
	 * Hexadecimal notations must be prefixed with one of:
	 * {@code x', %, #, 0x, \x, %x, $, h', 16#, 16r, #x, #16r, &h} or
	 * {@code 0h} .<br>
	 * <br>
	 * Supported formats are:
	 *
	 * <ul>
	 * <li>{@code 0xRRGGBBAA}</li>
	 * <li>{@code 0xRRGGBB}</li>
	 * <li>{@code R, G, B, A}</li>
	 * <li>{@code R, G, B}</li>
	 * <li>{@code ARGB}</li>
	 * <li>{@code <color name>} (Red, Yellow etc.)</li>
	 * <li>{@code <integer system property name>}</li>
	 *
	 * If the parsing fails, an {@link InvalidArgumentException} is thrown.
	 *
	 * @param color the {@link String} to attempt to parse.
	 * @return A newly created {@link SubtitleColor}.
	 * @throws InvalidArgumentException if {@code color} cannot be resolved to a
	 *             color value.
	 */
	public SubtitleColor(String color) throws InvalidArgumentException {
		super(calculateIntValue(color), true);
	}

	/**
	 * @return A formatted hexadecimal {@link String} with the current value
	 *         from this {@link SubtitleColor} instance suitable as an argument
	 *         for FFmpeg.
	 */
	public String getFFmpegHexValue() {
		return getHexValue("0x", "AABBGGRR", null, true, false);
	}

	/**
	 * @return A formatted hexadecimal {@link String} with the current value
	 *         from this {@link SubtitleColor} instance suitable as a ASS v4+
	 *         styles parameter.
	 */
	public String getASSv4StylesHexValue() {
		return getHexValue("&H", "AABBGGRR", null, true, true);
	}

	/**
	 * @return A formatted hexadecimal {@link String} with the current value
	 *         from this {@link SubtitleColor} instance suitable as a SSA/ASS
	 *         parameter.
	 */
	public String getSSAHexValue() {
		return getHexValue("&H", "BBGGRR", "&", true, true);
	}

	/**
	 * @return A formatted hexadecimal {@link String} with the current value
	 *         from this {@link SubtitleColor} instance suitable as a SSA/ASS
	 *         parameter.
	 */
	public String getSSAAlphaHexValue() {
		return getHexValue("&H", "AA", "&", true, true);
	}

	/**
	 * @return A formatted hexadecimal {@link String} with the current value
	 *         from this {@link SubtitleColor} instance suitable as an argument
	 *         for MEncoder.
	 */
	public String getMEncoderHexValue() {
		return getHexValue(null, "RRGGBBAA", null, true, true);
	}
}
