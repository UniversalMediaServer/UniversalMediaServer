package net.pms.formats;

import net.pms.formats.subtitle.SubRip;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({ "deprecation", "unused" })
@SuppressFBWarnings({ "DLS_DEAD_LOCAL_STORE", "MS_SHOULD_BE_FINAL" })
public class FormatsRegressionTest0 {

	@Test
	public void test01() throws Throwable {
		int i0 = net.pms.formats.Format.UNKNOWN;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 8);
	}

	@Test
	public void test02() throws Throwable {
		net.pms.formats.audio.ADPCM aDPCM0 = new net.pms.formats.audio.ADPCM();
		net.pms.formats.Format format1 = aDPCM0.getSecondaryFormat();
		aDPCM0.setIcon("");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);
	}

	@Test
	public void test03() throws Throwable {
		net.pms.formats.Format format1 = net.pms.formats.FormatFactory.getAssociatedExtension("hi!");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);
	}

	@Test
	public void test05() throws Throwable {
		net.pms.formats.Format format1 = net.pms.formats.FormatFactory.getAssociatedFormat("");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);
	}

	@Test
	public void test06() throws Throwable {
		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		net.pms.formats.audio.RA rA1 = new net.pms.formats.audio.RA();
		mKV0.setSecondaryFormat(rA1);
	}

	@Test
	public void test07() throws Throwable {
		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		java.lang.String str1 = mKV0.toString();
		java.lang.String[] str_array2 = mKV0.getId();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array2);
	}

	@Test
	public void test08() throws Throwable {
		net.pms.formats.audio.ADPCM aDPCM0 = new net.pms.formats.audio.ADPCM();
		boolean b1 = aDPCM0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);
	}

	@Test
	public void test09() throws Throwable {
		java.util.List<net.pms.formats.Format> list_format0 = net.pms.formats.FormatFactory.getSupportedFormats();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(list_format0);
	}

	@Test
	public void test10() throws Throwable {
		net.pms.formats.MPG mPG0 = new net.pms.formats.MPG();
		net.pms.dlna.DLNAMediaInfo dLNAMediaInfo1 = null;
		net.pms.dlna.InputFile inputFile2 = null;
		// The following exception was thrown during execution in test
		// generation
		try {
			mPG0.parse(dLNAMediaInfo1, inputFile2, (short) 0);
			org.junit.Assert.fail("Expected exception of type java.lang.NullPointerException");
		} catch (java.lang.NullPointerException e) {
			// Expected exception.
		}
	}

	@Test
	public void test11() throws Throwable {
		net.pms.formats.image.JPG jPG0 = new net.pms.formats.image.JPG();
		boolean b1 = jPG0.transcodable();
		java.lang.String[] str_array2 = jPG0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == false);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array2);
	}

	@Test
	public void test12() throws Throwable {
		net.pms.formats.audio.ATRAC aTRAC0 = new net.pms.formats.audio.ATRAC();
		java.lang.String[] str_array1 = aTRAC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test13() throws Throwable {
		net.pms.formats.audio.WavPack wavPack0 = new net.pms.formats.audio.WavPack();
	}

	@Test
	public void test14() throws Throwable {
		net.pms.formats.subtitle.SUP sUP0 = new net.pms.formats.subtitle.SUP();
		java.lang.String[] str_array1 = null;
		// The following exception was thrown during execution in test
		// generation
		try {
			boolean b2 = sUP0.skip(str_array1);
			org.junit.Assert.fail("Expected exception of type java.lang.NullPointerException");
		} catch (java.lang.NullPointerException e) {
			// Expected exception.
		}
	}

	@Test
	public void test15() throws Throwable {
		int i0 = net.pms.formats.Format.SUBTITLE;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 64);
	}

	@Test
	public void test16() throws Throwable {
		net.pms.formats.subtitle.WebVTT webVTT0 = new net.pms.formats.subtitle.WebVTT();
		boolean b1 = webVTT0.isAudio();
		boolean b2 = webVTT0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == false);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b2 == false);
	}

	@Test
	public void test17() throws Throwable {
		net.pms.formats.Format format0 = null;
		// The following exception was thrown during execution in test
		// generation
		try {
			boolean b1 = net.pms.formats.FormatFactory.addFormat(format0);
			org.junit.Assert.fail("Expected exception of type java.lang.NullPointerException");
		} catch (java.lang.NullPointerException e) {
			// Expected exception.
		}
	}

	@Test
	public void test18() throws Throwable {
		net.pms.formats.subtitle.ASS aSS0 = new net.pms.formats.subtitle.ASS();
		java.lang.String[] str_array1 = aSS0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test20() throws Throwable {
		net.pms.formats.MPG mPG0 = new net.pms.formats.MPG();
		net.pms.formats.Format.Identifier identifier1 = mPG0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.MPG + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.MPG));
	}

	@Test
	public void test21() throws Throwable {
		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		java.lang.String str1 = mKV0.toString();
		net.pms.formats.Format.Identifier identifier2 = mKV0.getIdentifier();
		boolean b3 = mKV0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));
		org.junit.Assert.assertTrue("'" + identifier2 + "' != '" + net.pms.formats.Format.Identifier.MKV + "'",
			identifier2.equals(net.pms.formats.Format.Identifier.MKV));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b3 == true);
	}

	@Test
	public void test23() throws Throwable {
		int i0 = net.pms.formats.Format.AUDIO;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 1);
	}

	@Test
	public void test24() throws Throwable {
		net.pms.formats.audio.MKA mKA0 = new net.pms.formats.audio.MKA();
	}

	@Test
	public void test25() throws Throwable {
		net.pms.formats.audio.MPC mPC0 = new net.pms.formats.audio.MPC();
		net.pms.formats.Format.Identifier identifier1 = mPC0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.MPC + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.MPC));
	}

	@Test
	public void test26() throws Throwable {
		net.pms.formats.audio.THREEG2A tHREEG2A0 = new net.pms.formats.audio.THREEG2A();
	}

	@Test
	public void test27() throws Throwable {
		net.pms.formats.subtitle.SUP sUP0 = new net.pms.formats.subtitle.SUP();
		net.pms.formats.Format format1 = sUP0.getSecondaryFormat();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);
	}

	@Test
	public void test28() throws Throwable {
		int i0 = net.pms.formats.Format.ISO;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 32);
	}

	@Test
	public void test29() throws Throwable {
		net.pms.formats.audio.THREEGA tHREEGA0 = new net.pms.formats.audio.THREEGA();
		java.lang.String[] str_array1 = tHREEGA0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test30() throws Throwable {
		net.pms.formats.audio.MPC mPC0 = new net.pms.formats.audio.MPC();
		java.lang.String[] str_array1 = mPC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test31() throws Throwable {
		net.pms.formats.audio.THREEGA tHREEGA0 = new net.pms.formats.audio.THREEGA();
		net.pms.formats.Format.Identifier identifier1 = tHREEGA0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.THREEGA + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.THREEGA));
	}

	@Test
	public void test32() throws Throwable {
		net.pms.formats.image.JPG jPG0 = new net.pms.formats.image.JPG();
		boolean b1 = jPG0.transcodable();
		net.pms.formats.ISO iSO2 = new net.pms.formats.ISO();
		java.lang.String[] str_array3 = iSO2.getSupportedExtensions();
		boolean b4 = jPG0.skip(str_array3);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == false);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array3);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b4 == false);
	}

	@Test
	public void test33() throws Throwable {
		net.pms.formats.audio.RA rA0 = new net.pms.formats.audio.RA();
		net.pms.formats.Format.Identifier identifier1 = rA0.getIdentifier();
		java.lang.String str2 = rA0.getMatchedExtension();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.RA + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(str2);
	}

	@Test
	public void test34() throws Throwable {
		net.pms.formats.audio.RA rA0 = new net.pms.formats.audio.RA();
		net.pms.formats.Format.Identifier identifier1 = rA0.getIdentifier();
		boolean b2 = rA0.isUnknown();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.RA + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b2 == false);
	}

	@Test
	public void test35() throws Throwable {
		net.pms.formats.audio.OGG oGG0 = new net.pms.formats.audio.OGG();
	}

	@Test
	public void test36() throws Throwable {
		net.pms.formats.ISO iSO0 = new net.pms.formats.ISO();
		java.lang.String[] str_array1 = iSO0.getSupportedExtensions();
		net.pms.formats.MKV mKV2 = new net.pms.formats.MKV();
		iSO0.setSecondaryFormat(mKV2);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test37() throws Throwable {
		net.pms.formats.subtitle.SUP sUP0 = new net.pms.formats.subtitle.SUP();
		boolean b1 = net.pms.formats.FormatFactory.addFormat(sUP0);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);
	}

	@Test
	public void test38() throws Throwable {
		net.pms.formats.subtitle.WebVTT webVTT0 = new net.pms.formats.subtitle.WebVTT();
		net.pms.formats.Format.Identifier identifier1 = webVTT0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.WEBVTT + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.WEBVTT));
	}

	@Test
	public void test39() throws Throwable {
		net.pms.formats.audio.MPC mPC0 = new net.pms.formats.audio.MPC();
		java.lang.String[] str_array1 = mPC0.getSupportedExtensions();
		boolean b3 = mPC0.match("");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b3 == false);
	}

	@Test
	public void test40() throws Throwable {
		net.pms.formats.ISO iSO0 = new net.pms.formats.ISO();
		net.pms.formats.Format.Identifier identifier1 = iSO0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.ISO + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.ISO));
	}

	@Test
	public void test41() throws Throwable {
		SubRip subRip0 = new SubRip();
		java.lang.String[] str_array1 = subRip0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test42() throws Throwable {
		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		java.lang.String str1 = mKV0.toString();
		java.lang.String[] str_array2 = mKV0.getSupportedExtensions();
		java.lang.String str3 = mKV0.mimeType();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array2);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue("'" + str3 + "' != '" + "video/mpeg" + "'", str3.equals("video/mpeg"));
	}

	@Test
	public void test43() throws Throwable {
		net.pms.formats.audio.AIFF aIFF0 = new net.pms.formats.audio.AIFF();
	}

	@Test
	public void test44() throws Throwable {
		int i0 = net.pms.formats.Format.IMAGE;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 2);
	}

	@Test
	public void test45() throws Throwable {
		net.pms.formats.audio.MLP mLP0 = new net.pms.formats.audio.MLP();
		net.pms.formats.Format.Identifier identifier1 = mLP0.getIdentifier();
		boolean b2 = mLP0.isAudio();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.MLP + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.MLP));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b2 == true);
	}

	@Test
	public void test46() throws Throwable {
		net.pms.formats.audio.MonkeysAudio monkeysAudio0 = new net.pms.formats.audio.MonkeysAudio();
		monkeysAudio0.setMatchedExtension("video/mpeg");
	}

	@Test
	public void test47() throws Throwable {
		net.pms.formats.Format format1 = net.pms.formats.FormatFactory.getAssociatedFormat("hi!");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);
	}

	@Test
	public void test48() throws Throwable {
		net.pms.formats.image.PNG pNG0 = new net.pms.formats.image.PNG();
	}

	@Test
	public void test49() throws Throwable {
		net.pms.formats.audio.MonkeysAudio monkeysAudio0 = new net.pms.formats.audio.MonkeysAudio();
		java.lang.String[] str_array1 = monkeysAudio0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test50() throws Throwable {
		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		java.lang.String[] str_array1 = mKV0.getId();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test51() throws Throwable {
		net.pms.formats.ISO iSO0 = new net.pms.formats.ISO();
		boolean b1 = iSO0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);
	}

	@Test
	public void test52() throws Throwable {
		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		java.lang.String str1 = mKV0.toString();
		java.lang.String[] str_array2 = mKV0.getSupportedExtensions();
		java.lang.String str3 = mKV0.toString();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array2);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue("'" + str3 + "' != '" + "MKV" + "'", str3.equals("MKV"));
	}

	@Test
	public void test53() throws Throwable {
		net.pms.formats.subtitle.SAMI sAMI0 = new net.pms.formats.subtitle.SAMI();
	}

	@Test
	public void test54() throws Throwable {
		net.pms.formats.audio.AU aU0 = new net.pms.formats.audio.AU();
		java.lang.String[] str_array1 = aU0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test55() throws Throwable {
		net.pms.formats.audio.RA rA0 = new net.pms.formats.audio.RA();
		net.pms.formats.Format.Identifier identifier1 = rA0.getIdentifier();
		java.lang.String[] str_array2 = rA0.getSupportedExtensions();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.RA + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array2);
	}

	@Test
	public void test56() throws Throwable {
		net.pms.formats.WEB wEB0 = new net.pms.formats.WEB();
		boolean b1 = wEB0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);
	}

	@Test
	public void test57() throws Throwable {
		net.pms.formats.audio.MP3 mP30 = new net.pms.formats.audio.MP3();
		java.lang.String[] str_array1 = mP30.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test58() throws Throwable {
		net.pms.formats.AudioAsVideo audioAsVideo0 = new net.pms.formats.AudioAsVideo();
		java.lang.String[] str_array1 = audioAsVideo0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test59() throws Throwable {
		net.pms.formats.WEB wEB0 = new net.pms.formats.WEB();
		net.pms.formats.Format.Identifier identifier1 = wEB0.getIdentifier();
		boolean b2 = wEB0.transcodable();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.WEB + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.WEB));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b2 == true);
	}

	@Test
	public void test60() throws Throwable {
		SubRip subRip0 = new SubRip();
		boolean b1 = subRip0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == false);
	}

	@Test
	public void test61() throws Throwable {
		int i0 = net.pms.formats.Format.PLAYLIST;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 16);
	}

	@Test
	public void test62() throws Throwable {
		net.pms.formats.audio.FLAC fLAC0 = new net.pms.formats.audio.FLAC();
		java.lang.String[] str_array1 = fLAC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);
	}

	@Test
	public void test63() throws Throwable {
		net.pms.formats.audio.DTS dTS0 = new net.pms.formats.audio.DTS();
		int i1 = dTS0.getType();
		java.lang.String[] str_array2 = dTS0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i1 == 1);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array2);
	}

	@Test
	public void test64() throws Throwable {
		SubRip subRip0 = new SubRip();
		net.pms.formats.Format.Identifier identifier1 = subRip0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.SUBRIP + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.SUBRIP));
	}

	@Test
	public void test65() throws Throwable {
		net.pms.formats.audio.RA rA0 = new net.pms.formats.audio.RA();
		net.pms.formats.Format.Identifier identifier1 = rA0.getIdentifier();
		boolean b2 = rA0.transcodable();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.RA + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b2 == true);
	}

	@Test
	public void test66() throws Throwable {
		SubRip subRip0 = new SubRip();
		boolean b1 = subRip0.isUnknown();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == false);
	}

	@Test
	public void test67() throws Throwable {
		net.pms.formats.image.JPG jPG0 = new net.pms.formats.image.JPG();
		net.pms.formats.Format.Identifier identifier1 = jPG0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.JPG + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.JPG));
	}

}
