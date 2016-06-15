package net.pms.dlna;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;

public class FileSearch implements SearchObj {
	private ArrayList<RealFile> folders;

	public FileSearch(List<RealFile> folders) {
		this.folders = new ArrayList<>(folders);
	}

	public void update(List<RealFile> folders) {
		this.folders = (ArrayList<RealFile>) folders;
	}

	@Override
	public void search(String searchString, DLNAResource searcher) {
		searchString = searchString.toLowerCase();
		for (RealFile res : folders) {
			String name = res.getName().toLowerCase();
			if (name.contains(searchString)) {
				// easy case file contians search string, add it and be gone
				searcher.addChild(res.clone());
				continue;
			}
			if (res.getFile().isDirectory()) {
				// tricky case, recursive in to folders
				File f = res.getFile();
				searchFiles(f.listFiles(), searchString, searcher, 0);
			}
		}
	}

	private void searchFiles(File[] files, String str, DLNAResource searcher, int cnt) {
		if (files == null) {
			return;
		}
		for (File f : files) {
			String name = f.getName().toLowerCase();
			if (name.contains(str)) {
				searcher.addChild(new RealFile(f));
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
