package net.pms.dlna;

import java.io.IOException;
import net.pms.dlna.virtual.*;

public class SearchAction extends VirtualFolder {
	private Search sobj;
	private char ch;
	private String name;

	public SearchAction(Search sobj, char ch) {
		this(sobj, ch, String.valueOf(ch));
	}

	public SearchAction(Search sobj, char ch, String name) {
		super(name, "images/Play1Hot_120.jpg");
		this.sobj = sobj;
		this.ch = ch;
		this.name = name;
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		return DLNAThumbnailInputStream.toThumbnailInputStream(getResourceInputStream("images/Play1Hot_120.jpg"));
	}

	@Override
	public synchronized void resolve() {
		setDiscovered(false);  // we can't clear this enough
	}

	@Override
	public void discoverChildren() {
		sobj.append(ch);
		setDiscovered(false);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isFolder() {
		return true;
		//	return false;
	}

	@Override
	public long length() {
		return -1; //DLNAMediaInfo.TRANS_SIZE;
	}

	@Override
	public boolean isValid() {
		return true;
	}
}
