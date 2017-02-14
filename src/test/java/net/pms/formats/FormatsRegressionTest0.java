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

	public static boolean debug = false;

	@Test
	public void test01() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test01");
		}

		int i0 = net.pms.formats.Format.UNKNOWN;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 8);

	}

	@Test
	public void test02() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test02");
		}

		net.pms.formats.audio.ADPCM aDPCM0 = new net.pms.formats.audio.ADPCM();
		net.pms.formats.Format format1 = aDPCM0.getSecondaryFormat();
		aDPCM0.setIcon("");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);

	}

	@Test
	public void test03() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test03");
		}

		net.pms.formats.Format format1 = net.pms.formats.FormatFactory.getAssociatedExtension("hi!");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);

	}

	@Test
	public void test05() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test05");
		}

		net.pms.formats.Format format1 = net.pms.formats.FormatFactory.getAssociatedFormat("");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);

	}

	@Test
	public void test06() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test06");
		}

		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		net.pms.formats.audio.RA rA1 = new net.pms.formats.audio.RA();
		mKV0.setSecondaryFormat((net.pms.formats.Format) rA1);

	}

	@Test
	public void test07() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test07");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test08");
		}

		net.pms.formats.audio.ADPCM aDPCM0 = new net.pms.formats.audio.ADPCM();
		boolean b1 = aDPCM0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);

	}

	@Test
	public void test09() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test09");
		}

		java.util.List<net.pms.formats.Format> list_format0 = net.pms.formats.FormatFactory.getSupportedFormats();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(list_format0);

	}

	@Test
	public void test10() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test10");
		}

		net.pms.formats.MPG mPG0 = new net.pms.formats.MPG();
		net.pms.dlna.DLNAMediaInfo dLNAMediaInfo1 = null;
		net.pms.dlna.InputFile inputFile2 = null;
		// The following exception was thrown during execution in test
		// generation
		try {
			mPG0.parse(dLNAMediaInfo1, inputFile2, (int) (short) 0);
			org.junit.Assert.fail("Expected exception of type java.lang.NullPointerException");
		} catch (java.lang.NullPointerException e) {
			// Expected exception.
		}

	}

	@Test
	public void test11() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test11");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test12");
		}

		net.pms.formats.audio.ATRAC aTRAC0 = new net.pms.formats.audio.ATRAC();
		java.lang.String[] str_array1 = aTRAC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test13() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test13");
		}

		net.pms.formats.audio.WavPack wavPack0 = new net.pms.formats.audio.WavPack();

	}

	@Test
	public void test14() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test14");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test15");
		}

		int i0 = net.pms.formats.Format.SUBTITLE;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 64);

	}

	@Test
	public void test16() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test16");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test17");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test18");
		}

		net.pms.formats.subtitle.ASS aSS0 = new net.pms.formats.subtitle.ASS();
		java.lang.String[] str_array1 = aSS0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test20() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test20");
		}

		net.pms.formats.MPG mPG0 = new net.pms.formats.MPG();
		net.pms.formats.Format.Identifier identifier1 = mPG0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.MPG + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.MPG));

	}

	@Test
	public void test21() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test21");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test23");
		}

		int i0 = net.pms.formats.Format.AUDIO;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 1);

	}

	@Test
	public void test24() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test24");
		}

		net.pms.formats.audio.MKA mKA0 = new net.pms.formats.audio.MKA();

	}

	@Test
	public void test25() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test25");
		}

		net.pms.formats.audio.MPC mPC0 = new net.pms.formats.audio.MPC();
		net.pms.formats.Format.Identifier identifier1 = mPC0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.MPC + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.MPC));

	}

	@Test
	public void test26() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test26");
		}

		net.pms.formats.audio.THREEG2A tHREEG2A0 = new net.pms.formats.audio.THREEG2A();

	}

	@Test
	public void test27() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test27");
		}

		net.pms.formats.subtitle.SUP sUP0 = new net.pms.formats.subtitle.SUP();
		net.pms.formats.Format format1 = sUP0.getSecondaryFormat();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);

	}

	@Test
	public void test28() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test28");
		}

		int i0 = net.pms.formats.Format.ISO;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 32);

	}

	@Test
	public void test29() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test29");
		}

		net.pms.formats.audio.THREEGA tHREEGA0 = new net.pms.formats.audio.THREEGA();
		java.lang.String[] str_array1 = tHREEGA0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test30() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test30");
		}

		net.pms.formats.audio.MPC mPC0 = new net.pms.formats.audio.MPC();
		java.lang.String[] str_array1 = mPC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test31() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test31");
		}

		net.pms.formats.audio.THREEGA tHREEGA0 = new net.pms.formats.audio.THREEGA();
		net.pms.formats.Format.Identifier identifier1 = tHREEGA0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.THREEGA + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.THREEGA));

	}

	@Test
	public void test32() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test32");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test33");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test34");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test35");
		}

		net.pms.formats.audio.OGG oGG0 = new net.pms.formats.audio.OGG();

	}

	@Test
	public void test36() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test36");
		}

		net.pms.formats.ISO iSO0 = new net.pms.formats.ISO();
		java.lang.String[] str_array1 = iSO0.getSupportedExtensions();
		net.pms.formats.MKV mKV2 = new net.pms.formats.MKV();
		iSO0.setSecondaryFormat((net.pms.formats.Format) mKV2);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test37() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test37");
		}

		net.pms.formats.subtitle.SUP sUP0 = new net.pms.formats.subtitle.SUP();
		boolean b1 = net.pms.formats.FormatFactory.addFormat((net.pms.formats.Format) sUP0);

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);

	}

	@Test
	public void test38() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test38");
		}

		net.pms.formats.subtitle.WebVTT webVTT0 = new net.pms.formats.subtitle.WebVTT();
		net.pms.formats.Format.Identifier identifier1 = webVTT0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.WEBVTT + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.WEBVTT));

	}

	@Test
	public void test39() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test39");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test40");
		}

		net.pms.formats.ISO iSO0 = new net.pms.formats.ISO();
		net.pms.formats.Format.Identifier identifier1 = iSO0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.ISO + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.ISO));

	}

	@Test
	public void test41() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test41");
		}

		SubRip subRip0 = new SubRip();
		java.lang.String[] str_array1 = subRip0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test42() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test42");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test43");
		}

		net.pms.formats.audio.AIFF aIFF0 = new net.pms.formats.audio.AIFF();

	}

	@Test
	public void test44() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test44");
		}

		int i0 = net.pms.formats.Format.IMAGE;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 2);

	}

	@Test
	public void test45() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test45");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test46");
		}

		net.pms.formats.audio.MonkeysAudio monkeysAudio0 = new net.pms.formats.audio.MonkeysAudio();
		monkeysAudio0.setMatchedExtension("video/mpeg");

	}

	@Test
	public void test47() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test47");
		}

		net.pms.formats.Format format1 = net.pms.formats.FormatFactory.getAssociatedFormat("hi!");

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNull(format1);

	}

	@Test
	public void test48() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test48");
		}

		net.pms.formats.image.PNG pNG0 = new net.pms.formats.image.PNG();

	}

	@Test
	public void test49() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test49");
		}

		net.pms.formats.audio.MonkeysAudio monkeysAudio0 = new net.pms.formats.audio.MonkeysAudio();
		java.lang.String[] str_array1 = monkeysAudio0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test50() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test50");
		}

		net.pms.formats.MKV mKV0 = new net.pms.formats.MKV();
		java.lang.String[] str_array1 = mKV0.getId();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test51() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test51");
		}

		net.pms.formats.ISO iSO0 = new net.pms.formats.ISO();
		boolean b1 = iSO0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);

	}

	@Test
	public void test52() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test52");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test53");
		}

		net.pms.formats.subtitle.SAMI sAMI0 = new net.pms.formats.subtitle.SAMI();

	}

	@Test
	public void test54() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test54");
		}

		net.pms.formats.audio.AU aU0 = new net.pms.formats.audio.AU();
		java.lang.String[] str_array1 = aU0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test55() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test55");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test56");
		}

		net.pms.formats.WEB wEB0 = new net.pms.formats.WEB();
		boolean b1 = wEB0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == true);

	}

	@Test
	public void test57() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test57");
		}

		net.pms.formats.audio.MP3 mP30 = new net.pms.formats.audio.MP3();
		java.lang.String[] str_array1 = mP30.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test58() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test58");
		}

		net.pms.formats.AudioAsVideo audioAsVideo0 = new net.pms.formats.AudioAsVideo();
		java.lang.String[] str_array1 = audioAsVideo0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test59() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test59");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test60");
		}

		SubRip subRip0 = new SubRip();
		boolean b1 = subRip0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == false);

	}

	@Test
	public void test61() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test61");
		}

		int i0 = net.pms.formats.Format.PLAYLIST;

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(i0 == 16);

	}

	@Test
	public void test62() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test62");
		}

		net.pms.formats.audio.FLAC fLAC0 = new net.pms.formats.audio.FLAC();
		java.lang.String[] str_array1 = fLAC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertNotNull(str_array1);

	}

	@Test
	public void test63() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test63");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test64");
		}

		SubRip subRip0 = new SubRip();
		net.pms.formats.Format.Identifier identifier1 = subRip0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.SUBRIP + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.SUBRIP));

	}

	@Test
	public void test65() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test65");
		}

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

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test66");
		}

		SubRip subRip0 = new SubRip();
		boolean b1 = subRip0.isUnknown();

		// Regression assertion (captures the current behavior of the code)
		org.junit.Assert.assertTrue(b1 == false);

	}

	@Test
	public void test67() throws Throwable {

		if (debug) {
			System.out.format("%n%s%n", "FormatsRegressionTest0.test67");
		}

		net.pms.formats.image.JPG jPG0 = new net.pms.formats.image.JPG();
		net.pms.formats.Format.Identifier identifier1 = jPG0.getIdentifier();
		org.junit.Assert.assertTrue("'" + identifier1 + "' != '" + net.pms.formats.Format.Identifier.JPG + "'",
			identifier1.equals(net.pms.formats.Format.Identifier.JPG));

	}

}
