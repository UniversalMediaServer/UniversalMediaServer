/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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


/**
 * An abstract class that is the parent of all on-demand subtitles resources
 * that might not be available locally until fetched from its source.
 *
 * @author Nadahar
 */
public abstract class DLNAMediaOnDemandSubtitle extends DLNAMediaSubtitle {

	/**
	 * Attempts to fetch the on-demand subtitles from its source.
	 *
	 * @return {@code true} if the subtitles were successfully fetched,
	 *         {@code false} otherwise.
	 */
	public abstract boolean fetch();

	@Override
	public void setExternalFile(File externalFile) throws FileNotFoundException {
		// Invalid operation for on-demand subtitles
	}

	@Override
	public void setExternalFileOnly(File externalFile) {
		// Invalid operation for on-demand subtitles
	}
}
