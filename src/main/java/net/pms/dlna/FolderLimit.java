package net.pms.dlna;

import java.util.ArrayList;
import net.pms.dlna.virtual.VirtualFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderLimit extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(FolderLimit.class);
	private ArrayList<FolderLimitLevel> levels;
	private boolean discover;

	public FolderLimit() {
		super("Folder Limit", null);
		discover = false;
		levels = new ArrayList<>();
		levels.add(new FolderLimitLevel(0)); // create level 0
	}

	public void setStart(DLNAResource res) {
		LOGGER.debug("setting folder lim " + res);
		if (res == null) {
			return;
		}
		if (discover) {
			return;
		}
		int level = -1;
		DLNAResource tmp = res;
		while (tmp != null) {
			if (tmp instanceof FolderLimit) {
				return;
			}
			if (tmp instanceof FolderLimitLevel) {
				level = ((FolderLimitLevel) tmp).level();
				break;
			}
			tmp = tmp.getParent();
		}
		try {
			FolderLimitLevel fll = levels.get(level + 1);
			fll.setStart(res);
			if ((fll.level() == 0) && (levels.size() > 1)) {
				// we want to remove all levels 1+ so we clear all
				// and read level 0, its easier
				levels.clear();
				levels.add(fll);
			}
		} catch (IndexOutOfBoundsException e) { // create new level
			FolderLimitLevel fll = new FolderLimitLevel(level + 1);
			fll.setStart(res);
			levels.add(fll);
		}
	}

	@Override
	public void discoverChildren() {
		discover = true;
		for (DLNAResource res : levels) {
			addChild(res);
		}
		discover = false;
	}

	@Override
	public synchronized void resolve() {
		this.setDiscovered(false);
		this.getChildren().clear();
	}
}
