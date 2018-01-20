package net.pms.dlna;

import java.io.File;
import java.util.List;
import net.pms.Messages;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.external.ExternalListener;
import net.pms.util.UMSUtils;
import net.pms.util.UMSUtils.IOList;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Playlist extends VirtualFolder implements UMSUtils.IOListModes {
	private static final Logger LOGGER = LoggerFactory.getLogger(Playlist.class);
	protected UMSUtils.IOList list;
	protected int maxSize, mode;

	public Playlist(String name) {
		this(name, null, 0, AUTOSAVE);
	}

	public Playlist(String name, String filename) {
		this(name, filename, 0, AUTOSAVE);
	}

	public Playlist(String name, String filename, int maxSize, int mode) {
		super(name, "images/thumbnail-folder-256.png");
		this.maxSize = maxSize > 0 ? maxSize : 0;
		this.mode = mode;
//		list = Collections.synchronizedList(new ArrayList<DLNAResource>());
		list = new UMSUtils.IOList(filename, mode);
		list.save();
	}

	public File getFile()  {
		return list.getFile();
	}

	public void add(DLNAResource res) {
		DLNAResource res1;
		LOGGER.debug("Adding \"{}\" to playlist \"{}\"", res.getName(), getName());
		if (res instanceof VirtualVideoAction) {
			// don't add these
			return;
		}
		if (res.getParent() == this) {
			res1 = res; // best guess
			for (DLNAResource r : list) {
				if (r.getName().equals(res.getName())
					&& r.getSystemName().equals(res.getSystemName())) {
					res1 = r;
					break;
				}
			}
		} else {
			String data = res.write();
			if (!StringUtils.isEmpty(data) && res.getMasterParent() != null) {
				res1 = IOList.resolveCreateMethod(res.getMasterParent(), data);
				res1.setMasterParent(res.getMasterParent());
				res1.setMediaAudio(res.getMediaAudio());
				res1.setMediaSubtitle(res.getMediaSubtitle());
				res1.setResume(res.getResume());
			} else {
				res1 = res.clone();
				res1.setResume(res.getResume());
			}
		}
		list.remove(res1);
		if (maxSize > 0 && list.size() == maxSize) {
			list.remove(maxSize - 1);
		}
		list.add(0, res1);
		update();
	}

	public void remove(DLNAResource res) {
		LOGGER.debug("Removing \"{}\" from playlist \"{}\"", res.getName(), getName());
		list.remove(res);
		update();
	}

	public void clear() {
		LOGGER.debug("Clearing playlist \"{}\": {} items", getName(), list.size());
		list.clear();
		update();
	}

	public boolean isMode(int m) {
		return (mode & m) == m;
	}

	@Override
	public void discoverChildren() {
		if (list.size() > 0) {
			final Playlist self = this;
			// Save
			if (! isMode(AUTOSAVE)) {
				addChild(new VirtualVideoAction(Messages.getString("LooksFrame.9"), true) {
					@Override
					public boolean enable() {
						self.save();
						return true;
					}
				});
			}
			// Clear
			addChild(new VirtualVideoAction(Messages.getString("TracesTab.3"), true) {
				@Override
				public boolean enable() {
					self.clear();
					return true;
				}
			});
		}
		for (DLNAResource r : list) {
			// addchild might clear the masterparent
			// so fetch it first and readd
			ExternalListener master = r.getMasterParent();
			addChild(r);
			r.setMasterParent(master);
			if (r.isResume()) {
				// add this non resume after
				DLNAResource clone = r.clone();
				clone.setResume(null);
				addChild(clone);
				clone.setMasterParent(master);
			}
		}
	}

	public List<DLNAResource> getList() {
		return list;
	}

	public void update() {
		if (isMode(AUTOSAVE)) {
			save();
		}
		getChildren().clear();
		setDiscovered(false);
		if (list.size() < 1 && ! isMode(PERMANENT)) {
			// Self-delete if empty
			getParent().getChildren().remove(this);
		}
	}

	public void save() {
		list.save();
	}
}
