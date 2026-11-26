package net.pms.store.container.audioaddict;

import java.io.IOException;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.external.audioaddict.AudioAddictLogo.Logo;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

class AudioAddictFolder extends StoreContainer {

	private final Platform logoNetwork;

	AudioAddictFolder(Renderer renderer, String name, Platform network, String artwork) {
		super(renderer, name, artwork != null ? artwork : "/images/audioaddict/audioAddict.png");
		logoNetwork = artwork == null ? network : null;
	}

	@Override
	public String getThumbnailETag() {
		if (logoNetwork == null) {
			return super.getThumbnailETag();
		}
		Logo logo = logoNetwork.logo.get();
		return logo != null ? logo.etag() : "\"aa-default\"";
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (logoNetwork != null) {
			Logo logo = logoNetwork.logo.get();
			if (logo != null) {
				return DLNAThumbnailInputStream.toThumbnailInputStream(logo.thumbnail());
			}
		}
		return super.getThumbnailInputStream();
	}
}
