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
package net.pms.store;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.store.item.RealFile;

public class FileSearch implements SearchObj {

	private List<StoreResource> folders;

	public FileSearch(List<StoreResource> folders) {
		this.folders = new ArrayList<>(folders);
	}

	public void update(List<StoreResource> folders) {
		this.folders = new ArrayList<>(folders);
	}

	@Override
	public void search(String searchString, StoreContainer searcher) {
		searchString = searchString.toLowerCase();
		for (StoreResource res : folders) {
			if (res instanceof SystemFileResource systemFileResource) {
				String name = res.getName().toLowerCase();
				if (name.contains(searchString)) {
					// easy case file contains search string, add it and be gone
					searcher.addChild(res.clone());
					continue;
				}
				if (systemFileResource.getSystemFile().isDirectory()) {
					// tricky case, recursive in to folders
					File f = systemFileResource.getSystemFile();
					searchFiles(f.listFiles(), searchString, searcher, 0);
				}
			}
		}
	}

	private void searchFiles(File[] files, String str, StoreContainer searcher, int cnt) {
		if (files == null) {
			return;
		}
		for (File f : files) {
			String name = f.getName().toLowerCase();
			if (name.contains(str)) {
				searcher.addChild(new RealFile(searcher.getDefaultRenderer(), f));
				continue;
			}
			if (f.isDirectory()) {
				if (cnt >= PMS.getConfiguration().getSearchDepth()) {
					// this is here to avoid endless looping
					return;
				}
				searchFiles(f.listFiles(), str, searcher, cnt + 1);
			}
		}
	}
}
