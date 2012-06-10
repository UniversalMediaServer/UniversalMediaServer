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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
	private File z;
	private ZipFile zip;

	public ZippedFile(File z) {
		this.z = z;
		setLastmodified(z.lastModified());
		try {
			zip = new ZipFile(z);
			Enumeration<? extends ZipEntry> enm = zip.entries();
			while (enm.hasMoreElements()) {
				ZipEntry ze = enm.nextElement();
				addChild(new ZippedEntry(z, ze.getName(), ze.getSize()));
			}
			zip.close();
		} catch (ZipException e) {
			LOGGER.error(null, e);
		} catch (IOException e) {
			LOGGER.error(null, e);
		}
	}

	@Override
	protected String getThumbnailURL() {
		if (getType() == Format.IMAGE) {
			// no thumbnail support for now for real based disk images
			return null;
		}
		return super.getThumbnailURL();
	}

	public InputStream getInputStream() {
		try {
			return new ZipInputStream(new FileInputStream(z));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		return z.getName();
	}

	public long length() {
		return z.length();
	}

	public boolean isFolder() {
		return true;
	}

	public long lastModified() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return z.getAbsolutePath();
	}

	@Override
	public boolean isValid() {
		return z.exists();
	}
}
