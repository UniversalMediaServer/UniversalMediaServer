package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import net.pms.formats.Format;
import net.pms.util.FileUtil;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SevenZipEntry extends DLNAResource implements IPushOutput {
	
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SevenZipEntry.class);
	private File z;
	private String zeName;
	private long length;
	private ISevenZipInArchive arc;
	
	public SevenZipEntry(File z, String zeName, long length) {
		this.zeName = zeName;
		this.z = z;
		this.length = length;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return zeName;
	}

	@Override
	public String getSystemName() {
		return FileUtil.getFileNameWithoutExtension(z.getAbsolutePath()) + "." + FileUtil.getExtension(zeName);
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public boolean isValid() {
		checktype();
		setSrtFile(FileUtil.doesSubtitlesExists(z, null));
		return getExt() != null;
	}

	@Override
	public long length() {
		if (getPlayer() != null && getPlayer().type() != Format.IMAGE) {
			return DLNAMediaInfo.TRANS_SIZE;
		}
		return length;
	}

	@Override
	public boolean isUnderlyingSeekSupported() {
			return length() < MAX_ARCHIVE_SIZE_SEEK;
	}

	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = new Runnable() {
			public void run() {
				try {
					RandomAccessFile rf = new RandomAccessFile(z, "r");
					arc = SevenZip.openInArchive(null,(IInStream) new RandomAccessFileInStream(rf));
					ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
					ISimpleInArchiveItem realItem = null;
					for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
						if(item.getPath().equals(zeName)) {
							realItem = item;
							break;
						}
					}
					if(realItem == null) {
						LOGGER.trace("No such item "+zeName+" found in archive");
						return;
					}
					int n = -1;
					//byte data[] = new byte[65536];
					 ExtractOperationResult result = realItem.extractSlow(new ISequentialOutStream() {
                        public int write(byte[] data) throws SevenZipException {
                        	try {
								out.write(data);
							} catch (IOException e) {
								LOGGER.debug("Caught exception", e);
								throw new SevenZipException();
							}
                        	return data.length;
                        }
					});
				} catch (Exception e) {
					LOGGER.debug("Unpack error, maybe it's normal, as backend can be terminated: " + e.getMessage());
				} finally {
					try {
						arc.close();
						out.close();
					} catch (IOException e) {
						LOGGER.debug("Caught exception", e);
					} catch (SevenZipException e) {
						LOGGER.debug("Caught exception", e);
					}
				}
			}
		};
		new Thread(r, "7Zip Extractor").start();
	}
	
	public void resolve() {
		if (getExt() == null || !getExt().isVideo()) {
			return;
		}
		boolean found = false;
		if (!found) {
			if (getMedia() == null) {
				setMedia(new DLNAMediaInfo());
			}
			found = !getMedia().isMediaparsed() && !getMedia().isParsing();
			if (getExt() != null) {
				InputFile input = new InputFile();
				input.setPush(this);
				input.setSize(length());
				getExt().parse(getMedia(), input, getType());
			}
		}
		super.resolve();
	}
	

}
