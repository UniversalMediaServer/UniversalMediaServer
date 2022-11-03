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
package net.pms.configuration;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class FormatConfigurationRegressionTest {

	private static void testFormatConfiguration(String format, String name) {
		// Regression assertion (captures the current behavior of the code)
		assertTrue(format.equals(name), "'" + format + "' != '" + name + "'");
	}

	@Test
	public void test01() throws Throwable {
		testFormatConfiguration(FormatConfiguration.H263, "h263");
	}

	@Test
	public void test02() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AVI, "avi");
	}

	@Test
	public void test03() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AU, "au");
	}

	@Test
	public void test04() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MPEGPS, "mpegps");
	}

	@Test
	public void test05() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WMALOSSLESS, "wmalossless");
	}

	@Test
	public void test06() throws Throwable {
		testFormatConfiguration(FormatConfiguration.FLV, "flv");
	}

	@Test
	public void test07() throws Throwable {
		testFormatConfiguration(FormatConfiguration.OPUS, "opus");
	}

	@Test
	public void test08() throws Throwable {
		testFormatConfiguration(FormatConfiguration.COOK, "cook");
	}

	@Test
	public void test09() throws Throwable {
		testFormatConfiguration(FormatConfiguration.ADTS, "adts");
	}

	@Test
	public void test10() throws Throwable {
		testFormatConfiguration(FormatConfiguration.RA, "ra");
	}

	@Test
	public void test11() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MKV, "mkv");
	}

	@Test
	public void test12() throws Throwable {
		testFormatConfiguration(FormatConfiguration.VC1, "vc1");
	}

	@Test
	public void test13() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MP2, "mp2");
	}

	@Test
	public void test14() throws Throwable {
		testFormatConfiguration(FormatConfiguration.LPCM, "lpcm");
	}

	@Test
	public void test15() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MPEG2, "mpeg2");
	}

	@Test
	public void test16() throws Throwable {
		testFormatConfiguration(FormatConfiguration.M4A, "m4a");
	}

	@Test
	public void test17() throws Throwable {
		testFormatConfiguration(FormatConfiguration.DIVX, "divx");
	}

	@Test
	public void test18() throws Throwable {
		testFormatConfiguration(FormatConfiguration.THREEGPP2, "3g2");
	}

	@Test
	public void test19() throws Throwable {
		testFormatConfiguration(FormatConfiguration.OGG, "ogg");
	}

	@Test
	public void test20() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AC3, "ac3");
	}

	@Test
	public void test21() throws Throwable {
		testFormatConfiguration(FormatConfiguration.ATMOS, "atmos");
	}

	@Test
	public void test22() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WMAPRO, "wmapro");
	}

	@Test
	public void test23() throws Throwable {
		testFormatConfiguration(FormatConfiguration.GIF, "gif");
	}

	@Test
	public void test24() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MP4, "mp4");
	}

	@Test
	public void test25() throws Throwable {
		testFormatConfiguration(FormatConfiguration.H264, "h264");
	}

	@Test
	public void test26() throws Throwable {
		testFormatConfiguration(FormatConfiguration.ATRAC, "atrac");
	}

	@Test
	public void test27() throws Throwable {
		testFormatConfiguration(FormatConfiguration.HE_AAC, "he-aac");
	}

	@Test
	public void test28() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WEBM, "webm");
	}

	@Test
	public void test29() throws Throwable {
		testFormatConfiguration(FormatConfiguration.TIFF, "tiff");
	}

	@Test
	public void test30() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WAV, "wav");
	}

	@Test
	public void test31() throws Throwable {
		testFormatConfiguration(FormatConfiguration.JPG, "jpg");
	}

	@Test
	public void test32() throws Throwable {
		testFormatConfiguration(FormatConfiguration.VP9, "vp9");
	}

	@Test
	public void test33() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MI_GOP, "gop");
	}

	@Test
	public void test34() throws Throwable {
		testFormatConfiguration(FormatConfiguration.VP8, "vp8");
	}

	@Test
	public void test35() throws Throwable {
		testFormatConfiguration(FormatConfiguration.THREEGA, "3ga");
	}

	@Test
	public void test36() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MONKEYS_AUDIO, "ape");
	}

	@Test
	public void test37() throws Throwable {
		testFormatConfiguration(FormatConfiguration.RALF, "ralf");
	}

	@Test
	public void test38() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MKA, "mka");
	}

	@Test
	public void test39() throws Throwable {
		testFormatConfiguration(FormatConfiguration.DSF, "dsf");
	}

	@Test
	public void test40() throws Throwable {
		testFormatConfiguration(FormatConfiguration.CINEPAK, "cvid");
	}

	@Test
	public void test41() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MIMETYPE_AUTO, "MIMETYPE_AUTO");
	}

	@Test
	public void test42() throws Throwable {
		testFormatConfiguration(FormatConfiguration.VP7, "vp7");
	}

	@Test
	public void test43() throws Throwable {
		testFormatConfiguration(FormatConfiguration.THREEGPP, "3gp");
	}

	@Test
	public void test44() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MJPEG, "mjpeg");
	}

	@Test
	public void test45() throws Throwable {
		testFormatConfiguration(FormatConfiguration.BMP, "bmp");
	}

	@Test
	public void test46() throws Throwable {
		testFormatConfiguration(FormatConfiguration.TTA, "tta");
	}

	@Test
	public void test47() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AAC_LC, "aac-lc");
	}

	@Test
	public void test48() throws Throwable {
		testFormatConfiguration(FormatConfiguration.PNG, "png");
	}

	@Test
	public void test49() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WMV, "wmv");
	}

	@Test
	public void test50() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MPEGTS, "mpegts");
	}

	@Test
	public void test51() throws Throwable {
		testFormatConfiguration(FormatConfiguration.DTS, "dts");
	}

	@Test
	public void test52() throws Throwable {
		testFormatConfiguration(FormatConfiguration.VP6, "vp6");
	}

	@Test
	public void test53() throws Throwable {
		testFormatConfiguration(FormatConfiguration.QDESIGN, "qdmc");
	}

	@Test
	public void test54() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MPA, "mpa");
	}

	@Test
	public void test55() throws Throwable {
		testFormatConfiguration(FormatConfiguration.TRUEHD, "truehd");
	}

	@Test
	public void test56() throws Throwable {
		testFormatConfiguration(FormatConfiguration.ALAC, "alac");
	}

	@Test
	public void test57() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MPEG1, "mpeg1");
	}

	@Test
	public void test58() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MPC, "mpc");
	}

	@Test
	public void test59() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MI_QPEL, "qpel");
	}

	@Test
	public void test60() throws Throwable {
		testFormatConfiguration(FormatConfiguration.EAC3, "eac3");
	}

	@Test
	public void test61() throws Throwable {
		testFormatConfiguration(FormatConfiguration.VORBIS, "vorbis");
	}

	@Test
	public void test62() throws Throwable {
		testFormatConfiguration(FormatConfiguration.UND, "und");
	}

	@Test
	public void test63() throws Throwable {
		testFormatConfiguration(FormatConfiguration.H265, "h265");
	}

	@Test
	public void test64() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WMA, "wma");
	}

	@Test
	public void test65() throws Throwable {
		testFormatConfiguration(FormatConfiguration.THEORA, "theora");
	}

	@Test
	public void test66() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MI_GMC, "gmc");
	}

	@Test
	public void test67() throws Throwable {
		testFormatConfiguration(FormatConfiguration.SHORTEN, "shn");
	}

	@Test
	public void test68() throws Throwable {
		testFormatConfiguration(FormatConfiguration.ADPCM, "adpcm");
	}

	@Test
	public void test69() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WMAVOICE, "wmavoice");
	}

	@Test
	public void test70() throws Throwable {
		testFormatConfiguration(FormatConfiguration.DV, "dv");
	}

	@Test
	public void test71() throws Throwable {
		testFormatConfiguration(FormatConfiguration.FLAC, "flac");
	}

	@Test
	public void test72() throws Throwable {
		testFormatConfiguration(FormatConfiguration.RM, "rm");
	}

	@Test
	public void test73() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WAVPACK, "wavpack");
	}

	@Test
	public void test74() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MOV, "mov");
	}

	@Test
	public void test75() throws Throwable {
		testFormatConfiguration(FormatConfiguration.SORENSON, "sor");
	}

	@Test
	public void test76() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MLP, "mlp");
	}

	@Test
	public void test77() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AMR, "amr");
	}

	@Test
	public void test78() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MP3, "mp3");
	}

	@Test
	public void test79() throws Throwable {
		testFormatConfiguration(FormatConfiguration.DTSHD, "dtshd");
	}

	@Test
	public void test80() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AIFF, "aiff");
	}

	@Test
	public void test81() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MI_VBD, "vbd");
	}

	@Test
	public void testALS() throws Throwable {
		testFormatConfiguration(FormatConfiguration.ALS, "als");
	}

	@Test
	public void testOGA() throws Throwable {
		testFormatConfiguration(FormatConfiguration.OGA, "oga");
	}

	@Test
	public void testRealAudio_14_4() throws Throwable {
		testFormatConfiguration(FormatConfiguration.REALAUDIO_14_4, "ra14.4");
	}

	@Test
	public void testRealAudio_28_8() throws Throwable {
		testFormatConfiguration(FormatConfiguration.REALAUDIO_28_8, "ra28.8");
	}

	@Test
	public void testRALF() throws Throwable {
		testFormatConfiguration(FormatConfiguration.RALF, "ralf");
	}

	@Test
	public void testSipro() throws Throwable {
		testFormatConfiguration(FormatConfiguration.SIPRO, "sipro");
	}

	@Test
	public void testACELP() throws Throwable {
		testFormatConfiguration(FormatConfiguration.ACELP, "acelp");
	}

		@Test
	public void testG729() throws Throwable {
		testFormatConfiguration(FormatConfiguration.G729, "g729");
	}

	@Test
	public void testWMA10() throws Throwable {
		testFormatConfiguration(FormatConfiguration.WMA10, "wma10");
	}

	@Test
	public void testDFF() throws Throwable {
		testFormatConfiguration(FormatConfiguration.DFF, "dff");
	}

	@Test
	public void testH261() throws Throwable {
		testFormatConfiguration(FormatConfiguration.H261, "h261");
	}

	@Test
	public void testINDEO() throws Throwable {
		testFormatConfiguration(FormatConfiguration.INDEO, "indeo");
	}

	@Test
	public void testRGB() throws Throwable {
		testFormatConfiguration(FormatConfiguration.RGB, "rgb");
	}

	@Test
	public void testDOLBYE() throws Throwable {
		testFormatConfiguration(FormatConfiguration.DOLBYE, "dolbye");
	}

	@Test
	public void testYUV() throws Throwable {
		testFormatConfiguration(FormatConfiguration.YUV, "yuv");
	}

	@Test
	public void testNELLYMOSER() throws Throwable {
		testFormatConfiguration(FormatConfiguration.NELLYMOSER, "nellymoser");
	}

	@Test
	public void testAAC_LTP() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AAC_LTP, "aac-ltp");
	}

	@Test
	public void testRLE() throws Throwable {
		testFormatConfiguration(FormatConfiguration.RLE, "rle");
	}

	@Test
	public void testAAC_MAIN() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AAC_MAIN, "aac-main");
	}

	@Test
	public void testAAC_SSR() throws Throwable {
		testFormatConfiguration(FormatConfiguration.AAC_SSR, "aac-ssr");
	}

	@Test
	public void testCAF() throws Throwable {
		testFormatConfiguration(FormatConfiguration.CAF, "caf");
	}

	@Test
	public void testMACE3() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MACE3, "mace3");
	}

	@Test
	public void testMACE6() throws Throwable {
		testFormatConfiguration(FormatConfiguration.MACE6, "mace6");
	}

	@Test
	public void testTGA() throws Throwable {
		testFormatConfiguration(FormatConfiguration.TGA, "tga");
	}

	@Test
	public void testFFV1() throws Throwable {
		testFormatConfiguration(FormatConfiguration.FFV1, "ffv1");
	}

	@Test
	public void testCELP() throws Throwable {
		testFormatConfiguration(FormatConfiguration.CELP, "celp");
	}

	@Test
	public void testQCELP() throws Throwable {
		testFormatConfiguration(FormatConfiguration.QCELP, "qcelp");
	}
}
