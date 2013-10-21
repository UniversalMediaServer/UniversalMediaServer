package net.pms.dlna;

import java.io.File;
import net.pms.dlna.virtual.VirtualFolder;

public class MonitorEntry extends VirtualFolder {
	private MediaMonitor mm;
	private File f;

	public MonitorEntry(File f, MediaMonitor mm) {
		super(f.getName(), null);
		this.mm = mm;
		this.f = f;
	}

	@Override
	public void discoverChildren() {
		mm.scanDir(f.listFiles(), this);
	}
}
