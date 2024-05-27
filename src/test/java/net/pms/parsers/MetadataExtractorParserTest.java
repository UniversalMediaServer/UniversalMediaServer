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
package net.pms.parsers;

import java.io.File;
import java.io.IOException;
import net.pms.media.MediaInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MetadataExtractorParserTest {

	@BeforeAll
	public static void setUPClass() {
		ParserTest.setUpClass();
	}

	private static MediaInfo getTestFileMediaInfo(String testFile) {
		try {
			File file = ParserTest.getTestFile(testFile);
			MediaInfo mediaInfo = new MediaInfo();
			MetadataExtractorParser.parse(file, mediaInfo);
			return mediaInfo;
		} catch (IOException ex) {
			return null;
		}
	}

	@Test
	public void testContainerProperties() throws Exception {


		assertEquals(
			"Container: GIF, Size: 16475, GIFInfo: [Format = GIF, Resolution = 256×256, Size = 16475, Bit Depth = 8, Number of Components = 3, Color Space = [Red, Green, Blue], Color Space Type = TYPE_RGB, ImageIO Support = True, GIF Version = 87a, Has Transparency = False], Mime Type: image/gif",
			getTestFileMediaInfo("image-gif.gif").toString()
		);
		assertEquals(
			"Container: BMP, Size: 196746, BMPInfo: [Format = BMP, Resolution = 256×256, Size = 196746, Bit Depth = 8, Number of Components = 3, Color Space = [Red, Green, Blue], Color Space Type = TYPE_RGB, ImageIO Support = True, Compression Type = No compression], Mime Type: image/bmp",
			getTestFileMediaInfo("image-bmp.bmp").toString()
		);
		assertEquals(
			"Container: JPG, Size: 15919, JPEGInfo: [Format = JPEG, Resolution = 256×256, Size = 15919, Bit Depth = 8, Number of Components = 3, Color Space = [Red, Green, Blue], Color Space Type = TYPE_RGB, ImageIO Support = True, Exif Orientation = Orientation TOP_LEFT (1), Original Exif Orientation = Orientation TOP_LEFT (1), Has Exif Thumbnail = False, JFIF Version = 101, Compression Type = PROGRESSIVE_HUFFMAN, Typical Huffman = False, Chroma Subsampling = 4:4:4, Components: [0 (Y): 1 x 1, 1 (Cb): 1 x 1, 2 (Cr): 1 x 1]], Mime Type: image/jpeg",
			getTestFileMediaInfo("image-jpeg.jpg").toString()
		);
		assertEquals(
			"Container: PNG, Size: 26681, PNGInfo: [Format = PNG, Resolution = 256×256, Size = 26681, Bit Depth = 8, Number of Components = 3, Color Space = [Red, Green, Blue], Color Space Type = TYPE_RGB, ImageIO Support = True, Color Type = 2, Interlace Method = NONE, Has Transparency Chunk = False, Has Modified Bit Depth = False], Mime Type: image/png",
			getTestFileMediaInfo("image-png.png").toString()
		);
		assertEquals(
			"Container: TIFF, Size: 201178, TIFFInfo: [Format = TIFF, Resolution = 256×256, Size = 201178, Bit Depth = 8, Number of Components = 3, Color Space = [Red, Green, Blue], Color Space Type = TYPE_RGB, ImageIO Support = True, Photometric Interpretation = RGB, Exif Orientation = Orientation TOP_LEFT (1), Original Exif Orientation = Orientation TOP_LEFT (1), Exif Compression = UNCOMPRESSED, Has Exif Thumbnail = False], Mime Type: image/tiff",
			getTestFileMediaInfo("image-tiff.tiff").toString()
		);

	}

}
