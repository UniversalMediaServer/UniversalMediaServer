package net.pms.remote;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


import com.sun.net.httpserver.Headers;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;


public class RemoteSrvMedia extends DLNAResource{
	private String id;
	private String name;
	private String thumb;
	private RemoteServer server;

	public RemoteSrvMedia(RemoteServer srv, String id,String name,String albumArt) {
		this.id = id;
		this.name=name;
		this.thumb=albumArt;
		this.server = srv;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		URL u=new URL(server.streamURL(id));
		return new BufferedInputStream(u.openStream());
	}

	public InputStream getThumbnailInputStream() {
		try {
			return downloadAndSend(server.url(thumb),true);
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSystemName() {
		return server.streamURL(id);
		//return getName();
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public long length() {
		return DLNAMediaInfo.TRANS_SIZE;
	}
}

