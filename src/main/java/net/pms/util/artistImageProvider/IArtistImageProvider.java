package net.pms.util.artistImageProvider;

import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;

public interface IArtistImageProvider {

	public DLNAThumbnailInputStream getThumbnail(Renderer renderer, String artistName);

	public String getIdent();
}
