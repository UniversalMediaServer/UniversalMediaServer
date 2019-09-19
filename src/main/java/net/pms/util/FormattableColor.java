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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is an extension of {@link Color} with improved {@link String}
 * parsing and methods for formatted hexadecimal {@link String} output.
 *
 * @author Nadahar
 */
@SuppressWarnings("serial")
public class FormattableColor extends Color {

	protected static final Pattern hexPattern =
		Pattern.compile("(?i)^\\s*(?:x'|%|#|0x|\\\\x|%x|\\$|h'|16#|16r|#x|#16r|&h|0h)([0-9a-f]{1,8})'?\\s*$");
	protected static final Pattern decPattern =
		Pattern.compile("^\\s*(-?\\d{1,10})\\s*$");
	private static final Map<String, int[]> knownColors = new HashMap<>(140, 1f);

	/**
	 * @see Color#Color(int)
	 */
	public FormattableColor(int rgb) {
		super(rgb);
	}

	/**
	 * @see Color#Color(int, boolean)
	 */
	public FormattableColor(int rgba, boolean hasalpha) {
		super(rgba, hasalpha);
	}

	/**
	 * @see Color#Color(int, int, int)
	 */
	public FormattableColor(int r, int g, int b) {
		super(r, g, b);
	}

	/**
	 * @see Color#Color(float, float, float)
	 */
	public FormattableColor(float r, float g, float b) {
		super(r, g, b);
	}

	/**
	 * @see Color#Color(ColorSpace, float[], float)
	 */
	public FormattableColor(ColorSpace cspace, float[] components, float alpha) {
		super(cspace, components, alpha);
	}

	/**
	 * @see Color#Color(int, int, int, int)
	 */
	public FormattableColor(int r, int g, int b, int a) {
		super(r, g, b, a);
	}

	/**
	 * @see Color#Color(float, float, float, float)
	 */
	public FormattableColor(float r, float g, float b, float a) {
		super(r, g, b, a);
	}

	/**
	 * Creates a {@link FormattableColor} instance from a {@link Color} instance.
	 *
	 * @param color the {@link Color} to use for the new instance.
	 */
	public FormattableColor(Color color) {
		super(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	/**
	 * Tries to create a {@link FormattableColor} instance from {@code color}.
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
	 * @return A newly created {@link FormattableColor}.
	 * @throws InvalidArgumentException if {@code color} cannot be resolved to a
	 *             color value.
	 */
	public FormattableColor(String color) throws InvalidArgumentException {
		super(calculateIntValue(color), true);
	}

	/**
	 * Tries to calculate an integer value with alpha included that can be used
	 * to create a {@link Color}. Parsing of the {@link String} parameter is as
	 * follows:
	 *
	 * <ul>
	 * <li>First try to parse {@code color} as a hexadecimal string. Several
	 * different notations are supported and it can be with or without alpha.
	 * Valid forms are RRGGBB or RRGGBBAA with one of the following prefixes:
	 * "x'", "%", "#", "0x", "\x", "%x", "$", "h'", "16#", "16r", "#x", "#16r",
	 * "&amp;h" or "0h".</li>
	 * <li>Second, try to parse {@code color} as a comma separated string of the
	 * form R, G, B [,A].
	 * <li>If that fails, try to parse {@code color} as a decimal ARGB string.</li>
	 * <li>If that also fails, try to parse {@code color} as a color name (Red,
	 * Yellow etc).</li>
	 * <li>As a last attempt, try to parse {@code color} as a system property
	 * integer.</li>
	 * </ul>
	 * If the parsing fails, a {@link InvalidArgumentException} is thrown.
	 *
	 * @param color the {@link String} to attempt to parse.
	 * @return An integer value with alpha included that can be used to create a
	 *         {@link Color}.
	 * @throws InvalidArgumentException if {@code color} cannot be resolved to a
	 *             color value.
	 */
	protected static int calculateIntValue(String color) throws InvalidArgumentException {
		if (color == null) {
			throw new NullPointerException("color cannot be null");
		}
		color = color.trim();

		NumberFormatException ne = null;
		try {
			// Try to parse hex string
			String value = extractHexString(color);
			if (value != null) {
				long l = Long.parseLong(value, 16);
				if (value.length() == 7) {
					// 0xRRGGBBA
					return (int) (
						((l & 0xFF00000) >> 4) |
						((l & 0xFF000) >> 4) |
						((l & 0xFF0) >> 4) |
						((l & 0xF) << 28));
				}  else if (value.length() == 8) {
					// 0xRRGGBBAA
					return (int) (
						((l & 0xFF000000) >> 8) |
						((l & 0xFF0000) >> 8) |
						((l & 0xFF00) >> 8) |
						((l & 0xFF) << 24));
				} else {
					// 0xRRGGBB
					return (int) (0xFF000000 | l);
				}
			}

			// Try to parse comma separated decimals
			if (color.contains(",")) {
				// R, G, B [,A]
				String[] colorElements = color.split("\\s*,\\s*");
				int r = Integer.parseInt(colorElements[0]);
				int g = Integer.parseInt(colorElements[1]);
				int b = Integer.parseInt(colorElements[2]);
				int a = colorElements.length > 3 ? Integer.parseInt(colorElements[3]) : 255;
				if (
					r > -1 && r < 256 &&
					g > -1 && g < 256 &&
					b > -1 && b < 256 &&
					a > -1 && a < 256
				) {
					return
			        	(a << 24) |
			        	(r << 16) |
			        	(g << 8)  |
			            b;
				}
			}

			// Try to parse decimal value ARGB
			value = extractDecString(color);
			if (value != null) {
				return (int) Long.parseLong(value);
			}
		} catch (NumberFormatException e) {
			ne = e;
		}

		// Try to parse as color name
		int[] rgb = getNamedColorValues(color);
		if (rgb != null && rgb.length == 3) {
			return
	        	(0xFF000000) |
	        	((rgb[0] & 0xFF) << 16) |
	        	((rgb[1] & 0xFF) << 8)  |
	            (rgb[2] & 0xFF);

		}
		// Try to parse as system property
		Color colorInstance = Color.getColor(color);
		if (colorInstance != null) {
			return
	        	((colorInstance.getAlpha() & 0xFF) << 24) |
	        	((colorInstance.getRed() & 0xFF) << 16) |
	        	((colorInstance.getGreen() & 0xFF) << 8)  |
	            (colorInstance.getBlue() & 0xFF);
		}

		// Parsing failed
		if (ne != null) {
			throw new InvalidArgumentException(String.format(Locale.ROOT,
				"Could not parse subtitle color \"%s\": %s",
				color,
				ne.getMessage()
			), ne);
		} else {
			throw new InvalidArgumentException(String.format(
				Locale.ROOT, "Could not parse subtitle color \"%s\"", color
			));
		}
	}



	/**
	 * @return A formatted hexadecimal {@link String} in the form
	 *         {@code 0xRRGGBBAA}.
	 */
	public String get0xRRGGBBAA() {
		return getHexValue("0x", "RRGGBBAA", null, true, false);
	}

	/**
	 * @return A formatted hexadecimal {@link String} in the form
	 *         {@code 0xRRGGBB}.
	 */
	public String get0xRRGGBB() {
		return getHexValue("0x", "RRGGBB", null, true, true);
	}

	/**
	 * Returns a formatted hexadecimal {@link String} with the current value
	 * from this {@link FormattableColor} instance.
	 *
	 * @param prefix a prefix to add to the start of the resulting
	 *            {@link String}. Can be {@code null}.
	 * @param pattern the pattern consisting of any combinations of the codes
	 *            {@code RR}, {@code R}, {@code GG}, {@code G}, {@code BB},
	 *            {@code B}, {@code AA} and {@code A}. Any other characters are
	 *            ignored.
	 * @param suffix a suffix to add to the end of the resulting {@link String}.
	 *            Can be {@code null}.
	 * @param upperCase whether or not the resulting characters in the
	 *            hexadecimal string should be upper- or lower case. Does not
	 *            apply to {@code prefix} and {@code suffix}.
	 * @param invertAlpha whether or not the alpha value should be inverted so
	 *            that 0 is opaque instead of 0xFF.
	 * @return The formatted hexadecimal {@link String}.
	 */
	public String getHexValue(String prefix, String pattern, String suffix, boolean upperCase, boolean invertAlpha) {
		StringBuilder sb = new StringBuilder(prefix != null ? prefix : "");

		String red = upperCase ?
			Integer.toHexString(getRed()).toUpperCase(Locale.ROOT) :
			Integer.toHexString(getRed());
		String green = upperCase ?
			Integer.toHexString(getGreen()).toUpperCase(Locale.ROOT) :
			Integer.toHexString(getGreen());
		String blue = upperCase ?
			Integer.toHexString(getBlue()).toUpperCase(Locale.ROOT) :
			Integer.toHexString(getBlue());
		Integer a = invertAlpha ? 0xFF - getAlpha() : getAlpha();
		String alpha = upperCase ?
			Integer.toHexString(a).toUpperCase(Locale.ROOT) :
			Integer.toHexString(a);

		pattern = pattern.toUpperCase(Locale.ROOT);
		for (int i = 0; i < pattern.length(); i++) {
			if ("RR".equals(pattern.substring(i, i+2))) {
				if (red.length() < 2) {
					sb.append("0").append(red);
				} else {
					sb.append(red);
				}
				i++;
			} else if ("R".equals(pattern.substring(i, i+1))) {
				sb.append(red);
			} else if ("GG".equals(pattern.substring(i, i+2))) {
				if (green.length() < 2) {
					sb.append("0").append(green);
				} else {
					sb.append(green);
				}
				i++;
			} else if ("G".equals(pattern.substring(i, i+1))) {
				sb.append(green);
			} else if ("BB".equals(pattern.substring(i, i+2))) {
				if (blue.length() < 2) {
					sb.append("0").append(blue);
				} else {
					sb.append(blue);
				}
				i++;
			} else if ("B".equals(pattern.substring(i, i+1))) {
				sb.append(blue);
			} else if ("AA".equals(pattern.substring(i, i+2))) {
				if (alpha.length() < 2) {
					sb.append("0").append(alpha);
				} else {
					sb.append(alpha);
				}
				i++;
			} else if ("A".equals(pattern.substring(i, i+1))) {
				sb.append(alpha);
			}
		}

		if (suffix != null) {
			sb.append(suffix);
		}
		return sb.toString();
	}

	@Override
    public String toString() {
        return
        	getClass().getSimpleName() +
        	"[r=" + getHexValue("0x", "RR", null, true, false) +
        	", g=" + getHexValue("0x", "GG", null, true, false) +
        	", b=" + getHexValue("0x", "BB", null, true, false) +
        	", a=" + getHexValue("0x", "AA", null, true, false) +
        	"]";
    }

	protected static String extractHexString(String s) {
		Matcher matcher = hexPattern.matcher(s);
		if (matcher.find()) {
			return matcher.group(1).toUpperCase(Locale.ROOT);
		} else {
			return null;
		}
	}

	protected static String extractDecString(String s) {
		Matcher matcher = decPattern.matcher(s);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

	/**
	 * Returns an array of RGB integer values for a named color from a predefined list of names.
	 *
	 * @param colorName the name of the color whose values to retrieve.
	 * @return The integer array with RGB values or {@code null} if
	 *         {@code colorName} isn't among the predefined color names.
	 */
	public static int[] getNamedColorValues(String colorName) {
		colorName = colorName.toUpperCase(Locale.ROOT);
		if (knownColors.containsKey(colorName)) {
			return knownColors.get(colorName);
		}
		return null;
	}

	static {
		// The color values are from http://ffmpeg.org/ffmpeg-utils.html#Color

		knownColors.put("ALICEBLUE", new int[] {0xF0, 0xF8, 0xFF});
		knownColors.put("ANTIQUEWHITE", new int[] {0xFA, 0xEB, 0xD7});
		knownColors.put("AQUA", new int[] {0x00, 0xFF, 0xFF});
		knownColors.put("AQUAMARINE", new int[] {0x7F, 0xFF, 0xD4});
		knownColors.put("AZURE", new int[] {0xF0, 0xFF, 0xFF});
		knownColors.put("BEIGE", new int[] {0xF5, 0xF5, 0xDC});
		knownColors.put("BISQUE", new int[] {0xFF, 0xE4, 0xC4});
		knownColors.put("BLACK", new int[] {0x00, 0x00, 0x00});
		knownColors.put("BLANCHEDALMOND", new int[] {0xFF, 0xEB, 0xCD});
		knownColors.put("BLUE", new int[] {0x00, 0x00, 0xFF});
		knownColors.put("BLUEVIOLET", new int[] {0x8A, 0x2B, 0xE2});
		knownColors.put("BROWN", new int[] {0xA5, 0x2A, 0x2A});
		knownColors.put("BURLYWOOD", new int[] {0xDE, 0xB8, 0x87});
		knownColors.put("CADETBLUE", new int[] {0x5F, 0x9E, 0xA0});
		knownColors.put("CHARTREUSE", new int[] {0x7F, 0xFF, 0x00});
		knownColors.put("CHOCOLATE", new int[] {0xD2, 0x69, 0x1E});
		knownColors.put("CORAL", new int[] {0xFF, 0x7F, 0x50});
		knownColors.put("CORNFLOWERBLUE", new int[] {0x64, 0x95, 0xED});
		knownColors.put("CORNSILK", new int[] {0xFF, 0xF8, 0xDC});
		knownColors.put("CRIMSON", new int[] {0xDC, 0x14, 0x3C});
		knownColors.put("CYAN", new int[] {0x00, 0xFF, 0xFF});
		knownColors.put("DARKBLUE", new int[] {0x00, 0x00, 0x8B});
		knownColors.put("DARKCYAN", new int[] {0x00, 0x8B, 0x8B});
		knownColors.put("DARKGOLDENROD", new int[] {0xB8, 0x86, 0x0B});
		knownColors.put("DARKGRAY", new int[] {0xA9, 0xA9, 0xA9});
		knownColors.put("DARKGREEN", new int[] {0x00, 0x64, 0x00});
		knownColors.put("DARKKHAKI", new int[] {0xBD, 0xB7, 0x6B});
		knownColors.put("DARKMAGENTA", new int[] {0x8B, 0x00, 0x8B});
		knownColors.put("DARKOLIVEGREEN", new int[] {0x55, 0x6B, 0x2F});
		knownColors.put("DARKORANGE", new int[] {0xFF, 0x8C, 0x00});
		knownColors.put("DARKORCHID", new int[] {0x99, 0x32, 0xCC});
		knownColors.put("DARKRED", new int[] {0x8B, 0x00, 0x00});
		knownColors.put("DARKSALMON", new int[] {0xE9, 0x96, 0x7A});
		knownColors.put("DARKSEAGREEN", new int[] {0x8F, 0xBC, 0x8F});
		knownColors.put("DARKSLATEBLUE", new int[] {0x48, 0x3D, 0x8B});
		knownColors.put("DARKSLATEGRAY", new int[] {0x2F, 0x4F, 0x4F});
		knownColors.put("DARKTURQUOISE", new int[] {0x00, 0xCE, 0xD1});
		knownColors.put("DARKVIOLET", new int[] {0x94, 0x00, 0xD3});
		knownColors.put("DEEPPINK", new int[] {0xFF, 0x14, 0x93});
		knownColors.put("DEEPSKYBLUE", new int[] {0x00, 0xBF, 0xFF});
		knownColors.put("DIMGRAY", new int[] {0x69, 0x69, 0x69});
		knownColors.put("DODGERBLUE", new int[] {0x1E, 0x90, 0xFF});
		knownColors.put("FIREBRICK", new int[] {0xB2, 0x22, 0x22});
		knownColors.put("FLORALWHITE", new int[] {0xFF, 0xFA, 0xF0});
		knownColors.put("FORESTGREEN", new int[] {0x22, 0x8B, 0x22});
		knownColors.put("FUCHSIA", new int[] {0xFF, 0x00, 0xFF});
		knownColors.put("GAINSBORO", new int[] {0xDC, 0xDC, 0xDC});
		knownColors.put("GHOSTWHITE", new int[] {0xF8, 0xF8, 0xFF});
		knownColors.put("GOLD", new int[] {0xFF, 0xD7, 0x00});
		knownColors.put("GOLDENROD", new int[] {0xDA, 0xA5, 0x20});
		knownColors.put("GRAY", new int[] {0x80, 0x80, 0x80});
		knownColors.put("GREEN", new int[] {0x00, 0x80, 0x00});
		knownColors.put("GREENYELLOW", new int[] {0xAD, 0xFF, 0x2F});
		knownColors.put("HONEYDEW", new int[] {0xF0, 0xFF, 0xF0});
		knownColors.put("HOTPINK", new int[] {0xFF, 0x69, 0xB4});
		knownColors.put("INDIANRED", new int[] {0xCD, 0x5C, 0x5C});
		knownColors.put("INDIGO", new int[] {0x4B, 0x00, 0x82});
		knownColors.put("IVORY", new int[] {0xFF, 0xFF, 0xF0});
		knownColors.put("KHAKI", new int[] {0xF0, 0xE6, 0x8C});
		knownColors.put("LAVENDER", new int[] {0xE6, 0xE6, 0xFA});
		knownColors.put("LAVENDERBLUSH", new int[] {0xFF, 0xF0, 0xF5});
		knownColors.put("LAWNGREEN", new int[] {0x7C, 0xFC, 0x00});
		knownColors.put("LEMONCHIFFON", new int[] {0xFF, 0xFA, 0xCD});
		knownColors.put("LIGHTBLUE", new int[] {0xAD, 0xD8, 0xE6});
		knownColors.put("LIGHTCORAL", new int[] {0xF0, 0x80, 0x80});
		knownColors.put("LIGHTCYAN", new int[] {0xE0, 0xFF, 0xFF});
		knownColors.put("LIGHTGOLDENRODYELLOW", new int[] {0xFA, 0xFA, 0xD2});
		knownColors.put("LIGHTGRAY", new int[] {0xD3, 0xD3, 0xD3});
		knownColors.put("LIGHTGREEN", new int[] {0x90, 0xEE, 0x90});
		knownColors.put("LIGHTPINK", new int[] {0xFF, 0xB6, 0xC1});
		knownColors.put("LIGHTSALMON", new int[] {0xFF, 0xA0, 0x7A});
		knownColors.put("LIGHTSEAGREEN", new int[] {0x20, 0xB2, 0xAA});
		knownColors.put("LIGHTSKYBLUE", new int[] {0x87, 0xCE, 0xFA});
		knownColors.put("LIGHTSLATEGRAY", new int[] {0x77, 0x88, 0x99});
		knownColors.put("LIGHTSTEELBLUE", new int[] {0xB0, 0xC4, 0xDE});
		knownColors.put("LIGHTYELLOW", new int[] {0xFF, 0xFF, 0xE0});
		knownColors.put("LIME", new int[] {0x00, 0xFF, 0x00});
		knownColors.put("LIMEGREEN", new int[] {0x32, 0xCD, 0x32});
		knownColors.put("LINEN", new int[] {0xFA, 0xF0, 0xE6});
		knownColors.put("MAGENTA", new int[] {0xFF, 0x00, 0xFF});
		knownColors.put("MAROON", new int[] {0x80, 0x00, 0x00});
		knownColors.put("MEDIUMAQUAMARINE", new int[] {0x66, 0xCD, 0xAA});
		knownColors.put("MEDIUMBLUE", new int[] {0x00, 0x00, 0xCD});
		knownColors.put("MEDIUMORCHID", new int[] {0xBA, 0x55, 0xD3});
		knownColors.put("MEDIUMPURPLE", new int[] {0x93, 0x70, 0xDB});
		knownColors.put("MEDIUMSEAGREEN", new int[] {0x3C, 0xB3, 0x71});
		knownColors.put("MEDIUMSLATEBLUE", new int[] {0x7B, 0x68, 0xEE});
		knownColors.put("MEDIUMSPRINGGREEN", new int[] {0x00, 0xFA, 0x9A});
		knownColors.put("MEDIUMTURQUOISE", new int[] {0x48, 0xD1, 0xCC});
		knownColors.put("MEDIUMVIOLETRED", new int[] {0xC7, 0x15, 0x85});
		knownColors.put("MIDNIGHTBLUE", new int[] {0x19, 0x19, 0x70});
		knownColors.put("MINTCREAM", new int[] {0xF5, 0xFF, 0xFA});
		knownColors.put("MISTYROSE", new int[] {0xFF, 0xE4, 0xE1});
		knownColors.put("MOCCASIN", new int[] {0xFF, 0xE4, 0xB5});
		knownColors.put("NAVAJOWHITE", new int[] {0xFF, 0xDE, 0xAD});
		knownColors.put("NAVY", new int[] {0x00, 0x00, 0x80});
		knownColors.put("OLDLACE", new int[] {0xFD, 0xF5, 0xE6});
		knownColors.put("OLIVE", new int[] {0x80, 0x80, 0x00});
		knownColors.put("OLIVEDRAB", new int[] {0x6B, 0x8E, 0x23});
		knownColors.put("ORANGE", new int[] {0xFF, 0xA5, 0x00});
		knownColors.put("ORANGERED", new int[] {0xFF, 0x45, 0x00});
		knownColors.put("ORCHID", new int[] {0xDA, 0x70, 0xD6});
		knownColors.put("PALEGOLDENROD", new int[] {0xEE, 0xE8, 0xAA});
		knownColors.put("PALEGREEN", new int[] {0x98, 0xFB, 0x98});
		knownColors.put("PALETURQUOISE", new int[] {0xAF, 0xEE, 0xEE});
		knownColors.put("PALEVIOLETRED", new int[] {0xDB, 0x70, 0x93});
		knownColors.put("PAPAYAWHIP", new int[] {0xFF, 0xEF, 0xD5});
		knownColors.put("PEACHPUFF", new int[] {0xFF, 0xDA, 0xB9});
		knownColors.put("PERU", new int[] {0xCD, 0x85, 0x3F});
		knownColors.put("PINK", new int[] {0xFF, 0xC0, 0xCB});
		knownColors.put("PLUM", new int[] {0xDD, 0xA0, 0xDD});
		knownColors.put("POWDERBLUE", new int[] {0xB0, 0xE0, 0xE6});
		knownColors.put("PURPLE", new int[] {0x80, 0x00, 0x80});
		knownColors.put("RED", new int[] {0xFF, 0x00, 0x00});
		knownColors.put("ROSYBROWN", new int[] {0xBC, 0x8F, 0x8F});
		knownColors.put("ROYALBLUE", new int[] {0x41, 0x69, 0xE1});
		knownColors.put("SADDLEBROWN", new int[] {0x8B, 0x45, 0x13});
		knownColors.put("SALMON", new int[] {0xFA, 0x80, 0x72});
		knownColors.put("SANDYBROWN", new int[] {0xF4, 0xA4, 0x60});
		knownColors.put("SEAGREEN", new int[] {0x2E, 0x8B, 0x57});
		knownColors.put("SEASHELL", new int[] {0xFF, 0xF5, 0xEE});
		knownColors.put("SIENNA", new int[] {0xA0, 0x52, 0x2D});
		knownColors.put("SILVER", new int[] {0xC0, 0xC0, 0xC0});
		knownColors.put("SKYBLUE", new int[] {0x87, 0xCE, 0xEB});
		knownColors.put("SLATEBLUE", new int[] {0x6A, 0x5A, 0xCD});
		knownColors.put("SLATEGRAY", new int[] {0x70, 0x80, 0x90});
		knownColors.put("SNOW", new int[] {0xFF, 0xFA, 0xFA});
		knownColors.put("SPRINGGREEN", new int[] {0x00, 0xFF, 0x7F});
		knownColors.put("STEELBLUE", new int[] {0x46, 0x82, 0xB4});
		knownColors.put("TAN", new int[] {0xD2, 0xB4, 0x8C});
		knownColors.put("TEAL", new int[] {0x00, 0x80, 0x80});
		knownColors.put("THISTLE", new int[] {0xD8, 0xBF, 0xD8});
		knownColors.put("TOMATO", new int[] {0xFF, 0x63, 0x47});
		knownColors.put("TURQUOISE", new int[] {0x40, 0xE0, 0xD0});
		knownColors.put("VIOLET", new int[] {0xEE, 0x82, 0xEE});
		knownColors.put("WHEAT", new int[] {0xF5, 0xDE, 0xB3});
		knownColors.put("WHITE", new int[] {0xFF, 0xFF, 0xFF});
		knownColors.put("WHITESMOKE", new int[] {0xF5, 0xF5, 0xF5});
		knownColors.put("YELLOW", new int[] {0xFF, 0xFF, 0x00});
		knownColors.put("YELLOWGREEN", new int[] {0x9A, 0xCD, 0x32});
	}
}
