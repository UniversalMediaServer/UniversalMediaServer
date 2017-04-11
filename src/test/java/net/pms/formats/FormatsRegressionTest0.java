package net.pms.formats;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.List;
import net.pms.formats.audio.*;
import net.pms.formats.image.*;
import net.pms.formats.subtitle.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({ "deprecation", "unused" })
@SuppressFBWarnings({ "DLS_DEAD_LOCAL_STORE", "MS_SHOULD_BE_FINAL" })
public class FormatsRegressionTest0 {

	@Test
	public void test02() throws Throwable {
		ADPCM aDPCM0 = new ADPCM();
		Format format1 = aDPCM0.getSecondaryFormat();

		// Regression assertion (captures the current behavior of the code)
		assertNull(format1);
	}

	@Test
	public void test03() throws Throwable {
		Format format1 = FormatFactory.getAssociatedExtension("hi!");

		// Regression assertion (captures the current behavior of the code)
		assertNull(format1);
	}

	@Test
	public void test05() throws Throwable {
		Format format1 = FormatFactory.getAssociatedFormat("");

		// Regression assertion (captures the current behavior of the code)
		assertNull(format1);
	}

	@Test
	public void test07() throws Throwable {
		MKV mKV0 = new MKV();
		String str1 = mKV0.toString();
		String[] str_array2 = mKV0.getId();

		// Regression assertion (captures the current behavior of the code)
		assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array2);
	}

	@Test
	public void test08() throws Throwable {
		// Regression assertion (captures the current behavior of the code)
		assertTrue(new ADPCM().transcodable());
	}

	@Test
	public void test09() throws Throwable {
		List<Format> list_format0 = FormatFactory.getSupportedFormats();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(list_format0);
	}

	@Test
	public void test10() throws Throwable {
		MPG mPG0 = new MPG();
		net.pms.dlna.DLNAMediaInfo dLNAMediaInfo1 = null;
		net.pms.dlna.InputFile inputFile2 = null;
		// The following exception was thrown during execution in test
		// generation
		try {
			mPG0.parse(dLNAMediaInfo1, inputFile2, (short) 0);
			fail("Expected exception of type NullPointerException");
		} catch (NullPointerException e) {
			// Expected exception.
		}
	}

	@Test
	public void test11() throws Throwable {
		JPG jPG0 = new JPG();
		boolean b1 = jPG0.transcodable();
		String[] str_array2 = jPG0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == false);

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array2);
	}

	@Test
	public void test12() throws Throwable {
		ATRAC aTRAC0 = new ATRAC();
		String[] str_array1 = aTRAC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test13() throws Throwable {
		WavPack wavPack0 = new WavPack();
	}

	@Test
	public void test14() throws Throwable {
		SUP sUP0 = new SUP();
		String[] str_array1 = null;
		// The following exception was thrown during execution in test
		// generation
		try {
			boolean b2 = sUP0.skip(str_array1);
			fail("Expected exception of type NullPointerException");
		} catch (NullPointerException e) {
			// Expected exception.
		}
	}

	@Test
	public void test15() throws Throwable {
		int i0 = Format.SUBTITLE;

		// Regression assertion (captures the current behavior of the code)
		assertTrue(i0 == 64);
	}

	@Test
	public void test16() throws Throwable {
		WebVTT webVTT0 = new WebVTT();
		boolean b1 = webVTT0.isAudio();
		boolean b2 = webVTT0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == false);

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b2 == false);
	}

	@Test
	public void test17() throws Throwable {
		Format format0 = null;
		// The following exception was thrown during execution in test
		// generation
		try {
			boolean b1 = FormatFactory.addFormat(format0);
			fail("Expected exception of type NullPointerException");
		} catch (NullPointerException e) {
			// Expected exception.
		}
	}

	@Test
	public void test18() throws Throwable {
		ASS aSS0 = new ASS();
		String[] str_array1 = aSS0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test20() throws Throwable {
		MPG mPG0 = new MPG();
		Format.Identifier identifier1 = mPG0.getIdentifier();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.MPG + "'",
			identifier1.equals(Format.Identifier.MPG));
	}

	@Test
	public void test21() throws Throwable {
		MKV mKV0 = new MKV();
		String str1 = mKV0.toString();
		Format.Identifier identifier2 = mKV0.getIdentifier();
		boolean b3 = mKV0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));
		assertTrue("'" + identifier2 + "' != '" + Format.Identifier.MKV + "'",
			identifier2.equals(Format.Identifier.MKV));

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b3 == true);
	}

	@Test
	public void test23() throws Throwable {
		int i0 = Format.AUDIO;

		// Regression assertion (captures the current behavior of the code)
		assertTrue(i0 == 1);
	}

	@Test
	public void test24() throws Throwable {
		MKA mKA0 = new MKA();
	}

	@Test
	public void test25() throws Throwable {
		MPC mPC0 = new MPC();
		Format.Identifier identifier1 = mPC0.getIdentifier();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.MPC + "'",
			identifier1.equals(Format.Identifier.MPC));
	}

	@Test
	public void test26() throws Throwable {
		THREEG2A tHREEG2A0 = new THREEG2A();
	}

	@Test
	public void test27() throws Throwable {
		SUP sUP0 = new SUP();
		Format format1 = sUP0.getSecondaryFormat();

		// Regression assertion (captures the current behavior of the code)
		assertNull(format1);
	}

	@Test
	public void test28() throws Throwable {
		int i0 = Format.ISO;

		// Regression assertion (captures the current behavior of the code)
		assertTrue(i0 == 32);
	}

	@Test
	public void test29() throws Throwable {
		THREEGA tHREEGA0 = new THREEGA();
		String[] str_array1 = tHREEGA0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test30() throws Throwable {
		MPC mPC0 = new MPC();
		String[] str_array1 = mPC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test31() throws Throwable {
		THREEGA tHREEGA0 = new THREEGA();
		Format.Identifier identifier1 = tHREEGA0.getIdentifier();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.THREEGA + "'",
			identifier1.equals(Format.Identifier.THREEGA));
	}

	@Test
	public void test32() throws Throwable {
		JPG jPG0 = new JPG();
		boolean b1 = jPG0.transcodable();
		ISO iSO2 = new ISO();
		String[] str_array3 = iSO2.getSupportedExtensions();
		boolean b4 = jPG0.skip(str_array3);

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == false);

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array3);

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b4 == false);
	}

	@Test
	public void test33() throws Throwable {
		RA rA0 = new RA();
		Format.Identifier identifier1 = rA0.getIdentifier();
		String str2 = rA0.getMatchedExtension();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.RA + "'",
			identifier1.equals(Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		assertNull(str2);
	}

	@Test
	public void test34() throws Throwable {
		RA rA0 = new RA();
		Format.Identifier identifier1 = rA0.getIdentifier();
		boolean b2 = rA0.isUnknown();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.RA + "'",
			identifier1.equals(Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b2 == false);
	}

	@Test
	public void test35() throws Throwable {
		OGG oGG0 = new OGG();
	}

	@Test
	public void test36() throws Throwable {
		ISO iSO0 = new ISO();
		String[] str_array1 = iSO0.getSupportedExtensions();
		MKV mKV2 = new MKV();
		iSO0.setSecondaryFormat(mKV2);

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test37() throws Throwable {
		SUP sUP0 = new SUP();
		boolean b1 = FormatFactory.addFormat(sUP0);

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == true);
	}

	@Test
	public void test38() throws Throwable {
		WebVTT webVTT0 = new WebVTT();
		Format.Identifier identifier1 = webVTT0.getIdentifier();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.WEBVTT + "'",
			identifier1.equals(Format.Identifier.WEBVTT));
	}

	@Test
	public void test39() throws Throwable {
		MPC mPC0 = new MPC();
		String[] str_array1 = mPC0.getSupportedExtensions();
		boolean b3 = mPC0.match("");

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b3 == false);
	}

	@Test
	public void test40() throws Throwable {
		ISO iSO0 = new ISO();
		Format.Identifier identifier1 = iSO0.getIdentifier();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.ISO + "'",
			identifier1.equals(Format.Identifier.ISO));
	}

	@Test
	public void test41() throws Throwable {
		SubRip subRip0 = new SubRip();
		String[] str_array1 = subRip0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test42() throws Throwable {
		MKV mKV0 = new MKV();
		String str1 = mKV0.toString();
		String[] str_array2 = mKV0.getSupportedExtensions();
		String str3 = mKV0.mimeType();

		// Regression assertion (captures the current behavior of the code)
		assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array2);

		// Regression assertion (captures the current behavior of the code)
		assertTrue("'" + str3 + "' != '" + "video/mpeg" + "'", str3.equals("video/mpeg"));
	}

	@Test
	public void test43() throws Throwable {
		AIFF aIFF0 = new AIFF();
	}

	@Test
	public void test44() throws Throwable {
		int i0 = Format.IMAGE;

		// Regression assertion (captures the current behavior of the code)
		assertTrue(i0 == 2);
	}

	@Test
	public void test45() throws Throwable {
		MLP mLP0 = new MLP();
		Format.Identifier identifier1 = mLP0.getIdentifier();
		boolean b2 = mLP0.isAudio();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.MLP + "'",
			identifier1.equals(Format.Identifier.MLP));

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b2 == true);
	}

	@Test
	public void test46() throws Throwable {
		MonkeysAudio monkeysAudio0 = new MonkeysAudio();
		monkeysAudio0.setMatchedExtension("video/mpeg");
	}

	@Test
	public void test47() throws Throwable {
		Format format1 = FormatFactory.getAssociatedFormat("hi!");

		// Regression assertion (captures the current behavior of the code)
		assertNull(format1);
	}

	@Test
	public void test48() throws Throwable {
		PNG pNG0 = new PNG();
	}

	@Test
	public void test49() throws Throwable {
		MonkeysAudio monkeysAudio0 = new MonkeysAudio();
		String[] str_array1 = monkeysAudio0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test50() throws Throwable {
		MKV mKV0 = new MKV();
		String[] str_array1 = mKV0.getId();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test51() throws Throwable {
		ISO iSO0 = new ISO();
		boolean b1 = iSO0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == true);
	}

	@Test
	public void test52() throws Throwable {
		MKV mKV0 = new MKV();
		String str1 = mKV0.toString();
		String[] str_array2 = mKV0.getSupportedExtensions();
		String str3 = mKV0.toString();

		// Regression assertion (captures the current behavior of the code)
		assertTrue("'" + str1 + "' != '" + "MKV" + "'", str1.equals("MKV"));

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array2);

		// Regression assertion (captures the current behavior of the code)
		assertTrue("'" + str3 + "' != '" + "MKV" + "'", str3.equals("MKV"));
	}

	@Test
	public void test53() throws Throwable {
		SAMI sAMI0 = new SAMI();
	}

	@Test
	public void test54() throws Throwable {
		AU aU0 = new AU();
		String[] str_array1 = aU0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test55() throws Throwable {
		RA rA0 = new RA();
		Format.Identifier identifier1 = rA0.getIdentifier();
		String[] str_array2 = rA0.getSupportedExtensions();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.RA + "'",
			identifier1.equals(Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array2);
	}

	@Test
	public void test56() throws Throwable {
		WEB wEB0 = new WEB();
		boolean b1 = wEB0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == true);
	}

	@Test
	public void test57() throws Throwable {
		MP3 mP30 = new MP3();
		String[] str_array1 = mP30.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test58() throws Throwable {
		AudioAsVideo audioAsVideo0 = new AudioAsVideo();
		String[] str_array1 = audioAsVideo0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test59() throws Throwable {
		WEB wEB0 = new WEB();
		Format.Identifier identifier1 = wEB0.getIdentifier();
		boolean b2 = wEB0.transcodable();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.WEB + "'",
			identifier1.equals(Format.Identifier.WEB));

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b2 == true);
	}

	@Test
	public void test60() throws Throwable {
		SubRip subRip0 = new SubRip();
		boolean b1 = subRip0.transcodable();

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == false);
	}

	@Test
	public void test61() throws Throwable {
		int i0 = Format.PLAYLIST;

		// Regression assertion (captures the current behavior of the code)
		assertTrue(i0 == 16);
	}

	@Test
	public void test62() throws Throwable {
		FLAC fLAC0 = new FLAC();
		String[] str_array1 = fLAC0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array1);
	}

	@Test
	public void test63() throws Throwable {
		DTS dTS0 = new DTS();
		int i1 = dTS0.getType();
		String[] str_array2 = dTS0.getSupportedExtensions();

		// Regression assertion (captures the current behavior of the code)
		assertTrue(i1 == 1);

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(str_array2);
	}

	@Test
	public void test64() throws Throwable {
		SubRip subRip0 = new SubRip();
		Format.Identifier identifier1 = subRip0.getIdentifier();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.SUBRIP + "'",
			identifier1.equals(Format.Identifier.SUBRIP));
	}

	@Test
	public void test65() throws Throwable {
		RA rA0 = new RA();
		Format.Identifier identifier1 = rA0.getIdentifier();
		boolean b2 = rA0.transcodable();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.RA + "'",
			identifier1.equals(Format.Identifier.RA));

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b2 == true);
	}

	@Test
	public void test66() throws Throwable {
		SubRip subRip0 = new SubRip();
		boolean b1 = subRip0.isUnknown();

		// Regression assertion (captures the current behavior of the code)
		assertTrue(b1 == false);
	}

	@Test
	public void test67() throws Throwable {
		JPG jPG0 = new JPG();
		Format.Identifier identifier1 = jPG0.getIdentifier();
		assertTrue("'" + identifier1 + "' != '" + Format.Identifier.JPG + "'",
			identifier1.equals(Format.Identifier.JPG));
	}

}
