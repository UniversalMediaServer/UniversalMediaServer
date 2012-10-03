package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SevenZipFile extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(SevenZipFile.class);
	private File file;
	private ISevenZipInArchive arc;

	public SevenZipFile(File f) {
		file = f;
		setLastmodified(file.lastModified());
		try {
			RandomAccessFile rf = new RandomAccessFile(f, "r");
			arc = SevenZip.openInArchive(null, (IInStream) new RandomAccessFileInStream(rf));
			ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
			for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
				LOGGER.debug("found " + item.getPath() + " in arc " + file.getAbsolutePath());

				// Skip folders for now
				if (item.isFolder()) {
					continue;
				}
				addChild(new SevenZipEntry(f, item.getPath(), item.getSize()));
			}
		} catch (IOException e) {
			LOGGER.error(null, e);
		} catch (SevenZipException e) {
			LOGGER.error(null, e);
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public String getSystemName() {
		return file.getAbsolutePath();
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public boolean isValid() {
		return file.exists();
	}

	@Override
	public long length() {
		return 0;
	}
}
