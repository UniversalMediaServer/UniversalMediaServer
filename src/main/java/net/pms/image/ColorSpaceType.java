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
package net.pms.image;

import java.awt.color.ColorSpace;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an {@code enum} mirroring the integer constants in
 * {@link ColorSpace}. Use this for better type-safety.
 */
public enum ColorSpaceType {

    /**
     * Any of the family of XYZ color spaces.
     */
	TYPE_XYZ(0),

    /**
     * Any of the family of Lab color spaces.
     */
    TYPE_Lab(1),

    /**
     * Any of the family of Luv color spaces.
     */
    TYPE_Luv(2),

    /**
     * Any of the family of YCbCr color spaces.
     */
    TYPE_YCbCr(3),

    /**
     * Any of the family of Yxy color spaces.
     */
    TYPE_Yxy(4),

    /**
     * Any of the family of RGB color spaces.
     */
    TYPE_RGB(5),

    /**
     * Any of the family of GRAY color spaces.
     */
    TYPE_GRAY(6),

    /**
     * Any of the family of HSV color spaces.
     */
    TYPE_HSV(7),

    /**
     * Any of the family of HLS color spaces.
     */
    TYPE_HLS(8),

    /**
     * Any of the family of CMYK color spaces.
     */
    TYPE_CMYK(9),

    /**
     * Any of the family of CMY color spaces.
     */
    TYPE_CMY(11),

    /**
     * Generic 2 component color spaces.
     */
    TYPE_2CLR(12),

    /**
     * Generic 3 component color spaces.
     */
    TYPE_3CLR(13),

    /**
     * Generic 4 component color spaces.
     */
    TYPE_4CLR(14),

    /**
     * Generic 5 component color spaces.
     */
    TYPE_5CLR(15),

    /**
     * Generic 6 component color spaces.
     */
    TYPE_6CLR(16),

    /**
     * Generic 7 component color spaces.
     */
    TYPE_7CLR(17),

    /**
     * Generic 8 component color spaces.
     */
    TYPE_8CLR(18),

    /**
     * Generic 9 component color spaces.
     */
    TYPE_9CLR(19),

    /**
     * Generic 10 component color spaces.
     */
    TYPE_ACLR(20),

    /**
     * Generic 11 component color spaces.
     */
    TYPE_BCLR(21),

    /**
     * Generic 12 component color spaces.
     */
    TYPE_CCLR(22),

    /**
     * Generic 13 component color spaces.
     */
    TYPE_DCLR(23),

    /**
     * Generic 14 component color spaces.
     */
    TYPE_ECLR(24),

    /**
     * Generic 15 component color spaces.
     */
    TYPE_FCLR(25);

	private static final Map<Integer, ColorSpaceType> map = new HashMap<>();

    static {
        for (ColorSpaceType colorSpaceType : ColorSpaceType.values()) {
            map.put(colorSpaceType.typeId, colorSpaceType);
        }
    }

    public static ColorSpaceType toColorSpaceType(int typeId) {
        return map.get(typeId);
    }

	private final int typeId;

    private ColorSpaceType(int typeId) {
    	this.typeId = typeId;
	}

    public int getTypeId() {
    	return typeId;
    }
}
