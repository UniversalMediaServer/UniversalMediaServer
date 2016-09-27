package net.pms.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

public class IOTest {
	public static void main(String[] args) throws Exception {
		new IOTest().testFileConversion();
	}
	
	public void testFileConversion() throws Exception {
		// Test file conversion
		List<String> cmd = new ArrayList<>();
		cmd.add("bin\\win32\\ffmpeg64.exe");
		cmd.add("-i");
		cmd.add("C:\\Users\\IBM_ADMIN\\Downloads\\jungle\\utbr.mp3");
		cmd.add("-vn");
		cmd.add("-f");
		cmd.add("mp3");
		cmd.add("pipe:");
		String[] cmdArray = cmd.toArray(new String[cmd.size()]);
		OutputParams params = new OutputParams(null);
		
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();
		InputStream file = pw.getInputStream(0);
		PushbackInputStream pis = new PushbackInputStream(file);
		
		FileOutputStream os = new FileOutputStream("a.mp3");
		int read = file.read();
//		while(read != -1) {
//			os.write(read);
//			read = file.read();
//		}
		
		read = pis.read();
		byte[] bytes = new byte[1024 * 1024];
		while (read != -1) {
			read = pis.read();
			pis.unread(read);
			read = pis.read(bytes, 0, bytes.length);
			System.out.println(read);
			os.write(bytes);

			read = pis.read();
			pis.unread(read);
			System.out.println(read);
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
		File file = new File("Image_7z.7z");

		IInArchive arc;
		try {
			// byte data[] = new byte[65536];
			rf = new RandomAccessFile(file, "r");

			arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf));
			ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
			ISimpleInArchiveItem realItem = null;

			String zeName = "Image\\color_blind.jpg";
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

			final FileOutputStream out = new FileOutputStream("a.jpg"); 
			ExtractOperationResult result = realItem.extractSlow(new ISequentialOutStream() {
				@Override
				public int write(byte[] data) throws SevenZipException {
					try {
						out.write(data);
					} catch (IOException e) {
						System.out.println("Caught exception" + e);
						throw new SevenZipException();
					}
					return data.length;
				}
			});
			if (result != ExtractOperationResult.OK)
				System.out.println("Error extracting item: " + result);
			
			out.close();
			arc.close();
			rf.close();
		} catch (Exception e) {
			System.out.println("Unpack error. Possibly harmless." + e.getMessage());
		}
	}
}
