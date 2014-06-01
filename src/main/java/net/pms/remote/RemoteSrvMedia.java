package net.pms.remote;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RemoteSrvMedia extends DLNAResource{
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteSrvMedia.class);

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
	public byte[] getHeaders() {
		return server.authHdr().getBytes();
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

