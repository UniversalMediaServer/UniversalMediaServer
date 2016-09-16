/*
 * Universal Media Server
 * Copyright (C) 2012  SharkHunter
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SevenZipEntry extends ZippedEntry implements IPushOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(SevenZipEntry.class);
	private File file;
	private String zeName;
	private long length;
	private transient IInArchive arc;

	public SevenZipEntry(File file, String zeName, long length) {
		super(file, zeName, length);
		this.zeName = zeName;
		this.file = file;
		this.length = length;
	}
	
	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = new Runnable() {
			RandomAccessFile rf;

			@Override
			public void run() {
				try {
					//byte data[] = new byte[65536];
					rf = new RandomAccessFile(file, "r");

					arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf));
					ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
					ISimpleInArchiveItem realItem = null;

					for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
						if (item.getPath().equals(zeName)) {
							realItem = item;
							break;
						}
					}

					if (realItem == null) {
						LOGGER.trace("No such item " + zeName + " found in archive");
						return;
					}

					ExtractOperationResult result = realItem.extractSlow(new ISequentialOutStream() {
						@Override
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
					if (result != ExtractOperationResult.OK)
						LOGGER.error("Error extracting item: " + result);
				} catch (FileNotFoundException | SevenZipException e) {
					LOGGER.debug("Unpack error. Possibly harmless.", e.getMessage());
				} finally {
					try {
						if (rf != null) {
							rf.close();
						}
						arc.close();
						out.close();
					} catch (IOException e) {
						LOGGER.debug("Caught exception", e);
//					} catch (SevenZipException e) {
//						LOGGER.debug("Caught 7-Zip exception", e);
					}
				}
			}
		};

		new Thread(r, "7Zip Extractor").start();
	}
}
