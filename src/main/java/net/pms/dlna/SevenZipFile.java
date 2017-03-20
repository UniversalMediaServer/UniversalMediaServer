/*
 * Universal Media Server
 * Copyright (C) 2008  SharkHunter
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
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
		setLastModified(file.lastModified());
		try {
			RandomAccessFile rf = new RandomAccessFile(f, "r");
			arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf));
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
			LOGGER.error("Error reading archive file", e);
		} catch (SevenZipException e) {
			LOGGER.error("Caught 7-Zip exception", e);
		} catch (NullPointerException e) {
			LOGGER.error("Caught 7-Zip Null-Pointer Exception", e);
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
	public long length() {
		return 0;
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public String getSystemName() {
		return file.getAbsolutePath();
	}

	@Override
	public boolean isValid() {
		return file.exists();
	}
}