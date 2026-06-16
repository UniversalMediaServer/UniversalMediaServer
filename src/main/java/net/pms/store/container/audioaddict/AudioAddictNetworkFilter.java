package net.pms.store.container.audioaddict;

import java.util.List;
import net.pms.external.audioaddict.AudioAddictChannelDto;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.item.WebAudioStream;

public class AudioAddictNetworkFilter extends StoreContainer {

	private Platform network = null;

	public AudioAddictNetworkFilter(Renderer renderer, Platform network, String name) {
		super(renderer, name, null);
		this.network = network;

		List<AudioAddictChannelDto> filterList = AudioAddictService.get().getFilteredChannels(this.network, getName());
		for (AudioAddictChannelDto c : filterList) {
			MediaInfo mi = new MediaInfo();
			mi.setMimeType("audio/mpeg");
			mi.setMediaParser("STATIC");
			if (c.genres != null || c.descShort != null) {
				MediaAudioMetadata md = new MediaAudioMetadata();
				if (c.genres != null) {
					md.setArtist(c.genres);
					md.setGenre(c.genres);
				}
				if (c.descShort != null) {
					md.setAlbum(c.descShort);
				}
				mi.setAudioMetadata(md);
			}
			StoreResource sr = new WebAudioStream(renderer, c.name, c.streamUrl, "http:" + c.albumArt);
			sr.setMediaInfo(mi);
			addChild(sr);
		}
	}
}
