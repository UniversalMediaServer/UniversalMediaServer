package net.pms.dlna;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.dlna.virtual.VirtualFolder;

public class FolderLimitLevel extends VirtualFolder {
	
	private int level;
	private static final Logger logger = LoggerFactory.getLogger(FolderLimitLevel.class);
	private DLNAResource start;
	
	public FolderLimitLevel(int level) {
		super("Level "+String.valueOf(level),null);
		this.level=level;
		this.start=null;
	}
	
	public int level() {
		return level;
	}
	
	public void setStart(DLNAResource r) {
		if(r.getParent()==null)
			start=(DLNAResource) r.clone();
		else
			start=(DLNAResource) r.getParent().clone();
		resolve();
	}
	
	public void discoverChildren() {
		if(start!=null)
			addChild(start);
	}
	
	public void resolve() {
		this.setDiscovered(false);
		this.getChildren().clear();
	}

}
