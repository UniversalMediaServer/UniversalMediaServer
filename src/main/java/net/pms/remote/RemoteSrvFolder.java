package net.pms.remote;

import net.pms.dlna.virtual.VirtualFolder;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.formats.Format;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RemoteSrvFolder extends VirtualFolder {
	private String id;
	private RemoteServer server;

	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteSrvFolder.class);

	public RemoteSrvFolder(RemoteServer srv,String id,String name,String thumb) {
		super(name,thumb);
		this.id=id;
		this.server=srv;
	}

	public void discoverChildren() {
		try {
			String res = server.dlnaAction(id);
			LOGGER.debug("raw json "+res);
			JSONObject jobj = new JSONObject(res);
			JSONArray folders = jobj.getJSONArray("folders");
			JSONArray media = jobj.getJSONArray("media");
			for(int i=0; i< folders.length(); i++)  {
				JSONObject folder = folders.getJSONObject(i);
				String id = folder.getString("id");
				String name = folder.getString("name");
				String thumb = folder.getString("thumb");
				addChild(new RemoteSrvFolder(server, id, name, thumb));
			}
			for(int i=0; i< media.length(); i++)  {
				JSONObject m = media.getJSONObject(i);
				String id = m.getString("id");
				String name = m.getString("name");
				String thumb = m.getString("thumb");
				String fmt = m.getString("fmt");
				RemoteSrvMedia media1 = new RemoteSrvMedia(server, id, name, thumb);
				Class<?> clazz = Class.forName(fmt);
				media1.setFormat((Format)clazz.newInstance());
				addChild(media1);
			}
		} catch (Exception e) {
			LOGGER.debug("RemoteSrv error " + e);
		}
	}
}
