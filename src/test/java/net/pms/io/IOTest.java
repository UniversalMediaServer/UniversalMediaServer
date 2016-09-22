package net.pms.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

public class IOTest {
	public static void main(String[] args) throws Exception {
		List<String> cmd = new ArrayList<>();
		cmd.add("bin\\win32\\ffmpeg64.exe");
		cmd.add("-i");
		cmd.add("C:\\Users\\IBM_ADMIN\\Downloads\\jungle\\example_wma.wma");
		cmd.add("-f");
		cmd.add("mp3");
		cmd.add("pipe:");
		String[] cmdArray = cmd.toArray(new String[cmd.size()]);
		OutputParams params = new OutputParams(null);
		
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();
		InputStream file = pw.getInputStream(0);
		
		int i = -1;
		int j = 0;
		byte[] buf = new byte[2];
		while (i++ < 10 && j >= 0) {
			j = file.read();
			System.out.println(i + " " + j);
			j = file.read(buf);
			System.out.println(i + " " + j + " " + buf[0]);
//			if (i == 3)
//				file.detachInputStream();
		}
		
		FileOutputStream os = new FileOutputStream("a.mp3");
		while(file.available() != -1) {
			os.write(file.read());
		}
		file.close();
		os.close();
	}
	
//	@Test
	public void testBufferIO() throws Exception {
		OutputParams params = new OutputParams(null);
		byte[] bytes = new byte[] {3, 4}; 
		BufferedOutputFileImpl file = new BufferedOutputFileImpl(params);
		file.write(bytes);

		int i = -1;
		int j = 0;
		byte[] buf = new byte[2];
		while (i++ < 10 && j >= 0) {
			j = file.read(i == 0, i);
			System.out.println(i + " " + j);
			j = file.read(false, i, buf , 0, 1);
			System.out.println(i + " " + j + " " + buf[0]);
			if (i == 3)
				file.detachInputStream();
		}
		file.close();
	}

	public void test7Zip() {

		RandomAccessFile rf;
		File file = new File("Image.7z");

		IInArchive arc;
		try {
			// byte data[] = new byte[65536];
			rf = new RandomAccessFile(file, "r");

			arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf));
			ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
			ISimpleInArchiveItem realItem = null;

			String zeName = "";
			for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
				if (item.getPath().equals(zeName)) {
					realItem = item;
					break;
				}
			}

			if (realItem == null) {
				System.out.println("No such item " + zeName + " found in archive");
				return;
			}

			// ExtractOperationResult result = realItem.extractSlow(new
			// ISequentialOutStream() {
			// @Override
			// public int write(byte[] data) throws SevenZipException {
			// try {
			// out.write(data);
			// } catch (IOException e) {
			// System.out.println("Caught exception", e);
			// throw new SevenZipException();
			// }
			// return data.length;
			// }
			// });
			// if (result != ExtractOperationResult.OK)
			// System.out.println("Error extracting item: " + result);
		} catch (FileNotFoundException | SevenZipException e) {
			System.out.println("Unpack error. Possibly harmless." + e.getMessage());
		}
	}
}
