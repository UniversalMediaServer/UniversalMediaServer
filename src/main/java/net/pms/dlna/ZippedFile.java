/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.pms.formats.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippedFile extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZippedFile.class);
	private File file;
	private ZipFile zip;

	public ZippedFile(File file) {
		this.file = file;
		setLastModified(file.lastModified());

		try {
			zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> enm = zip.entries();

			while (enm.hasMoreElements()) {
				ZipEntry ze = enm.nextElement();
				addChild(new ZippedEntry(file, ze.getName(), ze.getSize()));
			}

			zip.close();
		} catch (ZipException e) {
			LOGGER.error("Error reading zip file", e);
		} catch (IOException e) {
			LOGGER.error("Error reading zip file", e);
		}
	}

	@Override
	protected String getThumbnailURL(DLNAImageProfile profile) {
		if (getType() == Format.IMAGE) {
			// no thumbnail support for now for zip files
			return null;
		}

		return super.getThumbnailURL(profile);
	}

	@Override
	public InputStream getInputStream() {
		try {
			return new ZipInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public long length() {
		return file.length();
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	// XXX unused
	@Deprecated
	public long lastModified() {
		return 0;
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
