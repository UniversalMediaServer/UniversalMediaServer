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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.pms.image.BMPInfo;
import net.pms.image.BufferedImageFilter;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.CURInfo;
import net.pms.image.ColorSpaceType;
import net.pms.image.ExifInfo;
import net.pms.image.ExifOrientation;
import net.pms.image.GIFInfo;
import net.pms.image.GenericImageInfo;
import net.pms.image.ICOInfo;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.JPEGInfo;
import net.pms.image.JPEGSubsamplingNotation;
import net.pms.image.PCXInfo;
import net.pms.image.PNGInfo;
import net.pms.image.PSDInfo;
import net.pms.image.RAWInfo;
import net.pms.image.TIFFInfo;
import net.pms.image.WebPInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Surf@ceS
 */
public class DLNAThumbnailFixer {

	/**
	 * This class is not meant to be instantiated.
	 */
	protected DLNAThumbnailFixer() {
	}

	public static DLNAThumbnail fixDLNAThumbnail(InputStream is) throws IOException {
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return (DLNAThumbnail) ois.readObject();
		} catch (InvalidClassException ex) {
			String classname = ex.classname;
			String message = ex.getMessage();
			if (message.contains("local class incompatible: stream classdesc serialVersionUID = ")) {
				//bad uid, replace it
				String oldUidStr = message.substring(message.indexOf(" = ") + 3, message.indexOf(","));
				String newUidStr = message.substring(message.lastIndexOf(" = ") + 3);
				Long oldUid;
				Long newUid;
				try {
					oldUid = Long.valueOf(oldUidStr);
					newUid = Long.valueOf(newUidStr);
				} catch (NumberFormatException ex2) {
					IOException ioe = new StreamCorruptedException("class invalid");
					ioe.initCause(ex2);
					throw ioe;
				}
				InputStream correctedIs = replaceSerialVersionUID(is, classname, oldUid, newUid);
				return fixDLNAThumbnail(correctedIs);
			} else {
				IOException ioe = new StreamCorruptedException("class invalid");
				ioe.initCause(ex);
				throw ioe;
			}
		} catch (ClassNotFoundException ex) {
			String oldClassname = ex.getMessage();
			String newClassname = getRefactoredClassname(oldClassname);
			if (newClassname != null && !newClassname.equals(oldClassname)) {
				InputStream correctedIs = replaceRefactoredClass(is, oldClassname, newClassname);
				return fixDLNAThumbnail(correctedIs);
			} else {
				IOException ioe = new StreamCorruptedException("class missing");
				ioe.initCause(ex);
				throw ioe;
			}
		}
	}

	private static InputStream replaceSerialVersionUID(InputStream is, String classname, Long oldUid, Long newUid) throws IOException {
		is.reset();
		byte[] classnameBytes = getClassnameBytes(classname);
		byte[] oldUidBytes = ByteBuffer.allocate(8).putLong(oldUid).array();
		byte[] newUidBytes = ByteBuffer.allocate(8).putLong(newUid).array();
		byte[] search = ArrayUtils.addAll(classnameBytes, oldUidBytes);
		byte[] replacement = ArrayUtils.addAll(classnameBytes, newUidBytes);
		byte[] oldBytes = IOUtils.toByteArray(is);
		byte[] newBytes = replaceAll(oldBytes, search, replacement);
		return new ByteArrayInputStream(newBytes);
	}

	private static String getRefactoredClassname(String classname) {
		if (classname == null) {
			return null;
		}
		if (classname.contains(".")) {
			classname = classname.substring(classname.lastIndexOf(".") + 1);
		}
		return switch (classname) {
			case "BMPInfo" ->
				BMPInfo.class.getName();
			case "BMPInfo$BMPParseInfo" ->
				BMPInfo.class.getName() + "$BMPParseInfo";
			case "BMPInfo$CompressionType" ->
				BMPInfo.class.getName() + "$CompressionType";
			case "BufferedImageFilter" ->
				BufferedImageFilter.class.getName();
			case "BufferedImageFilter$BufferedImageFilterResult" ->
				BufferedImageFilter.class.getName() + "$BufferedImageFilterResult";
			case "BufferedImageFilterChain" ->
				BufferedImageFilterChain.class.getName();
			case "CURInfo" ->
				CURInfo.class.getName();
			case "ColorSpaceType" ->
				ColorSpaceType.class.getName();
			case "DLNAThumbnail" ->
				DLNAThumbnail.class.getName();
			case "ExifInfo" ->
				ExifInfo.class.getName();
			case "ExifInfo$ExifColorSpace" ->
				ExifInfo.class.getName() + "$ExifColorSpace";
			case "ExifInfo$ExifCompression" ->
				ExifInfo.class.getName() + "$ExifCompression";
			case "ExifInfo$ExifParseInfo" ->
				ExifInfo.class.getName() + "$ExifParseInfo";
			case "ExifInfo$PhotometricInterpretation" ->
				ExifInfo.class.getName() + "$PhotometricInterpretation";
			case "ExifOrientation" ->
				ExifOrientation.class.getName();
			case "GIFInfo" ->
				GIFInfo.class.getName();
			case "GIFInfo$GIFParseInfo" ->
				GIFInfo.class.getName() + "$GIFParseInfo";
			case "GenericImageInfo" ->
				GenericImageInfo.class.getName();
			case "ICOInfo" ->
				ICOInfo.class.getName();
			case "Image" ->
				Image.class.getName();
			case "ImageFormat" ->
				ImageFormat.class.getName();
			case "ImageInfo" ->
				ImageInfo.class.getName();
			case "ImageInfo$ParseInfo" ->
				ImageInfo.class.getName() + "$ParseInfo";
			case "JPEGInfo" ->
				JPEGInfo.class.getName();
			case "JPEGInfo$CompressionType" ->
				JPEGInfo.class.getName() + "$CompressionType";
			case "JPEGInfo$JPEGParseInfo" ->
				JPEGInfo.class.getName() + "$JPEGParseInfo";
			case "JPEGSubsamplingNotation" ->
				JPEGSubsamplingNotation.class.getName();
			case "PCXInfo" ->
				PCXInfo.class.getName();
			case "PNGInfo" ->
				PNGInfo.class.getName();
			case "PNGInfo$InterlaceMethod" ->
				PNGInfo.class.getName() + "$InterlaceMethod";
			case "PNGInfo$PNGParseInfo" ->
				PNGInfo.class.getName() + "$PNGParseInfo";
			case "PSDInfo" ->
				PSDInfo.class.getName();
			case "PSDInfo$ColorMode" ->
				PSDInfo.class.getName() + "$ColorMode";
			case "PSDInfo$PSDParseInfo" ->
				PSDInfo.class.getName() + "$PSDParseInfo";
			case "RAWInfo" ->
				RAWInfo.class.getName();
			case "TIFFInfo" ->
				TIFFInfo.class.getName();
			case "WebPInfo" ->
				WebPInfo.class.getName();
			default ->
				null;
		};
	}

	private static InputStream replaceRefactoredClass(InputStream is, String oldClassname, String newClassname) throws IOException {
		is.reset();
		byte[] search = getClassnameBytes(oldClassname);
		byte[] replacement = getClassnameBytes(newClassname);
		byte[] oldBytes = IOUtils.toByteArray(is);
		byte[] newBytes = replaceAll(oldBytes, search, replacement);
		return new ByteArrayInputStream(newBytes);
	}

	private static byte[] getClassnameBytes(String classname) {
		byte[] classnameStrBytes = classname.getBytes();
		byte[] classnameLenBytes = ByteBuffer.allocate(2).putShort((short) classnameStrBytes.length).array();
		return ArrayUtils.addAll(classnameLenBytes, classnameStrBytes);
	}

	private static byte[] replaceAll(byte[] src, byte[] search, byte[] replacement) {
		if (src == null || search == null || Arrays.compare(search, replacement) == 0) {
			return src;
		}
		int index = findBytes(src, search);
		if (index < 0) {
			return src;
		}
		if (replacement == null) {
			replacement = new byte[0];
		}
		byte[] dst = null;
		while (index > -1) {
			dst = new byte[src.length - search.length + replacement.length];
			System.arraycopy(src, 0, dst, 0, index);
			System.arraycopy(replacement, 0, dst, index, replacement.length);
			System.arraycopy(src, index + search.length, dst, index + replacement.length, src.length - (index + search.length));
			index = findBytes(dst, search);
		}
		return dst;
	}

	private static int findBytes(byte[] src, byte[] find) {
		if (src == null || find == null || src.length == 0 || find.length == 0 || find.length > src.length) {
			return -1;
		}
		for (int i = 0; i < src.length - find.length + 1; i++) {
			if (src[i] == find[0]) {
				for (int m = 1; m < find.length; m++) {
					if (src[i + m] != find[m]) {
						break;
					}
					if (m == find.length - 1) {
						return i;
					}
				}
			}
		}
		return -1;
	}

}
