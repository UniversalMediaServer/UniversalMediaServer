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
package net.pms.dlna;

import java.util.Locale;
import net.pms.media.video.MediaVideo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO : This enums should be moved to net.pms.media.video.MediaVideo.
 * As MediaTableFiles use it as object, it can't be done now.
 */
public class DLNAMediaInfo {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaVideo.class);

	/**
	 * This {@code enum} represents the different video "scan types".
	 */
	public enum ScanType {

		/** Interlaced scan, any sub-type */
		INTERLACED,

		/** Mixed scan */
		MIXED,

		/** Progressive scan */
		PROGRESSIVE;

		@Override
		public String toString() {
			return switch (this) {
				case INTERLACED -> "Interlaced";
				case MIXED -> "Mixed";
				case PROGRESSIVE -> "Progressive";
				default -> name();
			};
		}

		public static ScanType typeOf(String scanType) {
			if (StringUtils.isBlank(scanType)) {
				return null;
			}
			scanType = scanType.trim().toLowerCase(Locale.ROOT);
			switch (scanType) {
				case "interlaced" -> {
					return INTERLACED;
				}
				case "mixed" -> {
					return MIXED;
				}
				case "progressive" -> {
					return PROGRESSIVE;
				}
				default -> {
					LOGGER.debug("Warning: Unrecognized ScanType \"{}\"", scanType);
					return null;
				}
			}
		}
	}

	/**
	 * This {@code enum} represents the video scan order.
	 */
	public enum ScanOrder {

		/** Bottom Field First */
		BFF,

		/** Bottom Field Only */
		BFO,

		/** Pulldown */
		PULLDOWN,

		/** 2:2:2:2:2:2:2:2:2:2:2:3 Pulldown */
		PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3,

		/** 2:3 Pulldown */
		PULLDOWN_2_3,

		/** Top Field First */
		TFF,

		/** Top Field Only */
		TFO;

		@Override
		public String toString() {
			return switch (this) {
				case BFF -> "Bottom Field First";
				case BFO -> "Bottom Field Only";
				case PULLDOWN -> "Pulldown";
				case PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3 -> "2:2:2:2:2:2:2:2:2:2:2:3 Pulldown";
				case PULLDOWN_2_3 -> "2:3 Pulldown";
				case TFF -> "Top Field First";
				case TFO -> "Top Field Only";
				default -> name();
			};
		}

		public static ScanOrder typeOf(String scanOrder) {
			if (StringUtils.isBlank(scanOrder)) {
				return null;
			}
			scanOrder = scanOrder.trim().toLowerCase(Locale.ROOT);
			switch (scanOrder) {
				case "bff", "bottom field first" -> {
					return BFF;
				}
				case "bfo", "bottom field only" -> {
					return BFO;
				}
				case "pulldown" -> {
					return PULLDOWN;
				}
				case "2:2:2:2:2:2:2:2:2:2:2:3 pulldown" -> {
					return PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3;
				}
				case "2:3 pulldown" -> {
					return PULLDOWN_2_3;
				}
				case "tff", "top field first" -> {
					return TFF;
				}
				case "tfo", "top field only" -> {
					return TFO;
				}
				default -> {
					LOGGER.debug("Warning: Unrecognized ScanOrder \"{}\"", scanOrder);
					if (scanOrder.contains("pulldown")) {
						return PULLDOWN;
					}
					return null;
				}
			}
		}
	}
}
