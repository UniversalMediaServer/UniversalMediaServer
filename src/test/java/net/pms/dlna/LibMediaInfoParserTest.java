/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2013  I. Sokolov
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.dlna;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class LibMediaInfoParserTest {
	@Test
	public void testGetReferenceFrameCount() throws Exception {
		assertThat(LibMediaInfoParser.getReferenceFrameCount("-5 6")).isEqualTo((byte) -5);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("7")).isEqualTo((byte) 7);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("2 frame2")).isEqualTo((byte) 2);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("-16 frame3")).isEqualTo((byte) -16);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("")).isEqualTo((byte) -1);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("strange1")).isEqualTo((byte) -1);
		assertThat(LibMediaInfoParser.getReferenceFrameCount("6ref")).isEqualTo((byte) -1);
	}

	@Test
	public void testGetAvcLevel() throws Exception {
		assertThat(LibMediaInfoParser.getAvcLevel("Main@L2.0")).isEqualTo("2.0");
		assertThat(LibMediaInfoParser.getAvcLevel("High@L3.0")).isEqualTo("3.0");
		assertThat(LibMediaInfoParser.getAvcLevel("high@l4.0")).isEqualTo("4.0");
		assertThat(LibMediaInfoParser.getAvcLevel("hIgH@L4.1")).isEqualTo("4.1");
		assertThat(LibMediaInfoParser.getAvcLevel("5.1")).isNull();
		assertThat(LibMediaInfoParser.getAvcLevel("level5")).isNull();
	}
}
